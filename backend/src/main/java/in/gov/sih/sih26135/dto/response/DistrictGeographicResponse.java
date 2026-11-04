package in.gov.sih.sih26135.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Standardized response for District-specific Geographic & Outcome Analytics (GET /api/v1/dashboards/districts/{districtId}).
 */
public record DistrictGeographicResponse(
    Long districtId,
    String districtCode,
    String districtName,
    Long stateId,
    String stateCode,
    String stateName,
    DistrictPerformanceResponse performance,
    Long trainingCenterCount,
    List<TrainingCenterSummaryResponse> trainingCenters,
    Integer nationalRank,
    Integer stateRank,
    Map<String, Object> regionalIndicators
) {}
