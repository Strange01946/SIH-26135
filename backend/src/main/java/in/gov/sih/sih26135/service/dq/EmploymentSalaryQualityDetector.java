package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anomaly detector for rule DQ-EMPLOYMENT-003 (INVALID).
 *
 * <p>Validates starting salaries in employment records:
 * <ul>
 *   <li>Flags missing, zero, or negative starting salaries.</li>
 *   <li>Validates against authoritative, configured statutory minimum wage thresholds per jurisdiction.</li>
 *   <li>Safely skips statutory evaluations if no threshold is configured for a jurisdiction (no invented wages).</li>
 *   <li>Flags salaries exceeding configured realistic upper operational boundaries.</li>
 * </ul>
 */
@Component
public class EmploymentSalaryQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(EmploymentSalaryQualityDetector.class);
  public static final String RULE_CODE = "DQ-EMPLOYMENT-003";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityIssueRegistrationService registrationService;
  private final DataQualityEngineProperties properties;

  public EmploymentSalaryQualityDetector(
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
          "SELECT er.employment_id, er.starting_salary, er.salary_frequency_id, " +
              "sf.frequency_code, er.work_state_id, er.employer_id, er.enrollment_id " +
              "FROM employment_records er " +
              "LEFT JOIN ref_salary_frequency sf ON er.salary_frequency_id = sf.salary_frequency_id " +
              "WHERE er.deleted_at IS NULL AND er.employment_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin() && scopeContext.stateId() != null) {
          sql.append("AND er.work_state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isEmployer() && scopeContext.employerId() != null) {
          sql.append("AND er.employer_id = ? ");
          params.add(scopeContext.employerId());
        } else if (scopeContext.isTrainingProvider() && scopeContext.trainingProviderId() != null) {
          sql.append("AND er.enrollment_id IN (SELECT te.enrollment_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        }
      }

      // Entity filter
      if (request != null && request.getEntityId() != null) {
        sql.append("AND er.employment_id = ? ");
        params.add(request.getEntityId());
      }

      sql.append("ORDER BY er.employment_id ASC LIMIT ?");
      params.add(chunkSize);

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
      if (rows.isEmpty()) {
        hasMore = false;
        break;
      }

      for (Map<String, Object> row : rows) {
        evaluatedCount++;
        Long employmentId = ((Number) row.get("employment_id")).longValue();
        lastSeenId = employmentId;

        BigDecimal startingSalary = row.get("starting_salary") != null ? (BigDecimal) row.get("starting_salary") : null;
        String frequencyCode = row.get("frequency_code") != null ? (String) row.get("frequency_code") : "MONTHLY";
        Long stateId = row.get("work_state_id") != null ? ((Number) row.get("work_state_id")).longValue() : null;

        String anomalySummary = null;

        if (startingSalary == null) {
          anomalySummary = "Employment record has missing starting salary for employment ID " + employmentId;
        } else if (startingSalary.compareTo(BigDecimal.ZERO) <= 0) {
          anomalySummary = "Employment record has non-positive starting salary: " + startingSalary + " for employment ID " + employmentId;
        } else {
          // Positive salary: normalize to monthly basis
          BigDecimal monthlySalary = normalizeToMonthly(startingSalary, frequencyCode);

          // 1. Check applicable statutory minimum wage if configured for jurisdiction
          BigDecimal statutoryThreshold = properties.resolveMinimumWageForState(stateId);
          if (statutoryThreshold != null) {
            if (monthlySalary.compareTo(statutoryThreshold) < 0) {
              anomalySummary = "Employment starting salary normalized to " + monthlySalary +
                  " INR/month is below applicable statutory minimum wage threshold of " + statutoryThreshold +
                  " INR/month for employment ID " + employmentId;
            }
          } else {
            // No statutory threshold configured for this jurisdiction: strictly skip without inventing a threshold
            log.debug("Statutory wage evaluation skipped for employment ID {} in state {}: no configured threshold",
                employmentId, stateId);
          }

          // 2. Check realistic upper operational boundary
          BigDecimal maxThreshold = properties.getMaximumMonthlySalaryThreshold();
          if (anomalySummary == null && maxThreshold != null && monthlySalary.compareTo(maxThreshold) > 0) {
            anomalySummary = "Employment starting salary normalized to " + monthlySalary +
                " INR/month exceeds maximum realistic operational threshold for employment ID " + employmentId;
          }
        }

        if (anomalySummary != null) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "EmploymentRecord",
              employmentId,
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
        "EmploymentRecord",
        evaluatedCount,
        violationCount,
        newIssuesCreated,
        deduplicatedSkipped,
        executedAt,
        durationMs,
        "SUCCESS"
    );
  }

  /**
   * Normalizes salary to monthly equivalent according to pay frequency.
   */
  public static BigDecimal normalizeToMonthly(BigDecimal salary, String frequencyCode) {
    if (salary == null) {
      return BigDecimal.ZERO;
    }
    if (frequencyCode == null || "MONTHLY".equalsIgnoreCase(frequencyCode)) {
      return salary;
    }
    return switch (frequencyCode.toUpperCase()) {
      case "ANNUAL" -> salary.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
      case "WEEKLY" -> salary.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
      case "DAILY" -> salary.multiply(BigDecimal.valueOf(26)).setScale(2, RoundingMode.HALF_UP);
      default -> salary;
    };
  }
}
