package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.AttritionReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
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
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.RetentionAttritionDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetentionAttritionDashboardService Unit Tests")
class RetentionAttritionDashboardServiceTest {

  @Mock
  private RetentionAttritionDashboardRepository repository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @InjectMocks
  private RetentionAttritionDashboardServiceImpl service;

  @Test
  @DisplayName("Q12: Correct milestone-to-response mapping for 6, 12, 24, and 36 months")
  void testLongitudinalRetentionMilestoneMapping() {
    RetentionSurvivalProjection mockProjection = new RetentionSurvivalProjection(
        1000L,
        800L, 680L, new BigDecimal("85.00"),
        600L, 480L, new BigDecimal("80.00"),
        400L, 280L, new BigDecimal("70.00"),
        200L, 120L, new BigDecimal("60.00")
    );
    when(repository.getFormalQ12RetentionSurvival()).thenReturn(mockProjection);

    LongitudinalRetentionResponse response = service.getLongitudinalRetention();

    assertThat(response).isNotNull();
    assertThat(response.employmentSpellCount()).isEqualTo(1000L);
    assertThat(response.cohortSummary()).isNotNull();
    assertThat(response.cohortSummary().employmentSpellCount()).isEqualTo(1000L);

    List<RetentionMilestoneResponse> milestones = response.milestones();
    assertThat(milestones).hasSize(4);

    RetentionMilestoneResponse m6 = milestones.get(0);
    assertThat(m6.milestone()).isEqualTo("6_MONTHS");
    assertThat(m6.milestoneMonths()).isEqualTo(6);
    assertThat(m6.observableCount()).isEqualTo(800L);
    assertThat(m6.retainedCount()).isEqualTo(680L);
    assertThat(m6.retentionRatePct()).isEqualByComparingTo(new BigDecimal("85.00"));

    RetentionMilestoneResponse m12 = milestones.get(1);
    assertThat(m12.milestone()).isEqualTo("12_MONTHS");
    assertThat(m12.milestoneMonths()).isEqualTo(12);
    assertThat(m12.observableCount()).isEqualTo(600L);
    assertThat(m12.retainedCount()).isEqualTo(480L);
    assertThat(m12.retentionRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));

