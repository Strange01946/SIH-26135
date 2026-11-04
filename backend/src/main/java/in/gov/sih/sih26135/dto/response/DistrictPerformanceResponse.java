package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response record for District Outcome Performance (formal query Q06).
 * Follows Phase 27.2 conventions: Long for count metrics, BigDecimal (scale 2) for percentage metrics.
 */
public record DistrictPerformanceResponse(
    Long districtId,
    String districtCode,
    String districtName,
    Long stateId,
    String stateCode,
    String stateName,
    Long enrollmentCount,
    Long traineeCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    Integer rank
) {}
