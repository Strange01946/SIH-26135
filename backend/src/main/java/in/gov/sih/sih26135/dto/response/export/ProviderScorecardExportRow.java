package in.gov.sih.sih26135.dto.response.export;

import java.math.BigDecimal;

/**
 * Represents a single tabular/JSON row in the Training Provider Performance Scorecard MIS export.
 * Reflects institutional performance, outcomes, capacity, and rankings.
 */
public record ProviderScorecardExportRow(
    Long providerId,
    String providerCode,
    String providerName,
    String stateName,
    String districtName,
    Long activeBatchCapacity,
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
