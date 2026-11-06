package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.AttritionReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.dto.response.RetentionCohortResponse;
import in.gov.sih.sih26135.dto.response.RetentionMilestoneResponse;
import in.gov.sih.sih26135.dto.response.SeparationAnalysisResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentSummaryResponse;
import in.gov.sih.sih26135.repository.analytics.RetentionAttritionDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.AttritionReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.RetentionSurvivalProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SeparationAnalysisProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentSummaryProjection;
import in.gov.sih.sih26135.service.RetentionAttritionDashboardService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link RetentionAttritionDashboardService}.
 * Coordinates repository executions of formal queries Q12, Q15, Q16, Q17, and Q18,
 * deriving milestone survival rates, re-employment conversion, exit classifications,
 * and root-cause shares with mathematical precision via {@link AnalyticsCalculationUtils}.
 */
@Service
@Transactional(readOnly = true)
public class RetentionAttritionDashboardServiceImpl implements RetentionAttritionDashboardService {

  private final RetentionAttritionDashboardRepository repository;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public RetentionAttritionDashboardServiceImpl(RetentionAttritionDashboardRepository repository) {
    this(repository, null);
  }

  @Autowired
  public RetentionAttritionDashboardServiceImpl(
      RetentionAttritionDashboardRepository repository,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.repository = repository;
    this.scopeValidator = scopeValidator;
  }

  @Override
  public LongitudinalRetentionResponse getLongitudinalRetention() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    RetentionSurvivalProjection projection = repository.getFormalQ12RetentionSurvival();
    if (projection == null) {
      return new LongitudinalRetentionResponse(0L, null, List.of());
    }

    BigDecimal rate6m = resolvePercentage(projection.retention6mPct(), projection.retained6mCount(), projection.observable6mCount());
    BigDecimal rate12m = resolvePercentage(projection.retention12mPct(), projection.retained12mCount(), projection.observable12mCount());
    BigDecimal rate24m = resolvePercentage(projection.retention24mPct(), projection.retained24mCount(), projection.observable24mCount());
    BigDecimal rate36m = resolvePercentage(projection.retention36mPct(), projection.retained36mCount(), projection.observable36mCount());

    RetentionCohortResponse cohortSummary = new RetentionCohortResponse(
        projection.employmentSpellCount(),
        projection.observable6mCount(),
        projection.retained6mCount(),
        rate6m,
        projection.observable12mCount(),
        projection.retained12mCount(),
        rate12m,
        projection.observable24mCount(),
        projection.retained24mCount(),
        rate24m,
        projection.observable36mCount(),
        projection.retained36mCount(),
        rate36m
    );

    List<RetentionMilestoneResponse> milestones = List.of(
        new RetentionMilestoneResponse("6_MONTHS", 6, projection.observable6mCount(), projection.retained6mCount(), rate6m),
        new RetentionMilestoneResponse("12_MONTHS", 12, projection.observable12mCount(), projection.retained12mCount(), rate12m),
        new RetentionMilestoneResponse("24_MONTHS", 24, projection.observable24mCount(), projection.retained24mCount(), rate24m),
        new RetentionMilestoneResponse("36_MONTHS", 36, projection.observable36mCount(), projection.retained36mCount(), rate36m)
    );

