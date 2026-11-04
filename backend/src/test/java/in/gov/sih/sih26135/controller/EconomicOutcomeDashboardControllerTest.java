package in.gov.sih.sih26135.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.response.CourseWageProgressionResponse;
import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentCategoryBreakdownResponse;
import in.gov.sih.sih26135.dto.response.EmploymentMixResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.SalaryDistributionResponse;
import in.gov.sih.sih26135.dto.response.WageProgressionResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.EconomicOutcomeDashboardService;
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
@DisplayName("EconomicOutcomeDashboardController Tests")
class EconomicOutcomeDashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private EconomicOutcomeDashboardService economicOutcomeDashboardService;

  private String tokenAnalyticsRead;
  private String tokenUnauthorized;

  private LeaderboardResponse<EmployerHiringPerformanceResponse> sampleEmployerLeaderboard;
  private EconomicOutcomeSummaryResponse sampleEconomicOutcomeSummary;

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

    EmployerHiringPerformanceResponse employerItem = new EmployerHiringPerformanceResponse(
        10L,
        "EMP-TATA",
        "Tata Consultancy Services",
        500L,
        450L,
        400L,
        new BigDecimal("80.00"),
        new BigDecimal("35000.00"),
        1
    );
    sampleEmployerLeaderboard = LeaderboardResponse.of(
        "joined_count DESC, placement_count DESC",
        List.of(employerItem)
    );

    WageProgressionResponse wage = new WageProgressionResponse(
        1000L, 800L, 750L,
        new BigDecimal("18000.00"), new BigDecimal("24000.00"),
        new BigDecimal("6000.00"), new BigDecimal("33.33")
    );
    List<SalaryDistributionResponse> dist = List.of(
        new SalaryDistributionResponse("BELOW_15K", "Below ₹15,000", 150L, new BigDecimal("20.00")),
        new SalaryDistributionResponse("FROM_15K_TO_25K", "₹15,000–₹25,000", 375L, new BigDecimal("50.00")),
        new SalaryDistributionResponse("FROM_25K_TO_50K", "₹25,000–₹50,000", 188L, new BigDecimal("25.00")),
        new SalaryDistributionResponse("ABOVE_50K", "Above ₹50,000", 37L, new BigDecimal("5.00"))
    );
    EmploymentMixResponse mix = new EmploymentMixResponse(
        1000L, 900L, 700L, 600L, 200L, 150L, 50L, 450L, 150L,
        new BigDecimal("180.50"), new BigDecimal("120.00"),
        new BigDecimal("60.00"), new BigDecimal("20.00"),
        new BigDecimal("15.00"), new BigDecimal("5.00")
    );
    List<EmploymentCategoryBreakdownResponse> categories = List.of(
        new EmploymentCategoryBreakdownResponse(
            "WAGE_EMPLOYMENT", "Wage employment", true, false, false, false,
            600L, 550L, 450L, new BigDecimal("60.00"), new BigDecimal("210.00")
        )
    );
    List<CourseWageProgressionResponse> courses = List.of(
        new CourseWageProgressionResponse(
            101L, "CRS-JAVA", "Full Stack Java", 200L,
            new BigDecimal("20000.00"), new BigDecimal("28000.00"), new BigDecimal("40.00")
        )
    );

    sampleEconomicOutcomeSummary = new EconomicOutcomeSummaryResponse(
        wage, dist, mix, categories, courses
    );
  }

  @Nested
  @DisplayName("GET /api/v1/dashboards/employers")
  class EmployerLeaderboardEndpointTests {

    @Test
    @DisplayName("Authorized user with analytics.read receives HTTP 200 and ApiResponse envelope")
    void testGetEmployerLeaderboardAuthorized() throws Exception {
      when(economicOutcomeDashboardService.getEmployerLeaderboard())
          .thenReturn(sampleEmployerLeaderboard);

      mockMvc.perform(get("/api/v1/dashboards/employers")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Employer hiring leaderboard retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/employers"))
          .andExpect(jsonPath("$.data.rankingMetric").value("joined_count DESC, placement_count DESC"))
          .andExpect(jsonPath("$.data.totalItems").value(1))
          .andExpect(jsonPath("$.data.items[0].employerCode").value("EMP-TATA"))
          .andExpect(jsonPath("$.data.items[0].joinedCount").value(400))
          .andExpect(jsonPath("$.data.items[0].joinRatePct").value(80.00))
          .andExpect(jsonPath("$.data.items[0].avgJoiningSalary").value(35000.00))
          .andExpect(jsonPath("$.data.items[0].rank").value(1));

      verify(economicOutcomeDashboardService).getEmployerLeaderboard();
    }
  }

  @Nested
  @DisplayName("GET /api/v1/dashboards/economic-outcomes")
  class EconomicOutcomesEndpointTests {

    @Test
    @DisplayName("Authorized user with analytics.read receives HTTP 200 and consolidated ApiResponse envelope")
    void testGetEconomicOutcomeSummaryAuthorized() throws Exception {
      when(economicOutcomeDashboardService.getEconomicOutcomeSummary())
          .thenReturn(sampleEconomicOutcomeSummary);

      mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("Economic outcome analytics retrieved successfully"))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/economic-outcomes"))
          .andExpect(jsonPath("$.data.wageProgression.employmentSpellCount").value(1000))
          .andExpect(jsonPath("$.data.wageProgression.employmentWithSalaryRowCount").doesNotExist())
          .andExpect(jsonPath("$.data.wageProgression.avgSalaryChangeAmount").value(6000.00))
          .andExpect(jsonPath("$.data.wageProgression.avgSalaryChangePct").value(33.33))
          .andExpect(jsonPath("$.data.salaryDistribution").isArray())
          .andExpect(jsonPath("$.data.salaryDistribution[0].bracketCode").value("BELOW_15K"))
          .andExpect(jsonPath("$.data.salaryDistribution[1].bracketCode").value("FROM_15K_TO_25K"))
          .andExpect(jsonPath("$.data.employmentMix.employmentSpellCount").value(1000))
          .andExpect(jsonPath("$.data.employmentMix.wageEmploymentSharePct").value(60.00))
          .andExpect(jsonPath("$.data.categoryBreakdown[0].engagementTypeCode").value("WAGE_EMPLOYMENT"))
          .andExpect(jsonPath("$.data.courseWageProgression[0].courseCode").value("CRS-JAVA"));

      verify(economicOutcomeDashboardService).getEconomicOutcomeSummary();
    }
  }

  @Nested
  @DisplayName("Security & Authorization Tests")
  class SecurityTests {

    @ParameterizedTest(name = "Unauthenticated request to {0} returns HTTP 401 Unauthorized")
    @ValueSource(strings = {
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes"
    })
    void testUnauthenticatedEndpointsReturn401(String url) throws Exception {
      mockMvc.perform(get(url)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "Forbidden request to {0} without analytics.read returns HTTP 403 Forbidden")
    @ValueSource(strings = {
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes"
    })
    void testUnauthorizedEndpointsReturn403(String url) throws Exception {
      mockMvc.perform(get(url)
              .header(HttpHeaders.AUTHORIZATION, tokenUnauthorized)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }
  }
}
