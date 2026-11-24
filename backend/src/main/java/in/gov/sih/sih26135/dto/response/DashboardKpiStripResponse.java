package in.gov.sih.sih26135.dto.response;

import in.gov.sih.sih26135.service.ExecutiveMisService.KpiComparison;
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
    BigDecimal employmentRatePct,
    KpiComparison executiveKpis
) {
  public DashboardKpiStripResponse(Long traineeCount, Long enrollmentCount, Long enrolledTraineeCount,
      Long completedEnrollmentCount, Long certifiedEnrollmentCount, Long placementCount, Long joinedPlacementCount,
      Long employmentSpellCount, Long employedTraineeCount, Long currentEmploymentSpellCount,
      Long currentSelfEmploymentCount, Long currentApprenticeshipCount, BigDecimal completionRatePct,
      BigDecimal certificationRatePct, BigDecimal placementRatePct, BigDecimal employmentRatePct) {
    this(traineeCount, enrollmentCount, enrolledTraineeCount, completedEnrollmentCount, certifiedEnrollmentCount,
        placementCount, joinedPlacementCount, employmentSpellCount, employedTraineeCount, currentEmploymentSpellCount,
        currentSelfEmploymentCount, currentApprenticeshipCount, completionRatePct, certificationRatePct, placementRatePct,
        employmentRatePct, null);
  }

  public static DashboardKpiStripResponse executive(KpiComparison comparison) {
    return new DashboardKpiStripResponse(null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, comparison);
  }
}
