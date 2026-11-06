package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.ExecutiveDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.DashboardKpiProjection;
import in.gov.sih.sih26135.repository.analytics.projection.PipelineFunnelProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SchemeProgramPerformanceProjection;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.ExecutiveDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutiveDashboardService Unit Tests")
class ExecutiveDashboardServiceTest {

  @Mock
  private ExecutiveDashboardRepository executiveDashboardRepository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @InjectMocks
  private ExecutiveDashboardServiceImpl executiveDashboardService;

  @Nested
  @DisplayName("Q01 Dashboard KPI Strip Tests")
  class DashboardKpiStripTests {

    @Test
    @DisplayName("getDashboardKpiStrip: maps all 12 counts and calculates rates")
    void testGetDashboardKpiStripNormal() {
      DashboardKpiProjection proj = mock(DashboardKpiProjection.class);
      when(proj.getTraineeCount()).thenReturn(1000L);
      when(proj.getEnrollmentCount()).thenReturn(1200L);
      when(proj.getEnrolledTraineeCount()).thenReturn(950L);
      when(proj.getCompletedEnrollmentCount()).thenReturn(800L);
      when(proj.getCertifiedEnrollmentCount()).thenReturn(750L);
      when(proj.getPlacementCount()).thenReturn(600L);
      when(proj.getJoinedPlacementCount()).thenReturn(550L);
      when(proj.getEmploymentSpellCount()).thenReturn(520L);
      when(proj.getEmployedTraineeCount()).thenReturn(500L);
      when(proj.getCurrentEmploymentSpellCount()).thenReturn(480L);
      when(proj.getCurrentSelfEmploymentCount()).thenReturn(30L);
      when(proj.getCurrentApprenticeshipCount()).thenReturn(10L);

      when(executiveDashboardRepository.getExecutiveDashboardKpiStrip()).thenReturn(proj);

      DashboardKpiStripResponse response = executiveDashboardService.getDashboardKpiStrip();

      assertThat(response.traineeCount()).isEqualTo(1000L);
      assertThat(response.enrollmentCount()).isEqualTo(1200L);
      assertThat(response.enrolledTraineeCount()).isEqualTo(950L);
      assertThat(response.completedEnrollmentCount()).isEqualTo(800L);
      assertThat(response.certifiedEnrollmentCount()).isEqualTo(750L);
      assertThat(response.placementCount()).isEqualTo(600L);
      assertThat(response.joinedPlacementCount()).isEqualTo(550L);
      assertThat(response.employmentSpellCount()).isEqualTo(520L);
      assertThat(response.employedTraineeCount()).isEqualTo(500L);
      assertThat(response.currentEmploymentSpellCount()).isEqualTo(480L);
      assertThat(response.currentSelfEmploymentCount()).isEqualTo(30L);
      assertThat(response.currentApprenticeshipCount()).isEqualTo(10L);

      // Rates: 800/1200*100 = 66.67%, 750/1200*100 = 62.50%, 550/1200*100 = 45.83%, 500/1200*100 = 41.67%
      assertThat(response.completionRatePct()).isEqualByComparingTo(new BigDecimal("66.67"));
      assertThat(response.certificationRatePct()).isEqualByComparingTo(new BigDecimal("62.50"));
      assertThat(response.placementRatePct()).isEqualByComparingTo(new BigDecimal("45.83"));
      assertThat(response.employmentRatePct()).isEqualByComparingTo(new BigDecimal("41.67"));
    }

    @Test
    @DisplayName("getDashboardKpiStrip: null projection returns safe zeros and null rates")
    void testGetDashboardKpiStripNullProjection() {
      when(executiveDashboardRepository.getExecutiveDashboardKpiStrip()).thenReturn(null);

      DashboardKpiStripResponse response = executiveDashboardService.getDashboardKpiStrip();

      assertThat(response.traineeCount()).isEqualTo(0L);
      assertThat(response.enrollmentCount()).isEqualTo(0L);
      assertThat(response.completedEnrollmentCount()).isEqualTo(0L);
      assertThat(response.completionRatePct()).isNull();
      assertThat(response.certificationRatePct()).isNull();
      assertThat(response.placementRatePct()).isNull();
      assertThat(response.employmentRatePct()).isNull();
    }
  }

  @Nested
  @DisplayName("Q02 Training Pipeline Conversion Funnel Tests")
  class TrainingPipelineFunnelTests {

