package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.dto.response.RetentionMilestoneResponse;
import in.gov.sih.sih26135.repository.analytics.RetentionAttritionDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.AttritionReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.RetentionSurvivalProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SeparationAnalysisProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentReasonProjection;
import in.gov.sih.sih26135.repository.analytics.projection.UnemploymentSummaryProjection;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Live database integration tests for Longitudinal Retention Survival, Unemployment Duration,
 * Re-employment Conversion, and Employment Attrition Dashboards.
 *
 * <p>Verifies exact formal queries Q12, Q15, Q16, Q17, and Q18 execution, mathematical reconciliation,
 * and REST endpoint behavior against dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RetentionAttritionDashboard Live Database Integration Tests (SIH26135_TEST)")
class RetentionAttritionDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private RetentionAttritionDashboardService retentionAttritionDashboardService;

  @Autowired
  private RetentionAttritionDashboardRepository retentionAttritionDashboardRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String tokenAnalyticsRead;

  @BeforeEach
  void enforceDatabaseIsolationGuardAndSetupTokens() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "executive",
        List.of("analytics.read")
    );
  }

  @Test
  @DisplayName("Repository: execute formal Q12 retention query against live SIH26135_TEST")
  void testLiveFormalQ12Execution() {
    RetentionSurvivalProjection retention =
        retentionAttritionDashboardRepository.getFormalQ12RetentionSurvival();

    assertThat(retention).isNotNull();
    assertThat(retention.employmentSpellCount()).isNotNull().isGreaterThanOrEqualTo(0L);

    // Verify observable count invariant: 6m observable >= 12m observable >= 24m observable >= 36m observable
    if (retention.observable6mCount() != null && retention.observable12mCount() != null) {
      assertThat(retention.observable6mCount()).isGreaterThanOrEqualTo(retention.observable12mCount());
    }
    if (retention.observable12mCount() != null && retention.observable24mCount() != null) {
      assertThat(retention.observable12mCount()).isGreaterThanOrEqualTo(retention.observable24mCount());
    }
    if (retention.observable24mCount() != null && retention.observable36mCount() != null) {
      assertThat(retention.observable24mCount()).isGreaterThanOrEqualTo(retention.observable36mCount());
    }

    // Verify retained <= observable at each milestone
    if (retention.observable6mCount() != null && retention.retained6mCount() != null) {
      assertThat(retention.retained6mCount()).isLessThanOrEqualTo(retention.observable6mCount());
    }
    if (retention.observable12mCount() != null && retention.retained12mCount() != null) {
      assertThat(retention.retained12mCount()).isLessThanOrEqualTo(retention.observable12mCount());
    }
    if (retention.observable24mCount() != null && retention.retained24mCount() != null) {
      assertThat(retention.retained24mCount()).isLessThanOrEqualTo(retention.observable24mCount());
    }
    if (retention.observable36mCount() != null && retention.retained36mCount() != null) {
      assertThat(retention.retained36mCount()).isLessThanOrEqualTo(retention.observable36mCount());
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q15 unemployment summary query against live SIH26135_TEST")
  void testLiveFormalQ15Execution() {
    UnemploymentSummaryProjection uSummary =
        retentionAttritionDashboardRepository.getFormalQ15UnemploymentSummary();

    assertThat(uSummary).isNotNull();
    assertThat(uSummary.unemploymentPeriodCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(uSummary.traineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    if (uSummary.unemploymentPeriodCount() > 0) {
      assertThat(uSummary.unemploymentPeriodCount()).isGreaterThanOrEqualTo(uSummary.traineeCount());
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q16 unemployment reasons query against live SIH26135_TEST")
  void testLiveFormalQ16Execution() {
    List<UnemploymentReasonProjection> reasons =
        retentionAttritionDashboardRepository.getFormalQ16UnemploymentReasons();

    assertThat(reasons).isNotNull();
    for (UnemploymentReasonProjection r : reasons) {
      assertThat(r.unemploymentReasonId()).isNotNull();
      assertThat(r.unemploymentReasonCode()).isNotBlank();
      assertThat(r.reasonName()).isNotBlank();
      assertThat(r.periodCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q17 exit reasons query against live SIH26135_TEST")
  void testLiveFormalQ17Execution() {
    List<AttritionReasonProjection> exits =
        retentionAttritionDashboardRepository.getFormalQ17AttritionReasons();

    assertThat(exits).isNotNull();
    for (AttritionReasonProjection e : exits) {
      assertThat(e.employmentExitReasonId()).isNotNull();
      assertThat(e.exitReasonCode()).isNotBlank();
      assertThat(e.exitCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q18 separation totals query against live SIH26135_TEST")
  void testLiveFormalQ18Execution() {
    SeparationAnalysisProjection sep =
        retentionAttritionDashboardRepository.getFormalQ18SeparationTotals();

    assertThat(sep).isNotNull();
    if (sep.totalExitCount() != null) {
      assertThat(sep.totalExitCount()).isGreaterThanOrEqualTo(0L);
      long sum = (sep.voluntaryExitCount() != null ? sep.voluntaryExitCount() : 0L)
          + (sep.involuntaryExitCount() != null ? sep.involuntaryExitCount() : 0L)
          + (sep.otherExitCount() != null ? sep.otherExitCount() : 0L);
      assertThat(sep.totalExitCount()).isEqualTo(sum);
    }
  }

  @Test
  @DisplayName("Service: execute getLongitudinalRetention() against live SIH26135_TEST")
  void testLiveServiceGetLongitudinalRetention() {
    LongitudinalRetentionResponse response =
        retentionAttritionDashboardService.getLongitudinalRetention();

    assertThat(response).isNotNull();
    assertThat(response.milestones()).hasSize(4);
    assertThat(response.cohortSummary()).isNotNull();

    for (RetentionMilestoneResponse m : response.milestones()) {
      assertThat(m.milestone()).isNotBlank();
      assertThat(m.milestoneMonths()).isPositive();
    }
  }

  @Test
  @DisplayName("Service: execute getAttritionDashboard() against live SIH26135_TEST")
  void testLiveServiceGetAttritionDashboard() {
    AttritionDashboardResponse response =
        retentionAttritionDashboardService.getAttritionDashboard();

    assertThat(response).isNotNull();
    assertThat(response.unemploymentSummary()).isNotNull();
    assertThat(response.separationAnalysis()).isNotNull();
    assertThat(response.unemploymentReasons()).isNotNull();
    assertThat(response.exitReasons()).isNotNull();
  }

  @Test
  @DisplayName("HTTP GET /api/v1/dashboards/retention returns 200 with ApiResponse on live SIH26135_TEST")
  void testLiveRetentionEndpointHttp200() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/retention")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Longitudinal retention analytics retrieved successfully"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/retention"))
        .andExpect(jsonPath("$.data.milestones").isArray())
        .andExpect(jsonPath("$.data.cohortSummary").exists());
  }

  @Test
  @DisplayName("HTTP GET /api/v1/dashboards/attrition returns 200 with ApiResponse on live SIH26135_TEST")
  void testLiveAttritionEndpointHttp200() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/attrition")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Attrition and unemployment analytics retrieved successfully"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/attrition"))
        .andExpect(jsonPath("$.data.unemploymentSummary").exists())
        .andExpect(jsonPath("$.data.unemploymentReasons").isArray())
        .andExpect(jsonPath("$.data.separationAnalysis").exists())
        .andExpect(jsonPath("$.data.exitReasons").isArray());
  }
}
