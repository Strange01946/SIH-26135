package in.gov.sih.sih26135.service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import in.gov.sih.sih26135.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.GeographicDashboardRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Live database integration tests for Geographic, State, and District Heatmap Dashboards.
 * Verifies exact formal query Q06 execution, state outcome rollups, mathematical reconciliation,
 * and REST endpoint behavior against the dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GeographicDashboardService Live Database Integration Tests (SIH26135_TEST)")
class GeographicDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private GeographicDashboardService geographicDashboardService;

  @Autowired
  private GeographicDashboardRepository geographicDashboardRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final Long DETERMINISTIC_STATE_ID = 99991L;
  private static final String DETERMINISTIC_STATE_CODE = "XX";
  private static final String DETERMINISTIC_STATE_NAME = "Deterministic Integration State 99991";

  private static final Long DETERMINISTIC_DISTRICT_ID = 99991L;
  private static final String DETERMINISTIC_DISTRICT_CODE = "INT_TEST_D99991";
  private static final String DETERMINISTIC_DISTRICT_NAME = "Deterministic Integration District 99991";

  private String tokenAnalyticsRead;
  private Long resolvedStateId;
  private Long resolvedDistrictId;
  private boolean stateFixtureInserted = false;
  private boolean districtFixtureInserted = false;

  @BeforeEach
  void enforceDatabaseIsolationGuardAndSetupTokens() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (1, 'admin', 'admin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE state_id = NULL, district_id = NULL, training_provider_id = NULL, employer_id = NULL
        """);

    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of("ROLE_super_admin", "analytics.read")
    );

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"), new SimpleGrantedAuthority("analytics.read"))
        )
    );

    // Identify existing active state with districts, or insert deterministic fixture records
    List<Long> statesWithDistricts = jdbcTemplate.queryForList(
        "SELECT DISTINCT s.state_id FROM states s JOIN districts d ON d.state_id = s.state_id "
            + "WHERE s.deleted_at IS NULL AND d.deleted_at IS NULL ORDER BY s.state_id ASC LIMIT 1",
        Long.class);

    if (!statesWithDistricts.isEmpty()) {
      resolvedStateId = statesWithDistricts.get(0);
      resolvedDistrictId = jdbcTemplate.queryForObject(
          "SELECT district_id FROM districts WHERE state_id = ? AND deleted_at IS NULL ORDER BY district_id ASC LIMIT 1",
          Long.class, resolvedStateId);
      stateFixtureInserted = false;
      districtFixtureInserted = false;
    } else {
      // Verify that deterministic fixture IDs are unoccupied before insertion; fail closed if occupied
      Integer stateCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM states WHERE state_id = ?", Integer.class, DETERMINISTIC_STATE_ID);
      if (stateCount != null && stateCount > 0) {
        throw new IllegalStateException(
            "Cannot insert test state fixture: state_id " + DETERMINISTIC_STATE_ID
                + " is already occupied by a pre-existing record in " + MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      }

      Integer districtCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM districts WHERE district_id = ?", Integer.class, DETERMINISTIC_DISTRICT_ID);
      if (districtCount != null && districtCount > 0) {
        throw new IllegalStateException(
            "Cannot insert test district fixture: district_id " + DETERMINISTIC_DISTRICT_ID
                + " is already occupied by a pre-existing record in " + MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      }

      jdbcTemplate.update("""
          INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
          VALUES (?, ?, ?, ?, 1, NOW(), NOW())
          """, DETERMINISTIC_STATE_ID, DETERMINISTIC_STATE_CODE, DETERMINISTIC_STATE_NAME, "LG99991");
      resolvedStateId = DETERMINISTIC_STATE_ID;
      stateFixtureInserted = true;

      jdbcTemplate.update("""
          INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, 1, NOW(), NOW())
          """, DETERMINISTIC_DISTRICT_ID, DETERMINISTIC_STATE_ID, DETERMINISTIC_DISTRICT_CODE, DETERMINISTIC_DISTRICT_NAME, "LGD99991");
      resolvedDistrictId = DETERMINISTIC_DISTRICT_ID;
      districtFixtureInserted = true;
    }
  }

  @AfterEach
  void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    if (districtFixtureInserted) {
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", DETERMINISTIC_DISTRICT_ID);
      districtFixtureInserted = false;
    }
    if (stateFixtureInserted) {
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", DETERMINISTIC_STATE_ID);
      stateFixtureInserted = false;
    }
  }

  @Test
  @DisplayName("Live Q06 query execution against vw_district_outcome_summary returns valid district rankings")
  void testLiveQ06DistrictOutcomes() {
    LeaderboardResponse<DistrictPerformanceResponse> leaderboard =
        geographicDashboardService.getDistrictRankings();
    assertThat(leaderboard).isNotNull();
    assertThat(leaderboard.rankingMetric()).contains("employmentRatePct");
    assertThat(leaderboard.totalItems()).isEqualTo((long) leaderboard.items().size());

    // Verify all rows map valid identifiers, Long count metrics, and scale 2 rates
    for (DistrictPerformanceResponse district : leaderboard.items()) {
      assertThat(district.districtId()).isNotNull().isPositive();
      assertThat(district.districtCode()).isNotBlank();
      assertThat(district.districtName()).isNotBlank();
      assertThat(district.stateId()).isNotNull().isPositive();
      assertThat(district.stateCode()).isNotBlank();
      assertThat(district.stateName()).isNotBlank();
      assertThat(district.rank()).isNotNull().isPositive();

      // All counts must be Long and non-negative
      assertThat(district.enrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(district.traineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(district.completedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(district.certifiedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(district.joinedPlacementCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(district.employedCount()).isNotNull().isGreaterThanOrEqualTo(0L);

      // Structural grain invariants
      assertThat(district.completedCount()).isLessThanOrEqualTo(district.enrollmentCount());
      assertThat(district.certifiedCount()).isLessThanOrEqualTo(district.enrollmentCount());

      // Percentages are BigDecimal scale 2 or null when denominator is 0
      if (district.enrollmentCount() > 0L) {
        BigDecimal expectedCompletionRate = AnalyticsCalculationUtils.calculatePercentage(
            district.completedCount(), district.enrollmentCount());
        BigDecimal expectedCertificationRate = AnalyticsCalculationUtils.calculatePercentage(
            district.certifiedCount(), district.enrollmentCount());
        BigDecimal expectedPlacementRate = AnalyticsCalculationUtils.calculatePercentage(
            district.joinedPlacementCount(), district.enrollmentCount());
        BigDecimal expectedEmploymentRate = AnalyticsCalculationUtils.calculatePercentage(
            district.employedCount(), district.enrollmentCount());

        assertThat(district.completionRatePct()).isEqualByComparingTo(expectedCompletionRate);
        assertThat(district.completionRatePct().scale()).isEqualTo(2);

        assertThat(district.certificationRatePct()).isEqualByComparingTo(expectedCertificationRate);
        assertThat(district.certificationRatePct().scale()).isEqualTo(2);

        assertThat(district.placementRatePct()).isEqualByComparingTo(expectedPlacementRate);
        assertThat(district.placementRatePct().scale()).isEqualTo(2);

        assertThat(district.employmentRatePct()).isEqualByComparingTo(expectedEmploymentRate);
        assertThat(district.employmentRatePct().scale()).isEqualTo(2);
      } else {
        assertThat(district.completionRatePct()).isNull();
        assertThat(district.certificationRatePct()).isNull();
        assertThat(district.placementRatePct()).isNull();
        assertThat(district.employmentRatePct()).isNull();
      }
    }
  }

  @Test
  @DisplayName("Live national geographic overview provides consistent state rollups and reconciles mathematically")
  void testLiveNationalGeographicOverview() {
    GeographicOverviewResponse overview = geographicDashboardService.getNationalGeographicOverview();
    assertThat(overview).isNotNull();

    // Verify counts are non-negative
    assertThat(overview.totalStates()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalDistricts()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalEnrollments()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalCompleted()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalCertified()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalJoinedPlacement()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(overview.totalEmployed()).isNotNull().isGreaterThanOrEqualTo(0L);

    // Verify mathematical reconciliation between national totals and state summaries
    long sumStateEnrollments = overview.stateSummaries().stream()
        .mapToLong(StatePerformanceResponse::enrollmentCount)
        .sum();
    long sumStateCompleted = overview.stateSummaries().stream()
        .mapToLong(StatePerformanceResponse::completedCount)
        .sum();
    long sumStateCertified = overview.stateSummaries().stream()
        .mapToLong(StatePerformanceResponse::certifiedCount)
        .sum();

    assertThat(overview.totalEnrollments()).isEqualTo(sumStateEnrollments);
    assertThat(overview.totalCompleted()).isEqualTo(sumStateCompleted);
    assertThat(overview.totalCertified()).isEqualTo(sumStateCertified);

    // Verify rate precision and null safety
    if (overview.totalEnrollments() > 0L) {
      BigDecimal expectedCompletionRate = AnalyticsCalculationUtils.calculatePercentage(
          overview.totalCompleted(), overview.totalEnrollments());
      BigDecimal expectedCertificationRate = AnalyticsCalculationUtils.calculatePercentage(
          overview.totalCertified(), overview.totalEnrollments());
      BigDecimal expectedPlacementRate = AnalyticsCalculationUtils.calculatePercentage(
          overview.totalJoinedPlacement(), overview.totalEnrollments());
      BigDecimal expectedEmploymentRate = AnalyticsCalculationUtils.calculatePercentage(
          overview.totalEmployed(), overview.totalEnrollments());

      assertThat(overview.overallCompletionRatePct()).isEqualByComparingTo(expectedCompletionRate);
      assertThat(overview.overallCompletionRatePct().scale()).isEqualTo(2);

      assertThat(overview.overallCertificationRatePct()).isEqualByComparingTo(expectedCertificationRate);
      assertThat(overview.overallCertificationRatePct().scale()).isEqualTo(2);

      assertThat(overview.overallPlacementRatePct()).isEqualByComparingTo(expectedPlacementRate);
      assertThat(overview.overallPlacementRatePct().scale()).isEqualTo(2);

      assertThat(overview.overallEmploymentRatePct()).isEqualByComparingTo(expectedEmploymentRate);
      assertThat(overview.overallEmploymentRatePct().scale()).isEqualTo(2);
    } else {
      assertThat(overview.overallCompletionRatePct()).isNull();
      assertThat(overview.overallCertificationRatePct()).isNull();
      assertThat(overview.overallPlacementRatePct()).isNull();
      assertThat(overview.overallEmploymentRatePct()).isNull();
    }

    // Verify regional disparity component
    assertThat(overview.regionalDisparity()).isNotNull();
    assertThat(overview.regionalDisparity().dataAvailabilityNotes())
        .containsKey("ruralUrbanClassification")
        .containsKey("aspirationalDistrictIndicator")
        .containsKey("populationDensity");
  }

  @Test
  @DisplayName("Live HTTP GET /api/v1/dashboards/geography returns HTTP 200 with complete ApiResponse envelope")
  void testLiveHttpNationalGeographyEndpoint() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/dashboards/geography")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/geography"))
        .andExpect(jsonPath("$.data.totalStates").exists())
        .andExpect(jsonPath("$.data.totalDistricts").exists())
        .andExpect(jsonPath("$.data.stateSummaries").isArray())
        .andExpect(jsonPath("$.data.districtRankings").exists())
        .andExpect(jsonPath("$.data.regionalDisparity").exists())
        .andReturn();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
    assertThat(json.get("success").asBoolean()).isTrue();
    assertThat(json.get("data").has("totalEnrollments")).isTrue();
  }

  @Test
  @DisplayName("Live HTTP GET /api/v1/dashboards/states/{stateId} returns state analytics for existing state and 404 for nonexistent state")
  void testLiveHttpStateGeographyEndpoint() throws Exception {
    // 1. Nonexistent state returns 404
    mockMvc.perform(get("/api/v1/dashboards/states/999999")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

    // 2. Existing state guaranteed to execute HTTP 200 path without silent skips
    assertThat(resolvedStateId).isNotNull().isPositive();
    mockMvc.perform(get("/api/v1/dashboards/states/" + resolvedStateId)
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/states/" + resolvedStateId))
        .andExpect(jsonPath("$.data.stateId").value(resolvedStateId))
        .andExpect(jsonPath("$.data.stateSummary").exists())
        .andExpect(jsonPath("$.data.districts").isArray())
        .andExpect(jsonPath("$.data.stateDisparity").exists());
  }

  @Test
  @DisplayName("Live HTTP GET /api/v1/dashboards/districts/{districtId} returns district analytics for existing district and 404 for nonexistent district")
  void testLiveHttpDistrictGeographyEndpoint() throws Exception {
    // 1. Nonexistent district returns 404
    mockMvc.perform(get("/api/v1/dashboards/districts/999999")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

    // 2. Existing district guaranteed to execute HTTP 200 path without silent skips
    assertThat(resolvedDistrictId).isNotNull().isPositive();
    mockMvc.perform(get("/api/v1/dashboards/districts/" + resolvedDistrictId)
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/districts/" + resolvedDistrictId))
        .andExpect(jsonPath("$.data.districtId").value(resolvedDistrictId))
        .andExpect(jsonPath("$.data.regionalIndicators").exists());
  }
}
