package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection for Training Provider Performance Leaderboard (formal query Q04).
 * Captures provider institutional outcome metrics, distinct trainee volume, and active batch capacity.
 */
public record ProviderPerformanceProjection(
    Long providerId,
    String providerCode,
    String providerName,
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
    Number activeBatchCapacity
) {}
