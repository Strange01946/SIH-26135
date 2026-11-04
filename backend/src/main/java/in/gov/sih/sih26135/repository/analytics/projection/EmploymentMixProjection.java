package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection record representing formal Query Q08 results
 * from {@code vw_employment_fact}.
 */
public record EmploymentMixProjection(
    Long employmentSpellCount,
    Long traineeCount,
    Long currentSpellCount,
    Long wageSpellCount,
    Long selfEmploymentSpellCount,
    Long apprenticeshipSpellCount,
    Long internshipSpellCount,
    Long currentWageCount,
    Long currentSelfEmploymentCount,
    BigDecimal avgDurationDays,
    BigDecimal avgCurrentSpellDurationDays
) {}
