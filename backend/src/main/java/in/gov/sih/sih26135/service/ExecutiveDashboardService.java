package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;

/**
 * Service interface for the Executive and National Overview Dashboards.
 * Coordinates aggregation and transformation of formal analytical queries Q01, Q02, and Q03.
 */
public interface ExecutiveDashboardService {

  /**
   * Retrieves the executive dashboard KPI strip (Q01).
   *
   * @return populated {@link DashboardKpiStripResponse}
   */
  DashboardKpiStripResponse getDashboardKpiStrip();

  /**
   * Retrieves the training pipeline conversion funnel (Q02).
   *
   * @return populated {@link PipelineFunnelResponse}
   */
  PipelineFunnelResponse getTrainingPipelineFunnel();

  /**
   * Retrieves the scheme & program comparative performance matrix (Q03).
   *
   * @return populated {@link LeaderboardResponse} containing ranked scheme-program outcomes
   */
  LeaderboardResponse<SchemeProgramPerformanceResponse> getSchemeProgramPerformanceMatrix();

  /**
   * Retrieves the consolidated executive overview dashboard combining Q01, Q02, and Q03.
   *
   * @return populated {@link ExecutiveDashboardResponse}
   */
  ExecutiveDashboardResponse getExecutiveDashboard();
}
