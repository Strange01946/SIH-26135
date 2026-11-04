package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Analytical query projection record representing formal Query Q09 results
 * from {@code vw_employment_fact} grouped by engagement type.
 */
public record EmploymentCategoryBreakdownProjection(
    String engagementTypeCode,
    String engagementTypeName,
    Boolean isWageEmployment,
    Boolean isSelfEmployment,
    Boolean isApprenticeship,
    Boolean isInternship,
    Long spellCount,
    Long traineeCount,
    Long currentSpellCount,
    BigDecimal avgDurationDays
) {}
