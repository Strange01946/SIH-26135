package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.InstitutionalAnalyticsRepository;
import in.gov.sih.sih26135.repository.analytics.projection.CourseEmployabilityProjection;
import in.gov.sih.sih26135.repository.analytics.projection.ProviderPerformanceProjection;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Live database integration tests for Institutional & Curriculum Performance Dashboards.
 * Verifies exact formal queries Q04 and Q05 execution, mathematical reconciliation,
 * and REST endpoint behavior against the dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("InstitutionalAnalyticsService Live Database Integration Tests (SIH26135_TEST)")
class InstitutionalAnalyticsIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private InstitutionalAnalyticsService institutionalAnalyticsService;

  @Autowired
  private InstitutionalAnalyticsRepository institutionalAnalyticsRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private final ObjectMapper objectMapper = new ObjectMapper();
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
  @DisplayName("Repository: execute formal Q04 provider query against live SIH26135_TEST")
  void testLiveFormalQ04Execution() {
    List<ProviderPerformanceProjection> providers =
        institutionalAnalyticsRepository.getFormalQ04ProviderLeaderboard();

    assertThat(providers).isNotNull();

    // Verify ordering and invariants if data is present
    for (int i = 0; i < providers.size(); i++) {
      ProviderPerformanceProjection p = providers.get(i);
      assertThat(p.providerId()).isNotNull();
      assertThat(p.providerCode()).isNotBlank();
      assertThat(p.providerName()).isNotBlank();

      if (p.employmentRatePct() != null) {
        assertThat(p.employmentRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (p.placementRatePct() != null) {
        assertThat(p.placementRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (p.certificationRatePct() != null) {
        assertThat(p.certificationRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (p.completionRatePct() != null) {
        assertThat(p.completionRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (p.activeBatchCapacity() != null) {
        assertThat(p.activeBatchCapacity().longValue()).isGreaterThanOrEqualTo(0L);
      }

      // Check ordering: non-null employment rates come first, descending
      if (i > 0) {
        ProviderPerformanceProjection prev = providers.get(i - 1);
        if (prev.employmentRatePct() != null && p.employmentRatePct() != null) {
          assertThat(prev.employmentRatePct()).isGreaterThanOrEqualTo(p.employmentRatePct());
        }
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q05 course query against live SIH26135_TEST")
  void testLiveFormalQ05Execution() {
    List<CourseEmployabilityProjection> courses =
        institutionalAnalyticsRepository.getFormalQ05CourseEmployability();

    assertThat(courses).isNotNull();

    // Verify ordering and invariants if data is present
    for (int i = 0; i < courses.size(); i++) {
      CourseEmployabilityProjection c = courses.get(i);
      assertThat(c.courseId()).isNotNull();
      assertThat(c.courseCode()).isNotBlank();
      assertThat(c.courseName()).isNotBlank();

      if (c.employmentRatePct() != null) {
        assertThat(c.employmentRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (c.placementRatePct() != null) {
        assertThat(c.placementRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (c.certificationRatePct() != null) {
        assertThat(c.certificationRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (c.completionRatePct() != null) {
        assertThat(c.completionRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (c.avgStartingWage() != null) {
        assertThat(c.avgStartingWage()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
      }

      // Check ordering: non-null employment rates come first, descending
      if (i > 0) {
        CourseEmployabilityProjection prev = courses.get(i - 1);
        if (prev.employmentRatePct() != null && c.employmentRatePct() != null) {
          assertThat(prev.employmentRatePct()).isGreaterThanOrEqualTo(c.employmentRatePct());
        }
      }
    }
  }

  @Test
  @DisplayName("Service: retrieve provider leaderboard and verify sequential ranks and DTO mapping")
  void testLiveProviderLeaderboardService() {
    LeaderboardResponse<ProviderPerformanceResponse> response =
        institutionalAnalyticsService.getProviderLeaderboard();

    assertThat(response).isNotNull();
    assertThat(response.rankingMetric()).isEqualTo("employment_rate_pct DESC, enrollment_count DESC");

    List<ProviderPerformanceResponse> items = response.items();
    assertThat(response.totalItems()).isEqualTo(items.size());

    for (int i = 0; i < items.size(); i++) {
      ProviderPerformanceResponse item = items.get(i);
      assertThat(item.rank()).isEqualTo(i + 1);
      assertThat(item.providerId()).isNotNull();
      assertThat(item.providerCode()).isNotBlank();
    }
  }

  @Test
  @DisplayName("Service: retrieve course leaderboard and verify dropout rates and qualification grouping")
  void testLiveCourseLeaderboardService() {
    LeaderboardResponse<CourseEmployabilityResponse> response =
        institutionalAnalyticsService.getCourseLeaderboard();

    assertThat(response).isNotNull();
    assertThat(response.rankingMetric()).isEqualTo("employment_rate_pct DESC, enrollment_count DESC");

    List<CourseEmployabilityResponse> items = response.items();
    assertThat(response.totalItems()).isEqualTo(items.size());

    for (int i = 0; i < items.size(); i++) {
      CourseEmployabilityResponse item = items.get(i);
      assertThat(item.rank()).isEqualTo(i + 1);
      assertThat(item.courseId()).isNotNull();
      assertThat(item.courseCode()).isNotBlank();

      if (item.dropoutRatePct() != null) {
        assertThat(item.dropoutRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
    }

    Map<String, List<CourseEmployabilityResponse>> grouped =
        institutionalAnalyticsService.getCourseLeaderboardByQualificationLevel();
    assertThat(grouped).isNotNull();

    int totalInGroups = grouped.values().stream().mapToInt(List::size).sum();
    assertThat(totalInGroups).isEqualTo(items.size());
  }

  @Test
  @DisplayName("HTTP: GET /api/v1/dashboards/providers returns HTTP 200 with valid ApiResponse")
  void testLiveProviderEndpoint() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/dashboards/providers")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Training provider leaderboard retrieved successfully"))
        .andExpect(jsonPath("$.data.rankingMetric").value("employment_rate_pct DESC, enrollment_count DESC"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/providers"))
        .andReturn();

    JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(jsonNode.hasNonNull("data")).isTrue();
    JsonNode dataNode = jsonNode.get("data");
    assertThat(dataNode.has("totalItems")).isTrue();
    assertThat(dataNode.has("items")).isTrue();
  }

  @Test
  @DisplayName("HTTP: GET /api/v1/dashboards/courses returns HTTP 200 with valid ApiResponse")
  void testLiveCourseEndpoint() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/dashboards/courses")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Course employability leaderboard retrieved successfully"))
        .andExpect(jsonPath("$.data.rankingMetric").value("employment_rate_pct DESC, enrollment_count DESC"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/courses"))
        .andReturn();

    JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(jsonNode.hasNonNull("data")).isTrue();
    JsonNode dataNode = jsonNode.get("data");
    assertThat(dataNode.has("totalItems")).isTrue();
    assertThat(dataNode.has("items")).isTrue();
  }
}
