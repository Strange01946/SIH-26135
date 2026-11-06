package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
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
@DisplayName("ExecutiveDashboardController Tests")
class ExecutiveDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private ExecutiveDashboardService executiveDashboardService;

  @MockBean
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator scopeValidator;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private ExecutiveDashboardResponse sampleDashboardResponse;
  private DashboardKpiStripResponse sampleKpiStrip;
  private PipelineFunnelResponse samplePipelineFunnel;
  private LeaderboardResponse<SchemeProgramPerformanceResponse> sampleMatrix;

  @BeforeEach
  void setUp() {
    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "executive",
        List.of("analytics.read")
    );
    tokenUnauthorized = "Bearer " + jwtTokenProvider.generateAccessToken(
        2L,
        "operator",
        List.of("trainee.read")
    );

    sampleKpiStrip = new DashboardKpiStripResponse(
        1000L, 1200L, 950L, 800L, 750L, 600L, 550L, 520L, 500L, 480L, 30L, 10L,
        new BigDecimal("66.67"), new BigDecimal("62.50"), new BigDecimal("45.83"), new BigDecimal("41.67")
    );

    samplePipelineFunnel = new PipelineFunnelResponse(
        1200L, 800L, 750L, 550L, 500L,
        new BigDecimal("66.67"), new BigDecimal("62.50"), new BigDecimal("45.83"), new BigDecimal("41.67")
    );

    SchemeProgramPerformanceResponse item = new SchemeProgramPerformanceResponse(
        101L, "PRG-01", "Solar Tech", 1L, "PMKVY", "Pradhan Mantri Kaushal Vikas Yojana",
        500L, 480L, 450L, 430L, 350L, 320L, 20L, 10L,
        new BigDecimal("90.00"), new BigDecimal("86.00"), new BigDecimal("70.00"), new BigDecimal("64.00")
    );
    sampleMatrix = LeaderboardResponse.of("employmentRatePct", List.of(item));

    sampleDashboardResponse = new ExecutiveDashboardResponse(sampleKpiStrip, samplePipelineFunnel, sampleMatrix);
  }

  @Nested
  @DisplayName("Endpoint & Envelope Contract Tests")
  class EndpointContractTests {

    @Test
    @DisplayName("GET /api/v1/dashboards/overview: returns 200 OK with full executive dashboard")
    void testGetExecutiveOverview() throws Exception {
      when(executiveDashboardService.getExecutiveDashboard()).thenReturn(sampleDashboardResponse);

      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Executive dashboard overview retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/overview"))
          .andExpect(jsonPath("$.data.kpiStrip.traineeCount").value(1000))
          .andExpect(jsonPath("$.data.kpiStrip.enrollmentCount").value(1200))
          .andExpect(jsonPath("$.data.kpiStrip.completionRatePct").value(66.67))
          .andExpect(jsonPath("$.data.pipelineFunnel.enrollmentCount").value(1200))
          .andExpect(jsonPath("$.data.pipelineFunnel.completedCount").value(800))
          .andExpect(jsonPath("$.data.schemeProgramMatrix.rankingMetric").value("employmentRatePct"))
          .andExpect(jsonPath("$.data.schemeProgramMatrix.totalItems").value(1))
          .andExpect(jsonPath("$.data.schemeProgramMatrix.items[0].programCode").value("PRG-01"))
          .andExpect(jsonPath("$.data.schemeProgramMatrix.items[0].schemeCode").value("PMKVY"));
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/national: returns 200 OK with national overview")
    void testGetNationalOverview() throws Exception {
      when(executiveDashboardService.getExecutiveDashboard()).thenReturn(sampleDashboardResponse);

      mockMvc.perform(get("/api/v1/dashboards/national")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("National dashboard overview retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/national"))
          .andExpect(jsonPath("$.data.kpiStrip.traineeCount").value(1000));
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/kpis: returns 200 OK with KPI strip")
    void testGetKpis() throws Exception {
      when(executiveDashboardService.getDashboardKpiStrip()).thenReturn(sampleKpiStrip);

      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Dashboard KPI strip retrieved successfully"))
          .andExpect(jsonPath("$.data.traineeCount").value(1000))
          .andExpect(jsonPath("$.data.completionRatePct").value(66.67));
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/pipeline-funnel: returns 200 OK with funnel stages")
    void testGetPipelineFunnel() throws Exception {
      when(executiveDashboardService.getTrainingPipelineFunnel()).thenReturn(samplePipelineFunnel);

      mockMvc.perform(get("/api/v1/dashboards/pipeline-funnel")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Training pipeline conversion funnel retrieved successfully"))
          .andExpect(jsonPath("$.data.enrollmentCount").value(1200))
          .andExpect(jsonPath("$.data.completionRatePct").value(66.67));
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/scheme-matrix: returns 200 OK with scheme matrix")
    void testGetSchemeMatrix() throws Exception {
      when(executiveDashboardService.getSchemeProgramPerformanceMatrix()).thenReturn(sampleMatrix);

      mockMvc.perform(get("/api/v1/dashboards/scheme-matrix")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Scheme and program performance matrix retrieved successfully"))
          .andExpect(jsonPath("$.data.rankingMetric").value("employmentRatePct"))
          .andExpect(jsonPath("$.data.totalItems").value(1));
    }
  }

  @Nested
  @DisplayName("Security & Authorization Tests")
  class SecurityAuthorizationTests {

    @ParameterizedTest(name = "Unauthenticated GET {0} returns 401 Unauthorized")
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix"
    })
    void testAllEndpointsRejectUnauthenticatedAccess(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "User without analytics.read authority on GET {0} returns 403 Forbidden")
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix"
    })
    void testAllEndpointsDenyAccessWithoutAnalyticsReadAuthority(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "User with analytics.read authority on GET {0} returns 200 OK")
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix"
    })
    void testAllEndpointsPermitAccessWithAnalyticsReadAuthority(String endpoint) throws Exception {
      when(executiveDashboardService.getExecutiveDashboard()).thenReturn(sampleDashboardResponse);
      when(executiveDashboardService.getDashboardKpiStrip()).thenReturn(sampleKpiStrip);
      when(executiveDashboardService.getTrainingPipelineFunnel()).thenReturn(samplePipelineFunnel);
      when(executiveDashboardService.getSchemeProgramPerformanceMatrix()).thenReturn(sampleMatrix);

      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }
}
