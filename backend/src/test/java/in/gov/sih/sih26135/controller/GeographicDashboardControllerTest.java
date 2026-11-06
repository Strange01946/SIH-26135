package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.RegionalDisparityResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.dto.response.TrainingCenterSummaryResponse;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
@DisplayName("GeographicDashboardController Tests")
class GeographicDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private GeographicDashboardService geographicDashboardService;

  @MockBean
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator scopeValidator;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private GeographicOverviewResponse sampleOverviewResponse;
  private StateGeographicResponse sampleStateResponse;
  private DistrictGeographicResponse sampleDistrictResponse;

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

    DistrictPerformanceResponse sampleDistrictPerf = new DistrictPerformanceResponse(
        101L, "DIST-01", "Pune", 1L, "MH", "Maharashtra",
        600L, 580L, 500L, 480L, 400L, 350L,
        new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33"),
        1
    );

    StatePerformanceResponse sampleStatePerf = new StatePerformanceResponse(
        1L, "MH", "Maharashtra", 1L, 600L, 580L, 500L, 480L, 400L, 350L,
        new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33"),
        5L, 1
    );

    RegionalDisparityResponse sampleDisparity = new RegionalDisparityResponse(
        List.of(sampleDistrictPerf),
        List.of(sampleDistrictPerf),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        Map.of("note", "value")
    );

    sampleOverviewResponse = new GeographicOverviewResponse(
        1L, 1L, 600L, 500L, 480L, 400L, 350L,
        new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33"),
        List.of(sampleStatePerf),
        LeaderboardResponse.of("employmentRatePct", List.of(sampleDistrictPerf)),
        sampleDisparity
    );

    sampleStateResponse = new StateGeographicResponse(
        1L, "MH", "Maharashtra",
        sampleStatePerf,
        List.of(sampleDistrictPerf),
        LeaderboardResponse.of("employmentRatePct", List.of(sampleDistrictPerf)),
        5L,
        sampleDisparity
    );

    sampleDistrictResponse = new DistrictGeographicResponse(
        101L, "DIST-01", "Pune", 1L, "MH", "Maharashtra",
        sampleDistrictPerf,
        1L,
        List.of(new TrainingCenterSummaryResponse(1L, "TC-01", "Pune TC", 100, "411001")),
        1,
        1,
        Map.of("hasTrainingCenter", true)
    );
  }

  @Nested
  @DisplayName("Endpoint & Envelope Contract Tests")
  class EndpointContractTests {

    @Test
    @DisplayName("GET /api/v1/dashboards/geography: returns 200 OK with national geographic overview")
    void testGetNationalGeography() throws Exception {
      when(geographicDashboardService.getNationalGeographicOverview()).thenReturn(sampleOverviewResponse);

      mockMvc.perform(get("/api/v1/dashboards/geography")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("National geographic overview retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/geography"))
          .andExpect(jsonPath("$.data.totalStates").value(1))
          .andExpect(jsonPath("$.data.totalDistricts").value(1))
          .andExpect(jsonPath("$.data.totalEnrollments").value(600))
          .andExpect(jsonPath("$.data.overallCompletionRatePct").value(83.33))
          .andExpect(jsonPath("$.data.stateSummaries").isArray())
          .andExpect(jsonPath("$.data.stateSummaries[0].stateCode").value("MH"))
          .andExpect(jsonPath("$.data.districtRankings.rankingMetric").value("employmentRatePct"))
          .andExpect(jsonPath("$.data.regionalDisparity").exists());
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/states/{stateId}: returns 200 OK with state geographic analytics")
    void testGetStateGeography() throws Exception {
      when(geographicDashboardService.getStateGeographicAnalytics(1L)).thenReturn(sampleStateResponse);

      mockMvc.perform(get("/api/v1/dashboards/states/1")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("State geographic analytics retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/states/1"))
          .andExpect(jsonPath("$.data.stateId").value(1))
          .andExpect(jsonPath("$.data.stateCode").value("MH"))
          .andExpect(jsonPath("$.data.districts[0].districtName").value("Pune"))
          .andExpect(jsonPath("$.data.totalTrainingCenters").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/districts/{districtId}: returns 200 OK with district geographic analytics")
    void testGetDistrictGeography() throws Exception {
      when(geographicDashboardService.getDistrictGeographicAnalytics(101L)).thenReturn(sampleDistrictResponse);

      mockMvc.perform(get("/api/v1/dashboards/districts/101")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("District geographic analytics retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/districts/101"))
          .andExpect(jsonPath("$.data.districtId").value(101))
          .andExpect(jsonPath("$.data.districtName").value("Pune"))
          .andExpect(jsonPath("$.data.trainingCenterCount").value(1))
          .andExpect(jsonPath("$.data.trainingCenters[0].centerCode").value("TC-01"));
    }
  }

  @Nested
  @DisplayName("Validation & Exception Handling Tests")
  class ValidationAndExceptionTests {

    @ParameterizedTest(name = "Invalid stateId path variable {0} returns 400 Bad Request")
    @ValueSource(strings = {"/api/v1/dashboards/states/0", "/api/v1/dashboards/states/-1", "/api/v1/dashboards/states/abc"})
    void testInvalidStateIdReturnsBadRequest(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").exists());
    }

    @ParameterizedTest(name = "Invalid districtId path variable {0} returns 400 Bad Request")
    @ValueSource(strings = {"/api/v1/dashboards/districts/0", "/api/v1/dashboards/districts/-1", "/api/v1/dashboards/districts/abc"})
    void testInvalidDistrictIdReturnsBadRequest(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @DisplayName("Nonexistent stateId returns 404 Not Found")
    void testNonexistentStateReturnsNotFound() throws Exception {
      when(geographicDashboardService.getStateGeographicAnalytics(9999L))
          .thenThrow(new ResourceNotFoundException("State not found with ID: 9999"));

      mockMvc.perform(get("/api/v1/dashboards/states/9999")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("State not found with ID: 9999"));
    }

    @Test
    @DisplayName("Nonexistent districtId returns 404 Not Found")
    void testNonexistentDistrictReturnsNotFound() throws Exception {
      when(geographicDashboardService.getDistrictGeographicAnalytics(9999L))
          .thenThrow(new ResourceNotFoundException("District not found with ID: 9999"));

      mockMvc.perform(get("/api/v1/dashboards/districts/9999")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("District not found with ID: 9999"));
    }
  }

  @Nested
  @DisplayName("Security & Authorization Tests")
  class SecurityAuthorizationTests {

    @ParameterizedTest(name = "Unauthenticated GET {0} returns 401 Unauthorized")
    @ValueSource(strings = {
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/states/1",
        "/api/v1/dashboards/districts/101"
    })
    void testAllEndpointsRejectUnauthenticatedAccess(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "User without analytics.read authority on GET {0} returns 403 Forbidden")
    @ValueSource(strings = {
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/states/1",
        "/api/v1/dashboards/districts/101"
    })
    void testAllEndpointsDenyAccessWithoutAnalyticsReadAuthority(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "User with analytics.read authority on GET {0} returns 200 OK")
    @ValueSource(strings = {
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/states/1",
        "/api/v1/dashboards/districts/101"
    })
    void testAllEndpointsPermitAccessWithAnalyticsReadAuthority(String endpoint) throws Exception {
      when(geographicDashboardService.getNationalGeographicOverview()).thenReturn(sampleOverviewResponse);
      when(geographicDashboardService.getStateGeographicAnalytics(1L)).thenReturn(sampleStateResponse);
      when(geographicDashboardService.getDistrictGeographicAnalytics(101L)).thenReturn(sampleDistrictResponse);

      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }
}
