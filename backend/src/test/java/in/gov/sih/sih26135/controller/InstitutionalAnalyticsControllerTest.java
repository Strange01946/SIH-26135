package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
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
@DisplayName("InstitutionalAnalyticsController Tests")
class InstitutionalAnalyticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private InstitutionalAnalyticsService institutionalAnalyticsService;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private LeaderboardResponse<ProviderPerformanceResponse> sampleProviderLeaderboard;
  private LeaderboardResponse<CourseEmployabilityResponse> sampleCourseLeaderboard;

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

    ProviderPerformanceResponse providerItem = new ProviderPerformanceResponse(
        10L,
        "TP-001",
        "Apex Training Institute",
        1500L,
        1400L,
        1350L,
        1300L,
        1100L,
        1050L,
        new BigDecimal("90.00"),
        new BigDecimal("86.67"),
        new BigDecimal("73.33"),
        new BigDecimal("70.00"),
        250L,
        1
    );
    sampleProviderLeaderboard = LeaderboardResponse.of(
        "employment_rate_pct DESC, enrollment_count DESC",
        List.of(providerItem)
    );

    CourseEmployabilityResponse courseItem = new CourseEmployabilityResponse(
        50L,
        "CRS-DATA-ENG",
        "Data Engineering Specialist",
        3L,
        "NSQF-5",
        "NSQF Level 5",
        5,
        800L,
        750L,
        720L,
        700L,
        600L,
        580L,
        new BigDecimal("90.00"),
        new BigDecimal("87.50"),
        new BigDecimal("75.00"),
        new BigDecimal("72.50"),
        new BigDecimal("10.00"),
        new BigDecimal("45000.00"),
        1
    );
    sampleCourseLeaderboard = LeaderboardResponse.of(
        "employment_rate_pct DESC, enrollment_count DESC",
        List.of(courseItem)
    );
  }

  @Nested
  @DisplayName("1. Endpoint Success Tests (HTTP 200 with ApiResponse Envelope)")
  class EndpointSuccessTests {

    @Test
    @DisplayName("GET /api/v1/dashboards/providers: returns HTTP 200 with standard ApiResponse envelope")
    void testGetProviderLeaderboardSuccess() throws Exception {
      when(institutionalAnalyticsService.getProviderLeaderboard()).thenReturn(sampleProviderLeaderboard);

      mockMvc.perform(get("/api/v1/dashboards/providers")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Training provider leaderboard retrieved successfully"))
          .andExpect(jsonPath("$.data.rankingMetric").value("employment_rate_pct DESC, enrollment_count DESC"))
          .andExpect(jsonPath("$.data.totalItems").value(1))
          .andExpect(jsonPath("$.data.items[0].providerId").value(10))
          .andExpect(jsonPath("$.data.items[0].providerCode").value("TP-001"))
          .andExpect(jsonPath("$.data.items[0].providerName").value("Apex Training Institute"))
          .andExpect(jsonPath("$.data.items[0].enrollmentCount").value(1500))
          .andExpect(jsonPath("$.data.items[0].traineeCount").value(1400))
          .andExpect(jsonPath("$.data.items[0].completedCount").value(1350))
          .andExpect(jsonPath("$.data.items[0].certifiedCount").value(1300))
          .andExpect(jsonPath("$.data.items[0].joinedPlacementCount").value(1100))
          .andExpect(jsonPath("$.data.items[0].employedCount").value(1050))
          .andExpect(jsonPath("$.data.items[0].completionRatePct").value(90.00))
          .andExpect(jsonPath("$.data.items[0].certificationRatePct").value(86.67))
          .andExpect(jsonPath("$.data.items[0].placementRatePct").value(73.33))
          .andExpect(jsonPath("$.data.items[0].employmentRatePct").value(70.00))
          .andExpect(jsonPath("$.data.items[0].activeBatchCapacity").value(250))
          .andExpect(jsonPath("$.data.items[0].rank").value(1))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/providers"));

      verify(institutionalAnalyticsService).getProviderLeaderboard();
    }

    @Test
    @DisplayName("GET /api/v1/dashboards/courses: returns HTTP 200 with standard ApiResponse envelope")
    void testGetCourseLeaderboardSuccess() throws Exception {
      when(institutionalAnalyticsService.getCourseLeaderboard()).thenReturn(sampleCourseLeaderboard);

      mockMvc.perform(get("/api/v1/dashboards/courses")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Course employability leaderboard retrieved successfully"))
          .andExpect(jsonPath("$.data.rankingMetric").value("employment_rate_pct DESC, enrollment_count DESC"))
          .andExpect(jsonPath("$.data.totalItems").value(1))
          .andExpect(jsonPath("$.data.items[0].courseId").value(50))
          .andExpect(jsonPath("$.data.items[0].courseCode").value("CRS-DATA-ENG"))
          .andExpect(jsonPath("$.data.items[0].courseName").value("Data Engineering Specialist"))
          .andExpect(jsonPath("$.data.items[0].qualificationLevelId").value(3))
          .andExpect(jsonPath("$.data.items[0].qualificationLevelCode").value("NSQF-5"))
          .andExpect(jsonPath("$.data.items[0].qualificationLevelName").value("NSQF Level 5"))
          .andExpect(jsonPath("$.data.items[0].nsqfLevel").value(5))
          .andExpect(jsonPath("$.data.items[0].enrollmentCount").value(800))
          .andExpect(jsonPath("$.data.items[0].traineeCount").value(750))
          .andExpect(jsonPath("$.data.items[0].completedCount").value(720))
          .andExpect(jsonPath("$.data.items[0].certifiedCount").value(700))
          .andExpect(jsonPath("$.data.items[0].joinedPlacementCount").value(600))
          .andExpect(jsonPath("$.data.items[0].employedCount").value(580))
          .andExpect(jsonPath("$.data.items[0].completionRatePct").value(90.00))
          .andExpect(jsonPath("$.data.items[0].certificationRatePct").value(87.50))
          .andExpect(jsonPath("$.data.items[0].placementRatePct").value(75.00))
          .andExpect(jsonPath("$.data.items[0].employmentRatePct").value(72.50))
          .andExpect(jsonPath("$.data.items[0].dropoutRatePct").value(10.00))
          .andExpect(jsonPath("$.data.items[0].avgStartingWage").value(45000.00))
          .andExpect(jsonPath("$.data.items[0].rank").value(1))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/courses"));

      verify(institutionalAnalyticsService).getCourseLeaderboard();
    }
  }

  @Nested
  @DisplayName("2. Security and Authorization Guard Tests")
  class SecurityAuthorizationTests {

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/dashboards/providers", "/api/v1/dashboards/courses"})
    @DisplayName("Unauthenticated request returns HTTP 401 Unauthorized")
    void testEndpointsRequireAuthentication(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/dashboards/providers", "/api/v1/dashboards/courses"})
    @DisplayName("Authenticated without analytics.read authority returns HTTP 403 Forbidden")
    void testEndpointsRequireAnalyticsReadAuthority(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }
  }
}
