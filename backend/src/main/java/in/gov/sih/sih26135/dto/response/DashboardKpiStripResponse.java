package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing consolidated dashboard-level KPIs (Q01).
 * Counts use {@link Long} and rates use {@link BigDecimal} normalized to 2 decimal places.
 */
public record DashboardKpiStripResponse(
    Long traineeCount,
    Long enrollmentCount,
    Long enrolledTraineeCount,
    Long completedEnrollmentCount,
    Long certifiedEnrollmentCount,
    Long placementCount,
    Long joinedPlacementCount,
    Long employmentSpellCount,
    Long employedTraineeCount,
    Long currentEmploymentSpellCount,
    Long currentSelfEmploymentCount,
    Long currentApprenticeshipCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct
) {}
