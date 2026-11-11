package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;

import java.util.List;

/**
 * Service managing automated data quality scans and domain anomaly detection.
 */
public interface DataQualityEngineService {

  /**
   * Executes an automated data quality scan according to client request filters and caller tenancy boundaries.
   *
   * @param request scan filter criteria and scope parameters
   * @return list of DataQualityRuleExecutionSummary objects detailing rule execution metrics
   */
  List<DataQualityRuleExecutionSummary> executeScan(DataQualityScanRequest request);
}
