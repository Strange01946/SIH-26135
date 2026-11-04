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

    // Top skill gaps
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
    assertThat(g1.sectorId()).isNull();
    assertThat(g1.jobRoleId()).isNull();

    // Severity breakdown
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
  @DisplayName("Empty repository results return zero totals, null rates, and empty collections")
  void testSkillGapDashboardEmptyResults() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(List.of());
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of());
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
  @DisplayName("Fallback calculation functions safely when summary projection is null")
  void testSkillGapDashboardFallbackWhenSummaryProjectionNull() {
    when(repository.getFormalQ13TopSkillGaps()).thenReturn(null);
    SkillGapSeverityMixProjection sev = new SkillGapSeverityMixProjection(
        "CRITICAL", "Critical", 50L, 40L, 30L, 15L
    );
    when(repository.getFormalQ14SeverityAndUptakeMix()).thenReturn(List.of(sev));
    when(repository.getFormalQ14OverallSummary()).thenReturn(null);

    SkillGapDashboardResponse response = service.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalCurrentGaps()).isEqualTo(50L);
    assertThat(response.totalAffectedTrainees()).isEqualTo(40L);
    assertThat(response.totalRecommendations()).isEqualTo(30L);
    assertThat(response.totalAcceptedRecommendations()).isEqualTo(15L);
    assertThat(response.overallAcceptanceRatePct()).isEqualByComparingTo(new BigDecimal("50.00"));
    assertThat(response.topSkillGaps()).isEmpty();
    assertThat(response.severityBreakdown()).hasSize(1);
    assertThat(response.severityBreakdown().get(0).sharePct()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(response.severityBreakdown().get(0).acceptanceRatePct()).isEqualByComparingTo(new BigDecimal("50.00"));
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
