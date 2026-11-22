package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.service.dq.ScanScopeContext;

import java.util.List;

/**
 * Service managing automated data quality scans and domain anomaly detection.
 */
public interface DataQualityEngineService {

  /**
   * Executes an automated data quality scan according to client request filters and caller tenancy boundaries.
   * Resolves caller tenancy scope automatically from the security context.
   *
   * @param request scan filter criteria and scope parameters
   * @return list of DataQualityRuleExecutionSummary objects detailing rule execution metrics
   */
  List<DataQualityRuleExecutionSummary> executeScan(DataQualityScanRequest request);

  /**
   * Executes an automated data quality scan with an explicit authoritative tenancy scope context.
   * Enforces that non-nationwide callers cannot elevate permissions or scan cross-tenant data.
   *
   * @param request scan filter criteria and scope parameters
   * @param explicitScopeContext explicit tenancy and caller scope context
   * @return list of DataQualityRuleExecutionSummary objects detailing rule execution metrics
   */
  List<DataQualityRuleExecutionSummary> executeScopedScan(DataQualityScanRequest request, ScanScopeContext explicitScopeContext);
}
