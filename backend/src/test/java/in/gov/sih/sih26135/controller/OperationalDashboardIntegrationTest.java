package in.gov.sih.sih26135.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.7 - Operational Health Scorecard, DQI & Monitoring API Integration Tests")
class OperationalDashboardIntegrationTest {

  private static final long STATE_A_ID = 9911L;
  private static final long STATE_B_ID = 9912L;
  private static final long DISTRICT_A1_ID = 9911L;
  private static final long DISTRICT_B1_ID = 9912L;
  private static final long DISTRICT_A2_ID = 9913L;

  private static final long SECTOR_ID = 9911L;
  private static final long COURSE_ID = 9911L;
  private static final long ORG_ID = 9911L;
  private static final long DEPT_ID = 9911L;
  private static final long SCHEME_ID = 9911L;
  private static final long PROGRAM_ID = 9911L;

  private static final long PROVIDER_A1_ID = 9911L;
  private static final long PROVIDER_B_ID = 9912L;
  private static final long PROVIDER_A2_ID = 9913L; // Same state as PROVIDER_A1, distinct provider
  private static final long EMPLOYER_A_ID = 9911L;

  private static final long CENTER_A1_ID = 9911L;
  private static final long CENTER_A2_ID = 9913L;
  private static final long BATCH_A1_ID = 9911L;
  private static final long BATCH_A2_ID = 9913L;

  private static final long TRAINEE_1_ID = 9911L;
  private static final long TRAINEE_2_ID = 9912L;
  private static final long TRAINEE_3_ID = 9913L;
  private static final long TRAINEE_A2_ID = 9914L;

  private static final long ENROLLMENT_1_ID = 9911L;
  private static final long ENROLLMENT_2_ID = 9912L;
  private static final long ENROLLMENT_3_ID = 9913L;
  private static final long ENROLLMENT_A2_ID = 9914L;
  private static final long PLACEMENT_EMP_A_ID = 9911L;

  private static final long RULE_1_ID = 9911L;
  private static final long RULE_2_ID = 9912L;
  private static final long RULE_3_ID = 9913L;

