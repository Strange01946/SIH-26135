package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing a single row in the Scheme & Program
 * Comparative Performance Matrix (Q03).
 * Preserves scheme and program codes and display names, enrollment and completion metrics,
 * and employment efficacy rates.
 */
public record SchemeProgramPerformanceResponse(
    Long programId,
    String programCode,
    String programName,
    Long schemeId,
    String schemeCode,
    String schemeName,
    Long enrollmentCount,
    Long traineeCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    Long selfEmploymentSpellCount,
    Long apprenticeshipSpellCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
