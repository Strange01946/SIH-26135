package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataQualityIssueService;
import in.gov.sih.sih26135.service.dq.DataQualityIssueRegistrationService;
import in.gov.sih.sih26135.service.dq.RegistrationResult;
import in.gov.sih.sih26135.service.dq.RegistrationStatus;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.4 - Generic Issue Tenant Isolation & Creation Lifecycle Security Tests")
class DataQualityGenericIssueSecurityApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private DataQualityIssueService dataQualityIssueService;

  @Autowired
  private DataQualityIssueRegistrationService registrationService;

  @Autowired
  private UserRepository userRepository;

  private String nationwideAdminToken;
  private String stateAAdminToken;
  private String stateAReadOnlyToken;
  private String providerAToken;
  private String employerAToken;
  private String unscopedToken;
  private String ambiguousStateProviderToken;
  private String conflictingProviderEmployerToken;

  private Long defaultRuleId;
  private Long defaultCategoryId;
  private Long defaultSeverityId;
  private Long defaultSourceId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    catalogService.ensureDefaultRulesProvisioned();
    cleanUpTestFixtures();

    // Provision States & Districts (9961 = Tenant A, 9962 = Tenant B)
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 'G1', 'Generic DQ State A', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9962, 'G2', 'Generic DQ State B', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 9961, 'GD61', 'Generic DQ District A', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9962, 9962, 'GD62', 'Generic DQ District B', 1, NOW(), NOW())");

    // Sector, Course, Organization, Department, Scheme, Program
    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 'SEC-GDQ-9961', 'Generic DQ Sector', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'CRS-GDQ-9961', 'Generic DQ Course', 9961, 1, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 'ORG-GDQ-9961', 'Generic DQ Org', 1, 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 9961, 'DEP-GDQ-9961', 'Generic DQ Dept', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 'SCH-GDQ-9961', 'Generic DQ Scheme', 9961, 'INR', 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9961, 'PRG-GDQ-9961', 'Generic DQ Program', 9961, 9961, 1, NOW(), NOW())");

    // Training Providers & Employers (Tenant A = 9961, Tenant B = 9962)
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'PRV-GDQ-9961', 'Provider A', 'REG-PRV-9961', 1, 9961, 9961, 1, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'PRV-GDQ-9962', 'Provider B', 'REG-PRV-9962', 1, 9962, 9962, 1, 1, NOW(), NOW(), NULL)");

    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'EMP-GDQ-9961', 'Employer A', 'REG-EMP-9961', 1, 9961, 9961, 1, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'EMP-GDQ-9962', 'Employer B', 'REG-EMP-9962', 1, 9962, 9962, 1, 1, NOW(), NOW(), NULL)");

    // Centers, Batches, Trainees, Enrollments, PlacementRecords
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 9961, 'CTR-GDQ-9961', 'Center A', 9961, 9961, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 9962, 'CTR-GDQ-9962', 'Center B', 9962, 9962, 1, NOW(), NOW(), NULL)");

    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'BAT-GDQ-9961', 9961, 9961, 9961, 9961, '2026-01-01', 30, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'BAT-GDQ-9962', 9961, 9962, 9962, 9961, '2026-01-01', 30, 1, NOW(), NOW(), NULL)");

    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'GDQ-TR-9961', 'TraineeA', 'TenantA', '9800009961', 'gdq9961@test.gov.in', '2000-01-01', 1, 9961, 9961, 1, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'GDQ-TR-9962', 'TraineeB', 'TenantB', '9800009962', 'gdq9962@test.gov.in', '2000-01-01', 1, 9962, 9962, 1, 1, NOW(), NOW(), NULL)");

    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'ENR-GDQ-9961', 9961, 9961, 9961, 9961, 9961, 9961, '2026-01-01', 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'ENR-GDQ-9962', 9962, 9961, 9961, 9962, 9962, 9962, '2026-01-01', 1, NOW(), NOW(), NULL)");

    jdbcTemplate.update(
        "INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, work_state_id, work_district_id, record_verification_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9961, 'PLC-GDQ-9961', 9961, 9961, 9961, 9961, 9961, 9961, 1, 1, 1, 1, 25000.00, 9961, 9961, 1, NOW(), NOW(), NULL)");
    jdbcTemplate.update(
        "INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, work_state_id, work_district_id, record_verification_status_id, created_at, updated_at, deleted_at) " +
            "VALUES (9962, 'PLC-GDQ-9962', 9962, 9962, 9961, 9961, 9962, 9962, 1, 1, 1, 1, 25000.00, 9962, 9962, 1, NOW(), NOW(), NULL)");

    // Users
    // 9960: Verified Nationwide Admin
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9960, 'gdq_superadmin', 'gdq9960@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())");
    // 9961: State A User (State 9961)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at) " +
            "VALUES (9961, 'gdq_state_a', 'gdq9961u@test.gov.in', 'hash', 'argon2id', 1, 9961, NOW(), NOW())");
    // 9962: State B User (State 9962)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at) " +
            "VALUES (9962, 'gdq_state_b', 'gdq9962u@test.gov.in', 'hash', 'argon2id', 1, 9962, NOW(), NOW())");
    // 9963: Provider A User (Provider 9961)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, created_at, updated_at) " +
            "VALUES (9963, 'gdq_provider_a', 'gdq9963@test.gov.in', 'hash', 'argon2id', 1, 9961, NOW(), NOW())");
    // 9964: Employer A User (Employer 9961)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at) " +
            "VALUES (9964, 'gdq_employer_a', 'gdq9964@test.gov.in', 'hash', 'argon2id', 1, 9961, NOW(), NOW())");
    // 9965: Unscoped User (all null tenancy, holds system.manage & audit.read without admin role)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9965, 'gdq_unscoped', 'gdq9965@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())");
    // 9966: Ambiguous User (state_id=9961 AND training_provider_id=9961)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, training_provider_id, created_at, updated_at) " +
            "VALUES (9966, 'gdq_ambiguous_sp', 'gdq9966@test.gov.in', 'hash', 'argon2id', 1, 9961, 9961, NOW(), NOW())");
    // 9967: Conflicting User (training_provider_id=9961 AND employer_id=9961)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, training_provider_id, employer_id, created_at, updated_at) " +
            "VALUES (9967, 'gdq_conflict_pe', 'gdq9967@test.gov.in', 'hash', 'argon2id', 1, 9961, 9961, NOW(), NOW())");

    nationwideAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9960L, "gdq_superadmin", List.of("ROLE_super_admin", "audit.read", "system.manage"));
    stateAAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9961L, "gdq_state_a", List.of("ROLE_state_admin", "audit.read", "system.manage"));
    stateAReadOnlyToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9961L, "gdq_state_a", List.of("ROLE_state_admin", "audit.read"));
    providerAToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9963L, "gdq_provider_a", List.of("ROLE_training_provider", "audit.read", "system.manage"));
    employerAToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9964L, "gdq_employer_a", List.of("ROLE_employer", "audit.read", "system.manage"));
    unscopedToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9965L, "gdq_unscoped", List.of("audit.read", "system.manage"));
    ambiguousStateProviderToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9966L, "gdq_ambiguous_sp", List.of("ROLE_state_admin", "audit.read", "system.manage"));
    conflictingProviderEmployerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9967L, "gdq_conflict_pe", List.of("ROLE_training_provider", "audit.read", "system.manage"));

    defaultRuleId = jdbcTemplate.queryForObject("SELECT MIN(data_quality_rule_id) FROM data_quality_rules", Long.class);
    defaultCategoryId = jdbcTemplate.queryForObject("SELECT MIN(data_quality_category_id) FROM ref_data_quality_category", Long.class);
    defaultSeverityId = jdbcTemplate.queryForObject("SELECT MIN(data_quality_severity_id) FROM ref_data_quality_severity", Long.class);
    defaultSourceId = jdbcTemplate.queryForObject("SELECT MIN(data_quality_detection_source_id) FROM ref_data_quality_detection_source", Long.class);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpTestFixtures();
  }

  private void cleanUpTestFixtures() {
    try {
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE changed_by_user_id BETWEEN 9960 AND 9969");
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_id IN (SELECT data_quality_issue_id FROM data_quality_issues WHERE issue_summary LIKE 'GDQ-TEST-%')");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE issue_summary LIKE 'GDQ-TEST-%'");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE entity_type = 'Trainee' AND entity_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM placement_records WHERE placement_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 9960 AND 9969");
      jdbcTemplate.update("DELETE FROM programs WHERE program_id = 9961");
      jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = 9961");
      jdbcTemplate.update("DELETE FROM departments WHERE department_id = 9961");
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = 9961");
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = 9961");
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = 9961");
      jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (9961, 9962)");
      jdbcTemplate.update("DELETE FROM states WHERE state_id IN (9961, 9962)");
    } catch (Exception ignored) {
    }
  }

  private Long insertIssue(String statusCode, String entityType, Long entityId, String summary, Long assignedUserId, Long resolvedByUserId) {
    Long statusId = getStatusId(statusCode);
    org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      java.sql.PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO data_quality_issues (" +
              "data_quality_rule_id, data_quality_category_id, data_quality_severity_id, " +
              "data_quality_issue_status_id, data_quality_detection_source_id, entity_type, " +
              "entity_id, issue_summary, detected_at, assigned_user_id, resolved_by_user_id, created_at, updated_at) " +
              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW() - INTERVAL 1 HOUR, ?, ?, NOW(), NOW())",
          java.sql.Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, defaultRuleId);
      ps.setLong(2, defaultCategoryId);
      ps.setLong(3, defaultSeverityId);
      ps.setLong(4, statusId);
      ps.setLong(5, defaultSourceId);
      ps.setString(6, entityType);
      if (entityId != null) {
        ps.setLong(7, entityId);
      } else {
        ps.setNull(7, java.sql.Types.BIGINT);
      }
      ps.setString(8, summary);
      if (assignedUserId != null) {
        ps.setLong(9, assignedUserId);
      } else {
        ps.setNull(9, java.sql.Types.BIGINT);
      }
      if (resolvedByUserId != null) {
        ps.setLong(10, resolvedByUserId);
      } else {
        ps.setNull(10, java.sql.Types.BIGINT);
      }
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  private Long getStatusId(String statusCode) {
    return jdbcTemplate.queryForObject(
        "SELECT data_quality_issue_status_id FROM ref_data_quality_issue_status WHERE status_code = ?",
        Long.class, statusCode
    );
  }

  @Nested
  @DisplayName("1. Tenant Isolation in Generic Issue Reads, Filters, Creation and Updates")
  class GenericIssueTenantIsolationTests {

    @Test
    @DisplayName("GET /{id}: Scoped callers read own tenant issues (200) but get 403 Forbidden for cross-tenant issues even with audit.read/system.manage")
    void shouldEnforceTenantIsolationOnGetById() throws Exception {
      Long traineeAIssueId = insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-GET-TA", null, null);
      Long traineeBIssueId = insertIssue("OPEN", "Trainee", 9962L, "GDQ-TEST-GET-TB", null, null);
      Long placementAIssueId = insertIssue("OPEN", "PlacementRecord", 9961L, "GDQ-TEST-GET-PA", null, null);
      Long placementBIssueId = insertIssue("OPEN", "PlacementRecord", 9962L, "GDQ-TEST-GET-PB", null, null);

      // Nationwide admin reads across all tenants
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeBIssueId)
              .header("Authorization", nationwideAdminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.issueSummary", is("GDQ-TEST-GET-TB")));

      // State A read-only user reads State A issue, rejected on State B issue (zero metadata leak)
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeAIssueId)
              .header("Authorization", stateAReadOnlyToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id", is(traineeAIssueId.intValue())));

      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeBIssueId)
              .header("Authorization", stateAAdminToken))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.data").doesNotExist());

      // Provider A reads own Trainee A & Placement A, rejected on B
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeAIssueId)
              .header("Authorization", providerAToken))
          .andExpect(status().isOk());
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeBIssueId)
              .header("Authorization", providerAToken))
          .andExpect(status().isForbidden());

      // Employer A reads own Placement A, rejected on Placement B and Trainee A
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", placementAIssueId)
              .header("Authorization", employerAToken))
          .andExpect(status().isOk());
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", placementBIssueId)
              .header("Authorization", employerAToken))
          .andExpect(status().isForbidden());
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeAIssueId)
              .header("Authorization", employerAToken))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET ?entityType=...&entityId=... enforces entity tenant scope and rejects cross-tenant probing with 403")
    void shouldEnforceTenantIsolationOnCompoundEntityLookup() throws Exception {
      insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-ENT-A", null, null);
      insertIssue("OPEN", "Trainee", 9962L, "GDQ-TEST-ENT-B", null, null);

      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("entityType", "Trainee")
              .param("entityId", "9961")
              .header("Authorization", stateAReadOnlyToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].issueSummary", is("GDQ-TEST-ENT-A")));

      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("entityType", "Trainee")
              .param("entityId", "9962")
              .header("Authorization", stateAAdminToken))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("entityType", "Trainee")
              .param("entityId", "9961")
              .header("Authorization", employerAToken))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Generic collection search/filter queries NEVER return out-of-scope records across rule, category, severity, status, source, entityType, assignedUser, and resolvedByUser filters")
    void shouldFilterAllGenericCollectionQueriesStrictlyByCallerScope() throws Exception {
      Long traineeAIssueId = insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-COL-TA", 9961L, 9960L);
      Long traineeBIssueId = insertIssue("OPEN", "Trainee", 9962L, "GDQ-TEST-COL-TB", 9962L, 9960L);
      Long placementAIssueId = insertIssue("OPEN", "PlacementRecord", 9961L, "GDQ-TEST-COL-PA", 9961L, 9960L);
      Long placementBIssueId = insertIssue("OPEN", "PlacementRecord", 9962L, "GDQ-TEST-COL-PB", 9962L, 9960L);
      Long openStatusId = getStatusId("OPEN");

      // Nationwide admin sees both tenant A and tenant B records
      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("statusId", openStatusId.toString())
              .header("Authorization", nationwideAdminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[*].id", hasItem(traineeAIssueId.intValue())))
          .andExpect(jsonPath("$.data[*].id", hasItem(traineeBIssueId.intValue())))
          .andExpect(jsonPath("$.data[*].id", hasItem(placementBIssueId.intValue())));

      // State A caller checks all collection filter endpoints: Tenant B records MUST NEVER appear
      String[][] stateFilterParams = {
          {"ruleId", defaultRuleId.toString()},
          {"categoryId", defaultCategoryId.toString()},
          {"severityId", defaultSeverityId.toString()},
          {"statusId", openStatusId.toString()},
          {"sourceId", defaultSourceId.toString()},
          {"entityType", "Trainee"},
          {"resolvedByUserId", "9960"}
      };
      for (String[] pair : stateFilterParams) {
        mockMvc.perform(get("/api/v1/data-quality-issues")
                .param(pair[0], pair[1])
                .header("Authorization", stateAReadOnlyToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].id", hasItem(traineeAIssueId.intValue())))
            .andExpect(jsonPath("$.data[*].id", not(hasItem(traineeBIssueId.intValue()))))
            .andExpect(jsonPath("$.data[*].id", not(hasItem(placementBIssueId.intValue()))));
      }

      // Query by assignedUserId=9962 (State B user): State A caller gets ZERO Tenant B records
      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("assignedUserId", "9962")
              .header("Authorization", stateAReadOnlyToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[*].id", not(hasItem(traineeBIssueId.intValue()))))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(placementBIssueId.intValue()))));

      // Provider A sees Trainee A and Placement A, NEVER Tenant B
      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("statusId", openStatusId.toString())
              .header("Authorization", providerAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[*].id", hasItem(traineeAIssueId.intValue())))
          .andExpect(jsonPath("$.data[*].id", hasItem(placementAIssueId.intValue())))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(traineeBIssueId.intValue()))))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(placementBIssueId.intValue()))));

      // Employer A sees ONLY Placement A (no Trainee A, no Placement B)
      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("statusId", openStatusId.toString())
              .header("Authorization", employerAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[*].id", hasItem(placementAIssueId.intValue())))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(traineeAIssueId.intValue()))))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(traineeBIssueId.intValue()))))
          .andExpect(jsonPath("$.data[*].id", not(hasItem(placementBIssueId.intValue()))));

      // Employer querying Trainee entityType fails closed with 403 Forbidden
      mockMvc.perform(get("/api/v1/data-quality-issues")
              .param("entityType", "Trainee")
              .header("Authorization", employerAToken))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST / and PUT /{id}: Scoped callers with system.manage cannot create or update issues for another tenant's entity (403)")
    void shouldRejectCrossTenantCreateAndUpdate() throws Exception {
      Long traineeBIssueId = insertIssue("OPEN", "Trainee", 9962L, "GDQ-TEST-ORIG-B", null, null);
      Long traineeAIssueId = insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-ORIG-A", null, null);

      // 1. State A Admin attempts POST / targeting State B Trainee 9962 -> 403 Forbidden
      CreateDataQualityIssueRequest crossTenantCreate = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-CROSS-CREATE");
      crossTenantCreate.setEntityId(9962L);

      mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(crossTenantCreate)))
          .andExpect(status().isForbidden());

      Integer crossCreateCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issues WHERE issue_summary = 'GDQ-TEST-CROSS-CREATE'", Integer.class);
      assertThat(crossCreateCount).isZero();

      // 2. Employer A attempts POST / targeting Trainee 9961 -> 403 Forbidden
      CreateDataQualityIssueRequest empTraineeCreate = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-EMP-TRAINEE");
      empTraineeCreate.setEntityId(9961L);

      mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", employerAToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(empTraineeCreate)))
          .andExpect(status().isForbidden());

      // 3. Employer A CAN create PlacementRecord issue for own Employer 9961 Placement -> 201 Created
      CreateDataQualityIssueRequest empPlacementCreate = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "PlacementRecord", "GDQ-TEST-EMP-PLACEMENT-OK");
      empPlacementCreate.setEntityId(9961L);

      mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", employerAToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(empPlacementCreate)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("OPEN")));

      // 4. State A Admin attempts PUT /{id} targeting State B issue -> 403 Forbidden, unchanged in DB
      UpdateDataQualityIssueRequest updateReq = new UpdateDataQualityIssueRequest();
      updateReq.setIssueSummary("GDQ-TEST-TAMPERED-B");

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", traineeBIssueId)
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateReq)))
          .andExpect(status().isForbidden());

      String preservedSummary = jdbcTemplate.queryForObject(
          "SELECT issue_summary FROM data_quality_issues WHERE data_quality_issue_id = ?", String.class, traineeBIssueId);
      assertThat(preservedSummary).isEqualTo("GDQ-TEST-ORIG-B");

      // 5. State A Admin CAN update own State A issue summary -> 200 OK
      UpdateDataQualityIssueRequest validUpdate = new UpdateDataQualityIssueRequest();
      validUpdate.setIssueSummary("GDQ-TEST-UPDATED-A");

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", traineeAIssueId)
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validUpdate)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.issueSummary", is("GDQ-TEST-UPDATED-A")));
    }

    @Test
    @DisplayName("Missing or ambiguous caller tenancy scopes MUST fail closed with 403 Forbidden across all generic endpoints")
    void shouldFailClosedForMissingOrAmbiguousScope() throws Exception {
      Long traineeAIssueId = insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-FAILCLOSED", null, null);
      Long openStatusId = getStatusId("OPEN");

      CreateDataQualityIssueRequest createReq = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-FAILCLOSED-CREATE");
      createReq.setEntityId(9961L);

      UpdateDataQualityIssueRequest updateReq = new UpdateDataQualityIssueRequest();
      updateReq.setIssueSummary("GDQ-TEST-FAILCLOSED-UPD");

      for (String badToken : List.of(unscopedToken, ambiguousStateProviderToken, conflictingProviderEmployerToken)) {
        mockMvc.perform(get("/api/v1/data-quality-issues/{id}", traineeAIssueId)
                .header("Authorization", badToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/data-quality-issues")
                .param("statusId", openStatusId.toString())
                .header("Authorization", badToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/data-quality-issues")
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/data-quality-issues/{id}", traineeAIssueId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isForbidden());
      }
    }

    @Test
    @DisplayName("Service-boundary enforcement: Direct DataQualityIssueService invocation enforces tenant boundaries without controller interceptors")
    void shouldEnforceTenantBoundariesAtServiceBoundary() {
      Long traineeAIssueId = insertIssue("OPEN", "Trainee", 9961L, "GDQ-TEST-SVC-A", null, null);
      Long traineeBIssueId = insertIssue("OPEN", "Trainee", 9962L, "GDQ-TEST-SVC-B", null, null);

      User stateAUser = userRepository.findById(9961L).orElseThrow();

      // 1. Unauthenticated direct service call fails closed
      SecurityContextHolder.clearContext();
      assertThatThrownBy(() -> dataQualityIssueService.getIssueById(traineeAIssueId))
          .isInstanceOf(AccessDeniedException.class);

      // 2. State A caller with system.manage + audit.read calling service directly
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              new in.gov.sih.sih26135.security.UserPrincipal(stateAUser.getId(), stateAUser.getUsername()),
              null,
              List.of(
                  new SimpleGrantedAuthority("ROLE_state_admin"),
                  new SimpleGrantedAuthority("audit.read"),
                  new SimpleGrantedAuthority("system.manage")
              )
          )
      );

      // Allowed for State A
      DataQualityIssueResponse ownIssue = dataQualityIssueService.getIssueById(traineeAIssueId);
      assertThat(ownIssue.id()).isEqualTo(traineeAIssueId);

      // Rejected for State B
      assertThatThrownBy(() -> dataQualityIssueService.getIssueById(traineeBIssueId))
          .isInstanceOf(AccessDeniedException.class);

      UpdateDataQualityIssueRequest upd = new UpdateDataQualityIssueRequest();
      upd.setIssueSummary("GDQ-TEST-SVC-BYPASS");
      assertThatThrownBy(() -> dataQualityIssueService.updateIssue(traineeBIssueId, upd))
          .isInstanceOf(AccessDeniedException.class);

      // Direct service legacy assign/resolve ALSO enforce scope (rejected for State B)
      assertThatThrownBy(() -> dataQualityIssueService.assignIssue(traineeBIssueId, 9962L))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(() -> dataQualityIssueService.resolveIssue(traineeBIssueId, new ResolveDataQualityIssueRequest("Notes")))
          .isInstanceOf(AccessDeniedException.class);

      // Collection filter directly excludes State B record
      List<DataQualityIssueResponse> filtered = dataQualityIssueService.getIssuesByStatusId(getStatusId("OPEN"));
      assertThat(filtered).extracting(DataQualityIssueResponse::id).contains(traineeAIssueId);
      assertThat(filtered).extracting(DataQualityIssueResponse::id).doesNotContain(traineeBIssueId);
    }
  }

  @Nested
  @DisplayName("2. Lifecycle Bypass Prevention During Issue Creation (POST /)")
  class CreationLifecycleBypassPreventionTests {

    @Test
    @DisplayName("Should reject POST creation specifying non-OPEN statuses: ASSIGNED, IN_PROGRESS, REOPENED, RESOLVED, DISMISSED")
    void shouldRejectNonOpenStatusesDuringCreation() throws Exception {
      for (String prohibitedStatus : List.of("ASSIGNED", "IN_PROGRESS", "REOPENED", "RESOLVED", "DISMISSED")) {
        Long statusId = getStatusId(prohibitedStatus);
        String summary = "GDQ-TEST-BYPASS-STATUS-" + prohibitedStatus;

        CreateDataQualityIssueRequest request = new CreateDataQualityIssueRequest(
            defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", summary);
        request.setEntityId(9961L);
        request.setDataQualityIssueStatusId(statusId);

        mockMvc.perform(post("/api/v1/data-quality-issues")
                .header("Authorization", nationwideAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("LIFECYCLE_BYPASS_PROHIBITED")));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_quality_issues WHERE issue_summary = ?", Integer.class, summary);
        assertThat(count).isZero();
      }
    }

    @Test
    @DisplayName("Should reject POST creation specifying assignedUserId even when status is omitted or OPEN")
    void shouldRejectAssignedUserIdDuringCreation() throws Exception {
      CreateDataQualityIssueRequest reqNullStatus = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-BYPASS-ASSIGN-1");
      reqNullStatus.setEntityId(9961L);
      reqNullStatus.setAssignedUserId(9961L);

      mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", nationwideAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(reqNullStatus)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("ASSIGNMENT_BYPASS_PROHIBITED")));

      CreateDataQualityIssueRequest reqOpenStatus = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-BYPASS-ASSIGN-2");
      reqOpenStatus.setEntityId(9961L);
      reqOpenStatus.setDataQualityIssueStatusId(getStatusId("OPEN"));
      reqOpenStatus.setAssignedUserId(9961L);

      mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(reqOpenStatus)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("ASSIGNMENT_BYPASS_PROHIBITED")));
    }

    @Test
    @DisplayName("Should accept compatible OPEN creation without fabricated events and allow valid subsequent workflow assignment")
    void shouldAllowOpenCreationAndSubsequentValidatedAssignment() throws Exception {
      CreateDataQualityIssueRequest request = new CreateDataQualityIssueRequest(
          defaultCategoryId, defaultSeverityId, defaultSourceId, "Trainee", "GDQ-TEST-VALID-OPEN-EXPLICIT");
      request.setEntityId(9961L);
      request.setDataQualityIssueStatusId(getStatusId("OPEN"));

      MvcResult result = mockMvc.perform(post("/api/v1/data-quality-issues")
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("OPEN")))
          .andExpect(jsonPath("$.data.assignedUserId", nullValue()))
          .andReturn();

      JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
      Long createdIssueId = root.path("data").path("id").asLong();

      // Verify zero fabricated historical events were created upon issue creation
      Integer preAssignEvents = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issue_events WHERE data_quality_issue_id = ?",
          Integer.class, createdIssueId);
      assertThat(preAssignEvents).isZero();

      // Now assign cleanly through the validated workflow
      AssignDataQualityIssueRequest assignReq = new AssignDataQualityIssueRequest(9961L, "Valid workflow assignment");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", createdIssueId)
              .header("Authorization", stateAAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(assignReq)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("ASSIGNED")))
          .andExpect(jsonPath("$.data.assignedUserId", is(9961)));

      // Verify exactly 1 event exists (the real assignment event)
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT remarks FROM data_quality_issue_events WHERE data_quality_issue_id = ?", createdIssueId);
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Valid workflow assignment");
    }

    @Test
    @DisplayName("Should preserve Phase 28.3 detection-engine registration and deduplication behavior")
    void shouldPreserveDetectionEngineRegistrationAndDeduplication() {
      DataQualityRule rule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
      RegistrationResult res1 = registrationService.registerOrUpdateIssue(
          rule, "Trainee", 9961L, "GDQ-TEST-DEDUP-SUMMARY");
      assertThat(res1.status()).isEqualTo(RegistrationStatus.CREATED);

      RegistrationResult res2 = registrationService.registerOrUpdateIssue(
          rule, "Trainee", 9961L, "GDQ-TEST-DEDUP-SUMMARY");
      assertThat(res2.status()).isEqualTo(RegistrationStatus.DEDUPLICATED);
      assertThat(res2.issueId()).isEqualTo(res1.issueId());
    }
  }
}
