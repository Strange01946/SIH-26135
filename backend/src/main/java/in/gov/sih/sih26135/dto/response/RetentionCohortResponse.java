package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing retention cohort and milestone tracking metrics (Q12).
 * Distinguishes between observable employment spells and verified retained spells at
 * 6-month, 12-month, 24-month, and 36-month milestones.
 * When observable counts are zero, retention percentage is {@code null} (unreached milestone),
 * preserving distinct semantics from zero retention.
 */
public record RetentionCohortResponse(
    Long employmentSpellCount,
    Long observable6mCount,
    Long retained6mCount,
    BigDecimal retention6mPct,
    Long observable12mCount,
    Long retained12mCount,
    BigDecimal retention12mPct,
    Long observable24mCount,
    Long retained24mCount,
    BigDecimal retention24mPct,
    Long observable36mCount,
    Long retained36mCount,
    BigDecimal retention36mPct
) {}
