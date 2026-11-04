package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection record representing multi-milestone retention survival metrics (formal Query Q12)
 * from {@code vw_employment_retention_fact}.
 *
 * <p>Captures independent observable spell counts and retained spell counts at
 * 6-month, 12-month, 24-month, and 36-month post-placement intervals.
 */
public record RetentionSurvivalProjection(
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
