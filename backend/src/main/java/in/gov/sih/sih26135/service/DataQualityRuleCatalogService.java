package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.entity.DataQualityRule;

import java.util.List;

/**
 * Service managing the Data Quality Rule Catalog and idempotent provisioning of domain rules.
 */
public interface DataQualityRuleCatalogService {

  /**
   * Stably provisions the 5 standard domain data quality rules idempotently.
   * If a rule already exists, its configuration is strictly preserved (does not overwrite or re-enable disabled rules).
   */
  void ensureDefaultRulesProvisioned();

  /**
   * Resolves a data quality rule by its unique business code.
   *
   * @param ruleCode unique business rule code (e.g., DQ-TRAINEE-001)
   * @return resolved DataQualityRule entity
   */
  DataQualityRule resolveRuleByCode(String ruleCode);

  /**
   * Retrieves all currently active and enabled data quality rules.
   *
   * @return list of enabled DataQualityRule entities
   */
  List<DataQualityRule> getEnabledRules();

  /**
   * Resolves the list of candidate rules to evaluate based on an incoming scan request.
   *
   * @param request scan filter request
   * @return list of matching, enabled DataQualityRule entities
   */
  List<DataQualityRule> resolveRulesForScan(DataQualityScanRequest request);
}
