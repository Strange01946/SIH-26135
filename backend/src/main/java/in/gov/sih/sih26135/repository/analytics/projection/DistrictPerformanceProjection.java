package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection for District Outcome Performance (formal query Q06).
 */
public record DistrictPerformanceProjection(
    Long districtId,
    String districtCode,
    String districtName,
    Long stateId,
    String stateCode,
    String stateName,
    Number enrollmentCount,
    Number traineeCount,
    Number completedCount,
    Number certifiedCount,
    Number joinedPlacementCount,
    Number employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
