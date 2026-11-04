package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Standardized response for State-specific Geographic & Outcome Analytics (GET /api/v1/dashboards/states/{stateId}).
 */
public record StateGeographicResponse(
    Long stateId,
    String stateCode,
    String stateName,
    StatePerformanceResponse stateSummary,
    List<DistrictPerformanceResponse> districts,
    LeaderboardResponse<DistrictPerformanceResponse> districtRankings,
    Long totalTrainingCenters,
    RegionalDisparityResponse stateDisparity
) {}
