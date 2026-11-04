package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing engagement type category breakdown
 * derived from formal analytical Query Q09.
 */
public record EmploymentCategoryBreakdownResponse(
    String engagementTypeCode,
    String engagementTypeName,
    Boolean isWageEmployment,
    Boolean isSelfEmployment,
    Boolean isApprenticeship,
    Boolean isInternship,
    Long spellCount,
    Long traineeCount,
    Long currentSpellCount,
    BigDecimal sharePct,
    BigDecimal avgDurationDays
) {}
