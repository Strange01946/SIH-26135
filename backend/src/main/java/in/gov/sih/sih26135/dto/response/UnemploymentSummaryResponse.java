package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing aggregate unemployment metrics and duration KPIs (formal Query Q15).
 */
public record UnemploymentSummaryResponse(
    Long unemploymentPeriodCount,
    Long traineeCount,
    Long currentUnemploymentCount,
    Long reemployedCount,
    Long neverPrecededByEmploymentCount,
    BigDecimal avgDurationDays,
    BigDecimal avgOpenPeriodDurationDays,
    BigDecimal reemploymentRatePct
) {}
