package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing root causes and reasons for unemployment (formal Query Q16).
 * Includes period counts, affected trainee counts, ongoing counts, re-employment conversion, and duration KPIs.
 */
public record UnemploymentReasonBreakdownResponse(
    Long unemploymentReasonId,
    String unemploymentReasonCode,
    String reasonName,
    Long periodCount,
    Long traineeCount,
    Long currentPeriodCount,
    Long reemployedCount,
    BigDecimal avgDurationDays,
    BigDecimal sharePct,
    BigDecimal reemploymentRatePct
) {}
