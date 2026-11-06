package in.gov.sih.sih26135.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.entity.analytics.EmploymentExitFact;
import in.gov.sih.sih26135.entity.analytics.EmploymentFact;
import in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact;
import in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact;
import in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact;
import in.gov.sih.sih26135.entity.analytics.FollowupFact;
import in.gov.sih.sih26135.entity.analytics.PlacementFact;
import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import in.gov.sih.sih26135.entity.analytics.SkillGapFact;
import in.gov.sih.sih26135.entity.analytics.SurveyResponseFact;
import in.gov.sih.sih26135.entity.analytics.UnemploymentFact;
import in.gov.sih.sih26135.repository.analytics.EmploymentExitFactRepository;
import in.gov.sih.sih26135.repository.analytics.EmploymentFactRepository;
import in.gov.sih.sih26135.repository.analytics.EmploymentRetentionFactRepository;
import in.gov.sih.sih26135.repository.analytics.EmploymentVerificationFactRepository;
import in.gov.sih.sih26135.repository.analytics.EnrollmentOutcomeFactRepository;
import in.gov.sih.sih26135.repository.analytics.FollowupFactRepository;
import in.gov.sih.sih26135.repository.analytics.PlacementFactRepository;
import in.gov.sih.sih26135.repository.analytics.SalaryProgressionFactRepository;
import in.gov.sih.sih26135.repository.analytics.SkillGapFactRepository;
import in.gov.sih.sih26135.repository.analytics.SurveyResponseFactRepository;
import in.gov.sih.sih26135.repository.analytics.UnemploymentFactRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 27.12 — Analytics Fact Endpoints Pagination & Volume Guardrails Integration Test.
 *
 * <p>Validates:
 * 1. All 11 analytical fact collection endpoints return bounded PagedResponse envelopes.
 * 2. Strict pagination bounds enforcement (page >= 0, 1 <= size <= 100).
 * 3. Deterministic sorting and allowlists per fact entity.
 * 4. Rejection of unapproved sort parameters and unsupported query parameters.
 * 5. Role-based authorization & tenancy boundaries via WorkflowSecurityScopeValidator.
 * 6. Live database query bounds and LIMIT / OFFSET execution against SIH26135_TEST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 27.12 — Analytics Fact Endpoints Pagination & Volume Guardrails Test Suite")
