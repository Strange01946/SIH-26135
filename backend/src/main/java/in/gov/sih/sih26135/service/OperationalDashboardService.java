package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.OperationalDashboardFilter;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse;
import in.gov.sih.sih26135.dto.response.OperationalImportMetricsResponse;
import in.gov.sih.sih26135.dto.response.OperationalSystemHealthResponse;

/**
 * Read-only operational dashboard service aggregating data quality scorecards,
 * bulk ingestion metrics, and system health / audit mutation telemetry under strict
 * caller tenancy and RBAC guardrails.
 */
public interface OperationalDashboardService {

  OperationalDataQualityScorecardResponse getDataQualityScorecard(OperationalDashboardFilter filter);

  OperationalImportMetricsResponse getImportMetrics(OperationalDashboardFilter filter);

  OperationalSystemHealthResponse getSystemHealth(OperationalDashboardFilter filter);
}
