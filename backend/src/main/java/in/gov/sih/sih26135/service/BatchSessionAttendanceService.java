package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.venue.BatchSessionAttendance.*;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BatchSessionAttendanceService {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
  private static final Set<String> MARKABLE_BATCH_STATUSES = Set.of("ONGOING", "COMPLETED");
  private static final Set<String> NEW_MARKS = Set.of("PRESENT", "ABSENT");
  static final String SCHEDULE = """
      SELECT s.session_id,s.starts_at,s.ends_at,v.batch_id,
             ROW_NUMBER() OVER (PARTITION BY v.batch_id,DATE(s.starts_at) ORDER BY s.starts_at,s.session_id) sequence_number
      FROM venue_booking_sessions s JOIN venue_bookings v USING (booking_id)
      WHERE s.reserved_at IS NOT NULL AND s.released_at IS NULL
      """;

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final AuditLogService audit;

  public BatchSessionAttendanceService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, AuditLogService audit) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.audit = audit;
  }

  public AttendanceTimetable timetable(long batchId) {
    var batch = ownBatch(batchId, false);
    var now = LocalDateTime.now(INDIA);
    return new AttendanceTimetable(batch.code(), batch.title(), batch.status(), batch.attendanceLocked(),
        sessions(batchId).stream().map(slot -> slot.view(batch, now)).toList());
  }

  public AttendanceSessionRoster roster(long batchId, long sessionId) {
    var batch = ownBatch(batchId, false);
    var slot = slot(batchId, sessionId);
    return new AttendanceSessionRoster(slot.view(batch, LocalDateTime.now(INDIA)), rows(batch, slot, false));
  }

  public Map<Long, ScheduledTimes> recordedTimes(long traineeId) {
    return jdbc.query("WITH slots AS (" + SCHEDULE + """
        AND v.batch_id IN (SELECT DISTINCT batch_id FROM attendance_records WHERE trainee_id=?))
        SELECT a.attendance_id,slots.starts_at,slots.ends_at FROM attendance_records a
        JOIN slots ON slots.batch_id=a.batch_id AND DATE(slots.starts_at)=a.session_date
          AND slots.sequence_number=a.session_sequence
        WHERE a.trainee_id=?
        """, (rs, index) -> Map.entry(rs.getLong(1),
        new ScheduledTimes(rs.getObject(2, LocalDateTime.class), rs.getObject(3, LocalDateTime.class))), traineeId, traineeId)
        .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public record ScheduledTimes(LocalDateTime startsAt, LocalDateTime endsAt) {}

  public void requireAttendanceEditable(long batchId) {
    if (Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM batch_assessment_publications WHERE batch_id=?)
        """, Boolean.class, batchId))) {
      throw attendanceLocked();
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public AttendanceSessionRoster save(long batchId, long sessionId, SaveAttendanceRoster request) {
    // Venue approval, rescheduling and the legacy attendance writers take the same batch lock.
    var batch = ownBatch(batchId, true);
    requireAttendanceEditable(batch.id());
    var slot = slot(batchId, sessionId);
    var now = LocalDateTime.now(INDIA);
    var session = slot.view(batch, now);
    if (!session.canMark()) {
      throw new ConflictException("Attendance is available once the training batch and this session have started.",
          "ATTENDANCE_SESSION_NOT_OPEN");
    }

    var current = rows(batch, slot, true).stream().collect(Collectors.toMap(AttendanceRosterRow::enrollmentId, row -> row));
    var seen = new HashSet<Long>();
    var changes = new ArrayList<SessionAttendanceMark>();
    for (var mark : request.marks()) {
      if (!seen.add(mark.enrollmentId())) {
        throw new BadRequestException("Submit a trainee only once per session.", "DUPLICATE_ATTENDANCE_MARK");
      }
      var saved = current.get(mark.enrollmentId());
      if (saved == null || !saved.eligible()) {
        throw new BadRequestException("An enrollment is outside this session's eligible roster.", "INVALID_ATTENDANCE_ENROLLMENT");
      }
      if (!NEW_MARKS.contains(mark.status()) && !mark.status().equals(saved.status())) {
        throw new BadRequestException("Choose Present or Absent.", "INVALID_ATTENDANCE_MARK");
      }
      String remarks = mark.remarks() == null || mark.remarks().isBlank() ? null : mark.remarks().strip();
      if (Objects.equals(saved.status(), mark.status()) && Objects.equals(saved.remarks(), remarks)) continue;
      if (!Objects.equals(saved.version(), mark.expectedVersion())) {
        throw new ConflictException("Attendance changed after you opened this session. Reload the saved marks before editing.",
            "ATTENDANCE_CHANGED");
      }
      changes.add(new SessionAttendanceMark(mark.enrollmentId(), mark.status(), remarks, mark.expectedVersion()));
    }

    if (!changes.isEmpty()) {
      writeMarks(slot, now, batch.actorId(), changes);
      audit.recordStateChange("UPDATE", "TrainingBatch", batchId, "Session attendance saved",
          null, Map.of("sessionId", sessionId, "marksSaved", changes.size()), List.of());
    }
    return new AttendanceSessionRoster(session, rows(batch, slot, false));
  }

  private void writeMarks(Slot slot, LocalDateTime now, long actorId, List<SessionAttendanceMark> changes) {
    var start = slot.startsAt().toLocalTime();
    // Legacy time-only columns cannot represent an overnight end. Full times come from the schedule.
    var end = slot.startsAt().toLocalDate().equals(slot.endsAt().toLocalDate()) ? slot.endsAt().toLocalTime() : null;
    jdbc.batchUpdate("""
        INSERT INTO attendance_records(enrollment_id,trainee_id,batch_id,session_date,session_sequence,
          session_start_time,session_end_time,attendance_status_id,marked_at,marked_by_user_id,remarks,created_at,updated_at)
        SELECT e.enrollment_id,e.trainee_id,e.batch_id,?,?,?,?,s.attendance_status_id,?,?,?,?,?
        FROM training_enrollments e JOIN ref_attendance_status s ON s.status_code=? WHERE e.enrollment_id=?
        ON DUPLICATE KEY UPDATE session_start_time=VALUES(session_start_time),session_end_time=VALUES(session_end_time),
          attendance_status_id=VALUES(attendance_status_id),marked_at=VALUES(marked_at),
          marked_by_user_id=VALUES(marked_by_user_id),remarks=VALUES(remarks),updated_at=VALUES(updated_at)
        """, changes.stream().map(mark -> new Object[]{
            slot.startsAt().toLocalDate(), slot.sequence(), start, end, now, actorId, mark.remarks(), now, now,
            mark.status(), mark.enrollmentId()
        }).toList());
  }

  private List<AttendanceRosterRow> rows(Batch batch, Slot slot, boolean lock) {
    var day = slot.startsAt().toLocalDate();
    return jdbc.query("""
        SELECT e.enrollment_id,t.registration_number,
          CONCAT_WS(' ',t.first_name,NULLIF(t.middle_name,''),NULLIF(t.last_name,'')) full_name,
          (e.deleted_at IS NULL AND t.deleted_at IS NULL AND e.enrollment_date<=?
            AND (e.actual_completion_date IS NULL OR e.actual_completion_date>=?)
            AND es.status_code IN ('ENROLLED','IN_PROGRESS','COMPLETED')) eligible,
          a.attendance_id,a.remarks,a.updated_at,a.marked_by_user_id,ast.status_code
        FROM training_enrollments e JOIN trainees t USING (trainee_id)
        JOIN ref_enrollment_status es USING (enrollment_status_id)
        LEFT JOIN attendance_records a ON a.enrollment_id=e.enrollment_id AND a.session_date=? AND a.session_sequence=?
        LEFT JOIN ref_attendance_status ast ON ast.attendance_status_id=a.attendance_status_id
        WHERE e.batch_id=? AND e.provider_id=?
        HAVING eligible OR attendance_id IS NOT NULL
        ORDER BY full_name,t.registration_number
        """ + (lock ? " FOR UPDATE OF e" : ""), (rs, index) -> {
      var id = rs.getObject("attendance_id", Long.class);
      var remarks = rs.getString("remarks");
      var status = rs.getString("status_code");
      String version = id == null ? null : UUID.nameUUIDFromBytes((id + "|" + status + "|" + rs.getString("updated_at")
          + "|" + rs.getString("marked_by_user_id") + "|" + Objects.toString(remarks, "")).getBytes(StandardCharsets.UTF_8)).toString();
      return new AttendanceRosterRow(rs.getLong("enrollment_id"), rs.getString("full_name"),
          rs.getString("registration_number"), rs.getBoolean("eligible"), status, remarks, version);
    }, day, day, day, slot.sequence(), batch.id(), batch.providerId());
  }

  private List<Slot> sessions(long batchId) {
    return jdbc.query(SCHEDULE + " AND v.batch_id=? ORDER BY s.starts_at,s.session_id", (rs, index) ->
        new Slot(rs.getLong(1), rs.getObject(2, LocalDateTime.class), rs.getObject(3, LocalDateTime.class),
            rs.getInt("sequence_number")), batchId);
  }

  private Slot slot(long batchId, long sessionId) {
    return sessions(batchId).stream().filter(slot -> slot.id() == sessionId).findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("This session is unavailable for the selected batch."));
  }

  private Batch ownBatch(long batchId, boolean lock) {
    var user = scope.resolveAuthenticatedUser().filter(account -> account.getTrainingProviderId() != null)
        .orElseThrow(() -> new AccessDeniedException("A training provider assignment is required."));
    return jdbc.query("""
        SELECT b.batch_code,c.course_name,s.status_code,
          EXISTS(SELECT 1 FROM batch_assessment_publications pub WHERE pub.batch_id=b.batch_id) locked
        FROM training_batches b
        JOIN courses c USING (course_id) JOIN ref_batch_status s USING (batch_status_id)
        WHERE b.batch_id=? AND b.provider_id=? AND b.deleted_at IS NULL
        """ + (lock ? " FOR UPDATE OF b" : ""), (rs, index) -> new Batch(batchId, user.getTrainingProviderId(), user.getId(),
        rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)), batchId, user.getTrainingProviderId())
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("This training batch is unavailable."));
  }

  private record Batch(long id, long providerId, long actorId, String code, String title, String status, boolean attendanceLocked) {}

  private static ConflictException attendanceLocked() {
    return new ConflictException("Training attendance is locked after the batch is opened for assessment.", "ASSESSMENT_ATTENDANCE_LOCKED");
  }

  private record Slot(long id, LocalDateTime startsAt, LocalDateTime endsAt, int sequence) {
    AttendanceSession view(Batch batch, LocalDateTime now) {
      boolean started = !startsAt.isAfter(now);
      return new AttendanceSession(id, startsAt, endsAt, started, !batch.attendanceLocked() && started && MARKABLE_BATCH_STATUSES.contains(batch.status()));
    }
  }
}
