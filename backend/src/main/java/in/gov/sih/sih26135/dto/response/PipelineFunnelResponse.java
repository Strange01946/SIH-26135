package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing training pipeline conversion metrics (Q02).
 * Captures stage counts as {@link Long} and conversion percentages as {@link BigDecimal}
 * with scale 2 and {@link java.math.RoundingMode#HALF_UP} rounding.
 */
public record PipelineFunnelResponse(
    Long enrollmentCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
