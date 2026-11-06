package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Live database integration tests for Executive and National Overview Dashboard queries (Q01, Q02, Q03).
 * Verifies exact schema mapping, type consistency, calculation precision, and endpoint consistency
 * against the dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ExecutiveDashboardService Live Database Integration Tests (SIH26135_TEST)")
class ExecutiveDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ExecutiveDashboardService executiveDashboardService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String tokenAnalyticsRead;

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
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Live Q01 query execution against vw_* views returns valid KPI strip with correct counts and rates")
  void testLiveQ01KpiStrip() {
    DashboardKpiStripResponse kpi = executiveDashboardService.getDashboardKpiStrip();
    assertThat(kpi).isNotNull();

    // 1. Verify all 12 count metrics are non-null and non-negative
    assertThat(kpi.traineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.enrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.enrolledTraineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.completedEnrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.certifiedEnrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.placementCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.joinedPlacementCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.employmentSpellCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.employedTraineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.currentEmploymentSpellCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.currentSelfEmploymentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(kpi.currentApprenticeshipCount()).isNotNull().isGreaterThanOrEqualTo(0L);

    // 2. Structural grain invariants
    assertThat(kpi.enrolledTraineeCount()).isLessThanOrEqualTo(kpi.traineeCount());
    assertThat(kpi.enrolledTraineeCount()).isLessThanOrEqualTo(kpi.enrollmentCount());
    assertThat(kpi.completedEnrollmentCount()).isLessThanOrEqualTo(kpi.enrollmentCount());
    assertThat(kpi.joinedPlacementCount()).isLessThanOrEqualTo(kpi.placementCount());
    assertThat(kpi.employedTraineeCount()).isLessThanOrEqualTo(kpi.traineeCount());
    assertThat(kpi.currentEmploymentSpellCount()).isLessThanOrEqualTo(kpi.employmentSpellCount());
    assertThat(kpi.currentSelfEmploymentCount()).isLessThanOrEqualTo(kpi.currentEmploymentSpellCount());
    assertThat(kpi.currentApprenticeshipCount()).isLessThanOrEqualTo(kpi.currentEmploymentSpellCount());

    // 3. Denominators and rate calculations verification
    if (kpi.enrollmentCount() > 0L) {
      BigDecimal expectedCompletionRate = AnalyticsCalculationUtils.calculatePercentage(
          kpi.completedEnrollmentCount(), kpi.enrollmentCount());
      BigDecimal expectedCertificationRate = AnalyticsCalculationUtils.calculatePercentage(
          kpi.certifiedEnrollmentCount(), kpi.enrollmentCount());
      BigDecimal expectedPlacementRate = AnalyticsCalculationUtils.calculatePercentage(
          kpi.joinedPlacementCount(), kpi.enrollmentCount());
      BigDecimal expectedEmploymentRate = AnalyticsCalculationUtils.calculatePercentage(
          kpi.employedTraineeCount(), kpi.enrollmentCount());

      assertThat(kpi.completionRatePct()).isEqualByComparingTo(expectedCompletionRate);
      assertThat(kpi.completionRatePct().scale()).isEqualTo(2);

      assertThat(kpi.certificationRatePct()).isEqualByComparingTo(expectedCertificationRate);
      assertThat(kpi.certificationRatePct().scale()).isEqualTo(2);

      assertThat(kpi.placementRatePct()).isEqualByComparingTo(expectedPlacementRate);
      assertThat(kpi.placementRatePct().scale()).isEqualTo(2);

      assertThat(kpi.employmentRatePct()).isEqualByComparingTo(expectedEmploymentRate);
      assertThat(kpi.employmentRatePct().scale()).isEqualTo(2);
    } else {
      // Safe null handling for zero-enrollment state (SQL NULLIF and AnalyticsCalculationUtils convention)
      assertThat(kpi.completionRatePct()).isNull();
      assertThat(kpi.certificationRatePct()).isNull();
      assertThat(kpi.placementRatePct()).isNull();
      assertThat(kpi.employmentRatePct()).isNull();
    }
  }

  @Test
  @DisplayName("Live Q02 query execution against vw_enrollment_outcome_fact returns valid pipeline funnel")
  void testLiveQ02PipelineFunnel() {
    PipelineFunnelResponse funnel = executiveDashboardService.getTrainingPipelineFunnel();
    assertThat(funnel).isNotNull();

    // 1. Verify 5 pipeline stage counts are non-null and non-negative
    assertThat(funnel.enrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(funnel.completedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(funnel.certifiedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(funnel.joinedPlacementCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(funnel.employedCount()).isNotNull().isGreaterThanOrEqualTo(0L);

    // 2. Stage relationship invariants
    assertThat(funnel.completedCount()).isLessThanOrEqualTo(funnel.enrollmentCount());
    assertThat(funnel.certifiedCount()).isLessThanOrEqualTo(funnel.enrollmentCount());

    // 3. Rate bounds and denominator verification against stage counts and enrollmentCount
    if (funnel.enrollmentCount() > 0L) {
      BigDecimal expectedCompletionRate = AnalyticsCalculationUtils.calculatePercentage(
          funnel.completedCount(), funnel.enrollmentCount());
      BigDecimal expectedCertificationRate = AnalyticsCalculationUtils.calculatePercentage(
          funnel.certifiedCount(), funnel.enrollmentCount());
      BigDecimal expectedPlacementRate = AnalyticsCalculationUtils.calculatePercentage(
          funnel.joinedPlacementCount(), funnel.enrollmentCount());
      BigDecimal expectedEmploymentRate = AnalyticsCalculationUtils.calculatePercentage(
          funnel.employedCount(), funnel.enrollmentCount());

      assertThat(funnel.completionRatePct()).isEqualByComparingTo(expectedCompletionRate);
      assertThat(funnel.completionRatePct().scale()).isEqualTo(2);

      assertThat(funnel.certificationRatePct()).isEqualByComparingTo(expectedCertificationRate);
      assertThat(funnel.certificationRatePct().scale()).isEqualTo(2);

      assertThat(funnel.placementRatePct()).isEqualByComparingTo(expectedPlacementRate);
      assertThat(funnel.placementRatePct().scale()).isEqualTo(2);

      assertThat(funnel.employmentRatePct()).isEqualByComparingTo(expectedEmploymentRate);
      assertThat(funnel.employmentRatePct().scale()).isEqualTo(2);
    } else {
      // Safe null handling for zero-enrollment state (SQL NULLIF convention)
      assertThat(funnel.completionRatePct()).isNull();
      assertThat(funnel.certificationRatePct()).isNull();
      assertThat(funnel.placementRatePct()).isNull();
      assertThat(funnel.employmentRatePct()).isNull();
    }
  }

  @Test
  @DisplayName("Live Q03 query execution against vw_program_outcome_summary returns valid matrix with Long counts")
  void testLiveQ03SchemeProgramMatrix() {
    LeaderboardResponse<SchemeProgramPerformanceResponse> matrix =
        executiveDashboardService.getSchemeProgramPerformanceMatrix();
    assertThat(matrix).isNotNull();
    assertThat(matrix.rankingMetric()).isEqualTo("employmentRatePct");
    assertThat(matrix.totalItems()).isEqualTo((long) matrix.items().size());

    // Verify all rows map identifiers, strongly typed Long count fields, and scale 2 rates
    for (SchemeProgramPerformanceResponse row : matrix.items()) {
      assertThat(row.programId()).isNotNull().isPositive();
      assertThat(row.programCode()).isNotBlank();
      assertThat(row.programName()).isNotBlank();
      assertThat(row.schemeId()).isNotNull().isPositive();
      assertThat(row.schemeCode()).isNotBlank();
      assertThat(row.schemeName()).isNotBlank();

      // All count metrics must be Long and non-negative
      assertThat(row.enrollmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.traineeCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.completedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.certifiedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.joinedPlacementCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.employedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.selfEmploymentSpellCount()).isNotNull().isGreaterThanOrEqualTo(0L);
      assertThat(row.apprenticeshipSpellCount()).isNotNull().isGreaterThanOrEqualTo(0L);

      // Hierarchical count integrity
      assertThat(row.traineeCount()).isLessThanOrEqualTo(row.enrollmentCount());
      assertThat(row.completedCount()).isLessThanOrEqualTo(row.enrollmentCount());
      assertThat(row.certifiedCount()).isLessThanOrEqualTo(row.enrollmentCount());

      // Percentages are BigDecimal with scale 2 or null within [0.00, 100.00]
      if (row.completionRatePct() != null) {
        assertThat(row.completionRatePct().scale()).isEqualTo(2);
        assertThat(row.completionRatePct()).isBetween(BigDecimal.ZERO, new BigDecimal("100.00"));
      }
      if (row.certificationRatePct() != null) {
        assertThat(row.certificationRatePct().scale()).isEqualTo(2);
        assertThat(row.certificationRatePct()).isBetween(BigDecimal.ZERO, new BigDecimal("100.00"));
      }
      if (row.placementRatePct() != null) {
        assertThat(row.placementRatePct().scale()).isEqualTo(2);
        assertThat(row.placementRatePct()).isBetween(BigDecimal.ZERO, new BigDecimal("100.00"));
      }
      if (row.employmentRatePct() != null) {
        assertThat(row.employmentRatePct().scale()).isEqualTo(2);
        assertThat(row.employmentRatePct()).isBetween(BigDecimal.ZERO, new BigDecimal("100.00"));
      }

      // Denominator consistency
      if (row.enrollmentCount() == 0L) {
        assertThat(row.completionRatePct()).isNull();
        assertThat(row.certificationRatePct()).isNull();
        assertThat(row.placementRatePct()).isNull();
        assertThat(row.employmentRatePct()).isNull();
      }
    }
  }

  @Test
  @DisplayName("Live consolidated dashboard retrieval succeeds and contains consistent Q01, Q02, and Q03 components")
  void testLiveExecutiveDashboard() {
    ExecutiveDashboardResponse response = executiveDashboardService.getExecutiveDashboard();
    assertThat(response).isNotNull();

    // 1. Verify component presence
    DashboardKpiStripResponse kpi = response.kpiStrip();
    PipelineFunnelResponse funnel = response.pipelineFunnel();
    LeaderboardResponse<SchemeProgramPerformanceResponse> matrix = response.schemeProgramMatrix();

    assertThat(kpi).isNotNull();
    assertThat(funnel).isNotNull();
    assertThat(matrix).isNotNull();

    // 2. Component consistency
    assertThat(kpi.enrollmentCount()).isEqualTo(funnel.enrollmentCount());
    assertThat(kpi.completedEnrollmentCount()).isEqualTo(funnel.completedCount());
    assertThat(kpi.certifiedEnrollmentCount()).isEqualTo(funnel.certifiedCount());
    assertThat(matrix.rankingMetric()).isEqualTo("employmentRatePct");
  }

  @Test
  @DisplayName("Overview and National endpoints retrieve identical aggregation scope via HTTP (semantic aliases)")
  void testLiveOverviewAndNationalEndpointsConsistency() throws Exception {
    MvcResult overviewResult = mockMvc.perform(get("/api/v1/dashboards/overview")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/overview"))
        .andExpect(jsonPath("$.data.kpiStrip").exists())
        .andExpect(jsonPath("$.data.pipelineFunnel").exists())
        .andExpect(jsonPath("$.data.schemeProgramMatrix").exists())
        .andReturn();

    MvcResult nationalResult = mockMvc.perform(get("/api/v1/dashboards/national")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/national"))
        .andExpect(jsonPath("$.data.kpiStrip").exists())
        .andExpect(jsonPath("$.data.pipelineFunnel").exists())
        .andExpect(jsonPath("$.data.schemeProgramMatrix").exists())
        .andReturn();

    // Compare JSON content trees to verify identical aggregation data
    ObjectMapper mapper = new ObjectMapper();
    JsonNode overviewJson = mapper.readTree(overviewResult.getResponse().getContentAsString());
    JsonNode nationalJson = mapper.readTree(nationalResult.getResponse().getContentAsString());

    // Both endpoints expose equivalent aggregation data
    assertThat(overviewJson.get("data")).isEqualTo(nationalJson.get("data"));

    // Path fields correctly identify their respective routes
    assertThat(overviewJson.get("path").asText()).isEqualTo("/api/v1/dashboards/overview");
    assertThat(nationalJson.get("path").asText()).isEqualTo("/api/v1/dashboards/national");
  }
}
