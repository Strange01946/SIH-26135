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

    // Current method efficacy projections (Q22) with database-side distinct trainee counting
    VerificationMethodEfficacyProjection m1 = new VerificationMethodEfficacyProjection(
        "GOVERNMENT_DATABASE", "Government / departmental database match",
        false, true, 350L, 320L, 300L, 50L, 0L
    );
    VerificationMethodEfficacyProjection m2 = new VerificationMethodEfficacyProjection(
        "TRAINEE_SELF_ATTESTATION", "Trainee self-attestation",
        true, false, 200L, 185L, 150L, 0L, 50L
    );
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of(m1, m2));

    // Total: 1000 verifications (attempts), 900 distinct employments (claims), 850 distinct trainees
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

    // Headline claim rates divide by total distinct employments (claims population = 900)
    // Completion rate: (600 + 100) / 900 = 77.78%
    assertThat(response.verificationCompletionRatePct()).isEqualByComparingTo(new BigDecimal("77.78"));
    // Success rate: 600 / 900 = 66.67%
    assertThat(response.verificationSuccessRatePct()).isEqualByComparingTo(new BigDecimal("66.67"));
    // Official rate: 300 / 900 = 33.33%
    assertThat(response.officialVerificationRatePct()).isEqualByComparingTo(new BigDecimal("33.33"));
    // Pending rate: 200 / 900 = 22.22%
    assertThat(response.pendingRatePct()).isEqualByComparingTo(new BigDecimal("22.22"));
    // Self-reported rate: 200 / 900 = 22.22%
    assertThat(response.selfReportedRatePct()).isEqualByComparingTo(new BigDecimal("22.22"));

    // Status breakdown assertions
    List<VerificationStatusBreakdownResponse> statuses = response.statusBreakdown();
    assertThat(statuses).hasSize(4);
    assertThat(statuses.get(0).statusCode()).isEqualTo("VERIFIED");
    assertThat(statuses.get(0).sharePct()).isEqualByComparingTo(new BigDecimal("60.00"));

    // Method efficacy assertions
    List<VerificationMethodEfficacyResponse> methods = response.methodEfficacyBreakdown();
    assertThat(methods).hasSize(2);
    // Method 1: GOVERNMENT_DATABASE (300 verified, 50 pending, 0 rejected -> total 350, 300/350 = 85.71%)
    VerificationMethodEfficacyResponse gov = methods.get(0);
    assertThat(gov.methodCode()).isEqualTo("GOVERNMENT_DATABASE");
    assertThat(gov.isOfficial()).isTrue();
    assertThat(gov.isTraineeSelfReported()).isFalse();
    assertThat(gov.currentVerificationCount()).isEqualTo(350L);
    assertThat(gov.traineeCount()).isEqualTo(320L);
    assertThat(gov.verifiedCount()).isEqualTo(300L);
    assertThat(gov.pendingCount()).isEqualTo(50L);
    assertThat(gov.rejectedCount()).isEqualTo(0L);
    assertThat(gov.successRatePct()).isEqualByComparingTo(new BigDecimal("85.71"));
    assertThat(gov.sharePct()).isEqualByComparingTo(new BigDecimal("63.64"));

    // Method 2: TRAINEE_SELF_ATTESTATION (150 verified, 0 pending, 50 rejected -> total 200, 150/200 = 75.00%)
    VerificationMethodEfficacyResponse self = methods.get(1);
    assertThat(self.methodCode()).isEqualTo("TRAINEE_SELF_ATTESTATION");
    assertThat(self.isOfficial()).isFalse();
    assertThat(self.isTraineeSelfReported()).isTrue();
    assertThat(self.currentVerificationCount()).isEqualTo(200L);
    assertThat(self.traineeCount()).isEqualTo(185L);
    assertThat(self.verifiedCount()).isEqualTo(150L);
    assertThat(self.pendingCount()).isEqualTo(0L);
    assertThat(self.rejectedCount()).isEqualTo(50L);
    assertThat(self.successRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(self.sharePct()).isEqualByComparingTo(new BigDecimal("36.36"));
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

    // The service must NEVER silently substitute totalVerifications (1000L) as claimRateDenominator
    // Since current claim population is unobservable without summary, claim rates must be strictly null
    assertThat(response.verificationSuccessRatePct()).isNull();
    assertThat(response.verificationCompletionRatePct()).isNull();
    assertThat(response.officialVerificationRatePct()).isNull();
    assertThat(response.selfReportedRatePct()).isNull();
    assertThat(response.pendingRatePct()).isNull();
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

  @Test
  @DisplayName("Verification Integrity: Historical re-verification attempts do not inflate current claim counts or distort rates")
  void testHistoricalVerificationAttemptsDoNotInflateCurrentClaims() {
    // 1500 total verification attempts recorded historically across lifecycle re-verification cycles
    // 1000 distinct employment records (current claims where is_current = 1)
    // 950 distinct trainees
    // Current claim counts reflect ONLY is_current = 1 records (superseded attempts excluded)
    VerificationOverallSummaryProjection summary = new VerificationOverallSummaryProjection(
        1500L, // total_verifications (historical attempts across all cycles)
        1000L, // total_employments (current claims population)
        950L,  // total_trainees
        600L,  // verified_count (current is_current = 1 verified)
        300L,  // official_verified_count (current is_current = 1 official)
        200L,  // self_reported_count (current is_current = 1 self-reported)
        200L,  // pending_count (current is_current = 1 pending)
        150L,  // rejected_count (current is_current = 1 rejected)
        50L    // unverified_count (current is_current = 1 unverified)
    );
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(summary);
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    // Verification attempt history preserved without loss
    assertThat(response.totalVerifications()).isEqualTo(1500L);
    // Current claim population correctly identified
    assertThat(response.totalEmployments()).isEqualTo(1000L);
    assertThat(response.totalTrainees()).isEqualTo(950L);

    // Current claim status counts reflect current state only (is_current = 1)
    assertThat(response.verifiedCount()).isEqualTo(600L);
    assertThat(response.officialVerifiedCount()).isEqualTo(300L);
    assertThat(response.selfReportedCount()).isEqualTo(200L);
    assertThat(response.pendingCount()).isEqualTo(200L);
    assertThat(response.rejectedCount()).isEqualTo(150L);
    assertThat(response.unverifiedCount()).isEqualTo(50L);

    // Headline claim rates divide by current claims population (totalEmployments = 1000L)
    // Success rate: 600 / 1000 = 60.00% (NOT 600 / 1500 = 40.00%)
    assertThat(response.verificationSuccessRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    // Completion rate: (600 + 150) / 1000 = 75.00% (NOT 750 / 1500 = 50.00%)
    assertThat(response.verificationCompletionRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    // Official rate: 300 / 1000 = 30.00% (NOT 300 / 1500 = 20.00%)
    assertThat(response.officialVerificationRatePct()).isEqualByComparingTo(new BigDecimal("30.00"));
    // Pending rate: 200 / 1000 = 20.00% (NOT 200 / 1500 = 13.33%)
    assertThat(response.pendingRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    // Self-reported rate: 200 / 1000 = 20.00%
    assertThat(response.selfReportedRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));
  }

  @Test
  @DisplayName("Verification Integrity: Method efficacy distinct trainee count uses exact database COUNT(DISTINCT) without Math.max or sum")
  void testMethodEfficacyDistinctTraineeCountNeverUsesMathMaxOrNaiveSum() {
    // 300 current verifications under DOCUMENT_REVIEW, with 200 verified, 70 pending, 30 rejected
    // Exact database COUNT(DISTINCT v.trainee_id) is 285 (some trainees have multiple employments)
    VerificationMethodEfficacyProjection proj = new VerificationMethodEfficacyProjection(
        "DOCUMENT_REVIEW",
        "Physical/digital document review",
        false, // isTraineeSelfReported
        true,  // isOfficialFlag
        300L,  // currentVerificationCount
        285L,  // traineeCount (authoritative database distinct count)
        200L,  // verifiedCount
        70L,   // pendingCount
        30L    // rejectedCount
    );
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of(proj));
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(
        new VerificationOverallSummaryProjection(300L, 300L, 285L, 200L, 200L, 0L, 70L, 30L, 0L)
    );

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.methodEfficacyBreakdown()).hasSize(1);

    VerificationMethodEfficacyResponse methodResp = response.methodEfficacyBreakdown().get(0);
    assertThat(methodResp.methodCode()).isEqualTo("DOCUMENT_REVIEW");
    assertThat(methodResp.currentVerificationCount()).isEqualTo(300L);
    // Trainee count must equal the exact database distinct trainee count (285)
    // It must NOT be the naive sum of status-level verifications (300)
    // and must NOT be an erroneous Math.max across statuses (e.g. 200)
    assertThat(methodResp.traineeCount()).isEqualTo(285L);
    assertThat(methodResp.traineeCount()).isNotEqualTo(300L);
    assertThat(methodResp.traineeCount()).isNotEqualTo(200L);
    assertThat(methodResp.verifiedCount()).isEqualTo(200L);
    assertThat(methodResp.pendingCount()).isEqualTo(70L);
    assertThat(methodResp.rejectedCount()).isEqualTo(30L);
    assertThat(methodResp.successRatePct()).isEqualByComparingTo(new BigDecimal("66.67"));
  }

  @Test
  @DisplayName("Verification Denominator Integrity: Historical-only employments excluded from claim denominator and attempts never substituted")
  void testHistoricalOnlyEmploymentsExcludedFromCurrentClaimDenominatorAndNeverSubstitutesAttemptCount() {
    // Scenario:
    // - 1200 total verification attempt records across all cycles (including historical-only employments)
    // - 1000 active employment records with current verification claim (is_current = 1)
    // - 100 historical-only employment records exist (is_current = 0 only; no is_current = 1 record)
    //   They contribute to totalVerifications (1200L), but must NOT be included in totalEmployments (1000L)
    // - 900 distinct trainees with current claims
    // - Current status counts: 600 VERIFIED, 250 PENDING, 100 REJECTED, 50 UNVERIFIED (sum = 1000 = totalEmployments)
    VerificationOverallSummaryProjection summary = new VerificationOverallSummaryProjection(
        1200L, // totalVerifications (all cycles, including historical-only)
        1000L, // totalEmployments (strictly distinct employments with is_current = 1)
        900L,  // totalTrainees (strictly distinct trainees with is_current = 1)
        600L,  // verifiedCount (is_current = 1)
        300L,  // officialVerifiedCount
        200L,  // selfReportedCount
        250L,  // pendingCount (is_current = 1)
        100L,  // rejectedCount (is_current = 1)
        50L    // unverifiedCount (is_current = 1)
    );
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(summary);
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    // Historical attempt count preserved without row loss
    assertThat(response.totalVerifications()).isEqualTo(1200L);
    // Denominator captures only active/current employment claims (1000, excluding the 100 historical-only records)
    assertThat(response.totalEmployments()).isEqualTo(1000L);
    assertThat(response.totalTrainees()).isEqualTo(900L);

    // Current claim rates must divide by totalEmployments (1000L), NOT totalVerifications (1200L)
    // and NOT an inflated denominator including historical-only employments (e.g. 1100L)
    // Success rate: 600 / 1000 = 60.00% (NOT 600 / 1200 = 50.00% and NOT 600 / 1100 = 54.55%)
    assertThat(response.verificationSuccessRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    // Completion rate: (600 + 100) / 1000 = 70.00% (NOT 700 / 1200 = 58.33% and NOT 700 / 1100 = 63.64%)
    assertThat(response.verificationCompletionRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));
    // Official rate: 300 / 1000 = 30.00%
    assertThat(response.officialVerificationRatePct()).isEqualByComparingTo(new BigDecimal("30.00"));
    // Self-reported rate: 200 / 1000 = 20.00%
    assertThat(response.selfReportedRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    // Pending rate: 250 / 1000 = 25.00% (NOT 250 / 1200 = 20.83% and NOT 250 / 1100 = 22.73%)
    assertThat(response.pendingRatePct()).isEqualByComparingTo(new BigDecimal("25.00"));
  }

  @Test
  @DisplayName("Verification Denominator Integrity: Zero current employments with non-zero historical attempts returns null rates")
  void testZeroCurrentEmploymentsWithHistoricalAttemptsReturnsNullRates() {
    // 500 historical verification rows exist, but 0 employments have a current verification (is_current = 1)
    VerificationOverallSummaryProjection summary = new VerificationOverallSummaryProjection(
        500L, // totalVerifications
        0L,   // totalEmployments (0 current claims)
        0L,   // totalTrainees
        0L, 0L, 0L, 0L, 0L, 0L
    );
    when(repository.getFormalQ21Q22OverallSummary()).thenReturn(summary);
    when(repository.getFormalQ21VerificationOutcomes()).thenReturn(List.of());
    when(repository.getFormalQ22VerificationMethodEfficacy()).thenReturn(List.of());

    VerificationDashboardResponse response = service.getVerificationDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalVerifications()).isEqualTo(500L);
    assertThat(response.totalEmployments()).isEqualTo(0L);
    assertThat(response.totalTrainees()).isEqualTo(0L);

    // The service must NEVER substitute totalVerifications (500L) as the denominator
    assertThat(response.verificationSuccessRatePct()).isNull();
    assertThat(response.verificationCompletionRatePct()).isNull();
    assertThat(response.officialVerificationRatePct()).isNull();
    assertThat(response.selfReportedRatePct()).isNull();
    assertThat(response.pendingRatePct()).isNull();
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
        new VerificationMethodEfficacyProjection("METHOD_1", "Method 1", false, true, 0L, 0L, 0L, 0L, 0L)
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
