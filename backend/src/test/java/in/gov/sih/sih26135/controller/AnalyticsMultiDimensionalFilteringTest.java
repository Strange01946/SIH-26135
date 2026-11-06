package in.gov.sih.sih26135.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("Phase 27.11 — Multi-Dimensional Query Filtering HTTP Contract & Validation Tests")
class AnalyticsMultiDimensionalFilteringTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private ExecutiveDashboardService executiveDashboardService;

  @MockBean
  private InstitutionalAnalyticsService institutionalAnalyticsService;

  @MockBean
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator scopeValidator;

  private String tokenNationalAdmin;

  @BeforeEach
  void setUp() {
    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "superadmin",
        List.of("ROLE_super_admin", "analytics.read")
    );

    // Sample mock returns
    DashboardKpiStripResponse kpi = new DashboardKpiStripResponse(
        100L, 120L, 95L, 80L, 75L, 60L, 55L, 52L, 50L, 48L, 3L, 1L,
        new BigDecimal("66.67"), new BigDecimal("62.50"), new BigDecimal("45.83"), new BigDecimal("41.67")
    );
    PipelineFunnelResponse funnel = new PipelineFunnelResponse(
        120L, 80L, 75L, 55L, 50L,
        new BigDecimal("66.67"), new BigDecimal("62.50"), new BigDecimal("45.83"), new BigDecimal("41.67")
    );
    LeaderboardResponse<SchemeProgramPerformanceResponse> schemeMatrix =
        LeaderboardResponse.of("employmentRatePct", List.of());
    ExecutiveDashboardResponse execResponse = new ExecutiveDashboardResponse(kpi, funnel, schemeMatrix);

    when(executiveDashboardService.getExecutiveDashboard(any(AnalyticsFilterCriteria.class))).thenReturn(execResponse);
    when(executiveDashboardService.getDashboardKpiStrip(any(AnalyticsFilterCriteria.class))).thenReturn(kpi);
    when(executiveDashboardService.getTrainingPipelineFunnel(any(AnalyticsFilterCriteria.class))).thenReturn(funnel);
    when(executiveDashboardService.getSchemeProgramPerformanceMatrix(any(AnalyticsFilterCriteria.class))).thenReturn(schemeMatrix);

    LeaderboardResponse<ProviderPerformanceResponse> providerLb =
        LeaderboardResponse.of("employmentRatePct", List.of());
    LeaderboardResponse<CourseEmployabilityResponse> courseLb =
        LeaderboardResponse.of("employmentRatePct", List.of());
    when(institutionalAnalyticsService.getProviderLeaderboard(any(AnalyticsFilterCriteria.class))).thenReturn(providerLb);
    when(institutionalAnalyticsService.getCourseLeaderboard(any(AnalyticsFilterCriteria.class))).thenReturn(courseLb);

    org.mockito.Mockito.doAnswer(invocation -> {
      AnalyticsFilterCriteria criteria = invocation.getArgument(0);
      if (criteria != null) {
        criteria.validate();
      }
      return null;
    }).when(scopeValidator).validateAnalyticsFilterScope(any());
  }

  @Nested
  @DisplayName("1. Supported Multi-Dimensional Query Combinations (HTTP 200)")
  class SupportedDimensionalQueries {

    @Test
    @DisplayName("Fiscal Year + State filter succeeds")
    void testFiscalYearAndState() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("fiscalYear", "FY2024-25")
              .param("stateId", "1")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Calendar Year + Program filter succeeds")
    void testCalendarYearAndProgram() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/pipeline-funnel")
              .param("calendarYear", "2024")
              .param("programId", "101")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Custom Date Range + Course filter succeeds")
    void testDateRangeAndCourse() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/scheme-matrix")
              .param("startDate", "2024-04-01")
              .param("endDate", "2024-09-30")
              .param("courseId", "5")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Sector + District filter succeeds on courses leaderboard")
    void testSectorAndDistrict() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/courses")
              .param("sectorId", "2")
              .param("districtId", "10")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Scheme + Program + Gender (case-insensitive) filter succeeds on KPIs")
    void testSchemeProgramAndGender() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("schemeId", "1")
              .param("programId", "101")
              .param("gender", "female")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  @Nested
  @DisplayName("2. Schema-Blocked Demographic Rejection (HTTP 400 UNSUPPORTED_FILTER)")
  class SchemaBlockedDemographicTests {

    @Test
    @DisplayName("Social Category filter rejected fail-closed with HTTP 400 UNSUPPORTED_FILTER")
    void testSocialCategoryRejection() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("socialCategory", "OBC")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILTER"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("socialCategory")));
    }

    @Test
    @DisplayName("Minority status filter rejected fail-closed with HTTP 400 UNSUPPORTED_FILTER")
    void testMinorityRejection() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("minority", "YES")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILTER"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minority")));
    }

    @Test
    @DisplayName("PwD (Disability) filter rejected fail-closed with HTTP 400 UNSUPPORTED_FILTER")
    void testPwdRejection() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("pwd", "YES")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILTER"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pwd")));
    }
  }

  @Nested
  @DisplayName("3. Conflicting & Invalid Filter Parameters (HTTP 400)")
  class ConflictingAndInvalidFilterTests {

    @Test
    @DisplayName("Conflicting Fiscal Year and Calendar Year rejected with CONFLICTING_TEMPORAL_FILTERS")
    void testConflictingFiscalAndCalendarYear() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("fiscalYear", "FY2024-25")
              .param("calendarYear", "2024")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("CONFLICTING_TEMPORAL_FILTERS"));
    }

    @Test
    @DisplayName("Reversed date range rejected with INVALID_DATE_RANGE")
    void testReversedDateRange() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("startDate", "2024-12-31")
              .param("endDate", "2024-01-01")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("Malformed fiscal year format rejected with VALIDATION_ERROR")
    void testMalformedFiscalYear() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("fiscalYear", "2024-25")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Unsupported gender value rejected with INVALID_DEMOGRAPHIC_FILTER")
    void testInvalidGender() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("gender", "INVALID_GENDER")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_DEMOGRAPHIC_FILTER"));
    }

    @Test
    @DisplayName("Non-positive schemeId rejected with VALIDATION_ERROR")
    void testNonPositiveIdentifier() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .param("schemeId", "-1")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
  }
}
