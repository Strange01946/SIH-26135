package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;

/**
 * Strategy interface for rule-specific automated anomaly detection.
 */
public interface DataQualityDetector {

  /**
   * The unique rule code this detector implements (e.g. DQ-TRAINEE-001).
   */
  String getSupportedRuleCode();

  /**
   * Executes the anomaly detection scan for the given rule against the domain dataset,
   * bounded by the scan request filters and caller tenancy scope.
   *
   * @param rule data quality rule definition
   * @param request client scan request filters
   * @param scopeContext resolved caller tenancy boundary
   * @return execution summary with evaluated, violation, created, and skipped metrics
   */
  DataQualityRuleExecutionSummary executeScan(
      DataQualityRule rule,
      DataQualityScanRequest request,
      ScanScopeContext scopeContext
  );
}