public class AnalyticsFactPaginationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  // Fact Repositories for live database integration
  @Autowired
  private EmploymentExitFactRepository employmentExitFactRepository;

  @Autowired
  private EmploymentFactRepository employmentFactRepository;

  @Autowired
  private EmploymentRetentionFactRepository employmentRetentionFactRepository;

  @Autowired
  private EmploymentVerificationFactRepository employmentVerificationFactRepository;

  @Autowired
  private EnrollmentOutcomeFactRepository enrollmentOutcomeFactRepository;

  @Autowired
  private FollowupFactRepository followupFactRepository;

  @Autowired
  private PlacementFactRepository placementFactRepository;

  @Autowired
  private SalaryProgressionFactRepository salaryProgressionFactRepository;

  @Autowired
  private SkillGapFactRepository skillGapFactRepository;

  @Autowired
  private SurveyResponseFactRepository surveyResponseFactRepository;

  @Autowired
  private UnemploymentFactRepository unemploymentFactRepository;

  // Test Fixture Identifiers
  private static final Long FIXTURE_STATE_ID = 99981L;
  private static final Long FIXTURE_DISTRICT_ID = 99981L;

  private static final Long USER_SUPER_ADMIN_ID = 99981L;
  private static final Long USER_NATIONAL_ADMIN_ID = 99982L;
  private static final Long USER_STATE_ADMIN_ID = 99983L;
  private static final Long USER_PROVIDER_ID = 99985L;
  private static final Long USER_EMPLOYER_ID = 99986L;
  private static final Long USER_TRAINEE_ID = 99987L;
  private static final Long USER_PERM_ONLY_ID = 99989L;

  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAdmin;
  private String tokenProvider;
  private String tokenEmployer;
  private String tokenTrainee;
  private String tokenPermissionOnly;

  private static final String[] ALL_FACT_ENDPOINTS = {
      "/api/v1/analytics/facts/employment-exits",
      "/api/v1/analytics/facts/employments",
      "/api/v1/analytics/facts/employment-retentions",
      "/api/v1/analytics/facts/employment-verifications",
      "/api/v1/analytics/facts/enrollment-outcomes",
      "/api/v1/analytics/facts/followups",
      "/api/v1/analytics/facts/placements",
      "/api/v1/analytics/facts/salary-progressions",
      "/api/v1/analytics/facts/skill-gaps",
      "/api/v1/analytics/facts/survey-responses",
      "/api/v1/analytics/facts/unemployments"
  };

  @BeforeEach
  void setUpFixturesAndTokens() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    cleanUpFixtures();

    // 1. Seed Geographic Fixtures
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SX', 'Fixture State 99981', 'LG99981', 1, NOW(), NOW())
        """, FIXTURE_STATE_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DX', 'Fixture District 99981', 'LGD99981', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_ID, FIXTURE_STATE_ID);

    // 2. Seed Organization Parent Fixtures
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-P2712-99985', 'Provider Fixture 99985', 'REG-PRV-99985', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_PROVIDER_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-P2712-99986', 'Employer Fixture 99986', 'REG-EMP-99986', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_EMPLOYER_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    // 3. Seed Users
    // Super Administrator (nationwide, no state/district assignment)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2712_superadmin', 'p2712_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_SUPER_ADMIN_ID);

    // National Administrator (nationwide, no state/district assignment)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2712_natadmin', 'p2712_natadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_NATIONAL_ADMIN_ID);

    // State Administrator (restricted to state 99981)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p2712_state_admin', 'p2712_state_admin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_ADMIN_ID, FIXTURE_STATE_ID);

    // Training Provider User (restricted to provider 99985)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'p2712_tp_user', 'p2712_tp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_ID, USER_PROVIDER_ID);

    // Employer User (restricted to employer 99986)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'p2712_emp_user', 'p2712_emp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_ID, USER_EMPLOYER_ID);

    // Trainee User (user 99987 + trainee entity)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2712_trainee_user', 'p2712_trainee_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_ID);

    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-P2712-99987', 'Test', 'Trainee', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_TRAINEE_ID, USER_TRAINEE_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    // Permission-Only User (holds analytics.read but no admin role)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2712_perm_only', 'p2712_perm_only@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_PERM_ONLY_ID);

    // 4. Issue Authentic JWT Access Tokens
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "p2712_superadmin", List.of("ROLE_super_admin", "analytics.read"));

    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_NATIONAL_ADMIN_ID, "p2712_natadmin", List.of("ROLE_government_admin", "analytics.read"));

    tokenStateAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_ADMIN_ID, "p2712_state_admin", List.of("ROLE_government_admin", "analytics.read"));

    tokenProvider = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_ID, "p2712_tp_user", List.of("ROLE_training_provider", "analytics.read"));

    tokenEmployer = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_ID, "p2712_emp_user", List.of("ROLE_employer", "analytics.read"));

    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_ID, "p2712_trainee_user", List.of("ROLE_trainee", "analytics.read"));

    tokenPermissionOnly = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PERM_ONLY_ID, "p2712_perm_only", List.of("analytics.read"));
  }

  @AfterEach
  void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", USER_TRAINEE_ID);
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_NATIONAL_ADMIN_ID, USER_STATE_ADMIN_ID,
        USER_PROVIDER_ID, USER_EMPLOYER_ID, USER_TRAINEE_ID, USER_PERM_ONLY_ID);
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", USER_PROVIDER_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", USER_EMPLOYER_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", FIXTURE_DISTRICT_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", FIXTURE_STATE_ID);
  }

  // =========================================================================
  // 1. All 11 Endpoints Return Default PagedResponse Envelopes (200 OK)
  // =========================================================================

  @Nested
  @DisplayName("1. Default Pagination Contract Tests (All 11 Endpoints)")
  class DefaultPaginationContractTests {

    @ParameterizedTest(name = "Endpoint {0} returns default PagedResponse envelope")
    @ValueSource(strings = {
        "/api/v1/analytics/facts/employment-exits",
        "/api/v1/analytics/facts/employments",
        "/api/v1/analytics/facts/employment-retentions",
        "/api/v1/analytics/facts/employment-verifications",
        "/api/v1/analytics/facts/enrollment-outcomes",
        "/api/v1/analytics/facts/followups",
        "/api/v1/analytics/facts/placements",
        "/api/v1/analytics/facts/salary-progressions",
        "/api/v1/analytics/facts/skill-gaps",
        "/api/v1/analytics/facts/survey-responses",
        "/api/v1/analytics/facts/unemployments"
    })
    void testDefaultPaginationContract(String endpoint) throws Exception {
      mockMvc.perform(get(endpoint)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)))
          .andExpect(jsonPath("$.data.page", equalTo(0)))
          .andExpect(jsonPath("$.data.size", equalTo(20)))
          .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(0)))
          .andExpect(jsonPath("$.data.totalPages", greaterThanOrEqualTo(0)))
          .andExpect(jsonPath("$.data.first", equalTo(true)));
    }
  }

  // =========================================================================
  // 2. Pagination Bounds Validation (Valid Ranges & Boundaries)
  // =========================================================================

  @Nested
  @DisplayName("2. Pagination Bounds & Sizing Tests")
  class PaginationBoundsTests {

    @Test
    @DisplayName("Accepts minimum page size 1")
    void testMinimumPageSize() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "0")
              .param("size", "1")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)))
          .andExpect(jsonPath("$.data.page", equalTo(0)))
          .andExpect(jsonPath("$.data.size", equalTo(1)));
    }

    @Test
    @DisplayName("Accepts maximum page size 100")
    void testMaximumPageSize() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "0")
              .param("size", "100")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)))
          .andExpect(jsonPath("$.data.page", equalTo(0)))
          .andExpect(jsonPath("$.data.size", equalTo(100)));
    }

    @Test
    @DisplayName("Rejects negative page index with HTTP 400 INVALID_PAGE")
    void testNegativePageIndexRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "-1")
              .param("size", "20")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_PAGE")));
    }

    @Test
    @DisplayName("Rejects page size 0 with HTTP 400 INVALID_PAGE_SIZE")
    void testZeroPageSizeRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "0")
              .param("size", "0")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_PAGE_SIZE")));
    }

    @Test
    @DisplayName("Rejects page size greater than 100 with HTTP 400 INVALID_PAGE_SIZE")
    void testOversizedPageSizeRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "0")
              .param("size", "101")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_PAGE_SIZE")));
    }

    @Test
    @DisplayName("Rejects negative page size with HTTP 400 INVALID_PAGE_SIZE")
    void testNegativePageSizeRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("page", "0")
              .param("size", "-10")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_PAGE_SIZE")));
    }
  }

  // =========================================================================
  // 3. Sorting & Allowlists Validation
  // =========================================================================

  @Nested
  @DisplayName("3. Sorting Allowlists & Parameter Validation Tests")
  class SortingValidationTests {

    @Test
    @DisplayName("Accepts valid sort=id with default ASC direction")
    void testValidSortDefaultDirection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("sort", "id")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)));
    }

    @Test
    @DisplayName("Accepts valid sort=id with explicit DESC direction")
    void testValidSortDescDirection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("sort", "id")
              .param("direction", "DESC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)));
    }

    @Test
    @DisplayName("Rejects unapproved sort field with HTTP 400 INVALID_SORT_FIELD")
    void testDisallowedSortFieldRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("sort", "nonexistentColumn")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_SORT_FIELD")));
    }

    @Test
    @DisplayName("Rejects sensitive sort property with HTTP 400 INVALID_SORT_FIELD")
    void testSensitiveSortFieldRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("sort", "password")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_SORT_FIELD")));
    }

    @Test
    @DisplayName("Rejects invalid direction with HTTP 400 INVALID_SORT_DIRECTION")
    void testInvalidSortDirectionRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("sort", "id")
              .param("direction", "SIDEWAYS")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("INVALID_SORT_DIRECTION")));
    }

    @Test
    @DisplayName("Rejects unsupported query parameters with HTTP 400 UNSUPPORTED_PARAMETER")
    void testUnsupportedQueryParamRejection() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .param("limit", "50")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status", equalTo(400)))
          .andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_PARAMETER")));
    }
  }

  // =========================================================================
  // 4. Role-Based Tenancy & Security Scope Hardening Tests
  // =========================================================================

  @Nested
  @DisplayName("4. Tenancy & Persona Authorization Boundary Tests")
  class TenancyAndAuthorizationTests {

    @Test
    @DisplayName("Unauthenticated caller rejected with HTTP 401 Unauthorized")
    void testUnauthenticatedCallFails() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("State Administrator rejected with HTTP 403 Forbidden (nationwide scope required)")
    void testStateAdminForbidden() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success", equalTo(false)));
    }

    @Test
    @DisplayName("Training Provider rejected with HTTP 403 Forbidden (nationwide scope required)")
    void testTrainingProviderForbidden() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenProvider)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success", equalTo(false)));
    }

    @Test
    @DisplayName("Employer rejected with HTTP 403 Forbidden (nationwide scope required)")
    void testEmployerForbidden() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success", equalTo(false)));
    }

    @Test
    @DisplayName("Trainee rejected with HTTP 403 Forbidden (nationwide scope required)")
    void testTraineeForbidden() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success", equalTo(false)));
    }

    @Test
    @DisplayName("Permission-only user rejected with HTTP 403 Forbidden (lacks national admin role)")
    void testPermissionOnlyForbidden() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenPermissionOnly)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success", equalTo(false)));
    }

    @Test
    @DisplayName("National Administrator permitted with HTTP 200 OK")
    void testNationalAdminPermitted() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)));
    }

    @Test
    @DisplayName("Super Administrator permitted with HTTP 200 OK")
    void testSuperAdminPermitted() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/facts/employments")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", equalTo(true)));
    }
  }

  // =========================================================================
  // 5. Live Database Repository Pagination & LIMIT/OFFSET Verification
  // =========================================================================

  @Nested
  @DisplayName("5. Live Database Query Bounding & Pagination Tests (SIH26135_TEST)")
  class LiveDatabaseRepositoryPaginationTests {

    @Test
    @DisplayName("Database catalog is verified as SIH26135_TEST")
    void testCatalogIsolation() throws Exception {
      try (Connection conn = dataSource.getConnection()) {
        MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
        assertThat(conn.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      }
    }

    @Test
    @DisplayName("EmploymentExitFactRepository executes bounded pagination query")
    void testEmploymentExitFactPagination() {
      Page<EmploymentExitFact> page = employmentExitFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "employmentExitEventId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("EmploymentFactRepository executes bounded pagination query")
    void testEmploymentFactPagination() {
      Page<EmploymentFact> page = employmentFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "employmentId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("EmploymentRetentionFactRepository executes bounded pagination query")
    void testEmploymentRetentionFactPagination() {
      Page<EmploymentRetentionFact> page = employmentRetentionFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "employmentId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("EmploymentVerificationFactRepository executes bounded pagination query")
    void testEmploymentVerificationFactPagination() {
      Page<EmploymentVerificationFact> page = employmentVerificationFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "employmentVerificationId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("EnrollmentOutcomeFactRepository executes bounded pagination query")
    void testEnrollmentOutcomeFactPagination() {
      Page<EnrollmentOutcomeFact> page = enrollmentOutcomeFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "enrollmentId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("FollowupFactRepository executes bounded pagination query")
    void testFollowupFactPagination() {
      Page<FollowupFact> page = followupFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "followupTaskId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("PlacementFactRepository executes bounded pagination query")
    void testPlacementFactPagination() {
      Page<PlacementFact> page = placementFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "placementId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("SalaryProgressionFactRepository executes bounded pagination query")
    void testSalaryProgressionFactPagination() {
      Page<SalaryProgressionFact> page = salaryProgressionFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "employmentId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("SkillGapFactRepository executes bounded pagination query")
    void testSkillGapFactPagination() {
      Page<SkillGapFact> page = skillGapFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "skillGapId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("SurveyResponseFactRepository executes bounded pagination query")
    void testSurveyResponseFactPagination() {
      Page<SurveyResponseFact> page = surveyResponseFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "surveyResponseId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("UnemploymentFactRepository executes bounded pagination query")
    void testUnemploymentFactPagination() {
      Page<UnemploymentFact> page = unemploymentFactRepository.findAll(
          PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "unemploymentEventId")));
      assertThat(page).isNotNull();
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(5);
      assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(5);
    }
  }
}
