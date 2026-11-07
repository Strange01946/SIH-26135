package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.config.AnalyticsExportProperties;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.export.DsdpExportRow;
import in.gov.sih.sih26135.dto.response.export.ProviderScorecardExportRow;
import in.gov.sih.sih26135.dto.response.export.QprExportRow;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.RateLimitExceededException;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.analytics.AnalyticsExportRepository;
import in.gov.sih.sih26135.repository.analytics.AnalyticsExportRepository.DistrictMetadata;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.export.ExportRateLimiter;
import in.gov.sih.sih26135.service.export.SafeCsvStreamWriter;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Comprehensive automated tests for Phase 27.15 Government MIS Report Generation & Data Export API.
 *
 * <p>Verifies:
 * <ul>
 *   <li>6 Export Combinations (DSDP CSV/JSON, QPR CSV/JSON, Provider Scorecard CSV/JSON).</li>
 *   <li>Streaming execution, response headers, and memory bounded chunking.</li>
 *   <li>In-memory sliding-window download rate limiting and active concurrency protection.</li>
 *   <li>Configurable export row-count guardrails with fail-closed rejection.</li>
 *   <li>Role-based authorization and territorial tenancy boundaries for all personas.</li>
 *   <li>Phase 27.14 statistical disclosure controls (k=5 cell-level outcome, complementary, and salary suppression).</li>
 *   <li>Spreadsheet formula injection (CSV Injection / CWE-1236) defense.</li>
 *   <li>Reporting period and Indian fiscal year parsing.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 27.15 — Government MIS Report Generation & Data Export API Tests")
public class AnalyticsExportTest {

  private static final Pattern PII_PATTERN = Pattern.compile(
      "(?<!\\d)[1-9]\\d{3}\\s?\\d{4}\\s?\\d{4}(?!\\d)" // Aadhaar (12-digit)
      + "|(?<!\\w)[A-Z]{5}[0-9]{4}[A-Z](?!\\w)"          // PAN card
      + "|(?<!\\d)[6-9]\\d{9}(?!\\d)"                   // Indian 10-digit mobile number
      + "|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}" // Email address
  );

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ExportRateLimiter rateLimiter;

  @Autowired
  private AnalyticsExportProperties exportProperties;

  @Autowired
  private AnalyticsPrivacySuppressionService privacyService;

  @Autowired
  private WorkflowSecurityScopeValidator scopeValidator;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TrainingProviderRepository trainingProviderRepository;

  @Autowired
  private AnalyticsExportRepository repository;

  @Autowired
  private AnalyticsExportService exportService;

  private Long testStateId;
  private Long testDistrictId;
  private Long testProviderId;
  private Long testForeignStateId;
  private Long testForeignDistrictId;

  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAdmin;
  private String tokenProvider;
  private String tokenEmployer;
  private String tokenTrainee;
  private String tokenPermissionOnly;

  private Long userSuperAdminId = 91001L;
  private Long userNationalAdminId = 91002L;
  private Long userStateAdminId = 91003L;
  private Long userProviderId = 91004L;
  private Long userEmployerId = 91005L;
  private Long userTraineeId = 91006L;
  private Long userPermOnlyId = 91007L;

  private static final Long FIXTURE_STATE_A_ID = 99881L;
  private static final Long FIXTURE_STATE_B_ID = 99882L;
  private static final Long FIXTURE_DISTRICT_A_ID = 99883L;
  private static final Long FIXTURE_DISTRICT_B_ID = 99884L;
  private static final Long FIXTURE_PROVIDER_ID = 99885L;
  private static final Long FIXTURE_EMPLOYER_ID = 99886L;
  private static final Long FIXTURE_SECTOR_ID = 99885L;
  private static final Long FIXTURE_PROGRAM_ID = 99885L;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    rateLimiter.reset();
    cleanUpFixtures();

