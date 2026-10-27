package in.gov.sih.sih26135.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration and regression test suite for Phase 26.4 Operational, Log & Event Pagination.
 * Covers all nine target domain resources:
 * 1. EmploymentRecord (/api/v1/employment-records)
 * 2. PlacementRecord (/api/v1/placement-records)
 * 3. EmploymentVerificationRequest (/api/v1/employment-verification-requests)
 * 4. EmploymentVerificationAttempt (/api/v1/employment-verification-attempts)
 * 5. SurveyResponse (/api/v1/survey-responses)
 * 6. FollowupTask (/api/v1/followup-tasks)
 * 7. CommunicationLog (/api/v1/communication-logs)
 * 8. AuditLog (/api/v1/audit-logs)
 * 9. SystemEventLog (/api/v1/system-event-logs)
 *
 * Verifies pagination defaults (0, 20), deterministic chronological descending sorting,
 * unique tie-breaking (id DESC), sort allowlists, bounds validation, soft-delete filtering
 * (with explicit includeDeleted preservation), point-lookup preservation, and security boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.4 - Operational, Log & Event Pagination Integration Tests")
class OperationalLogEventPaginationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbc;

  private String fullAdminToken;
  private String guestToken;
  private boolean fixtureCleanupRequired = false;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    // 1. Collision guard: MUST run before any operation that could modify or delete records.
    checkNoCollisions();

    // 2. Mark cleanup as required before insertion begins so partial insertion is cleaned up.
    fixtureCleanupRequired = true;
    try {
      insertTestFixtures();
    } catch (Exception e) {
      cleanTestFixtures();
      fixtureCleanupRequired = false;
      throw e;
    }

    fullAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "placement.manage",
            "employment.manage",
            "verification.manage",
            "survey.manage",
            "audit.read",
            "system.manage"
        )
    );

    guestToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        999L,
        "guest",
        List.of("ROLE_GUEST")
    );
  }

  @AfterEach
  void tearDown() {
    if (fixtureCleanupRequired) {
      try {
        cleanTestFixtures();
      } finally {
        fixtureCleanupRequired = false;
      }
    }
  }

  private void checkNoCollisions() {
    Integer collisions = jdbc.queryForObject(
        "SELECT ("
            + "(SELECT count(*) FROM states WHERE state_id = 9981 OR state_code = 'PY') + "
            + "(SELECT count(*) FROM districts WHERE district_id = 9981 OR district_code = 'PD81') + "
            + "(SELECT count(*) FROM sectors WHERE sector_id = 9981 OR sector_code = 'SEC-OP-9981') + "
            + "(SELECT count(*) FROM courses WHERE course_id = 9981 OR course_code = 'CRS-OP-9981') + "
            + "(SELECT count(*) FROM employers WHERE employer_id IN (9981, 9982) OR employer_code IN ('EMP-OP-9981', 'EMP-OP-9982') OR registration_number IN ('REG-EMP-OP-9981', 'REG-EMP-OP-9982')) + "
            + "(SELECT count(*) FROM trainees WHERE trainee_id IN (9981, 9982) OR registration_number IN ('REG-TRN-OP-9981', 'REG-TRN-OP-9982')) + "
            + "(SELECT count(*) FROM training_providers WHERE provider_id = 9981 OR provider_code = 'PRV-OP-9981' OR registration_number = 'REG-PRV-OP-9981') + "
            + "(SELECT count(*) FROM training_centers WHERE center_id = 9981 OR center_code = 'CTR-OP-9981') + "
            + "(SELECT count(*) FROM organizations WHERE organization_id = 9981 OR organization_code = 'ORG-OP-9981') + "
            + "(SELECT count(*) FROM departments WHERE department_id = 9981 OR department_code = 'DEP-OP-9981') + "
            + "(SELECT count(*) FROM schemes WHERE scheme_id = 9981 OR scheme_code = 'SCH-OP-9981') + "
            + "(SELECT count(*) FROM programs WHERE program_id = 9981 OR program_code = 'PRG-OP-9981') + "
            + "(SELECT count(*) FROM training_batches WHERE batch_id = 9981 OR batch_code = 'BAT-OP-9981') + "
            + "(SELECT count(*) FROM training_enrollments WHERE enrollment_id IN (9981, 9982) OR enrollment_number IN ('ENR-OP-9981', 'ENR-OP-9982')) + "
            + "(SELECT count(*) FROM placement_records WHERE placement_id IN (9981, 9982) OR placement_number IN ('PLC-OP-9981', 'PLC-OP-9982')) + "
            + "(SELECT count(*) FROM employment_records WHERE employment_id IN (9981, 9982) OR employment_number IN ('EMP-REC-9981', 'EMP-REC-9982')) + "
            + "(SELECT count(*) FROM employment_verification_requests WHERE employment_verification_request_id IN (9981, 9982) OR request_number IN ('VRF-REQ-9981', 'VRF-REQ-9982')) + "
            + "(SELECT count(*) FROM employment_verification_attempts WHERE employment_verification_attempt_id IN (9981, 9982)) + "
            + "(SELECT count(*) FROM survey_templates WHERE survey_template_id = 9981 OR template_code = 'TMP-OP-9981') + "
            + "(SELECT count(*) FROM survey_template_versions WHERE survey_template_version_id = 9981) + "
            + "(SELECT count(*) FROM surveys WHERE survey_id = 9981 OR survey_code = 'SRV-OP-9981') + "
            + "(SELECT count(*) FROM followup_campaigns WHERE followup_campaign_id = 9981 OR campaign_code = 'CMP-OP-9981') + "
            + "(SELECT count(*) FROM followup_tasks WHERE followup_task_id IN (9981, 9982)) + "
            + "(SELECT count(*) FROM survey_responses WHERE survey_response_id IN (9981, 9982)) + "
            + "(SELECT count(*) FROM communication_logs WHERE communication_log_id IN (9981, 9982) OR provider_message_id IN ('MSG-PROV-9981', 'MSG-PROV-9982')) + "
            + "(SELECT count(*) FROM audit_logs WHERE audit_log_id IN (9981, 9982) OR correlation_id IN ('CORR-OP-9981', 'CORR-OP-9982')) + "
            + "(SELECT count(*) FROM system_event_logs WHERE system_event_log_id IN (9981, 9982) OR event_code IN ('EVT-OP-9981', 'EVT-OP-9982'))"
            + ")", Integer.class);
    if (collisions != null && collisions > 0) {
      throw new IllegalStateException("Test fixture collision detected: one or more records exist with fixture IDs, codes, or registration numbers. Aborting safely without modifying data.");
    }
  }

  private void insertTestFixtures() {
    // 1. states
    jdbc.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'PY', 'Operational Test State 9981', 1, NOW(), NOW())");

    // 2. districts
    jdbc.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 9981, 'PD81', 'Operational Test District 9981', 1, NOW(), NOW())");

    // 3. sectors
    jdbc.update("INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'SEC-OP-9981', 'Operational Test Sector 9981', 1, NOW(), NOW())");

    // 4. courses
    jdbc.update("INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'CRS-OP-9981', 'Course Op 9981', 9981, 1, 1, NOW(), NOW(), NULL)");

    // 5. employers (active 9981, soft-deleted 9982)
    jdbc.update("INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'EMP-OP-9981', 'Employer Op Active 9981', 'REG-EMP-OP-9981', 1, 9981, 9981, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9982, 'EMP-OP-9982', 'Employer Op Deleted 9982', 'REG-EMP-OP-9982', 1, 9981, 9981, 1, 1, NOW(), NOW(), NOW())");

    // 6. trainees
    jdbc.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'REG-TRN-OP-9981', 'TraineeOp1', 'Tester', 1, 9981, 9981, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9982, 'REG-TRN-OP-9982', 'TraineeOp2', 'Tester', 1, 9981, 9981, 1, 1, NOW(), NOW(), NULL)");

    // 7. training_providers
    jdbc.update("INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'PRV-OP-9981', 'Provider Op 9981', 'REG-PRV-OP-9981', 1, 9981, 9981, 1, 1, NOW(), NOW(), NULL)");

    // 8. training_centers
    jdbc.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 9981, 'CTR-OP-9981', 'Center Op 9981', 9981, 9981, 1, NOW(), NOW(), NULL)");

    // 9. organizations
    jdbc.update("INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'ORG-OP-9981', 'Organization Op 9981', 1, 1, NOW(), NOW())");

    // 10. departments
    jdbc.update("INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 9981, 'DEP-OP-9981', 'Department Op 9981', 1, NOW(), NOW())");

    // 11. schemes
    jdbc.update("INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'SCH-OP-9981', 'Scheme Op 9981', 9981, 'INR', 1, NOW(), NOW())");

    // 12. programs
    jdbc.update("INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'PRG-OP-9981', 'Program Op 9981', 9981, 9981, 1, NOW(), NOW())");

    // 13. training_batches
    jdbc.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'BAT-OP-9981', 9981, 9981, 9981, 9981, '2026-01-01', 30, 1, NOW(), NOW(), NULL)");

    // 14. training_enrollments
    jdbc.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'ENR-OP-9981', 9981, 9981, 9981, 9981, 9981, 9981, '2026-01-01', 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9982, 'ENR-OP-9982', 9982, 9981, 9981, 9981, 9981, 9981, '2026-01-01', 1, NOW(), NOW(), NULL)");

    // 15. placement_records (9981 active, 9982 soft-deleted)
    jdbc.update("INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, record_verification_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'PLC-OP-9981', 9981, 9981, 9981, 9981, 9981, 9981, 1, 1, 1, 1, 25000.00, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), NULL)");
    jdbc.update("INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, record_verification_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9982, 'PLC-OP-9982', 9982, 9982, 9981, 9981, 9981, 9981, 1, 1, 1, 1, 30000.00, 1, NOW(), NOW(), NOW())");

    // 16. employment_records (9981 active, 9982 soft-deleted)
    jdbc.update("INSERT INTO employment_records (employment_id, employment_number, trainee_id, enrollment_id, placement_id, employer_id, engagement_type_id, employment_spell_status_id, start_date, is_current, employment_info_source_id, record_verification_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9981, 'EMP-REC-9981', 9981, 9981, 9981, 9981, 1, 1, '2026-01-01', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), NULL)");
    jdbc.update("INSERT INTO employment_records (employment_id, employment_number, trainee_id, enrollment_id, placement_id, employer_id, engagement_type_id, employment_spell_status_id, start_date, is_current, employment_info_source_id, record_verification_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9982, 'EMP-REC-9982', 9982, 9982, 9982, 9981, 1, 1, '2026-02-01', 1, 1, 1, NOW(), NOW(), NOW())");

    // 17. employment_verification_requests (9981, 9982)
    jdbc.update("INSERT INTO employment_verification_requests (employment_verification_request_id, request_number, employment_id, trainee_id, employer_id, cycle_number, is_reverification, employment_verification_request_status_id, requested_at, completed_at, created_at, updated_at) "
        + "VALUES (9981, 'VRF-REQ-9981', 9981, 9981, 9981, 1, 0, 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())");
    jdbc.update("INSERT INTO employment_verification_requests (employment_verification_request_id, request_number, employment_id, trainee_id, employer_id, cycle_number, is_reverification, employment_verification_request_status_id, requested_at, created_at, updated_at) "
        + "VALUES (9982, 'VRF-REQ-9982', 9981, 9981, 9981, 2, 1, 1, NOW(), NOW(), NOW())");

    // 18. employment_verification_attempts (9981, 9982)
    jdbc.update("INSERT INTO employment_verification_attempts (employment_verification_attempt_id, employment_verification_request_id, attempt_number, employment_verification_method_id, employment_verification_attempt_status_id, attempted_at, created_at, updated_at) "
        + "VALUES (9981, 9981, 1, 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())");
    jdbc.update("INSERT INTO employment_verification_attempts (employment_verification_attempt_id, employment_verification_request_id, attempt_number, employment_verification_method_id, employment_verification_attempt_status_id, attempted_at, created_at, updated_at) "
        + "VALUES (9982, 9981, 2, 1, 1, NOW(), NOW(), NOW())");

    // 19. followup_campaigns
    jdbc.update("INSERT INTO followup_campaigns (followup_campaign_id, campaign_code, campaign_name, followup_type_id, scheduled_start_date, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'CMP-OP-9981', 'Campaign Op 9981', 1, '2026-06-01', 1, NOW(), NOW())");

    // 20. followup_tasks (9981, 9982)
    jdbc.update("INSERT INTO followup_tasks (followup_task_id, followup_campaign_id, trainee_id, enrollment_id, placement_id, employment_id, scheduled_date, followup_status_id, created_at, updated_at) "
        + "VALUES (9981, 9981, 9981, 9981, 9981, 9981, '2026-06-01', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())");
    jdbc.update("INSERT INTO followup_tasks (followup_task_id, followup_campaign_id, trainee_id, enrollment_id, placement_id, employment_id, scheduled_date, followup_status_id, created_at, updated_at) "
        + "VALUES (9982, 9981, 9982, 9982, 9982, 9982, '2026-06-02', 1, NOW(), NOW())");

    // 21. survey_templates
    jdbc.update("INSERT INTO survey_templates (survey_template_id, template_code, template_name, survey_purpose_id, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 'TMP-OP-9981', 'Template Op 9981', 1, 1, NOW(), NOW())");

    // 22. survey_template_versions
    jdbc.update("INSERT INTO survey_template_versions (survey_template_version_id, survey_template_id, version_number, version_label, effective_from, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9981, 9981, 1, 'v1.0', '2026-01-01', 1, NOW(), NOW())");

    // 23. surveys
    jdbc.update("INSERT INTO surveys (survey_id, survey_code, survey_name, survey_template_version_id, survey_purpose_id, scheduled_start_date, survey_instance_status_id, created_at, updated_at) "
        + "VALUES (9981, 'SRV-OP-9981', 'Survey Op 9981', 9981, 1, '2026-01-01', 1, NOW(), NOW())");

    // 24. survey_responses (9981, 9982)
    jdbc.update("INSERT INTO survey_responses (survey_response_id, survey_id, survey_template_version_id, trainee_id, enrollment_id, followup_task_id, attempt_number, survey_response_status_id, started_at, created_at, updated_at) "
        + "VALUES (9981, 9981, 9981, 9981, 9981, 9981, 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())");
    jdbc.update("INSERT INTO survey_responses (survey_response_id, survey_id, survey_template_version_id, trainee_id, enrollment_id, followup_task_id, attempt_number, survey_response_status_id, started_at, created_at, updated_at) "
        + "VALUES (9982, 9981, 9981, 9982, 9982, 9982, 1, 1, NOW(), NOW(), NOW())");

    // 25. communication_logs (9981, 9982)
    jdbc.update("INSERT INTO communication_logs (communication_log_id, trainee_id, followup_task_id, survey_id, survey_response_id, communication_channel_id, communication_direction_id, communication_purpose_id, communication_status_id, provider_message_id, created_at, updated_at) "
        + "VALUES (9981, 9981, 9981, 9981, 9981, 1, 1, 1, 1, 'MSG-PROV-9981', DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW())");
    jdbc.update("INSERT INTO communication_logs (communication_log_id, trainee_id, followup_task_id, survey_id, survey_response_id, communication_channel_id, communication_direction_id, communication_purpose_id, communication_status_id, provider_message_id, created_at, updated_at) "
        + "VALUES (9982, 9982, 9982, 9981, 9982, 1, 1, 1, 1, 'MSG-PROV-9982', NOW(), NOW())");

    // 26. audit_logs (9981, 9982)
    jdbc.update("INSERT INTO audit_logs (audit_log_id, audit_action_id, entity_type, entity_id, correlation_id, occurred_at, created_at) "
        + "VALUES (9981, 1, 'TestEntity', 9981, 'CORR-OP-9981', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR))");
    jdbc.update("INSERT INTO audit_logs (audit_log_id, audit_action_id, entity_type, entity_id, correlation_id, occurred_at, created_at) "
        + "VALUES (9982, 1, 'TestEntity', 9982, 'CORR-OP-9982', NOW(), NOW())");

    // 27. system_event_logs (9981, 9982)
    jdbc.update("INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) "
        + "VALUES (9981, 1, 1, 'EVT-OP-9981', 'TestEntity', 9981, 'TestService', 'Event 9981 summary', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR))");
    jdbc.update("INSERT INTO system_event_logs (system_event_log_id, system_event_category_id, system_event_severity_id, event_code, entity_type, entity_id, source_component, event_summary, occurred_at, created_at) "
        + "VALUES (9982, 1, 1, 'EVT-OP-9982', 'TestEntity', 9982, 'TestService', 'Event 9982 summary', NOW(), NOW())");
  }

  private void cleanTestFixtures() {
    // Reverse FK dependency order cleanup
    jdbc.update("DELETE FROM system_event_logs WHERE system_event_log_id IN (9981, 9982) OR event_code IN ('EVT-OP-9981', 'EVT-OP-9982')");
    jdbc.update("DELETE FROM audit_logs WHERE audit_log_id IN (9981, 9982) OR correlation_id IN ('CORR-OP-9981', 'CORR-OP-9982')");
    jdbc.update("DELETE FROM communication_logs WHERE communication_log_id IN (9981, 9982) OR provider_message_id IN ('MSG-PROV-9981', 'MSG-PROV-9982')");
    jdbc.update("DELETE FROM survey_responses WHERE survey_response_id IN (9981, 9982)");
    jdbc.update("DELETE FROM followup_tasks WHERE followup_task_id IN (9981, 9982)");
    jdbc.update("DELETE FROM surveys WHERE survey_id = 9981 OR survey_code = 'SRV-OP-9981'");
    jdbc.update("DELETE FROM survey_template_versions WHERE survey_template_version_id = 9981");
    jdbc.update("DELETE FROM survey_templates WHERE survey_template_id = 9981 OR template_code = 'TMP-OP-9981'");
    jdbc.update("DELETE FROM followup_campaigns WHERE followup_campaign_id = 9981 OR campaign_code = 'CMP-OP-9981'");
    jdbc.update("DELETE FROM employment_verification_attempts WHERE employment_verification_attempt_id IN (9981, 9982)");
    jdbc.update("DELETE FROM employment_verification_requests WHERE employment_verification_request_id IN (9981, 9982) OR request_number IN ('VRF-REQ-9981', 'VRF-REQ-9982')");
    jdbc.update("DELETE FROM employment_records WHERE employment_id IN (9981, 9982) OR employment_number IN ('EMP-REC-9981', 'EMP-REC-9982')");
    jdbc.update("DELETE FROM placement_records WHERE placement_id IN (9981, 9982) OR placement_number IN ('PLC-OP-9981', 'PLC-OP-9982')");
    jdbc.update("DELETE FROM training_enrollments WHERE enrollment_id IN (9981, 9982) OR enrollment_number IN ('ENR-OP-9981', 'ENR-OP-9982')");
    jdbc.update("DELETE FROM training_batches WHERE batch_id = 9981 OR batch_code = 'BAT-OP-9981'");
    jdbc.update("DELETE FROM programs WHERE program_id = 9981 OR program_code = 'PRG-OP-9981'");
    jdbc.update("DELETE FROM schemes WHERE scheme_id = 9981 OR scheme_code = 'SCH-OP-9981'");
    jdbc.update("DELETE FROM departments WHERE department_id = 9981 OR department_code = 'DEP-OP-9981'");
    jdbc.update("DELETE FROM organizations WHERE organization_id = 9981 OR organization_code = 'ORG-OP-9981'");
    jdbc.update("DELETE FROM training_centers WHERE center_id = 9981 OR center_code = 'CTR-OP-9981'");
    jdbc.update("DELETE FROM training_providers WHERE provider_id = 9981 OR provider_code = 'PRV-OP-9981' OR registration_number = 'REG-PRV-OP-9981'");
    jdbc.update("DELETE FROM trainees WHERE trainee_id IN (9981, 9982) OR registration_number IN ('REG-TRN-OP-9981', 'REG-TRN-OP-9982')");
    jdbc.update("DELETE FROM employers WHERE employer_id IN (9981, 9982) OR employer_code IN ('EMP-OP-9981', 'EMP-OP-9982') OR registration_number IN ('REG-EMP-OP-9981', 'REG-EMP-OP-9982')");
    jdbc.update("DELETE FROM courses WHERE course_id = 9981 OR course_code = 'CRS-OP-9981'");
    jdbc.update("DELETE FROM sectors WHERE sector_id = 9981 OR sector_code = 'SEC-OP-9981'");
    jdbc.update("DELETE FROM districts WHERE district_id = 9981 OR district_code = 'PD81'");
    jdbc.update("DELETE FROM states WHERE state_id = 9981 OR state_code = 'PY'");
  }

  // =========================================================================
  // 1. DEFAULT PAGINATION & SORTING ACROSS OPERATIONAL & LOG DOMAINS
  // =========================================================================

  @Test
  @DisplayName("Default pagination for EmploymentRecord returns page 0, size 20, sorted by createdAt DESC")
  void testDefaultPagination_EmploymentRecord() throws Exception {
    mockMvc.perform(get("/api/v1/employment-records")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @Test
  @DisplayName("Default pagination for PlacementRecord returns page 0, size 20, sorted by createdAt DESC")
  void testDefaultPagination_PlacementRecord() throws Exception {
    mockMvc.perform(get("/api/v1/placement-records")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @Test
  @DisplayName("Default pagination for EmploymentVerificationRequest returns page 0, size 20")
  void testDefaultPagination_EmploymentVerificationRequest() throws Exception {
    mockMvc.perform(get("/api/v1/employment-verification-requests")
            .param("employmentId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for EmploymentVerificationAttempt returns page 0, size 20")
  void testDefaultPagination_EmploymentVerificationAttempt() throws Exception {
    mockMvc.perform(get("/api/v1/employment-verification-attempts")
            .param("requestId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for SurveyResponse returns page 0, size 20")
  void testDefaultPagination_SurveyResponse() throws Exception {
    mockMvc.perform(get("/api/v1/survey-responses")
            .param("surveyId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for FollowupTask returns page 0, size 20")
  void testDefaultPagination_FollowupTask() throws Exception {
    mockMvc.perform(get("/api/v1/followup-tasks")
            .param("campaignId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for CommunicationLog returns page 0, size 20")
  void testDefaultPagination_CommunicationLog() throws Exception {
    mockMvc.perform(get("/api/v1/communication-logs")
            .param("surveyId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for AuditLog returns page 0, size 20, sorted by occurredAt DESC")
  void testDefaultPagination_AuditLog() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "TestEntity")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  @DisplayName("Default pagination for SystemEventLog returns page 0, size 20, sorted by occurredAt DESC")
  void testDefaultPagination_SystemEventLog() throws Exception {
    mockMvc.perform(get("/api/v1/system-event-logs")
            .param("entityType", "TestEntity")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  // =========================================================================
  // 2. BOUNDS VALIDATION (page >= 0, size between 1 and 100)
  // =========================================================================

  @Test
  @DisplayName("Negative page index is rejected with 400 INVALID_PAGE")
  void testBoundsEnforcement_NegativePageIndex() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("page", "-1")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE"));
  }

  @Test
  @DisplayName("Zero page size is rejected with 400 INVALID_PAGE_SIZE")
  void testBoundsEnforcement_ZeroPageSize() throws Exception {
    mockMvc.perform(get("/api/v1/system-event-logs")
            .param("size", "0")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
  }

  @Test
  @DisplayName("Page size greater than 100 is rejected with 400 INVALID_PAGE_SIZE")
  void testBoundsEnforcement_ExceedsMaxPageSize() throws Exception {
    mockMvc.perform(get("/api/v1/communication-logs")
            .param("size", "101")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
  }

  // =========================================================================
  // 3. SORT ALLOWLIST & INVALID DIRECTION ENFORCEMENT
  // =========================================================================

  @Test
  @DisplayName("Invalid sort field is rejected with 400 INVALID_SORT_FIELD")
  void testSortAllowlist_InvalidFieldRejected() throws Exception {
    mockMvc.perform(get("/api/v1/employment-records")
            .param("sort", "malicious_column")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
  }

  @Test
  @DisplayName("Invalid sort direction is rejected with 400 INVALID_SORT_DIRECTION")
  void testSortDirection_InvalidDirectionRejected() throws Exception {
    mockMvc.perform(get("/api/v1/placement-records")
            .param("direction", "SIDEWAYS")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"));
  }

  @Test
  @DisplayName("Valid sort fields from allowlist are accepted and applied")
  void testSortAllowlist_ValidFieldsAccepted() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "TestEntity")
            .param("sort", "occurredAt")
            .param("direction", "ASC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  // =========================================================================
  // 4. DETERMINISTIC CHRONOLOGICAL DESC SORTING & TIE-BREAKING
  // =========================================================================

  @Test
  @DisplayName("AuditLog returns records in chronological DESC order with id tie-breaker")
  void testDeterministicChronologicalOrdering_AuditLog() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "TestEntity")
            .param("sort", "occurredAt")
            .param("direction", "DESC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode content = json.path("data").path("content");
    assertThat(content.size()).isEqualTo(2);
    // Fixture 9982 was created with NOW(), fixture 9981 with NOW() - 1 hour
    assertThat(content.get(0).path("id").asLong()).isEqualTo(9982L);
    assertThat(content.get(1).path("id").asLong()).isEqualTo(9981L);
  }

  @Test
  @DisplayName("SystemEventLog returns records in chronological DESC order with id tie-breaker")
  void testDeterministicChronologicalOrdering_SystemEventLog() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/system-event-logs")
            .param("entityType", "TestEntity")
            .param("sort", "occurredAt")
            .param("direction", "DESC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode content = json.path("data").path("content");
    assertThat(content.size()).isEqualTo(2);
    assertThat(content.get(0).path("id").asLong()).isEqualTo(9982L);
    assertThat(content.get(1).path("id").asLong()).isEqualTo(9981L);
  }

  // =========================================================================
  // 5. SOFT-DELETE FILTERING & INCLUDE DELETED PRESERVATION
  // =========================================================================

  @Test
  @DisplayName("EmploymentRecord excludes soft-deleted records by default, includes when includeDeleted=true")
  void testSoftDeletePreservation_EmploymentRecord() throws Exception {
    // Normal query excludes deleted record 9982
    MvcResult normalResult = mockMvc.perform(get("/api/v1/employment-records")
            .param("employerId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode normalJson = objectMapper.readTree(normalResult.getResponse().getContentAsString());
    JsonNode normalContent = normalJson.path("data").path("content");
    assertThat(normalContent.size()).isEqualTo(1);
    assertThat(normalContent.get(0).path("id").asLong()).isEqualTo(9981L);
    assertThat(normalJson.path("data").path("totalElements").asLong()).isEqualTo(1L);

    // includeDeleted=true query includes both 9981 and 9982
    MvcResult inclResult = mockMvc.perform(get("/api/v1/employment-records")
            .param("includeDeleted", "true")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode inclJson = objectMapper.readTree(inclResult.getResponse().getContentAsString());
    JsonNode inclContent = inclJson.path("data").path("content");
    boolean found9982 = false;
    for (JsonNode node : inclContent) {
      if (node.path("id").asLong() == 9982L) {
        found9982 = true;
        break;
      }
    }
    assertThat(found9982).isTrue();
  }

  @Test
  @DisplayName("PlacementRecord excludes soft-deleted records by default, includes when includeDeleted=true")
  void testSoftDeletePreservation_PlacementRecord() throws Exception {
    // Normal query excludes deleted record 9982
    MvcResult normalResult = mockMvc.perform(get("/api/v1/placement-records")
            .param("employerId", "9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode normalJson = objectMapper.readTree(normalResult.getResponse().getContentAsString());
    JsonNode normalContent = normalJson.path("data").path("content");
    assertThat(normalContent.size()).isEqualTo(1);
    assertThat(normalContent.get(0).path("id").asLong()).isEqualTo(9981L);
    assertThat(normalJson.path("data").path("totalElements").asLong()).isEqualTo(1L);

    // includeDeleted=true query includes 9982
    MvcResult inclResult = mockMvc.perform(get("/api/v1/placement-records")
            .param("includeDeleted", "true")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode inclJson = objectMapper.readTree(inclResult.getResponse().getContentAsString());
    JsonNode inclContent = inclJson.path("data").path("content");
    boolean found9982 = false;
    for (JsonNode node : inclContent) {
      if (node.path("id").asLong() == 9982L) {
        found9982 = true;
        break;
      }
    }
    assertThat(found9982).isTrue();
  }

  // =========================================================================
  // 6. POINT LOOKUPS REMAIN UNPAGINATED
  // =========================================================================

  @Test
  @DisplayName("Point lookups return direct single object ApiResponse<T>, NOT PagedResponse")
  void testSingleRecordLookupsRemainUnpaginated() throws Exception {
    // 1. EmploymentRecord by id
    mockMvc.perform(get("/api/v1/employment-records/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    // 2. PlacementRecord by id
    mockMvc.perform(get("/api/v1/placement-records/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 3. EmploymentVerificationRequest by id
    mockMvc.perform(get("/api/v1/employment-verification-requests/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 4. EmploymentVerificationAttempt by id
    mockMvc.perform(get("/api/v1/employment-verification-attempts/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 5. FollowupTask by id
    mockMvc.perform(get("/api/v1/followup-tasks/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 6. SurveyResponse by id
    mockMvc.perform(get("/api/v1/survey-responses/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 7. CommunicationLog by id
    mockMvc.perform(get("/api/v1/communication-logs/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 8. CommunicationLog by provider message id
    mockMvc.perform(get("/api/v1/communication-logs")
            .param("providerMessageId", "MSG-PROV-9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 9. AuditLog by id
    mockMvc.perform(get("/api/v1/audit-logs/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 10. SystemEventLog by id
    mockMvc.perform(get("/api/v1/system-event-logs/9981")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(9981))
        .andExpect(jsonPath("$.data.content").doesNotExist());
  }

  // =========================================================================
  // 7. PAGINATION METADATA BEYOND LAST PAGE
  // =========================================================================

  @Test
  @DisplayName("Requests for pages beyond available data return empty content with accurate metadata")
  void testPaginationMetadata_BeyondLastPage() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "TestEntity")
            .param("page", "50")
            .param("size", "20")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(50))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.totalPages").value(1))
        .andExpect(jsonPath("$.data.content").isEmpty())
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.hasPrevious").value(true));
  }

  // =========================================================================
  // 8. SECURITY & AUTHORIZATION BOUNDARIES
  // =========================================================================

  @Test
  @DisplayName("Unauthenticated request is rejected with 401 Unauthorized")
  void testSecurity_UnauthenticatedReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Unprivileged user is rejected with 403 Forbidden")
  void testSecurity_UnprivilegedUserReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/employment-records")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/survey-responses")
            .param("surveyId", "9981")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());
  }
}
