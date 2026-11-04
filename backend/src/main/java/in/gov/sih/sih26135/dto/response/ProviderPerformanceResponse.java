package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response record for Training Provider Performance Leaderboard (formal query Q04).
 * Follows Phase 27.2 conventions: Long for count metrics, BigDecimal (scale 2) for percentage metrics.
 */
public record ProviderPerformanceResponse(
    Long providerId,
    String providerCode,
    String providerName,
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
    Long activeBatchCapacity,
    Integer rank
) {}
