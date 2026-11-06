package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.repository.analytics.DataIntegrityDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.ExecutiveDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.GeographicDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.SkillGapDashboardRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataIntegrityDashboardService;
import in.gov.sih.sih26135.service.EconomicOutcomeDashboardService;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import in.gov.sih.sih26135.service.RetentionAttritionDashboardService;
import in.gov.sih.sih26135.service.SkillGapDashboardService;
import in.gov.sih.sih26135.service.impl.DataIntegrityDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.ExecutiveDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.GeographicDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.SkillGapDashboardServiceImpl;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 27.10 — Role-Based Persona Dashboards & Tenancy Scope Hardening Test Suite.
 *
 * <p>Verifies strict server-side tenancy boundaries and persona authorization across all 19
 * dashboard endpoints spanning all 7 Phase 27 dashboard modules:
 * <ol>
 *   <li>Super Administrator: Complete nationwide and state/district access.</li>
 *   <li>National Administrator: Verified nationwide analytical authority without state boundary.</li>
 *   <li>State Administrator (State A): Authorized strictly for assigned State A and constituent District A;
 *       forbidden from State B (HTTP 403), District B (HTTP 403), and all nationwide endpoints (HTTP 403).</li>
 *   <li>Training Provider: Strictly isolated to own operations; forbidden from all administrative dashboards (HTTP 403).</li>
 *   <li>Employer: Strictly isolated to own operations; forbidden from all administrative dashboards (HTTP 403).</li>
 *   <li>Trainee: Strictly isolated to self; forbidden from all administrative dashboards (HTTP 403).</li>
 *   <li>Unauthenticated caller: HTTP 401 Unauthorized across all dashboard endpoints.</li>
 *   <li>Defense-in-Depth: Both Controller boundary and Service boundary independently enforce tenancy.</li>
 *   <li>Zero analytical information disclosure on forbidden access attempts.</li>
 * </ol>
 *
 * Runs strictly against {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 27.10 — Role-Based Persona Dashboards & Tenancy Scope Hardening")
public class DashboardTenancyAndRoleAuthorizationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private WorkflowSecurityScopeValidator workflowSecurityScopeValidator;

  // Mock service responses to decouple controller security assertions from underlying view population
  @MockBean
  private GeographicDashboardService mockGeographicService;

  @MockBean
  private ExecutiveDashboardService mockExecutiveService;

  @MockBean
  private InstitutionalAnalyticsService mockInstitutionalService;

  @MockBean
  private EconomicOutcomeDashboardService mockEconomicService;

  @MockBean
  private RetentionAttritionDashboardService mockRetentionService;

  @MockBean
  private SkillGapDashboardService mockSkillGapService;

  @MockBean
  private DataIntegrityDashboardService mockDataIntegrityService;

  private static final Long FIXTURE_STATE_A_ID = 99881L;
  private static final Long FIXTURE_STATE_B_ID = 99882L;
  private static final Long FIXTURE_DISTRICT_A_ID = 99881L; // belongs to State A
  private static final Long FIXTURE_DISTRICT_B_ID = 99882L; // belongs to State B

  private static final Long USER_SUPER_ADMIN_ID = 99881L;
  private static final Long USER_NATIONAL_ADMIN_ID = 99882L;
  private static final Long USER_STATE_A_ADMIN_ID = 99883L;
  private static final Long USER_STATE_B_ADMIN_ID = 99884L;
  private static final Long USER_PROVIDER_ID = 99885L;
  private static final Long USER_EMPLOYER_ID = 99886L;
  private static final Long USER_TRAINEE_ID = 99887L;

  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAAdmin;
  private String tokenStateBAdmin;
  private String tokenProvider;
  private String tokenEmployer;
  private String tokenTrainee;

  @BeforeEach
  void setUpFixturesAndTokens() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    cleanUpFixtures();

    // 1. Seed Geographic Fixtures: States A and B, Districts A and B
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SA', 'Fixture State A', 'LG99881', 1, NOW(), NOW())
        """, FIXTURE_STATE_A_ID);

    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SB', 'Fixture State B', 'LG99882', 1, NOW(), NOW())
        """, FIXTURE_STATE_B_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DA', 'Fixture District A (State A)', 'LGD99881', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_A_ID, FIXTURE_STATE_A_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DB', 'Fixture District B (State B)', 'LGD99882', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_B_ID, FIXTURE_STATE_B_ID);

    // 2. Seed User Persona Fixtures
    // Super Administrator
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_superadmin', 'p2710_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_SUPER_ADMIN_ID);

    // National Administrator (no state_id)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_natadmin', 'p2710_natadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_NATIONAL_ADMIN_ID);

    // State A Administrator (state_id = 99881)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p2710_state_a_admin', 'p2710_state_a_admin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_A_ADMIN_ID, FIXTURE_STATE_A_ID);

    // State B Administrator (state_id = 99882)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p2710_state_b_admin', 'p2710_state_b_admin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_B_ADMIN_ID, FIXTURE_STATE_B_ID);

    // Training Provider User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_tp_user', 'p2710_tp_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_PROVIDER_ID);

    // Employer User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_emp_user', 'p2710_emp_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_EMPLOYER_ID);

    // Trainee User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_trainee_user', 'p2710_trainee_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_ID);

    // 3. Issue Authentic JWT Access Tokens for each Persona
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "p2710_superadmin", List.of("ROLE_super_admin", "analytics.read"));

    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_NATIONAL_ADMIN_ID, "p2710_natadmin", List.of("ROLE_government_admin", "analytics.read"));

    tokenStateAAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ADMIN_ID, "p2710_state_a_admin", List.of("ROLE_government_admin", "analytics.read"));

    tokenStateBAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ADMIN_ID, "p2710_state_b_admin", List.of("ROLE_government_admin", "analytics.read"));

    tokenProvider = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_ID, "p2710_tp_user", List.of("ROLE_training_provider", "batch.read"));

    tokenEmployer = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_ID, "p2710_emp_user", List.of("ROLE_employer", "placement.read"));

    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_ID, "p2710_trainee_user", List.of("ROLE_trainee"));

    // 4. Setup Default Lenient Mocks returning null data (200 OK wrapper)
    lenient().when(mockGeographicService.getNationalGeographicOverview()).thenReturn(null);
    lenient().when(mockGeographicService.getStateGeographicAnalytics(any())).thenReturn(null);
    lenient().when(mockGeographicService.getDistrictGeographicAnalytics(any())).thenReturn(null);
    lenient().when(mockGeographicService.getDistrictRankings()).thenReturn(null);

    lenient().when(mockExecutiveService.getExecutiveDashboard()).thenReturn(null);
    lenient().when(mockExecutiveService.getDashboardKpiStrip()).thenReturn(null);
    lenient().when(mockExecutiveService.getTrainingPipelineFunnel()).thenReturn(null);
    lenient().when(mockExecutiveService.getSchemeProgramPerformanceMatrix()).thenReturn(null);

    lenient().when(mockInstitutionalService.getProviderLeaderboard()).thenReturn(null);
    lenient().when(mockInstitutionalService.getCourseLeaderboard()).thenReturn(null);

    lenient().when(mockEconomicService.getEconomicOutcomeSummary()).thenReturn(null);
    lenient().when(mockEconomicService.getEmployerLeaderboard()).thenReturn(null);

    lenient().when(mockRetentionService.getLongitudinalRetention()).thenReturn(null);
    lenient().when(mockRetentionService.getAttritionDashboard()).thenReturn(null);

    lenient().when(mockSkillGapService.getSkillGapDashboard()).thenReturn(null);

    lenient().when(mockDataIntegrityService.getVerificationDashboard()).thenReturn(null);
    lenient().when(mockDataIntegrityService.getFollowupDashboard()).thenReturn(null);
    lenient().when(mockDataIntegrityService.getSurveyDashboard()).thenReturn(null);
  }

  @AfterEach
  void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_NATIONAL_ADMIN_ID, USER_STATE_A_ADMIN_ID,
        USER_STATE_B_ADMIN_ID, USER_PROVIDER_ID, USER_EMPLOYER_ID, USER_TRAINEE_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?)",
        FIXTURE_DISTRICT_A_ID, FIXTURE_DISTRICT_B_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)",
        FIXTURE_STATE_A_ID, FIXTURE_STATE_B_ID);
  }

  // =========================================================================
  // 1. Super Administrator Permitted Access Tests
  // =========================================================================
  @Nested
  @DisplayName("1. Super Administrator Persona (Permitted Nationwide)")
  class SuperAdminTests {

    @Test
    @DisplayName("Super admin permitted on national overview")
    void testSuperAdmin_nationalOverview() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Super admin permitted on State A and State B")
    void testSuperAdmin_crossStateAccess() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/{stateId}", FIXTURE_STATE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/dashboards/states/{stateId}", FIXTURE_STATE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Super admin permitted on District A and District B")
    void testSuperAdmin_crossDistrictAccess() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/{districtId}", FIXTURE_DISTRICT_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/dashboards/districts/{districtId}", FIXTURE_DISTRICT_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }
  }

  // =========================================================================
  // 2. National Administrator Permitted Access Tests
  // =========================================================================
  @Nested
  @DisplayName("2. National Administrator Persona (Permitted Nationwide)")
  class NationalAdminTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/courses",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/followups",
        "/api/v1/dashboards/surveys"
    })
    @DisplayName("National admin permitted across all nationwide dashboard endpoints")
    void testNationalAdmin_nationwideEndpoints(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("National admin permitted across all states and districts")
    void testNationalAdmin_stateAndDistrictEndpoints() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/{stateId}", FIXTURE_STATE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/dashboards/districts/{districtId}", FIXTURE_DISTRICT_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }
  }

  // =========================================================================
  // 3. State Administrator Tenancy Boundaries Tests
  // =========================================================================
  @Nested
  @DisplayName("3. State Administrator Tenancy Boundaries (State A)")
  class StateAdminTenancyTests {

    @Test
    @DisplayName("State A admin permitted on assigned State A (/states/99881)")
    void testStateAAdmin_permittedOnAssignedState() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/{stateId}", FIXTURE_STATE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("State A admin permitted on constituent District A (/districts/99881 in State A)")
    void testStateAAdmin_permittedOnConstituentDistrict() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/{districtId}", FIXTURE_DISTRICT_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("State A admin strictly FORBIDDEN from other State B (/states/99882) -> 403")
    void testStateAAdmin_forbiddenFromOtherState() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/states/{stateId}", FIXTURE_STATE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("State A admin strictly FORBIDDEN from cross-state District B (/districts/99882 in State B) -> 403")
    void testStateAAdmin_forbiddenFromCrossStateDistrict() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/{districtId}", FIXTURE_DISTRICT_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/national",
        "/api/v1/dashboards/kpis",
        "/api/v1/dashboards/pipeline-funnel",
        "/api/v1/dashboards/scheme-matrix",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/courses",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/economic-outcomes",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/attrition",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/followups",
        "/api/v1/dashboards/surveys"
    })
    @DisplayName("State A admin strictly FORBIDDEN from all 16 nationwide endpoints -> 403")
    void testStateAAdmin_forbiddenFromNationwideEndpoints(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }
  }

  // =========================================================================
  // 4. Non-Administrative Persona Isolation Tests (Provider, Employer, Trainee)
  // =========================================================================
  @Nested
  @DisplayName("4. Non-Administrative Persona Boundaries (Provider, Employer, Trainee)")
  class NonAdminPersonaTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/states/99881",
        "/api/v1/dashboards/districts/99881"
    })
    @DisplayName("Training Provider persona strictly FORBIDDEN from administrative dashboards -> 403")
    void testTrainingProvider_forbiddenFromAdministrativeDashboards(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenProvider)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/states/99881",
        "/api/v1/dashboards/districts/99881"
    })
    @DisplayName("Employer persona strictly FORBIDDEN from administrative dashboards -> 403")
    void testEmployer_forbiddenFromAdministrativeDashboards(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/states/99881",
        "/api/v1/dashboards/districts/99881"
    })
    @DisplayName("Trainee persona strictly FORBIDDEN from administrative dashboards -> 403")
    void testTrainee_forbiddenFromAdministrativeDashboards(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }
  }

  // =========================================================================
  // 5. Unauthenticated Caller Tests
  // =========================================================================
  @Nested
  @DisplayName("5. Unauthenticated Access Protection")
  class UnauthenticatedAccessTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/providers",
        "/api/v1/dashboards/employers",
        "/api/v1/dashboards/retention",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/dashboards/states/99881",
        "/api/v1/dashboards/districts/99881"
    })
    @DisplayName("Unauthenticated caller receives HTTP 401 Unauthorized")
    void testUnauthenticated_returns401(String path) throws Exception {
      mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }
  }

  // =========================================================================
  // 6. Defense-in-Depth: Service Implementation Scope Enforcement Tests
  // =========================================================================
  @Nested
  @DisplayName("6. Defense-in-Depth: Service Boundary Scope Enforcement")
  class ServiceBoundaryTenancyTests {

    @Test
    @DisplayName("GeographicDashboardServiceImpl rejects state admin attempting nationwide overview")
    void testGeographicService_rejectsStateAdminNationwide() {
      GeographicDashboardServiceImpl service = new GeographicDashboardServiceImpl(
          mock(GeographicDashboardRepository.class),
          workflowSecurityScopeValidator
      );

      UserPrincipal principal = new UserPrincipal(USER_STATE_A_ADMIN_ID, "p2710_state_a_admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_government_admin"), new SimpleGrantedAuthority("analytics.read"))));

      assertThatThrownBy(() -> service.getNationalGeographicOverview())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }

    @Test
    @DisplayName("ExecutiveDashboardServiceImpl rejects training provider attempting executive overview")
    void testExecutiveService_rejectsProvider() {
      ExecutiveDashboardServiceImpl service = new ExecutiveDashboardServiceImpl(
          mock(ExecutiveDashboardRepository.class),
          workflowSecurityScopeValidator
      );

      UserPrincipal principal = new UserPrincipal(USER_PROVIDER_ID, "p2710_tp_user");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_training_provider"))));

      assertThatThrownBy(() -> service.getExecutiveDashboard())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }

    @Test
    @DisplayName("SkillGapDashboardServiceImpl rejects employer attempting skill gap analytics")
    void testSkillGapService_rejectsEmployer() {
      SkillGapDashboardServiceImpl service = new SkillGapDashboardServiceImpl(
          mock(SkillGapDashboardRepository.class),
          workflowSecurityScopeValidator
      );

      UserPrincipal principal = new UserPrincipal(USER_EMPLOYER_ID, "p2710_emp_user");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_employer"))));

      assertThatThrownBy(() -> service.getSkillGapDashboard())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }

    @Test
    @DisplayName("DataIntegrityDashboardServiceImpl rejects trainee attempting integrity analytics")
    void testDataIntegrityService_rejectsTrainee() {
      DataIntegrityDashboardServiceImpl service = new DataIntegrityDashboardServiceImpl(
          mock(DataIntegrityDashboardRepository.class),
          workflowSecurityScopeValidator
      );

      UserPrincipal principal = new UserPrincipal(USER_TRAINEE_ID, "p2710_trainee_user");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_trainee"))));

      assertThatThrownBy(() -> service.getVerificationDashboard())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }
  }
}
