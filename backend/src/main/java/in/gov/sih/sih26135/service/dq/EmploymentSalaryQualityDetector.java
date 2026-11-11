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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detector for Rule DQ-EMPLOYMENT-003: Invalid or Suspicious Salary.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Non-positive salary detection (salary &le; 0).</li>
 *   <li>Salary frequency normalization (MONTHLY, ANNUAL, YEARLY, WEEKLY, DAILY, HOURLY).</li>
 *   <li>Approved hourly conversion assumption: 8 hours/day &times; 26 working days/month = 208 hours/month.</li>
 *   <li>Safely skips monthly benchmark comparisons for unsupported frequency codes without generating false legal violations.</li>
 *   <li>Does not invent statutory minimum wages; uses authoritative configuration properties or safely skips.</li>
 *   <li>Flags salaries exceeding realistic operational boundaries (maximumMonthlySalaryThreshold).</li>
 *   <li>Enforces strict, non-null tenant boundaries for state administrators, providers, and employers.</li>
 * </ul>
 */
@Component
public class EmploymentSalaryQualityDetector implements DataQualityDetector {

  private static final Logger log = LoggerFactory.getLogger(EmploymentSalaryQualityDetector.class);
  public static final String RULE_CODE = "DQ-EMPLOYMENT-003";

  private final JdbcTemplate jdbcTemplate;
  private final DataQualityEngineProperties properties;
  private final DataQualityIssueRegistrationService registrationService;

  public EmploymentSalaryQualityDetector(
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
          "SELECT er.employment_id, er.trainee_id, er.employer_id, er.starting_salary, " +
              "er.work_state_id, sf.frequency_code " +
              "FROM employment_records er " +
              "LEFT JOIN ref_salary_frequency sf ON er.salary_frequency_id = sf.salary_frequency_id " +
              "WHERE er.deleted_at IS NULL AND er.employment_id > ? "
      );
      List<Object> params = new ArrayList<>();
      params.add(lastSeenId);

      // Tenancy scope filters - strictly enforced with non-null checks
      if (!scopeContext.canScanNationwide()) {
        if (scopeContext.isStateAdmin()) {
          if (scopeContext.stateId() == null) {
            throw new AccessDeniedException("State administrator scope identifier cannot be resolved");
          }
          sql.append("AND er.work_state_id = ? ");
          params.add(scopeContext.stateId());
        } else if (scopeContext.isEmployer()) {
          if (scopeContext.employerId() == null) {
            throw new AccessDeniedException("Employer scope identifier cannot be resolved");
          }
          sql.append("AND er.employer_id = ? ");
          params.add(scopeContext.employerId());
        } else if (scopeContext.isTrainingProvider()) {
          if (scopeContext.trainingProviderId() == null) {
            throw new AccessDeniedException("Training provider scope identifier cannot be resolved");
          }
          sql.append("AND er.enrollment_id IN (SELECT te.enrollment_id FROM training_enrollments te WHERE te.provider_id = ?) ");
          params.add(scopeContext.trainingProviderId());
        } else {
          throw new AccessDeniedException("Caller persona is not authorized to scan employment records");
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

        Object salaryObj = row.get("starting_salary");
        if (salaryObj == null) {
          continue; // Missing salary is evaluated by missing data rules if required
        }

        BigDecimal salary = (salaryObj instanceof BigDecimal bd)
            ? bd
            : BigDecimal.valueOf(((Number) salaryObj).doubleValue());

        String frequencyCode = (String) row.get("frequency_code");
        Long workStateId = row.get("work_state_id") != null ? ((Number) row.get("work_state_id")).longValue() : null;

        // Determine if there is an authoritative statutory minimum wage for this jurisdiction
        BigDecimal minWageThreshold = resolveStateMinimumWage(workStateId);

        boolean isViolation = false;
        String summary = null;

        // 1. Non-positive salary violation (applies regardless of pay frequency)
        if (salary.compareTo(BigDecimal.ZERO) <= 0) {
          isViolation = true;
          summary = "Employment record contains invalid non-positive salary amount: " + salary;
        } else {
          // 2. Frequency normalization
          BigDecimal normalizedMonthly = normalizeToMonthly(salary, frequencyCode);
          if (normalizedMonthly == null) {
            // Unsupported or unconvertible frequency code: safely skip monthly comparisons without generating false legal violations
            log.debug("Skipping monthly benchmark comparison for employment record {} due to unsupported frequency code '{}'",
                employmentId, frequencyCode);
          } else {
            // 3. Realistic upper operational boundary check
            if (properties.getMaximumMonthlySalaryThreshold() != null &&
                normalizedMonthly.compareTo(properties.getMaximumMonthlySalaryThreshold()) > 0) {
              isViolation = true;
              summary = "Employment record normalized monthly salary (" + normalizedMonthly +
                  ") exceeds maximum operational threshold (" + properties.getMaximumMonthlySalaryThreshold() + ")";
            }
            // 4. Statutory minimum wage benchmark check (only when authoritative reference exists)
            else if (minWageThreshold != null && normalizedMonthly.compareTo(minWageThreshold) < 0) {
              isViolation = true;
              summary = "Employment record normalized monthly salary (" + normalizedMonthly +
                  ") is below applicable statutory minimum wage benchmark (" + minWageThreshold + ")";
            }
          }
        }

        if (isViolation) {
          violationCount++;
          boolean created = registrationService.registerIssueIfAbsent(
              rule,
              "EmploymentRecord",
              employmentId,
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
   *
   * <p>Conversion assumptions:
   * <ul>
   *   <li>MONTHLY: 1:1</li>
   *   <li>ANNUAL / YEARLY: Divided by 12</li>
   *   <li>WEEKLY: Multiplied by 52, divided by 12</li>
   *   <li>DAILY: Multiplied by 26 working days per month</li>
   *   <li>HOURLY: Multiplied by 8 hours/day &times; 26 working days = 208 hours per month</li>
   *   <li>Unsupported / null / blank: Returns {@code null} to skip monthly comparisons safely.</li>
   * </ul>
   */
  public static BigDecimal normalizeToMonthly(BigDecimal salary, String frequencyCode) {
    if (salary == null) {
      return BigDecimal.ZERO;
    }
    if (frequencyCode == null || frequencyCode.isBlank()) {
      return null;
    }
    return switch (frequencyCode.trim().toUpperCase()) {
      case "MONTHLY" -> salary.setScale(2, RoundingMode.HALF_UP);
      case "ANNUAL", "YEARLY" -> salary.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
      case "WEEKLY" -> salary.multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
      case "DAILY" -> salary.multiply(BigDecimal.valueOf(26)).setScale(2, RoundingMode.HALF_UP);
      case "HOURLY" -> salary.multiply(BigDecimal.valueOf(208)).setScale(2, RoundingMode.HALF_UP);
      default -> null; // Unsupported frequency code: do not interpret as MONTHLY
    };
  }

  private BigDecimal resolveStateMinimumWage(Long stateId) {
    if (stateId == null || properties == null || properties.getStateMinimumWages() == null) {
      return null;
    }
    return properties.getStateMinimumWages().get(stateId.toString());
  }
}
