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
import in.gov.sih.sih26135.controller.DataIntegrityDashboardController;
import in.gov.sih.sih26135.controller.EconomicOutcomeDashboardController;
import in.gov.sih.sih26135.controller.ExecutiveDashboardController;
import in.gov.sih.sih26135.controller.GeographicDashboardController;
import in.gov.sih.sih26135.controller.InstitutionalAnalyticsController;
import in.gov.sih.sih26135.controller.RetentionAttritionDashboardController;
import in.gov.sih.sih26135.controller.SkillGapDashboardController;
import in.gov.sih.sih26135.controller.analytics.AnalyticsSummaryController;
import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import in.gov.sih.sih26135.repository.analytics.DataIntegrityDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.EconomicOutcomeDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.ExecutiveDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.GeographicDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.InstitutionalAnalyticsRepository;
import in.gov.sih.sih26135.repository.analytics.RetentionAttritionDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.SkillGapDashboardRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.AttritionReasonSummaryService;
import in.gov.sih.sih26135.service.CourseOutcomeSummaryService;
import in.gov.sih.sih26135.service.DataIntegrityDashboardService;
import in.gov.sih.sih26135.service.DistrictOutcomeSummaryService;
import in.gov.sih.sih26135.service.EconomicOutcomeDashboardService;
import in.gov.sih.sih26135.service.EmployerHiringSummaryService;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.service.FollowupOutcomeSummaryService;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import in.gov.sih.sih26135.service.ProgramOutcomeSummaryService;
import in.gov.sih.sih26135.service.ProviderOutcomeSummaryService;
import in.gov.sih.sih26135.service.RetentionAttritionDashboardService;
import in.gov.sih.sih26135.service.SkillGapDashboardService;
import in.gov.sih.sih26135.service.SkillGapSummaryService;
import in.gov.sih.sih26135.service.TraineeOutcomeSummaryService;
import in.gov.sih.sih26135.service.UnemploymentReasonSummaryService;
import in.gov.sih.sih26135.service.impl.DataIntegrityDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.EconomicOutcomeDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.ExecutiveDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.GeographicDashboardServiceImpl;
import in.gov.sih.sih26135.service.impl.InstitutionalAnalyticsServiceImpl;
import in.gov.sih.sih26135.service.impl.RetentionAttritionDashboardServiceImpl;
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
 * dashboard endpoints spanning all 7 Phase 27 dashboard modules plus persona analytics APIs:
 * <ol>
 *   <li>Super Administrator: Complete nationwide and state/district access.</li>
 *   <li>National Administrator: Verified nationwide analytical authority without state boundary.</li>
 *   <li>State Administrator (State A): Authorized strictly for assigned State A and constituent District A;
 *       forbidden from State B (HTTP 403), District B (HTTP 403), and all nationwide endpoints (HTTP 403).</li>
 *   <li>Training Provider: Strictly isolated to own operations; forbidden from administrative dashboards (HTTP 403);
 *       permitted for own provider outcome summaries, forbidden from cross-provider outcomes and nationwide leaderboards.</li>
 *   <li>Employer: Strictly isolated to own operations; forbidden from administrative dashboards (HTTP 403);
 *       permitted for own hiring summaries, forbidden from cross-employer summaries and nationwide leaderboards.</li>
 *   <li>Trainee: Strictly isolated to self; forbidden from administrative dashboards (HTTP 403);
 *       permitted for own trainee outcome summaries, forbidden from cross-trainee outcomes and nationwide rollups.</li>
 *   <li>Permission-Only Privilege Escalation: analytics.read alone fails closed (HTTP 403) without national admin role.</li>
 *   <li>Missing User Records: Unregistered user tokens fail closed (HTTP 403).</li>
 *   <li>Username Fallback Attempts: Removed fallback logic ensures usernames cannot bypass role/DB checks (HTTP 403).</li>
 *   <li>Defense-in-Depth: Both Controller boundary and Service boundary independently enforce tenancy.</li>
 *   <li>Missing Security Context: Null authentication fails closed (AccessDeniedException).</li>
 *   <li>Mandatory Validator Dependency: Service constructors reject null validator with NullPointerException.</li>
 *   <li>Unauthenticated caller: HTTP 401 Unauthorized across all dashboard endpoints.</li>
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

  @MockBean
  private ProviderOutcomeSummaryService mockProviderOutcomeService;

  @MockBean
  private EmployerHiringSummaryService mockEmployerHiringService;

  @MockBean
  private TraineeOutcomeSummaryService mockTraineeOutcomeService;

  @MockBean
  private AttritionReasonSummaryService mockAttritionReasonSummaryService;

  @MockBean
  private CourseOutcomeSummaryService mockCourseOutcomeSummaryService;

  @MockBean
  private DistrictOutcomeSummaryService mockDistrictOutcomeSummaryService;

  @MockBean
  private FollowupOutcomeSummaryService mockFollowupOutcomeSummaryService;

  @MockBean
  private ProgramOutcomeSummaryService mockProgramOutcomeSummaryService;

  @MockBean
  private SkillGapSummaryService mockSkillGapSummaryService;

  @MockBean
  private UnemploymentReasonSummaryService mockUnemploymentReasonSummaryService;

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
  private static final Long USER_PERM_ONLY_ID = 99889L;

  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAAdmin;
  private String tokenStateBAdmin;
  private String tokenProvider;
  private String tokenEmployer;
  private String tokenTrainee;
  private String tokenPermissionOnly;
  private String tokenMissingUser;
  private String tokenUsernameFallbackAttempt;

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

    // 2. Seed Organization Parent Fixtures: Training Provider 99885 and Employer 99886
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-P2710-99885', 'Provider Fixture 99885', 'REG-PRV-99885', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_PROVIDER_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-P2710-99886', 'Employer Fixture 99886', 'REG-EMP-99886', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_EMPLOYER_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    // 3. Seed User Persona Fixtures
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

    // Training Provider User (assigned to provider 99885)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'p2710_tp_user', 'p2710_tp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_ID, USER_PROVIDER_ID);

    // Employer User (assigned to employer 99886)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'p2710_emp_user', 'p2710_emp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_ID, USER_EMPLOYER_ID);

    // Trainee User (user 99887)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_trainee_user', 'p2710_trainee_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_ID);

    // Seed Trainee entity fixture mapped to user 99887
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-P2710-99887', 'Test', 'Trainee', 1, ?, ?, 1, 1, NOW(), NOW())
        """, USER_TRAINEE_ID, USER_TRAINEE_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    // Permission-Only User (holds analytics.read but NO admin role)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2710_perm_only', 'p2710_perm_only@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_PERM_ONLY_ID);

    // 4. Issue Authentic JWT Access Tokens for each Persona
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

    tokenPermissionOnly = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PERM_ONLY_ID, "p2710_perm_only", List.of("analytics.read"));

    tokenMissingUser = "Bearer " + jwtTokenProvider.generateAccessToken(
        999999L, "p2710_ghost_admin", List.of("ROLE_super_admin", "analytics.read"));

    tokenUsernameFallbackAttempt = "Bearer " + jwtTokenProvider.generateAccessToken(
        999998L, "admin", List.of("ROLE_super_admin", "analytics.read"));

    // 5. Setup Default Lenient Mocks returning null or minimal data (200 OK wrapper)
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

    lenient().when(mockProviderOutcomeService.getProviderOutcomeByProviderId(any())).thenReturn(mock(ProviderOutcomeSummaryResponse.class));
    lenient().when(mockEmployerHiringService.getEmployerHiringSummaryByEmployerId(any())).thenReturn(mock(EmployerHiringSummaryResponse.class));
    lenient().when(mockTraineeOutcomeService.getTraineeOutcomeByTraineeId(any())).thenReturn(mock(TraineeOutcomeSummaryResponse.class));
  }

  @AfterEach
  void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", USER_TRAINEE_ID);
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_NATIONAL_ADMIN_ID, USER_STATE_A_ADMIN_ID,
        USER_STATE_B_ADMIN_ID, USER_PROVIDER_ID, USER_EMPLOYER_ID, USER_TRAINEE_ID, USER_PERM_ONLY_ID);
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", USER_PROVIDER_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", USER_EMPLOYER_ID);
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

  // =========================================================================
  // 7. Permission-Only Privilege Escalation Tests (analytics.read alone)
  // =========================================================================
  @Nested
  @DisplayName("7. Permission-Only Privilege Escalation (analytics.read without admin role fails closed)")
  class PermissionOnlyEscalationTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/dashboards/overview",
        "/api/v1/dashboards/geography",
        "/api/v1/dashboards/skill-gaps",
        "/api/v1/dashboards/verifications",
        "/api/v1/analytics/summaries/provider-outcomes/leaderboard",
        "/api/v1/analytics/summaries/employer-hirings/leaderboard"
    })
    @DisplayName("analytics.read authority alone without verified admin role is rejected -> 403 Forbidden")
    void testPermissionOnly_rejectedFromNationwideEndpoints(String path) throws Exception {
      mockMvc.perform(get(path)
              .header(HttpHeaders.AUTHORIZATION, tokenPermissionOnly)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Validator directly rejects permission-only caller for nationwide scope")
    void testValidatorDirectlyRejectsPermissionOnly() {
      UserPrincipal principal = new UserPrincipal(USER_PERM_ONLY_ID, "p2710_perm_only");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("analytics.read"))));

      assertThat(workflowSecurityScopeValidator.isNationwideAdmin()).isFalse();
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateNationwideScope())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }
  }

  // =========================================================================
  // 8. Missing User Records & Ambiguous Tenancy Assignment (Fail Closed)
  // =========================================================================
  @Nested
  @DisplayName("8. Missing User Records & Ambiguous Tenancy Assignment (Fail Closed)")
  class MissingUserRecordTests {

    @Test
    @DisplayName("Token claiming admin role with nonexistent user record fails closed -> 403 Forbidden")
    void testMissingUserRecord_rejectedAtController() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenMissingUser)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Validator directly fails closed when user record does not exist in database")
    void testMissingUserRecord_rejectedAtValidator() {
      UserPrincipal principal = new UserPrincipal(999999L, "p2710_ghost_admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_super_admin"), new SimpleGrantedAuthority("analytics.read"))));

      assertThat(workflowSecurityScopeValidator.isNationwideAdmin()).isFalse();
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateNationwideScope())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }
  }

  // =========================================================================
  // 9. Removal of Username-Based Administrative Fallbacks
  // =========================================================================
  @Nested
  @DisplayName("9. Removal of Username-Based Administrative Fallbacks")
  class UsernameFallbackRemovalTests {

    @Test
    @DisplayName("Caller with username 'admin' but missing database record is rejected -> 403 Forbidden")
    void testAdminUsername_missingRecordFailsClosed() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenUsernameFallbackAttempt)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Caller with username 'admin' and state assignment cannot access nationwide overview")
    void testAdminUsername_stateScopedDisqualifiedFromNationwide() {
      // User 99883 is assigned to State A (99881). Even if username is spoofed as "admin",
      // the authoritative database record restricts tenancy and disqualifies nationwide access.
      UserPrincipal principal = new UserPrincipal(USER_STATE_A_ADMIN_ID, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal, null, List.of(new SimpleGrantedAuthority("ROLE_government_admin"), new SimpleGrantedAuthority("analytics.read"))));

      assertThat(workflowSecurityScopeValidator.isNationwideAdmin()).isFalse();
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateNationwideScope())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
    }
  }

  // =========================================================================
  // 10. Missing Security Context & Direct Service Invocations
  // =========================================================================
  @Nested
  @DisplayName("10. Missing Security Context & Unauthenticated Direct Service Invocations")
  class MissingSecurityContextTests {

    @Test
    @DisplayName("All dashboard services reject invocations when SecurityContext is null or unauthenticated")
    void testNullSecurityContextRejectionAcrossServices() {
      SecurityContextHolder.getContext().setAuthentication(null);

      // Verify validator itself fails closed
      assertThat(workflowSecurityScopeValidator.isNationwideAdmin()).isFalse();
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateNationwideScope())
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateStateScope(FIXTURE_STATE_A_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateDistrictScope(FIXTURE_DISTRICT_A_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateDashboardProviderScope(USER_PROVIDER_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateDashboardEmployerScope(USER_EMPLOYER_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");
      assertThatThrownBy(() -> workflowSecurityScopeValidator.validateDashboardTraineeScope(USER_TRAINEE_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Access denied");

      // Verify each service implementation fails closed on null authentication
      ExecutiveDashboardServiceImpl execService = new ExecutiveDashboardServiceImpl(mock(ExecutiveDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(execService::getExecutiveDashboard).isInstanceOf(AccessDeniedException.class);

      GeographicDashboardServiceImpl geoService = new GeographicDashboardServiceImpl(mock(GeographicDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(geoService::getNationalGeographicOverview).isInstanceOf(AccessDeniedException.class);

      InstitutionalAnalyticsServiceImpl instService = new InstitutionalAnalyticsServiceImpl(mock(InstitutionalAnalyticsRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(instService::getProviderLeaderboard).isInstanceOf(AccessDeniedException.class);

      EconomicOutcomeDashboardServiceImpl econService = new EconomicOutcomeDashboardServiceImpl(mock(EconomicOutcomeDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(econService::getEconomicOutcomeSummary).isInstanceOf(AccessDeniedException.class);

      RetentionAttritionDashboardServiceImpl retService = new RetentionAttritionDashboardServiceImpl(mock(RetentionAttritionDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(retService::getLongitudinalRetention).isInstanceOf(AccessDeniedException.class);

      SkillGapDashboardServiceImpl skillService = new SkillGapDashboardServiceImpl(mock(SkillGapDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(skillService::getSkillGapDashboard).isInstanceOf(AccessDeniedException.class);

      DataIntegrityDashboardServiceImpl dataService = new DataIntegrityDashboardServiceImpl(mock(DataIntegrityDashboardRepository.class), workflowSecurityScopeValidator);
      assertThatThrownBy(dataService::getVerificationDashboard).isInstanceOf(AccessDeniedException.class);
    }
  }

  // =========================================================================
  // 11. Mandatory Validator Dependency & Fail-Fast Construction Tests
  // =========================================================================
  @Nested
  @DisplayName("11. Mandatory Validator Dependency & Fail-Fast Construction")
  class MandatoryValidatorConstructionTests {

    @Test
    @DisplayName("Dashboard service constructors strictly reject null WorkflowSecurityScopeValidator with NullPointerException")
    void testServiceConstructorsRejectNullValidator() {
      assertThatThrownBy(() -> new ExecutiveDashboardServiceImpl(mock(ExecutiveDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new GeographicDashboardServiceImpl(mock(GeographicDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new InstitutionalAnalyticsServiceImpl(mock(InstitutionalAnalyticsRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new EconomicOutcomeDashboardServiceImpl(mock(EconomicOutcomeDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new RetentionAttritionDashboardServiceImpl(mock(RetentionAttritionDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new SkillGapDashboardServiceImpl(mock(SkillGapDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");

      assertThatThrownBy(() -> new DataIntegrityDashboardServiceImpl(mock(DataIntegrityDashboardRepository.class), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required");
    }

    @Test
    @DisplayName("Dashboard controller constructors strictly reject null WorkflowSecurityScopeValidator with NullPointerException")
    void testControllerConstructorsRejectNullValidator() {
      assertThatThrownBy(() -> new ExecutiveDashboardController(mock(ExecutiveDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new GeographicDashboardController(mock(GeographicDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new InstitutionalAnalyticsController(mock(InstitutionalAnalyticsService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new EconomicOutcomeDashboardController(mock(EconomicOutcomeDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new RetentionAttritionDashboardController(mock(RetentionAttritionDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new SkillGapDashboardController(mock(SkillGapDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new DataIntegrityDashboardController(mock(DataIntegrityDashboardService.class), null))
          .isInstanceOf(NullPointerException.class);

      assertThatThrownBy(() -> new AnalyticsSummaryController(
          mock(AttritionReasonSummaryService.class), mock(CourseOutcomeSummaryService.class),
          mock(DistrictOutcomeSummaryService.class), mock(EmployerHiringSummaryService.class),
          mock(FollowupOutcomeSummaryService.class), mock(ProgramOutcomeSummaryService.class),
          mock(ProviderOutcomeSummaryService.class), mock(SkillGapSummaryService.class),
          mock(TraineeOutcomeSummaryService.class), mock(UnemploymentReasonSummaryService.class),
          null
      )).isInstanceOf(NullPointerException.class);
    }
  }

  // =========================================================================
  // 12. Persona Dashboard Boundaries & Cross-Tenant Rejection Tests
  // =========================================================================
  @Nested
  @DisplayName("12. Persona Dashboard Boundaries & Cross-Tenant Isolation")
  class PersonaDashboardBoundaryTests {

    @Test
    @DisplayName("Training Provider: permitted for own provider outcome (200 OK), forbidden from other provider (403), forbidden from nationwide leaderboard (403)")
    void testTrainingProviderBoundaries() throws Exception {
      // 1. Own provider outcome -> 200 OK
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/{providerId}", USER_PROVIDER_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProvider)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // 2. Cross-tenant other provider outcome -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/{providerId}", 99886L)
              .header(HttpHeaders.AUTHORIZATION, tokenProvider)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // 3. Nationwide provider leaderboard -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/leaderboard")
              .header(HttpHeaders.AUTHORIZATION, tokenProvider)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Employer: permitted for own hiring summary (200 OK), forbidden from other employer (403), forbidden from nationwide leaderboard (403)")
    void testEmployerBoundaries() throws Exception {
      // 1. Own employer hiring summary -> 200 OK
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/{employerId}", USER_EMPLOYER_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // 2. Cross-tenant other employer hiring summary -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/{employerId}", 99885L)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // 3. Nationwide employer leaderboard -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/leaderboard")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Trainee: permitted for own outcome (200 OK), forbidden from other trainee (403), forbidden from nationwide outcomes (403), forbidden from state outcomes (403)")
    void testTraineeBoundaries() throws Exception {
      // 1. Own trainee outcome -> 200 OK
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes/{traineeId}", USER_TRAINEE_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // 2. Cross-tenant other trainee outcome -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes/{traineeId}", 99888L)
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // 3. Nationwide trainee outcomes -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes")
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));

      // 4. State-scoped trainee outcomes -> 403 Forbidden (trainee is not a state admin)
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes")
              .param("stateId", String.valueOf(FIXTURE_STATE_A_ID))
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Super Administrator has legitimate cross-tenant visibility across provider, employer, and trainee outcomes")
    void testSuperAdminCrossTenantAccess() throws Exception {
      // Super admin can access provider outcome
      mockMvc.perform(get("/api/v1/analytics/summaries/provider-outcomes/{providerId}", USER_PROVIDER_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // Super admin can access employer hiring
      mockMvc.perform(get("/api/v1/analytics/summaries/employer-hirings/{employerId}", USER_EMPLOYER_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // Super admin can access trainee outcome
      mockMvc.perform(get("/api/v1/analytics/summaries/trainee-outcomes/{traineeId}", USER_TRAINEE_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Training Provider: strictly forbidden from all nationwide summaries and leaderboards with zero disclosure")
    void testTrainingProviderForbiddenFromNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/attrition-reasons/leaderboard",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/course-outcomes/leaderboard",
          "/api/v1/analytics/summaries/course-outcomes/1",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes/leaderboard",
          "/api/v1/analytics/summaries/followup-outcomes/T1/3",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/program-outcomes/leaderboard",
          "/api/v1/analytics/summaries/program-outcomes/1",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/skill-gaps/leaderboard",
          "/api/v1/analytics/summaries/skill-gaps/1/1",
          "/api/v1/analytics/summaries/unemployment-reasons",
          "/api/v1/analytics/summaries/unemployment-reasons/leaderboard",
          "/api/v1/analytics/summaries/unemployment-reasons/1"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenProvider)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("Employer: strictly forbidden from all nationwide summaries and leaderboards with zero disclosure")
    void testEmployerForbiddenFromNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/unemployment-reasons"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenEmployer)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("Trainee: strictly forbidden from all nationwide summaries and leaderboards with zero disclosure")
    void testTraineeForbiddenFromNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/unemployment-reasons"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenTrainee)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("State Administrator: strictly forbidden from unscoped nationwide summaries and leaderboards")
    void testStateAdminForbiddenFromNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/attrition-reasons/leaderboard",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/course-outcomes/leaderboard",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes/leaderboard",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/program-outcomes/leaderboard",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/skill-gaps/leaderboard",
          "/api/v1/analytics/summaries/unemployment-reasons",
          "/api/v1/analytics/summaries/unemployment-reasons/leaderboard"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("Permission-Only Caller (analytics.read without admin role): forbidden from all nationwide summaries")
    void testPermissionOnlyForbiddenFromNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/unemployment-reasons"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenPermissionOnly)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("Adversarial query parameter bypass attempts across summary endpoints fail closed with HTTP 403")
    void testAdversarialQueryParameterBypassAttemptsFailClosed() throws Exception {
      List<String> queryEndpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons?exitReasonId=1",
          "/api/v1/analytics/summaries/attrition-reasons?isVoluntaryFlag=true",
          "/api/v1/analytics/summaries/course-outcomes?courseId=1",
          "/api/v1/analytics/summaries/followup-outcomes?typeCode=T1",
          "/api/v1/analytics/summaries/followup-outcomes?offsetMonths=3",
          "/api/v1/analytics/summaries/program-outcomes?programId=1",
          "/api/v1/analytics/summaries/program-outcomes?schemeId=1",
          "/api/v1/analytics/summaries/skill-gaps?skillId=1",
          "/api/v1/analytics/summaries/skill-gaps?severityId=1",
          "/api/v1/analytics/summaries/skill-gaps?severityCode=HIGH",
          "/api/v1/analytics/summaries/unemployment-reasons?unemploymentReasonId=1",
          "/api/v1/analytics/summaries/unemployment-reasons?code=HEALTH"
      );

      for (String qUrl : queryEndpoints) {
        mockMvc.perform(get(qUrl)
                .header(HttpHeaders.AUTHORIZATION, tokenProvider)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Access denied"))
            .andExpect(jsonPath("$.data").doesNotExist());
      }
    }

    @Test
    @DisplayName("Super Admin and National Admin: permitted across nationwide summary and leaderboard endpoints")
    void testSuperAndNationalAdminPermittedAcrossNationwideSummaries() throws Exception {
      List<String> endpoints = List.of(
          "/api/v1/analytics/summaries/attrition-reasons",
          "/api/v1/analytics/summaries/attrition-reasons/leaderboard",
          "/api/v1/analytics/summaries/course-outcomes",
          "/api/v1/analytics/summaries/course-outcomes/leaderboard",
          "/api/v1/analytics/summaries/followup-outcomes",
          "/api/v1/analytics/summaries/followup-outcomes/leaderboard",
          "/api/v1/analytics/summaries/program-outcomes",
          "/api/v1/analytics/summaries/program-outcomes/leaderboard",
          "/api/v1/analytics/summaries/skill-gaps",
          "/api/v1/analytics/summaries/skill-gaps/leaderboard",
          "/api/v1/analytics/summaries/unemployment-reasons",
          "/api/v1/analytics/summaries/unemployment-reasons/leaderboard"
      );

      for (String endpoint : endpoints) {
        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get(endpoint)
                .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
      }
    }
  }
}
