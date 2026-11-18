package in.gov.sih.sih26135.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.DispatchNotificationRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Phase 29.5 — End-to-End Employer Placement & Verification Journey Integration Test.
 *
 * <p>Exercises the complete employer and placement lifecycle end-to-end against real HTTP endpoints:
 * <ol>
 *   <li>Setup: Employer A and Employer B with dedicated single-root user accounts and authority profiles</li>
 *   <li>Job Posting Creation via {@code POST /api/v1/job-postings} by Employer A</li>
 *   <li>Candidate Placement Creation via {@code POST /api/v1/placement-records}</li>
 *   <li>Placement to Employment Transition via {@code POST /api/v1/workflows/placement-employment/transition-placement-to-employment}</li>
 *   <li>DEMO Notification Verification via {@code POST /api/v1/communication-logs/dispatch} (SIMULATED, deliveryMode=DEMO, 0 external calls, recipient masked)</li>
 *   <li>Employer Verification Verdict via {@code POST /api/v1/workflows/verification/record-verdict}</li>
 *   <li>Six-Month Wage / Salary Follow-up via {@code POST /api/v1/salary-history} (milestone offset 6)</li>
 *   <li>Exit and Unemployment Lifecycle via {@code POST /api/v1/workflows/employment-lifecycle/record-exit-and-initiate-unemployment}</li>
 *   <li>Cross-Tenant Isolation: Employer B cannot mutate or inspect Employer A's placement, verification, or exit workflows (403 Forbidden)</li>
 * </ol>
 */
@DisplayName("Phase 29.5 — Employer Placement & Verification Journey Integration Test")
public class EmployerPlacementJourneyIntegrationTest extends BaseJourneyIntegrationTest {

  private static final Long FIXTURE_ADMIN_USER_ID = 9300L;
  private static final Long FIXTURE_EMPLOYER_A_ID = 9301L;
  private static final Long FIXTURE_EMPLOYER_B_ID = 9302L;
  private static final Long FIXTURE_EMP_A_USER_ID = 9301L;
  private static final Long FIXTURE_EMP_B_USER_ID = 9302L;

  private static final Long FIXTURE_SECTOR_ID = 9301L;
  private static final Long FIXTURE_JOB_ROLE_ID = 9301L;
  private static final Long FIXTURE_COURSE_ID = 9301L;
  private static final Long FIXTURE_ORG_ID = 9301L;
  private static final Long FIXTURE_DEPT_ID = 9301L;
  private static final Long FIXTURE_SCHEME_ID = 9301L;
  private static final Long FIXTURE_PROGRAM_ID = 9301L;
  private static final Long FIXTURE_PROVIDER_ID = 9301L;
  private static final Long FIXTURE_CENTER_ID = 9301L;
  private static final Long FIXTURE_BATCH_ID = 9301L;
  private static final Long FIXTURE_TRAINEE_ID = 9301L;
  private static final Long FIXTURE_CONSENT_ID = 9301L;
  private static final Long FIXTURE_ENROLLMENT_ID = 9301L;

  private String adminToken;
  private HttpHeaders adminHeaders;
  private String employerAToken;
  private HttpHeaders employerAHeaders;
  private String employerBToken;
  private HttpHeaders employerBHeaders;

  @BeforeEach
  void setUpEmployerPlacementFixtures() {
    // 0. Ensure clean rate limit window for test fixtures
    jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ? OR limiter_key = ?",
        "TRAINEE:%:" + FIXTURE_TRAINEE_ID, "DEDUP:" + FIXTURE_TRAINEE_ID + ":%", "PROVIDER:" + FIXTURE_PROVIDER_ID);