    RetentionMilestoneResponse m24 = milestones.get(2);
    assertThat(m24.milestone()).isEqualTo("24_MONTHS");
    assertThat(m24.milestoneMonths()).isEqualTo(24);
    assertThat(m24.observableCount()).isEqualTo(400L);
    assertThat(m24.retainedCount()).isEqualTo(280L);
    assertThat(m24.retentionRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));

    RetentionMilestoneResponse m36 = milestones.get(3);
    assertThat(m36.milestone()).isEqualTo("36_MONTHS");
    assertThat(m36.milestoneMonths()).isEqualTo(36);
    assertThat(m36.observableCount()).isEqualTo(200L);
    assertThat(m36.retainedCount()).isEqualTo(120L);
    assertThat(m36.retentionRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
  }

  @Test
  @DisplayName("Q12: Independent observable counts and retention percentage derivation when SQL rate is null")
  void testLongitudinalRetentionIndependentObservableCountsAndFallbackDerivation() {
    // Demonstrating milestones where SQL did not provide pre-computed percentage
    RetentionSurvivalProjection mockProjection = new RetentionSurvivalProjection(
        500L,
        400L, 300L, null, // 300/400 = 75.00%
        250L, 175L, null, // 175/250 = 70.00%
        100L, 50L, null,  // 50/100 = 50.00%
        0L, 0L, null      // 0 observable -> null
    );
    when(repository.getFormalQ12RetentionSurvival()).thenReturn(mockProjection);

    LongitudinalRetentionResponse response = service.getLongitudinalRetention();

    assertThat(response.milestones()).hasSize(4);
    assertThat(response.milestones().get(0).retentionRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(response.milestones().get(1).retentionRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));
    assertThat(response.milestones().get(2).retentionRatePct()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(response.milestones().get(3).retentionRatePct()).isNull();
  }

  @Test
  @DisplayName("Q12: Zero and null denominator behavior safely evaluates to null")
  void testLongitudinalRetentionZeroAndNullDenominators() {
    RetentionSurvivalProjection mockProjection = new RetentionSurvivalProjection(
        100L,
        null, null, null,
        0L, 0L, null,
        null, 10L, null,
        0L, 5L, null
    );
    when(repository.getFormalQ12RetentionSurvival()).thenReturn(mockProjection);

    LongitudinalRetentionResponse response = service.getLongitudinalRetention();

    for (RetentionMilestoneResponse m : response.milestones()) {
      assertThat(m.retentionRatePct()).isNull();
    }
  }

  @Test
  @DisplayName("Q15: Unemployment summary mapping, duration KPIs, and re-employment rate derivation")
  void testUnemploymentSummaryMappingAndReemploymentRate() {
    UnemploymentSummaryProjection mockU = new UnemploymentSummaryProjection(
        400L,
        350L,
        100L,
        240L,
        60L,
        new BigDecimal("135.25"),
        new BigDecimal("45.10")
    );
    when(repository.getFormalQ15UnemploymentSummary()).thenReturn(mockU);
    when(repository.getFormalQ16UnemploymentReasons()).thenReturn(List.of());
    when(repository.getFormalQ17AttritionReasons()).thenReturn(List.of());
    when(repository.getFormalQ18SeparationTotals()).thenReturn(new SeparationAnalysisProjection(0L, 0L, 0L, 0L));

    AttritionDashboardResponse response = service.getAttritionDashboard();

    UnemploymentSummaryResponse uSummary = response.unemploymentSummary();
    assertThat(uSummary.unemploymentPeriodCount()).isEqualTo(400L);
    assertThat(uSummary.traineeCount()).isEqualTo(350L);
    assertThat(uSummary.currentUnemploymentCount()).isEqualTo(100L);
    assertThat(uSummary.reemployedCount()).isEqualTo(240L);
    assertThat(uSummary.neverPrecededByEmploymentCount()).isEqualTo(60L);
    assertThat(uSummary.avgDurationDays()).isEqualByComparingTo(new BigDecimal("135.25"));
    assertThat(uSummary.avgOpenPeriodDurationDays()).isEqualByComparingTo(new BigDecimal("45.10"));
    // 240 / 400 * 100 = 60.00%
    assertThat(uSummary.reemploymentRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
  }

  @Test
  @DisplayName("Q16: Unemployment reasons share percentage and re-employment rate calculation")
  void testUnemploymentReasonsShareAndReemploymentRate() {
    UnemploymentSummaryProjection mockU = new UnemploymentSummaryProjection(
        300L, 280L, 70L, 150L, 40L, new BigDecimal("100.00"), new BigDecimal("30.00")
    );
    List<UnemploymentReasonProjection> mockReasons = List.of(
        new UnemploymentReasonProjection(1L, "SKILL_MISMATCH", "Skill mismatch", 150L, 140L, 30L, 90L, new BigDecimal("110.00")),
        new UnemploymentReasonProjection(2L, "LACK_OF_JOBS", "Lack of jobs", 90L, 85L, 25L, 45L, new BigDecimal("95.00")),
        new UnemploymentReasonProjection(3L, "PERSONAL", "Personal reasons", 60L, 55L, 15L, 15L, new BigDecimal("80.00"))
    );
    when(repository.getFormalQ15UnemploymentSummary()).thenReturn(mockU);
    when(repository.getFormalQ16UnemploymentReasons()).thenReturn(mockReasons);
    when(repository.getFormalQ17AttritionReasons()).thenReturn(List.of());
    when(repository.getFormalQ18SeparationTotals()).thenReturn(new SeparationAnalysisProjection(0L, 0L, 0L, 0L));

    AttritionDashboardResponse response = service.getAttritionDashboard();

    List<UnemploymentReasonBreakdownResponse> reasons = response.unemploymentReasons();
    assertThat(reasons).hasSize(3);

    // 150 / 300 = 50.00% share, 90 / 150 = 60.00% re-employment
    UnemploymentReasonBreakdownResponse r1 = reasons.get(0);
    assertThat(r1.unemploymentReasonCode()).isEqualTo("SKILL_MISMATCH");
    assertThat(r1.sharePct()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(r1.reemploymentRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));

    // 90 / 300 = 30.00% share, 45 / 90 = 50.00% re-employment
    UnemploymentReasonBreakdownResponse r2 = reasons.get(1);
    assertThat(r2.unemploymentReasonCode()).isEqualTo("LACK_OF_JOBS");
    assertThat(r2.sharePct()).isEqualByComparingTo(new BigDecimal("30.00"));
    assertThat(r2.reemploymentRatePct()).isEqualByComparingTo(new BigDecimal("50.00"));

    // 60 / 300 = 20.00% share, 15 / 60 = 25.00% re-employment
    UnemploymentReasonBreakdownResponse r3 = reasons.get(2);
    assertThat(r3.unemploymentReasonCode()).isEqualTo("PERSONAL");
    assertThat(r3.sharePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(r3.reemploymentRatePct()).isEqualByComparingTo(new BigDecimal("25.00"));
  }

  @Test
  @DisplayName("Q17: Exit reasons mapping and share percentage calculation")
  void testAttritionReasonsMappingAndShare() {
    SeparationAnalysisProjection mockSep = new SeparationAnalysisProjection(150L, 50L, 0L, 200L);
    List<AttritionReasonProjection> mockExits = List.of(
        new AttritionReasonProjection(1L, "BETTER_OPPORTUNITY", "Better opportunity", 1L, "VOLUNTARY", "Voluntary", true, false, 120L, 115L),
        new AttritionReasonProjection(2L, "EMPLOYER_TERMINATION", "Employer termination", 2L, "INVOLUNTARY", "Involuntary", false, true, 50L, 48L),
        new AttritionReasonProjection(3L, "RELOCATION", "Relocation", 1L, "VOLUNTARY", "Voluntary", true, false, 30L, 30L)
    );
    when(repository.getFormalQ15UnemploymentSummary()).thenReturn(new UnemploymentSummaryProjection(0L, 0L, 0L, 0L, 0L, null, null));
    when(repository.getFormalQ16UnemploymentReasons()).thenReturn(List.of());
    when(repository.getFormalQ17AttritionReasons()).thenReturn(mockExits);
    when(repository.getFormalQ18SeparationTotals()).thenReturn(mockSep);

    AttritionDashboardResponse response = service.getAttritionDashboard();

    List<AttritionReasonBreakdownResponse> exitReasons = response.exitReasons();
    assertThat(exitReasons).hasSize(3);

    // 120 / 200 = 60.00%
    assertThat(exitReasons.get(0).exitReasonCode()).isEqualTo("BETTER_OPPORTUNITY");
    assertThat(exitReasons.get(0).sharePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(exitReasons.get(0).isVoluntaryFlag()).isTrue();

    // 50 / 200 = 25.00%
    assertThat(exitReasons.get(1).exitReasonCode()).isEqualTo("EMPLOYER_TERMINATION");
    assertThat(exitReasons.get(1).sharePct()).isEqualByComparingTo(new BigDecimal("25.00"));
    assertThat(exitReasons.get(1).isInvoluntaryFlag()).isTrue();

    // 30 / 200 = 15.00%
    assertThat(exitReasons.get(2).exitReasonCode()).isEqualTo("RELOCATION");
    assertThat(exitReasons.get(2).sharePct()).isEqualByComparingTo(new BigDecimal("15.00"));
    assertThat(exitReasons.get(2).isVoluntaryFlag()).isTrue();
  }

  @Test
  @DisplayName("Q18: Separation analysis totals, voluntary vs involuntary ratio, and notice compliance null safety")
  void testSeparationAnalysisTotalsAndNoticeComplianceSafety() {
    SeparationAnalysisProjection mockSep = new SeparationAnalysisProjection(
        300L, 100L, 20L, 420L
    );
    when(repository.getFormalQ15UnemploymentSummary()).thenReturn(new UnemploymentSummaryProjection(0L, 0L, 0L, 0L, 0L, null, null));
    when(repository.getFormalQ16UnemploymentReasons()).thenReturn(List.of());
    when(repository.getFormalQ17AttritionReasons()).thenReturn(List.of());
    when(repository.getFormalQ18SeparationTotals()).thenReturn(mockSep);

    AttritionDashboardResponse response = service.getAttritionDashboard();

    SeparationAnalysisResponse sep = response.separationAnalysis();
    assertThat(sep.totalExitCount()).isEqualTo(420L);
    assertThat(sep.voluntaryExitCount()).isEqualTo(300L);
    assertThat(sep.involuntaryExitCount()).isEqualTo(100L);
    assertThat(sep.otherExitCount()).isEqualTo(20L);

    // 300 / 420 = 71.43%
    assertThat(sep.voluntaryExitSharePct()).isEqualByComparingTo(new BigDecimal("71.43"));
    // 100 / 420 = 23.81%
    assertThat(sep.involuntaryExitSharePct()).isEqualByComparingTo(new BigDecimal("23.81"));
    // 20 / 420 = 4.76%
    assertThat(sep.otherExitSharePct()).isEqualByComparingTo(new BigDecimal("4.76"));
    // 300 / 100 = 3.00 ratio
    assertThat(sep.voluntaryToInvoluntaryRatio()).isEqualByComparingTo(new BigDecimal("3.00"));

    // Critical verify: notice compliance is NOT fabricated; safely returns null
    assertThat(sep.noticeComplianceRatePct()).isNull();
  }

  @Test
  @DisplayName("Empty repository results handle null projections safely without throwing exceptions")
  void testEmptyRepositoryResultsSafety() {
    when(repository.getFormalQ12RetentionSurvival()).thenReturn(null);
    when(repository.getFormalQ15UnemploymentSummary()).thenReturn(null);
    when(repository.getFormalQ16UnemploymentReasons()).thenReturn(List.of());
    when(repository.getFormalQ17AttritionReasons()).thenReturn(List.of());
    when(repository.getFormalQ18SeparationTotals()).thenReturn(null);

    LongitudinalRetentionResponse retention = service.getLongitudinalRetention();
    assertThat(retention).isNotNull();
    assertThat(retention.employmentSpellCount()).isEqualTo(0L);
    assertThat(retention.milestones()).isEmpty();

    AttritionDashboardResponse attrition = service.getAttritionDashboard();
    assertThat(attrition).isNotNull();
    assertThat(attrition.unemploymentSummary().unemploymentPeriodCount()).isEqualTo(0L);
    assertThat(attrition.unemploymentReasons()).isEmpty();
    assertThat(attrition.separationAnalysis().totalExitCount()).isEqualTo(0L);
    assertThat(attrition.exitReasons()).isEmpty();
  }
}
