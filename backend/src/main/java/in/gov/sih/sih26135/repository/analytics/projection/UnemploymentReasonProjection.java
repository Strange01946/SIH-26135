package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection record representing reasons for unemployment (formal Query Q16)
 * from {@code vw_unemployment_reason_summary} joined with {@code ref_unemployment_reason}.
 */
public record UnemploymentReasonProjection(
    Long unemploymentReasonId,
    String unemploymentReasonCode,
    String reasonName,
    Long periodCount,
    Long traineeCount,
    Long currentPeriodCount,
    Long reemployedCount,
    BigDecimal avgDurationDays
) {}
