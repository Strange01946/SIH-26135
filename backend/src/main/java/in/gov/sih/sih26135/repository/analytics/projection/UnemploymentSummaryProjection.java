package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection record representing aggregate unemployment metrics (formal Query Q15)
 * from {@code vw_unemployment_fact}.
 */
public record UnemploymentSummaryProjection(
    Long unemploymentPeriodCount,
    Long traineeCount,
    Long currentUnemploymentCount,
    Long reemployedCount,
    Long neverPrecededByEmploymentCount,
    BigDecimal avgDurationDays,
    BigDecimal avgOpenPeriodDurationDays
) {}
