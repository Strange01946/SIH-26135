package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.repository.analytics.DataIntegrityDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupWaveProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyResponsePipelineProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationMethodEfficacyProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOutcomeProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOverallSummaryProjection;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Live database integration tests for Verification Integrity, Longitudinal Follow-up,
 * and Survey Analytics Dashboards.
 *
 * <p>Verifies exact formal queries Q19, Q20, Q21, and Q22 execution, mathematical reconciliation,
 * canonical reference categories, and REST endpoint behavior against dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DataIntegrityDashboard Live Database Integration Tests (SIH26135_TEST)")
class DataIntegrityDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataIntegrityDashboardService dataIntegrityDashboardService;

  @Autowired
  private DataIntegrityDashboardRepository dataIntegrityDashboardRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String tokenAnalyticsRead;

  @BeforeEach
  void setUp() {
    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "executive",
        List.of("analytics.read")
    );
  }

  @Test
  @DisplayName("Safety: Enforce strictly dedicated test catalog SIH26135_TEST")
  void testEnforceDedicatedTestCatalog() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
    }
  }

  @Test
  @DisplayName("Q21 & Q22 Schema Verification: Reference categories match canonical definitions")
  void testVerificationReferenceCategoriesMatchSchema() {
    List<Map<String, Object>> statuses = jdbcTemplate.queryForList(
        "SELECT status_code, is_verified_flag FROM ref_record_verification_status ORDER BY sort_order"
    );
    assertThat(statuses).isNotEmpty();
    Set<String> statusCodes = statuses.stream()
        .map(m -> (String) m.get("status_code"))
        .collect(java.util.stream.Collectors.toSet());
    assertThat(statusCodes).contains("UNVERIFIED", "SELF_REPORTED", "PENDING", "VERIFIED", "REJECTED");

    List<Map<String, Object>> methods = jdbcTemplate.queryForList(
        "SELECT method_code, is_official_flag, is_trainee_self_reported FROM ref_employment_verification_method"
    );
    assertThat(methods).isNotEmpty();
    Set<String> methodCodes = methods.stream()
        .map(m -> (String) m.get("method_code"))
        .collect(java.util.stream.Collectors.toSet());
    assertThat(methodCodes).contains(
        "GOVERNMENT_DATABASE", "EMPLOYER_PORTAL", "TRAINEE_SELF_ATTESTATION",
        "DOCUMENT_REVIEW", "PHONE_CONFIRMATION", "EMAIL_CONFIRMATION"
    );
  }

  @Test
  @DisplayName("Q19 Schema Verification: Follow-up wave types match canonical offsets")
  void testFollowupWaveTypesMatchSchema() {
    List<Map<String, Object>> waves = jdbcTemplate.queryForList(
        "SELECT type_code, followup_offset_months FROM ref_followup_type WHERE followup_offset_months IS NOT NULL ORDER BY sort_order"
    );
    assertThat(waves).isNotEmpty();
    Set<String> waveCodes = waves.stream()
        .map(m -> (String) m.get("type_code"))
        .collect(java.util.stream.Collectors.toSet());
    assertThat(waveCodes).contains("MONTH_3", "MONTH_6", "MONTH_12");
  }

  @Test
  @DisplayName("Q20 Schema Verification: Survey purpose codes match canonical definitions")
  void testSurveyPurposesMatchSchema() {
    List<Map<String, Object>> purposes = jdbcTemplate.queryForList(
        "SELECT purpose_code FROM ref_survey_purpose ORDER BY sort_order"
    );
    assertThat(purposes).isNotEmpty();
    Set<String> purposeCodes = purposes.stream()
        .map(m -> (String) m.get("purpose_code"))
        .collect(java.util.stream.Collectors.toSet());
    assertThat(purposeCodes).contains("EMPLOYMENT_OUTCOME", "RETENTION_3M", "RETENTION_6M", "RETENTION_12M");
  }

  @Test
  @DisplayName("Live Repository Queries Q19–Q22: Execute cleanly against test database views")
  void testRepositoryQueriesExecuteCleanlyAgainstViews() {
    List<VerificationOutcomeProjection> q21 = dataIntegrityDashboardRepository.getFormalQ21VerificationOutcomes();
    assertThat(q21).isNotNull();

    List<VerificationMethodEfficacyProjection> q22 = dataIntegrityDashboardRepository.getFormalQ22VerificationMethodEfficacy();
    assertThat(q22).isNotNull();

    VerificationOverallSummaryProjection vSummary = dataIntegrityDashboardRepository.getFormalQ21Q22OverallSummary();
    assertThat(vSummary).isNotNull();
    if (vSummary.totalVerifications() != null && vSummary.totalEmployments() != null) {
      assertThat(vSummary.totalEmployments()).isLessThanOrEqualTo(vSummary.totalVerifications());
    }

    List<FollowupWaveProjection> q19 = dataIntegrityDashboardRepository.getFormalQ19FollowupSuccessByWave();
    assertThat(q19).isNotNull();

    FollowupOverallSummaryProjection fSummary = dataIntegrityDashboardRepository.getFormalQ19OverallSummary();
    assertThat(fSummary).isNotNull();

    List<SurveyResponsePipelineProjection> q20 = dataIntegrityDashboardRepository.getFormalQ20SurveyResponsePipeline();
    assertThat(q20).isNotNull();

    SurveyOverallSummaryProjection sSummary = dataIntegrityDashboardRepository.getFormalQ20OverallSummary();
    assertThat(sSummary).isNotNull();
  }

  @Test
  @DisplayName("Live Service Aggregations: Successfully generate complete dashboard responses")
  void testServiceAggregationsExecuteCleanly() {
    VerificationDashboardResponse vResp = dataIntegrityDashboardService.getVerificationDashboard();
    assertThat(vResp).isNotNull();
    assertThat(vResp.statusBreakdown()).isNotNull();
    assertThat(vResp.methodEfficacyBreakdown()).isNotNull();

    FollowupDashboardResponse fResp = dataIntegrityDashboardService.getFollowupDashboard();
    assertThat(fResp).isNotNull();
    assertThat(fResp.wavePerformance()).isNotNull();
    assertThat(fResp.slaPerformance()).isNotNull();
    assertThat(fResp.slaPerformance().isAvailable()).isFalse();

    SurveyDashboardResponse sResp = dataIntegrityDashboardService.getSurveyDashboard();
    assertThat(sResp).isNotNull();
    assertThat(sResp.purposePipeline()).isNotNull();
    assertThat(sResp.satisfactionPerformance()).isNotNull();
    assertThat(sResp.satisfactionPerformance().isAvailable()).isFalse();
  }

  @Test
  @DisplayName("Live HTTP Endpoints: Return 200 OK and ApiResponse envelope")
  void testHttpEndpointsReturn200AndApiResponse() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/verifications")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Verification integrity analytics retrieved successfully"))
        .andExpect(jsonPath("$.data").isMap());

    mockMvc.perform(get("/api/v1/dashboards/followups")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Follow-up outreach analytics retrieved successfully"))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data.slaPerformance.isAvailable").value(false));

    mockMvc.perform(get("/api/v1/dashboards/surveys")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Survey response pipeline analytics retrieved successfully"))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data.satisfactionPerformance.isAvailable").value(false));
  }
}
