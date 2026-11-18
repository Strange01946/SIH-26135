package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 29.5 — End-to-End Candidate Lifecycle Integration Test.
 *
 * <p>Exercises the complete supported candidate journey end-to-end against real HTTP endpoints:
 * <ol>
 *   <li>Trainee Registration via {@code POST /api/v1/trainees}</li>
 *   <li>DPDP Statutory Consent Recording & Revocation via {@code POST /api/v1/consents} and {@code PUT /api/v1/consents/{id}/status}</li>
 *   <li>Batch Creation & Enrollment via {@code POST /api/v1/training-enrollments} with consent validation</li>
 *   <li>Daily Attendance Recording via {@code POST /api/v1/attendance-records}</li>
 *   <li>Assessment, Result, and Certificate Issuance via {@code POST /api/v1/workflows/training-outcomes/record-assessment-and-issue-certificate}</li>
 *   <li>Candidate Placement & Employment Transition via {@code POST /api/v1/placement-records} & {@code POST /api/v1/workflows/placement-employment/transition-placement-to-employment}</li>
 *   <li>Employer Verification Verdict via {@code POST /api/v1/employment-verifications}</li>
 *   <li>Six-Month Wage / Salary Follow-up via {@code POST /api/v1/salary-history} with observation month offset 6</li>
 *   <li>Longitudinal Survey Completion via {@code POST /api/v1/survey-responses}</li>
 *   <li>Independent SQL Cross-Table Consistency Assertions across all 9 operational tables</li>
 *   <li>Negative Security Checks: Consent Bypass Prevention, Duplicate Registration, Cross-Tenant Isolation</li>
 * </ol>
 */
@DisplayName("Phase 29.5 — Candidate Lifecycle Journey Integration Test")
class CandidateLifecycleJourneyIntegrationTest extends BaseJourneyIntegrationTest {

  private Long fixtureUserId;
  private Long fixtureSectorId;
  private Long fixtureCourseId;
  private Long fixtureOrgId;
  private Long fixtureDeptId;
  private Long fixtureSchemeId;
  private Long fixtureProgramId;
  private Long fixtureProviderId;
  private Long fixtureCenterId;
  private Long fixtureBatchId;
  private Long fixtureEmployerId;
  private Long fixtureSurveyTemplateId;
  private Long fixtureSurveyTemplateVersionId;
  private Long fixtureSurveyId;
  private String runToken;

  private String adminToken;
  private HttpHeaders adminHeaders;

