package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSeverityBreakdownResponse;
import in.gov.sih.sih26135.dto.response.TopSkillGapResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.SkillGapDashboardService;
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
@DisplayName("SkillGapDashboardController Security and HTTP Contract Tests")
class SkillGapDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private SkillGapDashboardService skillGapDashboardService;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private SkillGapDashboardResponse sampleDashboardResponse;

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

    TopSkillGapResponse topGap = new TopSkillGapResponse(
        101L, "SKILL-JAVA", "Java Programming", 5L, "CRITICAL",
        120L, 95L, 78L, new BigDecimal("2.35"),
        null, null, null, null
    );

    SkillGapSeverityBreakdownResponse sev = new SkillGapSeverityBreakdownResponse(
        "CRITICAL", "Critical", 78L, 65L, 50L, 38L,
        new BigDecimal("100.00"), new BigDecimal("76.00")
    );

    sampleDashboardResponse = new SkillGapDashboardResponse(
        78L, 65L, 50L, 38L, new BigDecimal("76.00"),
        List.of(topGap), List.of(sev)
    );
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/skill-gaps returns 200 OK and ApiResponse envelope with analytics.read")
  void testSkillGapDashboardEndpointSuccessWithAnalyticsReadAuthority() throws Exception {
    when(skillGapDashboardService.getSkillGapDashboard()).thenReturn(sampleDashboardResponse);

    mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Skill gap analytics retrieved successfully"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/skill-gaps"))
        .andExpect(jsonPath("$.data.totalCurrentGaps").value(78))
        .andExpect(jsonPath("$.data.totalAffectedTrainees").value(65))
        .andExpect(jsonPath("$.data.totalRecommendations").value(50))
        .andExpect(jsonPath("$.data.totalAcceptedRecommendations").value(38))
        .andExpect(jsonPath("$.data.overallAcceptanceRatePct").value(76.00))
        .andExpect(jsonPath("$.data.topSkillGaps[0].skillCode").value("SKILL-JAVA"))
        .andExpect(jsonPath("$.data.topSkillGaps[0].severityCode").value("CRITICAL"))
        .andExpect(jsonPath("$.data.severityBreakdown[0].severityCode").value("CRITICAL"))
        .andExpect(jsonPath("$.data.severityBreakdown[0].acceptanceRatePct").value(76.00));

    verify(skillGapDashboardService).getSkillGapDashboard();
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/skill-gaps returns 401 Unauthorized when unauthenticated")
  void testSkillGapDashboardEndpointUnauthorizedWithoutToken() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/dashboards/skill-gaps returns 403 Forbidden without analytics.read authority")
  void testSkillGapDashboardEndpointForbiddenWithoutAnalyticsReadAuthority() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
            .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
