package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CourseWageProgressionResponse;
import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentCategoryBreakdownResponse;
import in.gov.sih.sih26135.dto.response.EmploymentMixResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.SalaryDistributionResponse;
import in.gov.sih.sih26135.dto.response.WageProgressionResponse;
import in.gov.sih.sih26135.repository.analytics.EconomicOutcomeDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.CourseWageProgressionProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmployerHiringProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentCategoryBreakdownProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SalaryBracketCountProjection;
import in.gov.sih.sih26135.repository.analytics.projection.WageProgressionProjection;
import in.gov.sih.sih26135.service.EconomicOutcomeDashboardService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link EconomicOutcomeDashboardService}.
 * Coordinates repository query executions, derived share calculations,
 * deterministic ranking, and salary bracket distribution formatting.
 */
@Service
@Transactional(readOnly = true)
public class EconomicOutcomeDashboardServiceImpl implements EconomicOutcomeDashboardService {

  public static final String EMPLOYER_RANKING_METRIC = "joined_count DESC, placement_count DESC";

  private record BracketMeta(String code, String name) {}

  private static final List<BracketMeta> CANONICAL_BRACKETS = List.of(
      new BracketMeta("BELOW_15K", "Below ₹15,000"),
      new BracketMeta("FROM_15K_TO_25K", "₹15,000–₹25,000"),
      new BracketMeta("FROM_25K_TO_50K", "₹25,000–₹50,000"),
      new BracketMeta("ABOVE_50K", "Above ₹50,000")
  );

  private final EconomicOutcomeDashboardRepository repository;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService privacyService;
  private in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate;

