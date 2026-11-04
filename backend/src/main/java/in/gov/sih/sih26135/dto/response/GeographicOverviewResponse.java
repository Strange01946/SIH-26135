package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Standardized consolidated response for National Geographic Overview (GET /api/v1/dashboards/geography).
 * Contains national totals, state outcome summaries, district rankings (Q06), and regional disparity analysis.
 */
public record GeographicOverviewResponse(
    Long totalStates,
    Long totalDistricts,
    Long totalEnrollments,
    Long totalCompleted,
    Long totalCertified,
    Long totalJoinedPlacement,
    Long totalEmployed,
    BigDecimal overallCompletionRatePct,
    BigDecimal overallCertificationRatePct,
    BigDecimal overallPlacementRatePct,
    BigDecimal overallEmploymentRatePct,
    List<StatePerformanceResponse> stateSummaries,
    LeaderboardResponse<DistrictPerformanceResponse> districtRankings,
    RegionalDisparityResponse regionalDisparity
) {}
