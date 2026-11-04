package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing consolidated employment mix analytics
 * derived from formal analytical Query Q08.
 */
public record EmploymentMixResponse(
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
    BigDecimal avgCurrentSpellDurationDays,
    BigDecimal wageEmploymentSharePct,
    BigDecimal selfEmploymentSharePct,
    BigDecimal apprenticeshipSharePct,
    BigDecimal internshipSharePct
) {}
