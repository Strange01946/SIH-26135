package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection for State Outcome Performance and Regional Hierarchy.
 */
public record StatePerformanceProjection(
    Long stateId,
    String stateCode,
    String stateName,
    Number districtCount,
    Number enrollmentCount,
    Number traineeCount,
    Number completedCount,
    Number certifiedCount,
    Number joinedPlacementCount,
    Number employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    Number trainingCenterCount
) {}
