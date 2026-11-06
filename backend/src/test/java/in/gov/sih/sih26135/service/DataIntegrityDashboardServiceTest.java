package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupWavePerformanceResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyPurposePipelineResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationMethodEfficacyResponse;
import in.gov.sih.sih26135.dto.response.VerificationStatusBreakdownResponse;
import in.gov.sih.sih26135.repository.analytics.DataIntegrityDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupWaveProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyResponsePipelineProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationMethodEfficacyProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOutcomeProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOverallSummaryProjection;
import in.gov.sih.sih26135.service.impl.DataIntegrityDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataIntegrityDashboardService Unit Tests")
class DataIntegrityDashboardServiceTest {

  @Mock
  private DataIntegrityDashboardRepository repository;

  private DataIntegrityDashboardService service;

  @BeforeEach
  void setUp() {
    service = new DataIntegrityDashboardServiceImpl(repository);
  }

  // =========================================================================
  // 1. VERIFICATION DASHBOARD TESTS (Q21 & Q22)
  // =========================================================================

  @Test
  @DisplayName("Verification Dashboard: Populated data maps metrics, rates, and breakdowns correctly")
  void testVerificationDashboardMappingAndCalculations() {
    VerificationOutcomeProjection o1 = new VerificationOutcomeProjection(
        "VERIFIED", "Verified", true, 600L, 550L, 580L
    );
    VerificationOutcomeProjection o2 = new VerificationOutcomeProjection(
        "PENDING", "Pending verification", false, 200L, 190L, 195L
    );
    VerificationOutcomeProjection o3 = new VerificationOutcomeProjection(
        "REJECTED", "Rejected", false, 100L, 95L, 98L
    );
    VerificationOutcomeProjection o4 = new VerificationOutcomeProjection(
        "SELF_REPORTED", "Self-reported", false, 100L, 95L, 95L
    );
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of(o1, o2, o3, o4));

