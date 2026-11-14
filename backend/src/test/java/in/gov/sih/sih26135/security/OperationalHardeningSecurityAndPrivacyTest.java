package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.8 - Role-Based Tenancy Isolation, PII Sanitization & Security Hardening Integration Tests")
class OperationalHardeningSecurityAndPrivacyTest {

  private static final String SAMPLE_JWT =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5NzQwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

  private static final long STATE_A_ID = 9741L;
  private static final long STATE_B_ID = 9742L;
  private static final long DISTRICT_A1_ID = 9741L;
  private static final long DISTRICT_B1_ID = 9742L;
  private static final long DISTRICT_A2_ID = 9743L;

  private static final long PROVIDER_A1_ID = 9741L;
  private static final long PROVIDER_B1_ID = 9742L;
  private static final long PROVIDER_A2_ID = 9743L; // Same state & district as PROVIDER_A1

  private static final long EMPLOYER_A_ID = 9741L;
  private static final long EMPLOYER_B_ID = 9742L;

  private static final long SECTOR_ID = 9741L;
  private static final long COURSE_ID = 9741L;
  private static final long ORG_ID = 9741L;
  private static final long DEPT_ID = 9741L;
  private static final long SCHEME_ID = 9741L;
  private static final long PROGRAM_ID = 9741L;

  private static final long CENTER_A1_ID = 9741L;
  private static final long CENTER_B1_ID = 9742L;
  private static final long CENTER_A2_ID = 9743L;

  private static final long BATCH_A1_ID = 9741L;
  private static final long BATCH_B1_ID = 9742L;
  private static final long BATCH_A2_ID = 9743L;

  private static final long TRAINEE_A1_ID = 9741L;
  private static final long TRAINEE_B1_ID = 9742L;
  private static final long TRAINEE_A2_ID = 9743L;

  private static final long ENROLLMENT_A1_ID = 9741L;
  private static final long ENROLLMENT_B1_ID = 9742L;
  private static final long ENROLLMENT_A2_ID = 9743L;

  private static final long PLACEMENT_A1_ID = 9741L;
  private static final long PLACEMENT_B1_ID = 9742L;

  private static final long USER_NATIONWIDE_ID = 9740L;
  private static final long USER_SCOPED_GOV_ID = 97401L;
  private static final long USER_STATE_A_ID = 9741L;
  private static final long USER_STATE_B_ID = 9742L;
  private static final long USER_DIST_A1_ID = 9743L;
  private static final long USER_PROVIDER_A1_ID = 9744L;
  private static final long USER_PROVIDER_A2_ID = 9745L;
  private static final long USER_EMPLOYER_A_ID = 9746L;
  private static final long USER_TRAINEE_A1_ID = 9747L;
  private static final long USER_UNSCOPED_ID = 9748L;
  private static final long USER_AMBIGUOUS_ID = 9749L;
  private static final long USER_DELETED_ID = 97402L;

  private static final long IMPORT_BATCH_A1_ID = 974001L;
  private static final long IMPORT_BATCH_A2_ID = 974002L;
  private static final long IMPORT_BATCH_B1_ID = 974003L;

  private static final long IMPORT_REC_A1_ID = 974011L;
  private static final long IMPORT_REC_A2_ID = 974012L;

  private static final long DQ_RULE_ID = 974001L;
  private static final long DQ_ISSUE_A1_ID = 974021L;
  private static final long DQ_ISSUE_A2_ID = 974022L;
  private static final long DQ_ISSUE_B1_ID = 974023L;
  private static final long DQ_EVENT_A1_ID = 974031L;
  private static final long DQ_EVENT_B1_ID = 974032L;

  private static final long AUDIT_A1_ID = 974041L;
  private static final long AUDIT_B1_ID = 974042L;
  private static final long AUDIT_A2_ID = 974043L;

  private static final long EVENT_A1_ID = 974051L;
  private static final long EVENT_A2_ID = 974052L;
  private static final long EVENT_B1_ID = 974053L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private String nationwideToken;
  private String scopedGovAdminToken;
  private String stateAToken;
  private String stateBToken;
  private String districtA1Token;
  private String providerA1Token;
  private String providerA2Token;
  private String employerAToken;
  private String traineeA1Token;
  private String unscopedToken;
  private String ambiguousToken;
  private String deletedUserToken;
  private String readOnlyStateAToken;

  private Long defaultAuditActionId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    cleanUpFixtures();

