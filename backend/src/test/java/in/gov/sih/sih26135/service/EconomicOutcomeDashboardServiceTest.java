package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import in.gov.sih.sih26135.service.impl.EconomicOutcomeDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EconomicOutcomeDashboardServiceImpl}.
 * Verifies sequential 1-based ranking, null safety, salary distribution bracket completeness,
 * and derived share calculations.
 */
class EconomicOutcomeDashboardServiceTest {

  private EconomicOutcomeDashboardRepository repository;
  private EconomicOutcomeDashboardServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(EconomicOutcomeDashboardRepository.class);
    service = new EconomicOutcomeDashboardServiceImpl(repository);
  }

  @Test
  @DisplayName("getEmployerLeaderboard: ranks items sequentially and calculates metrics safely")
  void testEmployerLeaderboardRankingAndMapping() {
    List<EmployerHiringProjection> projections = List.of(
        new EmployerHiringProjection(
            1L, "EMP-TCS", "Tata Consultancy Services",
            500L, 450L, 400L, new BigDecimal("80.00"), new BigDecimal("35000.00")
        ),
        new EmployerHiringProjection(
            2L, "EMP-INFY", "Infosys Limited",
            300L, 280L, 250L, new BigDecimal("83.33"), new BigDecimal("32000.00")
        )
    );

    when(repository.getFormalQ07EmployerLeaderboard()).thenReturn(projections);

    LeaderboardResponse<EmployerHiringPerformanceResponse> result = service.getEmployerLeaderboard();

    assertThat(result.rankingMetric()).isEqualTo(EconomicOutcomeDashboardServiceImpl.EMPLOYER_RANKING_METRIC);
    assertThat(result.totalItems()).isEqualTo(2);
    assertThat(result.items()).hasSize(2);

    EmployerHiringPerformanceResponse first = result.items().get(0);
    assertThat(first.rank()).isEqualTo(1);
    assertThat(first.employerId()).isEqualTo(1L);
    assertThat(first.employerCode()).isEqualTo("EMP-TCS");
    assertThat(first.employerName()).isEqualTo("Tata Consultancy Services");
    assertThat(first.placementCount()).isEqualTo(500L);
    assertThat(first.traineeCount()).isEqualTo(450L);
    assertThat(first.joinedCount()).isEqualTo(400L);
    assertThat(first.joinRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(first.avgJoiningSalary()).isEqualByComparingTo(new BigDecimal("35000.00"));

    EmployerHiringPerformanceResponse second = result.items().get(1);
    assertThat(second.rank()).isEqualTo(2);
    assertThat(second.employerCode()).isEqualTo("EMP-INFY");
    assertThat(second.joinedCount()).isEqualTo(250L);

    verify(repository).getFormalQ07EmployerLeaderboard();
  }

  @Test
  @DisplayName("getEmployerLeaderboard: handles empty and null projections safely")
  void testEmployerLeaderboardEmptyAndNull() {
    when(repository.getFormalQ07EmployerLeaderboard()).thenReturn(Collections.emptyList());

    LeaderboardResponse<EmployerHiringPerformanceResponse> result = service.getEmployerLeaderboard();

    assertThat(result.totalItems()).isEqualTo(0);
    assertThat(result.items()).isEmpty();

    when(repository.getFormalQ07EmployerLeaderboard()).thenReturn(null);

    LeaderboardResponse<EmployerHiringPerformanceResponse> nullResult = service.getEmployerLeaderboard();
    assertThat(nullResult.totalItems()).isEqualTo(0);
    assertThat(nullResult.items()).isEmpty();
  }

  @Test
  @DisplayName("getEmployerLeaderboard: preserves nulls and zero values without fabrication")
  void testEmployerLeaderboardNullHandling() {
    List<EmployerHiringProjection> projections = List.of(
        new EmployerHiringProjection(
            3L, "EMP-NEW", "New Startup",
            null, null, null, null, null
        )
    );

    when(repository.getFormalQ07EmployerLeaderboard()).thenReturn(projections);

    LeaderboardResponse<EmployerHiringPerformanceResponse> result = service.getEmployerLeaderboard();

    assertThat(result.totalItems()).isEqualTo(1);
    EmployerHiringPerformanceResponse item = result.items().get(0);
    assertThat(item.rank()).isEqualTo(1);
    assertThat(item.placementCount()).isEqualTo(0L);
    assertThat(item.traineeCount()).isEqualTo(0L);
    assertThat(item.joinedCount()).isEqualTo(0L);
    assertThat(item.joinRatePct()).isNull();
    assertThat(item.avgJoiningSalary()).isNull();
  }

  @Test
  @DisplayName("getEconomicOutcomeSummary: maps all 4 canonical salary brackets with correct percentages")
  void testSalaryDistributionBracketsCompleteness() {
    WageProgressionProjection wageProj = new WageProgressionProjection(
        100L, 80L, 75L,
        new BigDecimal("18000.00"), new BigDecimal("24000.00"),
        new BigDecimal("6000.00"), new BigDecimal("33.33")
    );

    List<SalaryBracketCountProjection> bracketCounts = List.of(
        new SalaryBracketCountProjection("BELOW_15K", 20L),
        new SalaryBracketCountProjection("FROM_15K_TO_25K", 50L),
        new SalaryBracketCountProjection("FROM_25K_TO_50K", 25L),
        new SalaryBracketCountProjection("ABOVE_50K", 5L)
    );

    EmploymentMixProjection mixProj = new EmploymentMixProjection(
        100L, 90L, 70L, 60L, 20L, 15L, 5L, 45L, 15L,
        new BigDecimal("180.00"), new BigDecimal("120.00")
    );

    when(repository.getFormalQ10WageProgression()).thenReturn(wageProj);
    when(repository.getSalaryDistributionCounts()).thenReturn(bracketCounts);
    when(repository.getFormalQ08EmploymentMix()).thenReturn(mixProj);
    when(repository.getFormalQ09CategoryBreakdown()).thenReturn(Collections.emptyList());
    when(repository.getFormalQ11CourseWageProgression()).thenReturn(Collections.emptyList());

    EconomicOutcomeSummaryResponse result = service.getEconomicOutcomeSummary();

    List<SalaryDistributionResponse> dist = result.salaryDistribution();
    assertThat(dist).hasSize(4);

    assertThat(dist.get(0).bracketCode()).isEqualTo("BELOW_15K");
    assertThat(dist.get(0).bracketName()).isEqualTo("Below ₹15,000");
    assertThat(dist.get(0).count()).isEqualTo(20L);
    assertThat(dist.get(0).percentage()).isEqualByComparingTo(new BigDecimal("20.00"));

    assertThat(dist.get(1).bracketCode()).isEqualTo("FROM_15K_TO_25K");
    assertThat(dist.get(1).bracketName()).isEqualTo("₹15,000–₹25,000");
    assertThat(dist.get(1).count()).isEqualTo(50L);
    assertThat(dist.get(1).percentage()).isEqualByComparingTo(new BigDecimal("50.00"));

    assertThat(dist.get(2).bracketCode()).isEqualTo("FROM_25K_TO_50K");
    assertThat(dist.get(2).bracketName()).isEqualTo("₹25,000–₹50,000");
    assertThat(dist.get(2).count()).isEqualTo(25L);
    assertThat(dist.get(2).percentage()).isEqualByComparingTo(new BigDecimal("25.00"));

    assertThat(dist.get(3).bracketCode()).isEqualTo("ABOVE_50K");
    assertThat(dist.get(3).bracketName()).isEqualTo("Above ₹50,000");
    assertThat(dist.get(3).count()).isEqualTo(5L);
    assertThat(dist.get(3).percentage()).isEqualByComparingTo(new BigDecimal("5.00"));
  }

  @Test
  @DisplayName("getEconomicOutcomeSummary: guarantees 4 canonical brackets even when repository returns partial or empty brackets")
  void testSalaryDistributionBracketsWithMissingCategories() {
    // Only ABOVE_50K is present in DB
    List<SalaryBracketCountProjection> bracketCounts = List.of(
        new SalaryBracketCountProjection("ABOVE_50K", 10L)
    );

    when(repository.getFormalQ10WageProgression()).thenReturn(null);
    when(repository.getSalaryDistributionCounts()).thenReturn(bracketCounts);
    when(repository.getFormalQ08EmploymentMix()).thenReturn(null);
    when(repository.getFormalQ09CategoryBreakdown()).thenReturn(null);
    when(repository.getFormalQ11CourseWageProgression()).thenReturn(null);

    EconomicOutcomeSummaryResponse result = service.getEconomicOutcomeSummary();

    List<SalaryDistributionResponse> dist = result.salaryDistribution();
    assertThat(dist).hasSize(4);

    // Missing brackets have count 0 and percentage 0.00
    assertThat(dist.get(0).bracketCode()).isEqualTo("BELOW_15K");
    assertThat(dist.get(0).count()).isEqualTo(0L);
    assertThat(dist.get(0).percentage()).isEqualByComparingTo(new BigDecimal("0.00"));

    assertThat(dist.get(1).bracketCode()).isEqualTo("FROM_15K_TO_25K");
    assertThat(dist.get(1).count()).isEqualTo(0L);
    assertThat(dist.get(1).percentage()).isEqualByComparingTo(new BigDecimal("0.00"));

    assertThat(dist.get(2).bracketCode()).isEqualTo("FROM_25K_TO_50K");
    assertThat(dist.get(2).count()).isEqualTo(0L);
    assertThat(dist.get(2).percentage()).isEqualByComparingTo(new BigDecimal("0.00"));

    assertThat(dist.get(3).bracketCode()).isEqualTo("ABOVE_50K");
    assertThat(dist.get(3).count()).isEqualTo(10L);
    assertThat(dist.get(3).percentage()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("getEconomicOutcomeSummary: derives employment mix category shares correctly")
  void testEmploymentMixShareDerivation() {
    EmploymentMixProjection mixProj = new EmploymentMixProjection(
        1000L, 850L, 600L,
        600L, 200L, 150L, 50L,
        450L, 150L,
        new BigDecimal("210.50"), new BigDecimal("150.00")
    );

    when(repository.getFormalQ10WageProgression()).thenReturn(null);
    when(repository.getSalaryDistributionCounts()).thenReturn(Collections.emptyList());
    when(repository.getFormalQ08EmploymentMix()).thenReturn(mixProj);
    when(repository.getFormalQ09CategoryBreakdown()).thenReturn(Collections.emptyList());
    when(repository.getFormalQ11CourseWageProgression()).thenReturn(Collections.emptyList());

    EconomicOutcomeSummaryResponse result = service.getEconomicOutcomeSummary();

    EmploymentMixResponse mix = result.employmentMix();
    assertThat(mix.employmentSpellCount()).isEqualTo(1000L);
    assertThat(mix.traineeCount()).isEqualTo(850L);
    assertThat(mix.currentSpellCount()).isEqualTo(600L);
    assertThat(mix.wageSpellCount()).isEqualTo(600L);
    assertThat(mix.selfEmploymentSpellCount()).isEqualTo(200L);
    assertThat(mix.apprenticeshipSpellCount()).isEqualTo(150L);
    assertThat(mix.internshipSpellCount()).isEqualTo(50L);

    // Derived shares: 600/1000 = 60%, 200/1000 = 20%, 150/1000 = 15%, 50/1000 = 5%
    assertThat(mix.wageEmploymentSharePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(mix.selfEmploymentSharePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(mix.apprenticeshipSharePct()).isEqualByComparingTo(new BigDecimal("15.00"));
    assertThat(mix.internshipSharePct()).isEqualByComparingTo(new BigDecimal("5.00"));
  }

  @Test
  @DisplayName("getEconomicOutcomeSummary: maps Q09 categories and Q11 courses with derived shares")
  void testCategoryBreakdownAndCourseWageProgression() {
    EmploymentMixProjection mixProj = new EmploymentMixProjection(
        500L, 400L, 350L, 300L, 100L, 80L, 20L, 250L, 75L,
        new BigDecimal("150.00"), new BigDecimal("100.00")
    );

    List<EmploymentCategoryBreakdownProjection> categories = List.of(
        new EmploymentCategoryBreakdownProjection(
            "WAGE_EMPLOYMENT", "Wage employment",
            true, false, false, false,
            300L, 280L, 250L, new BigDecimal("180.00")
        ),
        new EmploymentCategoryBreakdownProjection(
            "SELF_EMPLOYMENT", "Self-employment / enterprise",
            false, true, false, false,
            100L, 90L, 75L, new BigDecimal("120.00")
        )
    );

    List<CourseWageProgressionProjection> courses = List.of(
        new CourseWageProgressionProjection(
            101L, "CRS-JAVA", "Full Stack Java",
            120L, new BigDecimal("20000.00"), new BigDecimal("28000.00"), new BigDecimal("40.00")
        )
    );

    when(repository.getFormalQ10WageProgression()).thenReturn(null);
    when(repository.getSalaryDistributionCounts()).thenReturn(Collections.emptyList());
    when(repository.getFormalQ08EmploymentMix()).thenReturn(mixProj);
    when(repository.getFormalQ09CategoryBreakdown()).thenReturn(categories);
    when(repository.getFormalQ11CourseWageProgression()).thenReturn(courses);

    EconomicOutcomeSummaryResponse result = service.getEconomicOutcomeSummary();

    List<EmploymentCategoryBreakdownResponse> catList = result.categoryBreakdown();
    assertThat(catList).hasSize(2);
    // 300 / 500 = 60.00%
    assertThat(catList.get(0).engagementTypeCode()).isEqualTo("WAGE_EMPLOYMENT");
    assertThat(catList.get(0).sharePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(catList.get(0).isWageEmployment()).isTrue();
    // 100 / 500 = 20.00%
    assertThat(catList.get(1).engagementTypeCode()).isEqualTo("SELF_EMPLOYMENT");
    assertThat(catList.get(1).sharePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(catList.get(1).isSelfEmployment()).isTrue();

    List<CourseWageProgressionResponse> courseList = result.courseWageProgression();
    assertThat(courseList).hasSize(1);
    assertThat(courseList.get(0).courseCode()).isEqualTo("CRS-JAVA");
    assertThat(courseList.get(0).spellCount()).isEqualTo(120L);
    assertThat(courseList.get(0).avgSalaryChangePct()).isEqualByComparingTo(new BigDecimal("40.00"));
  }
}
