package in.gov.sih.sih26135.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.dto.assessor.BatchAssessment.*;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BatchAssessmentService {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
  private static final String BATCH_INFO = """
      FROM training_batches b JOIN ref_batch_status bs USING (batch_status_id)
      JOIN courses c USING (course_id) JOIN training_providers p ON p.provider_id=b.provider_id
      LEFT JOIN training_centers tc ON tc.center_id=b.center_id
      LEFT JOIN districts d ON d.district_id=tc.district_id LEFT JOIN states st ON st.state_id=tc.state_id
      LEFT JOIN venue_bookings v ON v.booking_id=(SELECT MAX(vb.booking_id) FROM venue_bookings vb
        WHERE vb.batch_id=b.batch_id AND vb.booking_status IN ('APPROVED','COMPLETED'))
      LEFT JOIN venue_booking_training_plans plan ON plan.booking_id=v.booking_id
      LEFT JOIN training_requirements r ON r.requirement_id=plan.requirement_id
      """;
  private static final String PUBLICATION = """
      SELECT a.assessment_id,b.batch_id,b.batch_code,COALESCE(r.title,c.course_name) title,
        COALESCE(plan.skills_to_teach,c.description,c.course_name) skills,p.provider_name,tc.center_name,d.district_name,st.state_name,
        pub.starts_at,pub.venue,pub.brief,a.evaluator_name,a.evaluator_user_id,a.lifecycle_status_id,
        pub.checked_in_at,pub.completed_at,(SELECT COUNT(*) FROM trainee_assessments x WHERE x.assessment_id=a.assessment_id) candidates,
        EXISTS(SELECT 1 FROM assessment_results x JOIN trainee_assessments ta USING (trainee_assessment_id)
          WHERE ta.assessment_id=a.assessment_id) scored
      """ + BATCH_INFO + """
      JOIN batch_assessment_publications pub ON pub.batch_id=b.batch_id
      JOIN assessments a ON a.assessment_id=pub.assessment_id
      WHERE b.deleted_at IS NULL AND a.deleted_at IS NULL
      """;
  private static final String ELIGIBLE = """
      FROM training_enrollments e JOIN trainees t USING (trainee_id)
      WHERE e.batch_id=? AND e.deleted_at IS NULL AND t.deleted_at IS NULL
        AND e.enrollment_status_id NOT IN (4,7)
      """;
  private static final String SESSION_MARK = """
      ON DATE(slot.starts_at)>=e.enrollment_date
        AND (e.actual_completion_date IS NULL OR DATE(slot.starts_at)<=e.actual_completion_date)
      LEFT JOIN attendance_records ar ON ar.enrollment_id=e.enrollment_id AND ar.session_date=DATE(slot.starts_at)
        AND ar.session_sequence=slot.sequence_number
      LEFT JOIN ref_attendance_status s USING (attendance_status_id)
      """;

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final AuditLogService audit;
  private final ObjectMapper mapper;
  private final TraineeConsentService consent;

  public BatchAssessmentService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, AuditLogService audit, ObjectMapper mapper,
      TraineeConsentService consent) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.audit = audit;
    this.mapper = mapper;
    this.consent = consent;
  }

  public Readiness readiness(long batchId) {
    return readiness(ownBatch(batchId, false));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Publication publish(long batchId, PublishBatchAssessmentRequest input) {
    var batch = ownBatch(batchId, true);
    var readiness = readiness(batch);
    if (readiness.publication() != null) return readiness.publication();
    if (!readiness.canPublish()) throw conflict(readiness.blocker(), "ASSESSMENT_BATCH_NOT_READY");
    var keys = new GeneratedKeyHolder();
    int inserted = jdbc.update(connection -> {
      PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO assessments(assessment_code,assessment_name,assessment_type_id,course_id,batch_id,
            program_id,maximum_score,pass_score,lifecycle_status_id)
          SELECT ?,?,assessment_type_id,?,?,?,100,50,1 FROM ref_assessment_type WHERE type_code='SUMMATIVE'
          """, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, "BATCH-EXIT-" + batchId);
      statement.setString(2, batch.title());
      statement.setLong(3, batch.courseId());
      statement.setLong(4, batchId);
      statement.setLong(5, batch.programId());
      return statement;
    }, keys);
    if (inserted != 1 || keys.getKey() == null) throw new IllegalStateException("Summative assessment reference is missing.");
    long id = keys.getKey().longValue();
    jdbc.update("""
        INSERT INTO batch_assessment_publications(assessment_id,batch_id,brief,published_by)
        VALUES (?,?,?,?)
        """, id, batchId, input.brief().strip(), batch.actorId());
    int candidates = jdbc.update("""
        INSERT INTO trainee_assessments(assessment_id,enrollment_id,trainee_id,attempt_number,appeared_flag)
        SELECT ?,e.enrollment_id,e.trainee_id,1,0
        """ + ELIGIBLE, id, batchId);
    if (candidates != readiness.candidates()) throw conflict("The batch roster changed. Reload before publishing.", "ASSESSMENT_ROSTER_CHANGED");
    audit.recordStateChange("CREATE", "Assessment", id, "Batch opened for assessment", null,
        Map.of("batchId", batchId, "candidateCount", candidates), List.of());
    return publication(id).view();
  }

  public List<Publication> openAssessments() {
    return jdbc.query(PUBLICATION + " AND a.lifecycle_status_id=1 AND a.evaluator_user_id IS NULL ORDER BY a.assessment_id",
        (rs, row) -> publicationRow(rs).view());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Publication claim(long id, ClaimBatchAssessmentRequest input) {
    var caller = scope.resolveAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Assessor account is required."));
    var publication = publication(id, true);
    if (publication.assessorId() != null && publication.assessorId().equals(caller.getId())) return publication.view();
    if (!"OPEN".equals(publication.view().status())) throw conflict("Another assessor has already claimed this batch.", "ASSESSMENT_ALREADY_CLAIMED");
    var startsAt = input.startsAt();
    if (startsAt.isBefore(now().withSecond(0).withNano(0))) {
      throw new BadRequestException("Choose an assessment time from now onward (IST).", "ASSESSMENT_TIME_IN_PAST");
    }
    var date = startsAt.toLocalDate();
    jdbc.update("UPDATE assessments SET evaluator_user_id=?,evaluator_name=?,assessment_date=? WHERE assessment_id=?",
        caller.getId(), caller.getUsername(), date, id);
    jdbc.update("UPDATE trainee_assessments SET evaluator_user_id=?,evaluator_name=?,assessment_date=? WHERE assessment_id=?",
        caller.getId(), caller.getUsername(), date, id);
    String venue = input.venue().strip();
    jdbc.update("UPDATE batch_assessment_publications SET starts_at=?,venue=? WHERE assessment_id=?", startsAt, venue, id);
    audit.recordStateChange("UPDATE", "Assessment", id, "Assessor claimed and scheduled batch", null,
        Map.of("assessorUserId", caller.getId(), "startsAt", startsAt.toString(), "venue", venue), List.of());
    return publication(id).view();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Publication complete(long id) {
    var state = publication(id, true);
    requireAssessor(state);
    var view = state.view();
    if (view.completedAt() != null) return view;
    if (!view.canComplete()) {
      throw conflict("Complete the assessment after its scheduled start.",
          "ASSESSMENT_COMPLETION_NOT_READY");
    }
    var completedAt = now();
    if (!view.checkedIn()) {
      jdbc.update("WITH slots AS (" + BatchSessionAttendanceService.SCHEDULE + """
          AND v.batch_id=?), present AS (
            SELECT DISTINCT e.enrollment_id FROM training_enrollments e JOIN slots slot
          """ + SESSION_MARK + """
            WHERE e.batch_id=? AND s.status_code='PRESENT'
          )
          UPDATE trainee_assessments ta LEFT JOIN present p USING (enrollment_id)
          SET ta.appeared_flag=(p.enrollment_id IS NOT NULL) WHERE ta.assessment_id=?
          """, view.batchId(), view.batchId(), id);
    }
    jdbc.update("UPDATE batch_assessment_publications SET completed_at=?,checked_in_at=COALESCE(checked_in_at,?) WHERE assessment_id=?",
        completedAt, completedAt, id);
    audit.recordStateChange("UPDATE", "Assessment", id, "Assessor completed batch assessment", null,
        Map.of("completedAt", completedAt.toString()), List.of());
    return publication(id).view();
  }

  public Review providerReview(long batchId) {
    ownBatch(batchId, false);
    var state = publicationForBatch(batchId);
    if (state == null) throw missing();
    return new Review(state.view(), candidates(state));
  }

  public Review assessorReview(long id) {
    var state = publication(id);
    if (!"OPEN".equals(state.view().status())) requireAssessor(state);
    return new Review(state.view(), candidates(state));
  }

  public List<Appointment> ownAppointments() {
    long traineeId = scope.resolveAuthenticatedTrainee().filter(trainee -> trainee.getDeletedAt() == null)
        .orElseThrow(() -> new AccessDeniedException("A trainee profile is required.")).getId();
    return jdbc.query("""
        SELECT b.batch_code,COALESCE(r.title,c.course_name) title,pub.starts_at,pub.venue,pub.brief,pub.completed_at,
          a.evaluator_name,a.evaluator_user_id,a.lifecycle_status_id,ar.assessment_result_id,ao.outcome_code,
          ar.remarks,ar.obtained_score,
          (SELECT MIN(certification_id) FROM certifications cert
            WHERE cert.assessment_result_id=ar.assessment_result_id AND cert.deleted_at IS NULL) certificate_id
        """ + BATCH_INFO + """
        JOIN batch_assessment_publications pub ON pub.batch_id=b.batch_id
        JOIN assessments a ON a.assessment_id=pub.assessment_id
        JOIN trainee_assessments ta ON ta.assessment_id=a.assessment_id
        LEFT JOIN assessment_results ar ON ar.trainee_assessment_id=ta.trainee_assessment_id AND ar.result_declared_at IS NOT NULL
        LEFT JOIN ref_assessment_outcome ao USING (assessment_outcome_id)
        WHERE b.deleted_at IS NULL AND a.deleted_at IS NULL AND ta.trainee_id=?
        ORDER BY pub.starts_at DESC,a.assessment_id DESC
        """, (rs, row) -> {
      var scores = AssessmentDocuments.componentScores(mapper, rs.getString("remarks"));
      String status = rs.getLong("lifecycle_status_id") == 2 ? "FINALIZED"
          : rs.getObject("evaluator_user_id") == null ? "OPEN" : "CLAIMED";
      return new Appointment(rs.getString("batch_code"), rs.getString("title"), rs.getObject("starts_at", LocalDateTime.class),
          rs.getString("venue"), rs.getString("brief"), rs.getString("evaluator_name"), status, rs.getObject("completed_at", LocalDateTime.class),
          rs.getObject("assessment_result_id", Long.class), rs.getString("outcome_code"), scores.getTheory(),
          scores.getPractical(), scores.getViva(), rs.getBigDecimal("obtained_score"), rs.getObject("certificate_id", Long.class));
    }, traineeId);
  }

  public boolean managed(long assessmentId) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM batch_assessment_publications WHERE assessment_id=?)", Boolean.class, assessmentId));
  }

  public void requireLegacyAssessment(long assessmentId) {
    if (managed(assessmentId)) {
      throw conflict("Use the published batch assessment workflow to make changes.", "MANAGED_BATCH_ASSESSMENT");
    }
  }

  public Set<Long> hiddenResults() {
    var caller = scope.resolveAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("An account is required."));
    boolean national = scope.isNationwideAdmin();
    return new HashSet<>(jdbc.queryForList("""
        SELECT ar.assessment_result_id FROM assessment_results ar
        JOIN trainee_assessments ta USING (trainee_assessment_id) JOIN batch_assessment_publications pub USING (assessment_id)
        JOIN assessments a USING (assessment_id) JOIN training_batches b ON b.batch_id=pub.batch_id
        JOIN trainees t ON t.trainee_id=ta.trainee_id
        WHERE ar.result_declared_at IS NULL OR NOT (? OR t.user_id <=> ? OR a.evaluator_user_id <=> ?
          OR b.provider_id <=> ?)
        """, Long.class, national, caller.getId(), caller.getId(), caller.getTrainingProviderId()));
  }

  public boolean checkedIn(long assessmentId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM batch_assessment_publications WHERE assessment_id=? AND checked_in_at IS NOT NULL)
        """, Boolean.class, assessmentId));
  }

  public Map<Long, Boolean> scoringRoster(long id) {
    var state = publication(id);
    requireAssessor(state);
    if (state.view().completedAt() == null) {
      throw conflict("Mark the assessment completed before entering marks or publishing results.", "ASSESSMENT_COMPLETION_REQUIRED");
    }
    return jdbc.query("SELECT trainee_id,appeared_flag FROM trainee_assessments WHERE assessment_id=?",
        (rs, row) -> Map.entry(rs.getLong(1), rs.getBoolean(2)), id).stream()
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public boolean attendanceMeetsThreshold(long batchId, long enrollmentId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("WITH slots AS (" + BatchSessionAttendanceService.SCHEDULE + """
        AND v.batch_id=?) SELECT COUNT(*)>0 AND SUM(s.status_code='PRESENT')*10>=COUNT(*)*7
        FROM training_enrollments e JOIN slots slot
        """ + SESSION_MARK + " WHERE e.enrollment_id=?", Boolean.class, batchId, enrollmentId));
  }

  @Transactional
  public void awardTrainingSkills(long assessmentId) {
    var state = publication(assessmentId);
    var earned = candidates(state).stream()
        .filter(row -> "PASS".equals(row.outcome()) && row.present() > 0)
        .filter(row -> consent.hasActiveConsent(row.traineeId(), "DATA_PROCESSING")).toList();
    if (earned.isEmpty()) return;

    var skills = jdbc.queryForList("""
        SELECT s.skill_id FROM assessments a JOIN course_skills cs USING (course_id)
        JOIN skills s USING (skill_id) WHERE a.assessment_id=? AND s.deleted_at IS NULL AND s.lifecycle_status_id=1
        ORDER BY s.skill_id
        """, Long.class, assessmentId);
    if (skills.isEmpty() && state.view().skills() != null) {
      var labels = java.util.Arrays.stream(state.view().skills().split("[,;\\r\\n]+"))
          .map(label -> java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip())
          .filter(label -> !label.isBlank() && label.codePointCount(0, label.length()) <= 150)
          .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
      if (!labels.isEmpty()) {
        jdbc.update("""
            INSERT INTO skill_categories(category_code,category_name,lifecycle_status_id) VALUES ('TRAINING-TOPICS','Training topics',1)
            ON DUPLICATE KEY UPDATE category_code=skill_categories.category_code
            """);
      }
      for (String label : labels) {
        jdbc.update("""
            INSERT INTO skills(skill_code,skill_name,skill_category_id,lifecycle_status_id)
            SELECT CONCAT('TRN-',LEFT(SHA2(LOWER(?),256),28)),?,skill_category_id,1
              FROM skill_categories WHERE category_code='TRAINING-TOPICS'
            ON DUPLICATE KEY UPDATE skill_code=skills.skill_code
            """, label, label);
        skills.addAll(jdbc.queryForList("""
            SELECT skill_id FROM skills WHERE skill_name=? AND deleted_at IS NULL AND lifecycle_status_id=1
            """, Long.class, label));
      }
    }
    if (skills.isEmpty()) return;
    var uniqueSkills = new java.util.LinkedHashSet<>(skills);
    var scoreRows = jdbc.queryForList("""
        SELECT ar.trainee_id, CASE WHEN ar.score_percentage>=90 THEN 5 WHEN ar.score_percentage>=80 THEN 4
          WHEN ar.score_percentage>=65 THEN 3 ELSE 2 END AS skill_rank
        FROM assessment_results ar JOIN trainee_assessments ta USING (trainee_assessment_id)
        WHERE ta.assessment_id=? AND ar.result_declared_at IS NOT NULL
        """, assessmentId);
    var ranks = new java.util.HashMap<Long, Integer>();
    for (var row : scoreRows) ranks.put(((Number) row.get("trainee_id")).longValue(), ((Number) row.get("skill_rank")).intValue());
    for (var candidate : earned) {
      for (long skillId : uniqueSkills) {
        jdbc.update("""
            INSERT INTO trainee_skill_evidence(assessment_id,trainee_id,skill_id,skill_level_id,assessed_by)
            SELECT ?,?,?,skill_level_id,? FROM skill_levels WHERE level_rank=?
            ON DUPLICATE KEY UPDATE skill_level_id=VALUES(skill_level_id),assessed_by=VALUES(assessed_by)
            """, assessmentId, candidate.traineeId(), skillId, state.assessorId(), ranks.get(candidate.traineeId()));
      }
    }
  }

  private List<Candidate> candidates(State state) {
    var marks = trainingSessionMarks(state.view());
    return jdbc.query("""
        SELECT t.trainee_id,CONCAT_WS(' ',t.first_name,NULLIF(t.middle_name,''),NULLIF(t.last_name,'')) full_name,
          t.registration_number,ta.appeared_flag,ar.assessment_result_id,ao.outcome_code,ar.obtained_score,ar.remarks,
          (SELECT MIN(c.certification_id) FROM certifications c WHERE c.assessment_result_id=ar.assessment_result_id
             AND c.deleted_at IS NULL) certificate_id
        FROM trainee_assessments ta JOIN trainees t USING (trainee_id)
        LEFT JOIN assessment_results ar ON ar.trainee_assessment_id=ta.trainee_assessment_id AND ar.result_declared_at IS NOT NULL
        LEFT JOIN ref_assessment_outcome ao USING (assessment_outcome_id)
        WHERE ta.assessment_id=? ORDER BY full_name,t.registration_number
        """, (rs, row) -> {
      var scores = AssessmentDocuments.componentScores(mapper, rs.getString("remarks"));
      long traineeId = rs.getLong("trainee_id");
      var sessions = marks.getOrDefault(traineeId, List.of());
      int present = (int) sessions.stream().filter(session -> "PRESENT".equals(session.status())).count();
      return new Candidate(traineeId, rs.getString("full_name"), rs.getString("registration_number"),
          state.view().checkedIn() ? rs.getBoolean("appeared_flag") : null,
          present, sessions.size(), rs.getObject("assessment_result_id", Long.class),
          rs.getString("outcome_code"), scores.getTheory(), scores.getPractical(), scores.getViva(),
          rs.getBigDecimal("obtained_score"), rs.getObject("certificate_id", Long.class), sessions);
    }, state.view().assessmentId());
  }

  private Map<Long, List<TrainingSessionMark>> trainingSessionMarks(Publication publication) {
    return jdbc.query("WITH slots AS (" + BatchSessionAttendanceService.SCHEDULE + """
        AND v.batch_id=?)
        SELECT ta.trainee_id,slot.starts_at,slot.ends_at,s.status_code,ar.remarks
        FROM trainee_assessments ta JOIN training_enrollments e USING (enrollment_id)
        JOIN slots slot
        """ + SESSION_MARK + """
        WHERE ta.assessment_id=? ORDER BY slot.starts_at,slot.session_id
        """, rs -> {
      Map<Long, List<TrainingSessionMark>> marks = new java.util.HashMap<>();
      while (rs.next()) {
        marks.computeIfAbsent(rs.getLong("trainee_id"), key -> new java.util.ArrayList<>())
            .add(new TrainingSessionMark(rs.getObject("starts_at", LocalDateTime.class),
                rs.getObject("ends_at", LocalDateTime.class), rs.getString("status_code"), rs.getString("remarks")));
      }
      return marks;
    }, publication.batchId(), publication.assessmentId());
  }

  private Readiness readiness(Batch batch) {
    var existing = publicationForBatch(batch.id());
    if (existing != null) {
      int sessions = jdbc.queryForObject("SELECT COUNT(*) FROM (" + BatchSessionAttendanceService.SCHEDULE
          + " AND v.batch_id=?) schedule", Integer.class, batch.id());
      return new Readiness(batch.code(), batch.status(), existing.view().candidates(), sessions, 0, false, null, existing.view());
    }
    int enrolled = jdbc.queryForObject("SELECT COUNT(*) " + ELIGIBLE, Integer.class, batch.id());
    String blocker = !"COMPLETED".equals(batch.status()) ? "Complete the training batch first."
        : enrolled == 0 ? "At least one enrolled trainee is required." : null;
    if (blocker != null) return new Readiness(batch.code(), batch.status(), enrolled, 0, 0, false, blocker, null);

    var totals = jdbc.queryForObject("WITH slots AS (" + BatchSessionAttendanceService.SCHEDULE + """
        AND v.batch_id=?), enrolled AS (SELECT e.enrollment_id,e.enrollment_date,e.actual_completion_date
        """ + ELIGIBLE + """
        )
        SELECT (SELECT COUNT(*) FROM slots) slots,
          (SELECT COUNT(*) FROM slots WHERE ends_at>?) future_slots,
          SUM(CASE WHEN slot.session_id IS NOT NULL AND (s.status_code IS NULL OR s.status_code NOT IN ('PRESENT','ABSENT')) THEN 1 ELSE 0 END) missing,
          COUNT(DISTINCT CASE WHEN slot.session_id IS NULL THEN e.enrollment_id END) uncovered
        FROM enrolled e LEFT JOIN slots slot
        """ + SESSION_MARK, (rs, row) -> new Coverage(rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getInt(4)), batch.id(), batch.id(), now());
    blocker = totals.sessions() == 0 ? "The batch needs completed, approved training sessions."
        : totals.future() > 0 ? "Wait until all approved training sessions have ended."
        : totals.uncovered() > 0 ? "Every enrolled trainee needs at least one applicable training session."
        : totals.missing() > 0 ? "Mark every eligible trainee Present or Absent for each training session." : null;
    return new Readiness(batch.code(), batch.status(), enrolled, totals.sessions(), totals.missing(), blocker == null, blocker, null);
  }

  private Batch ownBatch(long batchId, boolean lock) {
    var caller = scope.resolveAuthenticatedUser().filter(user -> user.getTrainingProviderId() != null)
        .orElseThrow(() -> new AccessDeniedException("A training provider account is required."));
    return jdbc.query("""
        SELECT b.batch_id,b.batch_code,bs.status_code,COALESCE(r.title,c.course_name),b.course_id,b.program_id
        """ + BATCH_INFO + " WHERE b.batch_id=? AND b.provider_id=? AND b.deleted_at IS NULL" + (lock ? " FOR UPDATE OF b" : ""),
        (rs, row) -> new Batch(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5),
            rs.getLong(6), caller.getId()), batchId, caller.getTrainingProviderId())
        .stream().findFirst().orElseThrow(BatchAssessmentService::missing);
  }

  private State publicationForBatch(long batchId) {
    return jdbc.query(PUBLICATION + " AND b.batch_id=?", (rs, row) -> publicationRow(rs), batchId).stream().findFirst().orElse(null);
  }

  private State publication(long id) { return publication(id, false); }

  private State publication(long id, boolean lock) {
    return jdbc.query(PUBLICATION + " AND a.assessment_id=?" + (lock ? " FOR UPDATE OF a" : ""),
        (rs, row) -> publicationRow(rs), id).stream().findFirst().orElseThrow(BatchAssessmentService::missing);
  }

  private static State publicationRow(java.sql.ResultSet rs) throws java.sql.SQLException {
    var starts = rs.getObject("starts_at", LocalDateTime.class);
    var completedAt = rs.getObject("completed_at", LocalDateTime.class);
    var assessorId = rs.getObject("evaluator_user_id", Long.class);
    boolean finalized = rs.getLong("lifecycle_status_id") == 2;
    return new State(assessorId, new Publication(rs.getLong("assessment_id"), rs.getLong("batch_id"),
        rs.getString("batch_code"), rs.getString("title"), rs.getString("skills"), rs.getString("provider_name"),
        rs.getString("center_name"), rs.getString("district_name"), rs.getString("state_name"), starts, rs.getString("venue"),
        rs.getString("brief"), rs.getString("evaluator_name"), finalized ? "FINALIZED" : assessorId == null ? "OPEN" : "CLAIMED",
        rs.getInt("candidates"), rs.getObject("checked_in_at") != null, !finalized && assessorId != null
            && starts != null && completedAt == null && !rs.getBoolean("scored") && !starts.isAfter(now()), completedAt));
  }

  private void requireAssessor(State state) {
    if (state.assessorId() == null || !state.assessorId().equals(scope.resolveAuthenticatedUserId())) {
      throw new AccessDeniedException("Only the assessor who claimed this batch can evaluate it.");
    }
  }

  private static LocalDateTime now() { return LocalDateTime.now(INDIA); }
  private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Batch assessment is unavailable."); }
  private static ConflictException conflict(String message, String code) { return new ConflictException(message, code); }
  private record Batch(long id, String code, String status, String title, long courseId, long programId, long actorId) {}
  private record State(Long assessorId, Publication view) {}
  private record Coverage(int sessions, int future, long missing, int uncovered) {}
}