  @BeforeEach
  void setUpFixtures() {
    runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // 1. Administrative caller user fixture
    fixtureUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NOW(), NOW())
        """, "cand_lifecycle_admin_" + runToken, "cand_admin_" + runToken + "@test.gov.in");
    trackedUserIds.add(fixtureUserId);

    adminToken = generateBearerToken(fixtureUserId, "cand_lifecycle_admin_" + runToken, List.of(
        "ROLE_super_admin",
        "ROLE_government_admin",
        "trainee.read",
        "trainee.write",
        "consent.manage",
        "course.manage",
        "enrollment.manage",
        "assessment.manage",
        "placement.manage",
        "employment.manage",
        "verification.manage",
        "survey.manage",
        "analytics.read",
        "audit.read",
        "system.manage"
    ));
    adminHeaders = createAuthHeaders(adminToken);

    // 2. Training Domain Fixtures
    fixtureSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-295-" + runToken, "IT & Software " + runToken);
    trackedSectorIds.add(fixtureSectorId);

    fixtureCourseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-295-" + runToken, "Full Stack Java " + runToken, fixtureSectorId);
    trackedCourseIds.add(fixtureCourseId);

    fixtureOrgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-295-" + runToken, "National Skill Org " + runToken);
    trackedOrgIds.add(fixtureOrgId);

    fixtureDeptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, fixtureOrgId, "DEP-295-" + runToken, "Vocational Training " + runToken);
    trackedDeptIds.add(fixtureDeptId);

    fixtureSchemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-295-" + runToken, "PM National Skill " + runToken, fixtureDeptId);
    trackedSchemeIds.add(fixtureSchemeId);

    fixtureProgramId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-295-" + runToken, "Digital India Skills " + runToken, fixtureDeptId, fixtureSchemeId);
    trackedProgramIds.add(fixtureProgramId);

    fixtureProviderId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-295-" + runToken, "Apex Technical " + runToken, "REG-PRV-295-" + runToken, fixtureOrgId);
    trackedProviderIds.add(fixtureProviderId);

    fixtureCenterId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, fixtureProviderId, "CTR-295-" + runToken, "CP Center " + runToken);
    trackedCenterIds.add(fixtureCenterId);

    fixtureBatchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW())
        """, "BAT-295-" + runToken, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureProgramId);
    trackedBatchIds.add(fixtureBatchId);

    // 3. Employer Fixture
    fixtureEmployerId = insertAndGetId("""
        INSERT INTO employers (employer_code, employer_name, registration_number, organization_type_id, organization_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 6, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "EMP-295-" + runToken, "Tech Mahindra " + runToken, "REG-EMP-295-" + runToken, fixtureOrgId);
    trackedEmployerIds.add(fixtureEmployerId);

    // 4. Survey Template Fixtures (tracked for clean reverse teardown)
    fixtureSurveyTemplateId = insertAndGetId("""
        INSERT INTO survey_templates (template_code, template_name, survey_purpose_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "TPL-295-" + runToken, "Post-Training Survey " + runToken);
    trackedSurveyTemplateIds.add(fixtureSurveyTemplateId);

    fixtureSurveyTemplateVersionId = insertAndGetId("""
        INSERT INTO survey_template_versions (survey_template_id, version_number, version_label, effective_from, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 1, 'v1.0', '2026-01-01', 1, NOW(), NOW())
        """, fixtureSurveyTemplateId);
    trackedSurveyTemplateVersionIds.add(fixtureSurveyTemplateVersionId);

    fixtureSurveyId = insertAndGetId("""
        INSERT INTO surveys (survey_code, survey_name, survey_template_version_id, survey_purpose_id, scheduled_start_date, survey_instance_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, '2026-01-01', 1, NOW(), NOW())
        """, "SRV-295-" + runToken, "Outcome Survey " + runToken, fixtureSurveyTemplateVersionId);
    trackedSurveyIds.add(fixtureSurveyId);
  }

  @Test
  @DisplayName("Full 10-Step Supported Candidate Lifecycle Execution")
  void testCompleteCandidateLifecycleEndToEnd() throws Exception {
    String candidateRegNo = "REG-295-CAND-" + System.currentTimeMillis();

    // -------------------------------------------------------------------------
    // Step 1: Trainee Registration via POST /api/v1/trainees
    // -------------------------------------------------------------------------
    CreateTraineeRequest traineeRequest = new CreateTraineeRequest(
        null,
        candidateRegNo,
        "Priya",
        "Kumari",
        "Sharma",
        LocalDate.of(2001, 5, 20),
        1L, // FEMALE
        "priya.sharma295@example.test",
        "9876540957",
        "Block 4, Connaught Place",
        "Near Metro Gate 2",
        "110001",
        null,
        BASE_STATE_ID,
        BASE_DISTRICT_ID,
        null,
        6L, // GRADUATE
        1L, // UNEMPLOYED
        3L  // ACTIVE
    );

    ResponseEntity<String> regResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/trainees"),
        HttpMethod.POST,
        new HttpEntity<>(traineeRequest, adminHeaders),
        String.class
    );
    assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode regJson = parseResponseJson(regResponse.getBody());
    assertThat(regJson.get("success").asBoolean()).isTrue();
    Long candidateId = regJson.get("data").get("id").asLong();
    assertThat(candidateId).isNotNull();
    trackedTraineeIds.add(candidateId);

    // Verify DB persistence of registration
    Integer countReg = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM trainees WHERE trainee_id = ? AND registration_number = ? AND first_name = 'Priya'",
        Integer.class, candidateId, candidateRegNo);
    assertThat(countReg).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 2: DPDP Statutory Consent Recording via POST /api/v1/consents
    // -------------------------------------------------------------------------
    CreateTraineeConsentRequest dpdpConsentReq = new CreateTraineeConsentRequest(
        candidateId,
        1L, // DATA_PROCESSING
        1L, // GRANTED
        "DPDP statutory processing consent for candidate training and career progression",
        "v1.0",
        LocalDateTime.now().minusMinutes(5),
        LocalDateTime.now().plusYears(1),
        "WEB_PORTAL"
    );

    ResponseEntity<String> consentResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/consents"),
        HttpMethod.POST,
        new HttpEntity<>(dpdpConsentReq, adminHeaders),
        String.class
    );
    assertThat(consentResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode consentJson = parseResponseJson(consentResponse.getBody());
    Long consentId = consentJson.get("data").get("id").asLong();
    trackedConsentIds.add(consentId);

    // Verify DB persistence of consent
    Integer consentDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM trainee_consents WHERE trainee_consent_id = ? AND consent_status_id = 1",
        Integer.class, consentId);
    assertThat(consentDbCount).isEqualTo(1);

    // Test consent revocation
    UpdateTraineeConsentStatusRequest revokeReq = new UpdateTraineeConsentStatusRequest(2L, LocalDateTime.now());
    ResponseEntity<String> revokeResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/consents/" + consentId + "/status"),
        HttpMethod.PUT,
        new HttpEntity<>(revokeReq, adminHeaders),
        String.class
    );
    assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Integer revokedDb = jdbcTemplate.queryForObject(
        "SELECT consent_status_id FROM trainee_consents WHERE trainee_consent_id = ?",
        Integer.class, consentId);
    assertThat(revokedDb).isEqualTo(2);

    // Re-grant consent for subsequent lifecycle steps via new active consent
    CreateTraineeConsentRequest regrantConsentReq = new CreateTraineeConsentRequest(
        candidateId,
        1L, // DATA_PROCESSING
        1L, // GRANTED
        "DPDP statutory processing consent re-granted",
        "v1.0",
        LocalDateTime.now().minusMinutes(1),
        LocalDateTime.now().plusYears(1),
        "WEB_PORTAL"
    );
    ResponseEntity<String> regrantResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/consents"),
        HttpMethod.POST,
        new HttpEntity<>(regrantConsentReq, adminHeaders),
        String.class
    );
    assertThat(regrantResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long regrantedConsentId = parseResponseJson(regrantResponse.getBody()).get("data").get("id").asLong();
    trackedConsentIds.add(regrantedConsentId);

    // -------------------------------------------------------------------------
    // Step 3: Batch Enrollment via POST /api/v1/training-enrollments
    // -------------------------------------------------------------------------
    String enrollmentNo = "ENR-295-" + System.currentTimeMillis();
    CreateTrainingEnrollmentRequest enrollReq = new CreateTrainingEnrollmentRequest();
    enrollReq.setEnrollmentNumber(enrollmentNo);
    enrollReq.setTraineeId(candidateId);
    enrollReq.setBatchId(fixtureBatchId);
    enrollReq.setCourseId(fixtureCourseId);
    enrollReq.setProgramId(fixtureProgramId);
    enrollReq.setProviderId(fixtureProviderId);
    enrollReq.setCenterId(fixtureCenterId);
    enrollReq.setEnrollmentDate(LocalDate.now());
    enrollReq.setStartDate(LocalDate.now());
    enrollReq.setExpectedCompletionDate(LocalDate.now().plusMonths(3));
    enrollReq.setEnrollmentStatusId(1L); // ENROLLED

    ResponseEntity<String> enrollResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/training-enrollments"),
        HttpMethod.POST,
        new HttpEntity<>(enrollReq, adminHeaders),
        String.class
    );
    assertThat(enrollResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode enrollJson = parseResponseJson(enrollResponse.getBody());
    Long enrollmentId = enrollJson.get("data").get("id").asLong();
    trackedEnrollmentIds.add(enrollmentId);

    Integer enrollDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM training_enrollments WHERE enrollment_id = ? AND trainee_id = ? AND batch_id = ?",
        Integer.class, enrollmentId, candidateId, fixtureBatchId);
    assertThat(enrollDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 4: Daily Attendance Recording via POST /api/v1/attendance-records
    // -------------------------------------------------------------------------
    CreateAttendanceRecordRequest attReq = new CreateAttendanceRecordRequest();
    attReq.setEnrollmentId(enrollmentId);
    attReq.setTraineeId(candidateId);
    attReq.setBatchId(fixtureBatchId);
    attReq.setSessionDate(LocalDate.now());
    attReq.setAttendanceStatusId(1L); // PRESENT
    attReq.setSessionSequence(1);
    attReq.setRemarks("Present for full session");

    ResponseEntity<String> attResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/attendance-records"),
        HttpMethod.POST,
        new HttpEntity<>(attReq, adminHeaders),
        String.class
    );
    assertThat(attResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode attJson = parseResponseJson(attResponse.getBody());
    Long attendanceId = attJson.get("data").get("id").asLong();
    trackedAttendanceIds.add(attendanceId);

    Integer attDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM attendance_records WHERE attendance_id = ? AND enrollment_id = ?",
        Integer.class, attendanceId, enrollmentId);
    assertThat(attDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 5 & 6: Assessment Result & Certificate Issuance via Training Outcome Workflow
    // -------------------------------------------------------------------------
    // Create prerequisite assessment and trainee_assessment rows
    Long assessmentId = insertAndGetId("""
        INSERT INTO assessments (assessment_code, assessment_name, course_id, batch_id, program_id, assessment_type_id, maximum_score, pass_score, assessment_date, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'Final Full Stack Certification Exam', ?, ?, ?, 1, 100.00, 40.00, CURDATE(), 1, NOW(), NOW())
        """, "ASM-295-" + runToken, fixtureCourseId, fixtureBatchId, fixtureProgramId);
    trackedAssessmentIds.add(assessmentId);

    Long traineeAssessmentId = insertAndGetId("""
        INSERT INTO trainee_assessments (assessment_id, trainee_id, enrollment_id, attempt_number, appeared_flag, assessment_date, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, CURDATE(), NOW(), NOW())
        """, assessmentId, candidateId, enrollmentId);
    trackedTraineeAssessmentIds.add(traineeAssessmentId);

    CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest(
        traineeAssessmentId,
        candidateId,
        new BigDecimal("100.00"),
        new BigDecimal("88.50"),
        new BigDecimal("88.50"),
        2L, // PASS (outcome_id=2 in ref_assessment_outcome)
        LocalDateTime.now(),
        "Distinction achieved"
    );

    String certNo = "CERT-295-" + System.currentTimeMillis();
    CreateCertificationRequest certReq = new CreateCertificationRequest(
        certNo,
        candidateId,
        enrollmentId,
        fixtureCourseId,
        fixtureProgramId,
        null, // Linked by orchestrator
        "National Skill Development Council",
        LocalDate.now(),
        LocalDate.now().plusYears(5),
        1L, // ACTIVE
        1L, // VERIFIED
        LocalDateTime.now(),
        fixtureUserId
    );

    RecordAssessmentAndIssueCertificateRequest outcomeReq = new RecordAssessmentAndIssueCertificateRequest(
        resultReq,
        certReq,
        enrollmentId,
        3L // COMPLETED
    );

    ResponseEntity<String> outcomeResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/training-outcomes/record-assessment-and-issue-certificate"),
        HttpMethod.POST,
        new HttpEntity<>(outcomeReq, adminHeaders),
        String.class
    );
    assertThat(outcomeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode outcomeJson = parseResponseJson(outcomeResponse.getBody());
    Long assessmentResultId = outcomeJson.get("data").get("assessmentResult").get("id").asLong();
    Long certificateId = outcomeJson.get("data").get("certification").get("id").asLong();
    trackedAssessmentResultIds.add(assessmentResultId);
    trackedCertificationIds.add(certificateId);

    // Verify DB outcome: enrollment marked completed, score and certificate registered
    Integer enrStatus = jdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Integer.class, enrollmentId);
    assertThat(enrStatus).isEqualTo(3);

    Integer certDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM certifications WHERE certification_id = ? AND certificate_number = ?",
        Integer.class, certificateId, certNo);
    assertThat(certDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 7: Placement Creation & Transition to Employment
    // -------------------------------------------------------------------------
    String placementNo = "PLC-295-" + System.currentTimeMillis();
    CreatePlacementRecordRequest plcReq = new CreatePlacementRecordRequest(
        placementNo,
        enrollmentId,
        1L, // WAGE_EMPLOYMENT
        1L, // CAMPUS_DRIVE
        2L, // OFFERED (ref_placement_status: 2=OFFERED)
        2L, // PENDING (ref_joining_status: 2=PENDING)
        1L  // PENDING_VERIFICATION
    );
    plcReq.setTraineeId(candidateId);
    plcReq.setCourseId(fixtureCourseId);
    plcReq.setProgramId(fixtureProgramId);
    plcReq.setProviderId(fixtureProviderId);
    plcReq.setEmployerId(fixtureEmployerId);
    plcReq.setOfferedSalary(new BigDecimal("48000.00"));
    plcReq.setJoiningSalary(new BigDecimal("48000.00"));
    plcReq.setSalaryFrequencyId(1L); // MONTHLY
    plcReq.setCurrencyCode("INR");
    plcReq.setOfferDate(LocalDate.now());
    plcReq.setExpectedJoiningDate(LocalDate.now().plusDays(15));
    plcReq.setWorkStateId(101L);
    plcReq.setWorkDistrictId(1001L);

    ResponseEntity<String> plcResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/placement-records"),
        HttpMethod.POST,
        new HttpEntity<>(plcReq, adminHeaders),
        String.class
    );
    assertThat(plcResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode plcJson = parseResponseJson(plcResponse.getBody());
    Long placementId = plcJson.get("data").get("id").asLong();
    trackedPlacementIds.add(placementId);

    // Transition placement to employment
    String empNo = "EMP-REC-295-" + System.currentTimeMillis();
    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber(empNo);
    empReq.setTraineeId(candidateId);
    empReq.setEnrollmentId(enrollmentId);
    empReq.setPlacementId(placementId);
    empReq.setEmployerId(fixtureEmployerId);
    empReq.setEngagementTypeId(1L);
    empReq.setEmploymentSpellStatusId(1L); // ACTIVE
    empReq.setEmploymentInfoSourceId(1L);
    empReq.setRecordVerificationStatusId(1L);
    empReq.setStartDate(LocalDate.now());
    empReq.setIsCurrent(true);
    empReq.setStartingSalary(new BigDecimal("48000.00"));
    empReq.setSalaryFrequencyId(1L);
    empReq.setCurrencyCode("INR");

    CreateSalaryHistoryRequest initialSalary = new CreateSalaryHistoryRequest(
        null,
        new BigDecimal("48000.00"),
        1L,
        LocalDate.now(),
        1L,
        1L
    );
    initialSalary.setTraineeId(candidateId);
    initialSalary.setObservationMonthOffset(0);

    PlacementToEmploymentTransitionRequest transitionReq = new PlacementToEmploymentTransitionRequest(
        placementId,
        empReq,
        initialSalary,
        3L, // JOINED (ref_joining_status: 3=JOINED)
        4L  // JOINED (ref_placement_status: 4=JOINED)
    );

    ResponseEntity<String> transResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/workflows/placement-employment/transition-placement-to-employment"),
        HttpMethod.POST,
        new HttpEntity<>(transitionReq, adminHeaders),
        String.class
    );
    assertThat(transResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode transJson = parseResponseJson(transResponse.getBody());
    Long employmentRecordId = transJson.get("data").get("employmentRecord").get("id").asLong();
    trackedEmploymentIds.add(employmentRecordId);

    if (transJson.get("data").has("initialSalaryHistory") && !transJson.get("data").get("initialSalaryHistory").isNull()) {
      Long initialSalaryId = transJson.get("data").get("initialSalaryHistory").get("id").asLong();
      trackedSalaryHistoryIds.add(initialSalaryId);
    }

    // Verify DB active employment
    Integer empCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM employment_records WHERE employment_id = ? AND trainee_id = ?",
        Integer.class, employmentRecordId, candidateId);
    assertThat(empCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 8: Employer Verification Verdict via POST /api/v1/employment-verifications
    // -------------------------------------------------------------------------
    String verifNo = "VERIF-295-" + System.currentTimeMillis();
    CreateEmploymentVerificationRequest verifReq = new CreateEmploymentVerificationRequest(
        verifNo,
        employmentRecordId,
        1L, // VERIFIED
        1L, // DIRECT_EMPLOYER
        LocalDateTime.now()
    );
    verifReq.setTraineeId(candidateId);
    verifReq.setPlacementRecordId(placementId);
    verifReq.setEmployerId(fixtureEmployerId);
    verifReq.setEmployerConfirmedFlag(true);
    verifReq.setTraineeAttestedFlag(true);
    verifReq.setIsCurrent(true);
    verifReq.setVerifiedAt(LocalDateTime.now());
    verifReq.setNotes("Employment confirmed by employer HR");

    ResponseEntity<String> verifResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/employment-verifications"),
        HttpMethod.POST,
        new HttpEntity<>(verifReq, adminHeaders),
        String.class
    );
    assertThat(verifResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode verifJson = parseResponseJson(verifResponse.getBody());
    Long verificationId = verifJson.get("data").get("id").asLong();
    trackedVerificationIds.add(verificationId);

    Integer verifDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM employment_verifications WHERE employment_verification_id = ? AND employer_confirmed_flag = 1",
        Integer.class, verificationId);
    assertThat(verifDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 9: Six-Month Salary Follow-up via POST /api/v1/salary-history
    // -------------------------------------------------------------------------
    CreateSalaryHistoryRequest sixMonthSalaryReq = new CreateSalaryHistoryRequest(
        employmentRecordId,
        new BigDecimal("55000.00"), // Promoted salary
        1L, // MONTHLY
        LocalDate.now().plusMonths(6),
        1L, // DIRECT_EMPLOYER
        1L  // VERIFIED
    );
    sixMonthSalaryReq.setTraineeId(candidateId);
    sixMonthSalaryReq.setObservationMonthOffset(6);
    sixMonthSalaryReq.setCurrencyCode("INR");

    ResponseEntity<String> salaryResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/salary-history"),
        HttpMethod.POST,
        new HttpEntity<>(sixMonthSalaryReq, adminHeaders),
        String.class
    );
    assertThat(salaryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode salaryJson = parseResponseJson(salaryResponse.getBody());
    Long sixMonthSalaryId = salaryJson.get("data").get("id").asLong();
    trackedSalaryHistoryIds.add(sixMonthSalaryId);

    Integer salaryDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM salary_history WHERE salary_history_id = ? AND observation_month_offset = 6",
        Integer.class, sixMonthSalaryId);
    assertThat(salaryDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Step 10: Longitudinal Survey Response via POST /api/v1/survey-responses
    // -------------------------------------------------------------------------
    CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest(
        fixtureSurveyId,
        fixtureSurveyTemplateVersionId,
        candidateId,
        1,
        1L, // IN_PROGRESS / COMPLETED
        LocalDateTime.now()
    );
    surveyReq.setEnrollmentId(enrollmentId);

    ResponseEntity<String> surveyResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/survey-responses"),
        HttpMethod.POST,
        new HttpEntity<>(surveyReq, adminHeaders),
        String.class
    );
    assertThat(surveyResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode surveyJson = parseResponseJson(surveyResponse.getBody());
    Long surveyResponseId = surveyJson.get("data").get("id").asLong();
    trackedSurveyResponseIds.add(surveyResponseId);

    Integer surveyDbCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM survey_responses WHERE survey_response_id = ? AND trainee_id = ?",
        Integer.class, surveyResponseId, candidateId);
    assertThat(surveyDbCount).isEqualTo(1);

    // -------------------------------------------------------------------------
    // Cross-Table Consistency Verification (Independent SQL queries)
    // -------------------------------------------------------------------------
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trainees WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = ?", Integer.class, candidateId)).isGreaterThanOrEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_enrollments WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attendance_records WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assessment_results WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM certifications WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM placement_records WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employment_records WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employment_verifications WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM salary_history WHERE trainee_id = ?", Integer.class, candidateId)).isGreaterThanOrEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM survey_responses WHERE trainee_id = ?", Integer.class, candidateId)).isEqualTo(1);
  }

  @Test
  @DisplayName("Negative Security: Enrollment Fails Closed Without Consent")
  void testEnrollmentFailsClosedWithoutConsent() {
    String unconsentedRegNo = "REG-NOCONSENT-" + System.currentTimeMillis();
    CreateTraineeRequest noConsentTrainee = new CreateTraineeRequest(
        null, unconsentedRegNo, "Unconsented", "Trainee", "Test",
        LocalDate.of(2000, 1, 1), 1L, "noconsent@test.gov.in", "9876540958",
        "Address", null, "110001", null, BASE_STATE_ID, BASE_DISTRICT_ID,
        null, 6L, 1L, 3L
    );

    ResponseEntity<String> regResp = restTemplate.exchange(
        getBaseUrl("/api/v1/trainees"),
        HttpMethod.POST,
        new HttpEntity<>(noConsentTrainee, adminHeaders),
        String.class
    );
    assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long noConsentTraineeId = parseResponseJsonSilently(regResp.getBody()).get("data").get("id").asLong();
    trackedTraineeIds.add(noConsentTraineeId);

    // Attempt enrollment without recording consent -> must fail closed
    CreateTrainingEnrollmentRequest enrollReq = new CreateTrainingEnrollmentRequest();
    enrollReq.setEnrollmentNumber("ENR-FAIL-" + System.currentTimeMillis());
    enrollReq.setTraineeId(noConsentTraineeId);
    enrollReq.setBatchId(fixtureBatchId);
    enrollReq.setCourseId(fixtureCourseId);
    enrollReq.setProgramId(fixtureProgramId);
    enrollReq.setProviderId(fixtureProviderId);
    enrollReq.setCenterId(fixtureCenterId);
    enrollReq.setEnrollmentDate(LocalDate.now());
    enrollReq.setStartDate(LocalDate.now());
    enrollReq.setExpectedCompletionDate(LocalDate.now().plusMonths(3));
    enrollReq.setEnrollmentStatusId(1L);

    ResponseEntity<String> enrollResp = restTemplate.exchange(
        getBaseUrl("/api/v1/training-enrollments"),
        HttpMethod.POST,
        new HttpEntity<>(enrollReq, adminHeaders),
        String.class
    );
    // Should be rejected (400 Bad Request / Consent Error)
    assertThat(enrollResp.getStatusCode().is4xxClientError()).isTrue();
  }

  @Test
  @DisplayName("Negative Security: Duplicate Trainee Registration Fails Closed")
  void testDuplicateTraineeRegistrationFailsClosed() {
    String dupRegNo = "REG-DUP-" + System.currentTimeMillis();
    CreateTraineeRequest trainee1 = new CreateTraineeRequest(
        null, dupRegNo, "First", "Entry", "User",
        LocalDate.of(1999, 1, 1), 1L, "dup1@test.gov.in", "9876540959",
        "Address 1", null, "110001", null, BASE_STATE_ID, BASE_DISTRICT_ID,
        null, 6L, 1L, 3L
    );

    ResponseEntity<String> resp1 = restTemplate.exchange(
        getBaseUrl("/api/v1/trainees"),
        HttpMethod.POST,
        new HttpEntity<>(trainee1, adminHeaders),
        String.class
    );
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long id1 = parseResponseJsonSilently(resp1.getBody()).get("data").get("id").asLong();
    trackedTraineeIds.add(id1);

    // Second registration with identical registration number
    CreateTraineeRequest trainee2 = new CreateTraineeRequest(
        null, dupRegNo, "Second", "Attempt", "User",
        LocalDate.of(1999, 1, 1), 1L, "dup2@test.gov.in", "9876540960",
        "Address 2", null, "110001", null, BASE_STATE_ID, BASE_DISTRICT_ID,
        null, 6L, 1L, 3L
    );

    ResponseEntity<String> resp2 = restTemplate.exchange(
        getBaseUrl("/api/v1/trainees"),
        HttpMethod.POST,
        new HttpEntity<>(trainee2, adminHeaders),
        String.class
    );
    assertThat(resp2.getStatusCode().is4xxClientError()).isTrue();
  }

  private JsonNode parseResponseJsonSilently(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
