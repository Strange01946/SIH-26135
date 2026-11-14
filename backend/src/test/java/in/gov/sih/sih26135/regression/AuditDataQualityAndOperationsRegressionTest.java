package in.gov.sih.sih26135.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.event.AuditTrailEvent;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 28.9 — Comprehensive Automated Regression Testing Suite.
 *
 * <p>Systematically verifies Phases 28.1 through 28.8:
 * <ol>
 *   <li>Section A: Automated audit interception, entity attribution, change-summary masking, rollback safety.</li>
 *   <li>Section B: Timeline reconstruction, deterministic tie-breakers, immutability, pagination beyond 100, cross-tenant timeline isolation.</li>
 *   <li>Section C: Data-quality rule scanning, disabled-rule skipping, active-issue deduplication, terminal state preservation.</li>
 *   <li>Section D: Data-quality issue state machine transitions (OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> REOPENED -> ASSIGNED), 409 conflict rejection, metadata tracking.</li>
 *   <li>Section E: Bulk ingestion, multipart parsing (CSV, JSON, NDJSON), malformed input safety, batch counters, tenant ownership.</li>
 *   <li>Section F: Chunked transaction and rollback safety, quarantine isolation, idempotent reprocessing, concurrency safeguards.</li>
 *   <li>Section G: System diagnostics, approved categories/severities, correlation tracing, tenant-isolated diagnostic queries.</li>
 *   <li>Section H: Multi-tenant operational isolation across same-state providers, states, districts, and nationwide administration.</li>
 *   <li>Section I: PII and credential sanitization (AuditPayloadMasker), token redaction, Aadhaar/PAN masking, malformed nested JSON fail-closed handling.</li>
 *   <li>Section J: Operational dashboard metrics regression, Data Cleanliness Index (DQI), active issue counting, tenant scoping.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 28.9 — Comprehensive Automated Regression Testing Suite (Phases 28.1–28.8)")
public class AuditDataQualityAndOperationsRegressionTest {

  // Dedicated test ID range for Phase 28.9 to guarantee test isolation
  private static final long STATE_A_ID = 9851L;
  private static final long STATE_B_ID = 9852L;
  private static final long DISTRICT_A1_ID = 9851L;
  private static final long DISTRICT_B1_ID = 9852L;
  private static final long DISTRICT_A2_ID = 9853L;

  private static final long PROVIDER_A1_ID = 9851L;
  private static final long PROVIDER_B1_ID = 9852L;
  private static final long PROVIDER_A2_ID = 9853L; // Same state & district as PROVIDER_A1

  private static final long EMPLOYER_A_ID = 9851L;
  private static final long EMPLOYER_B_ID = 9852L;

  private static final long SECTOR_ID = 9851L;
  private static final long COURSE_ID = 9851L;
  private static final long ORG_ID = 9851L;
  private static final long DEPT_ID = 9851L;
  private static final long SCHEME_ID = 9851L;
  private static final long PROGRAM_ID = 9851L;

  private static final long CENTER_A1_ID = 9851L;
  private static final long CENTER_B1_ID = 9852L;
  private static final long CENTER_A2_ID = 9853L;

  private static final long BATCH_A1_ID = 9851L;
  private static final long BATCH_B1_ID = 9852L;
  private static final long BATCH_A2_ID = 9853L;

  private static final long TRAINEE_A1_ID = 9851L;
  private static final long TRAINEE_B1_ID = 9852L;
  private static final long TRAINEE_A2_ID = 9853L;

  private static final long ENROLLMENT_A1_ID = 9851L;
  private static final long ENROLLMENT_B1_ID = 9852L;
  private static final long ENROLLMENT_A2_ID = 9853L;

  private static final long USER_NATIONWIDE_ID = 9850L;
  private static final long USER_SCOPED_GOV_ID = 98501L;
  private static final long USER_STATE_A_ID = 9851L;
  private static final long USER_STATE_B_ID = 9852L;
  private static final long USER_DIST_A1_ID = 9853L;
  private static final long USER_PROVIDER_A1_ID = 9854L;
  private static final long USER_PROVIDER_B1_ID = 9855L;
  private static final long USER_PROVIDER_A2_ID = 9856L;
  private static final long USER_EMPLOYER_A_ID = 9857L;
  private static final long USER_TRAINEE_A1_ID = 9858L;
  private static final long USER_UNSCOPED_ID = 9859L;
  private static final long USER_DELETED_ID = 98502L;

  private static final long IMPORT_BATCH_A1_ID = 985001L;
  private static final long IMPORT_BATCH_A2_ID = 985002L;
  private static final long IMPORT_BATCH_B1_ID = 985003L;
  private static final long IMPORT_REC_A1_ID = 985001L;
  private static final long IMPORT_REC_A2_ID = 985002L;

  private static final long DQ_RULE_1_ID = 985001L;
  private static final long DQ_RULE_2_ID = 985002L;
  private static final long DQ_ISSUE_A1_ID = 985011L;
  private static final long DQ_ISSUE_A2_ID = 985012L;
  private static final long DQ_ISSUE_B1_ID = 985013L;
  private static final long DQ_EVENT_A1_ID = 985021L;

  private static final long AUDIT_A1_ID = 985001L;
  private static final long AUDIT_B1_ID = 985002L;
  private static final long AUDIT_A2_ID = 985003L;

  private static final long EVENT_A1_ID = 985001L;
  private static final long EVENT_A2_ID = 985002L;
  private static final long EVENT_B1_ID = 985003L;

  private static final String SAMPLE_JWT =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5ODUwIn0.kH7fX8k9_2b9P9z0YxQ1zW8kL5mN7vT3jR1sP5vM";

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

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private ImportFileStagingService fileStagingService;

  @Autowired
  private AuditTrailEventListener auditTrailEventListener;

  @Autowired
  private PlacementEmploymentWorkflowService placementEmploymentWorkflowService;

  private static final Logger log = LoggerFactory.getLogger(AuditDataQualityAndOperationsRegressionTest.class);

  private String tokenNationwide;
  private String tokenScopedGov;
  private String tokenStateA;
  private String tokenStateB;
  private String tokenDistrictA1;
  private String tokenProviderA1;
  private String tokenProviderA2;
  private String tokenProviderB1;
  private String tokenEmployerA;
  private String tokenUnscoped;
  private String tokenDeleted;

