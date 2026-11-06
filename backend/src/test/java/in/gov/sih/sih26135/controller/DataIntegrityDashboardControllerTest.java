package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupSlaResponse;
import in.gov.sih.sih26135.dto.response.FollowupWavePerformanceResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyPurposePipelineResponse;
import in.gov.sih.sih26135.dto.response.SurveySatisfactionResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationMethodEfficacyResponse;
import in.gov.sih.sih26135.dto.response.VerificationStatusBreakdownResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataIntegrityDashboardService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DataIntegrityDashboardController Security and HTTP Contract Tests")
class DataIntegrityDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private DataIntegrityDashboardService dataIntegrityDashboardService;

  @MockBean
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator scopeValidator;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private VerificationDashboardResponse sampleVerificationResponse;
  private FollowupDashboardResponse sampleFollowupResponse;
  private SurveyDashboardResponse sampleSurveyResponse;

  @BeforeEach
  void setUp() {
    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "executive",
        List.of("analytics.read")
    );

    tokenUnauthorized = "Bearer " + jwtTokenProvider.generateAccessToken(
        2L,
        "guest",
        List.of("trainee.read")
    );

    VerificationStatusBreakdownResponse statusItem = new VerificationStatusBreakdownResponse(
        "VERIFIED", "Verified", true, 500L, 450L, 480L, new BigDecimal("62.50")
    );
    VerificationMethodEfficacyResponse methodItem = new VerificationMethodEfficacyResponse(
        "GOVERNMENT_DATABASE", "Government / departmental database match",
        true, false, 300L, 280L, 270L, 20L, 10L,
        new BigDecimal("90.00"), new BigDecimal("37.50")
    );
    sampleVerificationResponse = new VerificationDashboardResponse(
        800L, 750L, 700L, 500L, 350L, 150L, 100L, 50L, 0L,
        new BigDecimal("68.75"), new BigDecimal("62.50"), new BigDecimal("43.75"),
        new BigDecimal("18.75"), new BigDecimal("12.50"),
        List.of(statusItem), List.of(methodItem)
    );

    FollowupWavePerformanceResponse waveItem = new FollowupWavePerformanceResponse(
        "MONTH_3", "3-month follow-up", 3, 200L, 190L, 150L, 30L, 20L, 0L,
        new BigDecimal("75.00"), new BigDecimal("10.00"), new BigDecimal("40.00")
    );
    FollowupSlaResponse sla = new FollowupSlaResponse(
        false, "SLA tracking deferred: schema unconfigured", null, null, null, null
    );
    sampleFollowupResponse = new FollowupDashboardResponse(
        500L, 450L, 420L, 80L, 350L, 40L, 30L,
        new BigDecimal("70.00"), new BigDecimal("6.00"), new BigDecimal("84.00"),
        List.of(waveItem), sla
    );

    SurveyPurposePipelineResponse pipelineItem = new SurveyPurposePipelineResponse(
        "EMPLOYMENT_OUTCOME", "Employment outcome", null,
        250L, 230L, 200L, 50L, new BigDecimal("80.00"), new BigDecimal("50.00")
    );
    SurveySatisfactionResponse satisfaction = new SurveySatisfactionResponse(
        false, "Satisfaction rating metrics deferred: dynamic questions", null, null
    );
    sampleSurveyResponse = new SurveyDashboardResponse(
        500L, 450L, 380L, 120L, new BigDecimal("76.00"),
        List.of(pipelineItem), satisfaction
    );
  }

  // =========================================================================
  // 1. GET /api/v1/dashboards/verifications
  // =========================================================================

  @Test
  @DisplayName("GET /api/v1/dashboards/verifications: Anonymous request returns 401 Unauthorized")
  void testGetVerificationDashboardAnonymousReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/verifications")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/verifications: User without analytics.read returns 403 Forbidden")
  void testGetVerificationDashboardUnauthorizedReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/verifications")
            .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/verifications: User with analytics.read returns 200 OK and ApiResponse")
  void testGetVerificationDashboardSuccessReturns200() throws Exception {
    when(dataIntegrityDashboardService.getVerificationDashboard()).thenReturn(sampleVerificationResponse);

    mockMvc.perform(get("/api/v1/dashboards/verifications")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Verification integrity analytics retrieved successfully"))
        .andExpect(jsonPath("$.data.totalVerifications").value(800))
        .andExpect(jsonPath("$.data.totalEmployments").value(750))
        .andExpect(jsonPath("$.data.verifiedCount").value(500))
        .andExpect(jsonPath("$.data.verificationSuccessRatePct").value(62.50))
        .andExpect(jsonPath("$.data.statusBreakdown[0].statusCode").value("VERIFIED"))
        .andExpect(jsonPath("$.data.methodEfficacyBreakdown[0].methodCode").value("GOVERNMENT_DATABASE"));

    verify(dataIntegrityDashboardService).getVerificationDashboard();
  }

  // =========================================================================
  // 2. GET /api/v1/dashboards/followups
  // =========================================================================

  @Test
  @DisplayName("GET /api/v1/dashboards/followups: Anonymous request returns 401 Unauthorized")
  void testGetFollowupDashboardAnonymousReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/followups")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/followups: User without analytics.read returns 403 Forbidden")
  void testGetFollowupDashboardUnauthorizedReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/followups")
            .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/followups: User with analytics.read returns 200 OK and ApiResponse")
  void testGetFollowupDashboardSuccessReturns200() throws Exception {
    when(dataIntegrityDashboardService.getFollowupDashboard()).thenReturn(sampleFollowupResponse);

    mockMvc.perform(get("/api/v1/dashboards/followups")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Follow-up outreach analytics retrieved successfully"))
        .andExpect(jsonPath("$.data.totalTasks").value(500))
        .andExpect(jsonPath("$.data.contactSuccessRatePct").value(70.00))
        .andExpect(jsonPath("$.data.slaPerformance.isAvailable").value(false))
        .andExpect(jsonPath("$.data.wavePerformance[0].typeCode").value("MONTH_3"));

    verify(dataIntegrityDashboardService).getFollowupDashboard();
  }

  // =========================================================================
  // 3. GET /api/v1/dashboards/surveys
  // =========================================================================

  @Test
  @DisplayName("GET /api/v1/dashboards/surveys: Anonymous request returns 401 Unauthorized")
  void testGetSurveyDashboardAnonymousReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/surveys")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/surveys: User without analytics.read returns 403 Forbidden")
  void testGetSurveyDashboardUnauthorizedReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/surveys")
            .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/surveys: User with analytics.read returns 200 OK and ApiResponse")
  void testGetSurveyDashboardSuccessReturns200() throws Exception {
    when(dataIntegrityDashboardService.getSurveyDashboard()).thenReturn(sampleSurveyResponse);

    mockMvc.perform(get("/api/v1/dashboards/surveys")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Survey response pipeline analytics retrieved successfully"))
        .andExpect(jsonPath("$.data.totalResponseRows").value(500))
        .andExpect(jsonPath("$.data.overallSubmissionRatePct").value(76.00))
        .andExpect(jsonPath("$.data.satisfactionPerformance.isAvailable").value(false))
        .andExpect(jsonPath("$.data.purposePipeline[0].purposeCode").value("EMPLOYMENT_OUTCOME"));

    verify(dataIntegrityDashboardService).getSurveyDashboard();
  }
}
