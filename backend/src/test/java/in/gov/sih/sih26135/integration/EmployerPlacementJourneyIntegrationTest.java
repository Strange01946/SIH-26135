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
import java.util.UUID;
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

  private Long fixtureAdminUserId;
  private Long fixtureEmployerAId;
  private Long fixtureEmployerBId;
  private Long fixtureEmpAUserId;
  private Long fixtureEmpBUserId;

  private Long fixtureSectorId;
  private Long fixtureJobRoleId;
  private Long fixtureCourseId;
  private Long fixtureOrgId;
  private Long fixtureDeptId;
  private Long fixtureSchemeId;
  private Long fixtureProgramId;
  private Long fixtureProviderId;
  private Long fixtureCenterId;
  private Long fixtureBatchId;
  private Long fixtureTraineeId;
  private Long fixtureConsentId;
  private Long fixtureSmsConsentId;
  private Long fixtureEnrollmentId;
  private String runToken;

  private String adminToken;
  private HttpHeaders adminHeaders;
  private String employerAToken;
  private HttpHeaders employerAHeaders;
  private String employerBToken;
  private HttpHeaders employerBHeaders;

  @BeforeEach
  void setUpEmployerPlacementFixtures() {
    runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // 1. Provision Admin User
    fixtureAdminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NULL, NULL, NOW(), NOW())
        """, "superadmin_emp_" + runToken, "superadmin_emp_" + runToken + "@test.gov.in");
    trackedUserIds.add(fixtureAdminUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        """, fixtureAdminUserId);

    adminToken = generateBearerToken(
        fixtureAdminUserId,
        "superadmin_emp_" + runToken,
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
    fixtureSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-EMP-" + runToken, "Information Tech " + runToken);
    trackedSectorIds.add(fixtureSectorId);

    fixtureCourseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-EMP-" + runToken, "Cloud Architecture " + runToken, fixtureSectorId);
    trackedCourseIds.add(fixtureCourseId);

    fixtureOrgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-EMP-" + runToken, "Placement Council " + runToken);
    trackedOrgIds.add(fixtureOrgId);

    fixtureDeptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, fixtureOrgId, "DEP-EMP-" + runToken, "Enterprise Placement " + runToken);
    trackedDeptIds.add(fixtureDeptId);

    fixtureSchemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-EMP-" + runToken, "Tech Placement Scheme " + runToken, fixtureDeptId);
    trackedSchemeIds.add(fixtureSchemeId);

    fixtureProgramId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-EMP-" + runToken, "Tech Talent Program " + runToken, fixtureDeptId, fixtureSchemeId);
    trackedProgramIds.add(fixtureProgramId);

    fixtureProviderId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-EMP-" + runToken, "Tech Academy " + runToken, "REG-PRV-EMP-" + runToken, fixtureOrgId);
    trackedProviderIds.add(fixtureProviderId);

    fixtureCenterId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, fixtureProviderId, "CTR-EMP-" + runToken, "Connaught Center " + runToken);
    trackedCenterIds.add(fixtureCenterId);

    fixtureBatchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, '2026-01-01', 60, 1, NOW(), NOW())
        """, "BAT-EMP-" + runToken, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureProgramId);
    trackedBatchIds.add(fixtureBatchId);

    // 3. Job Role Fixture
    fixtureJobRoleId = insertAndGetId("""
        INSERT INTO job_roles (job_role_code, job_role_name, sector_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, "JR-EMP-" + runToken, "Senior Systems Architect " + runToken, fixtureSectorId);
    trackedJobRoleIds.add(fixtureJobRoleId);

    // 4. Employers A & B
    fixtureEmployerAId = insertAndGetId("""
        INSERT INTO employers (employer_code, employer_name, registration_number, organization_type_id, organization_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 6, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "EMP-295-A-" + runToken, "TechCorp Sol " + runToken, "REG-EMP-295-A-" + runToken, fixtureOrgId);
    trackedEmployerIds.add(fixtureEmployerAId);

    fixtureEmployerBId = insertAndGetId("""
        INSERT INTO employers (employer_code, employer_name, registration_number, organization_type_id, organization_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 6, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "EMP-295-B-" + runToken, "InnovateX Solutions " + runToken, "REG-EMP-295-B-" + runToken, fixtureOrgId);
    trackedEmployerIds.add(fixtureEmployerBId);

    // 5. Employer Users (Single-root multi-tenancy: state_id & district_id must be NULL)
    fixtureEmpAUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, employer_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, ?, NULL, NULL, NOW(), NOW())
        """, "emp_user_a_" + runToken, "emp_a_" + runToken + "@techcorp.com", fixtureEmployerAId);
    trackedUserIds.add(fixtureEmpAUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'employer' LIMIT 1
        """, fixtureEmpAUserId);

    employerAToken = generateBearerToken(
        fixtureEmpAUserId,
        "emp_user_a_" + runToken,
        List.of("ROLE_employer", "placement.manage", "employment.manage", "verification.manage")
    );
    employerAHeaders = createAuthHeaders(employerAToken);

    fixtureEmpBUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, employer_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, ?, NULL, NULL, NOW(), NOW())
        """, "emp_user_b_" + runToken, "emp_b_" + runToken + "@innovatex.com", fixtureEmployerBId);
    trackedUserIds.add(fixtureEmpBUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'employer' LIMIT 1
        """, fixtureEmpBUserId);

    employerBToken = generateBearerToken(
        fixtureEmpBUserId,
        "emp_user_b_" + runToken,
        List.of("ROLE_employer", "placement.manage", "employment.manage", "verification.manage")
    );
    employerBHeaders = createAuthHeaders(employerBToken);

    // 6. Trainee Fixture with Masked Phone (+XXXXXXXX0957)
    fixtureTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, phone, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'Vikram', '+919876540957', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, "REG-EMP-" + runToken);
    trackedTraineeIds.add(fixtureTraineeId);

    // Clean rate limit window for test trainee and provider
    jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ? OR limiter_key = ?",
        "TRAINEE:%:" + fixtureTraineeId, "DEDUP:" + fixtureTraineeId + ":%", "PROVIDER:" + fixtureProviderId);

    // Statutory DPDP Consent: DATA_PROCESSING & COMMUNICATION_SMS
    fixtureConsentId = insertAndGetId("""
        INSERT INTO trainee_consents (trainee_id, consent_type_id, consent_status_id, purpose, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
        VALUES (?, 1, 1, 'Statutory Data Processing Consent', 'v1.0', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 1 YEAR, 'PORTAL', NOW(), NOW())
        """, fixtureTraineeId);
    trackedConsentIds.add(fixtureConsentId);

    fixtureSmsConsentId = insertAndGetId("""
        INSERT INTO trainee_consents (trainee_id, consent_type_id, consent_status_id, purpose, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
        VALUES (?, 5, 1, 'Statutory SMS Notification Consent', 'v1.0', NOW() - INTERVAL 5 MINUTE, NOW() + INTERVAL 1 YEAR, 'PORTAL', NOW(), NOW())
        """, fixtureTraineeId);
    trackedConsentIds.add(fixtureSmsConsentId);

    // Completed Enrollment
    fixtureEnrollmentId = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, program_id, batch_id, course_id, provider_id, center_id, enrollment_status_id, enrollment_date, actual_completion_date, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 3, '2026-01-01', '2026-03-01', NOW(), NOW())
        """, "ENR-EMP-" + runToken, fixtureTraineeId, fixtureProgramId, fixtureBatchId, fixtureCourseId, fixtureProviderId, fixtureCenterId);
    trackedEnrollmentIds.add(fixtureEnrollmentId);
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
        fixtureJobRoleId,
        1L, // Full-time wage employment
        BASE_STATE_ID,
        BASE_DISTRICT_ID,
        LocalDate.now(),
        1L // Open / Active status
    );
    jobRequest.setEmployerId(fixtureEmployerAId);
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
    assertThat(jobJson.get("data").get("employerId").asLong()).isEqualTo(fixtureEmployerAId);

    // -------------------------------------------------------------------------
    // Step 2: Placement Creation via POST /api/v1/placement-records
    // -------------------------------------------------------------------------
    String placementNumber = "PLC-EMP-295-" + timestamp;
    CreatePlacementRecordRequest placementReq = new CreatePlacementRecordRequest(
        placementNumber,
        fixtureEnrollmentId,
        1L, // WAGE_EMPLOYMENT
        1L, // CAMPUS_DRIVE
        2L, // OFFERED
        2L, // PENDING
        1L  // PENDING_VERIFICATION
    );
    placementReq.setTraineeId(fixtureTraineeId);
    placementReq.setCourseId(fixtureCourseId);
    placementReq.setProgramId(fixtureProgramId);
    placementReq.setProviderId(fixtureProviderId);
    placementReq.setEmployerId(fixtureEmployerAId);
    placementReq.setJobRoleId(fixtureJobRoleId);
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
    empReq.setTraineeId(fixtureTraineeId);
    empReq.setEnrollmentId(fixtureEnrollmentId);
    empReq.setPlacementId(placementId);
    empReq.setEmployerId(fixtureEmployerAId);
    empReq.setJobRoleId(fixtureJobRoleId);
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
    initialSalary.setTraineeId(fixtureTraineeId);
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
        fixtureTraineeId,
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
    verifReq.setTraineeId(fixtureTraineeId);
    verifReq.setPlacementRecordId(placementId);
    verifReq.setEmployerId(fixtureEmployerAId);
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
    sixMonthSalaryReq.setTraineeId(fixtureTraineeId);
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
        fixtureTraineeId,
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
        fixtureEnrollmentId,
        1L, // WAGE_EMPLOYMENT
        1L, // CAMPUS_DRIVE
        2L, // OFFERED
        2L, // PENDING
        1L  // PENDING_VERIFICATION
    );
    plcReq.setTraineeId(fixtureTraineeId);
    plcReq.setCourseId(fixtureCourseId);
    plcReq.setProgramId(fixtureProgramId);
    plcReq.setProviderId(fixtureProviderId);
    plcReq.setEmployerId(fixtureEmployerAId);
    plcReq.setJobRoleId(fixtureJobRoleId);
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
    unauthorizedEmpReq.setTraineeId(fixtureTraineeId);
    unauthorizedEmpReq.setEnrollmentId(fixtureEnrollmentId);
    unauthorizedEmpReq.setPlacementId(placementId);
    unauthorizedEmpReq.setEmployerId(fixtureEmployerBId); // Mismatched employer ID
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
    unauthorizedEmpReq.setEmployerId(fixtureEmployerAId);
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
    badVerifReq.setEmployerId(fixtureEmployerAId);
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