  @org.springframework.beans.factory.annotation.Autowired
  public EconomicOutcomeDashboardServiceImpl(
      EconomicOutcomeDashboardRepository repository,
      WorkflowSecurityScopeValidator scopeValidator,
      in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService privacyService) {
    this.repository = java.util.Objects.requireNonNull(repository, "EconomicOutcomeDashboardRepository is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
    this.privacyService = privacyService != null ? privacyService : new in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService();
  }

  public EconomicOutcomeDashboardServiceImpl(
      EconomicOutcomeDashboardRepository repository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this(repository, scopeValidator, new in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService());
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public void setCacheDelegate(in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate cacheDelegate) {
    this.cacheDelegate = cacheDelegate;
  }

  @Override
  public LeaderboardResponse<EmployerHiringPerformanceResponse> getEmployerLeaderboard() {
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getEmployerLeaderboard(null, this::computeUnfilteredEmployerLeaderboard);
    }
    return computeUnfilteredEmployerLeaderboard();
  }

  private LeaderboardResponse<EmployerHiringPerformanceResponse> computeUnfilteredEmployerLeaderboard() {
    List<EmployerHiringProjection> projections = repository.getFormalQ07EmployerLeaderboard();
    if (projections == null || projections.isEmpty()) {
      return LeaderboardResponse.of(EMPLOYER_RANKING_METRIC, Collections.emptyList());
    }

    List<EmployerHiringPerformanceResponse> items = new ArrayList<>(projections.size());
    for (int i = 0; i < projections.size(); i++) {
      EmployerHiringProjection p = projections.get(i);
      items.add(new EmployerHiringPerformanceResponse(
          p.employerId(),
          p.employerCode(),
          p.employerName(),
          p.placementCount() != null ? p.placementCount() : 0L,
          p.traineeCount() != null ? p.traineeCount() : 0L,
          p.joinedCount() != null ? p.joinedCount() : 0L,
          p.joinRatePct(),
          p.avgJoiningSalary(),
          i + 1
      ));
    }

    return LeaderboardResponse.of(EMPLOYER_RANKING_METRIC, privacyService.applyPrivacyGuardrailsToEmployers(items));
  }

  @Override
  public EconomicOutcomeSummaryResponse getEconomicOutcomeSummary() {
    scopeValidator.validateNationwideScope();
    if (cacheDelegate != null) {
      return cacheDelegate.getEconomicOutcomeSummary(null, this::computeUnfilteredEconomicOutcomeSummary);
    }
    return computeUnfilteredEconomicOutcomeSummary();
  }

  private EconomicOutcomeSummaryResponse computeUnfilteredEconomicOutcomeSummary() {
    // 1. Wage Progression KPIs (Q10)
    WageProgressionResponse wageProgression = mapWageProgression(repository.getFormalQ10WageProgression());

    // 2. Salary Distribution Brackets
    List<SalaryDistributionResponse> salaryDistribution = buildSalaryDistribution(
        repository.getSalaryDistributionCounts()
    );

    // 3. Employment Mix (Q08)
    EmploymentMixResponse employmentMix = mapEmploymentMix(repository.getFormalQ08EmploymentMix());

    // 4. Engagement Category Breakdown (Q09)
    Long totalSpells = employmentMix != null ? employmentMix.employmentSpellCount() : null;
    List<EmploymentCategoryBreakdownResponse> categoryBreakdown = mapCategoryBreakdown(
        repository.getFormalQ09CategoryBreakdown(),
        totalSpells
    );

    // 5. Course Wage Progression (Q11)
    List<CourseWageProgressionResponse> courseWageProgression = mapCourseWageProgression(
        repository.getFormalQ11CourseWageProgression()
    );

    return privacyService.applyPrivacyGuardrail(new EconomicOutcomeSummaryResponse(
        wageProgression,
        salaryDistribution,
        employmentMix,
        categoryBreakdown,
        courseWageProgression
    ));
  }

  private WageProgressionResponse mapWageProgression(WageProgressionProjection p) {
    if (p == null) {
      return new WageProgressionResponse(0L, 0L, 0L, null, null, null, null);
    }
    return new WageProgressionResponse(
        p.employmentSpellCount() != null ? p.employmentSpellCount() : 0L,
        p.firstSalaryCount() != null ? p.firstSalaryCount() : 0L,
        p.latestSalaryCount() != null ? p.latestSalaryCount() : 0L,
        p.avgFirstRecordedSalary(),
        p.avgLatestRecordedSalary(),
        p.avgSalaryChangeAmount(),
        p.avgSalaryChangePct()
    );
  }

  private List<SalaryDistributionResponse> buildSalaryDistribution(
      List<SalaryBracketCountProjection> counts) {
    Map<String, Long> countMap = new HashMap<>();
    long totalCount = 0L;

    if (counts != null) {
      for (SalaryBracketCountProjection item : counts) {
        if (item.bracketCode() != null && item.count() != null) {
          countMap.put(item.bracketCode(), item.count());
          totalCount += item.count();
        }
      }
    }

    List<SalaryDistributionResponse> distribution = new ArrayList<>(CANONICAL_BRACKETS.size());
    for (BracketMeta meta : CANONICAL_BRACKETS) {
      long count = countMap.getOrDefault(meta.code(), 0L);
      BigDecimal pct = AnalyticsCalculationUtils.calculatePercentage(count, totalCount);
      distribution.add(new SalaryDistributionResponse(
          meta.code(),
          meta.name(),
          count,
          pct
      ));
    }

    return distribution;
  }

  private EmploymentMixResponse mapEmploymentMix(EmploymentMixProjection p) {
    if (p == null) {
      return new EmploymentMixResponse(
          0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
          null, null, null, null, null, null
      );
    }

    Long totalSpells = p.employmentSpellCount() != null ? p.employmentSpellCount() : 0L;
    Long wageCount = p.wageSpellCount() != null ? p.wageSpellCount() : 0L;
    Long selfCount = p.selfEmploymentSpellCount() != null ? p.selfEmploymentSpellCount() : 0L;
    Long appCount = p.apprenticeshipSpellCount() != null ? p.apprenticeshipSpellCount() : 0L;
    Long internCount = p.internshipSpellCount() != null ? p.internshipSpellCount() : 0L;

    BigDecimal wageShare = AnalyticsCalculationUtils.calculatePercentage(wageCount, totalSpells);
    BigDecimal selfShare = AnalyticsCalculationUtils.calculatePercentage(selfCount, totalSpells);
    BigDecimal appShare = AnalyticsCalculationUtils.calculatePercentage(appCount, totalSpells);
    BigDecimal internShare = AnalyticsCalculationUtils.calculatePercentage(internCount, totalSpells);

    return new EmploymentMixResponse(
        totalSpells,
        p.traineeCount() != null ? p.traineeCount() : 0L,
        p.currentSpellCount() != null ? p.currentSpellCount() : 0L,
        wageCount,
        selfCount,
        appCount,
        internCount,
        p.currentWageCount() != null ? p.currentWageCount() : 0L,
        p.currentSelfEmploymentCount() != null ? p.currentSelfEmploymentCount() : 0L,
        p.avgDurationDays(),
        p.avgCurrentSpellDurationDays(),
        wageShare,
        selfShare,
        appShare,
        internShare
    );
  }

  private List<EmploymentCategoryBreakdownResponse> mapCategoryBreakdown(
      List<EmploymentCategoryBreakdownProjection> projections,
      Long totalSpells) {
    if (projections == null || projections.isEmpty()) {
      return Collections.emptyList();
    }

    List<EmploymentCategoryBreakdownResponse> items = new ArrayList<>(projections.size());
    for (EmploymentCategoryBreakdownProjection p : projections) {
      Long spellCount = p.spellCount() != null ? p.spellCount() : 0L;
      BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(spellCount, totalSpells);

      items.add(new EmploymentCategoryBreakdownResponse(
          p.engagementTypeCode(),
          p.engagementTypeName(),
          Boolean.TRUE.equals(p.isWageEmployment()),
          Boolean.TRUE.equals(p.isSelfEmployment()),
          Boolean.TRUE.equals(p.isApprenticeship()),
          Boolean.TRUE.equals(p.isInternship()),
          spellCount,
          p.traineeCount() != null ? p.traineeCount() : 0L,
          p.currentSpellCount() != null ? p.currentSpellCount() : 0L,
          sharePct,
          p.avgDurationDays()
      ));
    }

    return items;
  }

  private List<CourseWageProgressionResponse> mapCourseWageProgression(
      List<CourseWageProgressionProjection> projections) {
    if (projections == null || projections.isEmpty()) {
      return Collections.emptyList();
    }

    List<CourseWageProgressionResponse> items = new ArrayList<>(projections.size());
    for (CourseWageProgressionProjection p : projections) {
      items.add(new CourseWageProgressionResponse(
          p.courseId(),
          p.courseCode(),
          p.courseName(),
          p.spellCount() != null ? p.spellCount() : 0L,
          p.avgFirstRecordedSalary(),
          p.avgLatestRecordedSalary(),
          p.avgSalaryChangePct()
      ));
    }

    return items;
  }
}
