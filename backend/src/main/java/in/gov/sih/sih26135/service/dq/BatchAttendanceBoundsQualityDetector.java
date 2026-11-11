package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anomaly detector for rule DQ-ATTENDANCE-004 (INCONSISTENT).
 *
 * <p>Identifies attendance records recorded outside their associated training batch's operational date bounds:
 * <ul>
 *   <li>Attendance session date occurring strictly prior to batch start date.</li>
 *   <li>Attendance session date occurring strictly after batch end date.</li>
 *   <li>Boundary days (start date, end date) are strictly valid (inclusive).</li>
 * </ul>
 */
@Component
public class BatchAttendanceBoundsQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(BatchAttendanceBoundsQualityDetector.class);
  public static final String RULE_CODE = "DQ-ATTENDANCE-004";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRegistrationService registrationService;
  private final DataQualityEngineProperties properties;

  public BatchAttendanceBoundsQualityDetector(
      JdbcTemplate jdbcTemplate,
      DataQualityIssueRegistrationService registrationService,
      DataQualityEngineProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.registrationService = registrationService;
    this.properties = properties;
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

      // Tenancy scope filters
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin() && scopeContext.stateId() != null) {
          sql.append("AND tb.center_id IN (SELECT tc.center_id FROM training_centers tc WHERE tc.state_id = ?) ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider() && scopeContext.trainingProviderId() != null) {
          sql.append("AND ar.enrollment_id IN (SELECT te.enrollment_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND ar.attendance_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY ar.attendance_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long attendanceId = ((Number) row.get("attendance_id")).longValue();
        lastSeenId = attendanceId;

        LocalDate sessionDate = row.get("session_date") != null ? ((Date) row.get("session_date")).toLocalDate() : null;
        LocalDate startDate = row.get("start_date") != null ? ((Date) row.get("start_date")).toLocalDate() : null;
        LocalDate endDate = row.get("end_date") != null ? ((Date) row.get("end_date")).toLocalDate() : null;

        if (sessionDate == null) {
          continue;
        }

        String anomalySummary = null;

        // Condition 1: Session date strictly before batch start date (inclusive boundary: sessionDate == startDate is valid)
        if (startDate != null && sessionDate.isBefore(startDate)) {
          anomalySummary = "Attendance session date (" + sessionDate +
              ") is recorded prior to batch start date (" + startDate + ") for attendance ID " + attendanceId;
        }

        // Condition 2: Session date strictly after batch end date (inclusive boundary: sessionDate == endDate is valid)
        if (endDate != null && sessionDate.isAfter(endDate)) {
          if (anomalySummary != null) {
            anomalySummary += "; Also after batch end date";
          } else {
            anomalySummary = "Attendance session date (" + sessionDate +
                ") is recorded after batch end date (" + endDate + ") for attendance ID " + attendanceId;
          }
        }

        if (anomalySummary != null) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "AttendanceRecord",
              attendanceId,
              anomalySummary
          );
          if (created) {
            newIssuesCreated++;
          } else {
            deduplicatedSkipped++;
          }
        }
      }

      if (rows.size() < chunkSize || (request != null && request.getEntityId() != null)) {
        hasMore = false;
      }
    }

    long durationMs = System.currentTimeMillis() - startTime;
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
        "SUCCESS"
    );
  }
}
