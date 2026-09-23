package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record UnemploymentReasonSummaryResponse(
    Long unemploymentReasonId,
    String unemploymentReasonCode,
    Long periodCount,
    Long traineeCount,
    BigDecimal currentPeriodCount,
    BigDecimal reemployedCount,
    BigDecimal avgDurationDays
) {}
