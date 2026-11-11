package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detector for Rule DQ-ATTENDANCE-004: Attendance Outside Batch Operational Boundaries.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Detects attendance session dates strictly prior to batch start date.</li>
 *   <li>Detects attendance session dates strictly after batch end date (when end date is present).</li>
 *   <li>Inclusive boundaries: Attendance on batch start date or batch end date is valid.</li>
 *   <li>Rejects employer access with 403 Forbidden since employers do not have attendance tenancy.</li>
 *   <li>Enforces non-null state and training provider tenant restrictions.</li>
 *   <li>Distinguishes registration lock failures from deduplication and preserves partial progress on error.</li>
 * </ul>
 */
@Component
public class BatchAttendanceBoundsQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(BatchAttendanceBoundsQualityDetector.class);
  public static final String RULE_CODE = "DQ-ATTENDANCE-004";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityEngineProperties properties;
  private final DataQualityIssueRegistrationService registrationService;

  public BatchAttendanceBoundsQualityDetector(
      JdbcTemplate jdbcTemplate,
      DataQualityEngineProperties properties,
      DataQualityIssueRegistrationService registrationService) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
    this.registrationService = registrationService;
  }

  @Override
  public String getSupportedRuleCode() {
    return RULE_CODE;
  }

  @Override
  public DataQualityRuleExecutionSummary executeScan(
      DataQualityRule rule,
      DataQualityScanRequest request,
      ScanScopeContext scopeContext) {

    long startTime = System.currentTimeMillis();
    LocalDateTime executedAt = LocalDateTime.now();
    long evaluatedCount = 0;
    long violationCount = 0;
    long newIssuesCreated = 0;
    long deduplicatedSkipped = 0;
    boolean hasFailure = false;

    int chunkSize = properties.getBatchChunkSize();
    long lastSeenId = 0;
    boolean hasMore = true;

    while (hasMore) {
      StringBuilder sql = new StringBuilder(
          "SELECT ar.attendance_id, ar.session_date, tb.batch_id, tb.start_date, tb.end_date, tb.center_id, ar.enrollment_id " +
              "FROM attendance_records ar " +
              "JOIN training_batches tb ON ar.batch_id = tb.batch_id " +
              "WHERE ar.attendance_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters - strictly enforced with non-null checks
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isEmployer()) {
          throw new AccessDeniedException("Employers are not authorized to scan training batch attendance records");
        } else if (scopeContext.isStateAdmin()) {
          if (scopeContext.stateId() == null) {
            throw new AccessDeniedException("State administrator scope identifier cannot be resolved");
          }
          sql.append("AND tb.center_id IN (SELECT tc.center_id FROM training_centers tc WHERE tc.state_id = ?) ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider()) {
          if (scopeContext.trainingProviderId() == null) {
            throw new AccessDeniedException("Training provider scope identifier cannot be resolved");
          }
          sql.append("AND ar.enrollment_id IN (SELECT te.enrollment_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        } else {
          throw new AccessDeniedException("Caller persona is not authorized to scan attendance records");
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND ar.attendance_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY ar.attendance_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows;
      try {
        rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      } catch (DataAccessException dae) {
        log.error("Failed to query attendance chunk for rule {}: {}", RULE_CODE, dae.getMessage(), dae);
        hasFailure = true;
        break;
      }

      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        Long attendanceId = ((Number) row.get("attendance_id")).longValue();
        lastSeenId = attendanceId;

        Object sessionDateObj = row.get("session_date");
        Object startDateObj = row.get("start_date");
        Object endDateObj = row.get("end_date");

        if (sessionDateObj == null || startDateObj == null) {
          evaluatedCount++;
          continue;
        }

        LocalDate sessionDate = ((Date) sessionDateObj).toLocalDate();
        LocalDate startDate = ((Date) startDateObj).toLocalDate();
        LocalDate endDate = endDateObj != null ? ((Date) endDateObj).toLocalDate() : null;

        boolean isViolation = false;
        String summary = null;

        // 1. Attendance session prior to batch start date (inclusive boundary: sessionDate == startDate is valid)
        if (sessionDate.isBefore(startDate)) {
          isViolation = true;
          summary = "Attendance session date (" + sessionDate + ") precedes batch scheduled start date (" + startDate + ")";
        }
        // 2. Attendance session after batch end date (inclusive boundary: sessionDate == endDate is valid)
        else if (endDate != null && sessionDate.isAfter(endDate)) {
          isViolation = true;
          summary = "Attendance session date (" + sessionDate + ") occurs after batch scheduled end date (" + endDate + ")";
        }

        evaluatedCount++;

        if (isViolation) {
          violationCount++;
          RegistrationResult result = registrationService.registerOrUpdateIssue(
              rule,
              "AttendanceRecord",
              attendanceId,
              summary
          );
          if (result.isCreated()) {
            newIssuesCreated++;
          } else if (result.isUpdated() || result.isDeduplicated()) {
            deduplicatedSkipped++;
          } else if (result.isLockFailed()) {
            log.warn("Lock acquisition failed during issue registration for AttendanceRecord {}: {}", attendanceId, result.message());
            hasFailure = true;
            break;
          } else if (result.isDatabaseError()) {
            log.error("Database error during issue registration for AttendanceRecord {}: {}", attendanceId, result.message());
            hasFailure = true;
            break;
          }
        }
      }

      if (hasFailure) {
        break;
      }

      if (rows.size() < chunkSize || (request != null && request.getEntityId() != null)) {
        hasMore = false;
      }
    }

    long durationMs = System.currentTimeMillis() - startTime;
    String finalStatus = hasFailure ? "FAILED" : "SUCCESS";
    return new DataQualityRuleExecutionSummary(
        rule.getId(),
        RULE_CODE,
        "AttendanceRecord",
        evaluatedCount,
        violationCount,
        newIssuesCreated,
        deduplicatedSkipped,
        executedAt,
        durationMs,
        finalStatus
    );
  }
}
