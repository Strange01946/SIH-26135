package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSeverityBreakdownResponse;
import in.gov.sih.sih26135.dto.response.TopSkillGapResponse;
import in.gov.sih.sih26135.repository.analytics.SkillGapDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSeverityMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TopSkillGapProjection;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.SkillGapDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillGapDashboardService Unit Tests")
class SkillGapDashboardServiceTest {

  @Mock
  private SkillGapDashboardRepository repository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @InjectMocks
  private SkillGapDashboardServiceImpl service;

  @Test
  @DisplayName("Q13 & Q14: Correct mapping, share percentage, and recommendation acceptance rate calculations")
  void testSkillGapDashboardMappingAndCalculations() {
    TopSkillGapProjection top1 = new TopSkillGapProjection(
        101L, "SKILL-JAVA", "Java Programming", 5L, "CRITICAL",
        150L, 120L, 80L, new BigDecimal("2.50")
    );
    TopSkillGapProjection top2 = new TopSkillGapProjection(
        102L, "SKILL-SQL", "Relational Database SQL", 3L, "MODERATE",
        100L, 90L, 40L, new BigDecimal("1.75")
    );
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of(top1, top2));

    SkillGapSeverityMixProjection sevCritical = new SkillGapSeverityMixProjection(
        "CRITICAL", "Critical", 80L, 70L, 60L, 45L // 45/60 = 75.00%
    );
    SkillGapSeverityMixProjection sevModerate = new SkillGapSeverityMixProjection(
        "MODERATE", "Moderate", 40L, 35L, 30L, 15L // 15/30 = 50.00%
    );
    SkillGapSeverityMixProjection sevLow = new SkillGapSeverityMixProjection(
        "LOW", "Low", 80L, 65L, 50L, 20L // 20/50 = 40.00%
    );
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of(sevCritical, sevModerate, sevLow));

    // Total current gaps: 80 + 40 + 80 = 200
    // Total recommendations: 60 + 30 + 50 = 140
    // Total accepted: 45 + 15 + 20 = 80 -> 80/140 = 57.14%
    // Note: Due to trainee overlap across categories (70 + 35 + 65 = 170),
    // the authoritative distinct trainee count is 150, NOT 170.
    SkillGapSummaryProjection summary = new SkillGapSummaryProjection(
        200L, 150L, 140L, 80L
    );
    when(repository.getFormalQ14OverallSummary()).thenReturn(summary);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalCurrentGaps()).isEqualTo(200L);
    assertThat(response.totalAffectedTrainees()).isEqualTo(150L);
    assertThat(response.totalRecommendations()).isEqualTo(140L);
    assertThat(response.totalAcceptedRecommendations()).isEqualTo(80L);
    assertThat(response.overallAcceptanceRatePct()).isEqualByComparingTo(new BigDecimal("57.14"));

    // Top skill gaps (Q13)
    List<TopSkillGapResponse> topGaps = response.topSkillGaps();
    assertThat(topGaps).hasSize(2);
    TopSkillGapResponse g1 = topGaps.get(0);
    assertThat(g1.skillId()).isEqualTo(101L);
    assertThat(g1.skillCode()).isEqualTo("SKILL-JAVA");
    assertThat(g1.skillName()).isEqualTo("Java Programming");
    assertThat(g1.severityCode()).isEqualTo("CRITICAL");
    assertThat(g1.gapCount()).isEqualTo(150L);
    assertThat(g1.traineeCount()).isEqualTo(120L);
    assertThat(g1.currentGapCount()).isEqualTo(80L);
    assertThat(g1.avgGapLevelDelta()).isEqualByComparingTo(new BigDecimal("2.50"));

    // Severity breakdown (Q14)
    List<SkillGapSeverityBreakdownResponse> severities = response.severityBreakdown();
    assertThat(severities).hasSize(3);

    // CRITICAL: 80 / 200 = 40.00% share; 45 / 60 = 75.00% acceptance
    SkillGapSeverityBreakdownResponse sCrit = severities.get(0);
    assertThat(sCrit.severityCode()).isEqualTo("CRITICAL");
    assertThat(sCrit.severityName()).isEqualTo("Critical");
    assertThat(sCrit.currentGapCount()).isEqualTo(80L);
    assertThat(sCrit.traineeCount()).isEqualTo(70L);
    assertThat(sCrit.sharePct()).isEqualByComparingTo(new BigDecimal("40.00"));
    assertThat(sCrit.acceptanceRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));

    // MODERATE: 40 / 200 = 20.00% share; 15 / 30 = 50.00% acceptance
    SkillGapSeverityBreakdownResponse sMod = severities.get(1);
    assertThat(sMod.severityCode()).isEqualTo("MODERATE");
    assertThat(sMod.sharePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(sMod.acceptanceRatePct()).isEqualByComparingTo(new BigDecimal("50.00"));

    // LOW: 80 / 200 = 40.00% share; 20 / 50 = 40.00% acceptance
    SkillGapSeverityBreakdownResponse sLow = severities.get(2);
    assertThat(sLow.severityCode()).isEqualTo("LOW");
    assertThat(sLow.sharePct()).isEqualByComparingTo(new BigDecimal("40.00"));
    assertThat(sLow.acceptanceRatePct()).isEqualByComparingTo(new BigDecimal("40.00"));
  }

  @Test
  @DisplayName("Overlapping Trainees: Severity-level trainee counts must NOT be summed to produce nationwide distinct total")
  void testOverlappingTraineesAcrossSeveritiesNotSummed() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());

    // Three severities where distinct trainees within each category sum to 70 + 40 + 50 = 160
    SkillGapSeverityMixProjection sevCrit = new SkillGapSeverityMixProjection(
        "CRITICAL", "Critical", 80L, 70L, 50L, 35L
    );
    SkillGapSeverityMixProjection sevMod = new SkillGapSeverityMixProjection(
        "MODERATE", "Moderate", 50L, 40L, 30L, 20L
    );
    SkillGapSeverityMixProjection sevLow = new SkillGapSeverityMixProjection(
        "LOW", "Low", 60L, 50L, 40L, 25L
    );
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of(sevCrit, sevMod, sevLow));

    // Authoritative nationwide distinct trainee count from SQL COUNT(DISTINCT trainee_id) is 125 (due to 35 multi-severity trainees)
    SkillGapSummaryProjection summary = new SkillGapSummaryProjection(
        190L, 125L, 120L, 80L
    );
    when(repository.getFormalQ14OverallSummary()).thenReturn(summary);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    // Authoritative distinct trainee count must be exactly 125, NOT the naive sum of 160
    assertThat(response.totalAffectedTrainees())
        .as("Nationwide distinct trainees must use authoritative SQL aggregate, not sum of severity-level distinct counts")
        .isEqualTo(125L)
        .isNotEqualTo(160L);

    // Verify individual severity trainee counts are preserved as measured
    assertThat(response.severityBreakdown().get(0).traineeCount()).isEqualTo(70L);
    assertThat(response.severityBreakdown().get(1).traineeCount()).isEqualTo(40L);
    assertThat(response.severityBreakdown().get(2).traineeCount()).isEqualTo(50L);
  }

  @Test
  @DisplayName("Missing Summary Projection: Does NOT produce a fabricated nationwide distinct trainee total")
  void testMissingSummaryProjectionDoesNotProduceFabricatedTraineeTotal() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());

    SkillGapSeverityMixProjection sev1 = new SkillGapSeverityMixProjection(
        "CRITICAL", "Critical", 50L, 40L, 30L, 20L
    );
    SkillGapSeverityMixProjection sev2 = new SkillGapSeverityMixProjection(
        "MODERATE", "Moderate", 30L, 25L, 20L, 10L
    );
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of(sev1, sev2));
    // Unexpectedly missing summary projection
    when(repository.getFormalQ14OverallSummary()).thenReturn(null);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    // Gaps and recommendations are partition-additive across severities
    assertThat(response.totalCurrentGaps()).isEqualTo(80L);
    assertThat(response.totalRecommendations()).isEqualTo(50L);
    assertThat(response.totalAcceptedRecommendations()).isEqualTo(30L);
    assertThat(response.overallAcceptanceRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));

    // Trainee count CANNOT be safely summed across severities; must be explicitly null rather than fabricated (40 + 25 = 65)
    assertThat(response.totalAffectedTrainees())
        .as("When summary projection is missing, nationwide distinct trainees must be null rather than a fabricated sum")
        .isNull();
  }

  @Test
  @DisplayName("Missing Summary Projection with Empty Severity Results: Returns null trainee total rather than zero")
  void testMissingSummaryProjectionWithEmptySeverityResultsReturnsNullTraineeTotal() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of());
    when(repository.getFormalQ14OverallSummary()).thenReturn(null);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalAffectedTrainees())
        .as("When aggregate summary projection is missing, totalAffectedTrainees must be null even if severity results are empty")
        .isNull();
    assertThat(response.totalCurrentGaps()).isEqualTo(0L);
    assertThat(response.totalRecommendations()).isEqualTo(0L);
    assertThat(response.totalAcceptedRecommendations()).isEqualTo(0L);
    assertThat(response.overallAcceptanceRatePct()).isNull();
    assertThat(response.topSkillGaps()).isEmpty();
    assertThat(response.severityBreakdown()).isEmpty();
  }

  @Test
  @DisplayName("Q14 Severity Reconciliation: All 5 canonical severity categories preserved and distinctly evaluated")
  void testAllFiveCanonicalSeverityCategoriesPreserved() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());

    // All 5 canonical categories from ref_skill_gap_severity:
    // 1: NONE (No gap / meets requirement)
    // 2: LOW (Low - represents Minor severity tier)
    // 3: MODERATE (Moderate)
    // 4: HIGH (High)
    // 5: CRITICAL (Critical)
    SkillGapSeverityMixProjection sevCritical = new SkillGapSeverityMixProjection(
        "CRITICAL", "Critical", 50L, 45L, 40L, 30L // 30/40 = 75.00%
    );
    SkillGapSeverityMixProjection sevHigh = new SkillGapSeverityMixProjection(
        "HIGH", "High", 40L, 38L, 35L, 21L // 21/35 = 60.00%
    );
    SkillGapSeverityMixProjection sevModerate = new SkillGapSeverityMixProjection(
        "MODERATE", "Moderate", 60L, 55L, 50L, 30L // 30/50 = 60.00%
    );
    SkillGapSeverityMixProjection sevLow = new SkillGapSeverityMixProjection(
        "LOW", "Low", 30L, 28L, 20L, 10L // 10/20 = 50.00%
    );
    SkillGapSeverityMixProjection sevNone = new SkillGapSeverityMixProjection(
        "NONE", "No gap / meets requirement", 20L, 20L, 0L, 0L // 0 recs -> null acceptance
    );

    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(
        List.of(sevCritical, sevHigh, sevModerate, sevLow, sevNone)
    );

    // Total current gaps: 50 + 40 + 60 + 30 + 20 = 200
    // Total recommendations: 40 + 35 + 50 + 20 + 0 = 145
    // Total accepted: 30 + 21 + 30 + 10 + 0 = 91 -> 91/145 = 62.76%
    SkillGapSummaryProjection summary = new SkillGapSummaryProjection(
        200L, 160L, 145L, 91L
    );
    when(repository.getFormalQ14OverallSummary()).thenReturn(summary);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalCurrentGaps()).isEqualTo(200L);
    assertThat(response.overallAcceptanceRatePct()).isEqualByComparingTo(new BigDecimal("62.76"));

    List<SkillGapSeverityBreakdownResponse> severities = response.severityBreakdown();
    assertThat(severities).hasSize(5);

    // Verify HIGH and CRITICAL remain separate categories (not merged)
    SkillGapSeverityBreakdownResponse rCrit = severities.get(0);
    assertThat(rCrit.severityCode()).isEqualTo("CRITICAL");
    assertThat(rCrit.sharePct()).isEqualByComparingTo(new BigDecimal("25.00")); // 50/200

    SkillGapSeverityBreakdownResponse rHigh = severities.get(1);
    assertThat(rHigh.severityCode()).isEqualTo("HIGH");
    assertThat(rHigh.sharePct()).isEqualByComparingTo(new BigDecimal("20.00")); // 40/200

    // Verify MODERATE
    SkillGapSeverityBreakdownResponse rMod = severities.get(2);
    assertThat(rMod.severityCode()).isEqualTo("MODERATE");
    assertThat(rMod.sharePct()).isEqualByComparingTo(new BigDecimal("30.00")); // 60/200

    // Verify LOW is preserved as "LOW" and not silently renamed
    SkillGapSeverityBreakdownResponse rLow = severities.get(3);
    assertThat(rLow.severityCode()).isEqualTo("LOW");
    assertThat(rLow.severityName()).isEqualTo("Low");
    assertThat(rLow.sharePct()).isEqualByComparingTo(new BigDecimal("15.00")); // 30/200

    // Verify NONE is preserved if present in findings
    SkillGapSeverityBreakdownResponse rNone = severities.get(4);
    assertThat(rNone.severityCode()).isEqualTo("NONE");
    assertThat(rNone.severityName()).isEqualTo("No gap / meets requirement");
    assertThat(rNone.sharePct()).isEqualByComparingTo(new BigDecimal("10.00")); // 20/200
    assertThat(rNone.recommendationCount()).isEqualTo(0L);
    assertThat(rNone.acceptanceRatePct()).isNull(); // Zero recommendations safely returns null
  }

  @Test
  @DisplayName("Empty repository results return valid zero totals, null rates, and empty collections")
  void testSkillGapDashboardEmptyResults() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of());
    // Authoritative aggregate query against empty database returns a row with zero counts
    when(repository.getFormalQ14OverallSummary()).thenReturn(new SkillGapSummaryProjection(0L, 0L, 0L, 0L));

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalCurrentGaps()).isEqualTo(0L);
    assertThat(response.totalAffectedTrainees()).isEqualTo(0L);
    assertThat(response.totalRecommendations()).isEqualTo(0L);
    assertThat(response.totalAcceptedRecommendations()).isEqualTo(0L);
    assertThat(response.overallAcceptanceRatePct()).isNull();
    assertThat(response.topSkillGaps()).isEmpty();
    assertThat(response.severityBreakdown()).isEmpty();
  }

  @Test
  @DisplayName("Zero denominators safely return null percentage without ArithmeticException")
  void testSkillGapDashboardZeroDenominatorHandling() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());
    SkillGapSeverityMixProjection sev = new SkillGapSeverityMixProjection(
        "LOW", "Low", 0L, 0L, 0L, 0L
    );
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of(sev));
    when(repository.getFormalQ14OverallSummary()).thenReturn(new SkillGapSummaryProjection(0L, 0L, 0L, 0L));

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.overallAcceptanceRatePct()).isNull();
    assertThat(response.severityBreakdown()).hasSize(1);
    assertThat(response.severityBreakdown().get(0).sharePct()).isNull();
    assertThat(response.severityBreakdown().get(0).acceptanceRatePct()).isNull();
  }
}
