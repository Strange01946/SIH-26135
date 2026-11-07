package in.gov.sih.sih26135.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 27.16 — Analytics & Dashboard Comprehensive Regression Testing Suite.
 *
 * <p>Requirements:
 * 1. Dedicated test catalog strictly {@code SIH26135_TEST} (never production {@code SIH26135}).
 * 2. Automated verification covering all Phase 27 endpoints, calculations, tenancy rules, and regressions:
 *    - Section 1: Live integration tests for all 22 Phase 18 dashboard query endpoints (Q01-Q22).
 *    - Section 2: Multi-tenant security tests verifying providers and employers cannot access cross-tenant metrics.
 *    - Section 3: Boundary and validation tests for all filter parameters and pagination limits.
 *    - Section 4: Statistical disclosure control (k=5 anonymity) and calculation invariant tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 27.16 — Analytics & Dashboard Comprehensive Regression Testing Suite (SIH26135_TEST)")
public class AnalyticsAndDashboardComprehensiveRegressionTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AnalyticsPrivacySuppressionService privacySuppressionService;

  // Test Fixture Identifiers
  private static final Long FIXTURE_STATE_A_ID = 98881L;
  private static final Long FIXTURE_STATE_B_ID = 98882L;
  private static final Long FIXTURE_DISTRICT_A_ID = 98881L; // in State A
  private static final Long FIXTURE_DISTRICT_B_ID = 98882L; // in State B

  private static final Long USER_SUPER_ADMIN_ID = 98881L;
  private static final Long USER_NATIONAL_ADMIN_ID = 98882L;
  private static final Long USER_STATE_A_ADMIN_ID = 98883L;
  private static final Long USER_STATE_B_ADMIN_ID = 98884L;
  private static final Long USER_PROVIDER_A_ID = 98885L;
  private static final Long USER_PROVIDER_B_ID = 98886L;
  private static final Long USER_EMPLOYER_A_ID = 98887L;
  private static final Long USER_EMPLOYER_B_ID = 98888L;
  private static final Long USER_TRAINEE_A_ID = 98889L;
  private static final Long USER_PERM_ONLY_ID = 98890L;

  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAdminA;
  private String tokenStateAdminB;
  private String tokenProviderA;
  private String tokenProviderB;
  private String tokenEmployerA;
  private String tokenEmployerB;
  private String tokenTraineeA;
  private String tokenPermissionOnly;

  @BeforeEach
  void setUp() throws Exception {
    // 1. Mandatory Catalog Isolation Guard
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      assertThat(conn.getCatalog()).isNotEqualToIgnoringCase(MysqlConnectionTest.FORBIDDEN_PRODUCTION_CATALOG);
    }

    cleanUpFixtures();

    // 2. Seed Geographic Fixtures (States & Districts)
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'XA', 'Phase 27.16 State A', 'LGD98881', 1, NOW(), NOW())
        """, FIXTURE_STATE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'XB', 'Phase 27.16 State B', 'LGD98882', 1, NOW(), NOW())
        """, FIXTURE_STATE_B_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DA', 'Phase 27.16 District A', 'LGD98881', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_A_ID, FIXTURE_STATE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DB', 'Phase 27.16 District B', 'LGD98882', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_B_ID, FIXTURE_STATE_B_ID);

    // 3. Seed Organizations (Training Providers & Employers)
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-16A', 'Provider 16A', 'REG-16A', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_PROVIDER_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-16B', 'Provider 16B', 'REG-16B', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_PROVIDER_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-16A', 'Employer 16A', 'REG-EMP-16A', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_EMPLOYER_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-16B', 'Employer 16B', 'REG-EMP-16B', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_EMPLOYER_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    // 4. Seed User Personas
    // Super Administrator
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'reg2716_superadmin', 'reg2716_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_SUPER_ADMIN_ID);

    // National Administrator
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'reg2716_nationaladmin', 'reg2716_nationaladmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_NATIONAL_ADMIN_ID);

    // State A Administrator
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'reg2716_state_a_admin', 'reg2716_state_a_admin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_A_ADMIN_ID, FIXTURE_STATE_A_ID);

    // State B Administrator
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'reg2716_state_b_admin', 'reg2716_state_b_admin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_B_ADMIN_ID, FIXTURE_STATE_B_ID);

    // Training Provider A User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'reg2716_tp_a_user', 'reg2716_tp_a_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_A_ID, USER_PROVIDER_A_ID);

    // Training Provider B User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'reg2716_tp_b_user', 'reg2716_tp_b_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_B_ID, USER_PROVIDER_B_ID);

    // Employer A User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'reg2716_emp_a_user', 'reg2716_emp_a_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_A_ID, USER_EMPLOYER_A_ID);

    // Employer B User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'reg2716_emp_b_user', 'reg2716_emp_b_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_B_ID, USER_EMPLOYER_B_ID);

    // Trainee A User & Record
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'reg2716_trainee_a_user', 'reg2716_trainee_a_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-16-98889', 'Trainee', 'A', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_TRAINEE_A_ID, USER_TRAINEE_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    // Permission-Only User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'reg2716_perm_only', 'reg2716_perm_only@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_PERM_ONLY_ID);

    // 5. Generate Authentic JWT Access Tokens
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "reg2716_superadmin", List.of("ROLE_super_admin", "analytics.read"));
    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_NATIONAL_ADMIN_ID, "reg2716_nationaladmin", List.of("ROLE_government_admin", "analytics.read"));
    tokenStateAdminA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ADMIN_ID, "reg2716_state_a_admin", List.of("ROLE_government_admin", "analytics.read"));
    tokenStateAdminB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ADMIN_ID, "reg2716_state_b_admin", List.of("ROLE_government_admin", "analytics.read"));
    tokenProviderA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A_ID, "reg2716_tp_a_user", List.of("ROLE_training_provider", "batch.read"));
    tokenProviderB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_B_ID, "reg2716_tp_b_user", List.of("ROLE_training_provider", "batch.read"));
    tokenEmployerA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_A_ID, "reg2716_emp_a_user", List.of("ROLE_employer", "placement.read"));
    tokenEmployerB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_B_ID, "reg2716_emp_b_user", List.of("ROLE_employer", "placement.read"));
    tokenTraineeA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_A_ID, "reg2716_trainee_a_user", List.of("ROLE_trainee"));
    tokenPermissionOnly = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PERM_ONLY_ID, "reg2716_perm_only", List.of("analytics.read"));
  }

  @AfterEach
  void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", USER_TRAINEE_A_ID);
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_NATIONAL_ADMIN_ID, USER_STATE_A_ADMIN_ID, USER_STATE_B_ADMIN_ID,
        USER_PROVIDER_A_ID, USER_PROVIDER_B_ID, USER_EMPLOYER_A_ID, USER_EMPLOYER_B_ID,
        USER_TRAINEE_A_ID, USER_PERM_ONLY_ID);
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)",
        USER_PROVIDER_A_ID, USER_PROVIDER_B_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (?, ?)",
        USER_EMPLOYER_A_ID, USER_EMPLOYER_B_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?)",
        FIXTURE_DISTRICT_A_ID, FIXTURE_DISTRICT_B_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)",
        FIXTURE_STATE_A_ID, FIXTURE_STATE_B_ID);
  }

  // =========================================================================
  // Section 1: All 22 Phase 18 Dashboard Query Endpoints Integration Tests
  // =========================================================================
  @Nested
  @DisplayName("Section 1 — All 22 Phase 18 Dashboard Query Endpoints Live Integration Tests")
  class Phase18DashboardEndpointsIntegrationTests {

    @Test
    @DisplayName("Q01: Dashboard KPI Strip (/api/v1/dashboards/kpis) returns 200 OK and valid DTO")
    void testQ01_DashboardKpiStrip() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.path").value("/api/v1/dashboards/kpis"))
          .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("Q02: Training Pipeline Funnel (/api/v1/dashboards/pipeline-funnel) returns 200 OK and valid rates")
    void testQ02_PipelineFunnel() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/pipeline-funnel")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.enrollmentCount").exists());
    }

    @Test
    @DisplayName("Q03: Scheme & Program Comparative Matrix (/api/v1/dashboards/scheme-matrix) returns 200 OK")
    void testQ03_SchemeProgramMatrix() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/scheme-matrix")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("Q04: Training Provider Leaderboard (/api/v1/dashboards/providers) returns 200 OK")
    void testQ04_TrainingProviderLeaderboard() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/providers")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("Q05: Course Outcome Table (/api/v1/dashboards/courses) returns 200 OK")
    void testQ05_CourseOutcomeTable() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/courses")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("Q06: District & State Heatmap Geography (/api/v1/dashboards/geography) returns 200 OK")
    void testQ06_GeographicOverview() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/geography")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.stateSummaries").isArray())
          .andExpect(jsonPath("$.data.districtRankings").exists());
    }

    @Test
    @DisplayName("Q06 (State Drilldown): (/api/v1/dashboards/states/{stateId}) returns 200 OK")
    void testQ06_StateGeographicDrilldown() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/" + FIXTURE_STATE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.stateId").value(FIXTURE_STATE_A_ID))
          .andExpect(jsonPath("$.data.districts").isArray());
    }

    @Test
    @DisplayName("Q06 (District Drilldown): (/api/v1/dashboards/districts/{districtId}) returns 200 OK")
    void testQ06_DistrictGeographicDrilldown() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/" + FIXTURE_DISTRICT_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.districtId").value(FIXTURE_DISTRICT_A_ID));
    }

    @Test
    @DisplayName("Q07: Employer Hiring Dashboard (/api/v1/dashboards/employers) returns 200 OK")
    void testQ07_EmployerHiringLeaderboard() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/employers")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("Q08: Employment Mix (/api/v1/dashboards/economic-outcomes) returns 200 OK")
    void testQ08_EmploymentMix() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.employmentMix").exists())
          .andExpect(jsonPath("$.data.employmentMix.employmentSpellCount").exists());
    }

    @Test
    @DisplayName("Q09: Engagement Type Breakdown (/api/v1/dashboards/economic-outcomes) returns 200 OK")
    void testQ09_EngagementBreakdown() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.categoryBreakdown").isArray());
    }

    @Test
    @DisplayName("Q10: Salary Progression KPI (/api/v1/dashboards/economic-outcomes) returns 200 OK")
    void testQ10_SalaryProgressionKpi() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.wageProgression").exists());
    }

    @Test
    @DisplayName("Q11: Course Salary Progression (/api/v1/dashboards/economic-outcomes) returns 200 OK")
    void testQ11_CourseSalaryProgression() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.courseWageProgression").isArray());
    }

    @Test
    @DisplayName("Q12: Retention Survival Milestones (/api/v1/dashboards/retention) returns 200 OK")
    void testQ12_RetentionMilestones() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/retention")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.milestones").isArray());
    }

    @Test
    @DisplayName("Q13: Top Current Skill Gaps (/api/v1/dashboards/skill-gaps) returns 200 OK")
    void testQ13_TopSkillGaps() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.topSkillGaps").isArray());
    }

    @Test
    @DisplayName("Q14: Skill Gap Severity Mix & Uptake (/api/v1/dashboards/skill-gaps) returns 200 OK")
    void testQ14_SeverityMixAndUptake() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.severityBreakdown").isArray());
    }

    @Test
    @DisplayName("Q15: Unemployment Period KPI (/api/v1/dashboards/attrition) returns 200 OK")
    void testQ15_UnemploymentPeriodKpi() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/attrition")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.unemploymentSummary").exists());
    }

    @Test
    @DisplayName("Q16: Unemployment Reasons (/api/v1/dashboards/attrition) returns 200 OK")
    void testQ16_UnemploymentReasons() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/attrition")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.unemploymentReasons").isArray());
    }

    @Test
    @DisplayName("Q17: Exit Reasons & Separation Mix (/api/v1/dashboards/attrition) returns 200 OK")
    void testQ17_ExitReasons() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/attrition")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.exitReasons").isArray());
    }

    @Test
    @DisplayName("Q18: Voluntary vs Involuntary Totals (/api/v1/dashboards/attrition) returns 200 OK")
    void testQ18_SeparationTotals() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/attrition")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.separationAnalysis").exists());
    }

    @Test
    @DisplayName("Q19: Follow-up Outreach Success by Wave (/api/v1/dashboards/followups) returns 200 OK")
    void testQ19_FollowupSuccessByWave() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/followups")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.wavePerformance").isArray())
          .andExpect(jsonPath("$.data.slaPerformance").exists());
    }

    @Test
    @DisplayName("Q20: Survey Response Pipeline (/api/v1/dashboards/surveys) returns 200 OK")
    void testQ20_SurveyResponsePipeline() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/surveys")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.purposePipeline").isArray())
          .andExpect(jsonPath("$.data.satisfactionPerformance").exists());
    }

    @Test
    @DisplayName("Q21: Employment Verification Outcomes (/api/v1/dashboards/verifications) returns 200 OK")
    void testQ21_VerificationOutcomes() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/verifications")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.statusBreakdown").isArray());
    }

    @Test
    @DisplayName("Q22: Verification Method Efficacy Breakdown (/api/v1/dashboards/verifications) returns 200 OK")
    void testQ22_MethodEfficacyBreakdown() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/verifications")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.methodEfficacyBreakdown").isArray());
    }

    @Test
    @DisplayName("Semantic Alias: /overview returns consolidated Q01, Q02, Q03 with 200 OK")
    void testConsolidatedOverviewEndpoint() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.kpiStrip").exists())
          .andExpect(jsonPath("$.data.pipelineFunnel").exists())
          .andExpect(jsonPath("$.data.schemeProgramMatrix").exists());
    }

    @Test
    @DisplayName("Semantic Alias: /national returns consolidated Q01, Q02, Q03 with 200 OK")
    void testConsolidatedNationalEndpoint() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/national")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.kpiStrip").exists())
          .andExpect(jsonPath("$.data.pipelineFunnel").exists())
          .andExpect(jsonPath("$.data.schemeProgramMatrix").exists());
    }
  }

  // =========================================================================
  // Section 2: Multi-Tenant Security & Tenancy Isolation Tests
  // =========================================================================
  @Nested
  @DisplayName("Section 2 — Multi-Tenant Security & Cross-Tenant Isolation Tests")
  class MultiTenantSecurityTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/courses",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/followups",
        "/api/v1/dashboards/surveys"
    })
    @DisplayName("Training Provider is strictly forbidden (HTTP 403) from all national administrative dashboards")
    void testTrainingProvider_forbiddenFromAdministrativeDashboards(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/courses",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/followups",
        "/api/v1/dashboards/surveys"
    })
    @DisplayName("Employer is strictly forbidden (HTTP 403) from all national administrative dashboards")
    void testEmployer_forbiddenFromAdministrativeDashboards(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Training Provider A cannot access Training Provider B outcome summary (HTTP 403)")
    void testTrainingProvider_cannotAccessCrossTenantProviderMetrics() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/" + USER_PROVIDER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Training Provider A is forbidden from requesting nationwide provider leaderboard (HTTP 403)")
    void testTrainingProvider_cannotAccessProviderLeaderboard() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/leaderboard")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Training Provider A is forbidden from requesting employer hiring metrics (HTTP 403)")
    void testTrainingProvider_cannotAccessEmployerMetrics() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/" + USER_EMPLOYER_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employer A cannot access Employer B hiring summary (HTTP 403)")
    void testEmployer_cannotAccessCrossTenantEmployerMetrics() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/" + USER_EMPLOYER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employer A is forbidden from requesting nationwide employer leaderboard (HTTP 403)")
    void testEmployer_cannotAccessEmployerLeaderboard() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/leaderboard")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employer A is forbidden from requesting provider outcome metrics (HTTP 403)")
    void testEmployer_cannotAccessProviderMetrics() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/" + USER_PROVIDER_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Trainee A cannot access Trainee B outcome summary (HTTP 403)")
    void testTrainee_cannotAccessCrossTenantTraineeMetrics() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes/999999")
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State Administrator A is strictly forbidden from accessing State B dashboard (HTTP 403)")
    void testStateAdminA_forbiddenFromStateBDashboard() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/" + FIXTURE_STATE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdminA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State Administrator A is strictly forbidden from accessing District B in State B (HTTP 403)")
    void testStateAdminA_forbiddenFromDistrictBInStateB() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/" + FIXTURE_DISTRICT_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdminA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State Administrator A is authorized for assigned State A and District A")
    void testStateAdminA_authorizedForAssignedStateAndDistrict() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/" + FIXTURE_STATE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdminA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/dashboards/districts/" + FIXTURE_DISTRICT_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdminA)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Caller with analytics.read but lacking admin role fails closed with HTTP 403")
    void testPermissionOnlyCaller_failsClosed() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .header(HttpHeaders.AUTHORIZATION, tokenPermissionOnly)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated caller without JWT token fails with HTTP 401 Unauthorized")
    void testUnauthenticatedCaller_failsWith401() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }
  }

  // =========================================================================
  // Section 3: Boundary & Validation Tests for Filter Parameters & Pagination Limits
  // =========================================================================
  @Nested
  @DisplayName("Section 3 — Boundary & Validation Tests for Filter Parameters and Pagination Limits")
  class FilterAndPaginationBoundaryValidationTests {

    @Test
    @DisplayName("Temporal boundary: startDate after endDate is rejected with HTTP 400 Bad Request")
    void testTemporalBoundary_startDateAfterEndDate_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("startDate", "2026-06-01")
              .param("endDate", "2026-01-01")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Temporal boundary: conflicting temporal filters (fiscalYear and calendarYear) rejected with HTTP 400")
    void testTemporalBoundary_conflictingFiscalAndCalendarYear_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("fiscalYear", "FY2024-25")
              .param("calendarYear", "2025")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Temporal boundary: invalid fiscal year format is rejected with HTTP 400")
    void testTemporalBoundary_invalidFiscalYearFormat_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("fiscalYear", "2024-2025") // Invalid format, expected FYyyyy-yy
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("ID boundary: negative IDs (stateId, districtId, courseId) are rejected with HTTP 400")
    void testIdBoundary_negativeValues_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("stateId", "-1")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("districtId", "-5")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("courseId", "-99")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demographic boundary: unsupported schema dimensions (socialCategory, minority, pwd) rejected with HTTP 400")
    void testDemographicBoundary_unsupportedDimensions_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("socialCategory", "OBC")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not supported by the existing database schema")));

      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("minority", "YES")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not supported by the existing database schema")));

      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("pwd", "YES")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not supported by the existing database schema")));
    }

    @Test
    @DisplayName("Demographic boundary: invalid gender string is rejected with HTTP 400")
    void testDemographicBoundary_invalidGender_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("gender", "UNKNOWN_GENDER")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Valid multi-dimensional filters succeed with HTTP 200 OK")
    void testValidMultiDimensionalFilters_succeed() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/kpis")
              .param("fiscalYear", "FY2024-25")
              .param("stateId", FIXTURE_STATE_A_ID.toString())
              .param("districtId", FIXTURE_DISTRICT_A_ID.toString())
              .param("gender", "FEMALE")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Pagination boundary: negative page (page=-1) rejected with HTTP 400")
    void testPaginationBoundary_negativePage_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("page", "-1")
              .param("size", "20")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Pagination boundary: page size zero (size=0) rejected with HTTP 400")
    void testPaginationBoundary_zeroSize_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("page", "0")
              .param("size", "0")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Pagination boundary: negative page size (size=-10) rejected with HTTP 400")
    void testPaginationBoundary_negativeSize_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("page", "0")
              .param("size", "-10")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Pagination boundary: page size exceeding maximum (size=101) rejected with HTTP 400")
    void testPaginationBoundary_exceedingMaxSize_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("page", "0")
              .param("size", "101")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Pagination boundary: default parameters enforce page=0 and size=20")
    void testPaginationBoundary_defaultParameters_enforced() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.page").value(0))
          .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("Pagination boundary: valid maximum page size (size=100) accepted with HTTP 200 OK")
    void testPaginationBoundary_validMaxSize_accepted() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("page", "0")
              .param("size", "100")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("Sort boundary: unwhitelisted sort parameter rejected with HTTP 400")
    void testSortBoundary_unwhitelistedSortProperty_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("sort", "unapprovedColumnName")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sort boundary: SQL injection attempt in sort parameter rejected with HTTP 400")
    void testSortBoundary_sqlInjectionAttempt_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("sort", "traineeId;DROP TABLE users;--")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sort boundary: invalid direction parameter rejected with HTTP 400")
    void testSortBoundary_invalidDirection_rejected() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/enrollment-outcomes")
              .param("direction", "SIDEWAYS")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }
  }

  // =========================================================================
  // Section 4: Statistical Disclosure Control & Calculation Invariant Tests
  // =========================================================================
  @Nested
  @DisplayName("Section 4 — Statistical Disclosure Control (k=5) & Calculation Invariant Tests")
  class StatisticalDisclosureAndCalculationInvariantTests {

    @Test
    @DisplayName("k=5 Anonymity: 0 count is safely preserved without suppression")
    void testKAnonymity_zeroCountPreserved() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          1L, "D01", "District 1", 1L, "S01", "State 1",
          0L, 0L, 0L, 0L, 0L, 0L,
          BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
          1
      );
      DistrictPerformanceResponse suppressed = privacySuppressionService.applyPrivacyGuardrail(input);

      assertThat(suppressed).isNotNull();
      assertThat(suppressed.traineeCount()).isEqualTo(0L);
      assertThat(suppressed.enrollmentCount()).isEqualTo(0L);
      assertThat(suppressed.completedCount()).isEqualTo(0L);
      assertThat(suppressed.employedCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("k=5 Anonymity: sparse cohort (1 <= traineeCount < 5) has sensitive outcomes suppressed")
    void testKAnonymity_sparseCohortSuppressed() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          1L, "D01", "District 1", 1L, "S01", "State 1",
          3L, 3L, 2L, 2L, 1L, 1L,
          new BigDecimal("66.67"), new BigDecimal("66.67"), new BigDecimal("33.33"), new BigDecimal("33.33"),
          1
      );
      DistrictPerformanceResponse suppressed = privacySuppressionService.applyPrivacyGuardrail(input);

      assertThat(suppressed).isNotNull();
      assertThat(suppressed.traineeCount()).isNull();
      assertThat(suppressed.enrollmentCount()).isNull();
      assertThat(suppressed.completedCount()).isNull();
      assertThat(suppressed.employedCount()).isNull();
      assertThat(suppressed.completionRatePct()).isNull();
      assertThat(suppressed.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("k=5 Anonymity: authorized cohort (traineeCount >= 5) releases calculated metrics")
    void testKAnonymity_authorizedCohortReleased() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          1L, "D01", "District 1", 1L, "S01", "State 1",
          25L, 20L, 18L, 16L, 14L, 12L,
          new BigDecimal("72.00"), new BigDecimal("64.00"), new BigDecimal("56.00"), new BigDecimal("48.00"),
          1
      );
      DistrictPerformanceResponse suppressed = privacySuppressionService.applyPrivacyGuardrail(input);

      assertThat(suppressed).isNotNull();
      assertThat(suppressed.traineeCount()).isEqualTo(20L);
      assertThat(suppressed.enrollmentCount()).isEqualTo(25L);
      assertThat(suppressed.completedCount()).isEqualTo(18L);
      assertThat(suppressed.employedCount()).isEqualTo(12L);
      assertThat(suppressed.completionRatePct()).isEqualByComparingTo("72.00");
      assertThat(suppressed.employmentRatePct()).isEqualByComparingTo("48.00");
    }

    @Test
    @DisplayName("k=5 Independent Cell Suppression: sensitive outcomes < 5 suppressed even when parent >= 5")
    void testKAnonymity_independentCellSuppression() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          1L, "D01", "District 1", 1L, "S01", "State 1",
          12L, 10L, 4L, 4L, 2L, 2L, // completedCount=4, employedCount=2 (< 5)
          new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("16.67"), new BigDecimal("16.67"),
          1
      );
      DistrictPerformanceResponse suppressed = privacySuppressionService.applyPrivacyGuardrail(input);

      assertThat(suppressed).isNotNull();
      assertThat(suppressed.traineeCount()).isEqualTo(10L); // Parent meets k >= 5
      assertThat(suppressed.enrollmentCount()).isEqualTo(12L);
      // Sensitive person-level outcome cells < 5 must be suppressed
      assertThat(suppressed.completedCount()).isNull();
      assertThat(suppressed.employedCount()).isNull();
      assertThat(suppressed.completionRatePct()).isNull();
      assertThat(suppressed.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("Calculation Invariant: division by zero returns null rate safely")
    void testCalculationUtils_divisionByZero_safeNull() {
      BigDecimal rate = AnalyticsCalculationUtils.calculatePercentage(5L, 0L);
      assertThat(rate).isNull();

      BigDecimal rateNullDenom = AnalyticsCalculationUtils.calculatePercentage(5L, null);
      assertThat(rateNullDenom).isNull();

      BigDecimal rateNullNum = AnalyticsCalculationUtils.calculatePercentage(null, 10L);
      assertThat(rateNullNum).isNull();
    }

    @Test
    @DisplayName("Calculation Invariant: percentage rounding with 2 decimal places")
    void testCalculationUtils_percentageRounding() {
      BigDecimal rate = AnalyticsCalculationUtils.calculatePercentage(1L, 3L);
      assertThat(rate).isEqualByComparingTo("33.33");

      BigDecimal rate2 = AnalyticsCalculationUtils.calculatePercentage(2L, 3L);
      assertThat(rate2).isEqualByComparingTo("66.67");
    }
  }
}