  private static final long USER_SUPER_ADMIN_ID = 9910L;
  private static final long USER_STATE_A_ID = 9911L;
  private static final long USER_DIST_A1_ID = 9912L;
  private static final long USER_STATE_B_ID = 9913L;
  private static final long USER_PROVIDER_A1_ID = 9914L;
  private static final long USER_PROVIDER_B_ID = 9915L;
  private static final long USER_PROVIDER_A2_ID = 9916L;
  private static final long USER_EMPLOYER_A_ID = 9917L;
  private static final long USER_TRAINEE_ID = 9918L;
  private static final long USER_UNSCOPED_ID = 9919L;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String tokenSuperAdmin;
  private String tokenStateA;
  private String tokenDistrictA1;
  private String tokenStateB;
  private String tokenProviderA1;
  private String tokenProviderB;
  private String tokenProviderA2;
  private String tokenEmployerA;
  private String tokenTrainee;
  private String tokenUnscoped;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    cleanUpFixtures();
    seedBaseFixtures();

    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "op287_super",
        List.of("ROLE_super_admin", "audit.read", "system.manage"));
    tokenStateA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ID, "op287_state_a",
        List.of("ROLE_state_admin", "audit.read"));
    tokenDistrictA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_DIST_A1_ID, "op287_dist_a1",
        List.of("ROLE_district_officer", "audit.read"));
    tokenStateB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ID, "op287_state_b",
        List.of("ROLE_state_admin", "audit.read"));
    tokenProviderA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A1_ID, "op287_prov_a1",
        List.of("ROLE_training_provider", "audit.read"));
    tokenProviderB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_B_ID, "op287_prov_b",
        List.of("ROLE_training_provider", "audit.read"));
    tokenProviderA2 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A2_ID, "op287_prov_a2",
        List.of("ROLE_training_provider", "audit.read"));
    tokenEmployerA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_A_ID, "op287_emp_a",
        List.of("ROLE_employer", "audit.read"));
    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_ID, "op287_trainee",
        List.of("ROLE_trainee", "audit.read", "analytics.read"));
    tokenUnscoped = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_UNSCOPED_ID, "op287_unscoped",
        List.of("ROLE_state_admin", "audit.read"));
  }

  @AfterEach
  void tearDown() {
    cleanUpFixtures();
  }

  private void seedBaseFixtures() {
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'O1', 'Op State A', 'LG9911', 1, NOW(), NOW())",
        STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'O2', 'Op State B', 'LG9912', 1, NOW(), NOW())",
        STATE_B_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'OD1', 'Op District A1', 'LGD9911', 1, NOW(), NOW())",
        DISTRICT_A1_ID, STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'OD2', 'Op District B1', 'LGD9912', 1, NOW(), NOW())",
        DISTRICT_B1_ID, STATE_B_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'OD3', 'Op District A2', 'LGD9913', 1, NOW(), NOW())",
        DISTRICT_A2_ID, STATE_A_ID);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9911', 'Op Provider A1', 'REG-PRV-9911', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9912', 'Op Provider B', 'REG-PRV-9912', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_B_ID, STATE_B_ID, DISTRICT_B1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9913', 'Op Provider A2', 'REG-PRV-9913', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-9911', 'Op Employer A', 'REG-EMP-9911', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-9911', 'Op Sector', 1, NOW(), NOW())",
        SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-9911', 'Op Course', ?, 1, 1, NOW(), NOW(), NULL)",
        COURSE_ID, SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-9911', 'Op Org', 1, 1, NOW(), NOW())",
        ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-9911', 'Op Dept', 1, NOW(), NOW())",
        DEPT_ID, ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-9911', 'Op Scheme', ?, 'INR', 1, NOW(), NOW())",
        SCHEME_ID, DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-9911', 'Op Program', ?, ?, 1, NOW(), NOW())",
        PROGRAM_ID, DEPT_ID, SCHEME_ID);

    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-9911', 'Op Center A1', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A1_ID, PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-9913', 'Op Center A2', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A2_ID, PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-9911', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A1_ID, COURSE_ID, PROVIDER_A1_ID, CENTER_A1_ID, PROGRAM_ID);
    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-9913', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A2_ID, COURSE_ID, PROVIDER_A2_ID, CENTER_A2_ID, PROGRAM_ID);

    insertUser(USER_SUPER_ADMIN_ID, "op287_super", "op9910@test.gov.in", null, null, null, null, "super_admin");
    insertUser(USER_STATE_A_ID, "op287_state_a", "op9911@test.gov.in", STATE_A_ID, null, null, null, "state_admin");
    insertUser(USER_DIST_A1_ID, "op287_dist_a1", "op9912@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, "district_officer");
    insertUser(USER_STATE_B_ID, "op287_state_b", "op9913@test.gov.in", STATE_B_ID, null, null, null, "state_admin");
    insertUser(USER_PROVIDER_A1_ID, "op287_prov_a1", "op9914@test.gov.in", null, null, PROVIDER_A1_ID, null, "training_provider");
    insertUser(USER_PROVIDER_B_ID, "op287_prov_b", "op9915@test.gov.in", null, null, PROVIDER_B_ID, null, "training_provider");
    insertUser(USER_PROVIDER_A2_ID, "op287_prov_a2", "op9916@test.gov.in", null, null, PROVIDER_A2_ID, null, "training_provider");
    insertUser(USER_EMPLOYER_A_ID, "op287_emp_a", "op9917@test.gov.in", null, null, null, EMPLOYER_A_ID, "employer");
    insertUser(USER_TRAINEE_ID, "op287_trainee", "op9918@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, "trainee");
    insertUser(USER_UNSCOPED_ID, "op287_unscoped", "op9919@test.gov.in", null, null, null, null, "state_admin");

    jdbcTemplate.update(
        "INSERT INTO data_quality_rules (data_quality_rule_id, rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'OP287_R_ALPHA', 'Alpha Rule', 'Test rule 1', 'Trainee', 2, 3, 1, NOW(), NOW())",
        RULE_1_ID);
    jdbcTemplate.update(
        "INSERT INTO data_quality_rules (data_quality_rule_id, rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'OP287_R_BETA', 'Beta Rule', 'Test rule 2', 'Trainee', 4, 4, 1, NOW(), NOW())",
        RULE_2_ID);
    jdbcTemplate.update(
        "INSERT INTO data_quality_rules (data_quality_rule_id, rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'OP287_R_GAMMA', 'Gamma Rule', 'Test rule 3', 'Trainee', 3, 2, 1, NOW(), NOW())",
        RULE_3_ID);
  }

  private void insertUser(
      Long userId,
      String username,
      String email,
      Long stateId,
      Long districtId,
      Long providerId,
      Long employerId,
      String roleCode) {
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, employer_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'hash', 'argon2id', 1, ?, ?, ?, ?, NOW(), NOW())",
        userId, username, email, stateId, districtId, providerId, employerId);
    if (roleCode != null) {
      jdbcTemplate.update(
          "INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT ?, role_id, NOW() FROM roles WHERE role_code = ? LIMIT 1",
          userId, roleCode);
    }
  }

  private void cleanUpFixtures() {
    jdbcTemplate.update("DELETE FROM audit_logs WHERE audit_log_id BETWEEN 99100 AND 99199 OR audit_log_id BETWEEN 987000 AND 987999 OR actor_user_id BETWEEN 9910 AND 9919");
    jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id BETWEEN 99100 AND 99199 OR system_event_log_id BETWEEN 987000 AND 987999 OR actor_user_id BETWEEN 9910 AND 9919");

    jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 99100 AND 99199 OR data_quality_issue_event_id BETWEEN 987000 AND 987999 OR data_quality_issue_id BETWEEN 99100 AND 99199 OR data_quality_issue_id BETWEEN 987000 AND 987999");
    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_issue_id BETWEEN 99100 AND 99199 OR data_quality_issue_id BETWEEN 987000 AND 987999");
    jdbcTemplate.update("DELETE FROM data_quality_rules WHERE data_quality_rule_id IN (?, ?, ?)", RULE_1_ID, RULE_2_ID, RULE_3_ID);

    jdbcTemplate.update("DELETE FROM import_records WHERE import_record_id BETWEEN 99100 AND 99199 OR import_record_id BETWEEN 987000 AND 987999 OR import_batch_id BETWEEN 99100 AND 99199 OR import_batch_id BETWEEN 987000 AND 987999");
    jdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id BETWEEN 99100 AND 99199 OR import_batch_id BETWEEN 987000 AND 987999 OR initiated_by_user_id BETWEEN 9910 AND 9919");

    jdbcTemplate.update("DELETE FROM placement_records WHERE placement_id = ?", PLACEMENT_EMP_A_ID);
    jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id BETWEEN 9911 AND 9919 OR enrollment_id BETWEEN 987000 AND 987999");
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id BETWEEN 9911 AND 9919 OR trainee_id BETWEEN 987000 AND 987999");
    jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?)", BATCH_A1_ID, BATCH_A2_ID);
    jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (?, ?)", CENTER_A1_ID, CENTER_A2_ID);
    jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", COURSE_ID);
    jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", PROGRAM_ID);
    jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", SCHEME_ID);
    jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", DEPT_ID);
    jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", ORG_ID);
    jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SECTOR_ID);

    jdbcTemplate.update("DELETE FROM user_roles WHERE user_id BETWEEN 9910 AND 9919");
    jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 9910 AND 9919");
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?, ?)", PROVIDER_A1_ID, PROVIDER_B_ID, PROVIDER_A2_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", EMPLOYER_A_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)", DISTRICT_A1_ID, DISTRICT_B1_ID, DISTRICT_A2_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)", STATE_A_ID, STATE_B_ID);
  }

  private void seedTraineeWithEnrollment(long traineeId, long enrollmentId, long providerId, long centerId, long batchId, long stateId, long districtId) {
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'OpTest', 'Trainee', '9840099111', 'op@test.gov.in', '2000-01-01', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        traineeId, "OP287-TR-" + traineeId, stateId, districtId);
    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)",
        enrollmentId, "OP287-ENR-" + enrollmentId, traineeId, PROGRAM_ID, COURSE_ID, providerId, centerId, batchId);
  }

  private void insertDqIssue(
      long issueId,
      long ruleId,
      long categoryId,
      long severityId,
      long statusId,
      String entityType,
      long entityId,
      LocalDateTime detectedAt,
      LocalDateTime resolvedAt) {
    jdbcTemplate.update(
        "INSERT INTO data_quality_issues (data_quality_issue_id, data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, resolved_at, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, 1, ?, ?, 'OP287 test issue', ?, ?, ?, NOW())",
        issueId, ruleId, categoryId, severityId, statusId, entityType, entityId,
        Timestamp.valueOf(detectedAt),
        resolvedAt != null ? Timestamp.valueOf(resolvedAt) : null,
        Timestamp.valueOf(detectedAt));
  }

  @Nested
  @DisplayName("1. Data Quality Scorecard Endpoint Tests")
  class DataQualityScorecardTests {

    @Test
    @DisplayName("Handles empty population safely (null DQI), clean cohort (100.00), multi-issue non-double-counting, lifecycle filtering, rule ranking, resolution hours, and same-state provider isolation")
    void scorecardCalculationLifecycleAndTenantIsolation() throws Exception {
      // 1. Provider B has zero eligible records and zero issues -> DQI is null (never fake 100%)
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.eligiblePopulation").value(0))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(0))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").doesNotExist())
          .andExpect(jsonPath("$.data.totalActiveIssues").value(0))
          .andExpect(jsonPath("$.data.averageResolutionTimeHours").doesNotExist());

      // Seed 3 trainees + 3 enrollments under Provider A1 (eligible = 6), and 1 trainee + 1 enrollment under Provider A2 (same State A!)
      seedTraineeWithEnrollment(TRAINEE_1_ID, ENROLLMENT_1_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
      seedTraineeWithEnrollment(TRAINEE_2_ID, ENROLLMENT_2_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
      seedTraineeWithEnrollment(TRAINEE_3_ID, ENROLLMENT_3_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
      seedTraineeWithEnrollment(TRAINEE_A2_ID, ENROLLMENT_A2_ID, PROVIDER_A2_ID, CENTER_A2_ID, BATCH_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

      // 2. Clean Provider A1 cohort -> DQI = 100.00
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(6))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(0))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(100.00));

      LocalDateTime base = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
      // Seed active issues on Provider A1:
      // - 3 active issues on ONE trainee (TRAINEE_1): Rule 1 OPEN (CRITICAL=4), Rule 2 ASSIGNED (HIGH=3), Rule 3 IN_PROGRESS (MEDIUM=2)
      insertDqIssue(99101L, RULE_1_ID, 2L, 4L, 1L, "Trainee", TRAINEE_1_ID, base, null);
      insertDqIssue(99102L, RULE_2_ID, 4L, 3L, 2L, "Trainee", TRAINEE_1_ID, base.plusHours(1), null);
      insertDqIssue(99103L, RULE_3_ID, 3L, 2L, 3L, "Trainee", TRAINEE_1_ID, base.plusHours(2), null);
      // - 1 REOPENED issue on TRAINEE_2 (Rule 1, LOW=1) -> active count = 4, distinct affected entities = 2 of 6 (DQI = 4/6*100 = 66.67%)
      insertDqIssue(99104L, RULE_1_ID, 2L, 1L, 6L, "Trainee", TRAINEE_2_ID, base.plusHours(3), null);
      // - Historical events on TRAINEE_1 issue must NOT inflate active counts
      jdbcTemplate.update(
          "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_at, remarks, created_at) "
              + "VALUES (99101, 99103, 1, ?, 'Opened', NOW()), (99102, 99103, 2, ?, 'Assigned', NOW()), (99103, 99103, 3, ?, 'Started', NOW())",
          Timestamp.valueOf(base), Timestamp.valueOf(base.plusMinutes(5)), Timestamp.valueOf(base.plusMinutes(10)));

      // - 1 RESOLVED issue on TRAINEE_3 (duration = 3 hours), 1 RESOLVED missing resolved_at (ignored in duration avg), 1 DISMISSED (excluded from active & resolution avg)
      insertDqIssue(99105L, RULE_2_ID, 4L, 3L, 4L, "Trainee", TRAINEE_3_ID, base, base.plusHours(3));
      insertDqIssue(99106L, RULE_2_ID, 4L, 3L, 4L, "Trainee", TRAINEE_3_ID, base, null); // missing resolved_at
      insertDqIssue(99107L, RULE_3_ID, 3L, 2L, 5L, "Trainee", TRAINEE_3_ID, base, base.plusHours(10)); // DISMISSED

      // Seed active issue on Provider A2 (same State A, different provider!) -> must NOT appear in Provider A1's scorecard
      insertDqIssue(99108L, RULE_2_ID, 4L, 4L, 1L, "Trainee", TRAINEE_A2_ID, base, null);

      MvcResult a1Res = mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(6))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(2))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(66.67))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(4))
          .andExpect(jsonPath("$.data.activeIssuesByStatus.OPEN").value(1))
          .andExpect(jsonPath("$.data.activeIssuesByStatus.ASSIGNED").value(1))
          .andExpect(jsonPath("$.data.activeIssuesByStatus.IN_PROGRESS").value(1))
          .andExpect(jsonPath("$.data.activeIssuesByStatus.REOPENED").value(1))
          .andExpect(jsonPath("$.data.activeIssuesBySeverity.CRITICAL").value(1))
          .andExpect(jsonPath("$.data.activeIssuesBySeverity.HIGH").value(1))
          .andExpect(jsonPath("$.data.activeIssuesBySeverity.MEDIUM").value(1))
          .andExpect(jsonPath("$.data.activeIssuesBySeverity.LOW").value(1))
          .andExpect(jsonPath("$.data.topFailingRules[0].ruleCode").value("OP287_R_ALPHA"))
          .andExpect(jsonPath("$.data.topFailingRules[0].activeIssueCount").value(2))
          // Tie-breaker: OP287_R_BETA vs OP287_R_GAMMA both 1 active issue -> alphabetical OP287_R_BETA precedes OP287_R_GAMMA
          .andExpect(jsonPath("$.data.topFailingRules[1].ruleCode").value("OP287_R_BETA"))
          .andExpect(jsonPath("$.data.topFailingRules[2].ruleCode").value("OP287_R_GAMMA"))
          .andExpect(jsonPath("$.data.resolvedIssueCount").value(1))
          .andExpect(jsonPath("$.data.reopenedIssueCount").value(1))
          .andExpect(jsonPath("$.data.averageResolutionTimeHours").value(3.00))
          .andExpect(jsonPath("$.data.resolutionTimeUnit").value("HOURS"))
          .andReturn();
      assertThat(a1Res.getResponse().getContentAsString()).doesNotContain("9840099111");

      // State A Admin sees BOTH Provider A1 + Provider A2 (eligible = 8, distinct affected = 3 -> 5/8 = 62.50%)
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(8))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(3))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(62.50))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(5));

      // State B Admin still sees 0 (complete isolation from State A)
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenStateB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(0))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(0));
    }

    @Test
    @DisplayName("Date-filtered scorecard keeps current DQI accurate for unresolved issues detected before windowStart without double-counting or cross-provider leakage")
    void dateFilteredScorecardPreservesPreWindowUnresolvedIssues() throws Exception {
      // Seed 50 trainees + 50 enrollments under Provider A1 = 100 eligible entities
      for (int i = 1; i <= 50; i++) {
        long id = 987000L + i;
        seedTraineeWithEnrollment(id, id, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
      }
      // Seed 1 trainee + 1 enrollment under same-state Provider A2 = 2 eligible entities
      seedTraineeWithEnrollment(987090L, 987090L, PROVIDER_A2_ID, CENTER_A2_ID, BATCH_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

      // Selected query window: [2026-11-10 00:00:00, 2026-11-13 00:00:00)
      LocalDateTime beforeWindow = LocalDateTime.of(2026, 11, 5, 10, 0, 0);
      long nextIssueId = 987001L;

      // 20 distinct entities with unresolved issues detected BEFORE windowStart:
      // - Entity #1 gets 3 open issues (must count as 1 affected entity)
      insertDqIssue(nextIssueId++, RULE_1_ID, 2L, 4L, 1L, "Trainee", 987001L, beforeWindow, null);
      insertDqIssue(nextIssueId++, RULE_2_ID, 4L, 3L, 2L, "Trainee", 987001L, beforeWindow.plusMinutes(5), null);
      insertDqIssue(nextIssueId++, RULE_3_ID, 3L, 2L, 3L, "Trainee", 987001L, beforeWindow.plusMinutes(10), null);
      // - Entity #2 gets 2 open issues (must count as 1 affected entity)
      insertDqIssue(nextIssueId++, RULE_1_ID, 2L, 4L, 1L, "Trainee", 987002L, beforeWindow, null);
      insertDqIssue(nextIssueId++, RULE_2_ID, 4L, 3L, 6L, "Trainee", 987002L, beforeWindow.plusMinutes(5), null);
      // - Entities #3..#20 get 1 open issue each
      for (int i = 3; i <= 20; i++) {
        insertDqIssue(nextIssueId++, RULE_1_ID, 2L, 3L, 1L, "Trainee", 987000L + i, beforeWindow, null);
      }
      // Total unresolved issues on Provider A1 = 23 across 20 distinct entities; 0 detected inside [2026-11-10, 2026-11-12]

      // Resolved (status=4) and Dismissed (status=5) issues on clean entities (#21, #22) must NOT affect DQI
      insertDqIssue(nextIssueId++, RULE_2_ID, 4L, 3L, 4L, "Trainee", 987021L, beforeWindow, beforeWindow.plusHours(2));
      insertDqIssue(nextIssueId++, RULE_3_ID, 3L, 2L, 5L, "Trainee", 987022L, beforeWindow, beforeWindow.plusHours(3));

      // Unresolved issue under same-state Provider A2 detected before window -> isolated from Provider A1
      insertDqIssue(nextIssueId, RULE_1_ID, 2L, 4L, 1L, "Trainee", 987090L, beforeWindow, null);

      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .param("startDate", "2026-11-10")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(100))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(20))
          .andExpect(jsonPath("$.data.cleanEntitiesEstimate").value(80))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(80.00))
          .andExpect(jsonPath("$.data.cleanlinessScope").value("CURRENT_COHORT_ACTIVE_ISSUES"))
          .andExpect(jsonPath("$.data.periodMetricsScope").value("WINDOWED_ISSUE_ACTIVITY"))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(0))
          .andExpect(jsonPath("$.data.domainBreakdown[?(@.entityType=='Trainee')].eligibleCount").value(50))
          .andExpect(jsonPath("$.data.domainBreakdown[?(@.entityType=='Trainee')].distinctAffectedCount").value(20))
          .andExpect(jsonPath("$.data.domainBreakdown[?(@.entityType=='Trainee')].activeIssueCount").value(23))
          .andExpect(jsonPath("$.data.domainBreakdown[?(@.entityType=='Trainee')].cleanlinessIndexPct").value(60.00));

      // State A Admin rolls up Provider A1 (100/20) + Provider A2 (2/1) -> 102 eligible, 21 affected (81/102 = 79.41%)
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .param("startDate", "2026-11-10")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(102))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(21))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(79.41))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(0));
    }
  }

  @Nested
  @DisplayName("2. Import Metrics Endpoint Tests")
  class ImportMetricsTests {

    @Test
    @DisplayName("Computes batch status breakdown, mixed record ratios, timed throughput, windowing, and provider isolation accurately")
    void importMetricsStatusesRatiosThroughputAndIsolation() throws Exception {
      LocalDateTime t0 = LocalDateTime.of(2026, 11, 12, 8, 0, 0);

      // Provider B with zero batches -> zero counts & null ratios
      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-12")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalBatches").value(0))
          .andExpect(jsonPath("$.data.totalProcessedRecords").value(0))
          .andExpect(jsonPath("$.data.successRatioPct").doesNotExist())
          .andExpect(jsonPath("$.data.failureRatioPct").doesNotExist())
          .andExpect(jsonPath("$.data.processedRowsPerExecutionHour").doesNotExist());

      // Seed Provider A1 batches inside [2026-11-12, 2026-11-13):
      // Batch 1: COMPLETED, 6 records (3 SUCCESS, 1 FAILED, 1 DUPLICATE, 1 SKIPPED), elapsed = 2 hours -> execution rate = 3.00 rows/hr
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99101, 'IMP-OP-99101', 'SRC', 'Trainee', 3, ?, 6, 3, 1, ?, ?, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0), Timestamp.valueOf(t0.plusHours(2)), Timestamp.valueOf(t0));
      // Batch 2: PENDING, 0 records
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99102, 'IMP-OP-99102', 'SRC', 'Trainee', 1, ?, 0, 0, 0, NULL, NULL, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(1)));
      // Batch 3: RUNNING
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99103, 'IMP-OP-99103', 'SRC', 'Trainee', 2, ?, 0, 0, 0, ?, NULL, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(2)), Timestamp.valueOf(t0.plusHours(2)));
      // Batch 4: FAILED (must NEVER be counted as COMPLETED)
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99104, 'IMP-OP-99104', 'SRC', 'Trainee', 4, ?, 0, 0, 0, ?, ?, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(3)), Timestamp.valueOf(t0.plusHours(3).plusMinutes(10)), Timestamp.valueOf(t0.plusHours(3)));
      // Batch 5: CANCELLED
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99105, 'IMP-OP-99105', 'SRC', 'Trainee', 5, ?, 0, 0, 0, NULL, NULL, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(4)));
      // Batch 6: COMPLETED OUTSIDE window (yesterday) -> must be excluded by date filter
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99106, 'IMP-OP-99106', 'SRC', 'Trainee', 3, ?, 0, 0, 0, ?, ?, ?, NOW())",
          USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.minusDays(2)), Timestamp.valueOf(t0.minusDays(2).plusHours(1)), Timestamp.valueOf(t0.minusDays(2)));
      // Batch 7: COMPLETED under Provider A2 (same State A!) -> must NOT leak to Provider A1
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (99107, 'IMP-OP-99107', 'SRC', 'Trainee', 3, ?, 0, 0, 0, ?, ?, ?, NOW())",
          USER_PROVIDER_A2_ID, Timestamp.valueOf(t0), Timestamp.valueOf(t0.plusHours(1)), Timestamp.valueOf(t0));

      // Seed committed import_records for Batch 1 (3 SUCCESS=1, 1 FAILED=2, 1 SKIPPED=3, 1 DUPLICATE=4) processed at t0 + 30m
      Timestamp recTs = Timestamp.valueOf(t0.plusMinutes(30));
      jdbcTemplate.update(
          "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, created_at, updated_at) VALUES "
              + "(99101, 99101, 1, 'Trainee', 1, 1, ?, ?),"
              + "(99102, 99101, 2, 'Trainee', 2, 1, ?, ?),"
              + "(99103, 99101, 3, 'Trainee', 3, 1, ?, ?),"
              + "(99104, 99101, 4, 'Trainee', NULL, 2, ?, ?),"
              + "(99105, 99101, 5, 'Trainee', NULL, 3, ?, ?),"
              + "(99106, 99101, 6, 'Trainee', 1, 4, ?, ?)",
          recTs, recTs, recTs, recTs, recTs, recTs, recTs, recTs, recTs, recTs, recTs, recTs);

      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-12")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalBatches").value(5))
          .andExpect(jsonPath("$.data.activeBatches").value(2))
          .andExpect(jsonPath("$.data.terminalBatches").value(3))
          .andExpect(jsonPath("$.data.pendingBatches").value(1))
          .andExpect(jsonPath("$.data.runningBatches").value(1))
          .andExpect(jsonPath("$.data.completedBatches").value(1))
          .andExpect(jsonPath("$.data.failedBatches").value(1))
          .andExpect(jsonPath("$.data.cancelledBatches").value(1))
          .andExpect(jsonPath("$.data.totalProcessedRecords").value(6))
          .andExpect(jsonPath("$.data.successRecordCount").value(3))
          .andExpect(jsonPath("$.data.failedRecordCount").value(1))
          .andExpect(jsonPath("$.data.duplicateRecordCount").value(1))
          .andExpect(jsonPath("$.data.skippedRecordCount").value(1))
          .andExpect(jsonPath("$.data.successRatioPct").value(50.00))
          .andExpect(jsonPath("$.data.failureRatioPct").value(16.67))
          .andExpect(jsonPath("$.data.duplicateRatioPct").value(16.67))
          .andExpect(jsonPath("$.data.skippedRatioPct").value(16.67))
          .andExpect(jsonPath("$.data.completedBatchesInWindow").value(1))
          .andExpect(jsonPath("$.data.successfulRowsProcessedInWindow").value(3))
          .andExpect(jsonPath("$.data.timedCompletedBatchesCount").value(1))
          .andExpect(jsonPath("$.data.timedCompletedRecordsCount").value(6))
          .andExpect(jsonPath("$.data.processedRowsPerExecutionHour").value(3.00))
          .andExpect(jsonPath("$.data.windowDurationHours").value(24.00));

      // State A Admin aggregates Provider A1 (5) + Provider A2 (1) = 6 batches in window
      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-12")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalBatches").value(6))
          .andExpect(jsonPath("$.data.completedBatches").value(2))
          .andExpect(jsonPath("$.data.completedBatchesInWindow").value(2));
    }

    @Test
    @DisplayName("Separates creation cohort from completed_at/record-processing throughput across overnight, future-completed, partially recovered, and cross-provider batches")
    void importThroughputWindowingCrossDayAndRecovery() throws Exception {
      LocalDateTime yesterday = LocalDateTime.of(2026, 11, 11, 0, 0, 0);
      LocalDateTime today = LocalDateTime.of(2026, 11, 12, 0, 0, 0);
      LocalDateTime tomorrow = LocalDateTime.of(2026, 11, 13, 0, 0, 0);

      // 1. Batch 987101 (Provider A1): created YESTERDAY (22:00), started YESTERDAY (23:00), COMPLETED TODAY (01:00 -> 2h exec)
      //    4 total records (2 SUCCESS yesterday + 2 SUCCESS today) -> execution rate = 4 rows / 2h = 2.00 rows/hr
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (987101, 'IMP-OP-987101', 'SRC', 'Trainee', 3, ?, 4, 4, 0, ?, ?, ?, NOW())",
          USER_PROVIDER_A1_ID,
          Timestamp.valueOf(yesterday.plusHours(23)),
          Timestamp.valueOf(today.plusHours(1)),
          Timestamp.valueOf(yesterday.plusHours(22)));
      jdbcTemplate.update(
          "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, created_at, updated_at) VALUES "
              + "(987101, 987101, 1, 'Trainee', 1, 1, ?, ?),"
              + "(987102, 987101, 2, 'Trainee', 2, 1, ?, ?),"
              + "(987103, 987101, 3, 'Trainee', 3, 1, ?, ?),"
              + "(987104, 987101, 4, 'Trainee', 4, 1, ?, ?)",
          Timestamp.valueOf(yesterday.plusHours(23).plusMinutes(20)), Timestamp.valueOf(yesterday.plusHours(23).plusMinutes(20)),
          Timestamp.valueOf(yesterday.plusHours(23).plusMinutes(40)), Timestamp.valueOf(yesterday.plusHours(23).plusMinutes(40)),
          Timestamp.valueOf(today.plusMinutes(20)), Timestamp.valueOf(today.plusMinutes(20)),
          Timestamp.valueOf(today.plusMinutes(40)), Timestamp.valueOf(today.plusMinutes(40)));

      // 2. Batch 987102 (Provider A1): created TODAY (22:00), started TODAY (23:00), COMPLETED TOMORROW (01:00 -> 2h exec)
      //    5 mixed records processed TOMORROW (2 SUCCESS, 1 FAILED, 1 SKIPPED, 1 DUPLICATE) -> counts in TODAY creation cohort & ratios, NOT TODAY completion throughput!
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (987102, 'IMP-OP-987102', 'SRC', 'Trainee', 3, ?, 5, 2, 1, ?, ?, ?, NOW())",
          USER_PROVIDER_A1_ID,
          Timestamp.valueOf(today.plusHours(23)),
          Timestamp.valueOf(tomorrow.plusHours(1)),
          Timestamp.valueOf(today.plusHours(22)));
      Timestamp tmrwRec = Timestamp.valueOf(tomorrow.plusMinutes(30));
      jdbcTemplate.update(
          "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, created_at, updated_at) VALUES "
              + "(987105, 987102, 1, 'Trainee', 5, 1, ?, ?),"
              + "(987106, 987102, 2, 'Trainee', 6, 1, ?, ?),"
              + "(987107, 987102, 3, 'Trainee', NULL, 2, ?, ?),"
              + "(987108, 987102, 4, 'Trainee', NULL, 3, ?, ?),"
              + "(987109, 987102, 5, 'Trainee', 5, 4, ?, ?)",
          tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec, tmrwRec);

      // 3. Batch 987103 (Provider A1): partially processed & recovered batch (created YESTERDAY 10:00, still RUNNING today, completed_at NULL)
      //    Had 1 FAILED yesterday, then recovered chunk persisted 2 SUCCESS rows TODAY at 10:00 -> counts in today's successfulRowsProcessedInWindow, NOT completedBatchesInWindow
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (987103, 'IMP-OP-987103', 'SRC', 'Trainee', 2, ?, 3, 2, 1, ?, NULL, ?, NOW())",
          USER_PROVIDER_A1_ID,
          Timestamp.valueOf(yesterday.plusHours(10).plusMinutes(5)),
          Timestamp.valueOf(yesterday.plusHours(10)));
      Timestamp ystFail = Timestamp.valueOf(yesterday.plusHours(10).plusMinutes(10));
      Timestamp tdRec = Timestamp.valueOf(today.plusHours(10));
      jdbcTemplate.update(
          "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, created_at, updated_at) VALUES "
              + "(987110, 987103, 1, 'Trainee', NULL, 2, ?, ?),"
              + "(987111, 987103, 2, 'Trainee', 7, 1, ?, ?),"
              + "(987112, 987103, 3, 'Trainee', 8, 1, ?, ?)",
          ystFail, ystFail, tdRec, tdRec, tdRec, tdRec);

      // 4. Batch 987104 (same-state Provider A2): completed TODAY with 10 SUCCESS rows TODAY -> isolated from Provider A1
      jdbcTemplate.update(
          "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, started_at, completed_at, created_at, updated_at) "
              + "VALUES (987104, 'IMP-OP-987104', 'SRC', 'Trainee', 3, ?, 10, 10, 0, ?, ?, ?, NOW())",
          USER_PROVIDER_A2_ID,
          Timestamp.valueOf(today.plusHours(5)),
          Timestamp.valueOf(today.plusHours(6)),
          Timestamp.valueOf(yesterday.plusHours(20)));
      Timestamp a2Rec = Timestamp.valueOf(today.plusHours(5).plusMinutes(30));
      jdbcTemplate.update(
          "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, created_at, updated_at) VALUES "
              + "(987113, 987104, 1, 'Trainee', 10, 1, ?, ?),"
              + "(987114, 987104, 2, 'Trainee', 11, 1, ?, ?)",
          a2Rec, a2Rec, a2Rec, a2Rec);

      // Today [2026-11-12, 2026-11-13) for Provider A1:
      // - Creation cohort: Batch 987102 only (1 batch, 5 mixed records -> 40.00% / 20.00% / 20.00% / 20.00%)
      // - Completion throughput: Batch 987101 only (1 completed in window -> 0.04 batches/hr; 4 records over 2h -> 2.00 rows/hr)
      // - Successful row throughput: 2 rows from Batch 987101 + 2 recovered rows from Batch 987103 = 4 rows -> 4 / 24 = 0.17 rows/hr
      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-12")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalBatches").value(1))
          .andExpect(jsonPath("$.data.completedBatches").value(1))
          .andExpect(jsonPath("$.data.totalProcessedRecords").value(5))
          .andExpect(jsonPath("$.data.successRatioPct").value(40.00))
          .andExpect(jsonPath("$.data.failureRatioPct").value(20.00))
          .andExpect(jsonPath("$.data.duplicateRatioPct").value(20.00))
          .andExpect(jsonPath("$.data.skippedRatioPct").value(20.00))
          .andExpect(jsonPath("$.data.completedBatchesInWindow").value(1))
          .andExpect(jsonPath("$.data.batchesPerWindowHour").value(0.04))
          .andExpect(jsonPath("$.data.successfulRowsProcessedInWindow").value(4))
          .andExpect(jsonPath("$.data.successfulRowsPerWindowHour").value(0.17))
          .andExpect(jsonPath("$.data.timedCompletedBatchesCount").value(1))
          .andExpect(jsonPath("$.data.timedCompletedRecordsCount").value(4))
          .andExpect(jsonPath("$.data.processedRowsPerExecutionHour").value(2.00));

      // Tomorrow [2026-11-13, 2026-11-14) for Provider A1:
      // - Creation cohort: 0 batches, 0 records, null ratios
      // - Completion throughput: Batch 987102 (1 batch, 5 records over 2h -> 2.50 rows/hr; 2 SUCCESS rows -> 2 / 24 = 0.08 rows/hr)
      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-13")
              .param("endDate", "2026-11-13")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalBatches").value(0))
          .andExpect(jsonPath("$.data.totalProcessedRecords").value(0))
          .andExpect(jsonPath("$.data.successRatioPct").doesNotExist())
          .andExpect(jsonPath("$.data.completedBatchesInWindow").value(1))
          .andExpect(jsonPath("$.data.successfulRowsProcessedInWindow").value(2))
          .andExpect(jsonPath("$.data.successfulRowsPerWindowHour").value(0.08))
          .andExpect(jsonPath("$.data.timedCompletedBatchesCount").value(1))
          .andExpect(jsonPath("$.data.timedCompletedRecordsCount").value(5))
          .andExpect(jsonPath("$.data.processedRowsPerExecutionHour").value(2.50));

      // State A Admin for Today combines Provider A1 (1 completed, 4 SUCCESS rows) + Provider A2 (1 completed, 2 SUCCESS rows)
      mockMvc.perform(get("/api/v1/operational/import-metrics")
              .param("startDate", "2026-11-12")
              .param("endDate", "2026-11-12")
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.completedBatchesInWindow").value(2))
          .andExpect(jsonPath("$.data.successfulRowsProcessedInWindow").value(6));
    }
  }

  @Nested
  @DisplayName("3. System Health, RBAC & Filter Validation Tests")
  class SystemHealthAndSecurityTests {

    @Test
    @DisplayName("Separates CRITICAL vs ERROR velocity, distinguishes audit mutations vs exports, enforces half-open intervals, RBAC, and tenant isolation")
    void systemHealthVelocityAuditMutationsRbacAndTenancy() throws Exception {
      LocalDateTime t0 = LocalDateTime.of(2026, 11, 12, 10, 0, 0);

      // Seed Provider A1 operational events inside [10:00, 12:00) (2.00 hours)
      // - 3 CRITICAL (category INTEGRATION=2, severity CRITICAL=4) -> 3 / 2.00h = 1.50 events/hr
      // - 1 ERROR (category BATCH_JOB=3, severity ERROR=3) -> 1 / 2.00h = 0.50 events/hr
      // - 1 WARNING correlated outside [10:00, 12:00) at 12:00:00 exact endExclusive -> excluded!
      jdbcTemplate.update(
          "INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, actor_user_id, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) VALUES "
              + "(99101, 2, 4, 'OP287_CRIT_1', ?, 'TrainingProvider', ?, 'DQ', '[cid=c1|rid=r1] crit 1', ?, NOW()),"
              + "(99102, 2, 4, 'OP287_CRIT_2', ?, 'TrainingProvider', ?, 'DQ', '[cid=c2|rid=r2] crit 2', ?, NOW()),"
              + "(99103, 1, 4, 'OP287_CRIT_3', ?, 'TrainingProvider', ?, 'SEC', 'crit 3 uncorr', ?, NOW()),"
              + "(99104, 3, 3, 'OP287_ERR_1', ?, 'TrainingProvider', ?, 'IMP', '[cid=c4|rid=r4] err 1', ?, NOW()),"
              + "(99105, 5, 2, 'OP287_OUTSIDE', ?, 'TrainingProvider', ?, 'API', '[cid=c5|rid=r5] edge', ?, NOW())",
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0),
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(30)),
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(45)),
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(1)),
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusHours(2))); // exactly endExclusive

      // Cross-tenant CRITICAL event under Provider B inside [10:00, 12:00)
      jdbcTemplate.update(
          "INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, actor_user_id, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) "
              + "VALUES (99106, 2, 4, 'OP287_PROVB_CRIT', ?, 'TrainingProvider', ?, 'DQ', '[cid=cb|rid=rb] prov B crit', ?, NOW())",
          USER_PROVIDER_B_ID, PROVIDER_B_ID, Timestamp.valueOf(t0.plusMinutes(15)));

      // Seed Audit logs under Provider A1: 3 mutations on 2 distinct entities (CREATE=1 on tp, UPDATE=2 x2 on user) + 1 EXPORT=7 (excluded from mutation volume!)
      jdbcTemplate.update(
          "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, correlation_id, change_summary, created_at) VALUES "
              + "(99101, ?, 1, 'TrainingProvider', ?, ?, 'c1', 'created', NOW()),"
              + "(99102, ?, 2, 'User', ?, ?, 'c2', 'updated 1', NOW()),"
              + "(99103, ?, 2, 'User', ?, ?, 'c3', 'updated 2', NOW()),"
              + "(99104, ?, 7, 'TrainingProvider', ?, ?, 'c4', 'exported non-mutation', NOW())",
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(10)),
          USER_PROVIDER_A1_ID, USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(20)),
          USER_PROVIDER_A1_ID, USER_PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(30)),
          USER_PROVIDER_A1_ID, PROVIDER_A1_ID, Timestamp.valueOf(t0.plusMinutes(40)));

      // Cross-tenant audit mutation under Provider B
      jdbcTemplate.update(
          "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, correlation_id, change_summary, created_at) "
              + "VALUES (99105, ?, 1, 'TrainingProvider', ?, ?, 'cb', 'prov B mut', NOW())",
          USER_PROVIDER_B_ID, PROVIDER_B_ID, Timestamp.valueOf(t0.plusMinutes(15)));

      mockMvc.perform(get("/api/v1/operational/system-health")
              .param("startDate", "2026-11-12T10:00:00")
              .param("endDate", "2026-11-12T12:00:00")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.totalIncidents").value(4))
          .andExpect(jsonPath("$.data.criticalEventCount").value(3))
          .andExpect(jsonPath("$.data.errorEventCount").value(1))
          .andExpect(jsonPath("$.data.correlatedIncidentsCount").value(3))
          .andExpect(jsonPath("$.data.incidentsByCategory.INTEGRATION").value(2))
          .andExpect(jsonPath("$.data.incidentsByCategory.SECURITY").value(1))
          .andExpect(jsonPath("$.data.incidentsByCategory.BATCH_JOB").value(1))
          .andExpect(jsonPath("$.data.windowDurationHours").value(2.00))
          .andExpect(jsonPath("$.data.criticalEventsPerHour").value(1.50))
          .andExpect(jsonPath("$.data.errorEventsPerHour").value(0.50))
          .andExpect(jsonPath("$.data.auditMutationVolume").value(3))
          .andExpect(jsonPath("$.data.distinctEntitiesModified").value(2))
          .andExpect(jsonPath("$.data.auditNonMutationCount").value(1));

      // Nationwide Admin sees Provider A1 + Provider B (4 CRITICAL, 1 ERROR -> 2.00/hr, 4 audit mutations)
      mockMvc.perform(get("/api/v1/operational/system-health")
              .param("startDate", "2026-11-12T10:00:00")
              .param("endDate", "2026-11-12T12:00:00")
              .param("trainingProviderId", String.valueOf(PROVIDER_A1_ID))
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.criticalEventCount").value(3))
          .andExpect(jsonPath("$.data.auditMutationVolume").value(3));

      // Unauthenticated -> 401; Trainee & Unscoped & Cross-Tenant Filter -> 403; Invalid date range -> 400
      mockMvc.perform(get("/api/v1/operational/system-health"))
          .andExpect(status().isUnauthorized());

      for (String path : List.of("/api/v1/operational/data-quality-scorecard", "/api/v1/operational/import-metrics", "/api/v1/operational/system-health")) {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, tokenTrainee))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, tokenUnscoped))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(path)
                .param("stateId", String.valueOf(STATE_B_ID))
                .header(HttpHeaders.AUTHORIZATION, tokenStateA))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(path)
                .param("trainingProviderId", String.valueOf(PROVIDER_A2_ID))
                .header(HttpHeaders.AUTHORIZATION, tokenProviderA1))
            .andExpect(status().isForbidden());
        mockMvc.perform(get(path)
                .param("startDate", "2026-11-15")
                .param("endDate", "2026-11-01")
                .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        mockMvc.perform(get(path)
                .param("unexpectedParam", "1")
                .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_PARAMETER"));
        mockMvc.perform(get(path)
                .param("stateId", String.valueOf(STATE_A_ID))
                .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
            .andExpect(status().isForbidden());
      }
    }

    @Test
    @DisplayName("Employer scorecard and import metrics strictly restrict to employer-owned Placement/Employment and ImportBatches")
    void employerScopeIsolationTest() throws Exception {
      seedTraineeWithEnrollment(TRAINEE_1_ID, ENROLLMENT_1_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID, STATE_A_ID, DISTRICT_A1_ID);
      jdbcTemplate.update(
          "INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, currency_code, work_state_id, work_district_id, record_verification_status_id, created_at, updated_at, deleted_at) "
              + "VALUES (?, 'OP287-PLC-1', ?, ?, ?, ?, ?, ?, 1, 1, 1, 1, 'INR', ?, ?, 1, NOW(), NOW(), NULL)",
          PLACEMENT_EMP_A_ID, TRAINEE_1_ID, ENROLLMENT_1_ID, COURSE_ID, PROGRAM_ID, PROVIDER_A1_ID, EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID);

      LocalDateTime now = LocalDateTime.now();
      // 1 active Trainee issue (must NOT be visible to Employer A) + 1 active PlacementRecord issue (MUST be visible to Employer A)
      insertDqIssue(99101L, RULE_1_ID, 2L, 3L, 1L, "Trainee", TRAINEE_1_ID, now, null);
      insertDqIssue(99102L, RULE_2_ID, 4L, 4L, 1L, "PlacementRecord", PLACEMENT_EMP_A_ID, now, null);

      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eligiblePopulation").value(1))
          .andExpect(jsonPath("$.data.distinctAffectedEntities").value(1))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").value(0.00))
          .andExpect(jsonPath("$.data.totalActiveIssues").value(1))
          .andExpect(jsonPath("$.data.activeIssuesBySeverity.CRITICAL").value(1));
    }
  }
}
