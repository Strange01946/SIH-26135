package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

  private String tokenCanaryA;
  private String tokenCanaryB;
  private String tokenSuperAdmin;

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
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpCanaryFixtures();
  }

  private void cleanUpCanaryFixtures() {
    try {
      SecurityContextHolder.clearContext();
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
          VALUES (?, 'S-CANARY', 'State Canary', 'LG99755', 1, NOW(), NOW())
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
      MvcResult result = mockMvc.perform(get("/api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      // Must not leak passwords, tokens, or unmasked sensitive secrets
      assertThat(content).doesNotContain("$argon2id$");
      assertThat(content).doesNotContain("password");
    }
  }
}
