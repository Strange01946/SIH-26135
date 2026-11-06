package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.ExecutiveDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.DashboardKpiProjection;
import in.gov.sih.sih26135.repository.analytics.projection.PipelineFunnelProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SchemeProgramPerformanceProjection;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link ExecutiveDashboardService}.
 * Executes formal analytical queries Q01, Q02, and Q03 via read-only repository projections
 * and transforms results into standardized dashboard DTOs using {@link AnalyticsCalculationUtils}.
 */
@Service
@Transactional(readOnly = true)
public class ExecutiveDashboardServiceImpl implements ExecutiveDashboardService {

  private final ExecutiveDashboardRepository executiveDashboardRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public ExecutiveDashboardServiceImpl(ExecutiveDashboardRepository executiveDashboardRepository) {
    this(executiveDashboardRepository, null);
  }

  @Autowired
  public ExecutiveDashboardServiceImpl(
      ExecutiveDashboardRepository executiveDashboardRepository,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.executiveDashboardRepository = executiveDashboardRepository;
    this.scopeValidator = scopeValidator;
  }

  @Override
  public DashboardKpiStripResponse getDashboardKpiStrip() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    DashboardKpiProjection proj = executiveDashboardRepository.getExecutiveDashboardKpiStrip();

    Long traineeCount = toLong(proj != null ? proj.getTraineeCount() : null);
    Long enrollmentCount = toLong(proj != null ? proj.getEnrollmentCount() : null);
    Long enrolledTraineeCount = toLong(proj != null ? proj.getEnrolledTraineeCount() : null);
    Long completedEnrollmentCount = toLong(proj != null ? proj.getCompletedEnrollmentCount() : null);
    Long certifiedEnrollmentCount = toLong(proj != null ? proj.getCertifiedEnrollmentCount() : null);
    Long placementCount = toLong(proj != null ? proj.getPlacementCount() : null);
    Long joinedPlacementCount = toLong(proj != null ? proj.getJoinedPlacementCount() : null);
    Long employmentSpellCount = toLong(proj != null ? proj.getEmploymentSpellCount() : null);
    Long employedTraineeCount = toLong(proj != null ? proj.getEmployedTraineeCount() : null);
    Long currentEmploymentSpellCount = toLong(proj != null ? proj.getCurrentEmploymentSpellCount() : null);
    Long currentSelfEmploymentCount = toLong(proj != null ? proj.getCurrentSelfEmploymentCount() : null);
    Long currentApprenticeshipCount = toLong(proj != null ? proj.getCurrentApprenticeshipCount() : null);

    BigDecimal completionRatePct = AnalyticsCalculationUtils.calculatePercentage(completedEnrollmentCount, enrollmentCount);
    BigDecimal certificationRatePct = AnalyticsCalculationUtils.calculatePercentage(certifiedEnrollmentCount, enrollmentCount);
    BigDecimal placementRatePct = AnalyticsCalculationUtils.calculatePercentage(joinedPlacementCount, enrollmentCount);
    BigDecimal employmentRatePct = AnalyticsCalculationUtils.calculatePercentage(employedTraineeCount, enrollmentCount);

    return new DashboardKpiStripResponse(
        traineeCount,
        enrollmentCount,
        enrolledTraineeCount,
        completedEnrollmentCount,
        certifiedEnrollmentCount,
        placementCount,
        joinedPlacementCount,
        employmentSpellCount,
        employedTraineeCount,
        currentEmploymentSpellCount,
        currentSelfEmploymentCount,
        currentApprenticeshipCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct
    );
  }

  @Override
  public PipelineFunnelResponse getTrainingPipelineFunnel() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    PipelineFunnelProjection proj = executiveDashboardRepository.getTrainingPipelineConversionFunnel();

    Long enrollmentCount = toLong(proj != null ? proj.getEnrollmentCount() : null);
    Long completedCount = toLong(proj != null ? proj.getCompletedCount() : null);
    Long certifiedCount = toLong(proj != null ? proj.getCertifiedCount() : null);
    Long joinedPlacementCount = toLong(proj != null ? proj.getJoinedPlacementCount() : null);
    Long employedCount = toLong(proj != null ? proj.getEmployedCount() : null);

    BigDecimal completionRatePct = proj != null && proj.getCompletionRatePct() != null
        ? AnalyticsCalculationUtils.round(proj.getCompletionRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(completedCount, enrollmentCount);

    BigDecimal certificationRatePct = proj != null && proj.getCertificationRatePct() != null
        ? AnalyticsCalculationUtils.round(proj.getCertificationRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(certifiedCount, enrollmentCount);

    BigDecimal placementRatePct = proj != null && proj.getPlacementRatePct() != null
        ? AnalyticsCalculationUtils.round(proj.getPlacementRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(joinedPlacementCount, enrollmentCount);

    BigDecimal employmentRatePct = proj != null && proj.getEmploymentRatePct() != null
        ? AnalyticsCalculationUtils.round(proj.getEmploymentRatePct())
        : AnalyticsCalculationUtils.calculatePercentage(employedCount, enrollmentCount);

    return new PipelineFunnelResponse(
        enrollmentCount,
        completedCount,
        certifiedCount,
        joinedPlacementCount,
        employedCount,
        completionRatePct,
        certificationRatePct,
        placementRatePct,
        employmentRatePct
    );
  }

  @Override
  public LeaderboardResponse<SchemeProgramPerformanceResponse> getSchemeProgramPerformanceMatrix() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    List<SchemeProgramPerformanceProjection> projections = executiveDashboardRepository.getSchemeProgramPerformanceMatrix();
    if (projections == null || projections.isEmpty()) {
      return LeaderboardResponse.of("employmentRatePct", Collections.emptyList());
    }

    List<SchemeProgramPerformanceResponse> items = projections.stream()
        .map(this::toSchemeProgramResponse)
        .toList();

    return LeaderboardResponse.of("employmentRatePct", items);
  }

  @Override
  public ExecutiveDashboardResponse getExecutiveDashboard() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    DashboardKpiStripResponse kpiStrip = getDashboardKpiStrip();
    PipelineFunnelResponse pipelineFunnel = getTrainingPipelineFunnel();
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeProgramMatrix = getSchemeProgramPerformanceMatrix();

    return new ExecutiveDashboardResponse(kpiStrip, pipelineFunnel, schemeProgramMatrix);
  }

  private SchemeProgramPerformanceResponse toSchemeProgramResponse(SchemeProgramPerformanceProjection p) {
    return new SchemeProgramPerformanceResponse(
        p.getProgramId(),
        p.getProgramCode(),
        p.getProgramName(),
        p.getSchemeId(),
        p.getSchemeCode(),
        p.getSchemeName(),
        toLong(p.getEnrollmentCount()),
        toLong(p.getTraineeCount()),
        toLong(p.getCompletedCount()),
        toLong(p.getCertifiedCount()),
        toLong(p.getJoinedPlacementCount()),
        toLong(p.getEmployedCount()),
        toLong(p.getSelfEmploymentSpellCount()),
        toLong(p.getApprenticeshipSpellCount()),
        AnalyticsCalculationUtils.round(p.getCompletionRatePct()),
        AnalyticsCalculationUtils.round(p.getCertificationRatePct()),
        AnalyticsCalculationUtils.round(p.getPlacementRatePct()),
        AnalyticsCalculationUtils.round(p.getEmploymentRatePct())
    );
  }

  private Long toLong(Number number) {
    if (number == null) {
      return 0L;
    }
    return number.longValue();
  }
}
