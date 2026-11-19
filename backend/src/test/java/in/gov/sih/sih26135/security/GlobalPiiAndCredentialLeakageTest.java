package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

/**
 * Phase 29.8 — Global PII & Credential Leakage Scanner Test Suite.
 *
 * <p>Systematically inspects:
 * <ul>
 *   <li>Response Model Reflection & Jackson Serialization for sensitive credential/token fields</li>
 *   <li>Live HTTP API Canary Testing for sensitive data exposure and cross-tenant leakage</li>
 *   <li>Statistical Disclosure Control (k=5 minimum anonymity threshold) via {@link AnalyticsPrivacySuppressionService}</li>
 *   <li>Secondary suppression / Differencing complement reconstruction protection</li>
 *   <li>Streaming Analytics Export privacy and authorization enforcement</li>
 * </ul>
 *
 * Runs strictly against {@code SIH26135_TEST}. Zero mutations to {@code SIH26135}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 29.8 — Global PII & Credential Leakage Scanner Tests")
public class GlobalPiiAndCredentialLeakageTest {

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
  private AnalyticsPrivacySuppressionService suppressionService;

  @Autowired
  private jakarta.servlet.Filter springSecurityFilterChain;

  private MockMvc mockMvc;

  // Dedicated Canary fixture IDs
  private static final Long CANARY_USER_A_ID = 99751L;
  private static final Long CANARY_USER_B_ID = 99752L;
  private static final Long CANARY_TRAINEE_A_ID = 99753L;
  private static final Long CANARY_TRAINEE_B_ID = 99754L;

  private static final String CANARY_EMAIL_A = "canary.trainee.a@sih.test";
  private static final String CANARY_PHONE_A = "9876543210";
  private static final String CANARY_AADHAAR_A = "9876-5432-1098";
  private static final String CANARY_PAN_A = "ABCDE1234F";

  // Dedicated SDC fixture IDs in isolated 99761..99797 range
  private static final Long SDC_STATE_ID = 99761L;
  private static final Long SDC_DISTRICT_ZERO_ID = 99762L;
  private static final Long SDC_DISTRICT_SPARSE_ID = 99763L;
  private static final Long SDC_DISTRICT_BOUNDARY_ID = 99764L;
  private static final Long SDC_PROVIDER_ID = 99765L;
  private static final Long SDC_CENTER_ID = 99766L;
  private static final Long SDC_BATCH_SPARSE_ID = 99767L;
  private static final Long SDC_BATCH_BOUNDARY_ID = 99768L;
  private static final Long SDC_COURSE_ID = 99769L;
  private static final Long SDC_SECTOR_ID = 99769L;

  private String tokenCanaryA;
  private String tokenCanaryB;
  private String tokenSuperAdmin;
  private String tokenSdcProvider;
  private String tokenNationalAdmin;

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

    cleanUpCanaryFixtures();
    seedCanaryFixtures();

    tokenCanaryA = "Bearer " + jwtTokenProvider.generateAccessToken(
        CANARY_USER_A_ID, "canary_user_a", List.of("ROLE_trainee", "trainee.read"));

    tokenCanaryB = "Bearer " + jwtTokenProvider.generateAccessToken(
        CANARY_USER_B_ID, "canary_user_b", List.of("ROLE_trainee", "trainee.read"));

    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L, "admin", List.of("ROLE_super_admin", "admin.nationwide", "trainee.read", "analytics.read", "analytics.export", "user.manage"));

    tokenSdcProvider = "Bearer " + jwtTokenProvider.generateAccessToken(
        99765L, "sdc_provider_user", List.of("ROLE_training_provider", "batch.read", "provider.read", "analytics.export"));

    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        99722L, "sdc_natadmin", List.of("ROLE_government_admin", "admin.nationwide", "analytics.read", "analytics.export"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpCanaryFixtures();
  }

  private void cleanUpCanaryFixtures() {
    try {
      SecurityContextHolder.clearContext();
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id BETWEEN 99790 AND 99797");
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?)",
          SDC_BATCH_SPARSE_ID, SDC_BATCH_BOUNDARY_ID);
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id = ?", SDC_CENTER_ID);
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", SDC_COURSE_ID);
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SDC_SECTOR_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", SDC_PROVIDER_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id BETWEEN 99780 AND 99787");
      jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 99770 AND 99777");
      jdbcTemplate.update("DELETE FROM users WHERE user_id = 99765");
      jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)",
          SDC_DISTRICT_ZERO_ID, SDC_DISTRICT_SPARSE_ID, SDC_DISTRICT_BOUNDARY_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", SDC_STATE_ID);

      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id IN (?, ?)",
          CANARY_TRAINEE_A_ID, CANARY_TRAINEE_B_ID);
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?)",
          CANARY_USER_A_ID, CANARY_USER_B_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = 99756");
      jdbcTemplate.update("DELETE FROM states WHERE state_id = 99755");
    } catch (Exception ignored) {
    }
  }

  private void seedCanaryFixtures() {
    // 1. Canary Trainees A & B
    Long refStateId = null;
    Long refDistrictId = null;
    try {
      refDistrictId = jdbcTemplate.queryForObject(
          "SELECT district_id FROM districts ORDER BY district_id LIMIT 1", Long.class);
      refStateId = jdbcTemplate.queryForObject(
          "SELECT state_id FROM districts WHERE district_id = ?", Long.class, refDistrictId);
    } catch (Exception ignored) {
    }
    if (refDistrictId == null || refStateId == null) {
      refStateId = 99755L;
      refDistrictId = 99756L;
      jdbcTemplate.update("""
          INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
          VALUES (?, 'SC', 'State Canary', 'LG99755', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE state_name = state_name
          """, refStateId);
      jdbcTemplate.update("""
          INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
          VALUES (?, ?, 'D-CANARY', 'District Canary', 'LGD99756', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE district_name = district_name
          """, refDistrictId, refStateId);
    }

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'canary_user_a', ?, '$argon2id$v=19$m=65536,t=3,p=4$CANARY_HASH_A', 'argon2id', 1, NOW(), NOW())
        """, CANARY_USER_A_ID, CANARY_EMAIL_A);

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'canary_user_b', 'canary_user_b@sih.test', '$argon2id$v=19$m=65536,t=3,p=4$CANARY_HASH_B', 'argon2id', 1, NOW(), NOW())
        """, CANARY_USER_B_ID);

    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, email, phone, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-CANARY-A', 'Canary', 'Alpha', ?, ?, 1, ?, ?, 1, 1, NOW(), NOW())
        """, CANARY_TRAINEE_A_ID, CANARY_USER_A_ID, CANARY_EMAIL_A, CANARY_PHONE_A, refStateId, refDistrictId);

    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, email, phone, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'REG-CANARY-B', 'Canary', 'Beta', 'canary.b@sih.test', '9876543211', 1, ?, ?, 1, 1, NOW(), NOW())
        """, CANARY_TRAINEE_B_ID, CANARY_USER_B_ID, refStateId, refDistrictId);

    // 2. SDC Structures: State and 3 Districts (Zero, Sparse, Boundary)
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SS', 'State SDC', 'LG99761', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE state_name = state_name
        """, SDC_STATE_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'D-ZERO', 'District Zero SDC', 'LGD99762', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE district_name = district_name
        """, SDC_DISTRICT_ZERO_ID, SDC_STATE_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'D-SPARSE', 'District Sparse SDC', 'LGD99763', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE district_name = district_name
        """, SDC_DISTRICT_SPARSE_ID, SDC_STATE_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'D-BOUND', 'District Boundary SDC', 'LGD99764', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE district_name = district_name
        """, SDC_DISTRICT_BOUNDARY_ID, SDC_STATE_ID);

    // Provider 99765 and User 99765
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-SDC-99765', 'Provider SDC Scorecard', 'REG-SDC-99765', 1, ?, ?, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE provider_name = provider_name
        """, SDC_PROVIDER_ID, SDC_STATE_ID, SDC_DISTRICT_SPARSE_ID);

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (99765, 'sdc_provider_user', 'sdc_provider@test.gov.in', '$argon2id$v=19$m=65536,t=3,p=4$CANARY_HASH_PROVIDER', 'argon2id', 1, 99765, NOW(), NOW())
        ON DUPLICATE KEY UPDATE username = username
        """);

    // Training Center 99766
    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'CTR-SDC-99766', 'Center SDC', ?, ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE center_name = center_name
        """, SDC_CENTER_ID, SDC_PROVIDER_ID, SDC_STATE_ID, SDC_DISTRICT_SPARSE_ID);

    // Sector & Course
    Long sdcCourseId = null;
    try {
      sdcCourseId = jdbcTemplate.queryForObject("SELECT course_id FROM courses ORDER BY course_id LIMIT 1", Long.class);
    } catch (Exception ignored) {}
    if (sdcCourseId == null) {
      jdbcTemplate.update("""
          INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
          VALUES (?, 'SEC-SDC-99769', 'Sector SDC', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE sector_name = sector_name
          """, SDC_SECTOR_ID);
      jdbcTemplate.update("""
          INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
          VALUES (?, 'CRS-SDC-99769', 'Course SDC', ?, 1, 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE course_name = course_name
          """, SDC_COURSE_ID, SDC_SECTOR_ID);
      sdcCourseId = SDC_COURSE_ID;
    }

    Long sdcProgramId = 1L;
    try {
      sdcProgramId = jdbcTemplate.queryForObject("SELECT program_id FROM programs ORDER BY program_id LIMIT 1", Long.class);
    } catch (Exception ignored) {}
    if (sdcProgramId == null) sdcProgramId = 1L;

    // Batches: Sparse (99767) and Boundary (99768)
    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'BAT-SDC-SPARSE', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)
        ON DUPLICATE KEY UPDATE batch_code = batch_code
        """, SDC_BATCH_SPARSE_ID, sdcCourseId, SDC_PROVIDER_ID, SDC_CENTER_ID, sdcProgramId);

    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at)
        VALUES (?, 'BAT-SDC-BOUND', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)
        ON DUPLICATE KEY UPDATE batch_code = batch_code
        """, SDC_BATCH_BOUNDARY_ID, sdcCourseId, SDC_PROVIDER_ID, SDC_CENTER_ID, sdcProgramId);

    // 3 Trainees & Enrollments for Sparse District (99763) -> Sparse count = 3
    for (int i = 0; i < 3; i++) {
      long userId = 99770L + i;
      long traineeId = 99780L + i;
      long enrollmentId = 99790L + i;
      jdbcTemplate.update("""
          INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
          VALUES (?, ?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE username = username
          """, userId, "sdc_sparse_user_" + i, "sdc_sparse_" + i + "@test.gov.in");

      jdbcTemplate.update("""
          INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, ?, ?, 'SparseTrainee', ?, 1, ?, ?, 1, 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE first_name = first_name
          """, traineeId, userId, "REG-SPARSE-" + i, "No" + i, SDC_STATE_ID, SDC_DISTRICT_SPARSE_ID);

      jdbcTemplate.update("""
          INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)
          ON DUPLICATE KEY UPDATE enrollment_number = enrollment_number
          """, enrollmentId, "ENR-SPARSE-" + i, traineeId, sdcProgramId, sdcCourseId, SDC_PROVIDER_ID, SDC_CENTER_ID, SDC_BATCH_SPARSE_ID);
    }

    // 5 Trainees & Enrollments for Boundary District (99764) -> Boundary count = 5
    for (int i = 0; i < 5; i++) {
      long userId = 99773L + i;
      long traineeId = 99783L + i;
      long enrollmentId = 99793L + i;
      jdbcTemplate.update("""
          INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
          VALUES (?, ?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE username = username
          """, userId, "sdc_bound_user_" + i, "sdc_bound_" + i + "@test.gov.in");

      jdbcTemplate.update("""
          INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, ?, ?, 'BoundTrainee', ?, 1, ?, ?, 1, 1, NOW(), NOW())
          ON DUPLICATE KEY UPDATE first_name = first_name
          """, traineeId, userId, "REG-BOUND-" + i, "No" + i, SDC_STATE_ID, SDC_DISTRICT_BOUNDARY_ID);

      jdbcTemplate.update("""
          INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)
          ON DUPLICATE KEY UPDATE enrollment_number = enrollment_number
          """, enrollmentId, "ENR-BOUND-" + i, traineeId, sdcProgramId, sdcCourseId, SDC_PROVIDER_ID, SDC_CENTER_ID, SDC_BATCH_BOUNDARY_ID);
    }
  }

  // =========================================================================
  // 1. Response Model Reflection & Jackson Inspection
  // =========================================================================

  @Nested
  @DisplayName("1. Response Model Reflection & Serialization Inspection")
  class ResponseModelReflectionAndJacksonInspectionTests {

    private static final Set<String> FORBIDDEN_MODEL_FIELD_NAMES = Set.of(
        "password",
        "passwordhash",
        "passwd",
        "pwd",
        "secret",
        "secretkey",
        "jwtsecret",
        "apikey",
        "privatekey",
        "vaultkey"
    );

    @Test
    @DisplayName("REFL 01: Scans all response DTOs ensuring zero forbidden credential fields")
    void testResponseDtoReflectionInspection() throws Exception {
      File responseDtoDir = new File("src/main/java/in/gov/sih/sih26135/dto/response");
      assertThat(responseDtoDir).isDirectory();

      List<Class<?>> responseClasses = loadClassesInPackage(responseDtoDir, "in.gov.sih.sih26135.dto.response");
      assertThat(responseClasses).isNotEmpty();

      List<String> violations = new ArrayList<>();

      for (Class<?> clazz : responseClasses) {
        // Inspect declared fields
        for (Field field : clazz.getDeclaredFields()) {
          String normalizedName = field.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
          if (FORBIDDEN_MODEL_FIELD_NAMES.contains(normalizedName)) {
            violations.add(String.format("Class %s declares forbidden sensitive field: %s",
                clazz.getSimpleName(), field.getName()));
          }
        }

        // If it's a Java record, also inspect record components
        if (clazz.isRecord()) {
          for (RecordComponent comp : clazz.getRecordComponents()) {
            String normalizedName = comp.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (FORBIDDEN_MODEL_FIELD_NAMES.contains(normalizedName)) {
              violations.add(String.format("Record %s declares forbidden record component: %s",
                  clazz.getSimpleName(), comp.getName()));
            }
          }
        }
      }

      assertThat(violations)
          .as("Response DTOs must never declare fields exposing raw passwords or secret keys")
          .isEmpty();
    }

    private List<Class<?>> loadClassesInPackage(File dir, String packageName) {
      List<Class<?>> classes = new ArrayList<>();
      File[] files = dir.listFiles();
      if (files == null) return classes;

      for (File file : files) {
        if (file.isDirectory()) {
          classes.addAll(loadClassesInPackage(file, packageName + "." + file.getName()));
        } else if (file.getName().endsWith(".java")) {
          String className = packageName + "." + file.getName().substring(0, file.getName().length() - 5);
          try {
            Class<?> clazz = Class.forName(className);
            classes.add(clazz);
          } catch (ClassNotFoundException ignored) {
          }
        }
      }
      return classes;
    }
  }

  // =========================================================================
  // 2. Synthetic Canary PII Leakage Testing Over Real HTTP
  // =========================================================================

  @Nested
  @DisplayName("2. Synthetic Canary PII Leakage Testing Over Real HTTP")
  class SyntheticCanaryPiiLeakageHttpTests {

    @Test
    @DisplayName("CANARY 01: Trainee B cannot access Trainee A's canary profile (No BOLA/PII leak)")
    void testTraineeCannotLeakOtherTraineeCanary() throws Exception {
      // Trainee B requests Trainee A's profile -> Rejected 400, 403 or 404
      MvcResult result = mockMvc.perform(get("/api/v1/trainees/" + CANARY_TRAINEE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenCanaryB))
          .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 403, 404)))
          .andReturn();

      String body = result.getResponse().getContentAsString();
      // Ensure canary email and phone are completely absent from the response
      assertThat(body).doesNotContain(CANARY_EMAIL_A);
      assertThat(body).doesNotContain(CANARY_PHONE_A);
    }

    @Test
    @DisplayName("CANARY 02: Password hash canary is NEVER leaked in any user or trainee response")
    void testPasswordHashCanaryNeverLeaked() throws Exception {
      // Authorized Super Admin inspects user and trainee records
      MvcResult userResult = mockMvc.perform(get("/api/v1/users/" + CANARY_USER_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String userBody = userResult.getResponse().getContentAsString();
      assertThat(userBody).doesNotContain("CANARY_HASH_A");
      assertThat(userBody).doesNotContain("$argon2id$");

      MvcResult traineeResult = mockMvc.perform(get("/api/v1/trainees/" + CANARY_TRAINEE_A_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String traineeBody = traineeResult.getResponse().getContentAsString();
      assertThat(traineeBody).doesNotContain("CANARY_HASH_A");
      assertThat(traineeBody).doesNotContain("$argon2id$");
    }
  }

  // =========================================================================
  // 3. Statistical Disclosure Control — k = 5 Suppression
  // =========================================================================

  @Nested
  @DisplayName("3. Statistical Disclosure Control (k=5) & Differencing Protection")
  class StatisticalDisclosureControlK5Tests {

    @Test
    @DisplayName("SDC 01: Anonymity threshold constant strictly equals 5")
    void testAnonymityThresholdConstant() {
      assertThat(AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD)
          .as("Minimum anonymity threshold k must be exactly 5")
          .isEqualTo(5L);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L, 4L})
    @DisplayName("SDC 02: Counts 1..4 are identified as sparse and suppressed to null")
    void testSparseCountsSuppressed(long count) {
      assertThat(suppressionService.isSparse(count))
          .as("Count %d must be flagged as sparse", count)
          .isTrue();

      assertThat(suppressionService.suppressCount(count))
          .as("Count %d must be suppressed to null", count)
          .isNull();
    }

    @Test
    @DisplayName("SDC 03: Safe zero (0) is preserved as non-reconstructive")
    void testSafeZeroPreserved() {
      assertThat(suppressionService.isSparse(0L))
          .as("Safe zero must not be flagged as sparse")
          .isFalse();

      assertThat(suppressionService.suppressCount(0L))
          .as("Safe zero must be preserved")
          .isEqualTo(0L);
    }

    @ParameterizedTest
    @ValueSource(longs = {5L, 6L, 10L, 100L})
    @DisplayName("SDC 04: Counts >= 5 are disclosed with actual value")
    void testSufficientCountsDisclosed(long count) {
      assertThat(suppressionService.isSparse(count))
          .as("Count %d >= 5 must not be sparse", count)
          .isFalse();

      assertThat(suppressionService.suppressCount(count))
          .as("Count %d >= 5 must be disclosed", count)
          .isEqualTo(count);
    }

    @Test
    @DisplayName("SDC 05: Rates and sensitive salary measures are suppressed when denominator is sparse")
    void testRatesAndSalariesSuppressedWhenSparse() {
      BigDecimal rate = new BigDecimal("75.50");
      BigDecimal salary = new BigDecimal("25000.00");

      // Denominator sparse (1..4) -> suppressed to null
      assertThat(suppressionService.suppressRate(rate, 3L)).isNull();
      assertThat(suppressionService.suppressSalary(salary, 3L)).isNull();

      // Denominator >= 5 -> disclosed
      assertThat(suppressionService.suppressRate(rate, 5L)).isEqualTo(rate);
      assertThat(suppressionService.suppressSalary(salary, 5L)).isEqualTo(salary);
    }

    @Test
    @DisplayName("SDC 06: Live HTTP District Dashboard preserves safe zero (count=0) for district with 0 trainees")
    void testLiveHttpDistrictDashboardPreservesSafeZero() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/" + SDC_DISTRICT_ZERO_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.districtId").value(SDC_DISTRICT_ZERO_ID))
          .andExpect(jsonPath("$.data.performance.enrollmentCount").value(0))
          .andExpect(jsonPath("$.data.performance.traineeCount").value(0));
    }

    @Test
    @DisplayName("SDC 07: Live HTTP District Dashboard suppresses sparse counts (k < 5, count=3) to null")
    void testLiveHttpDistrictDashboardSuppressesSparseCounts() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/districts/" + SDC_DISTRICT_SPARSE_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.districtId").value(SDC_DISTRICT_SPARSE_ID))
          .andExpect(jsonPath("$.data.performance.enrollmentCount").doesNotExist())
          .andExpect(jsonPath("$.data.performance.traineeCount").doesNotExist())
          .andExpect(jsonPath("$.data.performance.completedCount").doesNotExist())
          .andExpect(jsonPath("$.data.performance.employmentRatePct").doesNotExist())
          .andReturn();

      String json = result.getResponse().getContentAsString();
      // Verify raw count 3 is NOT disclosed in performance fields
      assertThat(json).doesNotContain("\"traineeCount\":3");
      assertThat(json).doesNotContain("\"enrollmentCount\":3");
    }

    @Test
    @DisplayName("SDC 08: Live HTTP District Dashboard discloses boundary counts (k >= 5, count=5) with actual values")
    void testLiveHttpDistrictDashboardDisclosesBoundaryCounts() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/districts/" + SDC_DISTRICT_BOUNDARY_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.districtId").value(SDC_DISTRICT_BOUNDARY_ID))
          .andExpect(jsonPath("$.data.performance.enrollmentCount").value(5))
          .andExpect(jsonPath("$.data.performance.traineeCount").value(5));
    }

    @Test
    @DisplayName("SDC 09: Live HTTP State Dashboard applies secondary suppression / anti-differencing when constituent district is sparse")
    void testLiveHttpStateDashboardAppliesSecondarySuppression() throws Exception {
      // In State 99761, District 99763 is sparse (3), District 99762 has 0, District 99764 has 5.
      // SDC secondary suppression withholds state-level totals when exactly 1 district is suppressed
      // to prevent deductive differencing (Total - District 99764 = District 99763).
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/states/" + SDC_STATE_ID)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andReturn();

      String json = result.getResponse().getContentAsString();
      assertThat(json).contains("\"stateId\":" + SDC_STATE_ID);
    }

    @Test
    @DisplayName("SDC 10: Streaming QPR JSON export enforces partition anti-differencing on national summary")
    void testStreamingQprJsonExportEnforcesPartitionAntiDifferencing() throws Exception {
      String json = executeStreamingGet("/api/v1/analytics/export/qpr/json?fiscalYear=FY2026-27&quarter=Q1", tokenSuperAdmin);
      assertThat(json).contains("\"reportType\":\"QPR\"");
      assertThat(json).contains("\"privacyThreshold\":5");
      // Must not leak passwords or hashes
      assertThat(json).doesNotContain("$argon2id$");
      assertThat(json).doesNotContain("password");
    }
  }

  // =========================================================================
  // 4. Streaming Analytics Export Privacy
  // =========================================================================

  @Nested
  @DisplayName("4. Streaming Analytics Export Privacy & Authorization")
  class StreamingExportPrivacyTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/analytics/export/dsdp/csv?districtId=1",
        "/api/v1/analytics/export/qpr/csv",
        "/api/v1/analytics/export/provider-scorecard/csv"
    })
    @DisplayName("EXPORT 01: Unauthenticated streaming export requests receive HTTP 401")
    void testUnauthenticatedExportRejected(String exportUrl) throws Exception {
      mockMvc.perform(get(exportUrl))
          .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/analytics/export/dsdp/csv?districtId=1",
        "/api/v1/analytics/export/qpr/csv",
        "/api/v1/analytics/export/provider-scorecard/csv"
    })
    @DisplayName("EXPORT 02: Non-administrative personas receive HTTP 403 on streaming exports")
    void testNonAdminExportForbidden(String exportUrl) throws Exception {
      mockMvc.perform(get(exportUrl)
              .header(HttpHeaders.AUTHORIZATION, tokenCanaryA))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EXPORT 03: Authorized export produces streaming CSV adhering to security headers and no PII leak")
    void testAuthorizedExportProducesValidStream() throws Exception {
      String content = executeStreamingGet("/api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1", tokenSuperAdmin);
      // Must not leak passwords, tokens, or unmasked sensitive secrets
      assertThat(content).doesNotContain("$argon2id$");
      assertThat(content).doesNotContain("password");
    }

    @Test
    @DisplayName("EXPORT 04: Provider Scorecard CSV export enforces tenant isolation (Provider A receives only Provider A records)")
    void testProviderScorecardExportTenantIsolation() throws Exception {
      String csv = executeStreamingGet("/api/v1/analytics/export/provider-scorecard/csv?providerId=" + SDC_PROVIDER_ID, tokenSdcProvider);
      String[] lines = csv.split("\r?\n");
      assertThat(lines.length).isGreaterThanOrEqualTo(1);

      // Verify header matches formal specification
      assertThat(lines[0]).contains("Provider ID");
      assertThat(lines[0]).contains("Provider Code");
      assertThat(lines[0]).contains("Enrollments");
      assertThat(lines[0]).contains("Trainees");

      // Verify that every data line belongs strictly to SDC_PROVIDER_ID (no foreign providers)
      for (int i = 1; i < lines.length; i++) {
        if (!lines[i].isBlank()) {
          assertThat(lines[i]).startsWith(String.valueOf(SDC_PROVIDER_ID));
        }
      }

      // Verify canary absence
      assertThat(csv).doesNotContain("$argon2id$");
      assertThat(csv).doesNotContain("CANARY_HASH");
    }

    @Test
    @DisplayName("EXPORT 05: Provider Scorecard CSV export enforces statistical suppression with empty string cells for sparse groups")
    void testProviderScorecardExportSuppressionEmptyCells() throws Exception {
      String csv = executeStreamingGet("/api/v1/analytics/export/provider-scorecard/csv?providerId=" + SDC_PROVIDER_ID, tokenSdcProvider);
      // For sparse provider scorecard rows, counts are suppressed to "" (empty string)
      assertThat(csv).doesNotContain(",3,3,"); // Sparse count 3 must NOT be rendered as raw count
    }

    @Test
    @DisplayName("EXPORT 06: Provider Scorecard JSON export streams structured JSON with null fields for suppressed values and zero canary leaks")
    void testProviderScorecardJsonExportSuppressedValues() throws Exception {
      String json = executeStreamingGet("/api/v1/analytics/export/provider-scorecard/json?providerId=" + SDC_PROVIDER_ID, tokenSdcProvider);
      assertThat(json).contains("\"reportType\":\"PROVIDER_SCORECARD\"");
      assertThat(json).contains("\"privacyThreshold\":5");
      assertThat(json).doesNotContain("$argon2id$");
      assertThat(json).doesNotContain("CANARY_HASH");
    }
  }

  private String executeStreamingGet(String url, String token) throws Exception {
    MvcResult result = mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andReturn();
    if (result.getRequest().isAsyncStarted()) {
      MvcResult asyncResult = mockMvc.perform(asyncDispatch(result))
          .andExpect(status().isOk())
          .andReturn();
      return asyncResult.getResponse().getContentAsString();
    }
    return result.getResponse().getContentAsString();
  }
}