    // 1. Provision Admin User
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'superadmin_emp', 'superadmin_emp@test.gov.in', 'hash', 'argon2id', 2, NULL, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, updated_at = NOW()
        """, FIXTURE_ADMIN_USER_ID);
    trackedUserIds.add(FIXTURE_ADMIN_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_ADMIN_USER_ID);

    adminToken = generateBearerToken(
        FIXTURE_ADMIN_USER_ID,
        "superadmin_emp",
        List.of(
            "ROLE_super_admin",
            "system.manage",
            "placement.manage",
            "employment.manage",
            "verification.manage",
            "trainee.write",
            "trainee.read",
            "consent.manage",
            "audit.read"
        )
    );
    adminHeaders = createAuthHeaders(adminToken);

    // 2. Training & Domain Fixtures
    jdbcTemplate.update("""
        INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SEC-EMP-295', 'Information Technology', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_SECTOR_ID);
    trackedSectorIds.add(FIXTURE_SECTOR_ID);

    jdbcTemplate.update("""
        INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'CRS-EMP-295', 'Cloud Solutions Architecture', ?, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_COURSE_ID, FIXTURE_SECTOR_ID);
    trackedCourseIds.add(FIXTURE_COURSE_ID);

    jdbcTemplate.update("""
        INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'ORG-EMP-295', 'Tech Placement Council', 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_ORG_ID);
    trackedOrgIds.add(FIXTURE_ORG_ID);