    // Provision geographic and organizational hierarchy
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, '8A', 'Phase288 State A', 'LG9741', 1, NOW(), NOW()), "
            + "(?, '8B', 'Phase288 State B', 'LG9742', 1, NOW(), NOW())",
        STATE_A_ID, STATE_B_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, '8D1', 'District A1', 'LGD9741', 1, NOW(), NOW()), "
            + "(?, ?, '8D2', 'District B1', 'LGD9742', 1, NOW(), NOW()), "
            + "(?, ?, '8D3', 'District A2', 'LGD9743', 1, NOW(), NOW())",
        DISTRICT_A1_ID, STATE_A_ID,
        DISTRICT_B1_ID, STATE_B_ID,
        DISTRICT_A2_ID, STATE_A_ID);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9741', 'Provider A1', 'REG-PRV-9741', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'PRV-9742', 'Provider B1', 'REG-PRV-9742', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'PRV-9743', 'Provider A2', 'REG-PRV-9743', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID,
        PROVIDER_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-9741', 'Employer A', 'REG-EMP-9741', 1, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'EMP-9742', 'Employer B', 'REG-EMP-9742', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID,
        EMPLOYER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-9741', 'Phase288 Sector', 1, NOW(), NOW())",
        SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-9741', 'Phase288 Course', ?, 1, 1, NOW(), NOW(), NULL)",
        COURSE_ID, SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-9741', 'Phase288 Org', 1, 1, NOW(), NOW())",
        ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-9741', 'Phase288 Dept', 1, NOW(), NOW())",
        DEPT_ID, ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-9741', 'Phase288 Scheme', ?, 'INR', 1, NOW(), NOW())",
        SCHEME_ID, DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-9741', 'Phase288 Program', ?, ?, 1, NOW(), NOW())",
        PROGRAM_ID, DEPT_ID, SCHEME_ID);

    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-9741', 'Center A1', ?, ?, 1, NOW(), NOW(), NULL), "
            + "(?, ?, 'CTR-9742', 'Center B1', ?, ?, 1, NOW(), NOW(), NULL), "
            + "(?, ?, 'CTR-9743', 'Center A2', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A1_ID, PROVIDER_A1_ID, STATE_A_ID, DISTRICT_A1_ID,
        CENTER_B1_ID, PROVIDER_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        CENTER_A2_ID, PROVIDER_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-9741', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL), "
            + "(?, 'BAT-9742', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL), "
            + "(?, 'BAT-9743', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A1_ID, COURSE_ID, PROVIDER_A1_ID, CENTER_A1_ID, PROGRAM_ID,
        BATCH_B1_ID, COURSE_ID, PROVIDER_B1_ID, CENTER_B1_ID, PROGRAM_ID,
        BATCH_A2_ID, COURSE_ID, PROVIDER_A2_ID, CENTER_A2_ID, PROGRAM_ID);

    // Authoritative users (must be seeded before trainee user_id FK / import batches)
    insertUser(USER_NATIONWIDE_ID, "p288_super", "p288_super@test.gov.in", null, null, null, null, null, "super_admin");
    insertUser(USER_SCOPED_GOV_ID, "p288_scoped_gov", "p288_sg@test.gov.in", STATE_A_ID, null, null, null, null, "government_admin");
    insertUser(USER_STATE_A_ID, "p288_state_a", "p288_sa@test.gov.in", STATE_A_ID, null, null, null, null, "state_admin");
    insertUser(USER_STATE_B_ID, "p288_state_b", "p288_sb@test.gov.in", STATE_B_ID, null, null, null, null, "state_admin");
    insertUser(USER_DIST_A1_ID, "p288_dist_a1", "p288_da1@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, null, "district_officer");
    insertUser(USER_PROVIDER_A1_ID, "p288_prov_a1", "p288_pa1@test.gov.in", null, null, PROVIDER_A1_ID, null, null, "training_provider");
    insertUser(USER_PROVIDER_A2_ID, "p288_prov_a2", "p288_pa2@test.gov.in", null, null, PROVIDER_A2_ID, null, null, "training_provider");
    insertUser(USER_EMPLOYER_A_ID, "p288_emp_a", "p288_ea@test.gov.in", null, null, null, EMPLOYER_A_ID, null, "employer");
    insertUser(USER_TRAINEE_A1_ID, "p288_trainee", "p288_tr@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null, null, "trainee");
    insertUser(USER_UNSCOPED_ID, "p288_unscoped", "p288_unscoped@test.gov.in", null, null, null, null, null, null);
    insertUser(USER_AMBIGUOUS_ID, "p288_ambig", "p288_ambig@test.gov.in", STATE_A_ID, null, PROVIDER_A1_ID, null, null, "training_provider");
    insertUser(USER_DELETED_ID, "p288_deleted", "p288_del@test.gov.in", STATE_A_ID, null, null, null, "NOW()", "state_admin");

    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, user_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'P288-TR-9741', 'TrA1', 'User', '9840097411', 'tr9741@test.gov.in', '2000-01-01', 1, ?, ?, ?, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'P288-TR-9742', 'TrB1', 'User', '9840097412', 'tr9742@test.gov.in', '2000-01-01', 1, ?, ?, NULL, 1, 1, NOW(), NOW(), NULL), "
            + "(?, 'P288-TR-9743', 'TrA2', 'User', '9840097413', 'tr9743@test.gov.in', '2000-01-01', 1, ?, ?, NULL, 1, 1, NOW(), NOW(), NULL)",
        TRAINEE_A1_ID, STATE_A_ID, DISTRICT_A1_ID, USER_TRAINEE_A1_ID,
        TRAINEE_B1_ID, STATE_B_ID, DISTRICT_B1_ID,
        TRAINEE_A2_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'P288-ENR-9741', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL), "
            + "(?, 'P288-ENR-9742', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL), "
            + "(?, 'P288-ENR-9743', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)",
        ENROLLMENT_A1_ID, TRAINEE_A1_ID, PROGRAM_ID, COURSE_ID, PROVIDER_A1_ID, CENTER_A1_ID, BATCH_A1_ID,
        ENROLLMENT_B1_ID, TRAINEE_B1_ID, PROGRAM_ID, COURSE_ID, PROVIDER_B1_ID, CENTER_B1_ID, BATCH_B1_ID,
        ENROLLMENT_A2_ID, TRAINEE_A2_ID, PROGRAM_ID, COURSE_ID, PROVIDER_A2_ID, CENTER_A2_ID, BATCH_A2_ID);

    jdbcTemplate.update(
        "INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, work_state_id, work_district_id, record_verification_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'P288-PLC-9741', ?, ?, ?, ?, ?, ?, 1, 1, 1, 1, 25000.00, ?, ?, 1, NOW(), NOW(), NULL), "
            + "(?, 'P288-PLC-9742', ?, ?, ?, ?, ?, ?, 1, 1, 1, 1, 25000.00, ?, ?, 1, NOW(), NOW(), NULL)",
        PLACEMENT_A1_ID, TRAINEE_A1_ID, ENROLLMENT_A1_ID, COURSE_ID, PROGRAM_ID, PROVIDER_A1_ID, EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID,
        PLACEMENT_B1_ID, TRAINEE_B1_ID, ENROLLMENT_B1_ID, COURSE_ID, PROGRAM_ID, PROVIDER_B1_ID, EMPLOYER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    // Seed operational data: rules, issues, issue events (with historical unmasked PII/secrets)
    jdbcTemplate.update(
        "INSERT INTO data_quality_rules (data_quality_rule_id, rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P288_RULE', 'Phase288 Rule', 'Hardening verification rule', 'Trainee', 2, 3, 1, NOW(), NOW())",
        DQ_RULE_ID);

    jdbcTemplate.update(
        "INSERT INTO data_quality_issues (data_quality_issue_id, data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, resolution_notes, detected_at, created_at, updated_at) "
            + "VALUES (?, ?, 2, 3, 1, 1, 'Trainee', ?, ?, ?, NOW(), NOW(), NOW()), "
            + "(?, ?, 2, 3, 1, 1, 'Trainee', ?, 'Issue A2 provider 9743 summary', NULL, NOW(), NOW(), NOW()), "
            + "(?, ?, 2, 3, 1, 1, 'Trainee', ?, 'Issue B1 tenant summary password=LeakStateB', NULL, NOW(), NOW(), NOW())",
        DQ_ISSUE_A1_ID, DQ_RULE_ID, TRAINEE_A1_ID,
        "Legacy DQ summary password=DqSecret99 aadhaar=234567890123 PAN=ABCDE1234F",
        "Legacy resolution token=" + SAMPLE_JWT,
        DQ_ISSUE_A2_ID, DQ_RULE_ID, TRAINEE_A2_ID,
        DQ_ISSUE_B1_ID, DQ_RULE_ID, TRAINEE_B1_ID);

    jdbcTemplate.update(
        "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, remarks, created_at) "
            + "VALUES (?, ?, 1, ?, ?, NOW()), (?, ?, 1, ?, 'Cross-tenant B event', NOW())",
        DQ_EVENT_A1_ID, DQ_ISSUE_A1_ID, USER_NATIONWIDE_ID,
        "Historical event clientSecret=EvtSec11 aadhaar=1234-5678-1234 PAN=ABCDE1234F",
        DQ_EVENT_B1_ID, DQ_ISSUE_B1_ID, USER_NATIONWIDE_ID);

    // Seed import batches & records
    jdbcTemplate.update(
        "INSERT INTO import_batches (import_batch_id, batch_code, source_system, entity_type, import_batch_status_id, initiated_by_user_id, row_count, success_count, failure_count, created_at, updated_at) "
            + "VALUES (?, 'IMP-974001', 'WEB_PORTAL', 'Trainee', 4, ?, 1, 0, 1, NOW(), NOW()), "
            + "(?, 'IMP-974002', 'WEB_PORTAL', 'Trainee', 4, ?, 1, 0, 1, NOW(), NOW()), "
            + "(?, 'IMP-974003', 'WEB_PORTAL', 'Trainee', 4, ?, 1, 0, 1, NOW(), NOW())",
        IMPORT_BATCH_A1_ID, USER_PROVIDER_A1_ID,
        IMPORT_BATCH_A2_ID, USER_PROVIDER_A2_ID,
        IMPORT_BATCH_B1_ID, USER_STATE_B_ID);

    jdbcTemplate.update(
        "INSERT INTO import_records (import_record_id, import_batch_id, source_row_number, entity_type, entity_id, import_record_status_id, error_code, error_message, created_at, updated_at) "
            + "VALUES (?, ?, 1, 'Trainee', ?, 2, 'VALIDATION_ERROR', ?, NOW(), NOW()), "
            + "(?, ?, 1, 'Trainee', ?, 2, 'VALIDATION_ERROR', 'Provider A2 failure error', NOW(), NOW())",
        IMPORT_REC_A1_ID, IMPORT_BATCH_A1_ID, TRAINEE_A1_ID,
        "Legacy import error password=ImpSecret123 aadhaar=2345 6789 0123 PAN=ABCDE1234F",
        IMPORT_REC_A2_ID, IMPORT_BATCH_A2_ID, TRAINEE_A2_ID);

    // Seed historical unmasked audit_logs and system_event_logs
    defaultAuditActionId = jdbcTemplate.queryForObject(
        "SELECT MIN(audit_action_id) FROM ref_audit_action", Long.class);

    jdbcTemplate.update(
        "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, correlation_id, change_summary, created_at) "
            + "VALUES (?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 2 HOUR, 'p288-corr-a1', ?, NOW()), "
            + "(?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 1 HOUR, 'p288-corr-shared', ?, NOW()), "
            + "(?, ?, ?, 'TrainingCenter', ?, NOW() - INTERVAL 30 MINUTE, 'p288-corr-a2', ?, NOW())",
        AUDIT_A1_ID, USER_PROVIDER_A1_ID, defaultAuditActionId, CENTER_A1_ID,
        "{\"password\":\"LegacyPwd#99\",\"refreshToken\":\"ref_secret_99\",\"aadhaar\":\"234567890123\",\"pan\":\"ABCDE1234F\"}",
        AUDIT_B1_ID, USER_STATE_B_ID, defaultAuditActionId, CENTER_B1_ID,
        "Authorization: Bearer " + SAMPLE_JWT + " secretKey=B1Secret",
        AUDIT_A2_ID, USER_PROVIDER_A2_ID, defaultAuditActionId, CENTER_A2_ID,
        "Provider A2 change apiKey=provA2Key");

    jdbcTemplate.update(
        "INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, actor_user_id, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) "
            + "VALUES (?, 1, 3, 'P288_EVT_A1', ?, 'TrainingCenter', ?, 'SecurityTest', ?, NOW() - INTERVAL 2 HOUR, NOW()), "
            + "(?, 1, 3, 'P288_EVT_A2', ?, 'TrainingCenter', ?, 'SecurityTest', ?, NOW() - INTERVAL 1 HOUR, NOW()), "
            + "(?, 1, 3, 'P288_EVT_B1', ?, 'TrainingCenter', ?, 'SecurityTest', ?, NOW() - INTERVAL 30 MINUTE, NOW())",
        EVENT_A1_ID, USER_PROVIDER_A1_ID, CENTER_A1_ID,
        "[cid=p288-shared-cid|rid=p288-req-a1] Login fail password=EvtPwd88 jwt=" + SAMPLE_JWT + " aadhaar=1234-5678-1234 PAN=ABCDE1234F",
        EVENT_A2_ID, USER_PROVIDER_A2_ID, CENTER_A2_ID,
        "[cid=p288-shared-cid|rid=p288-req-a2] Provider A2 failure client_secret=A2Sec",
        EVENT_B1_ID, USER_STATE_B_ID, CENTER_B1_ID,
        "[cid=p288-shared-cid|rid=p288-req-b1] State B secret failure access_token=B1Tok");

    // Issue JWT tokens aligned to each persona
    nationwideToken = bearer(USER_NATIONWIDE_ID, "p288_super",
        List.of("ROLE_super_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    scopedGovAdminToken = bearer(USER_SCOPED_GOV_ID, "p288_scoped_gov",
        List.of("ROLE_government_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    stateAToken = bearer(USER_STATE_A_ID, "p288_state_a",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    stateBToken = bearer(USER_STATE_B_ID, "p288_state_b",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    districtA1Token = bearer(USER_DIST_A1_ID, "p288_dist_a1",
        List.of("ROLE_district_officer", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    providerA1Token = bearer(USER_PROVIDER_A1_ID, "p288_prov_a1",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    providerA2Token = bearer(USER_PROVIDER_A2_ID, "p288_prov_a2",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    employerAToken = bearer(USER_EMPLOYER_A_ID, "p288_emp_a",
        List.of("ROLE_employer", "audit.read", "system.manage", "center.manage", "course.manage", "import.execute", "analytics.view"));
    traineeA1Token = bearer(USER_TRAINEE_A1_ID, "p288_trainee",
        List.of("ROLE_trainee", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    unscopedToken = bearer(USER_UNSCOPED_ID, "p288_unscoped",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    ambiguousToken = bearer(USER_AMBIGUOUS_ID, "p288_ambig",
        List.of("ROLE_training_provider", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    deletedUserToken = bearer(USER_DELETED_ID, "p288_deleted",
        List.of("ROLE_state_admin", "audit.read", "system.manage", "center.manage", "course.manage", "analytics.view"));
    readOnlyStateAToken = bearer(USER_STATE_A_ID, "p288_state_a",
        List.of("ROLE_state_admin", "audit.read"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    cleanUpFixtures();
  }

  private String bearer(Long userId, String username, List<String> authorities) {
    return "Bearer " + jwtTokenProvider.generateAccessToken(userId, username, authorities);
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

  private void cleanUpFixtures() {
    try {
      jdbcTemplate.update("DELETE FROM audit_logs WHERE audit_log_id BETWEEN 974000 AND 974999 OR actor_user_id BETWEEN 9740 AND 9749 OR actor_user_id IN (97401, 97402) OR correlation_id LIKE 'p288-%'");
      jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id BETWEEN 974000 AND 974999 OR actor_user_id BETWEEN 9740 AND 9749 OR actor_user_id IN (97401, 97402) OR event_code LIKE 'P288%'");
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 974000 AND 979999 OR data_quality_issue_id BETWEEN 974000 AND 979999 OR remarks LIKE 'P288-%'");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_issue_id BETWEEN 974000 AND 979999");
      jdbcTemplate.update("DELETE FROM data_quality_rules WHERE data_quality_rule_id = ? OR rule_code LIKE 'P288_%'", DQ_RULE_ID);
      jdbcTemplate.update("DELETE FROM import_records WHERE import_record_id BETWEEN 974000 AND 974999 OR import_batch_id BETWEEN 974000 AND 974999");
      jdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id BETWEEN 974000 AND 974999 OR initiated_by_user_id BETWEEN 9740 AND 9749");
      jdbcTemplate.update("DELETE FROM placement_records WHERE placement_id IN (?, ?)", PLACEMENT_A1_ID, PLACEMENT_B1_ID);
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id IN (?, ?, ?)", ENROLLMENT_A1_ID, ENROLLMENT_B1_ID, ENROLLMENT_A2_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id IN (?, ?, ?)", TRAINEE_A1_ID, TRAINEE_B1_ID, TRAINEE_A2_ID);
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?, ?)", BATCH_A1_ID, BATCH_B1_ID, BATCH_A2_ID);
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (?, ?, ?) OR center_code LIKE 'CTR-P288-%'", CENTER_A1_ID, CENTER_B1_ID, CENTER_A2_ID);
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", COURSE_ID);
      jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", PROGRAM_ID);
      jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", SCHEME_ID);
      jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", DEPT_ID);
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", ORG_ID);
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SECTOR_ID);
      jdbcTemplate.update("DELETE FROM user_roles WHERE user_id BETWEEN 9740 AND 9749 OR user_id IN (97401, 97402)");
      jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 9740 AND 9749 OR user_id IN (97401, 97402)");
      jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (?, ?)", EMPLOYER_A_ID, EMPLOYER_B_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?, ?)", PROVIDER_A1_ID, PROVIDER_B1_ID, PROVIDER_A2_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)", DISTRICT_A1_ID, DISTRICT_B1_ID, DISTRICT_A2_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)", STATE_A_ID, STATE_B_ID);
    } catch (Exception ignored) {
    }
  }

  @Nested
  @DisplayName("1. Multi-Tenant & Role-Based Operational Isolation")
  class TenancyIsolationTests {

    @Test
    @DisplayName("Blocks unauthenticated, deleted, unscoped, ambiguous, and scoped-gov-admin callers cleanly")
    void blocksInvalidCallerStates() throws Exception {
      mockMvc.perform(get("/api/v1/system-event-logs"))
          .andExpect(status().isUnauthorized());

      for (String invalidToken : List.of(deletedUserToken, unscopedToken, ambiguousToken)) {
        mockMvc.perform(get("/api/v1/system-event-logs")
                .header("Authorization", invalidToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
                .header("Authorization", invalidToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A1_ID)
                .header("Authorization", invalidToken))
            .andExpect(status().isForbidden());
      }

      // Scoped ROLE_government_admin (stateId != null) must NOT bypass nationwide checks
      mockMvc.perform(get("/api/v1/audit-logs")
              .header("Authorization", scopedGovAdminToken))
          .andExpect(status().isForbidden());

      // User ID 1 / username "admin" with management permissions but WITHOUT verified nationwide role rejected
      String adminWithoutRoleToken = bearer(1L, "admin",
          List.of("center.manage", "course.manage", "provider.manage"));
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", adminWithoutRoleToken))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A1_ID)
              .header("Authorization", adminWithoutRoleToken))
          .andExpect(status().isForbidden());

      // Unscoped caller with management permissions is strictly rejected
      String unscopedManageToken = bearer(USER_UNSCOPED_ID, "p288_unscoped_mgr",
          List.of("center.manage", "course.manage", "provider.manage"));
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", unscopedManageToken))
          .andExpect(status().isForbidden());

      // Verified nationwide administrator is accepted
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", nationwideToken))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Enforces same-state Provider A1 vs Provider A2 and State A vs State B isolation on TrainingCenter & TrainingBatch")
    void enforcesTrainingCenterAndBatchTenantBoundaries() throws Exception {
      // Provider A1 sees own center and batch, blocked from Provider A2 (same state & district) and State B
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(CENTER_A1_ID));

      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A2_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_B1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(BATCH_A1_ID));

      mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A2_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isForbidden());

      // Provider A1 paginated list contains only Provider A1 centers & batches (never A2 or B1)
      mockMvc.perform(get("/api/v1/training-centers?page=0&size=20")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(CENTER_A1_ID));

      mockMvc.perform(get("/api/v1/training-batches?page=0&size=20")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(BATCH_A1_ID));

      // Same-course batches must still be strictly tenant-scoped for Provider A1 & State A
      mockMvc.perform(get("/api/v1/training-batches?courseId={id}&page=0&size=20", COURSE_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(BATCH_A1_ID));

      // State A sees Center A1 + Center A2 (2 total), NOT Center B1
      mockMvc.perform(get("/api/v1/training-centers?page=0&size=20")
              .header("Authorization", stateAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(2));

      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_B1_ID)
              .header("Authorization", stateAToken))
          .andExpect(status().isForbidden());

      // Employer and Trainee are denied from TrainingCenter & TrainingBatch endpoints
      mockMvc.perform(get("/api/v1/training-centers/{id}", CENTER_A1_ID)
              .header("Authorization", employerAToken))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/training-batches/{id}", BATCH_A1_ID)
              .header("Authorization", traineeA1Token))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Enforces privilege separation on DataQualityRules and DataQualityIssueEvents")
    void enforcesPrivilegeSeparationAndIssueEventTenancy() throws Exception {
      // Read-only state analyst with audit.read cannot create/update/delete rules or record issue events
      mockMvc.perform(post("/api/v1/data-quality-rules")
              .header("Authorization", readOnlyStateAToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"ruleCode\":\"P288_DENY\",\"ruleName\":\"Denied\",\"targetEntityType\":\"Trainee\",\"dataQualityCategoryId\":2,\"dataQualitySeverityId\":3}"))
          .andExpect(status().isForbidden());

      mockMvc.perform(delete("/api/v1/data-quality-rules/{id}", DQ_RULE_ID)
              .header("Authorization", readOnlyStateAToken))
          .andExpect(status().isForbidden());

      mockMvc.perform(post("/api/v1/data-quality-issue-events")
              .header("Authorization", readOnlyStateAToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"dataQualityIssueId\":" + DQ_ISSUE_A1_ID + ",\"dataQualityIssueStatusId\":2,\"remarks\":\"P288-WR-DENY\"}"))
          .andExpect(status().isForbidden());

      // Provider A1 cannot access Provider A2 or State B issue events
      mockMvc.perform(get("/api/v1/data-quality-issue-events/{id}", DQ_EVENT_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk());

      mockMvc.perform(get("/api/v1/data-quality-issue-events/{id}", DQ_EVENT_B1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/data-quality-issue-events?issueId={id}", DQ_ISSUE_B1_ID)
              .header("Authorization", stateAToken))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Enforces correlationId filtering within caller tenant across system events and audit logs")
    void enforcesCorrelationFilteringAndDistrictEntityAccess() throws Exception {
      // Correlation filter p288-shared-cid has events in Provider A1 (A1), Provider A2 (A2), State B (B1)
      mockMvc.perform(get("/api/v1/system-event-logs?correlationId=p288-shared-cid&page=0&size=20")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(EVENT_A1_ID));

      mockMvc.perform(get("/api/v1/system-event-logs?correlationId=p288-shared-cid&page=0&size=20")
              .header("Authorization", stateBToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(EVENT_B1_ID));

      // District A1 Officer can read AuditLog timeline for Center A1 in District A1, denied for Center B1
      mockMvc.perform(get("/api/v1/audit-logs/entities/TrainingCenter/{id}/timeline", CENTER_A1_ID)
              .header("Authorization", districtA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.timelineEntries[0].auditLogId").value(AUDIT_A1_ID));

      mockMvc.perform(get("/api/v1/audit-logs/entities/TrainingCenter/{id}/timeline", CENTER_B1_ID)
              .header("Authorization", districtA1Token))
          .andExpect(status().isForbidden());

      // ImportBatch query-param endpoints isolate Provider A1 from Provider A2 & State B in SQL
      mockMvc.perform(get("/api/v1/import-batches?statusId=4")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(IMPORT_BATCH_A1_ID));
    }
  }

  @Nested
  @DisplayName("2. Write-Time & Historical Read-Time PII & Credential Sanitization")
  class PrivacySanitizationTests {

    @Test
    @DisplayName("Sanitizes historical unmasked secrets, JWTs, Aadhaar & PAN at read time without mutating immutable audit_logs")
    void masksHistoricalUnmaskedRowsWithoutMutatingDb() throws Exception {
      // AuditLog getById & paginated search mask historical unmasked row while preserving DB row immutability
      mockMvc.perform(get("/api/v1/audit-logs/{id}", AUDIT_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.changeSummary", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.changeSummary", containsString("XXXX-XXXX-0123")))
          .andExpect(jsonPath("$.data.changeSummary", containsString("XXXXXX123X")))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString("LegacyPwd#99"))))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString("ref_secret_99"))))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString("234567890123"))))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString("ABCDE1234F"))));

      mockMvc.perform(get("/api/v1/audit-logs/{id}", AUDIT_B1_ID)
              .header("Authorization", stateBToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.changeSummary", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString(SAMPLE_JWT))))
          .andExpect(jsonPath("$.data.changeSummary", not(containsString("B1Secret"))));

      // Verify immutable audit_logs DB row was NOT modified on read
      String dbRawSummary = jdbcTemplate.queryForObject(
          "SELECT change_summary FROM audit_logs WHERE audit_log_id = ?",
          String.class, AUDIT_A1_ID);
      assertThat(dbRawSummary).contains("LegacyPwd#99").contains("234567890123").contains("ABCDE1234F");

      // SystemEventLog masks secrets & PII while preserving [cid=...|rid=...] tag
      mockMvc.perform(get("/api/v1/system-event-logs/{id}", EVENT_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.eventSummary", containsString("[cid=p288-shared-cid|rid=p288-req-a1]")))
          .andExpect(jsonPath("$.data.eventSummary", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.eventSummary", containsString("XXXX-XXXX-1234")))
          .andExpect(jsonPath("$.data.eventSummary", containsString("XXXXXX123X")))
          .andExpect(jsonPath("$.data.eventSummary", not(containsString("EvtPwd88"))))
          .andExpect(jsonPath("$.data.eventSummary", not(containsString(SAMPLE_JWT))));

      // DataQualityIssue & DataQualityIssueEvent mask historical rows at read time
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}", DQ_ISSUE_A1_ID)
              .header("Authorization", stateAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.issueSummary", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.issueSummary", containsString("XXXX-XXXX-0123")))
          .andExpect(jsonPath("$.data.issueSummary", containsString("XXXXXX123X")))
          .andExpect(jsonPath("$.data.resolutionNotes", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.issueSummary", not(containsString("DqSecret99"))))
          .andExpect(jsonPath("$.data.resolutionNotes", not(containsString(SAMPLE_JWT))));

      mockMvc.perform(get("/api/v1/data-quality-issue-events/{id}", DQ_EVENT_A1_ID)
              .header("Authorization", stateAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.remarks", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.remarks", containsString("XXXX-XXXX-1234")))
          .andExpect(jsonPath("$.data.remarks", containsString("XXXXXX123X")))
          .andExpect(jsonPath("$.data.remarks", not(containsString("EvtSec11"))));

      // ImportRecord masks historical unmasked error messages
      mockMvc.perform(get("/api/v1/import-records/{id}", IMPORT_REC_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.errorMessage", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.errorMessage", containsString("XXXX-XXXX-0123")))
          .andExpect(jsonPath("$.data.errorMessage", containsString("XXXXXX123X")))
          .andExpect(jsonPath("$.data.errorMessage", not(containsString("ImpSecret123"))));

      // Nested JSON sensitive values: {"password":{"value":"..."}} and {"accessToken":["..."]}
      String nestedJsonAudit = "{\"password\":{\"value\":\"NestedSecretPwd99\"},\"accessToken\":[\"tokA\",\"tokB\"],\"info\":\"safeVal\"}";
      Long nestedAuditId = 974099L;
      jdbcTemplate.update(
          "INSERT INTO audit_logs (audit_log_id, actor_user_id, audit_action_id, entity_type, entity_id, occurred_at, correlation_id, change_summary, created_at) "
              + "VALUES (?, ?, ?, 'TrainingCenter', ?, NOW(), 'p288-corr-nest', ?, NOW())",
          nestedAuditId, USER_PROVIDER_A1_ID, defaultAuditActionId, CENTER_A1_ID, nestedJsonAudit);
      try {
        mockMvc.perform(get("/api/v1/audit-logs/{id}", nestedAuditId)
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.changeSummary", containsString("\"password\":\"[REDACTED]\"")))
            .andExpect(jsonPath("$.data.changeSummary", containsString("\"accessToken\":\"[REDACTED]\"")))
            .andExpect(jsonPath("$.data.changeSummary", containsString("\"info\":\"safeVal\"")))
            .andExpect(jsonPath("$.data.changeSummary", not(containsString("NestedSecretPwd99"))))
            .andExpect(jsonPath("$.data.changeSummary", not(containsString("tokA"))));
      } finally {
        jdbcTemplate.update("DELETE FROM audit_logs WHERE audit_log_id = ?", nestedAuditId);
      }
    }

    @Test
    @DisplayName("Sanitizes newly written audit logs and workflow issue events at write time")
    void sanitizesPayloadsAtWriteTime() throws Exception {
      KeyHolder kh = new GeneratedKeyHolder();
      String eventBody = "{\"dataQualityIssueId\":" + DQ_ISSUE_A1_ID
          + ",\"dataQualityIssueStatusId\":2"
          + ",\"remarks\":\"P288-WR-OK password=WritePwd1 aadhaar=234567890123 PAN=ABCDE1234F\"}";

      MvcResult evRes = mockMvc.perform(post("/api/v1/data-quality-issue-events")
              .header("Authorization", stateAToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(eventBody))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.remarks", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.remarks", containsString("XXXX-XXXX-0123")))
          .andExpect(jsonPath("$.data.remarks", containsString("XXXXXX123X")))
          .andReturn();

      Long createdEventId = objectMapper.readTree(evRes.getResponse().getContentAsString())
          .path("data").path("id").asLong();
      String persistedRemark = jdbcTemplate.queryForObject(
          "SELECT remarks FROM data_quality_issue_events WHERE data_quality_issue_event_id = ?",
          String.class, createdEventId);
      assertThat(persistedRemark)
          .contains("[REDACTED]")
          .contains("XXXX-XXXX-0123")
          .contains("XXXXXX123X")
          .doesNotContain("WritePwd1")
          .doesNotContain("234567890123")
          .doesNotContain("ABCDE1234F");

      Long manualAuditActionId = jdbcTemplate.queryForObject(
          "SELECT MIN(audit_action_id) FROM ref_audit_action WHERE UPPER(action_code) IN ('UPDATE', 'EXPORT')",
          Long.class);
      // Audit log write-time sanitization via API (requires super_admin / nationwide)
      MvcResult auditRes = mockMvc.perform(post("/api/v1/audit-logs")
              .header("Authorization", nationwideToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"auditActionId\":" + manualAuditActionId
                  + ",\"entityType\":\"TrainingCenter\",\"entityId\":" + CENTER_A1_ID
                  + ",\"changeSummary\":\"apiKey=WriteKey77 aadhaar=1234-5678-1234\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.changeSummary", containsString("[REDACTED]")))
          .andExpect(jsonPath("$.data.changeSummary", containsString("XXXX-XXXX-1234")))
          .andReturn();

      Long createdAuditId = objectMapper.readTree(auditRes.getResponse().getContentAsString())
          .path("data").path("id").asLong();
      try {
        String persistedAudit = jdbcTemplate.queryForObject(
            "SELECT change_summary FROM audit_logs WHERE audit_log_id = ?",
            String.class, createdAuditId);
        assertThat(persistedAudit).contains("[REDACTED]").contains("XXXX-XXXX-1234").doesNotContain("WriteKey77");
      } finally {
        jdbcTemplate.update("DELETE FROM audit_logs WHERE audit_log_id = ?", createdAuditId);
      }
    }
  }

  @Nested
  @DisplayName("3. Bounded Pagination, Allowlisted Sorting & Edge Case Hardening")
  class PaginationHardeningTests {

    @Test
    @DisplayName("Rejects invalid page/size bounds, disallowed sort fields, and unknown parameters cleanly")
    void enforcesPaginationAndSortContracts() throws Exception {
      List<String> paginatedEndpoints = List.of(
          "/api/v1/audit-logs",
          "/api/v1/system-event-logs",
          "/api/v1/import-batches/" + IMPORT_BATCH_A1_ID + "/records",
          "/api/v1/import-records?statusId=2",
          "/api/v1/training-centers",
          "/api/v1/training-batches");

      for (String base : paginatedEndpoints) {
        String sep = base.contains("?") ? "&" : "?";

        // page < 0 -> 400
        mockMvc.perform(get(base + sep + "page=-1&size=10")
                .header("Authorization", nationwideToken))
            .andExpect(status().isBadRequest());

        // size < 1 -> 400
        mockMvc.perform(get(base + sep + "page=0&size=0")
                .header("Authorization", nationwideToken))
            .andExpect(status().isBadRequest());

        // size > 100 -> 400
        mockMvc.perform(get(base + sep + "page=0&size=101")
                .header("Authorization", nationwideToken))
            .andExpect(status().isBadRequest());

        // disallowed sort -> 400
        mockMvc.perform(get(base + sep + "page=0&size=10&sort=evilInjectedField,desc")
                .header("Authorization", nationwideToken))
            .andExpect(status().isBadRequest());

        // unsupported query parameter -> 400 on strict operational endpoints
        if (base.startsWith("/api/v1/audit-logs")
            || base.startsWith("/api/v1/system-event-logs")
            || base.startsWith("/api/v1/import-")) {
          mockMvc.perform(get(base + sep + "page=0&size=10&unknownParam=abc")
                  .header("Authorization", nationwideToken))
              .andExpect(status().isBadRequest());
        }

        // boundary size=1 and size=100 succeed
        mockMvc.perform(get(base + sep + "page=0&size=1")
                .header("Authorization", nationwideToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(1));

        mockMvc.perform(get(base + sep + "page=0&size=100")
                .header("Authorization", nationwideToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(100));

        // very large page number safely short-circuits to empty content without overflow
        mockMvc.perform(get(base + sep + "page=2147483647&size=10")
                .header("Authorization", nationwideToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(0));
      }
    }

    @Test
    @DisplayName("Computes tenant-scoped totalElements before pagination without cross-tenant leakage")
    void computesTenantScopedTotalsAccurately() throws Exception {
      // Provider A1 has 1 system event (EVENT_A1_ID), Provider A2 has 1 (EVENT_A2_ID), State A sees 2
      MvcResult provA1 = mockMvc.perform(get("/api/v1/system-event-logs?sourceComponent=SecurityTest&page=0&size=10")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(EVENT_A1_ID))
          .andReturn();

      MvcResult stateA = mockMvc.perform(get("/api/v1/system-event-logs?sourceComponent=SecurityTest&page=0&size=10")
              .header("Authorization", stateAToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(2))
          .andReturn();

      JsonNode stateAContent = objectMapper.readTree(stateA.getResponse().getContentAsString())
          .path("data").path("content");
      assertThat(stateAContent.toString()).doesNotContain("B1");
      assertThat(provA1.getResponse().getContentAsString()).doesNotContain("EvtPwd88");
    }

    @Test
    @DisplayName("Bounds legacy operational collection queries and enforces SQL-level tenant isolation")
    void boundsLegacyOperationalCollectionsInSql() throws Exception {
      // DataQualityIssue queries are bounded and scoped in SQL
      mockMvc.perform(get("/api/v1/data-quality-issues?ruleId={id}", DQ_RULE_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(DQ_ISSUE_A1_ID));

      mockMvc.perform(get("/api/v1/data-quality-issues?entityType=Trainee")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(DQ_ISSUE_A1_ID));

      // DataQualityIssueEvents queries are bounded and scoped in SQL
      mockMvc.perform(get("/api/v1/data-quality-issue-events?statusId=1")
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(DQ_EVENT_A1_ID));

      // AuditLogs queries bounded with LIMIT 100
      mockMvc.perform(get("/api/v1/audit-logs?entityType=TrainingCenter&entityId={id}", CENTER_A1_ID)
              .header("Authorization", providerA1Token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content.length()").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(AUDIT_A1_ID));
    }

    @Test
    @DisplayName("Navigates high-volume operational collections beyond 100 records with deterministic ordering and tenant isolation")
    void verifiesHighVolumeOperationalPaginationAndTenancy() throws Exception {
      try {
        // 1. Seed > 100 matching issues for Provider A1 (105 additional issues: 975001..975105)
        // and 5 issues for Provider B1 (975201..975205)
        for (int i = 1; i <= 105; i++) {
          long issueId = 975000L + i;
          jdbcTemplate.update(
              "INSERT INTO data_quality_issues (data_quality_issue_id, data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at) "
                  + "VALUES (?, ?, 2, 3, 1, 1, 'Trainee', ?, ?, NOW(), NOW(), NOW())",
              issueId, DQ_RULE_ID, TRAINEE_A1_ID, "Provider A1 High-Volume Issue " + i);
        }
        for (int i = 1; i <= 5; i++) {
          long issueId = 975200L + i;
          jdbcTemplate.update(
              "INSERT INTO data_quality_issues (data_quality_issue_id, data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at) "
                  + "VALUES (?, ?, 2, 3, 1, 1, 'Trainee', ?, ?, NOW(), NOW(), NOW())",
              issueId, DQ_RULE_ID, TRAINEE_B1_ID, "Provider B1 Issue " + i);
        }

        // 2. Seed > 100 matching issue events for Provider A1 (105 events: 976001..976105)
        // and 5 events for Provider B1 (976201..976205)
        for (int i = 1; i <= 105; i++) {
          long eventId = 976000L + i;
          jdbcTemplate.update(
              "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, remarks, created_at) "
                  + "VALUES (?, ?, 1, ?, ?, NOW())",
              eventId, DQ_ISSUE_A1_ID, USER_PROVIDER_A1_ID, "Provider A1 Event " + i);
        }
        for (int i = 1; i <= 5; i++) {
          long eventId = 976200L + i;
          jdbcTemplate.update(
              "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, remarks, created_at) "
                  + "VALUES (?, ?, 1, ?, ?, NOW())",
              eventId, DQ_ISSUE_B1_ID, USER_STATE_B_ID, "Provider B1 Event " + i);
        }

        // 3. Verify DataQualityIssue pagination: page 0 returns 50 most recent records
        MvcResult page0Result = mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=0&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(50))
            .andExpect(jsonPath("$.data[0].id").value(975105L))
            .andReturn();

        JsonNode page0Array = objectMapper.readTree(page0Result.getResponse().getContentAsString()).path("data");
        List<Long> page0Ids = new java.util.ArrayList<>();
        for (JsonNode item : page0Array) {
          page0Ids.add(item.path("id").asLong());
        }

        // 4. Verify DataQualityIssue pagination: page 1 returns next 50 records in descending order
        MvcResult page1Result = mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=1&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(50))
            .andExpect(jsonPath("$.data[0].id").value(975055L))
            .andReturn();

        JsonNode page1Array = objectMapper.readTree(page1Result.getResponse().getContentAsString()).path("data");
        List<Long> page1Ids = new java.util.ArrayList<>();
        for (JsonNode item : page1Array) {
          page1Ids.add(item.path("id").asLong());
        }

        // Page 0 and Page 1 return completely disjoint sets of records
        assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
        // Page 0 elements are strictly newer (higher ID) than Page 1 elements
        assertThat(page0Ids.get(page0Ids.size() - 1)).isGreaterThan(page1Ids.get(0));

        // 5. Verify older records remain accessible on subsequent pages (page 2)
        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=2&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(6)) // 5 remaining 975005..975001 + original DQ_ISSUE_A1_ID
            .andExpect(jsonPath("$.data[5].id").value(DQ_ISSUE_A1_ID)); // Original oldest record is accessible!

        // 6. Verify Tenancy: Provider A1 cannot retrieve Provider B1 records on ANY page
        for (int p = 0; p <= 2; p++) {
          MvcResult pageRes = mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=" + p + "&size=50")
                  .header("Authorization", providerA1Token))
              .andExpect(status().isOk())
              .andReturn();
          String content = pageRes.getResponse().getContentAsString();
          assertThat(content)
              .doesNotContain("975201")
              .doesNotContain("975202")
              .doesNotContain("975203")
              .doesNotContain("975204")
              .doesNotContain("975205")
              .doesNotContain(String.valueOf(DQ_ISSUE_B1_ID));
        }

        // Provider B1 querying ruleId sees ONLY their own records
        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=0&size=50")
                .header("Authorization", stateBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(6)) // 5 newly seeded + 1 original DQ_ISSUE_B1_ID
            .andExpect(jsonPath("$.data[0].id").value(975205L));

        // 7. Verify invalid page and size parameters are rejected with 400 Bad Request
        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=-1&size=10")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=0&size=0")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=0&size=101")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues?ruleId=" + DQ_RULE_ID + "&page=0&size=50&bogusParam=true")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        // 8. Verify DataQualityIssueEvent pagination and tenancy
        MvcResult evPage0Result = mockMvc.perform(get("/api/v1/data-quality-issue-events?issueId=" + DQ_ISSUE_A1_ID + "&page=0&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(50))
            .andExpect(jsonPath("$.data[0].id").value(976105L))
            .andReturn();

        MvcResult evPage1Result = mockMvc.perform(get("/api/v1/data-quality-issue-events?issueId=" + DQ_ISSUE_A1_ID + "&page=1&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(50))
            .andExpect(jsonPath("$.data[0].id").value(976055L))
            .andReturn();

        JsonNode evPage0Array = objectMapper.readTree(evPage0Result.getResponse().getContentAsString()).path("data");
        JsonNode evPage1Array = objectMapper.readTree(evPage1Result.getResponse().getContentAsString()).path("data");
        List<Long> evPage0Ids = new java.util.ArrayList<>();
        List<Long> evPage1Ids = new java.util.ArrayList<>();
        for (JsonNode n : evPage0Array) { evPage0Ids.add(n.path("id").asLong()); }
        for (JsonNode n : evPage1Array) { evPage1Ids.add(n.path("id").asLong()); }
        assertThat(evPage0Ids).doesNotContainAnyElementsOf(evPage1Ids);
        assertThat(evPage0Ids.get(evPage0Ids.size() - 1)).isGreaterThan(evPage1Ids.get(0));

        // Older events accessible on page 2
        mockMvc.perform(get("/api/v1/data-quality-issue-events?issueId=" + DQ_ISSUE_A1_ID + "&page=2&size=50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(6)) // 5 remaining 976005..976001 + original DQ_EVENT_A1_ID
            .andExpect(jsonPath("$.data[5].id").value(DQ_EVENT_A1_ID));

        // Provider A1 cannot access Provider B1's issue events
        mockMvc.perform(get("/api/v1/data-quality-issue-events?issueId=" + DQ_ISSUE_B1_ID)
                .header("Authorization", providerA1Token))
            .andExpect(status().isForbidden());

        // Invalid page/size for issue events rejected
        mockMvc.perform(get("/api/v1/data-quality-issue-events?statusId=1&page=-1&size=10")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issue-events?statusId=1&page=0&size=0")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issue-events?statusId=1&page=0&size=101")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

      } finally {
        jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 975000 AND 977000");
        jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_issue_id BETWEEN 975000 AND 977000");
      }
    }

    @Test
    @DisplayName("1.5 Verifies High-Volume Timeline Events Bounded Pagination and Multi-Tenant Isolation")
    void verifiesHighVolumeTimelineEventsBoundedPaginationAndTenancy() throws Exception {
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 977000 AND 978000");

      try {
        // Seed 105 events for DQ_ISSUE_A1_ID with distinct ascending timestamps and IDs
        for (int i = 1; i <= 105; i++) {
          long eventId = 977000L + i;
          jdbcTemplate.update(
              "INSERT INTO data_quality_issue_events (data_quality_issue_event_id, data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, remarks, created_at, changed_at) "
                  + "VALUES (?, ?, 1, ?, ?, NOW() + INTERVAL " + i + " MINUTE, NOW() + INTERVAL " + i + " MINUTE)",
              eventId, DQ_ISSUE_A1_ID, USER_NATIONWIDE_ID, "Timeline event " + i);
        }

        // 1. Verify page 0 (size 50) and page 1 (size 50) are bounded and non-overlapping
        MvcResult page0Result = mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "0")
                .param("size", "50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(50))
            .andReturn();

        MvcResult page1Result = mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "1")
                .param("size", "50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(50))
            .andReturn();

        JsonNode page0Array = objectMapper.readTree(page0Result.getResponse().getContentAsString()).path("data");
        JsonNode page1Array = objectMapper.readTree(page1Result.getResponse().getContentAsString()).path("data");

        List<Long> page0Ids = new ArrayList<>();
        List<Long> page1Ids = new ArrayList<>();
        for (JsonNode n : page0Array) {
          page0Ids.add(n.path("id").asLong());
        }
        for (JsonNode n : page1Array) {
          page1Ids.add(n.path("id").asLong());
        }

        // Non-overlapping verification
        assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);

        // Timeline chronology verification (ascending order)
        assertThat(page0Ids.get(page0Ids.size() - 1)).isLessThan(page1Ids.get(0));

        // 2. Verify older/subsequent events remain accessible on page 2
        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "2")
                .param("size", "50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(6)); // 1 original DQ_EVENT_A1_ID + 105 seeded = 106 total -> page 2 has 6

        // Out-of-range page returns empty list immediately
        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "10")
                .param("size", "50")
                .header("Authorization", providerA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(0));

        // 3. Verify another provider cannot access the issue timeline (HTTP 403 Forbidden)
        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_B1_ID)
                .header("Authorization", providerA1Token))
            .andExpect(status().isForbidden());

        // 4. Verify invalid page and size parameters return HTTP 400 Bad Request
        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "-1")
                .param("size", "10")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "0")
                .param("size", "0")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "0")
                .param("size", "101")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", DQ_ISSUE_A1_ID)
                .param("page", "0")
                .param("size", "50")
                .param("bogusParam", "true")
                .header("Authorization", providerA1Token))
            .andExpect(status().isBadRequest());

      } finally {
        jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_event_id BETWEEN 977000 AND 978000");
      }
    }
  }
}
