package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.venue.TrainingRequirementDto.Application;
import in.gov.sih.sih26135.dto.venue.TrainingAdmission;
import in.gov.sih.sih26135.dto.venue.TrainingRequirementDto.Requirement;
import in.gov.sih.sih26135.dto.venue.VenueBookingDto.*;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.service.ActiveGeography;
import in.gov.sih.sih26135.service.AuditLogService;
import in.gov.sih.sih26135.service.TrainingAdmissionService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VenueBookingService {
  private static final ZoneId TRAINING_ZONE = ZoneId.of("Asia/Kolkata");
  private static final String SHARED = "SHARED";
  private static final String TEMPORARY = "TEMPORARY";
  private static final String PENDING = "PENDING";
  private static final String APPROVED = "APPROVED";
  private static final String GOVERNMENT_BATCH_PREFIX = "GOV-REQ-";

  private static final String BOOKING_SELECT = """
      SELECT b.*, p.provider_name, c.course_name, pr.program_name, bs.status_code AS batch_status,
        bp.requirement_id,bp.skills_to_teach,bp.training_notes,bp.minimum_education_level_id,
        edu.education_name AS minimum_education_level,bp.recommended_prerequisites
      FROM venue_bookings b
      JOIN training_providers p ON p.provider_id=b.provider_id
      LEFT JOIN courses c ON c.course_id=b.course_id
      LEFT JOIN programs pr ON pr.program_id=b.program_id
      LEFT JOIN training_batches tb ON tb.batch_id=b.batch_id
      LEFT JOIN ref_batch_status bs ON bs.batch_status_id=tb.batch_status_id
      LEFT JOIN venue_booking_training_plans bp ON bp.booking_id=b.booking_id
      LEFT JOIN ref_education_level edu ON edu.education_level_id=bp.minimum_education_level_id
      """;
  private static final String VENUE_SELECT = """
      SELECT center_id,center_code,center_name,address_line1,state_id,district_id,operational_capacity,
        venue_type,provider_id,lifecycle_status_id,deleted_at
      FROM training_centers
      """;
  private static final RowMapper<Booking> BOOKING_ROW = (rs, index) -> new Booking(
      rs.getLong("booking_id"), rs.getLong("provider_id"), rs.getString("provider_name"),
      nullableLong(rs, "center_id"), nullableLong(rs, "batch_id"), nullableLong(rs, "replaces_booking_id"),
      rs.getString("venue_type"), rs.getString("booking_status"), rs.getString("batch_code"),
      nullableLong(rs, "course_id"), rs.getString("course_name"), nullableLong(rs, "program_id"), rs.getString("program_name"),
      rs.getInt("seats"), rs.getString("venue_name"), rs.getString("venue_address"), rs.getLong("state_id"),
      rs.getLong("district_id"), rs.getInt("venue_capacity"), rs.getString("premises_permission"),
      rs.getString("decision_note"), dateTime(rs, "created_at"), dateTime(rs, "reviewed_at"), rs.getString("batch_status"),
      nullableLong(rs, "requirement_id"), rs.getString("skills_to_teach"), rs.getString("training_notes"), TrainingAdmissionService.read(rs));

  private record Venue(SharedVenue details, String type, Long providerId, boolean active) {}
  private record BookableBatch(long id, String status, LocalDate startDate, boolean removed) {}

  private final JdbcTemplate jdbc;
  private final TrainingWorkflowAccess access;
  private final AuditLogService audits;
  private final SkillsTrainingReporting reporting;
  private final TrainingAdmissionService admissions;

  public VenueBookingService(JdbcTemplate jdbc, TrainingWorkflowAccess access, AuditLogService audits,
      SkillsTrainingReporting reporting, TrainingAdmissionService admissions) {
    this.jdbc = jdbc;
    this.access = access;
    this.audits = audits;
    this.reporting = reporting;
    this.admissions = admissions;
  }

  public List<SharedVenue> sharedVenues(Long state, Long district, int page, int size) {
    var scope = readScope();
    if (scope.isStateScoped()) {
      if ((state != null && !state.equals(scope.stateId()))
          || (district != null && scope.districtId() != null && !district.equals(scope.districtId()))) {
        throw new AccessDeniedException("The venue is outside your assigned jurisdiction");
      }
      state = scope.stateId();
      if (scope.districtId() != null) {
        district = scope.districtId();
      }
    }
    if (state == null && district != null) {
      state = jdbc.queryForList("SELECT state_id FROM districts WHERE district_id=?", Long.class, district)
          .stream().findFirst().orElse(null);
    }
    ActiveGeography.validate(jdbc, state, district);
    return jdbc.query(VENUE_SELECT + """
        WHERE venue_type='SHARED' AND deleted_at IS NULL AND lifecycle_status_id=1
          AND (? IS NULL OR state_id=?) AND (? IS NULL OR district_id=?)
        ORDER BY center_name,center_id LIMIT ? OFFSET ?
        """, (rs, i) -> sharedVenue(rs), state, state, district, district, size, (long) page * size);
  }

  public List<BusySlot> availability(long centerId, LocalDate from, LocalDate to) {
    var scope = readScope();
    Venue venue = venue(centerId, false);
    if (!SHARED.equals(venue.type()) || !venue.active()) {
      throw unavailable();
    }
    if (scope.isStateScoped()) {
      requireJurisdiction(scope, venue.details().stateId(), venue.details().districtId());
    }
    if (!from.isBefore(to) || to.isAfter(from.plusDays(93))) {
      throw invalid("Select a date range of up to 93 days.", "VENUE_CALENDAR_RANGE_INVALID");
    }
    boolean canManage = TrainingWorkflowAccess.hasAuthority("venue.shared.manage") && !scope.isProviderScoped();
    return jdbc.query("""
        SELECT s.starts_at,s.ends_at,NULL AS blackout_id,NULL AS reason
        FROM venue_booking_sessions s JOIN venue_bookings b ON b.booking_id=s.booking_id
        WHERE b.center_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL
          AND s.starts_at<? AND s.ends_at>?
        UNION ALL
        SELECT starts_at,ends_at,blackout_id,reason FROM venue_blackouts
        WHERE center_id=? AND released_at IS NULL AND starts_at<? AND ends_at>?
        ORDER BY starts_at,ends_at
        """, (rs, i) -> new BusySlot(dateTime(rs, "starts_at"), dateTime(rs, "ends_at"),
            canManage ? nullableLong(rs, "blackout_id") : null, canManage ? rs.getString("reason") : null),
        centerId, to.atStartOfDay(), from.atStartOfDay(), centerId, to.atStartOfDay(), from.atStartOfDay());
  }

  public List<Booking> bookings(String status, int page, int size) {
    var scope = readScope();
    StringBuilder where = new StringBuilder(" WHERE (? IS NULL OR b.booking_status=?)");
    List<Object> args = new ArrayList<>();
    args.add(status);
    args.add(status);
    if (scope.isProviderScoped()) {
      where.append(" AND b.provider_id=?");
      args.add(scope.trainingProviderId());
    } else if (scope.isStateScoped()) {
      where.append(" AND b.state_id=?");
      args.add(scope.stateId());
      if (scope.districtId() != null) {
        where.append(" AND b.district_id=?");
        args.add(scope.districtId());
      }
    }
    args.add(size);
    args.add((long) page * size);
    return jdbc.query(BOOKING_SELECT + where + " ORDER BY b.booking_id DESC LIMIT ? OFFSET ?", BOOKING_ROW, args.toArray());
  }

  public Details details(long id) {
    Booking booking = booking(id, false);
    requireReadAccess(readScope(), booking);
    return detailsFor(booking);
  }

  @Transactional
  public SharedVenue createSharedVenue(NewSharedVenue request) {
    var scope = reviewerScope();
    requireJurisdiction(scope, request.stateId(), request.districtId());
    ActiveGeography.validate(jdbc, request.stateId(), request.districtId());
    try {
      long id = insert("""
          INSERT INTO training_centers(center_code,center_name,address_line1,pincode,state_id,district_id,
            operational_capacity,lifecycle_status_id,venue_type)
          VALUES (?,?,?,?,?,?,?,1,'SHARED')
          """, request.code().trim(), request.name().trim(), request.address().trim(), request.pincode(),
          request.stateId(), request.districtId(), request.capacity());
      audits.recordStateChange("CREATE", "TrainingCenter", id, "Shared training center registered", null,
          Map.of("venueType", SHARED, "districtId", request.districtId(), "capacity", request.capacity()), List.of());
      return venue(id, false).details();
    } catch (DuplicateKeyException ex) {
      throw conflict("Center code already exists", "CENTER_CODE_ALREADY_EXISTS");
    }
  }

  @Transactional
  public BusySlot addBlackout(long centerId, Blackout request) {
    Venue venue = venue(centerId, true);
    requireJurisdiction(reviewerScope(), venue.details().stateId(), venue.details().districtId());
    if (!SHARED.equals(venue.type()) || !venue.active()) {
      throw unavailable();
    }
    LocalDateTime now = now();
    SessionRequest session = new SessionRequest(request.startsAt(), request.endsAt());
    validateSessions(List.of(session), now);
    checkAvailability(centerId, List.of(session), 0, now);
    long id = insert("INSERT INTO venue_blackouts(center_id,starts_at,ends_at,reason,created_by) VALUES (?,?,?,?,?)",
        centerId, session.startsAt(), session.endsAt(), request.reason().trim(), actor().getId());
    return new BusySlot(session.startsAt(), session.endsAt(), id, request.reason().trim());
  }

  @Transactional
  public void releaseBlackout(long centerId, long blackoutId) {
    Venue venue = venue(centerId, true);
    requireJurisdiction(reviewerScope(), venue.details().stateId(), venue.details().districtId());
    if (jdbc.update("""
        UPDATE venue_blackouts SET released_at=? WHERE blackout_id=? AND center_id=? AND released_at IS NULL
        """, now(), blackoutId, centerId) == 0) {
      throw new ResourceNotFoundException("VenueBlackout", "id");
    }
  }
  @Transactional
  public Details request(NewBooking request) {
    long providerId = providerScope().trainingProviderId();
    if (request.requirementId() != null) {
      throw invalid("Government training slots are reserved when a reviewer approves an application.",
          "TRAINING_GOVERNMENT_BOOKING_REQUIRED");
    }
    LocalDateTime now = now();
    List<SessionRequest> sessions = validateSessions(request.sessions(), now);
    if ((request.centerId() == null) == (request.temporaryVenue() == null)) {
      throw invalid("Select one shared center or propose one temporary location.", "VENUE_TYPE_REQUIRED");
    }
    String skills = text(request.skillsToTeach());
    if (skills == null) {
      throw invalid("Enter the skills you will teach.", "TRAINING_SKILLS_REQUIRED");
    }
    validateProvider(providerId);
    String code = request.batchCode().trim();
    if (code.toUpperCase(Locale.ROOT).startsWith(GOVERNMENT_BATCH_PREFIX)) {
      throw invalid("Batch codes beginning with GOV-REQ- are reserved for government training.", "TRAINING_BATCH_CODE_RESERVED");
    }
    ensureBatchCodeAvailable(code);
    TemporaryVenue temporary = request.temporaryVenue();
    SharedVenue venue;
    String kind;
    if (temporary != null) {
      ActiveGeography.validate(jdbc, temporary.stateId(), temporary.districtId());
      venue = new SharedVenue(0, "", temporary.name().trim(), temporary.address().trim(), temporary.stateId(),
          temporary.districtId(), temporary.capacity());
      kind = TEMPORARY;
    } else {
      Venue center = venue(request.centerId(), true);
      if (!SHARED.equals(center.type()) || !center.active()) {
        throw unavailable();
      }
      venue = center.details();
      kind = SHARED;
      checkAvailability(venue.id(), sessions, 0, now);
    }
    validateSeats(request.seats(), venue.capacity());
    admissions.validateMinimum(request.minimumEducationLevelId());
    long id = createPending(providerId, venue, kind, code, request.seats(),
        temporary != null ? temporary.permissionToUse().trim() : null, null, skills, text(request.trainingNotes()),
        new TrainingAdmission(request.minimumEducationLevelId(), null, text(request.recommendedPrerequisites())), sessions);
    return detailsFor(booking(id, false));
  }

  SharedVenue lockGovernmentCentre(long centerId, long stateId, long districtId, int seats) {
    Venue center = venue(centerId, true);
    if (!SHARED.equals(center.type()) || !center.active()) throw unavailable();
    SharedVenue details = center.details();
    if (details.stateId() != stateId || details.districtId() != districtId) {
      throw invalid("Choose a shared center in the requirement's district.", "TRAINING_VENUE_OUTSIDE_AREA");
    }
    validateSeats(seats, details.capacity());
    return details;
  }

  long reserveGovernmentTraining(Requirement requirement, Application application) {
    LocalDateTime now = now();
    SharedVenue centre = lockGovernmentCentre(requirement.centerId(), requirement.stateId(), requirement.districtId(),
        requirement.seats());
    List<SessionRequest> sessions = validateSessions(application.startDate().datesUntil(application.endDate().plusDays(1))
        .map(day -> new SessionRequest(day.atTime(application.dailyStartTime()), day.atTime(application.dailyEndTime())))
        .toList(), now);
    long id = createPending(application.providerId(), centre, SHARED, GOVERNMENT_BATCH_PREFIX + requirement.id(),
        requirement.seats(), null, requirement.id(), requirement.skillsToTeach(), application.deliveryNotes(), requirement.admission(), sessions);
    decide(booking(id, false), true, null, now, sessions);
    return id;
  }

  private long createPending(long providerId, SharedVenue venue, String kind, String code, int seats,
      String permission, Long requirementId, String skills, String notes, TrainingAdmission admission, List<SessionRequest> sessions) {
    long id = insert("""
        INSERT INTO venue_bookings(provider_id,center_id,venue_type,batch_code,seats,
          venue_name,venue_address,state_id,district_id,venue_capacity,premises_permission,requested_by)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """, providerId, SHARED.equals(kind) ? venue.id() : null, kind, code, seats,
        venue.name(), venue.address(), venue.stateId(), venue.districtId(), venue.capacity(),
        permission, actor().getId());
    insertSessions(id, sessions);
    jdbc.update("""
        INSERT INTO venue_booking_training_plans(booking_id,requirement_id,skills_to_teach,training_notes,
          minimum_education_level_id,recommended_prerequisites) VALUES (?,?,?,?,?,?)
        """, id, requirementId, skills, notes, admission.minimumEducationLevelId(), admission.recommendedPrerequisites());
    auditBooking("CREATE", id, null, PENDING, null);
    return id;
  }

  @Transactional
  public Details requestReschedule(long id, Reschedule request) {
    Booking existing = lockBooking(id, providerScope());
    requireApproved(existing);
    requireOpenBatch(lockBatch(existing.batchId()));
    LocalDateTime now = now();
    if (!hasFutureSessions(id, now)) {
      throw noFutureSessions();
    }
    if (!jdbc.queryForList("""
        SELECT booking_id FROM venue_bookings WHERE replaces_booking_id=? AND booking_status='PENDING' FOR UPDATE
        """, Long.class, id).isEmpty()) {
      throw conflict("There is already a rescheduling request awaiting review.", "VENUE_RESCHEDULE_PENDING");
    }
    List<SessionRequest> sessions = validateSessions(request.sessions(), now);
    validateAssignedSessions(existing, sessions);
    checkAvailability(existing.centerId(), sessions, existing.id(), now);
    long replacement = insert("""
        INSERT INTO venue_bookings(provider_id,center_id,batch_id,replaces_booking_id,venue_type,batch_code,course_id,
          program_id,seats,venue_name,venue_address,state_id,district_id,venue_capacity,premises_permission,requested_by)
        SELECT provider_id,center_id,batch_id,booking_id,venue_type,batch_code,course_id,program_id,seats,venue_name,
          venue_address,state_id,district_id,venue_capacity,premises_permission,?
        FROM venue_bookings WHERE booking_id=?
        """, actor().getId(), id);
    insertSessions(replacement, sessions);
    jdbc.update("""
        INSERT INTO venue_booking_training_plans(booking_id,requirement_id,skills_to_teach,training_notes,
          minimum_education_level_id,recommended_prerequisites)
        SELECT ?,requirement_id,skills_to_teach,training_notes,minimum_education_level_id,recommended_prerequisites
        FROM venue_booking_training_plans WHERE booking_id=?
        """, replacement, id);
    auditBooking("CREATE", replacement, null, PENDING, null);
    return detailsFor(booking(replacement, false));
  }

  @Transactional
  public Details review(long id, Review request) {
    Booking pending = lockBooking(id, reviewerScope());
    if (!PENDING.equals(pending.status())) {
      throw conflict("Only pending venue requests can be reviewed.", "VENUE_REQUEST_ALREADY_REVIEWED");
    }
    String reason = text(request.reason());
    if (!request.approved() && reason == null) {
      throw invalid("Enter a reason for declining this booking.", "VENUE_REASON_REQUIRED");
    }
    LocalDateTime now = now();
    List<SessionRequest> sessions = List.of();
    if (request.approved()) {
      sessions = validateSessions(detailsFor(pending).sessions().stream()
          .map(s -> new SessionRequest(s.startsAt(), s.endsAt())).toList(), now);
      validateAssignedSessions(pending, sessions);
    }
    return decide(pending, request.approved(), reason, now, sessions);
  }

  private Details decide(Booking pending, boolean approved, String reason, LocalDateTime now, List<SessionRequest> sessions) {
    long id = pending.id();
    Long centerId = pending.centerId();
    Long batchId = pending.batchId();
    Long courseId = pending.courseId();
    Long programId = pending.programId();
    if (approved) {
      validateProvider(pending.providerId());
      ActiveGeography.validate(jdbc, pending.stateId(), pending.districtId());
      centerId = approveVenue(pending);
      if (pending.replacesBookingId() != null) {
        rescheduleApprovedBatch(pending.replacesBookingId(), centerId, sessions, now);
      } else {
        checkAvailability(centerId, sessions, 0, now);
        if (courseId == null || programId == null) {
          var reference = reporting.forApprovedSkills(pending.skillsToTeach());
          courseId = reference.courseId();
          programId = reference.programId();
        }
        batchId = createBatch(pending, centerId, courseId, programId, sessions);
      }
      jdbc.update("UPDATE venue_booking_sessions SET reserved_at=? WHERE booking_id=?", now, id);
    }
    String decision = approved ? APPROVED : "REJECTED";
    jdbc.update("""
        UPDATE venue_bookings SET booking_status=?,center_id=?,batch_id=?,course_id=?,program_id=?,decision_note=?,reviewed_by=?,reviewed_at=?
        WHERE booking_id=?
        """, decision, centerId, batchId, courseId, programId, reason, actor().getId(), now, id);
    if (!approved) releaseLegacyPendingBooking(pending);
    auditBooking("STATUS_CHANGE", id, PENDING, decision, reason);
    return detailsFor(booking(id, false));
  }

  private long approveVenue(Booking pending) {
    if (pending.centerId() == null) {
      return insert("""
          INSERT INTO training_centers(provider_id,center_code,center_name,address_line1,state_id,
            district_id,operational_capacity,lifecycle_status_id,venue_type)
          VALUES (?,?,?,?,?,?,?,1,'TEMPORARY')
          """, pending.providerId(), "TMP-" + pending.id(), pending.venueName(), pending.venueAddress(),
          pending.stateId(), pending.districtId(), pending.venueCapacity());
    }
    Venue venue = venue(pending.centerId(), true);
    if (!venue.active() || !pending.venueType().equals(venue.type())
        || venue.details().stateId() != pending.stateId() || venue.details().districtId() != pending.districtId()
        || (TEMPORARY.equals(venue.type()) && !Objects.equals(pending.providerId(), venue.providerId()))) {
      throw unavailable();
    }
    validateSeats(pending.seats(), venue.details().capacity());
    return pending.centerId();
  }

  private void rescheduleApprovedBatch(long previousId, long centerId, List<SessionRequest> sessions, LocalDateTime now) {
    Booking existing = booking(previousId, true);
    requireApproved(existing);
    BookableBatch batch = lockBatch(existing.batchId());
    requireOpenBatch(batch);
    if (!hasFutureSessions(existing.id(), now)) {
      throw noFutureSessions();
    }
    checkAvailability(centerId, sessions, existing.id(), now);
    LocalDate first = hasEarlierSessions(batch.id(), now)
        ? batch.startDate() : sessions.get(0).startsAt().toLocalDate();
    releaseFuture(existing.id(), now);
    jdbc.update("UPDATE venue_bookings SET booking_status='SUPERSEDED' WHERE booking_id=?", existing.id());
    jdbc.update("UPDATE training_batches SET start_date=?,end_date=?,updated_at=? WHERE batch_id=?",
        first, sessions.get(sessions.size() - 1).endsAt().toLocalDate(), now, batch.id());
    auditBooking("STATUS_CHANGE", existing.id(), APPROVED, "SUPERSEDED", null);
  }

  @Transactional
  public Details close(long id, Close request) {
    Booking booking = lockBooking(id, providerScope());
    if (PENDING.equals(booking.status()) && request.action() == CloseAction.CANCEL) {
      jdbc.update("UPDATE venue_bookings SET booking_status='CANCELLED' WHERE booking_id=?", id);
      releaseLegacyPendingBooking(booking);
      auditBooking("STATUS_CHANGE", id, PENDING, "CANCELLED", text(request.reason()));
      return detailsFor(booking(id, false));
    }
    requireApproved(booking);
    BookableBatch batch = lockBatch(booking.batchId());
    requireOpenBatch(batch);
    LocalDateTime now = now();
    boolean complete = request.action() == CloseAction.COMPLETE;
    if (complete && (!"ONGOING".equals(batch.status()) || !hasEarlierSessions(batch.id(), now))) {
      throw conflict("Start the batch and begin a reserved session before completing training.", "VENUE_BATCH_NOT_STARTED");
    }
    String status = complete ? "COMPLETED" : "CANCELLED";
    releaseFuture(id, now);
    jdbc.update("UPDATE venue_bookings SET booking_status=? WHERE booking_id=?", status, id);
    jdbc.update("""
        UPDATE venue_bookings SET booking_status='CANCELLED',
          decision_note='The original batch was closed before rescheduling.'
        WHERE replaces_booking_id=? AND booking_status='PENDING'
        """, id);
    jdbc.update("""
        UPDATE training_batches SET batch_status_id=(SELECT batch_status_id FROM ref_batch_status WHERE status_code=?),
          updated_at=? WHERE batch_id=?
        """, status, now, batch.id());
    if (TEMPORARY.equals(booking.venueType())) {
      jdbc.update("UPDATE training_centers SET lifecycle_status_id=2 WHERE center_id=?", booking.centerId());
    }
    auditBooking("STATUS_CHANGE", id, APPROVED, status, text(request.reason()));
    audits.recordStateChange("STATUS_CHANGE", "TrainingBatch", batch.id(), "Venue batch closed",
        Map.of("status", batch.status()), Map.of("status", status), List.of());
    return detailsFor(booking(id, false));
  }

  private long createBatch(Booking pending, long centerId, long courseId, long programId, List<SessionRequest> sessions) {
    ensureBatchCodeAvailable(pending.batchCode());
    long batchId;
    try {
      batchId = insert("""
          INSERT INTO training_batches(batch_code,course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id)
          SELECT ?,?,?,?,?,?,?,?,batch_status_id FROM ref_batch_status WHERE status_code='PLANNED'
          """, pending.batchCode(), courseId, pending.providerId(), centerId, programId,
          sessions.get(0).startsAt().toLocalDate(), sessions.get(sessions.size() - 1).endsAt().toLocalDate(), pending.seats());
    } catch (DuplicateKeyException ex) {
      throw conflict("Batch code already exists", "BATCH_CODE_ALREADY_EXISTS");
    }
    audits.recordStateChange("CREATE", "TrainingBatch", batchId, "Batch created from an approved venue booking", null,
        Map.of("venueBookingId", pending.id(), "centerId", centerId, "status", "PLANNED"), List.of());
    return batchId;
  }
  private Booking lockBooking(long id, TrainingTenantScopeSupport.CallerScope scope) {
    Booking snapshot = booking(id, false);
    requireReadAccess(scope, snapshot);
    if (snapshot.centerId() != null) {
      venue(snapshot.centerId(), true);
    }
    Booking current = booking(id, true);
    requireReadAccess(scope, current);
    if (!Objects.equals(snapshot.centerId(), current.centerId())) {
      throw conflict("The booking changed while you were reviewing it. Refresh and retry.", "VENUE_REQUEST_CHANGED");
    }
    return current;
  }

  private Booking booking(long id, boolean lock) {
    return jdbc.query(BOOKING_SELECT + " WHERE b.booking_id=?" + (lock ? " FOR UPDATE OF b" : ""), BOOKING_ROW, id)
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("VenueBooking", "id"));
  }

  private Details detailsFor(Booking booking) {
    return new Details(booking, jdbc.query("""
        SELECT starts_at,ends_at,reserved_at,released_at FROM venue_booking_sessions WHERE booking_id=? ORDER BY starts_at
        """, (rs, i) -> new Session(dateTime(rs, "starts_at"), dateTime(rs, "ends_at"),
            dateTime(rs, "reserved_at"), dateTime(rs, "released_at")), booking.id()));
  }

  private Venue venue(long id, boolean lock) {
    return jdbc.query(VENUE_SELECT + " WHERE center_id=?" + (lock ? " FOR UPDATE" : ""), (rs, i) ->
        new Venue(sharedVenue(rs), rs.getString("venue_type"), nullableLong(rs, "provider_id"),
            rs.getLong("lifecycle_status_id") == 1L && rs.getTimestamp("deleted_at") == null), id)
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
  }

  private static SharedVenue sharedVenue(ResultSet rs) throws SQLException {
    return new SharedVenue(rs.getLong("center_id"), rs.getString("center_code"), rs.getString("center_name"),
        rs.getString("address_line1"), rs.getLong("state_id"), rs.getLong("district_id"), rs.getInt("operational_capacity"));
  }

  private BookableBatch lockBatch(long id) {
    return jdbc.query("""
        SELECT b.batch_id,s.status_code,b.start_date,b.deleted_at FROM training_batches b
        JOIN ref_batch_status s USING (batch_status_id) WHERE b.batch_id=? FOR UPDATE
        """, (rs, i) -> new BookableBatch(rs.getLong(1), rs.getString(2),
            rs.getDate(3).toLocalDate(), rs.getTimestamp(4) != null), id)
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));
  }

  private static void requireOpenBatch(BookableBatch batch) {
    if (batch.removed() || List.of("COMPLETED", "CANCELLED").contains(batch.status())) {
      throw conflict("This training batch is already closed.", "VENUE_BATCH_CLOSED");
    }
  }

  private static void requireApproved(Booking booking) {
    if (!APPROVED.equals(booking.status()) || booking.batchId() == null || booking.centerId() == null) {
      throw conflict("This action requires the current approved booking.", "VENUE_APPROVED_BOOKING_REQUIRED");
    }
  }

  private void checkAvailability(long centerId, List<SessionRequest> sessions, long replace, LocalDateTime now) {
    for (SessionRequest session : sessions) {
      var reserved = jdbc.queryForList("""
          SELECT s.session_id FROM venue_booking_sessions s JOIN venue_bookings b ON b.booking_id=s.booking_id
          WHERE b.center_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL
            AND s.starts_at<? AND s.ends_at>? AND NOT (b.booking_id=? AND s.starts_at>?)
          LIMIT 1 FOR UPDATE
          """, Long.class, centerId, session.endsAt(), session.startsAt(), replace, now);
      var blocked = jdbc.queryForList("""
          SELECT blackout_id FROM venue_blackouts
          WHERE center_id=? AND released_at IS NULL AND starts_at<? AND ends_at>? LIMIT 1 FOR UPDATE
          """, Long.class, centerId, session.endsAt(), session.startsAt());
      if (!reserved.isEmpty() || !blocked.isEmpty()) {
        throw conflict("At least one requested session overlaps an existing reservation or center closure.",
            "VENUE_SLOT_UNAVAILABLE");
      }
    }
  }

  private static List<SessionRequest> validateSessions(List<SessionRequest> sessions, LocalDateTime now) {
    List<SessionRequest> sorted = sessions.stream().sorted(Comparator.comparing(SessionRequest::startsAt)).toList();
    LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime previousEnd = null;
    for (SessionRequest session : sorted) {
      if (session.startsAt().isBefore(currentMinute) || !session.startsAt().isBefore(session.endsAt())
          || !session.endsAt().isAfter(now)
          || session.startsAt().getNano() != 0 || session.endsAt().getNano() != 0) {
        throw invalid("Enter a start time in the current minute or later and a future end time after it.",
            "VENUE_SESSION_TIME_INVALID");
      }
      if (previousEnd != null && session.startsAt().isBefore(previousEnd)) {
        throw invalid("Sessions in the same request cannot overlap.", "VENUE_REQUEST_SESSIONS_OVERLAP");
      }
      previousEnd = session.endsAt();
    }
    return sorted;
  }

  private void insertSessions(long id, List<SessionRequest> sessions) {
    jdbc.batchUpdate("INSERT INTO venue_booking_sessions(booking_id,starts_at,ends_at) VALUES (?,?,?)",
        sessions.stream().map(s -> new Object[]{id, s.startsAt(), s.endsAt()}).toList());
  }

  private void releaseFuture(long id, LocalDateTime now) {
    jdbc.update("""
        UPDATE venue_booking_sessions SET released_at=?
        WHERE booking_id=? AND reserved_at IS NOT NULL AND released_at IS NULL AND starts_at>?
        """, now, id, now);
  }

  private boolean hasFutureSessions(long id, LocalDateTime now) {
    return !jdbc.queryForList("""
        SELECT session_id FROM venue_booking_sessions WHERE booking_id=? AND reserved_at IS NOT NULL
          AND released_at IS NULL AND starts_at>? LIMIT 1 FOR UPDATE
        """, Long.class, id, now).isEmpty();
  }

  private boolean hasEarlierSessions(long batchId, LocalDateTime now) {
    return !jdbc.queryForList("""
        SELECT s.session_id FROM venue_booking_sessions s JOIN venue_bookings b ON b.booking_id=s.booking_id
        WHERE b.batch_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL AND s.starts_at<=?
        LIMIT 1 FOR UPDATE
        """, Long.class, batchId, now).isEmpty();
  }

  private void validateProvider(long provider) {
    if (!access.eligibleProvider(provider)) {
      throw invalid("The training provider is unavailable for venue booking.", "VENUE_PROVIDER_INELIGIBLE");
    }
  }

  private void validateAssignedSessions(Booking booking, List<SessionRequest> sessions) {
    if (booking.requirementId() == null) return;
    var windows = jdbc.query("""
        SELECT CASE WHEN r.center_id IS NULL THEN a.start_date ELSE r.earliest_date END AS start_date,
          CASE WHEN r.center_id IS NULL THEN a.end_date ELSE r.latest_date END AS end_date,
          CASE WHEN r.center_id IS NULL THEN a.daily_start_time ELSE r.daily_start_time END AS start_time,
          CASE WHEN r.center_id IS NULL THEN a.daily_end_time ELSE r.daily_end_time END AS end_time
        FROM training_requirements r JOIN training_requirement_applications a ON a.requirement_id=r.requirement_id
        WHERE r.requirement_id=? AND r.selected_provider_id=? AND a.provider_id=? AND a.application_status='SELECTED'
        """, (rs, row) -> new TrainingWindow(rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
            rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class)),
        booking.requirementId(), booking.providerId(), booking.providerId());
    TrainingWindow window = windows.stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("TrainingRequirement", "id"));
    for (SessionRequest session : sessions) {
      LocalDate day = session.startsAt().toLocalDate();
      if (!day.equals(session.endsAt().toLocalDate()) || day.isBefore(window.startDate()) || day.isAfter(window.endDate())
          || session.startsAt().toLocalTime().isBefore(window.startTime()) || session.endsAt().toLocalTime().isAfter(window.endTime())) {
        throw invalid("Book sessions within the assigned training dates and times.", "TRAINING_BOOKING_OUTSIDE_WINDOW");
      }
    }
  }

  private record TrainingWindow(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {}

  private void releaseLegacyPendingBooking(Booking pending) {
    if (pending.requirementId() != null && pending.replacesBookingId() == null) {
      jdbc.update("""
          UPDATE training_requirements SET root_booking_id=NULL
          WHERE requirement_id=? AND root_booking_id=? AND center_id IS NULL
          """, pending.requirementId(), pending.id());
    }
  }

  private static void validateSeats(int seats, int capacity) {
    if (seats > capacity) {
      throw invalid("The requested batch size exceeds the center capacity.", "VENUE_CAPACITY_EXCEEDED");
    }
  }

  private void ensureBatchCodeAvailable(String code) {
    if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM training_batches WHERE batch_code=?)",
        Boolean.class, code))) {
      throw conflict("Batch code already exists", "BATCH_CODE_ALREADY_EXISTS");
    }
  }

  private TrainingTenantScopeSupport.CallerScope readScope() {
    return TrainingWorkflowAccess.hasAuthority("venue.booking.request") ? providerScope() : reviewerScope();
  }

  private TrainingTenantScopeSupport.CallerScope providerScope() {
    return access.providerScope();
  }

  private TrainingTenantScopeSupport.CallerScope reviewerScope() {
    return access.reviewerScope();
  }

  private static void requireReadAccess(TrainingTenantScopeSupport.CallerScope scope, Booking booking) {
    if (scope.isProviderScoped()) {
      if (scope.trainingProviderId() != booking.providerId()) {
        throw new AccessDeniedException("This booking belongs to another training provider");
      }
    } else {
      requireJurisdiction(scope, booking.stateId(), booking.districtId());
    }
  }

  private static void requireJurisdiction(TrainingTenantScopeSupport.CallerScope scope, long state, long district) {
    TrainingTenantScopeSupport.validateCenterOwnership(scope, null, state, district);
  }

  private User actor() {
    return access.actor();
  }

  private long insert(String sql, Object... values) {
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.update(connection -> {
      var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < values.length; i++) {
        statement.setObject(i + 1, values[i]);
      }
      return statement;
    }, key);
    return Objects.requireNonNull(key.getKey(), "Insert did not return an identity").longValue();
  }

  private void auditBooking(String action, long id, String oldStatus, String status, String note) {
    Map<String, Object> after = new LinkedHashMap<>();
    after.put("status", status);
    if (note != null) {
      after.put("reason", note);
    }
    audits.recordStateChange(action, "VenueBooking", id, "Venue booking status recorded",
        oldStatus == null ? null : Map.of("status", oldStatus), after, List.of());
  }

  private static Long nullableLong(ResultSet rs, String column) throws SQLException {
    Number value = (Number) rs.getObject(column);
    return value == null ? null : value.longValue();
  }

  private static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, LocalDateTime.class);
  }

  private static String text(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static LocalDateTime now() {
    return LocalDateTime.now(TRAINING_ZONE).withNano(0);
  }

  private static ConflictException unavailable() {
    return conflict("The selected venue is unavailable for booking.", "VENUE_UNAVAILABLE");
  }

  private static ConflictException noFutureSessions() {
    return conflict("Only future sessions can be rescheduled.", "VENUE_NO_FUTURE_SESSIONS");
  }

  private static ConflictException conflict(String message, String code) {
    return new ConflictException(message, code);
  }

  private static BadRequestException invalid(String message, String code) {
    return new BadRequestException(message, code);
  }
}