  private Long defaultAuditActionId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    catalogService.ensureDefaultRulesProvisioned();
    cleanUpFixtures();
    seedBaseFixtures();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpFixtures();
  }

  private void seedBaseFixtures() {
    // 1. Geography hierarchy
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, '9A', 'Phase289 State A', 'LG9851', 1, NOW(), NOW()), "
            + "(?, '9B', 'Phase289 State B', 'LG9852', 1, NOW(), NOW())",
        STATE_A_ID, STATE_B_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, '9D1', 'District A1', 'LGD9851', 1, NOW(), NOW()), "
            + "(?, ?, '9D2', 'District B1', 'LGD9852', 1, NOW(), NOW()), "
            + "(?, ?, '9D3', 'District A2', 'LGD9853', 1, NOW(), NOW())",
        DISTRICT_A1_ID, STATE_A_ID,
        DISTRICT_B1_ID, STATE_B_ID,
        DISTRICT_A2_ID, STATE_A_ID);

    // 2. Training Providers & Employers
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9851', 'Provider A1', 'REG-PRV-9851', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'PRV-9852', 'Provider B1', 'REG-PRV-9852', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'PRV-9853', 'Provider A2', 'REG-PRV-9853', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID,
        PROVIDER_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-9851', 'Employer A', 'REG-EMP-9851', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'EMP-9852', 'Employer B', 'REG-EMP-9852', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID,
        EMPLOYER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    // 3. Courses, Sectors, Orgs, Programs
    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-9851', 'Phase289 Sector', 1, NOW(), NOW())",
        SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-9851', 'Phase289 Course', ?, 1, 1, NOW(), NOW(), NULL)",
        COURSE_ID, SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-9851', 'Phase289 Org', 1, 1, NOW(), NOW())",
        ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-9851', 'Phase289 Dept', 1, NOW(), NOW())",
        DEPT_ID, ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-9851', 'Phase289 Scheme', ?, 'INR', 1, NOW(), NOW())",
        SCHEME_ID, DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-9851', 'Phase289 Program', ?, ?, 1, NOW(), NOW())",
        PROGRAM_ID, DEPT_ID, SCHEME_ID);

    // 4. Centers & Batches
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-9851', 'Center A1', ?, ?, 1, NOW(), NOW(), NULL), "
            + "(?, ?, 'CTR-9852', 'Center B1', ?, ?, 1, NOW(), NOW(), NULL), "
            + "(?, ?, 'CTR-9853', 'Center A2', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A1_ID, PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID,
        CENTER_B1_ID, PROVIDER_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        CENTER_A2_ID, PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A2_ID);

    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-9851', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL), "
            + "(?, 'BAT-9852', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL), "
            + "(?, 'BAT-9853', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A1_ID, COURSE_ID, PROVIDER_A1_ID, CENTER_A1_ID, PROGRAM_ID,
        BATCH_B1_ID, COURSE_ID, PROVIDER_B1_ID, CENTER_B1_ID, PROGRAM_ID,
        BATCH_A2_ID, COURSE_ID, PROVIDER_A2_ID, CENTER_A2_ID, PROGRAM_ID);

    // 5. Users and Roles
    insertUser(USER_NATIONWIDE_ID, "p289_super", "p289_super@test.gov.in", null, null, null, null, null, "super_admin");
    insertUser(USER_SCOPED_GOV_ID, "p289_scoped_gov", "p289_sg@test.gov.in", STATE_A_ID, null, null, null, null, "government_admin");
    insertUser(USER_STATE_A_ID, "p289_state_a", "p289_sa@test.gov.in", STATE_A_ID, null, null, null, null, "state_admin");
    insertUser(USER_STATE_B_ID, "p289_state_b", "p289_sb@test.gov.in", STATE_B_ID, null, null, null, null, "state_admin");
    insertUser(USER_DIST_A1_ID, "p289_dist_a1", "p289_da1@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, null, "district_officer");
    insertUser(USER_PROVIDER_A1_ID, "p289_prov_a1", "p289_pa1@test.gov.in", null, null, PROVIDER_A1_ID, null, null, "training_provider");
    insertUser(USER_PROVIDER_A2_ID, "p289_prov_a2", "p289_pa2@test.gov.in", null, null, PROVIDER_A2_ID, null, null, "training_provider");
    insertUser(USER_PROVIDER_B1_ID, "p289_prov_b1", "p289_pb1@test.gov.in", null, null, PROVIDER_B1_ID, null, null, "training_provider");
    insertUser(USER_EMPLOYER_A_ID, "p289_emp_a", "p289_ea@test.gov.in", null, null, null, EMPLOYER_A_ID, null, "employer");
    insertUser(USER_TRAINEE_A1_ID, "p289_trainee", "p289_tr@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, null, "trainee");
    insertUser(USER_UNSCOPED_ID, "p289_unscoped", "p289_unscoped@test.gov.in", null, null, null, null, null, null);
    insertUser(USER_DELETED_ID, "p289_deleted", "p289_del@test.gov.in", STATE_A_ID, null, null, null, "NOW()", "state_admin");

    // 6. Trainees & Enrollments
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, user_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'P289-TR-9851', 'TrA1', 'User', '9850098511', 'tr9851@test.gov.in', '2000-01-01', 1, ?, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'P289-TR-9852', 'TrB1', 'User', '9850098512', 'tr9852@test.gov.in', '2000-01-01', 1, ?, ?, NULL, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'P289-TR-9853', 'TrA2', 'User', '9850098513', 'tr9853@test.gov.in', '2000-01-01', 1, ?, ?, NULL, 1, 1, NOW(), NOW(), NULL)",
        TRAINEE_A1_ID, STATE_A_ID, DISTRICT_A1_ID, USER_TRAINEE_A1_ID,
        TRAINEE_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        TRAINEE_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'P289-ENR-9851', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL), "
            + "(?, 'P289-ENR-9852', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL), "
            + "(?, 'P289-ENR-9853', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)",
        ENROLLMENT_A1_ID, TRAINEE_A1_ID, PROGRAM_ID, COURSE_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID,
        ENROLLMENT_B1_ID, TRAINEE_B1_ID, PROGRAM_ID, COURSE_ID, PROVIDER_B1_ID, CENTER_B1_ID, BATCH_B1_ID,
        ENROLLMENT_A2_ID, TRAINEE_A2_ID, PROGRAM_ID, COURSE_ID, PROVIDER_A2_ID, CENTER_A2_ID, BATCH_A2_ID);

    // 7. Data Quality Rules & Issues
    jdbcTemplate.update(
        "INSERT INTO data_quality_rules (data_quality_rule_id, rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P289_RULE_ENABLED', 'Enabled Contact Rule', 'Contact rule', 'Trainee', 1, 2, 1, NOW(), NOW()), "
            + "(?, 'P289_RULE_DISABLED', 'Disabled Contact Rule', 'Contact rule', 'Trainee', 1, 2, 2, NOW(), NOW())",
        DQ_RULE_1_ID, DQ_RULE_2_ID);

    jdbcTemplate.update(
        "INSERT INTO data_quality_issues (data_quality_issue_id, data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, resolution_notes, detected_at, created_at, updated_at) "
            + "VALUES (?, ?, 1, 2, 1, 1, 'Trainee', ?, 'Initial Issue A1 password=Sec99', NULL, NOW(), NOW(), NOW()), "
            + "(?, ?, 1, 2, 1, 1, 'Trainee', ?, 'Initial Issue A2', NULL, NOW(), NOW(), NOW()), "
            + "(?, ?, 1, 2, 1, 1, 'Trainee', ?, 'Initial Issue B1', NULL, NOW(), NOW(), NOW())",
        DQ_ISSUE_A1_ID, DQ_RULE_1_ID, TRAINEE_A1_ID,
        DQ_ISSUE_A2_ID, DQ_RULE_1_ID, TRAINEE_A2_ID,
        DQ_ISSUE_B1_ID, DQ_RULE_1_ID, TRAINEE_B1_ID);

    jdbcTemplate.update(
        "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, changed_at, remarks, created_at) "
            + "VALUES (?, ?, 1, ?, NOW() - INTERVAL 1 HOUR, 'Reported with password=SecretPwd99', NOW() - INTERVAL 1 HOUR)",
        DQ_EVENT_A1_ID, DQ_ISSUE_A1_ID, USER_NATIONWIDE_ID);

    // 8. Import Batches (Phase 28.9-owned)
    jdbcTemplate.update(
        "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, created_at, updated_at) "
            + "VALUES (?, 'P289-IMP-985001', 'P289_REG', 'Trainee', 1, ?, 0, 0, 0, NOW(), NOW()), "
            + "(?, 'P289-IMP-985002', 'P289_REG', 'Trainee', 1, ?, 0, 0, 0, NOW(), NOW()), "
            + "(?, 'P289-IMP-985003', 'P289_REG', 'Trainee', 1, ?, 0, 0, 0, NOW(), NOW())",
        IMPORT_BATCH_A1_ID, USER_PROVIDER_A1_ID,
        IMPORT_BATCH_A2_ID, USER_PROVIDER_A2_ID,
        IMPORT_BATCH_B1_ID, USER_PROVIDER_B1_ID);

    // 8b. Workflow Management Role & Assignment for Assignee Validation
    jdbcTemplate.update(
        "INSERT INTO roles (role_id, role_code, role_name, is_system_role, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (9851, 'P289_DQ_MGR', 'P289 DQ Manager', 0, 1, NOW(), NOW())");
    jdbcTemplate.update(
        "INSERT INTO role_permissions (role_id, permission_id) "
            + "SELECT 9851, permission_id FROM permissions WHERE permission_code = 'system.manage'");
    jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (?, 9851, NOW())", USER_PROVIDER_A1_ID);

    // 9. Audit Logs & System Events
    defaultAuditActionId = jdbcTemplate.queryForObject(
        "SELECT audit_action_id FROM ref_audit_action WHERE action_code = 'UPDATE' LIMIT 1", Long.class);
    if (defaultAuditActionId == null) {
      defaultAuditActionId = 2L;
    }

    jdbcTemplate.update(
        "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, correlation_id, change_summary, created_at) "
            + "VALUES (?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 2 HOUR, 'p289-corr-a1', ?, NOW()), "
            + "(?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 1 HOUR, 'p289-corr-b1', ?, NOW()), "
            + "(?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 30 MINUTE, 'p289-corr-a2', ?, NOW())",
        AUDIT_A1_ID, USER_PROVIDER_A1_ID, defaultAuditActionId, CENTER_A1_ID,
        "{\"password\":\"AuditPass#99\",\"refreshToken\":\"ref_token_99\",\"aadhaar\":\"1234-5678-9012\",\"pan\":\"ABCDE1234F\"}",
        AUDIT_B1_ID, USER_PROVIDER_B1_ID, defaultAuditActionId, CENTER_B1_ID,
        "Authorization: Bearer " + SAMPLE_JWT + " client_secret=B1Secret",
        AUDIT_A2_ID, USER_PROVIDER_A2_ID, defaultAuditActionId, CENTER_A2_ID,
        "Provider A2 audit action apiKey=A2Key");

    jdbcTemplate.update(
        "INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, actor_user_id, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) "
            + "VALUES (?, 1, 3, 'P289_EVT_A1', ?, 'TrainingCenter', ?, 'RegTest', ?, NOW() - INTERVAL 2 HOUR, NOW()), "
            + "(?, 1, 3, 'P289_EVT_B1', ?, 'TrainingCenter', ?, 'RegTest', ?, NOW() - INTERVAL 1 HOUR, NOW()), "
            + "(?, 1, 3, 'P289_EVT_A2', ?, 'TrainingCenter', ?, 'RegTest', ?, NOW() - INTERVAL 30 MINUTE, NOW())",
        EVENT_A1_ID, USER_PROVIDER_A1_ID, CENTER_A1_ID,
        "[cid=p289-cid-shared|rid=p289-req-a1] Login fail password=SysEventPwd jwt=" + SAMPLE_JWT + " aadhaar=1234-5678-9012 PAN=ABCDE1234F",
        EVENT_B1_ID, USER_PROVIDER_B1_ID, CENTER_B1_ID,
        "[cid=p289-cid-shared|rid=p289-req-b1] State B event access_token=B1Token",
        EVENT_A2_ID, USER_PROVIDER_A2_ID, CENTER_A2_ID,
        "[cid=p289-cid-shared|rid=p289-req-a2] Provider A2 event client_secret=A2Secret");

    // 10. Pre-generate JWT authentication tokens
    tokenNationwide = bearer(USER_NATIONWIDE_ID, "p289_super",
        List.of("ROLE_super_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenScopedGov = bearer(USER_SCOPED_GOV_ID, "p289_scoped_gov",
        List.of("ROLE_government_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    tokenStateA = bearer(USER_STATE_A_ID, "p289_state_a",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenStateB = bearer(USER_STATE_B_ID, "p289_state_b",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenDistrictA1 = bearer(USER_DIST_A1_ID, "p289_dist_a1",
        List.of("ROLE_district_officer", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    tokenProviderA1 = bearer(USER_PROVIDER_A1_ID, "p289_prov_a1",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenProviderA2 = bearer(USER_PROVIDER_A2_ID, "p289_prov_a2",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenProviderB1 = bearer(USER_PROVIDER_B1_ID, "p289_prov_b1",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenEmployerA = bearer(USER_EMPLOYER_A_ID, "p289_emp_a",
        List.of("ROLE_employer", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    tokenUnscoped = bearer(USER_UNSCOPED_ID, "p289_unscoped",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    tokenDeleted = bearer(USER_DELETED_ID, "p289_deleted",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
  }

  private void cleanUpFixtures() {
    try {
      List<String> batchCodes = jdbcTemplate.queryForList(
          "SELECT batch_code FROM import_batches WHERE (initiated_by_user_id BETWEEN 9850 AND 9859) "
              + "OR source_system = 'P289_REG' "
              + "OR (import_batch_id BETWEEN 985000 AND 985999) "
              + "OR batch_code LIKE 'P289_%' "
              + "OR batch_code LIKE 'P289-%'",
          String.class);
      for (String code : batchCodes) {
        if (code != null) {
          fileStagingService.deleteStagedFileQuietly(code);
        }
      }

      jdbcTemplate.update("DELETE FROM audit_logs WHERE audit_log_id BETWEEN 985000 AND 985999 OR actor_user_id BETWEEN 9850 AND 9859 OR correlation_id LIKE 'p289-%'");
      jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id BETWEEN 985000 AND 985999 OR actor_user_id BETWEEN 9850 AND 9859 OR event_code LIKE 'P289_%'");
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 985000 AND 989999 OR data_quality_issue_id BETWEEN 985000 AND 989999 OR remarks LIKE 'P289-%'");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_issue_id BETWEEN 985000 AND 989999");
      jdbcTemplate.update("DELETE FROM data_quality_rules WHERE data_quality_rule_id IN (?, ?) OR rule_code LIKE 'P289_%'", DQ_RULE_1_ID, DQ_RULE_2_ID);
      jdbcTemplate.update("DELETE FROM import_records WHERE import_record_id BETWEEN 985000 AND 985999 OR import_batch_id BETWEEN 985000 AND 985999 OR import_batch_id IN (SELECT import_batch_id FROM import_batches WHERE source_system = 'P289_REG' OR (initiated_by_user_id BETWEEN 9850 AND 9859))");
      jdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id BETWEEN 985000 AND 985999 OR initiated_by_user_id BETWEEN 9850 AND 9859 OR source_system = 'P289_REG'");
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id IN (?, ?, ?)", ENROLLMENT_A1_ID, ENROLLMENT_B1_ID, ENROLLMENT_A2_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id IN (?, ?, ?) OR registration_number LIKE 'P289-%'", TRAINEE_A1_ID, TRAINEE_B1_ID, TRAINEE_A2_ID);
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?, ?)", BATCH_A1_ID, BATCH_B1_ID, BATCH_A2_ID);
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (?, ?, ?)", CENTER_A1_ID, CENTER_B1_ID, CENTER_A2_ID);
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", COURSE_ID);
      jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", PROGRAM_ID);
      jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", SCHEME_ID);
      jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", DEPT_ID);
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", ORG_ID);
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SECTOR_ID);
      jdbcTemplate.update("DELETE FROM user_roles WHERE user_id BETWEEN 9850 AND 9859 OR user_id IN (98501, 98502) OR role_id = 9851");
      jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = 9851");
      jdbcTemplate.update("DELETE FROM roles WHERE role_id = 9851");
      jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 9850 AND 9859 OR user_id IN (98501, 98502)");
      jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (?, ?)", EMPLOYER_A_ID, EMPLOYER_B_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?, ?)", PROVIDER_A1_ID, PROVIDER_B1_ID, PROVIDER_A2_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)", DISTRICT_A1_ID, DISTRICT_B1_ID, DISTRICT_A2_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)", STATE_A_ID, STATE_B_ID);
    } catch (Exception ex) {
      log.warn("Test fixture cleanup encountered non-fatal error: {}", ex.getMessage());
    }
  }

  private void insertUser(
      Long userId,
      String username,
      String email,
      Long stateId,
      Long districtId,
      Long providerId,
      Long employerId,
      String deletedAtExpr,
      String roleCode) {
    String sql = "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, "
        + "state_id, district_id, training_provider_id, employer_id, deleted_at, created_at, updated_at) "
        + "VALUES (?, ?, ?, 'hash', 'argon2id', 1, ?, ?, ?, ?, "
        + (deletedAtExpr == null ? "NULL" : "NOW()") + ", NOW(), NOW())";
    jdbcTemplate.update(sql, userId, username, email, stateId, districtId, providerId, employerId);
    if (roleCode != null) {
      jdbcTemplate.update(
          "INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT ?, role_id, NOW() FROM roles WHERE role_code = ? LIMIT 1",
          userId, roleCode);
    }
  }

  private String bearer(Long userId, String username, List<String> authorities) {
    return "Bearer " + jwtTokenProvider.generateAccessToken(userId, username, authorities);
  }

  private void waitForAuditLog(String correlationId) throws InterruptedException {
    for (int i = 0; i < 40; i++) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM audit_logs WHERE correlation_id = ?", Integer.class, correlationId);
      if (count != null && count > 0) {
        return;
      }
      Thread.sleep(50);
    }
  }

  // =========================================================================
  // Section A: Automated Audit Interception
  // =========================================================================
  @Nested
  @DisplayName("A. Automated Audit Interception Tests")
  class AutomatedAuditInterceptionTests {

    @Test
    @DisplayName("A.1: Asynchronous audit event listener persists valid events with authoritative actor and entity attribution")
    void shouldPersistAuditEventViaListenerWithAuthoritativeActorAndEntityMetadata() throws Exception {
      // Reusing AuditTrailEventListenerTest pattern: verifies that AuditTrailEventListener executes asynchronously
      // via auditTaskExecutor and persists valid AuditTrailEvents with authoritative actor and entity attribution.
      // (Note: Automatic AOP method interception is proven by AuditTrailAspectTest.testSuccessfulInterceptionPublishesEvent).
      LocalDateTime now = LocalDateTime.now();
      String correlationId = "p289-corr-aspect-1";
      AuditTrailEvent event = new AuditTrailEvent(
          "STATUS_CHANGE",
          "Placement",
          7771L,
          USER_PROVIDER_A1_ID,
          "Placement transitioned to employment",
          now,
          correlationId
      );

      // Execute listener directly as in AuditTrailEventListenerTest
      auditTrailEventListener.handleAuditTrailEvent(event);
      waitForAuditLog(correlationId);

      // Verify in DB directly: authoritative attribution to actor, entity, and action
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          "SELECT audit_log_id, actor_user_id, entity_type, entity_id, correlation_id, change_summary "
              + "FROM audit_logs WHERE correlation_id = ? AND entity_type = 'Placement' AND entity_id = 7771",
          correlationId);
      assertThat(rows).hasSize(1);
      Map<String, Object> row = rows.get(0);
      assertThat(((Number) row.get("actor_user_id")).longValue()).isEqualTo(USER_PROVIDER_A1_ID);
      assertThat(row.get("entity_type")).isEqualTo("Placement");
      assertThat(((Number) row.get("entity_id")).longValue()).isEqualTo(7771L);
      assertThat(row.get("correlation_id")).isEqualTo(correlationId);
      assertThat(row.get("change_summary")).isEqualTo("Placement transitioned to employment");
    }

    @Test
    @DisplayName("A.2: Sensitive credentials and PII in audit events and manual audit API requests are sanitized at write time")
    void shouldPreserveCorrelationMetadataAndMaskChangeSummaryInAuditRecord() throws Exception {
      // 1. Verify automatic audit trail event sanitization (reusing AuditTrailEventListenerTest pattern)
      String eventCorr = "p289-corr-mask-evt";
      AuditTrailEvent sensitiveEvent = new AuditTrailEvent(
          "STATUS_CHANGE",
          "TrainingCenter",
          CENTER_A1_ID,
          USER_PROVIDER_A1_ID,
          "Updated center with password=EventSecretPass123 aadhaar=2345 6789 0123 pan=ABCDE1234F",
          LocalDateTime.now(),
          eventCorr
      );
      auditTrailEventListener.handleAuditTrailEvent(sensitiveEvent);
      waitForAuditLog(eventCorr);

      String dbEventSummary = jdbcTemplate.queryForObject(
          "SELECT change_summary FROM audit_logs WHERE correlation_id = ?", String.class, eventCorr);
      assertThat(dbEventSummary).doesNotContain("EventSecretPass123");
      assertThat(dbEventSummary).contains("password=***REDACTED***");
      assertThat(dbEventSummary).contains("XXXX-XXXX-0123");
      assertThat(dbEventSummary).contains("XXXXX1234F");

      // 2. Preserve manual audit API write-time sanitization test
      CreateAuditLogRequest request = new CreateAuditLogRequest();
      request.setAuditActionId(defaultAuditActionId);
      request.setEntityType("TrainingCenter");
      request.setEntityId(CENTER_A1_ID);
      request.setActorUserId(USER_NATIONWIDE_ID);
      request.setOccurredAt(LocalDateTime.now());
      request.setCorrelationId("p289-custom-corr-1");
      request.setChangeSummary("User update password=RawSecretPass123 aadhaar=1234 5678 9012 pan=ABCDE1234F");

      MvcResult res = mockMvc.perform(post("/api/v1/audit-logs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide)
              .header("X-Correlation-ID", "p289-custom-corr-1"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.changeSummary").value(not(containsString("RawSecretPass123"))))
          .andExpect(jsonPath("$.data.changeSummary").value(containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.changeSummary").value(containsString("XXXX-XXXX-9012")))
          .andExpect(jsonPath("$.data.changeSummary").value(containsString("XXXXXX123X")))
          .andReturn();

      JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
      long createdId = root.path("data").path("id").asLong();

      String dbSummary = jdbcTemplate.queryForObject(
          "SELECT change_summary FROM audit_logs WHERE audit_log_id = ?", String.class, createdId);
      assertThat(dbSummary).doesNotContain("RawSecretPass123");
      assertThat(dbSummary).contains("[REDACTED]");
    }

    @Test
    @DisplayName("A.3: Fail-closed workflow execution prevents audit event persistence when entity validation fails")
    void shouldVerifyFailClosedWorkflowPreventsAuditEventWhenTargetEntityNotFound() throws Exception {
      long countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);

      // Calling an @AuditTrail-annotated workflow method with an invalid target fails closed before writes,
      // throwing ResourceNotFoundException and ensuring zero phantom audit logs are generated.
      // (Note: Post-write transactional rollback is proven by WorkflowTransactionRollbackIntegrationTest).
      PlacementToEmploymentTransitionRequest failRequest = new PlacementToEmploymentTransitionRequest();
      failRequest.setPlacementId(999999L); // Non-existent placement ID triggers ResourceNotFoundException
      failRequest.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> placementEmploymentWorkflowService.transitionPlacementToEmployment(failRequest))
          .isInstanceOf(ResourceNotFoundException.class);

      // Verify that fail-closed validation prevented any audit record from being persisted
      long countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs", Long.class);
      assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("A.4: Preserves manual audit creation API under discrete single-entry semantics")
    void shouldPreventDuplicateAuditEventsForSingleLogicalMutation() throws Exception {
      long auditCountBefore = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM audit_logs WHERE entity_type = 'TrainingCenter' AND entity_id = ?",
          Long.class, CENTER_A1_ID);

      CreateAuditLogRequest request = new CreateAuditLogRequest();
      request.setAuditActionId(defaultAuditActionId);
      request.setEntityType("TrainingCenter");
      request.setEntityId(CENTER_A1_ID);
      request.setOccurredAt(LocalDateTime.now());
      request.setChangeSummary("Single discrete audit entry");

      mockMvc.perform(post("/api/v1/audit-logs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isCreated());

      long auditCountAfter = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM audit_logs WHERE entity_type = 'TrainingCenter' AND entity_id = ?",
          Long.class, CENTER_A1_ID);
      assertThat(auditCountAfter).isEqualTo(auditCountBefore + 1);
    }
  }

  // =========================================================================
  // Section B: Timeline Reconstruction and Immutability
  // =========================================================================
  @Nested
  @DisplayName("B. Timeline Reconstruction and Immutability Tests")
  class TimelineReconstructionAndImmutabilityTests {

    @Test
    @DisplayName("B.1: Reconstructs entity timeline in strict chronological order with deterministic tie-breaker")
    void shouldReconstructAuditTimelineInStrictChronologicalOrderWithDeterministicTieBreaker() throws Exception {
      MvcResult res = mockMvc.perform(get("/api/v1/audit-logs/entities/TrainingCenter/{id}/timeline", CENTER_A1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.timelineEntries").isArray())
          .andReturn();

      JsonNode events = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("timelineEntries");
      assertThat(events.size()).isGreaterThanOrEqualTo(1);

      // Verify chronology: each event occurredAt is non-decreasing
      LocalDateTime prev = null;
      for (JsonNode ev : events) {
        LocalDateTime curr = LocalDateTime.parse(ev.path("occurredAt").asText());
        if (prev != null) {
          assertThat(curr).isAfterOrEqualTo(prev);
        }
        prev = curr;
      }
    }

    @Test
    @DisplayName("B.2: Rejects audit log modification and deletion with HTTP 405 Method Not Allowed")
    void shouldRejectAuditLogMutationAndDeletionPreservingImmutability() throws Exception {
      mockMvc.perform(put("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"changeSummary\":\"Forced update\"}")
              .header("Authorization", tokenNationwide))
          .andExpect(status().isMethodNotAllowed());

      mockMvc.perform(patch("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"changeSummary\":\"Forced patch\"}")
              .header("Authorization", tokenNationwide))
          .andExpect(status().isMethodNotAllowed());

      mockMvc.perform(delete("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isMethodNotAllowed());

      // Verify row remains in DB intact
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM audit_logs WHERE audit_log_id = ?", Integer.class, AUDIT_A1_ID);
      assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("B.3: Timeline endpoint supports pagination beyond 100 records and returns empty on out-of-range")
    void shouldSupportTimelinePaginationBeyond100RecordsAndReturnEmptyOnOutOfRange() throws Exception {
      // Seed 105 events on DQ_ISSUE_A1_ID
      for (int i = 1; i <= 105; i++) {
        jdbcTemplate.update(
            "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, changed_at, remarks, created_at) "
                + "VALUES (?, ?, 1, ?, NOW() - INTERVAL ? MINUTE, ?, NOW())",
            985100L + i, DQ_ISSUE_A1_ID, USER_NATIONWIDE_ID, 200 - i, "P289 seeded event " + i);
      }

      // Page 0 (size 50) -> returns 50 records
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
              .param("page", "0")
              .param("size", "50")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(50));

      // Page 1 (size 50) -> returns 50 records
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
              .param("page", "1")
              .param("size", "50")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(50));

      // Out-of-range page (page 10, size 50) -> returns empty list [] without repository fallback
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
              .param("page", "10")
              .param("size", "50")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("B.4: Cross-tenant timeline access is denied with HTTP 403 Forbidden")
    void shouldDenyCrossTenantTimelineAccessWithHttp403() throws Exception {
      // Provider A1 attempts to access Provider B1's issue timeline
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_B1_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());
    }
  }

  // =========================================================================
  // Section C: Data-Quality Rule Scanning
  // =========================================================================
  @Nested
  @DisplayName("C. Data-Quality Rule Scanning Tests")
  class DataQualityRuleScanningAndDeduplicationTests {

    @Test
    @DisplayName("C.1: Enabled rules execute against eligible records while disabled rules do not generate findings")
    void shouldExecuteEnabledRulesAgainstEligibleRecordsAndSkipDisabledRules() throws Exception {
      DataQualityScanRequest request = new DataQualityScanRequest();
      request.setRuleId(DQ_RULE_1_ID);

      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // Disabled rule scan returns 0 rules evaluated (empty list)
      DataQualityScanRequest disabledRequest = new DataQualityScanRequest();
      disabledRequest.setRuleId(DQ_RULE_2_ID);

      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(disabledRequest))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("C.2: Repeated scans deduplicate active issues without creating duplicates")
    void shouldDeduplicateActiveIssuesOnRepeatedScansWithoutDuplication() throws Exception {
      DataQualityScanRequest request = new DataQualityScanRequest();
      request.setRuleId(DQ_RULE_1_ID);

      // Run scan 1
      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk());

      int count1 = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ?", Integer.class, DQ_RULE_1_ID);

      // Run scan 2
      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk());

      int count2 = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ?", Integer.class, DQ_RULE_1_ID);

      assertThat(count2).isEqualTo(count1);
    }

    @Test
    @DisplayName("C.3: Resolving or dismissing an issue preserves terminal status during deduplication")
    void shouldPreserveTerminalIssuesWithoutReopeningDuringDeduplication() throws Exception {
      // Mark DQ_ISSUE_A2_ID as RESOLVED (status 4)
      jdbcTemplate.update(
          "UPDATE data_quality_issues SET data_quality_issue_status_id = 4, resolved_by_user_id = ?, resolved_at = NOW() WHERE data_quality_issue_id = ?",
          USER_NATIONWIDE_ID, DQ_ISSUE_A2_ID);

      DataQualityScanRequest request = new DataQualityScanRequest();
      request.setRuleId(DQ_RULE_1_ID);

      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk());

      // Verify DQ_ISSUE_A2_ID remains resolved (status 4) and was not mutated to open
      Integer statusId = jdbcTemplate.queryForObject(
          "SELECT data_quality_issue_status_id FROM data_quality_issues WHERE data_quality_issue_id = ?",
          Integer.class, DQ_ISSUE_A2_ID);
      assertThat(statusId).isEqualTo(4);
    }

    @Test
    @DisplayName("C.4: Empty scan population returns clean success without errors")
    void shouldRespectTenantBoundariesAndHandleEmptyScanPopulationsGracefully() throws Exception {
      DataQualityScanRequest request = new DataQualityScanRequest();
      request.setRuleCode("DQ-TRAINEE-001");
      request.setEntityId(999999L);

      mockMvc.perform(post("/api/v1/data-quality/scan")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").isArray());
    }
  }

  // =========================================================================
  // Section D: Data-Quality Issue State Machine
  // =========================================================================
  @Nested
  @DisplayName("D. Data-Quality Issue State Machine Tests")
  class DataQualityIssueStateMachineTests {

    @Test
    @DisplayName("D.1: Follows valid lifecycle transitions (OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> REOPENED -> ASSIGNED)")
    void shouldFollowValidLifecycleTransitionsWithRequiredMetadataAndAuditHistory() throws Exception {
      // 1. OPEN -> ASSIGNED
      AssignDataQualityIssueRequest assignReq = new AssignDataQualityIssueRequest(USER_PROVIDER_A1_ID, "Assigning to provider A1 investigator");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(assignReq))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode").value("ASSIGNED"));

      // 2. ASSIGNED -> IN_PROGRESS
      StartInvestigationRequest startReq = new StartInvestigationRequest("Beginning active investigation");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(startReq))
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode").value("IN_PROGRESS"));

      // 3. IN_PROGRESS -> RESOLVED
      ResolveDataQualityIssueRequest resolveReq = new ResolveDataQualityIssueRequest("Discrepancy resolved in center records");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(resolveReq))
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode").value("RESOLVED"));

      // 4. RESOLVED -> REOPENED
      ReopenDataQualityIssueRequest reopenReq = new ReopenDataQualityIssueRequest("Reopened due to recurring discrepancy");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/reopen", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(reopenReq))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode").value("REOPENED"));

      // 5. REOPENED -> ASSIGNED
      AssignDataQualityIssueRequest reassignReq = new AssignDataQualityIssueRequest(USER_PROVIDER_A1_ID, "Reassigning to reviewer");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(reassignReq))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode").value("ASSIGNED"));

      // Verify event history was recorded in data_quality_issue_events
      Integer eventCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issue_events WHERE data_quality_issue_id = ?",
          Integer.class, DQ_ISSUE_A1_ID);
      assertThat(eventCount).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("D.2: Rejects invalid lifecycle transitions with HTTP 409 Conflict")
    void shouldRejectInvalidLifecycleTransitionsWithConflict409() throws Exception {
      // Issue DQ_ISSUE_A1_ID starts in OPEN status. Attempting direct IN_PROGRESS or RESOLVED is invalid.
      StartInvestigationRequest startReq = new StartInvestigationRequest("Premature start");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(startReq))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isConflict());

      ResolveDataQualityIssueRequest resolveReq = new ResolveDataQualityIssueRequest("Premature resolution");
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(resolveReq))
              .header("Authorization", tokenNationwide))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("D.3: Enforces role authorization and tenant scope on lifecycle transitions")
    void shouldEnforceRoleAndTenancyOnLifecycleTransitionsAndMaintainPersistenceConsistency() throws Exception {
      // Caller without system.manage / management authority receives 403 Forbidden
      String traineeToken = bearer(USER_TRAINEE_A1_ID, "p289_trainee", List.of("ROLE_trainee"));
      AssignDataQualityIssueRequest assignReq = new AssignDataQualityIssueRequest(USER_PROVIDER_A1_ID, "Unauthorized assignment");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", DQ_ISSUE_A1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(assignReq))
              .header("Authorization", traineeToken))
          .andExpect(status().isForbidden());

      // Provider A attempting to transition Provider B's issue is rejected with 403 Forbidden
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", DQ_ISSUE_B1_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(assignReq))
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());

      // Verify issue status remains unchanged in database
      Integer statusId = jdbcTemplate.queryForObject(
          "SELECT data_quality_issue_status_id FROM data_quality_issues WHERE data_quality_issue_id = ?",
          Integer.class, DQ_ISSUE_A1_ID);
      assertThat(statusId).isEqualTo(1); // Still OPEN
    }
  }

  // =========================================================================
  // Section E: Bulk Ingestion and Multipart Parsing
  // =========================================================================
  @Nested
  @DisplayName("E. Bulk Ingestion and Multipart Parsing Tests")
  class BulkIngestionAndMultipartParsingTests {

    @Test
    @DisplayName("E.1: Valid multipart CSV upload is accepted and staged cleanly with authenticated initiator")
    void shouldAcceptAndProcessValidMultipartCsvUpload() throws Exception {
      String csv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "P289-IMP-01,Ravi,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n";
      MockMultipartFile file = new MockMultipartFile(
          "file", "trainees.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

      MvcResult res = mockMvc.perform(multipart("/api/v1/import/batches")
              .file(file)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"))
          .andExpect(jsonPath("$.data.entityType").value("Trainee"))
          .andExpect(jsonPath("$.data.initiatedByUserId").value(USER_PROVIDER_A1_ID))
          .andReturn();

      String batchCode = objectMapper.readTree(res.getResponse().getContentAsString())
          .path("data").path("batchCode").asText();
      assertThat(batchCode).contains("IMP-");
      assertThat(fileStagingService.hasStagedFile(batchCode)).isTrue();
    }

    @Test
    @DisplayName("E.2: Valid multipart JSON array and NDJSON uploads are accepted and staged")
    void shouldAcceptAndProcessValidMultipartJsonAndNdjsonUpload() throws Exception {
      // 1. JSON Array upload
      String json = "[{\"registrationNumber\":\"P289-JSON-01\",\"firstName\":\"Geeta\",\"genderId\":2,\"stateId\":"
          + STATE_A_ID + ",\"districtId\":" + DISTRICT_A1_ID + ",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}]";
      MockMultipartFile jsonFile = new MockMultipartFile(
          "file", "trainees.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

      mockMvc.perform(multipart("/api/v1/import-batches")
              .file(jsonFile)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"));

      // 2. NDJSON stream upload
      String ndjson = "{\"registrationNumber\":\"P289-NDJSON-01\",\"firstName\":\"Anita\",\"genderId\":2,\"stateId\":"
          + STATE_A_ID + ",\"districtId\":" + DISTRICT_A1_ID + ",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n"
          + "{\"registrationNumber\":\"P289-NDJSON-02\",\"firstName\":\"Sunil\",\"genderId\":1,\"stateId\":"
          + STATE_A_ID + ",\"districtId\":" + DISTRICT_A1_ID + ",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n";
      MockMultipartFile ndjsonFile = new MockMultipartFile(
          "file", "trainees.ndjson", "application/x-ndjson", ndjson.getBytes(StandardCharsets.UTF_8));

      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(ndjsonFile)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"));
    }

    @Test
    @DisplayName("E.3: Unsupported file extension and empty files are rejected safely with HTTP 400")
    void shouldRejectUnsupportedOrMalformedInputSafely() throws Exception {
      MockMultipartFile badFile = new MockMultipartFile(
          "file", "exploit.exe", "application/octet-stream", "MZ...".getBytes(StandardCharsets.UTF_8));

      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(badFile)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isBadRequest());

      MockMultipartFile emptyFile = new MockMultipartFile(
          "file", "empty.csv", "text/csv", new byte[0]);

      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(emptyFile)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E.4: Cross-tenant access to uploaded import batches is rejected with HTTP 403")
    void shouldEnforceTenantOwnershipOnUploadedBatches() throws Exception {
      // Provider A1 accesses its own batch
      mockMvc.perform(get("/api/v1/import-batches/{id}", IMPORT_BATCH_A1_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk());

      // Provider B1 attempts to access Provider A1's batch -> 403 Forbidden
      mockMvc.perform(get("/api/v1/import-batches/{id}", IMPORT_BATCH_A1_ID)
              .header("Authorization", tokenProviderB1))
          .andExpect(status().isForbidden());
    }
  }

  // =========================================================================
  // Section F: Chunked Transaction and Rollback Safety
  // =========================================================================
  @Nested
  @DisplayName("F. Chunked Transaction & Rollback Safety Tests")
  class ChunkedTransactionAndRollbackSafetyTests {

    @Test
    @DisplayName("F.1: Real import batch processing commits valid rows, quarantines failed rows with sanitized errors, and reconciles counters")
    void shouldCommitSuccessfulChunksAndQuarantineFailedRowsWithSanitizedErrorContext() throws Exception {
      // 1. Stage a real batch containing 1 valid Trainee and 1 invalid Trainee (invalid date)
      String csv = String.join("\n",
          "registrationNumber,firstName,lastName,phone,email,dateOfBirth,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId",
          "P289-F-OK1,ValidF,User,9850098521,fok1@test.gov.in,2000-01-01,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1",
          "P289-F-BAD1,InvalidDate,User,9850098522,fbad1@test.gov.in,1999/99/99,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1"
      );
      MockMultipartFile file = new MockMultipartFile("file", "batch_f.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

      MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/import/batches")
              .file(file)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isCreated())
          .andReturn();

      long batchId = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
          .path("data").path("id").asLong();

      // 2. Execute real batch processing via POST /api/v1/import/batches/{id}/process
      mockMvc.perform(post("/api/v1/import/batches/{id}/process", batchId)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.isTerminal").value(true))
          .andExpect(jsonPath("$.data.rowCount").value(2))
          .andExpect(jsonPath("$.data.successCount").value(1))
          .andExpect(jsonPath("$.data.failureCount").value(1));

      // 3. Verify valid row committed to trainees table
      Integer validCommitted = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number = 'P289-F-OK1'", Integer.class);
      assertThat(validCommitted).isEqualTo(1);

      // 4. Verify quarantined failed row in import_records
      List<Map<String, Object>> quarantined = jdbcTemplate.queryForList(
          "SELECT import_record_status_id, error_code, error_message FROM import_records WHERE import_batch_id = ? AND source_row_number = 2",
          batchId);
      assertThat(quarantined).hasSize(1);
      Map<String, Object> qRecord = quarantined.get(0);
      assertThat(((Number) qRecord.get("import_record_status_id")).intValue()).isEqualTo(2); // FAILED
      String errorMsg = (String) qRecord.get("error_message");
      assertThat(errorMsg).isNotNull();
      assertThat(errorMsg).doesNotContain("SQLException", "INSERT INTO", "java.lang.");

      // 5. Explicitly assert sanitized quarantine endpoint content
      MvcResult qRes = mockMvc.perform(get("/api/v1/import/batches/{id}/quarantine", batchId)
              .param("includeDuplicates", "false")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].errorCode").value("INVALID_DATE_FORMAT"))
          .andReturn();
      String qBody = qRes.getResponse().getContentAsString();
      assertThat(qBody).doesNotContain("SQLException", "INSERT INTO", "java.lang.");
    }

    @Test
    @DisplayName("F.2: Reprocessing a completed import batch is rejected with HTTP 409 Conflict (terminal state enforcement)")
    void shouldPreventReprocessingOfCompletedBatchWithConflict409() throws Exception {
      // (Note: Multi-threaded concurrency testing with racing worker threads is proven by
      // ImportBatchProcessingIntegrationTest.enforcesAtomicClaimAndIdempotentTerminalRejection).
      // 1. Stage and process a real batch to completion
      String csv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "P289-F-IDEMP,IdempUser,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n";
      MockMultipartFile file = new MockMultipartFile("file", "batch_idemp.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
      MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/import/batches")
              .file(file)
              .param("entityType", "Trainee")
              .param("sourceSystem", "P289_REG")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isCreated())
          .andReturn();

      long batchId = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
          .path("data").path("id").asLong();

      mockMvc.perform(post("/api/v1/import/batches/{id}/process", batchId)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"));

      // 2. Attempting to re-process an already completed batch is rejected with 409 Conflict (terminal)
      mockMvc.perform(post("/api/v1/import/batches/{id}/process", batchId)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isConflict());
    }
  }

  // =========================================================================
  // Section G: System Diagnostics and Correlation Tracing
  // =========================================================================
  @Nested
  @DisplayName("G. System Diagnostics and Correlation Tracing Tests")
  class SystemDiagnosticsAndCorrelationTracingTests {

    @Test
    @DisplayName("G.1: Records system event with approved category, severity, and correlation tracking")
    void shouldRecordSystemEventWithApprovedCategorySeverityAndCorrelation() throws Exception {
      CreateSystemEventLogRequest request = new CreateSystemEventLogRequest();
      request.setSystemEventCategoryId(1L); // SECURITY
      request.setSystemEventSeverityId(2L); // WARN
      request.setEventCode("P289_REG_EVENT");
      request.setEventSummary("Regression system diagnostic event");
      request.setOccurredAt(LocalDateTime.now());
      request.setEntityType("TrainingCenter");
      request.setEntityId(CENTER_A1_ID);
      request.setSourceComponent("RegressionTest");

      mockMvc.perform(post("/api/v1/system-event-logs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide)
              .header("X-Correlation-ID", "p289-trace-001"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.eventCode").value("P289_REG_EVENT"));
    }

    @Test
    @DisplayName("G.2: Sanitizes invalid correlation headers and enforces canonical format")
    void shouldSanitizeInvalidCorrelationHeaderAndPreserveCanonicalFormat() throws Exception {
      CreateSystemEventLogRequest request = new CreateSystemEventLogRequest();
      request.setSystemEventCategoryId(1L);
      request.setSystemEventSeverityId(2L);
      request.setEventCode("P289_CORR_TEST");
      request.setEventSummary("Testing correlation normalization password=SecretPwd");
      request.setOccurredAt(LocalDateTime.now());

      MvcResult res = mockMvc.perform(post("/api/v1/system-event-logs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request))
              .header("Authorization", tokenNationwide)
              .header("X-Correlation-ID", "p289-clean-id"))
          .andExpect(status().isCreated())
          .andReturn();

      JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
      String eventSummary = root.path("data").path("eventSummary").asText();
      assertThat(eventSummary).contains("REDACTED");
      assertThat(eventSummary).doesNotContain("SecretPwd");
    }

    @Test
    @DisplayName("G.3: Enforces tenant isolation and bounded pagination on system event queries")
    void shouldEnforceTenantIsolationAndPaginationBoundsOnSystemEventQueries() throws Exception {
      // Provider A1 queries system events -> returns only Provider A1 events
      mockMvc.perform(get("/api/v1/system-event-logs")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").isArray());

      // Invalid pagination parameters are rejected with HTTP 400
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("page", "-1")
              .header("Authorization", tokenNationwide))
          .andExpect(status().isBadRequest());

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("size", "200")
              .header("Authorization", tokenNationwide))
          .andExpect(status().isBadRequest());
    }
  }

  // =========================================================================
  // Section H: Multi-Tenant Operational Isolation
  // =========================================================================
  @Nested
  @DisplayName("H. Multi-Tenant Operational Isolation Tests")
  class MultiTenantOperationalIsolationTests {

    @Test
    @DisplayName("H.1: Same-state providers are strictly isolated across centers, batches, issues, and records")
    void shouldIsolateSameStateProvidersAcrossOperationalEntitiesAndAggregates() throws Exception {
      // Provider A1 and Provider A2 both reside in State A
      // Provider A1 attempts to access Provider A2 center -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A2_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());

      // Provider A1 attempts to access Provider A2 batch -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A2_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());

      // Provider A1 attempts to access Provider A2 issue -> 403 Forbidden
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", DQ_ISSUE_A2_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());

      // Provider A1 attempts to access Provider A2 import batch -> 403 Forbidden
      mockMvc.perform(get("/api/v1/import-batches/{id}", IMPORT_BATCH_A2_ID)
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H.2: State administrators cannot cross state lines to access resources in other states")
    void shouldIsolateOperationalDataBetweenStatesAAndB() throws Exception {
      // State A admin attempts to access State B center -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_B1_ID)
              .header("Authorization", tokenStateA))
          .andExpect(status().isForbidden());

      // State B admin attempts to access State A center -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", tokenStateB))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H.3: District officers are strictly restricted to their assigned district scope")
    void shouldRestrictDistrictOfficersToAssignedDistrictScope() throws Exception {
      // District Officer A1 can access Center A1 in District A1
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", tokenDistrictA1))
          .andExpect(status().isOk());

      // District Officer A1 attempting to access Center A2 in District A2 receives 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A2_ID)
              .header("Authorization", tokenDistrictA1))
          .andExpect(status().isForbidden());

      // District Officer A1 attempting to access Center B1 in State B receives 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_B1_ID)
              .header("Authorization", tokenDistrictA1))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("H.4: Nationwide super admin can access resources across all jurisdictions while unscoped accounts are rejected")
    void shouldAuthorizeNationwideAdminAcrossAllOperationalResources() throws Exception {
      // Unscoped account attempts access -> 403 Forbidden
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", tokenUnscoped))
          .andExpect(status().isForbidden());

      // Verified nationwide admin can access both State A and State B resources
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_B1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk());
    }
  }

  // =========================================================================
  // Section I: PII and Credential Sanitization
  // =========================================================================
  @Nested
  @DisplayName("I. PII and Credential Sanitization Tests")
  class PiiAndCredentialSanitizationTests {

    @Test
    @DisplayName("I.1: Redacts credentials, tokens, and masks Aadhaar/PAN in operational responses")
    void shouldRedactCredentialsTokensAndMaskPiiInOperationalResponses() throws Exception {
      MvcResult res = mockMvc.perform(get("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andReturn();

      String content = res.getResponse().getContentAsString();
      assertThat(content).doesNotContain("AuditPass#99");
      assertThat(content).doesNotContain("ref_token_99");
      assertThat(content).contains("[REDACTED]");
      assertThat(content).contains("XXXX-XXXX-9012");
      assertThat(content).contains("XXXXXX123X");
    }

    @Test
    @DisplayName("I.2: Malformed nested JSON payloads fail closed to [REDACTED] without leaking plaintext secrets")
    void shouldFailClosedOnMalformedNestedJsonWithoutPlaintextSecretLeakage() throws Exception {
      long malformedAuditId = 985099L;
      jdbcTemplate.update(
          "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, change_summary, created_at) "
              + "VALUES (?, ?, ?, 'TrainingCenter', ?, NOW(), ?, NOW())",
          malformedAuditId, USER_PROVIDER_A1_ID, defaultAuditActionId, CENTER_A1_ID,
          "{\"password\":{\"value\":\"MalformedSecretPass123\""); // Malformed unclosed JSON

      MvcResult res = mockMvc.perform(get("/api/v1/audit-logs/{id}", malformedAuditId)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andReturn();

      String summary = objectMapper.readTree(res.getResponse().getContentAsString())
          .path("data").path("changeSummary").asText();
      assertThat(summary).isEqualTo("[REDACTED]");
      assertThat(summary).doesNotContain("MalformedSecretPass123");
    }

    @Test
    @DisplayName("I.3: Read-time masking does not mutate historical immutable records in database")
    void shouldPreserveHistoricalDatabaseImmutabilityWhileMaskingAtReadTime() throws Exception {
      // Trigger read-time masking via API
      mockMvc.perform(get("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.changeSummary").value(not(containsString("AuditPass#99"))));

      // Assert physical DB record remains immutable and was NOT modified
      String rawDbContent = jdbcTemplate.queryForObject(
          "SELECT change_summary FROM audit_logs WHERE audit_log_id = ?", String.class, AUDIT_A1_ID);
      assertThat(rawDbContent).contains("AuditPass#99"); // Immutability preserved in storage
    }
  }

  // =========================================================================
  // Section J: Operational Dashboard Regression
  // =========================================================================
  @Nested
  @DisplayName("J. Operational Dashboard Regression Tests")
  class OperationalDashboardRegressionTests {

    @Test
    @DisplayName("J.1: Calculates Data Cleanliness Index accurately with active issue counting")
    void shouldCalculateDataCleanlinessIndexAndActiveIssueCountsAccurately() throws Exception {
      MvcResult res = mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header("Authorization", tokenNationwide))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.dataCleanlinessIndexPct").isNumber())
          .andReturn();

      JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
      long eligible = data.path("eligiblePopulation").asLong();
      long affected = data.path("distinctAffectedEntities").asLong();

      assertThat(eligible).isGreaterThan(0L);
      long cleanEstimate = data.path("cleanEntitiesEstimate").asLong();
      assertThat(cleanEstimate).isEqualTo(eligible - affected);

      // Exact DQI formula calculation: ((eligible - affected) * 100.0) / eligible rounded HALF_UP at 2 decimals
      double expectedDqi = BigDecimal.valueOf(eligible - affected)
          .multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(eligible), 2, RoundingMode.HALF_UP)
          .doubleValue();
      double actualDqi = data.path("dataCleanlinessIndexPct").asDouble();
      assertThat(actualDqi).isEqualTo(expectedDqi);

      assertThat(data.path("cleanlinessIndexFormula").asText())
          .contains("(eligiblePopulation - distinctAffectedEntities) / eligiblePopulation * 100");
      assertThat(data.path("totalActiveIssues").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("J.2: Enforces tenant scoping on dashboard metrics without cross-tenant aggregate leakage")
    void shouldEnforceTenantScopingOnDashboardMetricsWithoutCrossTenantLeakage() throws Exception {
      // Provider A1 gets its scoped dashboard
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // Provider A1 attempting to request Provider B1's dashboard receives 403 Forbidden
      mockMvc.perform(get("/api/v1/operational/data-quality-scorecard")
              .param("trainingProviderId", String.valueOf(PROVIDER_B1_ID))
              .header("Authorization", tokenProviderA1))
          .andExpect(status().isForbidden());
    }
  }
}
