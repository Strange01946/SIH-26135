package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anomaly detector for rule DQ-TRAINEE-001 (MISSING).
 *
 * <p>Identifies trainee records with missing, empty, or whitespace-only contact information (phone number or email).
 */
@Component
public class TraineeContactQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(TraineeContactQualityDetector.class);
  public static final String RULE_CODE = "DQ-TRAINEE-001";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRegistrationService registrationService;
  private final DataQualityEngineProperties properties;

  public TraineeContactQualityDetector(
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
          "SELECT t.trainee_id, t.phone, t.email, t.state_id FROM trainees t WHERE t.deleted_at IS NULL AND t.trainee_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // 1. Tenancy scope filters
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin() && scopeContext.stateId() != null) {
          sql.append("AND t.state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isTrainingProvider() && scopeContext.trainingProviderId() != null) {
          sql.append("AND t.trainee_id IN (SELECT te.trainee_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        } else if (scopeContext.isEmployer() && scopeContext.employerId() != null) {
          sql.append("AND t.trainee_id IN (SELECT pr.trainee_id FROM placement_records pr WHERE pr.employer_id = ?) ");
          params.add(scopeContext.employerId());
        }
      }

      // 2. Specific entity filter if provided in request
      if (request != null && request.getEntityId() != null) {
        sql.append("AND t.trainee_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY t.trainee_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long traineeId = ((Number) row.get("trainee_id")).longValue();
        lastSeenId = traineeId;

        String phone = row.get("phone") != null ? ((String) row.get("phone")).trim() : "";
        String email = row.get("email") != null ? ((String) row.get("email")).trim() : "";

        boolean phoneMissing = phone.isEmpty();
        boolean emailMissing = email.isEmpty();

        if (phoneMissing && emailMissing) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "Trainee",
              traineeId,
              "Trainee record is missing both contact phone number and email address"
          );
          if (created) {
            newIssuesCreated++;
          } else {
            deduplicatedSkipped++;
          }
        } else if (phoneMissing) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "Trainee",
              traineeId,
              "Trainee record is missing required contact phone number"
          );
          if (created) {
            newIssuesCreated++;
          } else {
            deduplicatedSkipped++;
          }
        } else if (emailMissing) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "Trainee",
              traineeId,
              "Trainee record is missing required contact email address"
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
        "Trainee",
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