    jdbcTemplate.update("""
        INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DEP-EMP-295', 'Enterprise Placement Wing', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_DEPT_ID, FIXTURE_ORG_ID);
    trackedDeptIds.add(FIXTURE_DEPT_ID);

    jdbcTemplate.update("""
        INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SCH-EMP-295', 'National Tech Placement Scheme', ?, 'INR', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_SCHEME_ID, FIXTURE_DEPT_ID);
    trackedSchemeIds.add(FIXTURE_SCHEME_ID);

    jdbcTemplate.update("""
        INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRG-EMP-295', 'Enterprise Tech Talent Program', ?, ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_PROGRAM_ID, FIXTURE_DEPT_ID, FIXTURE_SCHEME_ID);
    trackedProgramIds.add(FIXTURE_PROGRAM_ID);

    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-EMP-295', 'Apex Tech Academy', 'REG-PRV-EMP-295', 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_PROVIDER_ID, FIXTURE_ORG_ID);
    trackedProviderIds.add(FIXTURE_PROVIDER_ID);

    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'CTR-EMP-295', 'Connaught Tech Center', 101, 1001, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_CENTER_ID, FIXTURE_PROVIDER_ID);
    trackedCenterIds.add(FIXTURE_CENTER_ID);

    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, 'BAT-EMP-295', ?, ?, ?, ?, '2026-01-01', 60, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_BATCH_ID, FIXTURE_COURSE_ID, FIXTURE_PROVIDER_ID, FIXTURE_CENTER_ID, FIXTURE_PROGRAM_ID);
    trackedBatchIds.add(FIXTURE_BATCH_ID);

    // 3. Job Role Fixture
    jdbcTemplate.update("""
        INSERT INTO job_roles (job_role_id, job_role_code, job_role_name, sector_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'JR-EMP-295', 'Senior Systems Architect', ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_JOB_ROLE_ID, FIXTURE_SECTOR_ID);
    trackedJobRoleIds.add(FIXTURE_JOB_ROLE_ID);

    // 4. Employers A & B
    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, organization_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-295-A', 'TechCorp Placement Sol', 'REG-EMP-295-A', 6, ?, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE organization_id = VALUES(organization_id), updated_at = NOW()
        """, FIXTURE_EMPLOYER_A_ID, FIXTURE_ORG_ID);
    trackedEmployerIds.add(FIXTURE_EMPLOYER_A_ID);

    jdbcTemplate.update("""
        INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, organization_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'EMP-295-B', 'InnovateX Solutions', 'REG-EMP-295-B', 6, ?, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE organization_id = VALUES(organization_id), updated_at = NOW()
        """, FIXTURE_EMPLOYER_B_ID, FIXTURE_ORG_ID);
    trackedEmployerIds.add(FIXTURE_EMPLOYER_B_ID);

    // 5. Employer Users (Single-root multi-tenancy: state_id & district_id must be NULL)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'emp_user_a', 'emp_a_295@techcorp.com', 'hash', 'argon2id', 2, ?, NULL, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, updated_at = NOW()
        """, FIXTURE_EMP_A_USER_ID, FIXTURE_EMPLOYER_A_ID);
    trackedUserIds.add(FIXTURE_EMP_A_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'employer' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_EMP_A_USER_ID);

    employerAToken = generateBearerToken(
        FIXTURE_EMP_A_USER_ID,
        "emp_user_a",
        List.of("ROLE_employer", "placement.manage", "employment.manage", "verification.manage")
    );
    employerAHeaders = createAuthHeaders(employerAToken);

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'emp_user_b', 'emp_b_295@innovatex.com', 'hash', 'argon2id', 2, ?, NULL, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, updated_at = NOW()
        """, FIXTURE_EMP_B_USER_ID, FIXTURE_EMPLOYER_B_ID);
    trackedUserIds.add(FIXTURE_EMP_B_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'employer' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_EMP_B_USER_ID);

    employerBToken = generateBearerToken(
        FIXTURE_EMP_B_USER_ID,
        "emp_user_b",
        List.of("ROLE_employer", "placement.manage", "employment.manage", "verification.manage")
    );
    employerBHeaders = createAuthHeaders(employerBToken);

    // 6. Trainee Fixture with Masked Phone (+XXXXXXXX0957)
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, registration_number, first_name, phone, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'REG-EMP-295-01', 'Vikram', '+919876540957', 1, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TRAINEE_ID);
    trackedTraineeIds.add(FIXTURE_TRAINEE_ID);

    // Statutory DPDP Consent: DATA_PROCESSING & COMMUNICATION_SMS
    jdbcTemplate.update("""
        INSERT INTO trainee_consents (trainee_consent_id, trainee_id, consent_type_id, consent_status_id, purpose, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
        VALUES (?, ?, 1, 1, 'Statutory Data Processing Consent', 'v1.0', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 1 YEAR, 'PORTAL', NOW(), NOW())
        ON DUPLICATE KEY UPDATE consent_type_id = VALUES(consent_type_id), consent_status_id = 1, updated_at = NOW()
        """, FIXTURE_CONSENT_ID, FIXTURE_TRAINEE_ID);
    trackedConsentIds.add(FIXTURE_CONSENT_ID);

    jdbcTemplate.update("""
        INSERT INTO trainee_consents (trainee_consent_id, trainee_id, consent_type_id, consent_status_id, purpose, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
        VALUES (?, ?, 5, 1, 'Statutory SMS Notification Consent', 'v1.0', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 1 YEAR, 'PORTAL', NOW(), NOW())
        ON DUPLICATE KEY UPDATE consent_type_id = VALUES(consent_type_id), consent_status_id = 1, updated_at = NOW()
        """, FIXTURE_CONSENT_ID + 1, FIXTURE_TRAINEE_ID);
    trackedConsentIds.add(FIXTURE_CONSENT_ID + 1);

    // Completed Enrollment
    jdbcTemplate.update("""
        INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, batch_id, course_id, provider_id, center_id, enrollment_status_id, enrollment_date, actual_completion_date, created_at, updated_at)
        VALUES (?, 'ENR-EMP-295-01', ?, ?, ?, ?, ?, ?, 3, '2026-01-01', '2026-03-01', NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_ENROLLMENT_ID, FIXTURE_TRAINEE_ID, FIXTURE_PROGRAM_ID, FIXTURE_BATCH_ID, FIXTURE_COURSE_ID, FIXTURE_PROVIDER_ID, FIXTURE_CENTER_ID);
    trackedEnrollmentIds.add(FIXTURE_ENROLLMENT_ID);
  }

  @Test
  @DisplayName("Journey 3: Complete Employer Placement, DEMO Notification, Verification, Salary, and Exit Lifecycle")
  void testCompleteEmployerPlacementLifecycleEndToEnd() throws Exception {
    long timestamp = System.currentTimeMillis();

    // -------------------------------------------------------------------------
    // Step 1: Job Posting Creation via POST /api/v1/job-postings by Employer A
    // -------------------------------------------------------------------------
    String postingCode = "JOB-EMP-295-" + timestamp;
    CreateJobPostingRequest jobRequest = new CreateJobPostingRequest(
        postingCode,
        "Senior Cloud Engineer",
        FIXTURE_JOB_ROLE_ID,
        1L, // Full-time wage employment
        BASE_STATE_ID,
        BASE_DISTRICT_ID,
        LocalDate.now(),
        1L // Open / Active status
    );
    jobRequest.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    jobRequest.setMinSalary(new BigDecimal("35000.00"));
    jobRequest.setMaxSalary(new BigDecimal("50000.00"));
    jobRequest.setVacancies(5);

    ResponseEntity<String> jobResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/job-postings"),
        HttpMethod.POST,
        new HttpEntity<>(jobRequest, employerAHeaders),
        String.class
    );
    assertThat(jobResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode jobJson = parseResponseJson(jobResponse.getBody());
    assertThat(jobJson.get("success").asBoolean()).isTrue();
    Long jobPostingId = jobJson.get("data").get("id").asLong();
    trackedJobPostingIds.add(jobPostingId);
    assertThat(jobJson.get("data").get("postingCode").asText()).isEqualTo(postingCode);
    assertThat(jobJson.get("data").get("employerId").asLong()).isEqualTo(FIXTURE_EMPLOYER_A_ID);

    // -------------------------------------------------------------------------
    // Step 2: Placement Creation via POST /api/v1/placement-records
    // -------------------------------------------------------------------------
    String placementNumber = "PLC-EMP-295-" + timestamp;
    CreatePlacementRecordRequest placementReq = new CreatePlacementRecordRequest(
        placementNumber,
        FIXTURE_ENROLLMENT_ID,
        1L, // WAGE_EMPLOYMENT
        1L, // CAMPUS_DRIVE
        2L, // OFFERED
        2L, // PENDING
        1L  // PENDING_VERIFICATION
    );
    placementReq.setTraineeId(FIXTURE_TRAINEE_ID);
    placementReq.setCourseId(FIXTURE_COURSE_ID);
    placementReq.setProgramId(FIXTURE_PROGRAM_ID);
    placementReq.setProviderId(FIXTURE_PROVIDER_ID);
    placementReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    placementReq.setJobRoleId(FIXTURE_JOB_ROLE_ID);
    placementReq.setJobPostingId(jobPostingId);
    placementReq.setOfferedSalary(new BigDecimal("42000.00"));

    ResponseEntity<String> placementResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/placement-records"),
        HttpMethod.POST,
        new HttpEntity<>(placementReq, adminHeaders),
        String.class
    );
    assertThat(placementResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode placementJson = parseResponseJson(placementResponse.getBody());
    Long placementId = placementJson.get("data").get("id").asLong();
    trackedPlacementIds.add(placementId);

    // -------------------------------------------------------------------------
    // Step 3: Transition Placement to Employment via Workflow API
    // -------------------------------------------------------------------------
    String employmentNumber = "EMP-REC-295-" + timestamp;
    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber(employmentNumber);
    empReq.setTraineeId(FIXTURE_TRAINEE_ID);
    empReq.setEnrollmentId(FIXTURE_ENROLLMENT_ID);
    empReq.setPlacementId(placementId);
    empReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    empReq.setJobRoleId(FIXTURE_JOB_ROLE_ID);
    empReq.setEngagementTypeId(1L);
    empReq.setEmploymentSpellStatusId(1L); // ACTIVE
    empReq.setEmploymentInfoSourceId(1L);  // PLACEMENT_CONVERSION
    empReq.setRecordVerificationStatusId(1L); // UNVERIFIED
    empReq.setStartDate(LocalDate.now());
    empReq.setIsCurrent(true);
    empReq.setStartingSalary(new BigDecimal("42000.00"));
    empReq.setSalaryFrequencyId(1L);
    empReq.setCurrencyCode("INR");

    CreateSalaryHistoryRequest initialSalary = new CreateSalaryHistoryRequest(
        null,
        new BigDecimal("42000.00"),
        1L,
        LocalDate.now(),
        1L,
        1L
    );
    initialSalary.setTraineeId(FIXTURE_TRAINEE_ID);
    initialSalary.setObservationMonthOffset(0);

    PlacementToEmploymentTransitionRequest transitionReq = new PlacementToEmploymentTransitionRequest(
        placementId,
        empReq,
        initialSalary,
        3L, // JOINED (ref_joining_status)
        4L  // JOINED (ref_placement_status)
    );

    ResponseEntity<String> transitionResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/placement-employment/transition-placement-to-employment"),
        HttpMethod.POST,
        new HttpEntity<>(transitionReq, employerAHeaders),
        String.class
    );
    assertThat(transitionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode transitionJson = parseResponseJson(transitionResponse.getBody());
    Long employmentId = transitionJson.get("data").get("employmentRecord").get("id").asLong();
    trackedEmploymentIds.add(employmentId);

    if (transitionJson.get("data").has("initialSalary") && !transitionJson.get("data").get("initialSalary").isNull()) {
      Long initialSalaryId = transitionJson.get("data").get("initialSalary").get("id").asLong();
      trackedSalaryHistoryIds.add(initialSalaryId);
    } else if (transitionJson.get("data").has("initialSalaryHistory") && !transitionJson.get("data").get("initialSalaryHistory").isNull()) {
      Long initialSalaryId = transitionJson.get("data").get("initialSalaryHistory").get("id").asLong();
      trackedSalaryHistoryIds.add(initialSalaryId);
    }

    // -------------------------------------------------------------------------
    // Step 4: DEMO Notification Verification via POST /api/v1/communication-logs/dispatch
    // Enforces: SIMULATED status, deliveryMode=DEMO, 0 external calls, recipient masked (+XXXXXXXX0957)
    // -------------------------------------------------------------------------
    DispatchNotificationRequest dispatchReq = new DispatchNotificationRequest(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-VERIF-003",
        Map.of(
            "traineeName", "Vikram",
            "employerName", "TechCorp Placement Sol",
            "verificationId", "VERIF-EMP-295-01",
            "verificationUrl", "https://skillindia.gov.in/verify"
        ),
        false,
        "IDEMP-EMP-295-" + timestamp
    );

    ResponseEntity<String> dispatchResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/communication-logs/dispatch"),
        HttpMethod.POST,
        new HttpEntity<>(dispatchReq, adminHeaders),
        String.class
    );
    assertThat(dispatchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode dispatchJson = parseResponseJson(dispatchResponse.getBody());
    assertThat(dispatchJson.get("success").asBoolean()).isTrue();
    JsonNode dispatchData = dispatchJson.get("data");

    assertThat(dispatchData.get("deliveryMode").asText()).isEqualTo("DEMO");
    assertThat(dispatchData.get("status").asText()).isEqualTo("SIMULATED");
    assertThat(dispatchData.get("simulated").asBoolean()).isTrue();
    assertThat(dispatchData.get("physicallyDelivered").asBoolean()).isFalse();
    assertThat(dispatchData.get("recipientMasked").asText()).isEqualTo("+XXXXXXXX0957");

    Long commLogId = dispatchData.get("communicationLogId").asLong();
    trackedCommunicationLogIds.add(commLogId);

    // Direct Database State Inspection of Communication Log
    Map<String, Object> logDbRow = jdbcTemplate.queryForMap(
        "SELECT communication_log_id, communication_status_id, message_template_code, sent_at, delivered_at, failed_at, failure_reason FROM communication_logs WHERE communication_log_id = ?",
        commLogId
    );
    assertThat(((Number) logDbRow.get("communication_status_id")).longValue()).isEqualTo(9L); // SIMULATED
    assertThat((String) logDbRow.get("message_template_code")).isEqualTo("TPL-VERIF-003");
    assertThat(logDbRow.get("sent_at")).isNull();
    assertThat(logDbRow.get("delivered_at")).isNull();
    assertThat(logDbRow.get("failed_at")).isNull();

    // -------------------------------------------------------------------------
    // Step 5: Employer Verification Verdict via POST /api/v1/workflows/verification/record-verdict
    // -------------------------------------------------------------------------
    String verificationNumber = "VERIF-EMP-295-" + timestamp;
    CreateEmploymentVerificationRequest verifReq = new CreateEmploymentVerificationRequest(
        verificationNumber,
        employmentId,
        1L, // VERIFIED
        1L, // DIRECT_EMPLOYER
        LocalDateTime.now().minusMinutes(1)
    );
    verifReq.setTraineeId(FIXTURE_TRAINEE_ID);
    verifReq.setPlacementRecordId(placementId);
    verifReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    verifReq.setEmployerConfirmedFlag(true);
    verifReq.setTraineeAttestedFlag(true);
    verifReq.setIsCurrent(true);
    verifReq.setVerifiedAt(LocalDateTime.now());
    verifReq.setNotes("Confirmed by TechCorp Placement Sol HR department");

    RecordVerificationVerdictRequest verdictReq = new RecordVerificationVerdictRequest(
        verifReq,
        Collections.emptyList(),
        null,
        1L // Sync employment verification status
    );

    ResponseEntity<String> verdictResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/verification/record-verdict"),
        HttpMethod.POST,
        new HttpEntity<>(verdictReq, employerAHeaders),
        String.class
    );
    assertThat(verdictResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode verdictJson = parseResponseJson(verdictResponse.getBody());
    Long verificationId = verdictJson.get("data").get("verification").get("id").asLong();
    trackedVerificationIds.add(verificationId);

    Integer verifCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM employment_verifications WHERE employment_verification_id = ? AND employer_confirmed_flag = 1",
        Integer.class, verificationId);
    assertThat(verifCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 6: Six-Month Wage / Salary Progression via POST /api/v1/salary-history
    // -------------------------------------------------------------------------
    CreateSalaryHistoryRequest sixMonthSalaryReq = new CreateSalaryHistoryRequest(
        employmentId,
        new BigDecimal("48000.00"), // Promotion wage
        1L, // MONTHLY
        LocalDate.now().plusMonths(6),
        3L, // EMPLOYER_CONFIRMATION
        1L  // VERIFIED
    );
    sixMonthSalaryReq.setTraineeId(FIXTURE_TRAINEE_ID);
    sixMonthSalaryReq.setObservationMonthOffset(6);

    ResponseEntity<String> salaryResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/salary-history"),
        HttpMethod.POST,
        new HttpEntity<>(sixMonthSalaryReq, employerAHeaders),
        String.class
    );
    assertThat(salaryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode salaryJson = parseResponseJson(salaryResponse.getBody());
    Long sixMonthSalaryId = salaryJson.get("data").get("id").asLong();
    trackedSalaryHistoryIds.add(sixMonthSalaryId);

    Integer dbMilestoneOffset = jdbcTemplate.queryForObject(
        "SELECT observation_month_offset FROM salary_history WHERE salary_history_id = ?",
        Integer.class, sixMonthSalaryId);
    assertThat(dbMilestoneOffset).isEqualTo(6);

    // -------------------------------------------------------------------------
    // Step 7: Record Exit and Initiate Unemployment via Workflow API
    // -------------------------------------------------------------------------
    CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest(
        employmentId,
        LocalDate.now(),
        1L, // LOW_SALARY
        1L, // VOLUNTARY
        3L, // EMPLOYER_CONFIRMATION
        1L  // VERIFIED
    );

    CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest(
        FIXTURE_TRAINEE_ID,
        LocalDate.now(),
        1L, // Labour Status
        1L, // Unemployment Reason
        2L, // TRAINEE_DECLARATION
        1L  // UNVERIFIED
    );

    EmploymentExitAndUnemploymentRequest exitAndUnempReq = new EmploymentExitAndUnemploymentRequest(
        exitReq,
        unempReq
    );

    ResponseEntity<String> exitResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/employment-lifecycle/record-exit-and-initiate-unemployment"),
        HttpMethod.POST,
        new HttpEntity<>(exitAndUnempReq, employerAHeaders),
        String.class
    );
    assertThat(exitResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode exitJson = parseResponseJson(exitResponse.getBody());
    Long exitEventId = exitJson.get("data").get("exitEvent").get("id").asLong();
    Long unemploymentEventId = exitJson.get("data").get("unemploymentEvent").get("id").asLong();
    trackedExitEventIds.add(exitEventId);
    trackedUnemploymentEventIds.add(unemploymentEventId);

    // Direct Database Assertion: employment marked not current, exit event recorded, unemployment registered
    Integer isCurrentEmp = jdbcTemplate.queryForObject(
        "SELECT is_current FROM employment_records WHERE employment_id = ?",
        Integer.class, employmentId);
    assertThat(isCurrentEmp).isEqualTo(0);

    Integer dbExitCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM employment_exit_events WHERE employment_exit_event_id = ?",
        Integer.class, exitEventId);
    assertThat(dbExitCount).isEqualTo(1);

    Integer dbUnempCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM trainee_unemployment_events WHERE unemployment_event_id = ?",
        Integer.class, unemploymentEventId);
    assertThat(dbUnempCount).isEqualTo(1);
  }

  @Test
  @DisplayName("Negative Security: Cross-Tenant Employer Cannot Mutate or Access Other Employer Records")
  void testCrossTenantEmployerCannotAccessOrMutateOtherEmployerRecords() throws Exception {
    long timestamp = System.currentTimeMillis();

    // 1. Create Placement belonging to Employer A
    String placementNumber = "PLC-XTENANT-" + timestamp;
    CreatePlacementRecordRequest plcReq = new CreatePlacementRecordRequest(
        placementNumber,
        FIXTURE_ENROLLMENT_ID,
        1L, // WAGE_EMPLOYMENT
        1L, // CAMPUS_DRIVE
        2L, // OFFERED
        2L, // PENDING
        1L  // PENDING_VERIFICATION
    );
    plcReq.setTraineeId(FIXTURE_TRAINEE_ID);
    plcReq.setCourseId(FIXTURE_COURSE_ID);
    plcReq.setProgramId(FIXTURE_PROGRAM_ID);
    plcReq.setProviderId(FIXTURE_PROVIDER_ID);
    plcReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    plcReq.setJobRoleId(FIXTURE_JOB_ROLE_ID);
    plcReq.setOfferedSalary(new BigDecimal("40000.00"));

    ResponseEntity<String> plcResp = restTemplate.exchange(
        getBaseUrl("/api/v1/placement-records"),
        HttpMethod.POST,
        new HttpEntity<>(plcReq, adminHeaders),
        String.class
    );
    assertThat(plcResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long placementId = parseResponseJson(plcResp.getBody()).get("data").get("id").asLong();
    trackedPlacementIds.add(placementId);

    // 2. Employer B attempts to transition Employer A's placement to employment -> 403 Forbidden
    CreateEmploymentRecordRequest unauthorizedEmpReq = new CreateEmploymentRecordRequest();
    unauthorizedEmpReq.setEmploymentNumber("EMP-UNAUTH-" + timestamp);
    unauthorizedEmpReq.setTraineeId(FIXTURE_TRAINEE_ID);
    unauthorizedEmpReq.setEnrollmentId(FIXTURE_ENROLLMENT_ID);
    unauthorizedEmpReq.setPlacementId(placementId);
    unauthorizedEmpReq.setEmployerId(FIXTURE_EMPLOYER_B_ID); // Mismatched employer ID
    unauthorizedEmpReq.setEngagementTypeId(1L);
    unauthorizedEmpReq.setEmploymentSpellStatusId(1L);
    unauthorizedEmpReq.setEmploymentInfoSourceId(1L);
    unauthorizedEmpReq.setRecordVerificationStatusId(1L);
    unauthorizedEmpReq.setStartDate(LocalDate.now());

    PlacementToEmploymentTransitionRequest badTransitionReq = new PlacementToEmploymentTransitionRequest(
        placementId,
        unauthorizedEmpReq,
        null,
        3L,
        4L
    );

    ResponseEntity<String> forbiddenTransitionResp = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/placement-employment/transition-placement-to-employment"),
        HttpMethod.POST,
        new HttpEntity<>(badTransitionReq, employerBHeaders),
        String.class
    );
    assertThat(forbiddenTransitionResp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN);
    if (forbiddenTransitionResp.getStatusCode() == HttpStatus.BAD_REQUEST) {
      assertThat(parseResponseJson(forbiddenTransitionResp.getBody()).get("code").asText()).isEqualTo("EMPLOYER_MISMATCH");
    }

    // 3. Employer A transitions its placement to employment
    unauthorizedEmpReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    PlacementToEmploymentTransitionRequest validTransitionReq = new PlacementToEmploymentTransitionRequest(
        placementId,
        unauthorizedEmpReq,
        null,
        3L,
        4L
    );
    ResponseEntity<String> validTransResp = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/placement-employment/transition-placement-to-employment"),
        HttpMethod.POST,
        new HttpEntity<>(validTransitionReq, employerAHeaders),
        String.class
    );
    assertThat(validTransResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long employmentId = parseResponseJson(validTransResp.getBody()).get("data").get("employmentRecord").get("id").asLong();
    trackedEmploymentIds.add(employmentId);

    // 4. Employer B attempts to record verification verdict on Employer A's employment -> 403 Forbidden
    CreateEmploymentVerificationRequest badVerifReq = new CreateEmploymentVerificationRequest(
        "VERIF-UNAUTH-" + timestamp,
        employmentId,
        1L,
        1L,
        LocalDateTime.now()
    );
    badVerifReq.setEmployerId(FIXTURE_EMPLOYER_A_ID);
    RecordVerificationVerdictRequest badVerdictReq = new RecordVerificationVerdictRequest(
        badVerifReq,
        Collections.emptyList(),
        null,
        1L
    );

    ResponseEntity<String> forbiddenVerdictResp = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/verification/record-verdict"),
        HttpMethod.POST,
        new HttpEntity<>(badVerdictReq, employerBHeaders),
        String.class
    );
    assertThat(forbiddenVerdictResp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN);
    if (forbiddenVerdictResp.getStatusCode() == HttpStatus.BAD_REQUEST) {
      assertThat(parseResponseJson(forbiddenVerdictResp.getBody()).get("code").asText()).isEqualTo("EMPLOYER_MISMATCH");
    }

    // 5. Employer B attempts to inspect Employer A's verification summary -> 400/403
    ResponseEntity<String> forbiddenSummaryResp = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/verification/summary?employmentId=" + employmentId),
        HttpMethod.GET,
        new HttpEntity<>(employerBHeaders),
        String.class
    );
    assertThat(forbiddenSummaryResp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN);
    if (forbiddenSummaryResp.getStatusCode() == HttpStatus.BAD_REQUEST) {
      assertThat(parseResponseJson(forbiddenSummaryResp.getBody()).get("code").asText()).isEqualTo("EMPLOYER_MISMATCH");
    }

    // 6. Employer B attempts to record exit on Employer A's employment record -> 400/403
    CreateEmploymentExitEventRequest badExitReq = new CreateEmploymentExitEventRequest(
        employmentId,
        LocalDate.now(),
        1L,
        1L,
        3L,
        1L
    );
    EmploymentExitAndUnemploymentRequest badExitAndUnempReq = new EmploymentExitAndUnemploymentRequest(
        badExitReq,
        null
    );

    ResponseEntity<String> forbiddenExitResp = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/employment-lifecycle/record-exit-and-initiate-unemployment"),
        HttpMethod.POST,
        new HttpEntity<>(badExitAndUnempReq, employerBHeaders),
        String.class
    );
    assertThat(forbiddenExitResp.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN);
    if (forbiddenExitResp.getStatusCode() == HttpStatus.BAD_REQUEST) {
      assertThat(parseResponseJson(forbiddenExitResp.getBody()).get("code").asText()).isEqualTo("EMPLOYER_MISMATCH");
    }
  }
}