    @Test
    @DisplayName("getTrainingPipelineFunnel: maps stages and preserves SQL conversion rates")
    void testGetTrainingPipelineFunnelNormal() {
      PipelineFunnelProjection proj = mock(PipelineFunnelProjection.class);
      when(proj.getEnrollmentCount()).thenReturn(5000L);
      when(proj.getCompletedCount()).thenReturn(4200L);
      when(proj.getCertifiedCount()).thenReturn(3800L);
      when(proj.getJoinedPlacementCount()).thenReturn(3000L);
      when(proj.getEmployedCount()).thenReturn(2800L);
      when(proj.getCompletionRatePct()).thenReturn(new BigDecimal("84.00"));
      when(proj.getCertificationRatePct()).thenReturn(new BigDecimal("76.00"));
      when(proj.getPlacementRatePct()).thenReturn(new BigDecimal("60.00"));
      when(proj.getEmploymentRatePct()).thenReturn(new BigDecimal("56.00"));

      when(executiveDashboardRepository.getTrainingPipelineConversionFunnel()).thenReturn(proj);

      PipelineFunnelResponse response = executiveDashboardService.getTrainingPipelineFunnel();

      assertThat(response.enrollmentCount()).isEqualTo(5000L);
      assertThat(response.completedCount()).isEqualTo(4200L);
      assertThat(response.certifiedCount()).isEqualTo(3800L);
      assertThat(response.joinedPlacementCount()).isEqualTo(3000L);
      assertThat(response.employedCount()).isEqualTo(2800L);

      assertThat(response.completionRatePct()).isEqualByComparingTo(new BigDecimal("84.00"));
      assertThat(response.certificationRatePct()).isEqualByComparingTo(new BigDecimal("76.00"));
      assertThat(response.placementRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
      assertThat(response.employmentRatePct()).isEqualByComparingTo(new BigDecimal("56.00"));
    }

    @Test
    @DisplayName("getTrainingPipelineFunnel: zero enrollment count yields null rates")
    void testGetTrainingPipelineFunnelZeroEnrollment() {
      PipelineFunnelProjection proj = mock(PipelineFunnelProjection.class);
      when(proj.getEnrollmentCount()).thenReturn(0L);
      when(proj.getCompletedCount()).thenReturn(0L);
      when(proj.getCertifiedCount()).thenReturn(0L);
      when(proj.getJoinedPlacementCount()).thenReturn(0L);
      when(proj.getEmployedCount()).thenReturn(0L);
      when(proj.getCompletionRatePct()).thenReturn(null);
      when(proj.getCertificationRatePct()).thenReturn(null);
      when(proj.getPlacementRatePct()).thenReturn(null);
      when(proj.getEmploymentRatePct()).thenReturn(null);

      when(executiveDashboardRepository.getTrainingPipelineConversionFunnel()).thenReturn(proj);

      PipelineFunnelResponse response = executiveDashboardService.getTrainingPipelineFunnel();

      assertThat(response.enrollmentCount()).isEqualTo(0L);
      assertThat(response.completionRatePct()).isNull();
      assertThat(response.certificationRatePct()).isNull();
      assertThat(response.placementRatePct()).isNull();
      assertThat(response.employmentRatePct()).isNull();
    }
  }

  @Nested
  @DisplayName("Q03 Scheme & Program Performance Matrix Tests")
  class SchemeProgramMatrixTests {

