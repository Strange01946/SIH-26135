package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response record for State-level Outcome Aggregation and Performance.
 * Reconciles 100% mathematically with underlying district outcome metrics.
 */
public record StatePerformanceResponse(
    Long stateId,
    String stateCode,
    String stateName,
    Long districtCount,
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
    Long trainingCenterCount,
    Integer rank
) {}