    return new LongitudinalRetentionResponse(
        projection.employmentSpellCount(),
        cohortSummary,
        milestones
    );
  }

  @Override
  public AttritionDashboardResponse getAttritionDashboard() {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    UnemploymentSummaryProjection uProj = repository.getFormalQ15UnemploymentSummary();
    List<UnemploymentReasonProjection> reasonProjs = repository.getFormalQ16UnemploymentReasons();
    List<AttritionReasonProjection> exitProjs = repository.getFormalQ17AttritionReasons();
    SeparationAnalysisProjection sepProj = repository.getFormalQ18SeparationTotals();

    UnemploymentSummaryResponse unemploymentSummary = mapUnemploymentSummary(uProj);
    List<UnemploymentReasonBreakdownResponse> unemploymentReasons = mapUnemploymentReasons(reasonProjs, uProj);
    SeparationAnalysisResponse separationAnalysis = mapSeparationAnalysis(sepProj);
    List<AttritionReasonBreakdownResponse> exitReasons = mapExitReasons(exitProjs, sepProj);

    return new AttritionDashboardResponse(
        unemploymentSummary,
        unemploymentReasons,
        separationAnalysis,
        exitReasons
    );
  }

  private UnemploymentSummaryResponse mapUnemploymentSummary(UnemploymentSummaryProjection p) {
    if (p == null) {
      return new UnemploymentSummaryResponse(0L, 0L, 0L, 0L, 0L, null, null, null);
    }

    BigDecimal reemploymentRatePct = AnalyticsCalculationUtils.calculatePercentage(
        p.reemployedCount(),
        p.unemploymentPeriodCount()
    );

    return new UnemploymentSummaryResponse(
        p.unemploymentPeriodCount(),
        p.traineeCount(),
        p.currentUnemploymentCount(),
        p.reemployedCount(),
        p.neverPrecededByEmploymentCount(),
        p.avgDurationDays(),
        p.avgOpenPeriodDurationDays(),
        reemploymentRatePct
    );
  }

  private List<UnemploymentReasonBreakdownResponse> mapUnemploymentReasons(
      List<UnemploymentReasonProjection> reasonProjs,
      UnemploymentSummaryProjection uProj) {
    if (reasonProjs == null || reasonProjs.isEmpty()) {
      return List.of();
    }

    Long totalPeriods = (uProj != null && uProj.unemploymentPeriodCount() != null && uProj.unemploymentPeriodCount() > 0)
        ? uProj.unemploymentPeriodCount()
        : sumPeriods(reasonProjs);

    List<UnemploymentReasonBreakdownResponse> results = new ArrayList<>(reasonProjs.size());
    for (UnemploymentReasonProjection r : reasonProjs) {
      BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(r.periodCount(), totalPeriods);
      BigDecimal reemploymentRatePct = AnalyticsCalculationUtils.calculatePercentage(r.reemployedCount(), r.periodCount());

      results.add(new UnemploymentReasonBreakdownResponse(
          r.unemploymentReasonId(),
          r.unemploymentReasonCode(),
          r.reasonName(),
          r.periodCount(),
          r.traineeCount(),
          r.currentPeriodCount(),
          r.reemployedCount(),
          r.avgDurationDays(),
          sharePct,
          reemploymentRatePct
      ));
    }
    return results;
  }

  private SeparationAnalysisResponse mapSeparationAnalysis(SeparationAnalysisProjection p) {
    if (p == null) {
      return new SeparationAnalysisResponse(0L, 0L, 0L, 0L, null, null, null, null, null);
    }

    BigDecimal voluntarySharePct = AnalyticsCalculationUtils.calculatePercentage(
        p.voluntaryExitCount(),
        p.totalExitCount()
    );

    BigDecimal involuntarySharePct = AnalyticsCalculationUtils.calculatePercentage(
        p.involuntaryExitCount(),
        p.totalExitCount()
    );

    BigDecimal otherSharePct = AnalyticsCalculationUtils.calculatePercentage(
        p.otherExitCount(),
        p.totalExitCount()
    );

    BigDecimal voluntaryToInvoluntaryRatio = null;
    if (p.voluntaryExitCount() != null && p.involuntaryExitCount() != null && p.involuntaryExitCount() > 0L) {
      voluntaryToInvoluntaryRatio = AnalyticsCalculationUtils.safeDivide(
          BigDecimal.valueOf(p.voluntaryExitCount()),
          BigDecimal.valueOf(p.involuntaryExitCount()),
          AnalyticsCalculationUtils.DEFAULT_PERCENTAGE_SCALE,
          AnalyticsCalculationUtils.DEFAULT_ROUNDING_MODE
      );
    }

    // Notice compliance is unobservable in the current database schema; evaluates safely to null
    BigDecimal noticeComplianceRatePct = null;

    return new SeparationAnalysisResponse(
        p.totalExitCount(),
        p.voluntaryExitCount(),
        p.involuntaryExitCount(),
        p.otherExitCount(),
        voluntarySharePct,
        involuntarySharePct,
        otherSharePct,
        voluntaryToInvoluntaryRatio,
        noticeComplianceRatePct
    );
  }

  private List<AttritionReasonBreakdownResponse> mapExitReasons(
      List<AttritionReasonProjection> exitProjs,
      SeparationAnalysisProjection sepProj) {
    if (exitProjs == null || exitProjs.isEmpty()) {
      return List.of();
    }

    Long totalExits = (sepProj != null && sepProj.totalExitCount() != null && sepProj.totalExitCount() > 0)
        ? sepProj.totalExitCount()
        : sumExits(exitProjs);

    List<AttritionReasonBreakdownResponse> results = new ArrayList<>(exitProjs.size());
    for (AttritionReasonProjection e : exitProjs) {
      BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(e.exitCount(), totalExits);

      results.add(new AttritionReasonBreakdownResponse(
          e.employmentExitReasonId(),
          e.exitReasonCode(),
          e.exitReasonName(),
          e.separationNatureId(),
          e.separationNatureCode(),
          e.separationNatureName(),
          e.isVoluntaryFlag(),
          e.isInvoluntaryFlag(),
          e.exitCount(),
          e.traineeCount(),
          sharePct
      ));
    }
    return results;
  }

  private BigDecimal resolvePercentage(BigDecimal sqlRate, Long numerator, Long denominator) {
    if (sqlRate != null) {
      return sqlRate.setScale(AnalyticsCalculationUtils.DEFAULT_PERCENTAGE_SCALE, AnalyticsCalculationUtils.DEFAULT_ROUNDING_MODE);
    }
    return AnalyticsCalculationUtils.calculatePercentage(numerator, denominator);
  }

  private Long sumPeriods(List<UnemploymentReasonProjection> projs) {
    long sum = 0L;
    for (UnemploymentReasonProjection p : projs) {
      if (p.periodCount() != null) {
        sum += p.periodCount();
      }
    }
    return sum > 0L ? sum : null;
  }

  private Long sumExits(List<AttritionReasonProjection> projs) {
    long sum = 0L;
    for (AttritionReasonProjection p : projs) {
      if (p.exitCount() != null) {
        sum += p.exitCount();
      }
    }
    return sum > 0L ? sum : null;
  }
}
