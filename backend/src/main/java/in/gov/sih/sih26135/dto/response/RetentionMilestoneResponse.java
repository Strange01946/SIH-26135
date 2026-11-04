package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing retention performance for an individual post-placement milestone.
 * Distinguishes observable population, verified retained population, and percentage rate.
 */
public record RetentionMilestoneResponse(
    String milestone,
    Integer milestoneMonths,
    Long observableCount,
    Long retainedCount,
    BigDecimal retentionRatePct
) {}
