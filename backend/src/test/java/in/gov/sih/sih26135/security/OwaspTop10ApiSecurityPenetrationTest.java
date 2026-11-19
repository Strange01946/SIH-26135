package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

/**
 * Phase 29.8 — OWASP API Security Top 10 Penetration Test Suite.
 *
 * <p>Executes real, evidence-based penetration tests against the running Spring Boot backend:
 * <ul>
 *   <li>API1:2023 Broken Object Level Authorization (BOLA/IDOR) across 9 isolated persona scopes</li>
 *   <li>API2:2023 Broken Authentication & JWT fuzzing (tampering, none-alg, expiration, missing claims)</li>
 *   <li>API3:2023 Broken Object Property Level Authorization (Mass Assignment & Excessive Data Exposure)</li>
 *   <li>API4:2023 Unrestricted Resource Consumption (Pagination bounds, oversized sizes, sensitive sort fields)</li>
 *   <li>API8:2023 Security Misconfiguration (Error response leakage scanning, security headers, actuator audit)</li>
 *   <li>API5, API6, API7, API9, API10 Applicability and function-level authorization penetration</li>
 * </ul>
 *
 * Runs strictly against {@code SIH26135_TEST}. Zero mutations to {@code SIH26135}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 29.8 — OWASP API Security Top 10 Penetration Tests")
public class OwaspTop10ApiSecurityPenetrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TestRestTemplate restTemplate;

  @Value("${security.jwt.secret:}")
  private String jwtSecret;

  @Autowired
  private jakarta.servlet.Filter springSecurityFilterChain;

  private MockMvc mockMvc;

  // Dedicated test fixture IDs in isolated 99710..99760 range
  private static final Long FIXTURE_STATE_A_ID = 99711L;
  private static final Long FIXTURE_STATE_B_ID = 99712L;
  private static final Long FIXTURE_DISTRICT_A_ID = 99713L; // in State A
  private static final Long FIXTURE_DISTRICT_B_ID = 99714L; // in State B

  private static final Long USER_SUPER_ADMIN_ID = 99721L;
  private static final Long USER_NATIONAL_ADMIN_ID = 99722L;
  private static final Long USER_STATE_A_ADMIN_ID = 99723L;
  private static final Long USER_STATE_B_ADMIN_ID = 99724L;
  private static final Long USER_PROVIDER_A_ID = 99725L;
  private static final Long USER_PROVIDER_B_ID = 99726L;
  private static final Long USER_EMPLOYER_A_ID = 99727L;
  private static final Long USER_EMPLOYER_B_ID = 99728L;
  private static final Long USER_TRAINEE_A_ID = 99729L;
  private static final Long USER_TRAINEE_B_ID = 99730L;

  private static final Long FIXTURE_PROVIDER_A_ID = 99731L;
  private static final Long FIXTURE_PROVIDER_B_ID = 99732L;
  private static final Long FIXTURE_EMPLOYER_A_ID = 99733L;
  private static final Long FIXTURE_EMPLOYER_B_ID = 99734L;
  private static final Long FIXTURE_TRAINEE_A_ID = 99735L;
  private static final Long FIXTURE_TRAINEE_B_ID = 99736L;
  private static final Long FIXTURE_BATCH_A_ID = 99737L;
  private static final Long FIXTURE_BATCH_B_ID = 99738L;
  private static final Long FIXTURE_ENROLLMENT_A_ID = 99739L;
  private static final Long FIXTURE_ENROLLMENT_B_ID = 99740L;
  private static final Long FIXTURE_CENTER_A_ID = 99741L;
  private static final Long FIXTURE_CENTER_B_ID = 99742L;
  private static final Long FIXTURE_COURSE_ID = 99743L;

  // Persona JWT bearer tokens
  private String tokenSuperAdmin;
  private String tokenNationalAdmin;
  private String tokenStateAAdmin;
  private String tokenStateBAdmin;
  private String tokenProviderA;
  private String tokenProviderB;
  private String tokenEmployerA;
  private String tokenEmployerB;
  private String tokenTraineeA;
  private String tokenTraineeB;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
        .webAppContextSetup(context)
        .addFilters(springSecurityFilterChain)
        .build();

    cleanUpFixtures();
    seedTestFixtures();
    generatePersonaTokens();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpFixtures();
  }

  private void cleanUpFixtures() {
    try {
      SecurityContextHolder.clearContext();
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id IN (?, ?)",
          FIXTURE_ENROLLMENT_A_ID, FIXTURE_ENROLLMENT_B_ID);
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?)",
          FIXTURE_BATCH_A_ID, FIXTURE_BATCH_B_ID);
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (?, ?)",
          FIXTURE_CENTER_A_ID, FIXTURE_CENTER_B_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id IN (?, ?)",
          FIXTURE_TRAINEE_A_ID, FIXTURE_TRAINEE_B_ID);
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          USER_SUPER_ADMIN_ID, USER_NATIONAL_ADMIN_ID, USER_STATE_A_ADMIN_ID, USER_STATE_B_ADMIN_ID,
          USER_PROVIDER_A_ID, USER_PROVIDER_B_ID, USER_EMPLOYER_A_ID, USER_EMPLOYER_B_ID,
          USER_TRAINEE_A_ID, USER_TRAINEE_B_ID);
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = 99743");
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = 99743");
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)",
          FIXTURE_PROVIDER_A_ID, FIXTURE_PROVIDER_B_ID);
      jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (?, ?)",
          FIXTURE_EMPLOYER_A_ID, FIXTURE_EMPLOYER_B_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?)",
          FIXTURE_DISTRICT_A_ID, FIXTURE_DISTRICT_B_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)",
          FIXTURE_STATE_A_ID, FIXTURE_STATE_B_ID);
    } catch (Exception e) {
      // In cleanUp, swallow foreign key or missing table errors so tests proceed cleanly
    }
  }

  private void seedTestFixtures() {
    // 1. Geographic reference records
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'S1', 'Phase29 State A', 'LG99711', 1, NOW(), NOW())
        """, FIXTURE_STATE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'S2', 'Phase29 State B', 'LG99712', 1, NOW(), NOW())
        """, FIXTURE_STATE_B_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'D1', 'Phase29 District A', 'LGD99713', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_A_ID, FIXTURE_STATE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'D2', 'Phase29 District B', 'LGD99714', 1, NOW(), NOW())
        """, FIXTURE_DISTRICT_B_ID, FIXTURE_STATE_B_ID);

    // 2. Training Providers A and B
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-P29-A', 'Provider A Pentest', 'REG-PRV-99731', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_PROVIDER_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-P29-B', 'Provider B Pentest', 'REG-PRV-99732', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_PROVIDER_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    // 3. Employers A and B
    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-P29-A', 'Employer A Pentest', 'REG-EMP-99733', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_EMPLOYER_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);
    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-P29-B', 'Employer B Pentest', 'REG-EMP-99734', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_EMPLOYER_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    // 4. Users for all 9 personas
    // Super Admin
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p29_superadmin', 'p29_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_SUPER_ADMIN_ID);

    // National Admin (no state or tenant restriction)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p29_natadmin', 'p29_natadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_NATIONAL_ADMIN_ID);

    // State A Admin
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p29_state_a_admin', 'p29_state_a@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_A_ADMIN_ID, FIXTURE_STATE_A_ID);

    // State B Admin
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p29_state_b_admin', 'p29_state_b@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_STATE_B_ADMIN_ID, FIXTURE_STATE_B_ID);

    // Provider A User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'p29_provider_a', 'p29_provider_a@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_A_ID, FIXTURE_PROVIDER_A_ID);

    // Provider B User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'p29_provider_b', 'p29_provider_b@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_PROVIDER_B_ID, FIXTURE_PROVIDER_B_ID);

    // Employer A User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'p29_employer_a', 'p29_employer_a@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_A_ID, FIXTURE_EMPLOYER_A_ID);

    // Employer B User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (?, 'p29_employer_b', 'p29_employer_b@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, USER_EMPLOYER_B_ID, FIXTURE_EMPLOYER_B_ID);

    // Trainee A User & Profile
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p29_trainee_a', 'p29_trainee_a@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_A_ID);
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-P29-99735', 'Trainee', 'A', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_TRAINEE_A_ID, USER_TRAINEE_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    // Trainee B User & Profile
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p29_trainee_b', 'p29_trainee_b@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, USER_TRAINEE_B_ID);
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-P29-99736', 'Trainee', 'B', 1, ?, ?, 1, 1, NOW(), NOW())
        """, FIXTURE_TRAINEE_B_ID, USER_TRAINEE_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    // 5. Dynamic resolution or fallback seed for course, center, and program
    Long refCourseId = null;
    try {
      refCourseId = jdbcTemplate.queryForObject("SELECT course_id FROM courses ORDER BY course_id LIMIT 1", Long.class);
    } catch (Exception ignored) {
    }
    if (refCourseId == null) {
      jdbcTemplate.update("""
          INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
          VALUES (99741, 'SEC-P29-99741', 'Sector P29', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE sector_name = sector_name
          """);
      jdbcTemplate.update("""
          INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
          VALUES (99741, 'CRS-P29-99741', 'Course P29', 99741, 1, 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE course_name = course_name
          """);
      refCourseId = 99741L;
    }

    // 5. Training Centers A and B
    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'CTR-P29-99741', 'Center P29 A', ?, ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE center_name = center_name
        """, FIXTURE_CENTER_A_ID, FIXTURE_PROVIDER_A_ID, FIXTURE_STATE_A_ID, FIXTURE_DISTRICT_A_ID);

    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'CTR-P29-99742', 'Center P29 B', ?, ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE center_name = center_name
        """, FIXTURE_CENTER_B_ID, FIXTURE_PROVIDER_B_ID, FIXTURE_STATE_B_ID, FIXTURE_DISTRICT_B_ID);

    Long refProgramId = null;
    try {
      refProgramId = jdbcTemplate.queryForObject("SELECT program_id FROM programs ORDER BY program_id LIMIT 1", Long.class);
    } catch (Exception ignored) {
    }
    if (refProgramId == null) {
      refProgramId = 1L;
    }

    // 6. Shared Batches A and B, Enrollments A and B
    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'BAT-P29-A', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)
        """, FIXTURE_BATCH_A_ID, refCourseId, FIXTURE_PROVIDER_A_ID, FIXTURE_CENTER_A_ID, refProgramId);

    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'BAT-P29-B', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)
        """, FIXTURE_BATCH_B_ID, refCourseId, FIXTURE_PROVIDER_B_ID, FIXTURE_CENTER_B_ID, refProgramId);

    jdbcTemplate.update("""
        INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'ENR-P29-A', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)
        """, FIXTURE_ENROLLMENT_A_ID, FIXTURE_TRAINEE_A_ID, refProgramId, refCourseId, FIXTURE_PROVIDER_A_ID, FIXTURE_CENTER_A_ID, FIXTURE_BATCH_A_ID);

    jdbcTemplate.update("""
        INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'ENR-P29-B', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)
        """, FIXTURE_ENROLLMENT_B_ID, FIXTURE_TRAINEE_B_ID, refProgramId, refCourseId, FIXTURE_PROVIDER_B_ID, FIXTURE_CENTER_B_ID, FIXTURE_BATCH_B_ID);
  }

  private void generatePersonaTokens() {
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "p29_superadmin",
        List.of("ROLE_super_admin", "admin.nationwide", "trainee.read", "trainee.write", "batch.read", "batch.write", "provider.read", "employer.read", "analytics.read", "audit.read", "system.event.read", "system.event.manage", "course.manage", "user.manage"));

    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_NATIONAL_ADMIN_ID, "p29_natadmin",
        List.of("ROLE_government_admin", "admin.nationwide", "trainee.read", "batch.read", "provider.read", "employer.read", "analytics.read", "course.manage", "user.manage"));

    tokenStateAAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ADMIN_ID, "p29_state_a_admin",
        List.of("ROLE_state_admin", "ROLE_government_admin", "analytics.read", "trainee.read", "batch.read", "provider.read", "course.manage"));

    tokenStateBAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ADMIN_ID, "p29_state_b_admin",
        List.of("ROLE_state_admin", "ROLE_government_admin", "analytics.read", "trainee.read", "batch.read", "provider.read", "course.manage"));

    tokenProviderA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A_ID, "p29_provider_a",
        List.of("ROLE_training_provider", "batch.read", "batch.write", "trainee.read", "provider.read", "course.manage", "center.manage", "analytics.export"));

    tokenProviderB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_B_ID, "p29_provider_b",
        List.of("ROLE_training_provider", "batch.read", "batch.write", "trainee.read", "provider.read", "course.manage", "center.manage", "analytics.export"));

    tokenEmployerA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_A_ID, "p29_employer_a",
        List.of("ROLE_employer", "employer.read", "placement.read", "placement.write"));

    tokenEmployerB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_B_ID, "p29_employer_b",
        List.of("ROLE_employer", "employer.read", "placement.read", "placement.write"));

    tokenTraineeA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_A_ID, "p29_trainee_a",
        List.of("ROLE_trainee", "trainee.read", "trainee.write"));

    tokenTraineeB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_B_ID, "p29_trainee_b",
        List.of("ROLE_trainee", "trainee.read", "trainee.write"));
  }

  // =========================================================================
  // 1. API1: Broken Object Level Authorization (BOLA / IDOR)
  // =========================================================================

  @Nested
  @DisplayName("API1:2023 — Broken Object Level Authorization (BOLA/IDOR)")
  class Api1BolaIdorPenetrationTests {

    @Test
    @DisplayName("BOLA 01: Provider A cannot access Provider B's batch")
    void testProviderCannotAccessOtherProviderBatch() throws Exception {
      // 1. Provider A accesses own batch -> Success (200 OK)
      mockMvc.perform(get("/api/v1/training-batches/" + FIXTURE_BATCH_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isOk());

      // 2. Provider A attempts to access Provider B's batch -> Forbidden (403) or Safe Not Found (404)
      MvcResult result = mockMvc.perform(get("/api/v1/training-batches/" + FIXTURE_BATCH_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andReturn();

      int status = result.getResponse().getStatus();
      assertThat(status).isIn(HttpStatus.FORBIDDEN.value(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("BOLA 02: Provider A cannot modify Provider B's batch")
    void testProviderCannotModifyOtherProviderBatch() throws Exception {
      String updatePayload = """
          {
            "batchCode": "BAT-TAMPERED-BY-A",
            "startDate": "2026-02-01",
            "endDate": "2026-05-31"
          }
          """;

      mockMvc.perform(put("/api/v1/training-batches/" + FIXTURE_BATCH_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(updatePayload))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

      // Verify DB state was NOT modified
      String batchCode = jdbcTemplate.queryForObject(
          "SELECT batch_code FROM training_batches WHERE batch_id = ?",
          String.class, FIXTURE_BATCH_B_ID);
      assertThat(batchCode).isEqualTo("BAT-P29-B");
    }

    @Test
    @DisplayName("BOLA 03: Provider A cannot delete Provider B's batch")
    void testProviderCannotDeleteOtherProviderBatch() throws Exception {
      mockMvc.perform(delete("/api/v1/training-batches/" + FIXTURE_BATCH_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

      // Verify DB record still exists
      Integer count = jdbcTemplate.queryForObject(
          "SELECT count(*) FROM training_batches WHERE batch_id = ?",
          Integer.class, FIXTURE_BATCH_B_ID);
      assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("BOLA 04: Trainee A cannot access Trainee B's profile")
    void testTraineeCannotAccessOtherTraineeProfile() throws Exception {
      // Trainee A accessing Trainee B profile -> Scope mismatch 400 Bad Request, 403 Forbidden, or 404
      mockMvc.perform(get("/api/v1/trainees/" + FIXTURE_TRAINEE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403, 404)));
    }

    @Test
    @DisplayName("BOLA 05: State Admin A cannot access State B geographic dashboard")
    void testStateAdminCannotAccessOtherStateDashboard() throws Exception {
      // State Admin A accessing State B dashboard -> 403 Forbidden
      mockMvc.perform(get("/api/v1/dashboards/states/" + FIXTURE_STATE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA 06: State Admin A cannot access District B dashboard")
    void testStateAdminCannotAccessOtherDistrictDashboard() throws Exception {
      // State Admin A accessing District B (which belongs to State B) -> 403 Forbidden
      mockMvc.perform(get("/api/v1/dashboards/districts/" + FIXTURE_DISTRICT_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenStateAAdmin))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA 07: Non-administrative persona cannot access national executive dashboard")
    void testNonAdminCannotAccessNationalDashboard() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA 08: National Admin has nationwide cross-state and cross-provider access")
    void testNationalAdminHasNationwideAccess() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/training-batches/" + FIXTURE_BATCH_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("BOLA 09: Trainee A cannot mutate Trainee B profile (PUT /api/v1/trainees/{id})")
    void testTraineeCannotMutateOtherTraineeProfile() throws Exception {
      String updatePayload = """
          {
            "firstName": "HackedByTraineeA",
            "lastName": "Compromised"
          }
          """;

      mockMvc.perform(put("/api/v1/trainees/" + FIXTURE_TRAINEE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(updatePayload))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403)))
          .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.isOneOf("TRAINEE_MISMATCH", "ACCESS_DENIED")));

      // Verify DB record of Trainee B is unchanged
      String firstName = jdbcTemplate.queryForObject(
          "SELECT first_name FROM trainees WHERE trainee_id = ?",
          String.class, FIXTURE_TRAINEE_B_ID);
      assertThat(firstName).isEqualTo("Trainee");
    }

    @Test
    @DisplayName("BOLA 10: Trainee A cannot delete Trainee B profile (DELETE /api/v1/trainees/{id})")
    void testTraineeCannotDeleteOtherTraineeProfile() throws Exception {
      mockMvc.perform(delete("/api/v1/trainees/" + FIXTURE_TRAINEE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403)))
          .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.isOneOf("TRAINEE_MISMATCH", "ACCESS_DENIED")));

      // Verify DB record of Trainee B is not deleted
      String deletedAt = jdbcTemplate.queryForObject(
          "SELECT IFNULL(deleted_at, 'NOT_DELETED') FROM trainees WHERE trainee_id = ?",
          String.class, FIXTURE_TRAINEE_B_ID);
      assertThat(deletedAt).isEqualTo("NOT_DELETED");
    }

    @Test
    @DisplayName("BOLA 11: Trainee A cannot enumerate Trainee B via filtered query params")
    void testTraineeCannotEnumerateOtherTraineeViaFilter() throws Exception {
      // 1. Filter by foreign user_id
      mockMvc.perform(get("/api/v1/trainees?userId=" + USER_TRAINEE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403)))
          .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.isOneOf("TRAINEE_MISMATCH", "ACCESS_DENIED")));

      // 2. Filter by foreign registration_number
      mockMvc.perform(get("/api/v1/trainees?registrationNumber=REG-P29-99736")
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403)))
          .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.isOneOf("TRAINEE_MISMATCH", "ACCESS_DENIED")));
    }

    @Test
    @DisplayName("BOLA 12: Provider A cannot access Provider B's training center (GET /api/v1/training-centers/{id})")
    void testProviderCannotAccessOtherProviderCenter() throws Exception {
      // Provider A accesses own center -> 200 OK
      mockMvc.perform(get("/api/v1/training-centers/" + FIXTURE_CENTER_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isOk());

      // Provider A accesses Provider B's center -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/" + FIXTURE_CENTER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("BOLA 13: Provider A cannot modify Provider B's training center (PUT /api/v1/training-centers/{id})")
    void testProviderCannotModifyOtherProviderCenter() throws Exception {
      String updatePayload = """
          {
            "centerName": "Center Tampered By Provider A",
            "operationalCapacity": 100
          }
          """;

      mockMvc.perform(put("/api/v1/training-centers/" + FIXTURE_CENTER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(updatePayload))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

      String centerName = jdbcTemplate.queryForObject(
          "SELECT center_name FROM training_centers WHERE center_id = ?",
          String.class, FIXTURE_CENTER_B_ID);
      assertThat(centerName).isEqualTo("Center P29 B");
    }

    @Test
    @DisplayName("BOLA 14: Provider A cannot delete Provider B's training center (DELETE /api/v1/training-centers/{id})")
    void testProviderCannotDeleteOtherProviderCenter() throws Exception {
      mockMvc.perform(delete("/api/v1/training-centers/" + FIXTURE_CENTER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

      Integer count = jdbcTemplate.queryForObject(
          "SELECT count(*) FROM training_centers WHERE center_id = ?",
          Integer.class, FIXTURE_CENTER_B_ID);
      assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("BOLA 15: Provider A cannot mutate status of Provider B's batch (PUT /api/v1/training-batches/{id}/status)")
    void testProviderCannotMutateBatchStatusOfOtherProvider() throws Exception {
      String statusPayload = """
          {
            "batchStatusId": 2
          }
          """;

      mockMvc.perform(put("/api/v1/training-batches/" + FIXTURE_BATCH_B_ID + "/status")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(statusPayload))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

      Long statusId = jdbcTemplate.queryForObject(
          "SELECT batch_status_id FROM training_batches WHERE batch_id = ?",
          Long.class, FIXTURE_BATCH_B_ID);
      assertThat(statusId).isEqualTo(1L);
    }

    @Test
    @DisplayName("BOLA 16: Provider A cannot export Provider B's scorecard (/api/v1/analytics/export/provider-scorecard/csv)")
    void testProviderCannotExportOtherProviderScorecard() throws Exception {
      mockMvc.perform(get("/api/v1/analytics/export/provider-scorecard/csv?providerId=" + FIXTURE_PROVIDER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA 17: Trainee A cannot access Trainee B's consent records (GET /api/v1/consents?traineeId={id})")
    void testTraineeCannotAccessOtherTraineeConsents() throws Exception {
      mockMvc.perform(get("/api/v1/consents?traineeId=" + FIXTURE_TRAINEE_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BOLA 18: Cross-employer access boundary is enforced (PUT /api/v1/employers/{id})")
    void testCrossEmployerBoundaryEnforced() throws Exception {
      String employerPayload = """
          {
            "employerName": "Tampered Employer Name",
            "contactPersonName": "Malicious Actor"
          }
          """;

      mockMvc.perform(put("/api/v1/employers/" + FIXTURE_EMPLOYER_B_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(employerPayload))
          .andExpect(status().isForbidden());

      String empName = jdbcTemplate.queryForObject(
          "SELECT employer_name FROM employers WHERE employer_id = ?",
          String.class, FIXTURE_EMPLOYER_B_ID);
      assertThat(empName).isEqualTo("Employer B Pentest");
    }
  }

  // =========================================================================
  // 2. API2: Broken Authentication & JWT Fuzzing
  // =========================================================================

  @Nested
  @DisplayName("API2:2023 — Broken Authentication & JWT Fuzzing")
  class Api2BrokenAuthenticationTests {

    @Test
    @DisplayName("AUTH 01: Missing Authorization header is rejected with HTTP 401")
    void testMissingAuthorizationHeader() throws Exception {
      mockMvc.perform(get("/api/v1/auth/me"))
          .andExpect(status().isUnauthorized());

      mockMvc.perform(get("/api/v1/trainees/" + FIXTURE_TRAINEE_A_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 02: Empty Bearer token is rejected with HTTP 401")
    void testEmptyBearerToken() throws Exception {
      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer "))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 03: Whitespace-only Bearer token is rejected with HTTP 401")
    void testWhitespaceBearerToken() throws Exception {
      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer    "))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 04: Random malformed string is rejected with HTTP 401")
    void testRandomMalformedToken() throws Exception {
      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt.token.string"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 05: Expired JWT is rejected with HTTP 401")
    void testExpiredJwtRejected() throws Exception {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
      Instant now = Instant.now();
      String expiredToken = Jwts.builder()
          .id(UUID.randomUUID().toString())
          .issuer("in.gov.sih.sih26135")
          .subject(String.valueOf(USER_SUPER_ADMIN_ID))
          .issuedAt(Date.from(now.minusSeconds(3600)))
          .expiration(Date.from(now.minusSeconds(1800)))
          .claim(JwtTokenProvider.CLAIM_USER_ID, USER_SUPER_ADMIN_ID)
          .claim(JwtTokenProvider.CLAIM_USERNAME, "p29_superadmin")
          .claim(JwtTokenProvider.CLAIM_TOKEN_TYPE, JwtTokenProvider.TOKEN_TYPE_ACCESS)
          .claim(JwtTokenProvider.CLAIM_AUTHORITIES, List.of("ROLE_super_admin"))
          .signWith(key, Jwts.SIG.HS256)
          .compact();

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 06: Tampered JWT payload is rejected with HTTP 401")
    void testTamperedPayloadRejected() throws Exception {
      // Split valid token into header.payload.signature and modify payload characters
      String validToken = tokenSuperAdmin.substring("Bearer ".length());
      String[] parts = validToken.split("\\.");
      assertThat(parts).hasSize(3);

      // Tamper payload byte
      String tamperedPayload = parts[1].substring(0, parts[1].length() - 2) + "==";
      String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 07: Modified signature is rejected with HTTP 401")
    void testModifiedSignatureRejected() throws Exception {
      String validToken = tokenSuperAdmin.substring("Bearer ".length());
      String[] parts = validToken.split("\\.");
      String forgedSig = parts[2].substring(0, parts[2].length() - 4) + "AAAA";
      String forgedToken = parts[0] + "." + parts[1] + "." + forgedSig;

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 08: Algorithm none JWT is rejected with HTTP 401")
    void testAlgorithmNoneRejected() throws Exception {
      // Header: {"alg":"none","typ":"JWT"} in base64url: eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0
      // Payload: valid claims without signature
      String headerNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0";
      String payload = "eyJzdWIiOiIxIiwidWlkIjoxLCJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiaXNzIjoiaW4uZ292LnNpaC5zaWgyNjEzNSIsImF1dGhvcml0aWVzIjpbIlJPTEVfc3VwZXJfYWRtaW4iXX0";
      String algNoneToken = headerNone + "." + payload + ".";

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + algNoneToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 09: Forged JWT signed with different key is rejected with HTTP 401")
    void testForgedKeyRejected() throws Exception {
      SecretKey attackerKey = Keys.hmacShaKeyFor("attacker_bogus_secret_key_32_bytes_long!".getBytes(StandardCharsets.UTF_8));
      Instant now = Instant.now();
      String forgedToken = Jwts.builder()
          .id(UUID.randomUUID().toString())
          .issuer("in.gov.sih.sih26135")
          .subject("1")
          .issuedAt(Date.from(now))
          .expiration(Date.from(now.plusSeconds(3600)))
          .claim(JwtTokenProvider.CLAIM_USER_ID, 1L)
          .claim(JwtTokenProvider.CLAIM_USERNAME, "admin")
          .claim(JwtTokenProvider.CLAIM_TOKEN_TYPE, JwtTokenProvider.TOKEN_TYPE_ACCESS)
          .claim(JwtTokenProvider.CLAIM_AUTHORITIES, List.of("ROLE_super_admin"))
          .signWith(attackerKey, Jwts.SIG.HS256)
          .compact();

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 10: Refresh token presented on protected business endpoint is rejected with HTTP 401")
    void testRefreshTokenRejectedOnProtectedApi() throws Exception {
      String refreshToken = jwtTokenProvider.generateRefreshToken(USER_SUPER_ADMIN_ID);

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
          .andExpect(status().isUnauthorized());

      mockMvc.perform(get("/api/v1/trainees/" + FIXTURE_TRAINEE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 11: JWT with invalid issuer is rejected with HTTP 401")
    void testInvalidIssuerRejected() throws Exception {
      SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
      Instant now = Instant.now();
      String invalidIssuerToken = Jwts.builder()
          .id(UUID.randomUUID().toString())
          .issuer("evil.attacker.org")
          .subject(String.valueOf(USER_SUPER_ADMIN_ID))
          .issuedAt(Date.from(now))
          .expiration(Date.from(now.plusSeconds(3600)))
          .claim(JwtTokenProvider.CLAIM_USER_ID, USER_SUPER_ADMIN_ID)
          .claim(JwtTokenProvider.CLAIM_USERNAME, "p29_superadmin")
          .claim(JwtTokenProvider.CLAIM_TOKEN_TYPE, JwtTokenProvider.TOKEN_TYPE_ACCESS)
          .claim(JwtTokenProvider.CLAIM_AUTHORITIES, List.of("ROLE_super_admin"))
          .signWith(key, Jwts.SIG.HS256)
          .compact();

      mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidIssuerToken))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH 12: Authentication failure responses never leak stack traces or internal exception classes")
    void testAuthFailureResponseSanitized() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
          .andExpect(status().isUnauthorized())
          .andReturn();

      String body = result.getResponse().getContentAsString();
      assertThat(body).doesNotContain("Exception");
      assertThat(body).doesNotContain("in.gov.sih");
      assertThat(body).doesNotContain("org.springframework");
      assertThat(body).doesNotContain("JwtValidationException");
    }
  }

  // =========================================================================
  // 3. API3: Broken Object Property Level Authorization (Mass Assignment & Excessive Exposure)
  // =========================================================================

  @Nested
  @DisplayName("API3:2023 — Broken Object Property Level Authorization")
  class Api3BrokenObjectPropertyLevelAuthTests {

    @Test
    @DisplayName("BOPLA 01: Mass assignment fields injected into user update are ignored/rejected")
    void testMassAssignmentOnUserUpdate() throws Exception {
      String massAssignmentPayload = """
          {
            "username": "p29_trainee_a",
            "email": "p29_trainee_a_updated@test.gov.in",
            "isAdmin": true,
            "role": "ROLE_super_admin",
            "authorities": ["ROLE_super_admin", "system.manage"],
            "userStatusId": 999999,
            "verified": true,
            "tenantId": 999999,
            "createdBy": 1
          }
          """;

      // Trainee attempts to update own profile with escalated fields
      mockMvc.perform(put("/api/v1/users/" + USER_TRAINEE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(massAssignmentPayload))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 403)));

      // Verify DB: User was NOT promoted to admin, user_status_id was not set to 999999
      Map<String, Object> userRecord = jdbcTemplate.queryForMap(
          "SELECT user_id, user_status_id FROM users WHERE user_id = ?",
          USER_TRAINEE_A_ID);
      assertThat(userRecord.get("user_status_id")).isNotEqualTo(999999L);

      // Verify no super_admin role was added to user_roles
      Integer roleCount = jdbcTemplate.queryForObject(
          "SELECT count(*) FROM user_roles ur JOIN roles r ON ur.role_id = r.role_id WHERE ur.user_id = ? AND r.role_code = 'ROLE_super_admin'",
          Integer.class, USER_TRAINEE_A_ID);
      assertThat(roleCount).isEqualTo(0);
    }

    @Test
    @DisplayName("BOPLA 02: Excessive data exposure audit on /api/v1/auth/me")
    void testExcessiveDataExposureOnAuthMe() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String body = result.getResponse().getContentAsString();
      // Must not contain password hashes, reset tokens, or secrets
      assertThat(body).doesNotContain("passwordHash");
      assertThat(body).doesNotContain("password_hash");
      assertThat(body).doesNotContain("$argon2");
      assertThat(body).doesNotContain("$2a$");
      assertThat(body).doesNotContain("jwtSecret");
      assertThat(body).doesNotContain("secretKey");
    }

    @Test
    @DisplayName("BOPLA 03: Excessive data exposure audit on /api/v1/users/{id}")
    void testExcessiveDataExposureOnUserGet() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/users/" + USER_SUPER_ADMIN_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String body = result.getResponse().getContentAsString();
      assertThat(body).doesNotContain("passwordHash");
      assertThat(body).doesNotContain("password_hash");
      assertThat(body).doesNotContain("$argon2");
      assertThat(body).doesNotContain("$2a$");
      assertThat(body).doesNotContain("resetToken");
    }
  }

  // =========================================================================
  // 4. API4: Unrestricted Resource Consumption (Pagination & Resource Bounds)
  // =========================================================================

  @Nested
  @DisplayName("API4:2023 — Unrestricted Resource Consumption")
  class Api4UnrestrictedResourceConsumptionTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/trainees?page=-1&size=20",
        "/api/v1/trainees?page=0&size=0",
        "/api/v1/trainees?page=0&size=-1",
        "/api/v1/trainees?page=0&size=101",
        "/api/v1/trainees?page=0&size=10000"
    })
    @DisplayName("RES 01: Out of bounds pagination parameters are rejected with HTTP 400")
    void testPaginationBoundsRejection(String url) throws Exception {
      mockMvc.perform(get(url)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value(containsString("INVALID_PAGE")));
    }

    @Test
    @DisplayName("RES 02: Sensitive sort field is rejected with HTTP 400")
    void testSensitiveSortFieldRejection() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?page=0&size=20&sort=password&direction=ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));

      mockMvc.perform(get("/api/v1/trainees?page=0&size=20&sort=passwordhash&direction=ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));

      mockMvc.perform(get("/api/v1/trainees?page=0&size=20&sort=aadhaar&direction=ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("RES 03: Unsupported arbitrary sort field is rejected with HTTP 400")
    void testUnsupportedSortFieldRejection() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?page=0&size=20&sort=evil_nonexistent_column&direction=ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("RES 04: Valid pagination within bounds (0..100) succeeds")
    void testValidPaginationSucceeds() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?page=0&size=20&sort=id&direction=ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk());
    }
  }

  // =========================================================================
  // 5. API8: Security Misconfiguration (Error Leakage & Security Headers)
  // =========================================================================

  @Nested
  @DisplayName("API8:2023 — Security Misconfiguration")
  class Api8SecurityMisconfigurationTests {

    @Test
    @DisplayName("MISC 01: Mandatory production HTTP security response headers are present")
    void testProductionSecurityHeadersPresent() throws Exception {
      mockMvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(header().string("X-Content-Type-Options", "nosniff"))
          .andExpect(header().string("X-Frame-Options", "DENY"))
          .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
          .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("MISC 02: Actuator exposure is restricted to health only with details hidden")
    void testActuatorExposureRestricted() throws Exception {
      // /actuator/health succeeds without details
      mockMvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"))
          .andExpect(jsonPath("$.components").doesNotExist());

      // Sensitive actuators must be inaccessible / unmapped
      mockMvc.perform(get("/actuator/env")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(401, 403, 404)));

      mockMvc.perform(get("/actuator/beans")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(401, 403, 404)));
    }

    @Test
    @DisplayName("MISC 03: Error responses do not leak stack traces, SQL, package names or file paths")
    void testErrorResponseSanitization() throws Exception {
      // Trigger 400 Bad Request with malformed JSON
      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"username\": \"test\", malformed"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String body = result.getResponse().getContentAsString();
      assertThat(body).doesNotContain("at in.gov.sih");
      assertThat(body).doesNotContain("org.springframework");
      assertThat(body).doesNotContain("Caused by:");
      assertThat(body).doesNotContain("JsonParseException");
      assertThat(body).doesNotContain("E:\\");
      assertThat(body).doesNotContain("C:\\");
    }
  }

  // =========================================================================
  // 6. API5: Broken Function Level Authorization (BFLA)
  // =========================================================================

  @Nested
  @DisplayName("API5:2023 — Broken Function Level Authorization (BFLA)")
  class Api5BrokenFunctionLevelAuthTests {

    @Test
    @DisplayName("BFLA 01: Trainee cannot invoke administrative system event log endpoints")
    void testTraineeCannotInvokeSystemEventLogs() throws Exception {
      mockMvc.perform(get("/api/v1/system-event-logs")
              .header(HttpHeaders.AUTHORIZATION, tokenTraineeA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BFLA 02: Employer cannot trigger data quality scans")
    void testEmployerCannotTriggerDqScans() throws Exception {
      mockMvc.perform(post("/api/v1/data-quality-rules/scan")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BFLA 03: Provider cannot create new user accounts")
    void testProviderCannotCreateUsers() throws Exception {
      String createUserPayload = """
          {
            "username": "new_user_attempt",
            "email": "new_user@test.gov.in",
            "password": "Password123!"
          }
          """;

      mockMvc.perform(post("/api/v1/users")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA)
              .contentType(MediaType.APPLICATION_JSON)
              .content(createUserPayload))
          .andExpect(status().isForbidden());
    }
  }
}