    @Test
    @DisplayName("getSchemeProgramPerformanceMatrix: maps projections to ranked leaderboard")
    void testGetSchemeProgramMatrixNormal() {
      SchemeProgramPerformanceProjection p1 = mock(SchemeProgramPerformanceProjection.class);
      when(p1.getProgramId()).thenReturn(101L);
      when(p1.getProgramCode()).thenReturn("PRG-01");
      when(p1.getProgramName()).thenReturn("Solar Tech");
      when(p1.getSchemeId()).thenReturn(1L);
      when(p1.getSchemeCode()).thenReturn("PMKVY");
      when(p1.getSchemeName()).thenReturn("Pradhan Mantri Kaushal Vikas Yojana");
      when(p1.getEnrollmentCount()).thenReturn(500L);
      when(p1.getTraineeCount()).thenReturn(480L);
      when(p1.getCompletedCount()).thenReturn(450L);
      when(p1.getCertifiedCount()).thenReturn(430L);
      when(p1.getJoinedPlacementCount()).thenReturn(350L);
      when(p1.getEmployedCount()).thenReturn(320L);
      when(p1.getSelfEmploymentSpellCount()).thenReturn(20L);
      when(p1.getApprenticeshipSpellCount()).thenReturn(10L);
      when(p1.getCompletionRatePct()).thenReturn(new BigDecimal("90.00"));
      when(p1.getCertificationRatePct()).thenReturn(new BigDecimal("86.00"));
      when(p1.getPlacementRatePct()).thenReturn(new BigDecimal("70.00"));
      when(p1.getEmploymentRatePct()).thenReturn(new BigDecimal("64.00"));

      when(executiveDashboardRepository.getSchemeProgramPerformanceMatrix()).thenReturn(List.of(p1));

      LeaderboardResponse<SchemeProgramPerformanceResponse> response =
          executiveDashboardService.getSchemeProgramPerformanceMatrix();

      assertThat(response.rankingMetric()).isEqualTo("employmentRatePct");
      assertThat(response.totalItems()).isEqualTo(1L);
      assertThat(response.items()).hasSize(1);

      SchemeProgramPerformanceResponse item = response.items().get(0);
      assertThat(item.programId()).isEqualTo(101L);
      assertThat(item.programCode()).isEqualTo("PRG-01");
      assertThat(item.programName()).isEqualTo("Solar Tech");
      assertThat(item.schemeId()).isEqualTo(1L);
      assertThat(item.schemeCode()).isEqualTo("PMKVY");
      assertThat(item.schemeName()).isEqualTo("Pradhan Mantri Kaushal Vikas Yojana");
      assertThat(item.enrollmentCount()).isEqualTo(500L);
      assertThat(item.traineeCount()).isEqualTo(480L);
      assertThat(item.completedCount()).isEqualTo(450L);
      assertThat(item.certifiedCount()).isEqualTo(430L);
      assertThat(item.joinedPlacementCount()).isEqualTo(350L);
      assertThat(item.employedCount()).isEqualTo(320L);
      assertThat(item.selfEmploymentSpellCount()).isEqualTo(20L);
      assertThat(item.apprenticeshipSpellCount()).isEqualTo(10L);
      assertThat(item.completionRatePct()).isEqualByComparingTo(new BigDecimal("90.00"));
      assertThat(item.employmentRatePct()).isEqualByComparingTo(new BigDecimal("64.00"));
    }

    @Test
    @DisplayName("getSchemeProgramPerformanceMatrix: empty results return empty leaderboard")
    void testGetSchemeProgramMatrixEmpty() {
      when(executiveDashboardRepository.getSchemeProgramPerformanceMatrix()).thenReturn(Collections.emptyList());

      LeaderboardResponse<SchemeProgramPerformanceResponse> response =
          executiveDashboardService.getSchemeProgramPerformanceMatrix();

      assertThat(response.rankingMetric()).isEqualTo("employmentRatePct");
      assertThat(response.totalItems()).isEqualTo(0L);
      assertThat(response.items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Consolidated Executive Dashboard Tests")
  class ExecutiveDashboardConsolidatedTests {

    @Test
    @DisplayName("getExecutiveDashboard: aggregates Q01, Q02, and Q03 into ExecutiveDashboardResponse")
    void testGetExecutiveDashboard() {
      DashboardKpiProjection kpiProj = mock(DashboardKpiProjection.class);
      when(kpiProj.getTraineeCount()).thenReturn(200L);
      when(kpiProj.getEnrollmentCount()).thenReturn(250L);
      when(executiveDashboardRepository.getExecutiveDashboardKpiStrip()).thenReturn(kpiProj);

      PipelineFunnelProjection funnelProj = mock(PipelineFunnelProjection.class);
      when(funnelProj.getEnrollmentCount()).thenReturn(250L);
      when(executiveDashboardRepository.getTrainingPipelineConversionFunnel()).thenReturn(funnelProj);

      when(executiveDashboardRepository.getSchemeProgramPerformanceMatrix()).thenReturn(Collections.emptyList());

      ExecutiveDashboardResponse response = executiveDashboardService.getExecutiveDashboard();

      assertThat(response).isNotNull();
      assertThat(response.kpiStrip()).isNotNull();
      assertThat(response.kpiStrip().traineeCount()).isEqualTo(200L);
      assertThat(response.pipelineFunnel()).isNotNull();
      assertThat(response.pipelineFunnel().enrollmentCount()).isEqualTo(250L);
      assertThat(response.schemeProgramMatrix()).isNotNull();
      assertThat(response.schemeProgramMatrix().totalItems()).isEqualTo(0L);
    }
  }
}
