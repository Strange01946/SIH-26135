package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.AttritionReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.dto.response.RetentionCohortResponse;
import in.gov.sih.sih26135.dto.response.RetentionMilestoneResponse;
import in.gov.sih.sih26135.dto.response.SeparationAnalysisResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentSummaryResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.RetentionAttritionDashboardService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RetentionAttritionDashboardController Tests")
class RetentionAttritionDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private RetentionAttritionDashboardService retentionAttritionDashboardService;

  @MockBean
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator scopeValidator;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private LongitudinalRetentionResponse sampleRetentionResponse;
  private AttritionDashboardResponse sampleAttritionResponse;

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

    RetentionMilestoneResponse m6 = new RetentionMilestoneResponse(
        "6_MONTHS", 6, 1000L, 850L, new BigDecimal("85.00")
    );
    RetentionCohortResponse cohort = new RetentionCohortResponse(
        1200L,
        1000L, 850L, new BigDecimal("85.00"),
        800L, 640L, new BigDecimal("80.00"),
        500L, 350L, new BigDecimal("70.00"),
        200L, 120L, new BigDecimal("60.00")
    );
    sampleRetentionResponse = new LongitudinalRetentionResponse(1200L, cohort, List.of(m6));

    UnemploymentSummaryResponse uSummary = new UnemploymentSummaryResponse(
        500L, 450L, 120L, 280L, 90L,
        new BigDecimal("145.50"), new BigDecimal("62.30"), new BigDecimal("56.00")
    );
    UnemploymentReasonBreakdownResponse uReason = new UnemploymentReasonBreakdownResponse(
        1L, "SKILL_MISMATCH", "Skill mismatch", 150L, 140L, 35L, 85L,
        new BigDecimal("120.00"), new BigDecimal("30.00"), new BigDecimal("56.67")
    );
    SeparationAnalysisResponse sep = new SeparationAnalysisResponse(
        400L, 250L, 120L, 30L,
        new BigDecimal("62.50"), new BigDecimal("30.00"), new BigDecimal("7.50"),
        new BigDecimal("2.08"), null
    );
    AttritionReasonBreakdownResponse aReason = new AttritionReasonBreakdownResponse(
        1L, "BETTER_OPPORTUNITY", "Better opportunity", 1L, "VOLUNTARY", "Voluntary resignation / quit",
        true, false, 150L, 140L, new BigDecimal("37.50")
    );
    sampleAttritionResponse = new AttritionDashboardResponse(
        uSummary, List.of(uReason), sep, List.of(aReason)
    );
  }

  @Nested
  @DisplayName("Security & Authorization Tests")
  class SecurityTests {

    @ParameterizedTest(name = "Endpoint {0} returns 401 when unauthenticated")
    @ValueSource(strings = {
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition"
    })
    void testEndpointsRequireAuthentication(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "Endpoint {0} returns 403 when user lacks analytics.read authority")
    @ValueSource(strings = {
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition"
    })
    void testEndpointsRequireAnalyticsReadAuthority(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/dashboards/retention Tests")
  class RetentionEndpointTests {

    @Test
    @DisplayName("Returns HTTP 200 with LongitudinalRetentionResponse envelope when authorized")
    void testGetLongitudinalRetentionAuthorized() throws Exception {
      when(retentionAttritionDashboardService.getLongitudinalRetention())
          .thenReturn(sampleRetentionResponse);

      mockMvc.perform(get("/api/v1/dashboards/retention")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Longitudinal retention analytics retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/retention"))
          .andExpect(jsonPath("$.data.employmentSpellCount").value(1200))
          .andExpect(jsonPath("$.data.cohortSummary.observable6mCount").value(1000))
          .andExpect(jsonPath("$.data.cohortSummary.retention6mPct").value(85.0))
          .andExpect(jsonPath("$.data.milestones").isArray())
          .andExpect(jsonPath("$.data.milestones[0].milestone").value("6_MONTHS"))
          .andExpect(jsonPath("$.data.milestones[0].milestoneMonths").value(6))
          .andExpect(jsonPath("$.data.milestones[0].observableCount").value(1000))
          .andExpect(jsonPath("$.data.milestones[0].retainedCount").value(850))
          .andExpect(jsonPath("$.data.milestones[0].retentionRatePct").value(85.0));

      verify(retentionAttritionDashboardService).getLongitudinalRetention();
    }
  }

  @Nested
  @DisplayName("GET /api/v1/dashboards/attrition Tests")
  class AttritionEndpointTests {

    @Test
    @DisplayName("Returns HTTP 200 with AttritionDashboardResponse envelope when authorized")
    void testGetAttritionDashboardAuthorized() throws Exception {
      when(retentionAttritionDashboardService.getAttritionDashboard())
          .thenReturn(sampleAttritionResponse);

      mockMvc.perform(get("/api/v1/dashboards/attrition")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Attrition and unemployment analytics retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/attrition"))
          .andExpect(jsonPath("$.data.unemploymentSummary.unemploymentPeriodCount").value(500))
          .andExpect(jsonPath("$.data.unemploymentSummary.reemploymentRatePct").value(56.0))
          .andExpect(jsonPath("$.data.unemploymentReasons").isArray())
          .andExpect(jsonPath("$.data.unemploymentReasons[0].unemploymentReasonCode").value("SKILL_MISMATCH"))
          .andExpect(jsonPath("$.data.unemploymentReasons[0].sharePct").value(30.0))
          .andExpect(jsonPath("$.data.separationAnalysis.totalExitCount").value(400))
          .andExpect(jsonPath("$.data.separationAnalysis.voluntaryToInvoluntaryRatio").value(2.08))
          .andExpect(jsonPath("$.data.separationAnalysis.noticeComplianceRatePct").doesNotExist())
          .andExpect(jsonPath("$.data.exitReasons").isArray())
          .andExpect(jsonPath("$.data.exitReasons[0].exitReasonCode").value("BETTER_OPPORTUNITY"))
          .andExpect(jsonPath("$.data.exitReasons[0].isVoluntaryFlag").value(true));

      verify(retentionAttritionDashboardService).getAttritionDashboard();
    }
  }
}
