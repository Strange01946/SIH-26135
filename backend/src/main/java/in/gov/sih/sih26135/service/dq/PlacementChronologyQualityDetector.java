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
 * Anomaly detector for rule DQ-PLACEMENT-002 (INCONSISTENT).
 *
 * <p>Identifies temporal incoherence in placement records:
 * <ul>
 *   <li>Offer date preceding training enrollment date.</li>
 *   <li>Actual joining date preceding offer date.</li>
 * </ul>
 */
@Component
public class PlacementChronologyQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(PlacementChronologyQualityDetector.class);
  public static final String RULE_CODE = "DQ-PLACEMENT-002";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRegistrationService registrationService;
  private final DataQualityEngineProperties properties;

  public PlacementChronologyQualityDetector(
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
          "SELECT pr.placement_id, pr.offer_date, pr.actual_joining_date, te.enrollment_date, " +
              "pr.work_state_id, pr.employer_id, pr.provider_id " +
              "FROM placement_records pr " +
              "JOIN training_enrollments te ON pr.enrollment_id = te.enrollment_id " +
              "WHERE pr.deleted_at IS NULL AND pr.placement_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin() && scopeContext.stateId() != null) {
          sql.append("AND pr.work_state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider() && scopeContext.trainingProviderId() != null) {
          sql.append("AND pr.provider_id = ? ");
          params.add(scopeContext.trainingProviderId());
        } else if (scopeContext.isEmployer() && scopeContext.employerId() != null) {
          sql.append("AND pr.employer_id = ? ");
          params.add(scopeContext.employerId());
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND pr.placement_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY pr.placement_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long placementId = ((Number) row.get("placement_id")).longValue();
        lastSeenId = placementId;

        LocalDate offerDate = row.get("offer_date") != null ? ((Date) row.get("offer_date")).toLocalDate() : null;
        LocalDate joiningDate = row.get("actual_joining_date") != null ? ((Date) row.get("actual_joining_date")).toLocalDate() : null;
        LocalDate enrollmentDate = row.get("enrollment_date") != null ? ((Date) row.get("enrollment_date")).toLocalDate() : null;

        String anomalySummary = null;

        // Condition 1: Offer date earlier than enrollment date
        if (offerDate != null && enrollmentDate != null && offerDate.isBefore(enrollmentDate)) {
          anomalySummary = "Placement offer date precedes training enrollment date for placement ID " + placementId;
        }

        // Condition 2: Joining date earlier than offer date
        if (joiningDate != null && offerDate != null && joiningDate.isBefore(offerDate)) {
          if (anomalySummary != null) {
            anomalySummary += "; Joining date also precedes offer date";
          } else {
            anomalySummary = "Employment joining date precedes offer date for placement ID " + placementId;
          }
        }

        if (anomalySummary != null) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "PlacementRecord",
              placementId,
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
        "PlacementRecord",
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
