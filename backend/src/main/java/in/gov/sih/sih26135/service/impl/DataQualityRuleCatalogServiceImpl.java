package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.exception.DataQualityRuleNotFoundException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.DataQualityRuleRepository;
import in.gov.sih.sih26135.repository.RefDataQualityCategoryRepository;
import in.gov.sih.sih26135.repository.RefDataQualitySeverityRepository;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Production implementation of DataQualityRuleCatalogService.
 *
 * <p>Guarantees idempotent provisioning of the 5 core domain data quality rules:
 * <ul>
 *   <li>DQ-TRAINEE-001 (MISSING): Missing trainee contact information</li>
 *   <li>DQ-PLACEMENT-002 (INCONSISTENT): Placement/employment temporal inconsistency</li>
 *   <li>DQ-EMPLOYMENT-003 (INVALID): Invalid or suspicious salary</li>
 *   <li>DQ-ATTENDANCE-004 (INCONSISTENT): Attendance outside training batch dates</li>
 *   <li>DQ-DUPLICATE-005 (DUPLICATE): Possible duplicate trainee registrations across centers</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DataQualityRuleCatalogServiceImpl implements DataQualityRuleCatalogService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityRuleCatalogServiceImpl.class);

  public static final Long LIFECYCLE_STATUS_ACTIVE = 1L;
  public static final Long LIFECYCLE_STATUS_INACTIVE = 2L;

  public static final String RULE_TRAINEE_001 = "DQ-TRAINEE-001";
  public static final String RULE_PLACEMENT_002 = "DQ-PLACEMENT-002";
  public static final String RULE_EMPLOYMENT_003 = "DQ-EMPLOYMENT-003";
  public static final String RULE_ATTENDANCE_004 = "DQ-ATTENDANCE-004";
  public static final String RULE_DUPLICATE_005 = "DQ-DUPLICATE-005";

  private final DataQualityRuleRepository dataQualityRuleRepository;
  private final RefDataQualityCategoryRepository refDataQualityCategoryRepository;
  private final RefDataQualitySeverityRepository refDataQualitySeverityRepository;

  public DataQualityRuleCatalogServiceImpl(
      DataQualityRuleRepository dataQualityRuleRepository,
      RefDataQualityCategoryRepository refDataQualityCategoryRepository,
      RefDataQualitySeverityRepository refDataQualitySeverityRepository) {
    this.dataQualityRuleRepository = dataQualityRuleRepository;
    this.refDataQualityCategoryRepository = refDataQualityCategoryRepository;
    this.refDataQualitySeverityRepository = refDataQualitySeverityRepository;
  }

  @PostConstruct
  @Transactional
  public void init() {
    try {
      ensureDefaultRulesProvisioned();
    } catch (Exception e) {
      log.warn("Deferred default rule provisioning during init: {}", e.getMessage());
    }
  }

  @Override
  @Transactional
  public void ensureDefaultRulesProvisioned() {
    provisionRuleIfMissing(
        RULE_TRAINEE_001,
        "Missing Trainee Contact Information",
        "Detects trainee records missing required contact information (phone number or email address).",
        "Trainee",
        "MISSING",
        "MEDIUM"
    );

    provisionRuleIfMissing(
        RULE_PLACEMENT_002,
        "Placement Temporal Incoherence",
        "Detects chronological inconsistencies in placement and employment records such as offer dates preceding enrollment or joining dates preceding offers.",
        "PlacementRecord",
        "INCONSISTENT",
        "HIGH"
    );

    provisionRuleIfMissing(
        RULE_EMPLOYMENT_003,
        "Invalid or Suspicious Salary",
        "Detects employment records with non-positive salaries, salaries below applicable statutory minimum wage benchmarks, or beyond realistic operational boundaries.",
        "EmploymentRecord",
        "INVALID",
        "HIGH"
    );

    provisionRuleIfMissing(
        RULE_ATTENDANCE_004,
        "Attendance Outside Batch Operational Boundaries",
        "Detects attendance session records recorded prior to batch start date or after batch scheduled end date.",
        "AttendanceRecord",
        "INCONSISTENT",
        "MEDIUM"
    );

    provisionRuleIfMissing(
        RULE_DUPLICATE_005,
        "Duplicate Trainee Registration Across Centers",
        "Identifies candidate duplicate trainee registrations across distinct training centers based on matching deterministic identity signals.",
        "Trainee",
        "DUPLICATE",
        "HIGH"
    );
  }

  private void provisionRuleIfMissing(
      String ruleCode,
      String ruleName,
      String description,
      String targetEntityType,
      String categoryCode,
      String severityCode) {

    Optional<DataQualityRule> existing = dataQualityRuleRepository.findByRuleCode(ruleCode);
    if (existing.isPresent()) {
      // Idempotency: Preserve existing configuration; never overwrite or re-enable disabled rules!
      return;
    }

    RefDataQualityCategory category = refDataQualityCategoryRepository.findByCategoryCode(categoryCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "categoryCode: " + categoryCode));

    RefDataQualitySeverity severity = refDataQualitySeverityRepository.findBySeverityCode(severityCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "severityCode: " + severityCode));

    LocalDateTime now = LocalDateTime.now();
    DataQualityRule rule = new DataQualityRule(
        ruleCode,
        ruleName,
        targetEntityType,
        category,
        severity,
        LIFECYCLE_STATUS_ACTIVE
    );
    rule.setDescription(description);
    rule.setCreatedAt(now);
    rule.setUpdatedAt(now);

    dataQualityRuleRepository.save(rule);
    log.info("Provisioned default data quality rule: {}", ruleCode);
  }

  @Override
  public DataQualityRule resolveRuleByCode(String ruleCode) {
    if (ruleCode == null || ruleCode.isBlank()) {
      throw new DataQualityRuleNotFoundException("Data quality rule code cannot be null or blank");
    }
    return dataQualityRuleRepository.findByRuleCode(ruleCode.trim())
        .orElseThrow(() -> new DataQualityRuleNotFoundException(ruleCode.trim(), true));
  }

  @Override
  public List<DataQualityRule> getEnabledRules() {
    return dataQualityRuleRepository.findByLifecycleStatusId(LIFECYCLE_STATUS_ACTIVE);
  }

  @Override
  public List<DataQualityRule> resolveRulesForScan(DataQualityScanRequest request) {
    if (request == null) {
      return getEnabledRules();
    }

    // 1. If explicit rule code requested:
    if (request.getRuleCode() != null && !request.getRuleCode().isBlank()) {
      DataQualityRule rule = resolveRuleByCode(request.getRuleCode().trim());
      if (!LIFECYCLE_STATUS_ACTIVE.equals(rule.getLifecycleStatusId())) {
        return List.of(); // Disabled rule
      }
      return List.of(rule);
    }

    // 2. If explicit rule ID requested:
    if (request.getRuleId() != null) {
      DataQualityRule rule = dataQualityRuleRepository.findById(request.getRuleId())
          .orElseThrow(() -> new DataQualityRuleNotFoundException(request.getRuleId()));
      if (!LIFECYCLE_STATUS_ACTIVE.equals(rule.getLifecycleStatusId())) {
        return List.of();
      }
      return List.of(rule);
    }

    // 3. Otherwise filter active rules by target entity type, category, and severity if provided
    List<DataQualityRule> candidates = getEnabledRules();
    List<DataQualityRule> filtered = new ArrayList<>();

    for (DataQualityRule rule : candidates) {
      if (request.getTargetEntityType() != null && !request.getTargetEntityType().isBlank()) {
        if (!rule.getTargetEntityType().equalsIgnoreCase(request.getTargetEntityType().trim())) {
          continue;
        }
      }
      if (request.getCategoryId() != null) {
        if (rule.getDataQualityCategory() == null || !request.getCategoryId().equals(rule.getDataQualityCategory().getId())) {
          continue;
        }
      }
      if (request.getSeverityId() != null) {
        if (rule.getDataQualitySeverity() == null || !request.getSeverityId().equals(rule.getDataQualitySeverity().getId())) {
          continue;
        }
      }
      filtered.add(rule);
    }

    return filtered;
  }
}
