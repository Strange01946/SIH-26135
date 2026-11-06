package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
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
  private in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate;

  public ExecutiveDashboardServiceImpl(
      ExecutiveDashboardRepository executiveDashboardRepository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.executiveDashboardRepository = java.util.Objects.requireNonNull(executiveDashboardRepository, "ExecutiveDashboardRepository is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public void setCacheDelegate(in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate) {
    this.cacheDelegate = cacheDelegate;
  }

  @Override
  public DashboardKpiStripResponse getDashboardKpiStrip() {
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getDashboardKpiStrip(null, this::computeUnfilteredKpiStrip);
    }
    return computeUnfilteredKpiStrip();
  }

  private DashboardKpiStripResponse computeUnfilteredKpiStrip() {
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
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getTrainingPipelineFunnel(null, this::computeUnfilteredFunnel);
    }
    return computeUnfilteredFunnel();
  }

  private PipelineFunnelResponse computeUnfilteredFunnel() {
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
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getSchemeProgramPerformanceMatrix(null, this::computeUnfilteredSchemeMatrix);
    }
    return computeUnfilteredSchemeMatrix();
  }

  private LeaderboardResponse<SchemeProgramPerformanceResponse> computeUnfilteredSchemeMatrix() {
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
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getExecutiveDashboard(null, this::computeUnfilteredExecutiveDashboard);
    }
    return computeUnfilteredExecutiveDashboard();
  }

  private ExecutiveDashboardResponse computeUnfilteredExecutiveDashboard() {
    DashboardKpiStripResponse kpiStrip = getDashboardKpiStrip();
    PipelineFunnelResponse pipelineFunnel = getTrainingPipelineFunnel();
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeProgramMatrix = getSchemeProgramPerformanceMatrix();

    return new ExecutiveDashboardResponse(kpiStrip, pipelineFunnel, schemeProgramMatrix);
  }

  @Override
  public DashboardKpiStripResponse getDashboardKpiStrip(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getDashboardKpiStrip();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (cacheDelegate != null) {
      return cacheDelegate.getDashboardKpiStrip(criteria, () -> computeFilteredKpiStrip(criteria));
    }
    return computeFilteredKpiStrip(criteria);
  }

  private DashboardKpiStripResponse computeFilteredKpiStrip(AnalyticsFilterCriteria criteria) {
    DashboardKpiProjection proj = executiveDashboardRepository.getExecutiveDashboardKpiStrip(criteria);

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
  public PipelineFunnelResponse getTrainingPipelineFunnel(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getTrainingPipelineFunnel();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (cacheDelegate != null) {
      return cacheDelegate.getTrainingPipelineFunnel(criteria, () -> computeFilteredFunnel(criteria));
    }
    return computeFilteredFunnel(criteria);
  }

  private PipelineFunnelResponse computeFilteredFunnel(AnalyticsFilterCriteria criteria) {
    PipelineFunnelProjection proj = executiveDashboardRepository.getTrainingPipelineConversionFunnel(criteria);

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
  public LeaderboardResponse<SchemeProgramPerformanceResponse> getSchemeProgramPerformanceMatrix(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getSchemeProgramPerformanceMatrix();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (cacheDelegate != null) {
      return cacheDelegate.getSchemeProgramPerformanceMatrix(criteria, () -> computeFilteredSchemeMatrix(criteria));
    }
    return computeFilteredSchemeMatrix(criteria);
  }

  private LeaderboardResponse<SchemeProgramPerformanceResponse> computeFilteredSchemeMatrix(AnalyticsFilterCriteria criteria) {
    List<SchemeProgramPerformanceProjection> projections = executiveDashboardRepository.getSchemeProgramPerformanceMatrix(criteria);
    if (projections == null || projections.isEmpty()) {
      return LeaderboardResponse.of("employmentRatePct", Collections.emptyList());
    }

    List<SchemeProgramPerformanceResponse> items = projections.stream()
        .map(this::toSchemeProgramResponse)
        .toList();

    return LeaderboardResponse.of("employmentRatePct", items);
  }

  @Override
  public ExecutiveDashboardResponse getExecutiveDashboard(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return getExecutiveDashboard();
    }
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    if (cacheDelegate != null) {
      return cacheDelegate.getExecutiveDashboard(criteria, () -> computeFilteredExecutiveDashboard(criteria));
    }
    return computeFilteredExecutiveDashboard(criteria);
  }

  private ExecutiveDashboardResponse computeFilteredExecutiveDashboard(AnalyticsFilterCriteria criteria) {
    DashboardKpiStripResponse kpiStrip = getDashboardKpiStrip(criteria);
    PipelineFunnelResponse pipelineFunnel = getTrainingPipelineFunnel(criteria);
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeProgramMatrix = getSchemeProgramPerformanceMatrix(criteria);

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
