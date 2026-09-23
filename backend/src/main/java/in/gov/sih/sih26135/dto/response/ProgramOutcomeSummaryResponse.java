package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record ProgramOutcomeSummaryResponse(
    Long programId,
    Long schemeId,
    Long enrollmentCount,
    Long traineeCount,
    BigDecimal completedCount,
    BigDecimal certifiedCount,
    BigDecimal joinedPlacementCount,
    BigDecimal employedCount,
    BigDecimal selfEmploymentSpellCount,
    BigDecimal apprenticeshipSpellCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
