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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detector for Rule DQ-TRAINEE-001: Missing Contact Information.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Identifies trainee records missing required contact information (phone number or email).</li>
 *   <li>Treats empty string, whitespace-only, and NULL as missing.</li>
 *   <li>Rejects employer callers with 403 Forbidden since employers lack general trainee tenancy.</li>
 *   <li>Enforces non-null state and training provider tenant restrictions.</li>
 *   <li>Distinguishes registration lock failures from deduplication and preserves partial progress on error.</li>
 * </ul>
 */
@Component
public class TraineeContactQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(TraineeContactQualityDetector.class);
  public static final String RULE_CODE = "DQ-TRAINEE-001";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityEngineProperties properties;
  private final DataQualityIssueRegistrationService registrationService;

  public TraineeContactQualityDetector(
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
          "SELECT t.trainee_id, t.first_name, t.last_name, t.phone, t.email, t.state_id " +
              "FROM trainees t " +
              "WHERE t.deleted_at IS NULL AND t.trainee_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters - strictly enforced with non-null checks
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isEmployer()) {
          throw new AccessDeniedException("Employers are not authorized to scan general trainee contact information");
        } else if (scopeContext.isStateAdmin()) {
          if (scopeContext.stateId() == null) {
            throw new AccessDeniedException("State administrator scope identifier cannot be resolved");
          }
          sql.append("AND t.state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider()) {
          if (scopeContext.trainingProviderId() == null) {
            throw new AccessDeniedException("Training provider scope identifier cannot be resolved");
          }
          sql.append("AND t.trainee_id IN (SELECT te.trainee_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        } else {
          throw new AccessDeniedException("Caller persona is not authorized to scan trainee contact information");
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND t.trainee_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY t.trainee_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows;
      try {
        rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      } catch (DataAccessException dae) {
        log.error("Failed to query trainee chunk for rule {}: {}", RULE_CODE, dae.getMessage(), dae);
        hasFailure = true;
        break;
      }

      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long traineeId = ((Number) row.get("trainee_id")).longValue();
        lastSeenId = traineeId;

        String phone = (String) row.get("phone");
        String email = (String) row.get("email");

        boolean phoneMissing = phone == null || phone.trim().isEmpty();
        boolean emailMissing = email == null || email.trim().isEmpty();

        if (phoneMissing || emailMissing) {
          violationCount++;
          StringBuilder details = new StringBuilder("Trainee is missing contact information: ");
          if (phoneMissing && emailMissing) {
            details.append("both phone number and email address are missing");
          } else if (phoneMissing) {
            details.append("phone number is missing");
          } else {
            details.append("email address is missing");
          }

          RegistrationResult result = registrationService.registerOrUpdateIssue(
              rule,
              "Trainee",
              traineeId,
              details.toString()
          );

          if (result.isCreated()) {
            newIssuesCreated++;
          } else if (result.isUpdated() || result.isDeduplicated()) {
            deduplicatedSkipped++;
          } else if (result.isLockFailed()) {
            log.warn("Lock acquisition failed during issue registration for Trainee {}: {}", traineeId, result.message());
            hasFailure = true;
            break;
          } else if (result.isDatabaseError()) {
            log.error("Database error during issue registration for Trainee {}: {}", traineeId, result.message());
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
        "Trainee",
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
