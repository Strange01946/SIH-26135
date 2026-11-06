package in.gov.sih.sih26135.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
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
import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.repository.analytics.EconomicOutcomeDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.CourseWageProgressionProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmployerHiringProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentCategoryBreakdownProjection;
import in.gov.sih.sih26135.repository.analytics.projection.EmploymentMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SalaryBracketCountProjection;
import in.gov.sih.sih26135.repository.analytics.projection.WageProgressionProjection;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
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
 * Live database integration tests for Employer Hiring, Wage Progression,
 * Economic Uplift, and Employment Mix Dashboards.
 * Verifies exact formal queries Q07–Q11 execution, mathematical reconciliation,
 * and REST endpoint behavior against dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EconomicOutcomeDashboard Live Database Integration Tests (SIH26135_TEST)")
class EconomicOutcomeDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EconomicOutcomeDashboardService economicOutcomeDashboardService;

  @Autowired
  private EconomicOutcomeDashboardRepository economicOutcomeDashboardRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

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

  @Test
  @DisplayName("Repository: execute formal Q07 employer query against live SIH26135_TEST")
  void testLiveFormalQ07Execution() {
    List<EmployerHiringProjection> employers =
        economicOutcomeDashboardRepository.getFormalQ07EmployerLeaderboard();

    assertThat(employers).isNotNull();

    for (EmployerHiringProjection emp : employers) {
      assertThat(emp.employerId()).isNotNull();
      assertThat(emp.employerCode()).isNotBlank();
      assertThat(emp.employerName()).isNotBlank();

      if (emp.placementCount() != null) {
        assertThat(emp.placementCount()).isGreaterThanOrEqualTo(0L);
      }
      if (emp.joinedCount() != null) {
        assertThat(emp.joinedCount()).isGreaterThanOrEqualTo(0L);
      }
      if (emp.joinRatePct() != null) {
        assertThat(emp.joinRatePct())
            .isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
      }
      if (emp.avgJoiningSalary() != null) {
        assertThat(emp.avgJoiningSalary()).isGreaterThan(BigDecimal.ZERO);
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q08 employment mix query against live SIH26135_TEST")
  void testLiveFormalQ08Execution() {
    EmploymentMixProjection mix =
        economicOutcomeDashboardRepository.getFormalQ08EmploymentMix();

    assertThat(mix).isNotNull();
    if (mix.employmentSpellCount() != null) {
      assertThat(mix.employmentSpellCount()).isGreaterThanOrEqualTo(0L);
    }
    if (mix.avgDurationDays() != null) {
      assertThat(mix.avgDurationDays()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q09 engagement category breakdown query against live SIH26135_TEST")
  void testLiveFormalQ09Execution() {
    List<EmploymentCategoryBreakdownProjection> categories =
        economicOutcomeDashboardRepository.getFormalQ09CategoryBreakdown();

    assertThat(categories).isNotNull();
    for (EmploymentCategoryBreakdownProjection cat : categories) {
      assertThat(cat.engagementTypeCode()).isNotBlank();
      assertThat(cat.engagementTypeName()).isNotBlank();
      if (cat.spellCount() != null) {
        assertThat(cat.spellCount()).isGreaterThanOrEqualTo(0L);
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q10 wage progression query against live SIH26135_TEST")
  void testLiveFormalQ10Execution() {
    WageProgressionProjection wage =
        economicOutcomeDashboardRepository.getFormalQ10WageProgression();

    assertThat(wage).isNotNull();
    if (wage.employmentSpellCount() != null) {
      assertThat(wage.employmentSpellCount()).isGreaterThanOrEqualTo(0L);
    }
    if (wage.firstSalaryCount() != null) {
      assertThat(wage.firstSalaryCount()).isGreaterThanOrEqualTo(0L);
      if (wage.employmentSpellCount() != null) {
        assertThat(wage.employmentSpellCount()).isGreaterThanOrEqualTo(wage.firstSalaryCount());
      }
    }
    if (wage.latestSalaryCount() != null) {
      assertThat(wage.latestSalaryCount()).isGreaterThanOrEqualTo(0L);
      if (wage.employmentSpellCount() != null) {
        assertThat(wage.employmentSpellCount()).isGreaterThanOrEqualTo(wage.latestSalaryCount());
      }
    }
    if (wage.avgFirstRecordedSalary() != null) {
      assertThat(wage.avgFirstRecordedSalary()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
    if (wage.avgLatestRecordedSalary() != null) {
      assertThat(wage.avgLatestRecordedSalary()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
  }

  @Test
  @DisplayName("Repository: execute salary distribution bracket counts query against live SIH26135_TEST")
  void testLiveSalaryDistributionCounts() {
    List<SalaryBracketCountProjection> counts =
        economicOutcomeDashboardRepository.getSalaryDistributionCounts();

    assertThat(counts).isNotNull();
    for (SalaryBracketCountProjection item : counts) {
      assertThat(item.bracketCode()).isIn(
          "BELOW_15K",
          "FROM_15K_TO_25K",
          "FROM_25K_TO_50K",
          "ABOVE_50K"
      );
      if (item.count() != null) {
        assertThat(item.count()).isGreaterThanOrEqualTo(0L);
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q11 course wage progression query against live SIH26135_TEST")
  void testLiveFormalQ11Execution() {
    List<CourseWageProgressionProjection> courses =
        economicOutcomeDashboardRepository.getFormalQ11CourseWageProgression();

    assertThat(courses).isNotNull();
    for (CourseWageProgressionProjection course : courses) {
      assertThat(course.courseId()).isNotNull();
      assertThat(course.courseCode()).isNotBlank();
      if (course.spellCount() != null) {
        assertThat(course.spellCount()).isGreaterThanOrEqualTo(0L);
      }
    }
  }

  @Test
  @DisplayName("REST: GET /api/v1/dashboards/employers returns HTTP 200 with valid leaderboard against SIH26135_TEST")
  void testLiveEmployersEndpointHttp200() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/dashboards/employers")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/employers"))
        .andExpect(jsonPath("$.data.rankingMetric").value("joined_count DESC, placement_count DESC"))
        .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(root.path("data").path("items").isArray()).isTrue();
  }

  @Test
  @DisplayName("REST: GET /api/v1/dashboards/economic-outcomes returns HTTP 200 with consolidated outcomes against SIH26135_TEST")
  void testLiveEconomicOutcomesEndpointHttp200() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/economic-outcomes"))
        .andExpect(jsonPath("$.data.wageProgression").isMap())
        .andExpect(jsonPath("$.data.wageProgression.employmentSpellCount").isNumber())
        .andExpect(jsonPath("$.data.wageProgression.employmentWithSalaryRowCount").doesNotExist())
        .andExpect(jsonPath("$.data.salaryDistribution").isArray())
        .andExpect(jsonPath("$.data.salaryDistribution.length()").value(4))
        .andExpect(jsonPath("$.data.employmentMix").isMap())
        .andExpect(jsonPath("$.data.categoryBreakdown").isArray())
        .andExpect(jsonPath("$.data.courseWageProgression").isArray())
        .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode dist = root.path("data").path("salaryDistribution");
    assertThat(dist.get(0).path("bracketCode").asText()).isEqualTo("BELOW_15K");
    assertThat(dist.get(1).path("bracketCode").asText()).isEqualTo("FROM_15K_TO_25K");
    assertThat(dist.get(2).path("bracketCode").asText()).isEqualTo("FROM_25K_TO_50K");
    assertThat(dist.get(3).path("bracketCode").asText()).isEqualTo("ABOVE_50K");
  }

  @AfterEach
  void tearDownSecurityContext() {
    SecurityContextHolder.clearContext();
  }
}