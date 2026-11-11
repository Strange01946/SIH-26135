package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Detector for Rule DQ-PLACEMENT-002: Placement Temporal Incoherence.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Offer date preceding training enrollment date.</li>
 *   <li>Actual joining date preceding offer date.</li>
 *   <li>Actual joining date preceding training enrollment date.</li>
 *   <li>Safely skips records with missing/null dates without generating false anomalies.</li>
 *   <li>Enforces non-null state, provider, and employer tenancy restrictions.</li>
 * </ul>
 */
@Component
public class PlacementChronologyQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(PlacementChronologyQualityDetector.class);
  public static final String RULE_CODE = "DQ-PLACEMENT-002";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityEngineProperties properties;
  private final DataQualityIssueRegistrationService registrationService;

  public PlacementChronologyQualityDetector(
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

      // Tenancy scope filters - strictly enforced with non-null checks
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin()) {
          if (scopeContext.stateId() == null) {
            throw new AccessDeniedException("State administrator scope identifier cannot be resolved");
          }
          sql.append("AND pr.work_state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider()) {
          if (scopeContext.trainingProviderId() == null) {
            throw new AccessDeniedException("Training provider scope identifier cannot be resolved");
          }
          sql.append("AND pr.provider_id = ? ");
          params.add(scopeContext.trainingProviderId());
        } else if (scopeContext.isEmployer()) {
          if (scopeContext.employerId() == null) {
            throw new AccessDeniedException("Employer scope identifier cannot be resolved");
          }
          sql.append("AND pr.employer_id = ? ");
          params.add(scopeContext.employerId());
        } else {
          throw new AccessDeniedException("Caller persona is not authorized to scan placement records");
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

        Object offerDateObj = row.get("offer_date");
        Object joiningDateObj = row.get("actual_joining_date");
        Object enrollmentDateObj = row.get("enrollment_date");

        LocalDate offerDate = offerDateObj != null ? ((Date) offerDateObj).toLocalDate() : null;
        LocalDate joiningDate = joiningDateObj != null ? ((Date) joiningDateObj).toLocalDate() : null;
        LocalDate enrollmentDate = enrollmentDateObj != null ? ((Date) enrollmentDateObj).toLocalDate() : null;

        boolean isViolation = false;
        String summary = null;

        // Condition 1: Offer date precedes enrollment date
        if (offerDate != null && enrollmentDate != null && offerDate.isBefore(enrollmentDate)) {
          isViolation = true;
          summary = "Placement offer date (" + offerDate + ") precedes candidate enrollment date (" + enrollmentDate + ")";
        }
        // Condition 2: Actual joining date precedes offer date
        else if (joiningDate != null && offerDate != null && joiningDate.isBefore(offerDate)) {
          isViolation = true;
          summary = "Placement joining date (" + joiningDate + ") precedes offer date (" + offerDate + ")";
        }
        // Condition 3: Actual joining date precedes enrollment date
        else if (joiningDate != null && enrollmentDate != null && joiningDate.isBefore(enrollmentDate)) {
          isViolation = true;
          summary = "Placement joining date (" + joiningDate + ") precedes candidate enrollment date (" + enrollmentDate + ")";
        }

        if (isViolation) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "PlacementRecord",
              placementId,
              summary
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
