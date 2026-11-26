package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.dto.trainee.TrainingRegistration.*;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingRegistrationService {
  private static final ZoneId TRAINING_ZONE = ZoneId.of("Asia/Kolkata");
  private static final String PROCESSING = "DATA_PROCESSING";
  private static final String SKILLS = "COALESCE(plan.skills_to_teach,c.description,c.course_name)";
  private static final String TITLE = "COALESCE(r.title,c.course_name)";
  private static final String FIRST_SESSION = """
      (SELECT MIN(s.starts_at) FROM venue_booking_sessions s JOIN venue_bookings schedule USING (booking_id)
        WHERE schedule.batch_id=b.batch_id AND s.reserved_at IS NOT NULL AND s.released_at IS NULL)
      """;
  private static final String BATCH_SELECT = """
      SELECT b.batch_id,b.batch_code,b.start_date,b.end_date,bs.status_code,
             %s AS title,%s AS skills,p.provider_name,tc.center_name,d.district_id,d.district_name,st.state_name,
             CONCAT_WS(', ',NULLIF(tc.address_line1,''),NULLIF(tc.address_line2,''),NULLIF(tc.pincode,'')) AS address,
             (plan.requirement_id IS NOT NULL) AS government_led,%s AS first_session,
             plan.minimum_education_level_id,edu.education_name AS minimum_education_level,plan.recommended_prerequisites,
             GREATEST(0,CAST(b.capacity AS SIGNED) - CAST((SELECT COUNT(*) FROM training_enrollments e
                       WHERE e.batch_id=b.batch_id AND e.deleted_at IS NULL) AS SIGNED)) AS seats_available,
             v.booking_status AS booking_status,
             (p.deleted_at IS NULL AND p.lifecycle_status_id=1 AND tc.deleted_at IS NULL AND tc.lifecycle_status_id=1
               AND c.deleted_at IS NULL) AS active_venue
      FROM training_batches b
      JOIN ref_batch_status bs USING (batch_status_id)
      JOIN courses c USING (course_id)
      JOIN training_providers p ON p.provider_id=b.provider_id
      LEFT JOIN training_centers tc ON tc.center_id=b.center_id
      LEFT JOIN districts d ON d.district_id=tc.district_id
      LEFT JOIN states st ON st.state_id=tc.state_id
      LEFT JOIN venue_bookings v ON v.booking_id=(
        SELECT MAX(vb.booking_id) FROM venue_bookings vb WHERE vb.batch_id=b.batch_id AND (
          vb.booking_status IN ('APPROVED','COMPLETED') OR (vb.booking_status='CANCELLED' AND vb.reviewed_at IS NOT NULL)))
      LEFT JOIN venue_booking_training_plans plan ON plan.booking_id=v.booking_id
      LEFT JOIN ref_education_level edu ON edu.education_level_id=plan.minimum_education_level_id
      LEFT JOIN training_requirements r ON r.requirement_id=plan.requirement_id
      WHERE b.deleted_at IS NULL
      """.formatted(TITLE, SKILLS, FIRST_SESSION);

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final TrainingBatchRepository batches;
  private final TraineeRepository trainees;
  private final TraineeConsentService consent;
  private final CandidateLifecycleOrchestratorService lifecycle;
  private final TrainingAdmissionService admissions;

  public TrainingRegistrationService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, TrainingBatchRepository batches,
      TraineeRepository trainees, TraineeConsentService consent, CandidateLifecycleOrchestratorService lifecycle,
      TrainingAdmissionService admissions) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.batches = batches;
    this.trainees = trainees;
    this.consent = consent;
    this.lifecycle = lifecycle;
    this.admissions = admissions;
  }

  public RegistrationBatchPage find(String search, Long stateId, Long districtId, LocalDate from, LocalDate through, int page, int size) {
    ownTraineeId();
    if (from != null && through != null && through.isBefore(from)) {
      throw new BadRequestException("The end of the date range must be on or after its start.", "INVALID_TRAINING_DATE_RANGE");
    }
    StringBuilder sql = new StringBuilder(BATCH_SELECT).append("""
        AND bs.status_code='OPEN' AND v.booking_status='APPROVED' AND b.start_date>=?
        """);
    LocalDateTime now = LocalDateTime.now(TRAINING_ZONE);
    List<Object> args = new ArrayList<>();
    args.add(now.toLocalDate());
    filter(sql, args, " AND tc.state_id=?", stateId);
    filter(sql, args, " AND tc.district_id=?", districtId);
    filter(sql, args, " AND b.start_date>=?", from);
    filter(sql, args, " AND b.start_date<=?", through);
    var keywords = new KeywordSearch(search);
    sql.append(" HAVING active_venue=1 AND first_session>? ORDER BY first_session,b.batch_id");
    args.add(now);
    long offset = (long) page * size;
    if (keywords.isEmpty()) {
      sql.append(" LIMIT ? OFFSET ?");
      args.add(size + 1);
      args.add(offset);
    }
    var rows = jdbc.query(sql.toString(), (rs, row) -> batch(rs), args.toArray());
    if (!keywords.isEmpty()) {
      rows = keywords.rank(rows, item -> item.title() + " " + item.skills())
          .stream().skip(offset).limit(size + 1L).toList();
    }
    return new RegistrationBatchPage(rows.stream().limit(size).toList(), rows.size() > size);
  }

  public TrainingBatchDetails details(long batchId) {
    long traineeId = ownTraineeId();
    var item = lookup(batchId);
    var enrollment = jdbc.query("""
        SELECT e.enrollment_number,s.status_code FROM training_enrollments e
        JOIN ref_enrollment_status s USING (enrollment_status_id)
        WHERE e.trainee_id=? AND e.batch_id=? AND e.deleted_at IS NULL
        """, (rs, row) -> new ExistingEnrollment(rs.getString(1), rs.getString(2)), traineeId, batchId)
        .stream().findFirst().orElse(null);
    boolean upcoming = item.isUpcoming(LocalDateTime.now(TRAINING_ZONE));
    if (enrollment == null && !upcoming) throw missingBatch();
    var timetable = jdbc.query("""
        SELECT s.starts_at,s.ends_at FROM venue_booking_sessions s JOIN venue_bookings v USING (booking_id)
        WHERE v.batch_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL
        ORDER BY s.starts_at,s.session_id
        """, (rs, row) -> new TrainingTimetableSession(rs.getObject(1, LocalDateTime.class), rs.getObject(2, LocalDateTime.class)), batchId);
    return new TrainingBatchDetails(item.batch(), item.address(), timetable, upcoming && "OPEN".equals(item.batch().status()),
        consent.hasActiveConsent(traineeId, PROCESSING), enrollment == null ? null : enrollment.number(),
        enrollment == null ? null : enrollment.status(), admissions.eligibility(traineeId, item.batch().admission()));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TrainingEnrollmentResponse register(long batchId) {
    ownTraineeId();
    Long actor = scope.resolveAuthenticatedUserId();
    batches.findByIdForUpdate(batchId).orElseThrow(TrainingRegistrationService::missingBatch);
    // Self-service consent changes lock the same trainee row.
    trainees.findLockedByUserId(actor).filter(t -> t.getDeletedAt() == null)
        .orElseThrow(TrainingRegistrationService::missingTrainee);
    var item = lookup(batchId);
    LocalDateTime now = LocalDateTime.now(TRAINING_ZONE);
    if (!item.isUpcoming(now) || !"OPEN".equals(item.batch().status())) {
      throw new ConflictException("Registration is closed for this batch.", "TRAINING_REGISTRATION_CLOSED");
    }
    return lifecycle.confirmSelfEnrollment(batchId, now.toLocalDate()).enrollment();
  }

  public TrainingRosterPage roster(long batchId, int page, int size) {
    long providerId = scope.resolveAuthenticatedUser().map(u -> u.getTrainingProviderId())
        .orElseThrow(() -> new AccessDeniedException("A training provider assignment is required."));
    if (!Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM training_batches WHERE batch_id=? AND provider_id=? AND deleted_at IS NULL)
        """, Boolean.class, batchId, providerId))) throw missingBatch();
    var entries = jdbc.query("""
        SELECT e.enrollment_id,e.enrollment_number,t.trainee_id,t.registration_number,t.email,t.phone,
               CONCAT_WS(' ',t.first_name,NULLIF(t.middle_name,''),NULLIF(t.last_name,'')) AS full_name,s.status_code
        FROM training_enrollments e JOIN trainees t USING (trainee_id)
        JOIN ref_enrollment_status s USING (enrollment_status_id)
        WHERE e.batch_id=? AND e.provider_id=? AND e.deleted_at IS NULL AND t.deleted_at IS NULL
        ORDER BY e.enrollment_id LIMIT ? OFFSET ?
        """, (rs, row) -> new RosterCandidate(rs.getLong("trainee_id"), rs.getLong("enrollment_id"),
            rs.getString("enrollment_number"), rs.getString("full_name"), rs.getString("registration_number"),
            rs.getString("status_code"), rs.getString("email"), rs.getString("phone")),
        batchId, providerId, size + 1, (long) page * size);
    return new TrainingRosterPage(entries.stream().limit(size).map(this::withConsentedContact).toList(), entries.size() > size);
  }

  private TrainingRosterEntry withConsentedContact(RosterCandidate candidate) {
    long traineeId = candidate.traineeId();
    boolean mayShare = (candidate.email() != null || candidate.phone() != null) && consent.hasActiveConsent(traineeId, PROCESSING)
        && consent.hasActiveConsent(traineeId, "PROVIDER_CONTACT_SHARING");
    return new TrainingRosterEntry(candidate.enrollmentId(), candidate.number(), candidate.name(), candidate.registrationNumber(),
        candidate.status(), mayShare ? candidate.email() : null, mayShare ? candidate.phone() : null);
  }

  private PublishedBatch lookup(long batchId) {
    return jdbc.query(BATCH_SELECT + " AND b.batch_id=?", (rs, row) -> new PublishedBatch(batch(rs),
        rs.getString("address"), rs.getString("booking_status"), rs.getBoolean("active_venue")), batchId)
        .stream().findFirst().orElseThrow(TrainingRegistrationService::missingBatch);
  }

  private long ownTraineeId() {
    return scope.resolveAuthenticatedTrainee().filter(t -> t.getDeletedAt() == null)
        .orElseThrow(TrainingRegistrationService::missingTrainee).getId();
  }

  private static RegistrationBatch batch(ResultSet rs) throws SQLException {
    return new RegistrationBatch(rs.getLong("batch_id"), rs.getString("batch_code"), rs.getString("title"), rs.getString("skills"),
        rs.getString("provider_name"), rs.getString("center_name"), rs.getLong("district_id"), rs.getString("district_name"),
        rs.getString("state_name"), rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
        rs.getObject("first_session", LocalDateTime.class), rs.getString("status_code"), rs.getBoolean("government_led"),
        rs.getLong("seats_available"), TrainingAdmissionService.read(rs));
  }

  private static void filter(StringBuilder sql, List<Object> args, String condition, Object value) {
    if (value == null) return;
    sql.append(condition);
    args.add(value);
  }

  private static ResourceNotFoundException missingBatch() {
    return new ResourceNotFoundException("This training batch is unavailable.");
  }

  private static ResourceNotFoundException missingTrainee() {
    return new ResourceNotFoundException("No trainee profile is linked to this account.");
  }

  private record PublishedBatch(RegistrationBatch batch, String address, String bookingStatus, boolean activeVenue) {
    boolean isUpcoming(LocalDateTime now) {
      return activeVenue && "APPROVED".equals(bookingStatus) && List.of("OPEN", "PLANNED").contains(batch.status())
          && batch.firstSession() != null && batch.firstSession().isAfter(now);
    }
  }

  private record ExistingEnrollment(String number, String status) {}

  private record RosterCandidate(long traineeId, long enrollmentId, String number, String name, String registrationNumber,
                                 String status, String email, String phone) {}
}