    // 1. Seed Geographic & Provider Fixtures
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
        VALUES (?, ?, 'DA', 'Fixture District A', 'LGD99881', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_A_ID, FIXTURE_STATE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DB', 'Fixture District B', 'LGD99882', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_B_ID, FIXTURE_STATE_B_ID);

    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-99885', 'Fixture Provider 99885', 'REG-99885', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_PROVIDER_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (99885, ?, 'TC-99885', 'Fixture Center 99885', ?, ?, 1, NOW(), NOW())
        """, FIXTURE_PROVIDER_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-99886', 'Fixture Employer 99886', 'REG-EMP-99886', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_EMPLOYER_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    jdbcTemplate.update("""
        INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SEC-99885', 'Fixture Sector 99885', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE sector_name = VALUES(sector_name)
        """, FIXTURE_SECTOR_ID);

    jdbcTemplate.update("""
        INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (99885, 'ORG-99885', 'Fixture Organization 99885', 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE organization_name = VALUES(organization_name)
        """);

    jdbcTemplate.update("""
        INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (99885, 99885, 'DEP-99885', 'Fixture Department 99885', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE department_name = VALUES(department_name)
        """);

    jdbcTemplate.update("""
        INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (99885, 'SCH-99885', 'Fixture Scheme 99885', 99885, 'INR', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE scheme_name = VALUES(scheme_name)
        """);

    jdbcTemplate.update("""
        INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRG-99885', 'Fixture Program 99885', 99885, 99885, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE program_name = VALUES(program_name)
        """, FIXTURE_PROGRAM_ID);

    testStateId = FIXTURE_STATE_A_ID;
    testDistrictId = FIXTURE_DISTRICT_A_ID;
    testForeignStateId = FIXTURE_STATE_B_ID;
    testForeignDistrictId = FIXTURE_DISTRICT_B_ID;
    testProviderId = FIXTURE_PROVIDER_ID;

    // 2. Seed test users
    seedTestUsers();

    // 3. Issue Authentic JWT tokens
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        userSuperAdminId, "export_superadmin", List.of("ROLE_super_admin", "analytics.read"));
    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        userNationalAdminId, "export_natadmin", List.of("ROLE_government_admin", "analytics.read"));
    tokenStateAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        userStateAdminId, "export_stateadmin", List.of("ROLE_government_admin", "analytics.read"));
    tokenProvider = "Bearer " + jwtTokenProvider.generateAccessToken(
        userProviderId, "export_tp_user", List.of("ROLE_training_provider", "batch.read"));
    tokenEmployer = "Bearer " + jwtTokenProvider.generateAccessToken(
        userEmployerId, "export_emp_user", List.of("ROLE_employer", "placement.read"));
    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        userTraineeId, "export_trainee_user", List.of("ROLE_trainee"));
    tokenPermissionOnly = "Bearer " + jwtTokenProvider.generateAccessToken(
        userPermOnlyId, "export_perm_only", List.of("analytics.read"));

    // Set super_admin in security context
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userSuperAdminId, "export_superadmin"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"), new SimpleGrantedAuthority("analytics.read"))
        )
    );
  }

  private void cleanUpFixtures() {
    jdbcTemplate.update("DELETE FROM salary_history WHERE trainee_id >= 99000");
    jdbcTemplate.update("DELETE FROM employment_records WHERE trainee_id >= 99000");
    jdbcTemplate.update("DELETE FROM training_enrollments WHERE trainee_id >= 99000");
    jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id >= 99890");
    jdbcTemplate.update("DELETE FROM training_centers WHERE center_id = 99885");
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id >= 99000 OR trainee_id = ?", userTraineeId);
    jdbcTemplate.update("DELETE FROM courses WHERE course_id IN (99890, 99891)");
    jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", FIXTURE_PROGRAM_ID);
    jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = 99885");
    jdbcTemplate.update("DELETE FROM departments WHERE department_id = 99885");
    jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = 99885");
    jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", FIXTURE_SECTOR_ID);
    jdbcTemplate.update("DELETE FROM users WHERE user_id >= 95000 OR user_id IN (?, ?, ?, ?, ?, ?, ?)",
        userSuperAdminId, userNationalAdminId, userStateAdminId, userProviderId,
        userEmployerId, userTraineeId, userPermOnlyId);
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", FIXTURE_PROVIDER_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", FIXTURE_EMPLOYER_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?)",
        FIXTURE_DISTRICT_A_ID, FIXTURE_DISTRICT_B_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)",
        FIXTURE_STATE_A_ID, FIXTURE_STATE_B_ID);
  }

  private void seedTestUsers() {
    // Super Admin
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'export_superadmin', 'export_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username)
        """, userSuperAdminId);

    // National Admin (no state tenancy)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'export_natadmin', 'export_natadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username), state_id = NULL
        """, userNationalAdminId);

    // State Admin (assigned to testStateId)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'export_stateadmin', 'export_stateadmin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username), state_id = VALUES(state_id)
        """, userStateAdminId, testStateId);

    // Provider User (assigned to testProviderId)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'export_tp_user', 'export_tp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username), training_provider_id = VALUES(training_provider_id)
        """, userProviderId, testProviderId);

    // Employer User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'export_emp_user', 'export_emp_user@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username), employer_id = VALUES(employer_id)
        """, userEmployerId, FIXTURE_EMPLOYER_ID);

    // Trainee User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'export_trainee_user', 'export_trainee_user@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username)
        """, userTraineeId);

    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-EXP-99112', 'Test', 'Trainee', 1, ?, ?, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE registration_number = VALUES(registration_number)
        """, userTraineeId, userTraineeId, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    // Permission-only user
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'export_perm_only', 'export_perm_only@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = VALUES(username)
        """, userPermOnlyId);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    rateLimiter.reset();
    cleanUpFixtures();
  }

  private String extractStreamingContent(MvcResult result) throws Exception {
    if (result.getRequest().isAsyncStarted()) {
      long deadline = System.currentTimeMillis() + 5000;
      while (result.getRequest().isAsyncStarted() && System.currentTimeMillis() < deadline) {
        Thread.sleep(10);
      }
    }
    return result.getResponse().getContentAsString();
  }

  // =========================================================================
  // 1. Export Format & Headers Tests
  // =========================================================================

  @Nested
  @DisplayName("1. Export Format & Headers Tests")
  class ExportFormatTests {

    @Test
    @DisplayName("GET /dsdp/csv: returns 200 OK with text/csv, attachment disposition, and valid headers")
    void testDsdpCsvFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/dsdp/csv")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dsdp-district-" + testDistrictId + ".csv\""))
          .andReturn();

      String content = extractStreamingContent(result);
      assertThat(content).startsWith("District ID,District Code,District Name,State Code,State Name,Course Code,Course Name,Sector Name,Enrollments,Trainees,Completions,Certifications,Placements,Employed,Completion Rate (%),Certification Rate (%),Placement Rate (%),Employment Rate (%),Avg Starting Wage");
      assertNoPii(content);
    }

    @Test
    @DisplayName("GET /dsdp/json: returns 200 OK with application/json, attachment disposition, and valid JSON structure")
    void testDsdpJsonFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dsdp-district-" + testDistrictId + ".json\""))
          .andReturn();

      String content = extractStreamingContent(result);
      JsonNode root = objectMapper.readTree(content);
      assertThat(root.path("reportType").asText()).isEqualTo("DSDP");
      assertThat(root.path("scope").path("level").asText()).isEqualTo("DISTRICT");
      assertThat(root.path("scope").path("districtId").asLong()).isEqualTo(testDistrictId);
      assertThat(root.path("rows").isArray()).isTrue();
      assertThat(root.path("metadata").path("privacyThreshold").asInt()).isEqualTo(5);
      assertNoPii(content);
    }

    @Test
    @DisplayName("GET /qpr/csv: returns 200 OK with text/csv, attachment disposition, and state breakdown headers")
    void testQprCsvFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/qpr/csv")
              .param("fiscalYear", "FY2026-27")
              .param("quarter", "Q1")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qpr-FY2026-27-Q1.csv\""))
          .andReturn();

      String content = extractStreamingContent(result);
      assertThat(content).startsWith("State ID,State Code,State Name,Fiscal Year,Quarter,Enrollments,Trainees,Completions,Certifications,Placements,Employed,Completion Rate (%),Certification Rate (%),Placement Rate (%),Employment Rate (%),Rank");
      assertNoPii(content);
    }

    @Test
    @DisplayName("GET /qpr/json: returns 200 OK with application/json, summary block, and state rows")
    void testQprJsonFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .param("fiscalYear", "FY2026-27")
              .param("quarter", "Q2")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qpr-FY2026-27-Q2.json\""))
          .andReturn();

      String content = extractStreamingContent(result);
      JsonNode root = objectMapper.readTree(content);
      assertThat(root.path("reportType").asText()).isEqualTo("QPR");
      assertThat(root.path("reportingPeriod").path("fiscalYear").asText()).isEqualTo("FY2026-27");
      assertThat(root.path("reportingPeriod").path("quarter").asText()).isEqualTo("Q2");
      assertThat(root.path("scope").path("level").asText()).isEqualTo("NATIONAL");
      assertThat(root.path("summary").isObject()).isTrue();
      assertThat(root.path("rows").isArray()).isTrue();
      assertThat(root.path("metadata").path("privacyThreshold").asInt()).isEqualTo(5);
      assertNoPii(content);
    }

    @Test
    @DisplayName("GET /provider-scorecard/csv: returns 200 OK with provider scorecard headers")
    void testProviderScorecardCsvFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/csv")
              .param("providerId", testProviderId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"provider-scorecard-provider-" + testProviderId + ".csv\""))
          .andReturn();

      String content = extractStreamingContent(result);
      assertThat(content).startsWith("Provider ID,Provider Code,Provider Name,State,District,Active Batch Capacity,Enrollments,Trainees,Completions,Certifications,Placements,Employed,Completion Rate (%),Certification Rate (%),Placement Rate (%),Employment Rate (%),Rank");
      assertNoPii(content);
    }

    @Test
    @DisplayName("GET /provider-scorecard/json: returns 200 OK with provider scorecard JSON structure")
    void testProviderScorecardJsonFormat() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json")
              .param("providerId", testProviderId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
          .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"provider-scorecard-provider-" + testProviderId + ".json\""))
          .andReturn();

      String content = extractStreamingContent(result);
      JsonNode root = objectMapper.readTree(content);
      assertThat(root.path("reportType").asText()).isEqualTo("PROVIDER_SCORECARD");
      assertThat(root.path("scope").path("level").asText()).isEqualTo("PROVIDER");
      assertThat(root.path("scope").path("providerId").asLong()).isEqualTo(testProviderId);
      assertThat(root.path("rows").isArray()).isTrue();
      assertThat(root.path("metadata").path("privacyThreshold").asInt()).isEqualTo(5);
      assertNoPii(content);
    }
  }

  // =========================================================================
  // 2. Download Rate Limiting & Concurrency Tests
  // =========================================================================

  @Nested
  @DisplayName("2. Download Rate Limiting & Concurrency Tests")
  class RateLimitAndConcurrencyTests {

    @Test
    @DisplayName("Rate Limiting: 6th request within 1 minute from same user returns 429 Too Many Requests")
    void testPerMinuteRateLimitExceeded() throws Exception {
      // 5 allowed requests
      for (int i = 0; i < 5; i++) {
        rateLimiter.checkAndAcquire(userSuperAdminId);
        rateLimiter.release(userSuperAdminId);
      }

      // 6th request triggers rate limit
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isTooManyRequests())
          .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
          .andExpect(jsonPath("$.code").value("EXPORT_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Rate Limiting: Limits are isolated per user identity; User A blocked does not affect User B")
    void testRateLimitIsolatedBetweenUsers() throws Exception {
      // Exhaust SuperAdmin limit (5 calls)
      for (int i = 0; i < 5; i++) {
        rateLimiter.checkAndAcquire(userSuperAdminId);
        rateLimiter.release(userSuperAdminId);
      }

      // SuperAdmin is blocked
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isTooManyRequests());

      // NationalAdmin is independent and can still export
      MvcResult rNat = mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(rNat);
    }

    @Test
    @DisplayName("Concurrency: Active permits released cleanly after completion")
    void testConcurrencyPermitsReleasedAfterCompletion() throws Exception {
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();

      MvcResult r = mockMvc.perform(get("/api/v1/analytics/export/dsdp/csv")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r);

      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
    }
  }

  // =========================================================================
  // 3. Row-Count Limits & Guardrails
  // =========================================================================

  @Nested
  @DisplayName("3. Row-Count Limits & Preflight Checks")
  class RowLimitTests {

    @Test
    @DisplayName("Row Ceiling: Setting rowLimitMax below current count rejects with 400 EXPORT_ROW_LIMIT_EXCEEDED before streaming")
    void testRowLimitCeilingRejection() throws Exception {
      int originalMax = exportProperties.getRowLimitMax();
      try {
        // Artificially lower max limit to 0
        exportProperties.setRowLimitMax(0);

        mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
                .param("fiscalYear", "FY2026-27")
                .param("quarter", "Q1")
                .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("EXPORT_ROW_LIMIT_EXCEEDED"));
      } finally {
        exportProperties.setRowLimitMax(originalMax);
      }
    }
  }

  // =========================================================================
  // 4. Role-Based Authorization & Territorial Scope
  // =========================================================================

  @Nested
  @DisplayName("4. Role-Based Authorization & Tenancy Boundaries")
  class AuthorizationAndTenancyTests {

    @Test
    @DisplayName("Super Admin & National Admin: Allowed nationwide across DSDP, QPR, and Provider Scorecards")
    void testAdministrativeNationwideAccess() throws Exception {
      MvcResult r1 = mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r1);

      MvcResult r2 = mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r2);

      MvcResult r3 = mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r3);
    }

    @Test
    @DisplayName("State Admin: Allowed for own-state district; 403 Forbidden for foreign-state district")
    void testStateAdminDistrictScope() throws Exception {
      // Own state district -> 200 OK
      MvcResult r1 = mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r1);

      // Foreign state district -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testForeignDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("State Admin: 403 Forbidden when requesting nationwide QPR export")
    void testStateAdminQprDenied() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/analytics/export/qpr/csv")
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Training Provider: 403 Forbidden for DSDP and QPR; Allowed only for own provider scorecard")
    void testTrainingProviderScope() throws Exception {
      // DSDP -> 403
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", testDistrictId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenProvider))
          .andExpect(status().isForbidden());

      // QPR -> 403
      mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .header(HttpHeaders.AUTHORIZATION, tokenProvider))
          .andExpect(status().isForbidden());

      // Own provider scorecard -> 200 OK
      MvcResult r = mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json")
              .param("providerId", testProviderId.toString())
              .header(HttpHeaders.AUTHORIZATION, tokenProvider))
          .andExpect(status().isOk())
          .andReturn();
      extractStreamingContent(r);

      // Foreign provider scorecard -> 403 Forbidden
      mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json")
              .param("providerId", "999888")
              .header(HttpHeaders.AUTHORIZATION, tokenProvider))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employer, Trainee, Permission-only: 403 Forbidden across all export endpoints")
    void testRestrictedPersonasForbidden() throws Exception {
      for (String token : List.of(tokenEmployer, tokenTrainee, tokenPermissionOnly)) {
        mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
                .param("districtId", testDistrictId.toString())
                .header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
                .header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json")
                .header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isForbidden());
      }
    }

    @Test
    @DisplayName("Anonymous: 401 Unauthorized across all export endpoints")
    void testAnonymousUnauthorized() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json").param("districtId", testDistrictId.toString()))
          .andExpect(status().isUnauthorized());

      mockMvc.perform(get("/api/v1/analytics/export/qpr/json"))
          .andExpect(status().isUnauthorized());

      mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/json"))
          .andExpect(status().isUnauthorized());
    }
  }

  // =========================================================================
  // 5. Phase 27.14 Privacy & Statistical Disclosure Control
  // =========================================================================

  @Nested
  @DisplayName("5. Phase 27.14 Privacy & Suppression Tests")
  class PrivacyPreservationTests {

    @Test
    @DisplayName("DSDP Row Suppression: traineeCount in [1, 4] suppresses all outcome counts and rates to null")
    void testDsdpRowSparseSuppression() {
      DsdpExportRow sparse = new DsdpExportRow(
          1L, "D1", "Dist1", 1L, "S1", "State1",
          101L, "C101", "Welding", "Engineering",
          4L, 3L, 2L, 2L, 1L, 1L,
          new BigDecimal("66.67"), new BigDecimal("66.67"), new BigDecimal("33.33"), new BigDecimal("33.33"),
          new BigDecimal("15000.00")
      );

      DsdpExportRow safe = privacyService.applyPrivacyGuardrail(sparse);
      assertThat(safe.traineeCount()).isNull();
      assertThat(safe.enrollmentCount()).isNull();
      assertThat(safe.completedCount()).isNull();
      assertThat(safe.certifiedCount()).isNull();
      assertThat(safe.joinedPlacementCount()).isNull();
      assertThat(safe.employedCount()).isNull();
      assertThat(safe.completionRatePct()).isNull();
      assertThat(safe.certificationRatePct()).isNull();
      assertThat(safe.placementRatePct()).isNull();
      assertThat(safe.employmentRatePct()).isNull();
      assertThat(safe.avgStartingWage()).isNull();
    }

    @Test
    @DisplayName("DSDP Cell-Level Suppression: cohort of 5 with 3 employed suppresses the 3 employed count and rate")
    void testDsdpCellLevelOutcomeSuppression() {
      DsdpExportRow row = new DsdpExportRow(
          1L, "D1", "Dist1", 1L, "S1", "State1",
          101L, "C101", "Welding", "Engineering",
          5L, 5L, 5L, 5L, 4L, 3L, // 3 employed is sparse!
          new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("80.00"), new BigDecimal("60.00"),
          new BigDecimal("18000.00")
      );

      DsdpExportRow safe = privacyService.applyPrivacyGuardrail(row);
      assertThat(safe.traineeCount()).isEqualTo(5L);
      assertThat(safe.completedCount()).isEqualTo(5L);
      assertThat(safe.certifiedCount()).isEqualTo(5L);
      // Joined (4) and Employed (3) must be suppressed to null
      assertThat(safe.joinedPlacementCount()).isNull();
      assertThat(safe.placementRatePct()).isNull();
      assertThat(safe.employedCount()).isNull();
      assertThat(safe.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("QPR Sibling Secondary Suppression: single sparse state suppresses smallest sibling >= 5")
    void testQprSecondarySuppression() {
      List<QprExportRow> states = List.of(
          new QprExportRow(1L, "MH", "Maharashtra", "FY2026-27", "Q1", 100L, 100L, 90L, 85L, 70L, 65L, new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("70.00"), new BigDecimal("65.00"), 1),
          new QprExportRow(2L, "GA", "Goa", "FY2026-27", "Q1", 3L, 3L, 2L, 2L, 1L, 1L, new BigDecimal("66.67"), new BigDecimal("66.67"), new BigDecimal("33.33"), new BigDecimal("33.33"), 3), // Sparse!
          new QprExportRow(3L, "KA", "Karnataka", "FY2026-27", "Q1", 20L, 20L, 18L, 17L, 14L, 12L, new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), 2) // Smallest sibling!
      );

      List<QprExportRow> safe = privacyService.applyPrivacyGuardrailsToQpr(states);
      // Goa primary suppressed
      assertThat(safe.get(1).traineeCount()).isNull();
      // Karnataka secondary suppressed
      assertThat(safe.get(2).traineeCount()).isNull();
      // Maharashtra retained
      assertThat(safe.get(0).traineeCount()).isEqualTo(100L);
    }
  }

  // =========================================================================
  // 6. CSV Injection (Formula Injection) Defense
  // =========================================================================

  @Nested
  @DisplayName("6. Spreadsheet Formula Injection Defense")
  class CsvInjectionTests {

    @Test
    @DisplayName("Formula Injection: =, +, -, @ triggers on string values are prefixed with single quote")
    void testFormulaInjectionTriggersPrefixed() {
      assertThat(SafeCsvStreamWriter.formatCell("=SUM(A1:A10)")).isEqualTo("\"'=SUM(A1:A10)\"");
      assertThat(SafeCsvStreamWriter.formatCell("+cmd|' /C calc'!A0")).isEqualTo("\"'+cmd|' /C calc'!A0\"");
      assertThat(SafeCsvStreamWriter.formatCell("-DUMMY_CODE")).isEqualTo("\"'-DUMMY_CODE\"");
      assertThat(SafeCsvStreamWriter.formatCell("@SUM(B1:B5)")).isEqualTo("\"'@SUM(B1:B5)\"");
    }

    @Test
    @DisplayName("Numeric Preservation: Valid negative/positive numbers and dates are not prefixed with quote")
    void testValidNumericsAndDatesPreserved() {
      assertThat(SafeCsvStreamWriter.formatCell(-15.5)).isEqualTo("-15.5");
      assertThat(SafeCsvStreamWriter.formatCell(new BigDecimal("25000.00"))).isEqualTo("25000.00");
      assertThat(SafeCsvStreamWriter.formatCell("-15.5")).isEqualTo("-15.5");
      assertThat(SafeCsvStreamWriter.formatCell("+100")).isEqualTo("+100");
      assertThat(SafeCsvStreamWriter.formatCell("2026-11-07")).isEqualTo("2026-11-07");
    }

    @Test
    @DisplayName("RFC 4180 Escaping: Embedded commas, quotes, and newlines are safely quoted")
    void testRfc4180Escaping() {
      assertThat(SafeCsvStreamWriter.formatCell("Pune, Maharashtra")).isEqualTo("\"Pune, Maharashtra\"");
      assertThat(SafeCsvStreamWriter.formatCell("Center \"Apex\"")).isEqualTo("\"Center \"\"Apex\"\"\"");
      assertThat(SafeCsvStreamWriter.formatCell("Line 1\nLine 2")).isEqualTo("\"Line 1\nLine 2\"");
    }
  }

  // =========================================================================
  // 7. Reporting Period & Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("7. Reporting Period Validation Tests")
  class ReportingPeriodValidationTests {

    @Test
    @DisplayName("Invalid Fiscal Year: 'FY2026-28' fails rollover validation and returns 400 Bad Request")
    void testInvalidFiscalYearRollover() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .param("fiscalYear", "FY2026-28")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_FISCAL_YEAR"));
    }

    @Test
    @DisplayName("Invalid Quarter: 'Q5' returns 400 Bad Request INVALID_QUARTER")
    void testInvalidQuarter() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .param("fiscalYear", "FY2026-27")
              .param("quarter", "Q5")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_QUARTER"));
    }
  }

  // =========================================================================
  // 8. Salary District Isolation Tests (Issue 1)
  // =========================================================================

  @Nested
  @DisplayName("8. Salary District Isolation & Scope Tests (Issue 1)")
  class SalaryDistrictIsolationTests {

    @Test
    @DisplayName("DSDP Salary Isolation: Course shared across two districts does not leak wages or satisfy k=5")
    void testCourseSalarySharedAcrossDistrictsIsolatedAndAnonymityEnforced() {
      Long courseId = 99890L;
      Long centerId = 99885L;
      Long programId = FIXTURE_PROGRAM_ID;
      Long sectorId = FIXTURE_SECTOR_ID;

      // Create Course X
      jdbcTemplate.update("""
          INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
          VALUES (?, 'CRS-SAL-99890', 'Shared Salary Course', ?, 1, 1, NOW(), NOW())
          """, courseId, sectorId);

      // Create Batch in Provider A
      jdbcTemplate.update("""
          INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
          VALUES (99890, 'BAT-SAL-99890', ?, ?, ?, ?, '2026-01-01', 50, 1, NOW(), NOW())
          """, courseId, FIXTURE_PROVIDER_ID, centerId, programId);

      // District A: 4 trainees (T99101..T99104) each with salary 15000.00
      for (long i = 1; i <= 4; i++) {
        long traineeId = 99100L + i;
        long enrollmentId = 99100L + i;
        long empId = 99100L + i;
        jdbcTemplate.update("""
            INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
            VALUES (?, ?, 'TraineeA', 'Test', 1, ?, ?, 1, 1, NOW(), NOW())
            """, traineeId, "REG-DA-" + traineeId, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
        jdbcTemplate.update("""
            INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 99890, '2026-04-15', 1, NOW(), NOW())
            """, enrollmentId, "ENR-DA-" + enrollmentId, traineeId, programId, courseId, FIXTURE_PROVIDER_ID, centerId);
        jdbcTemplate.update("""
            INSERT INTO employment_records (employment_id, employment_number, trainee_id, enrollment_id, employer_id, engagement_type_id, employment_spell_status_id, start_date, is_current, starting_salary, employment_info_source_id, record_verification_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 1, 1, '2026-05-01', 1, 15000.00, 1, 1, NOW(), NOW())
            """, empId, "EMP-DA-" + empId, traineeId, enrollmentId, FIXTURE_EMPLOYER_ID);
        jdbcTemplate.update("""
            INSERT INTO salary_history (salary_history_id, employment_id, trainee_id, salary_amount, salary_frequency_id, effective_from, employment_info_source_id, record_verification_status_id, created_at, updated_at)
            VALUES (?, ?, ?, 15000.00, 1, '2026-05-01', 1, 1, NOW(), NOW())
            """, empId, empId, traineeId);
      }

      // District B: 3 trainees (T99201..T99203) each with salary 25000.00
      for (long i = 1; i <= 3; i++) {
        long traineeId = 99200L + i;
        long enrollmentId = 99200L + i;
        long empId = 99200L + i;
        jdbcTemplate.update("""
            INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
            VALUES (?, ?, 'TraineeB', 'Test', 1, ?, ?, 1, 1, NOW(), NOW())
            """, traineeId, "REG-DB-" + traineeId, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);
        jdbcTemplate.update("""
            INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 99890, '2026-04-15', 1, NOW(), NOW())
            """, enrollmentId, "ENR-DB-" + enrollmentId, traineeId, programId, courseId, FIXTURE_PROVIDER_ID, centerId);
        jdbcTemplate.update("""
            INSERT INTO employment_records (employment_id, employment_number, trainee_id, enrollment_id, employer_id, engagement_type_id, employment_spell_status_id, start_date, is_current, starting_salary, employment_info_source_id, record_verification_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 1, 1, '2026-05-01', 1, 25000.00, 1, 1, NOW(), NOW())
            """, empId, "EMP-DB-" + empId, traineeId, enrollmentId, FIXTURE_EMPLOYER_ID);
        jdbcTemplate.update("""
            INSERT INTO salary_history (salary_history_id, employment_id, trainee_id, salary_amount, salary_frequency_id, effective_from, employment_info_source_id, record_verification_status_id, created_at, updated_at)
            VALUES (?, ?, ?, 25000.00, 1, '2026-05-01', 1, 1, NOW(), NOW())
            """, empId, empId, traineeId);
      }

      // Step 1: District A alone has 4 salary trainees (< 5).
      // Even though nationwide there are 4 + 3 = 7 trainees, District A MUST NOT satisfy k=5 using District B's trainees!
      DistrictMetadata metaA = repository.findDistrictMetadata(FIXTURE_DISTRICT_A_ID).orElseThrow();
      List<DsdpExportRow> rowsA = repository.fetchDsdpRows(FIXTURE_DISTRICT_A_ID, metaA, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRowA = rowsA.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();
      assertThat(courseRowA.traineeCount()).isEqualTo(4L);
      assertThat(courseRowA.avgStartingWage()).isNull();

      // District B alone has 3 salary trainees (< 5)
      DistrictMetadata metaB = repository.findDistrictMetadata(FIXTURE_DISTRICT_B_ID).orElseThrow();
      List<DsdpExportRow> rowsB = repository.fetchDsdpRows(FIXTURE_DISTRICT_B_ID, metaB, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRowB = rowsB.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();
      assertThat(courseRowB.traineeCount()).isEqualTo(3L);
      assertThat(courseRowB.avgStartingWage()).isNull();

      // Step 2: Add 5th trainee to District A with salary 15000.00
      long trainee5Id = 99105L;
      jdbcTemplate.update("""
          INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, 'REG-DA-99105', 'TraineeA5', 'Test', 1, ?, ?, 1, 1, NOW(), NOW())
          """, trainee5Id, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
      jdbcTemplate.update("""
          INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
          VALUES (99105, 'ENR-DA-99105', ?, ?, ?, ?, ?, 99890, '2026-04-15', 1, NOW(), NOW())
          """, trainee5Id, programId, courseId, FIXTURE_PROVIDER_ID, centerId);
      jdbcTemplate.update("""
          INSERT INTO employment_records (employment_id, employment_number, trainee_id, enrollment_id, employer_id, engagement_type_id, employment_spell_status_id, start_date, is_current, starting_salary, employment_info_source_id, record_verification_status_id, created_at, updated_at)
          VALUES (99105, 'EMP-DA-99105', ?, 99105, ?, 1, 1, '2026-05-01', 1, 15000.00, 1, 1, NOW(), NOW())
          """, trainee5Id, FIXTURE_EMPLOYER_ID);
      jdbcTemplate.update("""
          INSERT INTO salary_history (salary_history_id, employment_id, trainee_id, salary_amount, salary_frequency_id, effective_from, employment_info_source_id, record_verification_status_id, created_at, updated_at)
          VALUES (99105, 99105, ?, 15000.00, 1, '2026-05-01', 1, 1, NOW(), NOW())
          """, trainee5Id);

      // Now District A has 5 trainees: avgStartingWage must be released and equal 15000.00 (not mixed with District B)
      List<DsdpExportRow> rowsAUpdated = repository.fetchDsdpRows(FIXTURE_DISTRICT_A_ID, metaA, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRowAUpdated = rowsAUpdated.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();
      assertThat(courseRowAUpdated.traineeCount()).isEqualTo(5L);
      assertThat(courseRowAUpdated.avgStartingWage()).isEqualByComparingTo(new BigDecimal("15000.00"));

      // District B still only has 3 trainees: avgStartingWage MUST remain null
      List<DsdpExportRow> rowsBUpdated = repository.fetchDsdpRows(FIXTURE_DISTRICT_B_ID, metaB, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRowBUpdated = rowsBUpdated.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();
      assertThat(courseRowBUpdated.avgStartingWage()).isNull();
    }
  }

  // =========================================================================
  // 9. Person-Level Outcome Anonymity Tests (Issue 3)
  // =========================================================================

  @Nested
  @DisplayName("9. Person-Level Outcome Anonymity Tests (Issue 3)")
  class PersonLevelOutcomeAnonymityTests {

    @Test
    @DisplayName("Person-level outcome threshold: repeated records from fewer than 5 trainees cannot satisfy k=5")
    void testMultipleRecordsFromFewerThanFiveTraineesCannotSatisfyAnonymity() {
      Long courseId = 99891L;
      Long centerId = 99885L;
      Long programId = FIXTURE_PROGRAM_ID;
      Long sectorId = FIXTURE_SECTOR_ID;
      Long completedStatusId = 3L; // COMPLETED in ref_enrollment_status

      // Create Course Y
      jdbcTemplate.update("""
          INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
          VALUES (?, 'CRS-OUT-99891', 'Outcome Test Course', ?, 1, 1, NOW(), NOW())
          """, courseId, sectorId);

      // Create 8 batches
      for (long b = 1; b <= 8; b++) {
        jdbcTemplate.update("""
            INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW())
            """, 99890L + b, "BAT-OUT-" + (99890L + b), courseId, FIXTURE_PROVIDER_ID, centerId, programId);
      }

      // Seed 2 distinct trainees in District A
      long t1 = 99301L;
      long t2 = 99302L;
      jdbcTemplate.update("""
          INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, 'REG-T1-99301', 'Trainee1', 'Test', 1, ?, ?, 1, 1, NOW(), NOW()),
                 (?, 'REG-T2-99302', 'Trainee2', 'Test', 1, ?, ?, 1, 1, NOW(), NOW())
          """, t1, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID, t2, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

      // Trainee 1 has 3 completed enrollments
      jdbcTemplate.update("""
          INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
          VALUES (99311, 'ENR-99311', ?, ?, ?, ?, ?, 99891, '2026-04-01', ?, NOW(), NOW()),
                 (99312, 'ENR-99312', ?, ?, ?, ?, ?, 99892, '2026-04-02', ?, NOW(), NOW()),
                 (99313, 'ENR-99313', ?, ?, ?, ?, ?, 99893, '2026-04-03', ?, NOW(), NOW())
          """, t1, programId, courseId, FIXTURE_PROVIDER_ID, centerId, completedStatusId,
               t1, programId, courseId, FIXTURE_PROVIDER_ID, centerId, completedStatusId,
               t1, programId, courseId, FIXTURE_PROVIDER_ID, centerId, completedStatusId);

      // Trainee 2 has 2 completed enrollments
      jdbcTemplate.update("""
          INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
          VALUES (99314, 'ENR-99314', ?, ?, ?, ?, ?, 99894, '2026-04-04', ?, NOW(), NOW()),
                 (99315, 'ENR-99315', ?, ?, ?, ?, ?, 99895, '2026-04-05', ?, NOW(), NOW())
          """, t2, programId, courseId, FIXTURE_PROVIDER_ID, centerId, completedStatusId,
               t2, programId, courseId, FIXTURE_PROVIDER_ID, centerId, completedStatusId);

      // Query DSDP rows: raw completions = 5 (>= 5), but distinct completed trainees = 2 (< 5)!
      DistrictMetadata metaA = repository.findDistrictMetadata(FIXTURE_DISTRICT_A_ID).orElseThrow();
      List<DsdpExportRow> rows = repository.fetchDsdpRows(FIXTURE_DISTRICT_A_ID, metaA, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRow = rows.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();

      assertThat(courseRow.enrollmentCount()).isEqualTo(5L);
      assertThat(courseRow.traineeCount()).isEqualTo(2L);
      // Privacy check MUST use distinct trainees (2 < 5), so completedCount and rate MUST be null!
      assertThat(courseRow.completedCount()).isNull();
      assertThat(courseRow.completionRatePct()).isNull();

      // Now add 3 more distinct trainees with 1 completed enrollment each
      for (long i = 3; i <= 5; i++) {
        long tId = 99300L + i;
        long eId = 99310L + 3 + i; // 99316, 99317, 99318
        long bId = 99890L + 3 + i; // 99896, 99897, 99898
        jdbcTemplate.update("""
            INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
            VALUES (?, ?, 'Trainee', 'Test', 1, ?, ?, 1, 1, NOW(), NOW())
            """, tId, "REG-T-" + tId, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
        jdbcTemplate.update("""
            INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, '2026-04-10', ?, NOW(), NOW())
            """, eId, "ENR-" + eId, tId, programId, courseId, FIXTURE_PROVIDER_ID, centerId, bId, completedStatusId);
      }

      // Now distinct completed trainees = 5, total completions = 8
      List<DsdpExportRow> rowsUpdated = repository.fetchDsdpRows(FIXTURE_DISTRICT_A_ID, metaA, new AnalyticsFilterCriteria(), 100, 0);
      DsdpExportRow courseRowUpdated = rowsUpdated.stream().filter(r -> courseId.equals(r.courseId())).findFirst().orElseThrow();

      assertThat(courseRowUpdated.traineeCount()).isEqualTo(5L);
      assertThat(courseRowUpdated.completedCount()).isEqualTo(8L);
      assertThat(courseRowUpdated.completionRatePct()).isNotNull();
    }
  }

  // =========================================================================
  // 10. Preflight Permit & Concurrency Tests (Issue 4)
  // =========================================================================

  @Nested
  @DisplayName("10. Preflight Permit & Concurrency Tests (Issue 4)")
  class PreflightPermitAndConcurrencyTests {

    @Test
    @DisplayName("Preflight failure guarantees permit release: leaves 0 active permits")
    void testUnexpectedPreflightFailureLeavesZeroActivePermits() {
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();

      // Trigger preflight validation failure with non-existent district
      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
        exportService.exportDsdpCsv(-999L, new AnalyticsFilterCriteria(), userSuperAdminId);
      });

      // Verify acquired permit was deterministically released
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();

      // Trigger preflight failure with invalid fiscal year / quarter
      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
        exportService.exportQprCsv("FY2026-27", "INVALID_Q", new AnalyticsFilterCriteria(), userSuperAdminId);
      });

      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();

      // Calling release on a user who has no active permits must not decrement below 0 or corrupt state
      rateLimiter.release(userSuperAdminId);
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();
    }

    @Test
    @DisplayName("Global concurrency limit cannot be exceeded across concurrent requests from different users")
    void testConcurrentRequestsAcrossDifferentUsersCannotExceedGlobalLimit() throws Exception {
      rateLimiter.reset();
      int threadCount = 30;
      int globalLimit = exportProperties.getConcurrencyLimitGlobal();

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch readyLatch = new CountDownLatch(threadCount);
      CountDownLatch startLatch = new CountDownLatch(1);
      AtomicInteger acquiredCount = new AtomicInteger(0);
      AtomicInteger rejectedCount = new AtomicInteger(0);
      List<Long> acquiredUserIds = Collections.synchronizedList(new ArrayList<>());

      for (int i = 0; i < threadCount; i++) {
        final long testUserId = 96000L + i; // 30 distinct users
        executor.submit(() -> {
          readyLatch.countDown();
          try {
            startLatch.await();
            rateLimiter.checkAndAcquire(testUserId);
            acquiredCount.incrementAndGet();
            acquiredUserIds.add(testUserId);
          } catch (RateLimitExceededException e) {
            rejectedCount.incrementAndGet();
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
        });
      }

      readyLatch.await(5, TimeUnit.SECONDS);
      startLatch.countDown(); // Fire all 30 threads simultaneously
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      // Verify that active global permits exactly equal globalLimit and never exceed it
      assertThat(rateLimiter.getGlobalActiveExports()).isEqualTo(globalLimit);
      assertThat(acquiredCount.get()).isEqualTo(globalLimit);
      assertThat(rejectedCount.get()).isEqualTo(threadCount - globalLimit);

      // Cleanup: release acquired permits
      for (Long uid : acquiredUserIds) {
        rateLimiter.release(uid);
      }
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
    }

    @Test
    @DisplayName("Stream failure / client abort properly releases concurrency permits")
    void testStreamExecutionFailureReleasesPermit() {
      rateLimiter.reset();
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();

      StreamingResponseBody body = exportService.exportDsdpCsv(testDistrictId, new AnalyticsFilterCriteria(), userSuperAdminId);
      // Permit was acquired during preflight
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isEqualTo(1);
      assertThat(rateLimiter.getGlobalActiveExports()).isEqualTo(1);

      // Simulate client aborting connection: OutputStream throws IOException on write
      OutputStream failingStream = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          throw new IOException("Simulated client connection abort");
        }
      };

      org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> body.writeTo(failingStream));

      // After streaming failure, permit MUST be released
      assertThat(rateLimiter.getGlobalActiveExports()).isZero();
      assertThat(rateLimiter.getActiveExportsForUser(userSuperAdminId)).isZero();
    }
  }

  // =========================================================================
  // 11. QPR Anti-Reconstruction & Rate Suppression Tests (Issue 2)
  // =========================================================================

  @Nested
  @DisplayName("11. QPR Anti-Reconstruction & Rate Suppression Tests (Issue 2)")
  class QprAntiReconstructionTests {

    @Test
    @DisplayName("Multi-column complementary suppression: completedCount, certifiedCount, placedCount, employedCount protected")
    void testQprMultiColumnComplementarySuppression() {
      List<QprExportRow> rows = List.of(
          new QprExportRow(1L, "MH", "Maharashtra", "FY2026-27", "Q1", 100L, 100L, 90L, 85L, 70L, 65L,
              new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("70.00"), new BigDecimal("65.00"), 1),
          // Goa has completedCount = 3 (sparse 1..4), but other metrics are >= 5
          new QprExportRow(2L, "GA", "Goa", "FY2026-27", "Q1", 50L, 50L, 3L, 40L, 35L, 30L,
              new BigDecimal("6.00"), new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), 3),
          // Karnataka has completedCount = 20 (smallest sibling >= 5)
          new QprExportRow(3L, "KA", "Karnataka", "FY2026-27", "Q1", 50L, 50L, 20L, 40L, 35L, 30L,
              new BigDecimal("40.00"), new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), 2)
      );

      List<QprExportRow> safe = privacyService.applyPrivacyGuardrailsToQpr(rows);

      // Goa primary suppressed for completedCount and completionRatePct
      assertThat(safe.get(1).completedCount()).isNull();
      assertThat(safe.get(1).completionRatePct()).isNull();
      // Other safe counts in Goa retained
      assertThat(safe.get(1).certifiedCount()).isEqualTo(40L);

      // Karnataka secondary suppressed for completedCount and completionRatePct to prevent complementary reconstruction
      assertThat(safe.get(2).completedCount()).isNull();
      assertThat(safe.get(2).completionRatePct()).isNull();
      // Other counts in Karnataka retained
      assertThat(safe.get(2).certifiedCount()).isEqualTo(40L);

      // Maharashtra retained
      assertThat(safe.get(0).completedCount()).isEqualTo(90L);
      assertThat(safe.get(0).completionRatePct()).isEqualTo(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("Rate suppression: national summary rate percentage is suppressed when count is suppressed")
    void testNationalSummaryRateSuppressedWhenCountSuppressed() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/qpr/json")
              .param("fiscalYear", "FY2026-27")
              .param("quarter", "Q1")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String content = extractStreamingContent(result);
      JsonNode root = objectMapper.readTree(content);
      JsonNode summary = root.get("summary");
      if (summary != null && !summary.isNull()) {
        // For each pair, if count is null, rate MUST be null
        if (summary.get("totalCompleted") != null && summary.get("totalCompleted").isNull()) {
          assertThat(summary.get("nationalCompletionRatePct").isNull()).isTrue();
        }
        if (summary.get("totalCertified") != null && summary.get("totalCertified").isNull()) {
          assertThat(summary.get("nationalCertificationRatePct").isNull()).isTrue();
        }
        if (summary.get("totalJoinedPlacement") != null && summary.get("totalJoinedPlacement").isNull()) {
          assertThat(summary.get("nationalPlacementRatePct").isNull()).isTrue();
        }
        if (summary.get("totalEmployed") != null && summary.get("totalEmployed").isNull()) {
          assertThat(summary.get("nationalEmploymentRatePct").isNull()).isTrue();
        }
      }
    }
  }

  private void assertNoPii(String content) {
    var matcher = PII_PATTERN.matcher(content);
    if (matcher.find()) {
      org.junit.jupiter.api.Assertions.fail("Export payload contains sensitive direct PII pattern match: '" + matcher.group() + "'");
    }
  }
}
