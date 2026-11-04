package in.gov.sih.sih26135.dto.response;

/**
 * Standardized aggregate response DTO for the Executive and National Overview Dashboards.
 * Combines the consolidated KPI strip (Q01), training pipeline conversion funnel (Q02),
 * and scheme & program comparative performance matrix (Q03).
 */
public record ExecutiveDashboardResponse(
    DashboardKpiStripResponse kpiStrip,
    PipelineFunnelResponse pipelineFunnel,
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeProgramMatrix
) {

  /**
   * Compact constructor ensuring non-null component references.
   */
  public ExecutiveDashboardResponse {
    if (schemeProgramMatrix == null) {
      schemeProgramMatrix = LeaderboardResponse.of("employmentRatePct", null);
    }
  }
}