    // Current method efficacy projections (Q22)
    VerificationMethodEfficacyProjection m1 = new VerificationMethodEfficacyProjection(
        "GOVERNMENT_DATABASE", "Government / departmental database match",
        false, true, "VERIFIED", 300L, 290L
    );
    VerificationMethodEfficacyProjection m2 = new VerificationMethodEfficacyProjection(
        "GOVERNMENT_DATABASE", "Government / departmental database match",
        false, true, "PENDING", 50L, 48L
    );
    VerificationMethodEfficacyProjection m3 = new VerificationMethodEfficacyProjection(
        "TRAINEE_SELF_ATTESTATION", "Trainee self-attestation",
        true, false, "VERIFIED", 150L, 140L
    );
    VerificationMethodEfficacyProjection m4 = new VerificationMethodEfficacyProjection(
        "TRAINEE_SELF_ATTESTATION", "Trainee self-attestation",
        true, false, "REJECTED", 50L, 45L
    );
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of(m1, m2, m3, m4));

    // Total: 1000 verifications, 900 distinct employments, 850 distinct trainees
    VerificationOverallSummaryProjection summary = new VerificationOverallSummaryProjection(
        1000L, 900L, 850L, 600L, 300L, 200L, 200L, 100L, 0L
    );
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(summary);

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalVerifications()).isEqualTo(1000L);
    assertThat(response.totalEmployments()).isEqualTo(900L);
    assertThat(response.totalTrainees()).isEqualTo(850L);
    assertThat(response.verifiedCount()).isEqualTo(600L);
    assertThat(response.officialVerifiedCount()).isEqualTo(300L);
    assertThat(response.selfReportedCount()).isEqualTo(200L);
    assertThat(response.pendingCount()).isEqualTo(200L);
    assertThat(response.rejectedCount()).isEqualTo(100L);

    // Completion rate: (600 + 100) / 1000 = 70.00%
    assertThat(response.verificationCompletionRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));
    // Success rate: 600 / 1000 = 60.00%
    assertThat(response.verificationSuccessRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    // Official rate: 300 / 1000 = 30.00%
    assertThat(response.officialVerificationRatePct()).isEqualByComparingTo(new BigDecimal("30.00"));
    // Pending rate: 200 / 1000 = 20.00%
    assertThat(response.pendingRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));

    // Status breakdown assertions
    List<VerificationStatusBreakdownResponse> statuses = response.statusBreakdown();
    assertThat(statuses).hasSize(4);
    assertThat(statuses.get(0).statusCode()).isEqualTo("VERIFIED");
    assertThat(statuses.get(0).sharePct()).isEqualByComparingTo(new BigDecimal("60.00"));

    // Method efficacy assertions
    List<VerificationMethodEfficacyResponse> methods = response.methodEfficacyBreakdown();
    assertThat(methods).hasSize(2);
    // Method 1: GOVERNMENT_DATABASE (300 verified, 50 pending -> total 350, 300/350 = 85.71%)
    VerificationMethodEfficacyResponse gov = methods.get(0);
    assertThat(gov.methodCode()).isEqualTo("GOVERNMENT_DATABASE");
    assertThat(gov.isOfficial()).isTrue();
    assertThat(gov.isTraineeSelfReported()).isFalse();
    assertThat(gov.currentVerificationCount()).isEqualTo(350L);
    assertThat(gov.verifiedCount()).isEqualTo(300L);
    assertThat(gov.pendingCount()).isEqualTo(50L);
    assertThat(gov.successRatePct()).isEqualByComparingTo(new BigDecimal("85.71"));

    // Method 2: TRAINEE_SELF_ATTESTATION (150 verified, 50 rejected -> total 200, 150/200 = 75.00%)
    VerificationMethodEfficacyResponse self = methods.get(1);
    assertThat(self.methodCode()).isEqualTo("TRAINEE_SELF_ATTESTATION");
    assertThat(self.isOfficial()).isFalse();
    assertThat(self.isTraineeSelfReported()).isTrue();
    assertThat(self.currentVerificationCount()).isEqualTo(200L);
    assertThat(self.verifiedCount()).isEqualTo(150L);
    assertThat(self.rejectedCount()).isEqualTo(50L);
    assertThat(self.successRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
  }

  @Test
  @DisplayName("Verification Dashboard: Missing summary projection returns null for distinct counts (not fabricated)")
  void testVerificationDashboardMissingSummaryWithPopulatedOutcomes() {
    VerificationOutcomeProjection o1 = new VerificationOutcomeProjection(
        "VERIFIED", "Verified", true, 600L, 550L, 580L
    );
    VerificationOutcomeProjection o2 = new VerificationOutcomeProjection(
        "PENDING", "Pending verification", false, 400L, 380L, 390L
    );
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of(o1, o2));
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(null);

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalVerifications()).isEqualTo(1000L);
    assertThat(response.verifiedCount()).isEqualTo(600L);
    assertThat(response.pendingCount()).isEqualTo(400L);
    // Distinct counts must NOT be summed across statuses (550 + 380 = 930 would be fabricated)
    assertThat(response.totalEmployments()).isNull();
    assertThat(response.totalTrainees()).isNull();
  }

  @Test
  @DisplayName("Verification Dashboard: Missing summary with empty outcomes returns null for distinct counts")
  void testVerificationDashboardMissingSummaryWithEmptyOutcomes() {
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(null);

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalVerifications()).isEqualTo(0L);
    assertThat(response.totalEmployments()).isNull();
    assertThat(response.totalTrainees()).isNull();
    assertThat(response.statusBreakdown()).isEmpty();
    assertThat(response.methodEfficacyBreakdown()).isEmpty();
  }

  @Test
  @DisplayName("Verification Dashboard: Empty database with valid summary returns zeros and null rates")
  void testVerificationDashboardEmptyDatabase() {
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(
        new VerificationOverallSummaryProjection(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
    );

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalVerifications()).isEqualTo(0L);
    assertThat(response.totalEmployments()).isEqualTo(0L);
    assertThat(response.totalTrainees()).isEqualTo(0L);
    assertThat(response.verifiedCount()).isEqualTo(0L);
    assertThat(response.verificationCompletionRatePct()).isNull();
    assertThat(response.verificationSuccessRatePct()).isNull();
    assertThat(response.statusBreakdown()).isEmpty();
    assertThat(response.methodEfficacyBreakdown()).isEmpty();
  }

  // =========================================================================
  // 2. FOLLOW-UP DASHBOARD TESTS (Q19 & SLA)
  // =========================================================================

  @Test
  @DisplayName("Follow-up Dashboard: 3m, 6m, 12m waves map metrics and unconfigured SLA status correctly")
  void testFollowupDashboardMappingAndCalculations() {
    FollowupWaveProjection w3 = new FollowupWaveProjection(
        "MONTH_3", "3-month follow-up", 3, 500L, 480L, 400L, 60L, 40L, new BigDecimal("80.00")
    );
    FollowupWaveProjection w6 = new FollowupWaveProjection(
        "MONTH_6", "6-month follow-up", 6, 300L, 290L, 210L, 50L, 40L, new BigDecimal("70.00")
    );
    FollowupWaveProjection w12 = new FollowupWaveProjection(
        "MONTH_12", "12-month follow-up", 12, 200L, 195L, 120L, 50L, 30L, new BigDecimal("60.00")
    );
    when(repository.getFormalQ19FollowupSuccessByWave()).thenReturn(List.of(w3, w6, w12));

    // Total: 1000 tasks, 850 distinct trainees, 950 completed, 50 pending, 730 success, 160 no-response, 110 unreachable
    FollowupOverallSummaryProjection summary = new FollowupOverallSummaryProjection(
        1000L, 850L, 950L, 50L, 730L, 160L, 110L
    );
    when(repository.getFormalQ19OverallSummary()).thenReturn(summary);

    FollowupDashboardResponse response = service.getFollowupDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalTasks()).isEqualTo(1000L);
    assertThat(response.totalTrainees()).isEqualTo(850L);
    assertThat(response.completedTasks()).isEqualTo(950L);
    assertThat(response.pendingTasks()).isEqualTo(50L);
    assertThat(response.successfulContacts()).isEqualTo(730L);
    assertThat(response.noResponseCount()).isEqualTo(160L);
    assertThat(response.unreachableCount()).isEqualTo(110L);

    // Contact success rate: 730 / 1000 = 73.00%
    assertThat(response.contactSuccessRatePct()).isEqualByComparingTo(new BigDecimal("73.00"));
    // Unreachable rate: 110 / 1000 = 11.00%
    assertThat(response.unreachableRatePct()).isEqualByComparingTo(new BigDecimal("11.00"));
    // Completion rate: 950 / 1000 = 95.00%
    assertThat(response.taskCompletionRatePct()).isEqualByComparingTo(new BigDecimal("95.00"));

    // Wave breakdown assertions
    List<FollowupWavePerformanceResponse> waves = response.wavePerformance();
    assertThat(waves).hasSize(3);
    FollowupWavePerformanceResponse wave3 = waves.get(0);
    assertThat(wave3.typeCode()).isEqualTo("MONTH_3");
    assertThat(wave3.offsetMonths()).isEqualTo(3);
    assertThat(wave3.taskCount()).isEqualTo(500L);
    assertThat(wave3.successCount()).isEqualTo(400L);
    assertThat(wave3.unreachableRatePct()).isEqualByComparingTo(new BigDecimal("8.00")); // 40 / 500
    assertThat(wave3.sharePct()).isEqualByComparingTo(new BigDecimal("50.00")); // 500 / 1000

    // SLA performance assertions: explicitly false, unconfigured, null compliance
    assertThat(response.slaPerformance()).isNotNull();
    assertThat(response.slaPerformance().isAvailable()).isFalse();
    assertThat(response.slaPerformance().statusMessage()).contains("SLA tracking deferred");
    assertThat(response.slaPerformance().totalSlaEligibleTasks()).isNull();
    assertThat(response.slaPerformance().slaComplianceRatePct()).isNull();
  }

  @Test
  @DisplayName("Follow-up Dashboard: Missing summary projection returns null for distinct trainees")
  void testFollowupDashboardMissingSummaryWithPopulatedWaves() {
    FollowupWaveProjection w = new FollowupWaveProjection(
        "MONTH_3", "3-month follow-up", 3, 200L, 190L, 150L, 30L, 20L, new BigDecimal("75.00")
    );
    when(repository.getFormalQ19FollowupSuccessByWave()).thenReturn(List.of(w));
    when(repository.getFormalQ19OverallSummary()).thenReturn(null);

    FollowupDashboardResponse response = service.getFollowupDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalTasks()).isEqualTo(200L);
    assertThat(response.successfulContacts()).isEqualTo(150L);
    // Distinct trainees must be null rather than fabricated from wave trainee count
    assertThat(response.totalTrainees()).isNull();
  }

  @Test
  @DisplayName("Follow-up Dashboard: Empty database with valid summary returns zeros and null rates")
  void testFollowupDashboardEmptyDatabase() {
    when(repository.getFormalQ19FollowupSuccessByWave()).thenReturn(List.of());
    when(repository.getFormalQ19OverallSummary()).thenReturn(
        new FollowupOverallSummaryProjection(0L, 0L, 0L, 0L, 0L, 0L, 0L)
    );

    FollowupDashboardResponse response = service.getFollowupDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalTasks()).isEqualTo(0L);
    assertThat(response.totalTrainees()).isEqualTo(0L);
    assertThat(response.contactSuccessRatePct()).isNull();
    assertThat(response.wavePerformance()).isEmpty();
    assertThat(response.slaPerformance().isAvailable()).isFalse();
  }

  // =========================================================================
  // 3. SURVEY DASHBOARD TESTS (Q20 & SATISFACTION)
  // =========================================================================

  @Test
  @DisplayName("Survey Dashboard: Purpose pipeline maps metrics and unconfigured satisfaction status correctly")
  void testSurveyDashboardMappingAndCalculations() {
    SurveyResponsePipelineProjection p1 = new SurveyResponsePipelineProjection(
        "EMPLOYMENT_OUTCOME", "Employment outcome", null, 400L, 380L, 320L, new BigDecimal("80.00")
    );
    SurveyResponsePipelineProjection p2 = new SurveyResponsePipelineProjection(
        "RETENTION_3M", "Retention follow-up (3 months)", 3, 200L, 195L, 150L, new BigDecimal("75.00")
    );
    when(repository.getFormalQ20SurveyResponsePipeline()).thenReturn(List.of(p1, p2));

    SurveyOverallSummaryProjection summary = new SurveyOverallSummaryProjection(
        600L, 520L, 470L, 130L
    );
    when(repository.getFormalQ20OverallSummary()).thenReturn(summary);

    SurveyDashboardResponse response = service.getSurveyDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalResponseRows()).isEqualTo(600L);
    assertThat(response.totalTrainees()).isEqualTo(520L);
    assertThat(response.submittedCount()).isEqualTo(470L);
    assertThat(response.pendingCount()).isEqualTo(130L);

    // Overall submission rate: 470 / 600 = 78.33%
    assertThat(response.overallSubmissionRatePct()).isEqualByComparingTo(new BigDecimal("78.33"));

    // Purpose pipeline assertions
    List<SurveyPurposePipelineResponse> pipeline = response.purposePipeline();
    assertThat(pipeline).hasSize(2);
    SurveyPurposePipelineResponse item1 = pipeline.get(0);
    assertThat(item1.purposeCode()).isEqualTo("EMPLOYMENT_OUTCOME");
    assertThat(item1.responseRowCount()).isEqualTo(400L);
    assertThat(item1.submittedCount()).isEqualTo(320L);
    assertThat(item1.pendingCount()).isEqualTo(80L);
    assertThat(item1.submissionRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(item1.sharePct()).isEqualByComparingTo(new BigDecimal("66.67")); // 400 / 600

    // Satisfaction assertions: explicitly false, unconfigured, null ratings
    assertThat(response.satisfactionPerformance()).isNotNull();
    assertThat(response.satisfactionPerformance().isAvailable()).isFalse();
    assertThat(response.satisfactionPerformance().statusMessage()).contains("Satisfaction rating metrics deferred");
    assertThat(response.satisfactionPerformance().averageRating()).isNull();
    assertThat(response.satisfactionPerformance().totalRatingsCount()).isNull();
  }

  @Test
  @DisplayName("Survey Dashboard: Missing summary projection returns null for distinct trainees")
  void testSurveyDashboardMissingSummaryWithPopulatedPipeline() {
    SurveyResponsePipelineProjection p = new SurveyResponsePipelineProjection(
        "EMPLOYMENT_OUTCOME", "Employment outcome", null, 300L, 280L, 240L, new BigDecimal("80.00")
    );
    when(repository.getFormalQ20SurveyResponsePipeline()).thenReturn(List.of(p));
    when(repository.getFormalQ20OverallSummary()).thenReturn(null);

    SurveyDashboardResponse response = service.getSurveyDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalResponseRows()).isEqualTo(300L);
    assertThat(response.submittedCount()).isEqualTo(240L);
    assertThat(response.totalTrainees()).isNull();
  }

  @Test
  @DisplayName("Survey Dashboard: Empty database with valid summary returns zeros and null rates")
  void testSurveyDashboardEmptyDatabase() {
    when(repository.getFormalQ20SurveyResponsePipeline()).thenReturn(List.of());
    when(repository.getFormalQ20OverallSummary()).thenReturn(
        new SurveyOverallSummaryProjection(0L, 0L, 0L, 0L)
    );

    SurveyDashboardResponse response = service.getSurveyDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalResponseRows()).isEqualTo(0L);
    assertThat(response.totalTrainees()).isEqualTo(0L);
    assertThat(response.submittedCount()).isEqualTo(0L);
    assertThat(response.overallSubmissionRatePct()).isNull();
    assertThat(response.purposePipeline()).isEmpty();
    assertThat(response.satisfactionPerformance().isAvailable()).isFalse();
  }

  @Test
  @DisplayName("Zero Denominators: Safely return null without ArithmeticException across all dashboards")
  void testZeroDenominatorHandlingAcrossAllDashboards() {
    // 1. Verification with zero verifications
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of(
        new VerificationOutcomeProjection("VERIFIED", "Verified", true, 0L, 0L, 0L)
    ));
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of(
        new VerificationMethodEfficacyProjection("METHOD_1", "Method 1", false, true, "VERIFIED", 0L, 0L)
    ));
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(
        new VerificationOverallSummaryProjection(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
    );
    VerificationDashboardResponse vResp = service.getVerificationDashboard();
    assertThat(vResp.verificationSuccessRatePct()).isNull();
    assertThat(vResp.statusBreakdown().get(0).sharePct()).isNull();
    assertThat(vResp.methodEfficacyBreakdown().get(0).successRatePct()).isNull();

    // 2. Followup with zero tasks
    when(repository.getFormalQ19FollowupSuccessByWave()).thenReturn(List.of(
        new FollowupWaveProjection("MONTH_3", "3-month", 3, 0L, 0L, 0L, 0L, 0L, null)
    ));
    when(repository.getFormalQ19OverallSummary()).thenReturn(
        new FollowupOverallSummaryProjection(0L, 0L, 0L, 0L, 0L, 0L, 0L)
    );
    FollowupDashboardResponse fResp = service.getFollowupDashboard();
    assertThat(fResp.contactSuccessRatePct()).isNull();
    assertThat(fResp.wavePerformance().get(0).unreachableRatePct()).isNull();

    // 3. Survey with zero response rows
    when(repository.getFormalQ20SurveyResponsePipeline()).thenReturn(List.of(
        new SurveyResponsePipelineProjection("PURPOSE_1", "Purpose 1", null, 0L, 0L, 0L, null)
    ));
    when(repository.getFormalQ20OverallSummary()).thenReturn(
        new SurveyOverallSummaryProjection(0L, 0L, 0L, 0L)
    );
    SurveyDashboardResponse sResp = service.getSurveyDashboard();
    assertThat(sResp.overallSubmissionRatePct()).isNull();
    assertThat(sResp.purposePipeline().get(0).sharePct()).isNull();
  }
}
