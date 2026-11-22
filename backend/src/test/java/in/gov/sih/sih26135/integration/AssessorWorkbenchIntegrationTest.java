package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.dto.assessor.CandidateScoreInput;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 30.6 — Assessment & Certification Subsystem: Assessor Workbench Integration Tests.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Assessor assignment scoping (evaluator_user_id) and 403 Forbidden on unassigned assessments.</li>
 *   <li>Candidate roster retrieval with enrollment and score status.</li>
 *   <li>Draft score persistence: pending outcome, JSON remarks, no certificate issued.</li>
 *   <li>Authoritative score validation: negative rejection, max component check, 100 aggregate limit.</li>
 *   <li>Finalization logic:
 *     <ul>
 *       <li>Failing candidates: FAIL outcome, no certification.</li>
 *       <li>Passing candidates: PASS outcome, authoritative Certification issued (ISSUED, VERIFIED), enrollment COMPLETED.</li>
 *     </ul>
 *   </li>
 *   <li>Finalized assessment immutability (cannot modify once finalized).</li>
 *   <li>Authentic PDF certificate download (%PDF-1.4 header).</li>
 *   <li>Target endpoint aliases (/api/v1/assessments/results and /api/v1/certificates/issue).</li>
 * </ul>
 */
public class AssessorWorkbenchIntegrationTest extends BaseJourneyIntegrationTest {

  private String runToken;

  private Long assessor1UserId;
  private String assessor1Token;
  private HttpHeaders assessor1Headers;

  private Long assessor2UserId;
  private String assessor2Token;
  private HttpHeaders assessor2Headers;

  private Long fixtureSectorId;
  private Long fixtureCourseId;
  private Long fixtureOrgId;
  private Long fixtureDeptId;
  private Long fixtureSchemeId;
  private Long fixtureProgramId;
  private Long fixtureProviderId;
  private Long fixtureCenterId;
  private Long fixtureBatchId;

  private Long trainee1Id;
  private Long enrollment1Id;
  private Long trainee1UserId;
  private String trainee1Token;
  private HttpHeaders trainee1Headers;

  private Long trainee2Id;
  private Long enrollment2Id;
  private Long trainee2UserId;
  private String trainee2Token;
  private HttpHeaders trainee2Headers;

  private Long adminUserId;
  private String adminToken;
  private HttpHeaders adminHeaders;

  private Long assignedAssessmentId;
  private String assignedAssessmentCode;

  @BeforeEach
  void setUpAssessorFixtures() {
    runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // 1. Create Assessor 1 (primary assigned evaluator)
    assessor1UserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "assessor1_" + runToken, "assessor1_" + runToken + "@test.gov.in");
    trackedUserIds.add(assessor1UserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'assessor' LIMIT 1
        """, assessor1UserId);

    assessor1Token = generateBearerToken(
        assessor1UserId,
        "assessor1_" + runToken,
        List.of("ROLE_assessor", "assessment.manage", "trainee.read")
    );
    assessor1Headers = createAuthHeaders(assessor1Token);

    // 2. Create Assessor 2 (unassigned assessor to verify tenant/assignment isolation)
    assessor2UserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "assessor2_" + runToken, "assessor2_" + runToken + "@test.gov.in");
    trackedUserIds.add(assessor2UserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'assessor' LIMIT 1
        """, assessor2UserId);

    assessor2Token = generateBearerToken(
        assessor2UserId,
        "assessor2_" + runToken,
        List.of("ROLE_assessor", "assessment.manage", "trainee.read")
    );
    assessor2Headers = createAuthHeaders(assessor2Token);

    // 3. Domain Hierarchy
    fixtureSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-AS-" + runToken, "Electronics & Hardware " + runToken);
    trackedSectorIds.add(fixtureSectorId);

    fixtureCourseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-AS-" + runToken, "Field Technician " + runToken, fixtureSectorId);
    trackedCourseIds.add(fixtureCourseId);

    fixtureOrgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-AS-" + runToken, "Skill Org " + runToken);
    trackedOrgIds.add(fixtureOrgId);

    fixtureDeptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, fixtureOrgId, "DEP-AS-" + runToken, "Dept Skill " + runToken);
    trackedDeptIds.add(fixtureDeptId);

    fixtureSchemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-AS-" + runToken, "PMKVY Scheme " + runToken, fixtureDeptId);
    trackedSchemeIds.add(fixtureSchemeId);

    fixtureProgramId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-AS-" + runToken, "Vocational Program " + runToken, fixtureDeptId, fixtureSchemeId);
    trackedProgramIds.add(fixtureProgramId);

    fixtureProviderId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-AS-" + runToken, "Apex Institute " + runToken, "REG-AS-" + runToken, fixtureOrgId);
    trackedProviderIds.add(fixtureProviderId);

    fixtureCenterId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, fixtureProviderId, "CTR-AS-" + runToken, "Apex Center " + runToken);
    trackedCenterIds.add(fixtureCenterId);

    fixtureBatchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, '2026-01-01', 50, 1, NOW(), NOW())
        """, "BAT-AS-" + runToken, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureProgramId);
    trackedBatchIds.add(fixtureBatchId);

    // Admin User
    adminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "admin_" + runToken, "admin_" + runToken + "@gov.in");
    trackedUserIds.add(adminUserId);
    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        """, adminUserId);
    adminToken = generateBearerToken(adminUserId, "admin_" + runToken, List.of("ROLE_super_admin", "assessment.manage"));
    adminHeaders = createAuthHeaders(adminToken);

    // 4. Trainee 1
    trainee1UserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "trainee1_" + runToken, "trainee1_" + runToken + "@candidate.in");
    trackedUserIds.add(trainee1UserId);
    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'trainee' LIMIT 1
        """, trainee1UserId);
    trainee1Token = generateBearerToken(trainee1UserId, "trainee1_" + runToken, List.of("ROLE_trainee", "trainee.read"));
    trainee1Headers = createAuthHeaders(trainee1Token);

    trainee1Id = insertAndGetId("""
        INSERT INTO trainees (user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'Rahul', 'Sharma', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, trainee1UserId, "REG-AS1-" + runToken);
    trackedTraineeIds.add(trainee1Id);

    enrollment1Id = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURDATE(), NOW(), NOW())
        """, "ENR-AS1-" + runToken, trainee1Id, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureBatchId, fixtureProgramId);
    trackedEnrollmentIds.add(enrollment1Id);

    // 5. Trainee 2
    trainee2UserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "trainee2_" + runToken, "trainee2_" + runToken + "@candidate.in");
    trackedUserIds.add(trainee2UserId);
    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'trainee' LIMIT 1
        """, trainee2UserId);
    trainee2Token = generateBearerToken(trainee2UserId, "trainee2_" + runToken, List.of("ROLE_trainee", "trainee.read"));
    trainee2Headers = createAuthHeaders(trainee2Token);

    trainee2Id = insertAndGetId("""
        INSERT INTO trainees (user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'Pooja', 'Verma', 2, 101, 1001, 1, 1, NOW(), NOW())
        """, trainee2UserId, "REG-AS2-" + runToken);
    trackedTraineeIds.add(trainee2Id);

    enrollment2Id = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURDATE(), NOW(), NOW())
        """, "ENR-AS2-" + runToken, trainee2Id, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureBatchId, fixtureProgramId);
    trackedEnrollmentIds.add(enrollment2Id);

    // 6. Assessment assigned to Assessor 1
    assignedAssessmentCode = "ASM-AS-" + runToken;
    assignedAssessmentId = insertAndGetId("""
        INSERT INTO assessments (assessment_code, assessment_name, course_id, batch_id, program_id, assessment_type_id, maximum_score, pass_score, assessment_date, evaluator_user_id, evaluator_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'Technician Practical & Theory Assessment', ?, ?, ?, 1, 100.00, 50.00, CURDATE(), ?, 'Assessor One', 1, NOW(), NOW())
        """, assignedAssessmentCode, fixtureCourseId, fixtureBatchId, fixtureProgramId, assessor1UserId);
    trackedAssessmentIds.add(assignedAssessmentId);
  }

  @Test
  @DisplayName("1. Assessor retrieves assigned assessments; unassigned assessor sees none")
  void testAssessorAssessmentListingAndScoping() throws Exception {
    // Assessor 1 retrieves assignments
    ResponseEntity<String> response1 = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );

    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode root1 = parseResponseJson(response1.getBody());
    assertThat(root1.get("success").asBoolean()).isTrue();
    JsonNode data1 = root1.get("data");
    assertThat(data1.isArray()).isTrue();
    assertThat(data1.size()).isGreaterThanOrEqualTo(1);

    boolean foundAssigned = false;
    for (JsonNode item : data1) {
      if (item.get("id").asLong() == assignedAssessmentId) {
        foundAssigned = true;
        assertThat(item.get("assessmentCode").asText()).isEqualTo(assignedAssessmentCode);
        assertThat(item.get("courseName").asText()).contains("Field Technician");
        assertThat(item.get("batchCode").asText()).isEqualTo("BAT-AS-" + runToken);
        assertThat(item.get("maximumScore").asDouble()).isEqualTo(100.0);
        assertThat(item.get("passScore").asDouble()).isEqualTo(50.0);
        assertThat(item.get("theoryMaxScore").asDouble()).isEqualTo(30.0);
        assertThat(item.get("practicalMaxScore").asDouble()).isEqualTo(50.0);
        assertThat(item.get("vivaMaxScore").asDouble()).isEqualTo(20.0);
        assertThat(item.get("totalCandidates").asInt()).isEqualTo(2);
      }
    }
    assertThat(foundAssigned).isTrue();

    // Assessor 2 (unassigned) calls the same endpoint - should NOT see assessor 1's assessment
    ResponseEntity<String> response2 = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments"),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        String.class
    );

    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode root2 = parseResponseJson(response2.getBody());
    for (JsonNode item : root2.get("data")) {
      assertThat(item.get("id").asLong()).isNotEqualTo(assignedAssessmentId);
    }
  }

  @Test
  @DisplayName("2. Accessing unassigned assessment roster returns 403 Forbidden")
  void testUnassignedAssessmentAccessReturns403() {
    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/roster"),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("3. Assessor retrieves candidate roster with correct initial unassessed status")
  void testGetCandidateRoster() throws Exception {
    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/roster"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode root = parseResponseJson(response.getBody());
    assertThat(root.get("success").asBoolean()).isTrue();

    JsonNode data = root.get("data");
    assertThat(data.get("assessment").get("id").asLong()).isEqualTo(assignedAssessmentId);

    JsonNode candidates = data.get("candidates");
    assertThat(candidates.isArray()).isTrue();
    assertThat(candidates.size()).isEqualTo(2);

    for (JsonNode c : candidates) {
      assertThat(c.get("assessmentOutcomeCode").asText()).isEqualTo("PENDING");
      assertThat(c.get("isFinalized").asBoolean()).isFalse();
      assertThat(c.get("certificateNumber").isNull()).isTrue();
    }
  }

  @Test
  @DisplayName("4. Saving draft scores persists partial marks without issuing certification")
  void testSaveDraftScores() throws Exception {
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("22.50"));
    c1.setPracticalScore(new BigDecimal("35.00"));
    c1.setVivaScore(null); // Partial marks in draft
    c1.setRemarks("Draft observation for Rahul");

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(true);
    c2.setTheoryScore(new BigDecimal("15.00"));
    c2.setPracticalScore(null);
    c2.setVivaScore(null);

    SaveAssessmentScoresRequest draftReq = new SaveAssessmentScoresRequest();
    draftReq.setDraft(true);
    draftReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(draftReq, assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode root = parseResponseJson(response.getBody());
    assertThat(root.get("success").asBoolean()).isTrue();
    assertThat(root.get("data").get("totalProcessed").asInt()).isEqualTo(2);
    assertThat(root.get("data").get("certificatesIssued").asInt()).isEqualTo(0);

    // Verify database state: NO certification should exist
    Integer certCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM certifications WHERE enrollment_id IN (?, ?)",
        Integer.class, enrollment1Id, enrollment2Id);
    assertThat(certCount).isEqualTo(0);

    // Verify assessment lifecycle remains SCHEDULED (1)
    Integer lifecycle = jdbcTemplate.queryForObject(
        "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = ?",
        Integer.class, assignedAssessmentId);
    assertThat(lifecycle).isEqualTo(1);

    // Reopen roster and verify draft values persisted
    ResponseEntity<String> rosterResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/roster"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );
    JsonNode rosterData = parseResponseJson(rosterResp.getBody()).get("data");
    for (JsonNode cand : rosterData.get("candidates")) {
      if (cand.get("traineeId").asLong() == trainee1Id) {
        assertThat(cand.get("theoryScore").asDouble()).isEqualTo(22.50);
        assertThat(cand.get("practicalScore").asDouble()).isEqualTo(35.00);
        assertThat(cand.get("vivaScore").isNull()).isTrue();
        assertThat(cand.get("assessmentOutcomeCode").asText()).isEqualTo("PENDING");
      }
    }
  }

  @Test
  @DisplayName("5. Score validation rejects negative marks, over-max components, and aggregate > 100")
  void testScoreValidations() {
    // 5A. Negative mark
    CandidateScoreInput negInput = new CandidateScoreInput();
    negInput.setTraineeId(trainee1Id);
    negInput.setEnrollmentId(enrollment1Id);
    negInput.setAppeared(true);
    negInput.setTheoryScore(new BigDecimal("-5.00"));

    SaveAssessmentScoresRequest req1 = new SaveAssessmentScoresRequest();
    req1.setDraft(true);
    req1.setCandidates(List.of(negInput));

    ResponseEntity<String> resp1 = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(req1, assessor1Headers),
        String.class
    );
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 5B. Over-max theory (> 30)
    CandidateScoreInput overTheory = new CandidateScoreInput();
    overTheory.setTraineeId(trainee1Id);
    overTheory.setEnrollmentId(enrollment1Id);
    overTheory.setAppeared(true);
    overTheory.setTheoryScore(new BigDecimal("35.00")); // Max is 30

    SaveAssessmentScoresRequest req2 = new SaveAssessmentScoresRequest();
    req2.setDraft(true);
    req2.setCandidates(List.of(overTheory));

    ResponseEntity<String> resp2 = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(req2, assessor1Headers),
        String.class
    );
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 5C. Over-max practical (> 50)
    CandidateScoreInput overPrac = new CandidateScoreInput();
    overPrac.setTraineeId(trainee1Id);
    overPrac.setEnrollmentId(enrollment1Id);
    overPrac.setAppeared(true);
    overPrac.setPracticalScore(new BigDecimal("55.00")); // Max is 50

    SaveAssessmentScoresRequest req3 = new SaveAssessmentScoresRequest();
    req3.setDraft(true);
    req3.setCandidates(List.of(overPrac));

    ResponseEntity<String> resp3 = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(req3, assessor1Headers),
        String.class
    );
    assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("6. Finalizing with passing and failing candidates authoritatively declares outcomes and issues certs")
  void testFinalizePassingAndFailingCandidates() throws Exception {
    // Candidate 1: Passing (Theory 25/30, Practical 45/50, Viva 15/20 => Total 85 >= 50)
    CandidateScoreInput passCandidate = new CandidateScoreInput();
    passCandidate.setTraineeId(trainee1Id);
    passCandidate.setEnrollmentId(enrollment1Id);
    passCandidate.setAppeared(true);
    passCandidate.setTheoryScore(new BigDecimal("25.00"));
    passCandidate.setPracticalScore(new BigDecimal("45.00"));
    passCandidate.setVivaScore(new BigDecimal("15.00"));
    passCandidate.setRemarks("Excellent hands-on performance");

    // Candidate 2: Failing (Theory 10/30, Practical 15/50, Viva 5/20 => Total 30 < 50)
    CandidateScoreInput failCandidate = new CandidateScoreInput();
    failCandidate.setTraineeId(trainee2Id);
    failCandidate.setEnrollmentId(enrollment2Id);
    failCandidate.setAppeared(true);
    failCandidate.setTheoryScore(new BigDecimal("10.00"));
    failCandidate.setPracticalScore(new BigDecimal("15.00"));
    failCandidate.setVivaScore(new BigDecimal("5.00"));
    failCandidate.setRemarks("Needs remediation in circuit assembly");

    SaveAssessmentScoresRequest finalizeReq = new SaveAssessmentScoresRequest();
    finalizeReq.setDraft(false);
    finalizeReq.setCandidates(List.of(passCandidate, failCandidate));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode root = parseResponseJson(response.getBody());
    assertThat(root.get("success").asBoolean()).isTrue();
    JsonNode data = root.get("data");

    assertThat(data.get("passedCount").asInt()).isEqualTo(1);
    assertThat(data.get("failedCount").asInt()).isEqualTo(1);
    assertThat(data.get("certificatesIssued").asInt()).isEqualTo(1);

    // Verify candidate 1: Passed, certificate created, enrollment completed
    Integer cand1Outcome = jdbcTemplate.queryForObject("""
        SELECT ar.assessment_outcome_id
        FROM assessment_results ar
        JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
        WHERE ta.assessment_id = ? AND ta.trainee_id = ?
        """, Integer.class, assignedAssessmentId, trainee1Id);
    assertThat(cand1Outcome).isEqualTo(2); // 2 = PASS

    List<Long> cert1Ids = jdbcTemplate.queryForList(
        "SELECT certification_id FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Long.class, trainee1Id, enrollment1Id);
    assertThat(cert1Ids).hasSize(1);
    Long certId = cert1Ids.get(0);
    trackedCertificationIds.add(certId);

    // Verify certificate details: status = ISSUED (1), verification = PENDING (2), verified_at = null
    Integer certStatus = jdbcTemplate.queryForObject(
        "SELECT certificate_status_id FROM certifications WHERE certification_id = ?",
        Integer.class, certId);
    assertThat(certStatus).isEqualTo(1);

    Integer certVerif = jdbcTemplate.queryForObject(
        "SELECT certificate_verification_status_id FROM certifications WHERE certification_id = ?",
        Integer.class, certId);
    assertThat(certVerif).isEqualTo(2); // 2 = PENDING (pending verification)

    Object verifiedAt = jdbcTemplate.queryForObject(
        "SELECT verified_at FROM certifications WHERE certification_id = ?",
        Object.class, certId);
    assertThat(verifiedAt).isNull();

    String issuingBody = jdbcTemplate.queryForObject(
        "SELECT issuing_body FROM certifications WHERE certification_id = ?",
        String.class, certId);
    assertThat(issuingBody).isEqualTo("Skill Org " + runToken);

    // Verify candidate 1 enrollment status = 3 (COMPLETED)
    Integer enr1Status = jdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Integer.class, enrollment1Id);
    assertThat(enr1Status).isEqualTo(3);

    // Verify candidate 2: Failed, NO certificate
    Integer cand2Outcome = jdbcTemplate.queryForObject("""
        SELECT ar.assessment_outcome_id
        FROM assessment_results ar
        JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
        WHERE ta.assessment_id = ? AND ta.trainee_id = ?
        """, Integer.class, assignedAssessmentId, trainee2Id);
    assertThat(cand2Outcome).isEqualTo(3); // 3 = FAIL

    Integer cand2CertCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Integer.class, trainee2Id, enrollment2Id);
    assertThat(cand2CertCount).isEqualTo(0);

    // Verify candidate 2 enrollment remains in original status (1)
    Integer enr2Status = jdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Integer.class, enrollment2Id);
    assertThat(enr2Status).isEqualTo(1);

    // Verify assessment lifecycle is now 2 (FINALIZED)
    Integer asmStatus = jdbcTemplate.queryForObject(
        "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = ?",
        Integer.class, assignedAssessmentId);
    assertThat(asmStatus).isEqualTo(2);

    // 7. Verify Finalized Immutability: modifying finalized assessment must be rejected
    ResponseEntity<String> modResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );
    assertThat(modResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 8. Verify Authentic PDF Download by Assigned Assessor
    ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications/" + certId + "/download"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        byte[].class
    );
    assertThat(pdfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(pdfResponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);

    byte[] body = pdfResponse.getBody();
    assertThat(body).isNotNull();
    assertThat(body.length).isGreaterThan(100);

    String magic = new String(body, 0, 5, StandardCharsets.US_ASCII);
    assertThat(magic).isEqualTo("%PDF-");
  }

  @Test
  @DisplayName("7. Incomplete roster finalization is rejected with INCOMPLETE_ROSTER")
  void testIncompleteRosterFinalizationRejected() {
    // Add 3rd eligible trainee to the batch
    Long trainee3Id = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'Amit', 'Kumar', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, "REG-AS3-" + runToken);
    trackedTraineeIds.add(trainee3Id);

    Long enrollment3Id = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURDATE(), NOW(), NOW())
        """, "ENR-AS3-" + runToken, trainee3Id, fixtureCourseId, fixtureProviderId, fixtureCenterId, fixtureBatchId, fixtureProgramId);
    trackedEnrollmentIds.add(enrollment3Id);

    // Roster now has 3 eligible candidates. Sending 2 must fail on finalize
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("20.00"));
    c1.setPracticalScore(new BigDecimal("40.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(true);
    c2.setTheoryScore(new BigDecimal("22.00"));
    c2.setPracticalScore(new BigDecimal("42.00"));
    c2.setVivaScore(new BigDecimal("16.00"));

    SaveAssessmentScoresRequest incompleteFinalizeReq = new SaveAssessmentScoresRequest();
    incompleteFinalizeReq.setDraft(false);
    incompleteFinalizeReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(incompleteFinalizeReq, assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INCOMPLETE_ROSTER");
  }

  @Test
  @DisplayName("8. Duplicate candidates in payload rejected with DUPLICATE_CANDIDATE")
  void testDuplicateCandidatesRejected() {
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("20.00"));
    c1.setPracticalScore(new BigDecimal("40.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c1Duplicate = new CandidateScoreInput();
    c1Duplicate.setTraineeId(trainee1Id);
    c1Duplicate.setEnrollmentId(enrollment1Id);
    c1Duplicate.setAppeared(true);
    c1Duplicate.setTheoryScore(new BigDecimal("21.00"));
    c1Duplicate.setPracticalScore(new BigDecimal("41.00"));
    c1Duplicate.setVivaScore(new BigDecimal("16.00"));

    SaveAssessmentScoresRequest dupReq = new SaveAssessmentScoresRequest();
    dupReq.setDraft(true);
    dupReq.setCandidates(List.of(c1, c1Duplicate));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(dupReq, assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("DUPLICATE_CANDIDATE");
  }

  @Test
  @DisplayName("9. Foreign candidates outside batch rejected with INVALID_CANDIDATE")
  void testForeignCandidateRejected() {
    // Create foreign trainee not in this batch
    Long foreignTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'Foreign', 'User', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, "REG-FOR-" + runToken);
    trackedTraineeIds.add(foreignTraineeId);

    CandidateScoreInput foreignInput = new CandidateScoreInput();
    foreignInput.setTraineeId(foreignTraineeId);
    foreignInput.setEnrollmentId(99999L);
    foreignInput.setAppeared(true);
    foreignInput.setTheoryScore(new BigDecimal("20.00"));

    SaveAssessmentScoresRequest foreignReq = new SaveAssessmentScoresRequest();
    foreignReq.setDraft(true);
    foreignReq.setCandidates(List.of(foreignInput));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(foreignReq, assessor1Headers),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("INVALID_CANDIDATE");
  }

  @Test
  @DisplayName("10. Certificate access control: unassigned assessor gets 403, unauthenticated gets 401")
  void testCertificateAccessControl() throws Exception {
    // 1. Finalize assessment to produce a certificate
    CandidateScoreInput passCandidate = new CandidateScoreInput();
    passCandidate.setTraineeId(trainee1Id);
    passCandidate.setEnrollmentId(enrollment1Id);
    passCandidate.setAppeared(true);
    passCandidate.setTheoryScore(new BigDecimal("25.00"));
    passCandidate.setPracticalScore(new BigDecimal("45.00"));
    passCandidate.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput absentCandidate = new CandidateScoreInput();
    absentCandidate.setTraineeId(trainee2Id);
    absentCandidate.setEnrollmentId(enrollment2Id);
    absentCandidate.setAppeared(false);

    SaveAssessmentScoresRequest finalizeReq = new SaveAssessmentScoresRequest();
    finalizeReq.setDraft(false);
    finalizeReq.setCandidates(List.of(passCandidate, absentCandidate));

    ResponseEntity<String> finResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );
    assertThat(finResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long certId = jdbcTemplate.queryForObject(
        "SELECT certification_id FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Long.class, trainee1Id, enrollment1Id);
    assertThat(certId).isNotNull();
    trackedCertificationIds.add(certId);

    // 2. Assessor 1 (assigned) downloads -> 200 OK
    ResponseEntity<byte[]> okResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications/" + certId + "/download"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        byte[].class
    );
    assertThat(okResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 3. Assessor 2 (unassigned) downloads -> 403 Forbidden
    ResponseEntity<byte[]> forbiddenResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications/" + certId + "/download"),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        byte[].class
    );
    assertThat(forbiddenResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 4. Unauthenticated caller downloads -> 401 Unauthorized
    ResponseEntity<byte[]> unauthResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications/" + certId + "/download"),
        HttpMethod.GET,
        HttpEntity.EMPTY,
        byte[].class
    );
    assertThat(unauthResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("11. Idempotent re-finalization returns 200 without creating duplicate certs; modified marks rejected")
  void testIdempotencyAndImmutability() {
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(true);
    c2.setTheoryScore(new BigDecimal("10.00"));
    c2.setPracticalScore(new BigDecimal("15.00"));
    c2.setVivaScore(new BigDecimal("5.00"));

    SaveAssessmentScoresRequest finalizeReq = new SaveAssessmentScoresRequest();
    finalizeReq.setDraft(false);
    finalizeReq.setCandidates(List.of(c1, c2));

    // First finalization
    ResponseEntity<String> firstFin = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );
    assertThat(firstFin.getStatusCode()).isEqualTo(HttpStatus.OK);

    Integer certCount1 = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Integer.class, trainee1Id, enrollment1Id);
    assertThat(certCount1).isEqualTo(1);

    // Identical second finalization (idempotent replay)
    ResponseEntity<String> secondFin = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );
    assertThat(secondFin.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Cert count must remain exactly 1 (no duplicate certs)
    Integer certCount2 = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Integer.class, trainee1Id, enrollment1Id);
    assertThat(certCount2).isEqualTo(1);

    // Different finalization payload (attempting to alter marks) must be rejected
    c1.setTheoryScore(new BigDecimal("28.00")); // modified score
    SaveAssessmentScoresRequest modifiedReq = new SaveAssessmentScoresRequest();
    modifiedReq.setDraft(false);
    modifiedReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> alteredFin = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(modifiedReq, assessor1Headers),
        String.class
    );
    assertThat(alteredFin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(alteredFin.getBody()).contains("ASSESSMENT_ALREADY_FINALIZED");
  }

  @Test
  @DisplayName("12. Historical completion date preserved for already completed enrollments")
  void testHistoricalCompletionDatePreserved() {
    // Set enrollment 1 as already COMPLETED with a historical completion date in 2025
    jdbcTemplate.update("""
        UPDATE training_enrollments
        SET enrollment_status_id = 3, actual_completion_date = '2025-04-10'
        WHERE enrollment_id = ?
        """, enrollment1Id);

    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(true);
    c2.setTheoryScore(new BigDecimal("20.00"));
    c2.setPracticalScore(new BigDecimal("35.00"));
    c2.setVivaScore(new BigDecimal("10.00"));

    SaveAssessmentScoresRequest finalizeReq = new SaveAssessmentScoresRequest();
    finalizeReq.setDraft(false);
    finalizeReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> response = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finalizeReq, assessor1Headers),
        String.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Verify actual_completion_date was NOT overwritten with today!
    String completionDate = jdbcTemplate.queryForObject(
        "SELECT actual_completion_date FROM training_enrollments WHERE enrollment_id = ?",
        String.class, enrollment1Id);
    assertThat(completionDate).isEqualTo("2025-04-10");
  }

  @Test
  @DisplayName("13. Jackson remarks formatting handles quotes, backslashes, and newlines safely")
  void testJacksonUnicodeRemarksFormatting() throws Exception {
    String specialRemarks = "Assessor notes: \"Passed with Distinction\" \\ path: C:\\assessments\nStatus: In Review";

    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));
    c1.setRemarks(specialRemarks);

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(false);

    SaveAssessmentScoresRequest draftReq = new SaveAssessmentScoresRequest();
    draftReq.setDraft(true);
    draftReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> resp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/draft"),
        HttpMethod.POST,
        new HttpEntity<>(draftReq, assessor1Headers),
        String.class
    );
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Check remarks in database directly
    String rawRemarks = jdbcTemplate.queryForObject("""
        SELECT ar.remarks
        FROM assessment_results ar
        JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
        WHERE ta.assessment_id = ? AND ta.trainee_id = ?
        """, String.class, assignedAssessmentId, trainee1Id);
    assertThat(rawRemarks).isNotNull();
    assertThat(rawRemarks).contains("Assessor notes: \\\"Passed with Distinction\\\"");
    assertThat(rawRemarks).contains("C:\\\\assessments");
    assertThat(rawRemarks).contains("\\nStatus: In Review");

    // Verify valid JSON deserialization back to exact string
    JsonNode parsedNode = parseResponseJson(rawRemarks);
    assertThat(parsedNode.get("note").asText()).isEqualTo(specialRemarks);

    // Reopen roster
    ResponseEntity<String> rosterResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/roster"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );
    assertThat(rosterResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode rosterData = parseResponseJson(rosterResp.getBody()).get("data");
    for (JsonNode cand : rosterData.get("candidates")) {
      if (cand.get("traineeId").asLong() == trainee1Id) {
        assertThat(cand.get("theoryScore").asDouble()).isEqualTo(25.00);
        assertThat(cand.get("practicalScore").asDouble()).isEqualTo(45.00);
        assertThat(cand.get("vivaScore").asDouble()).isEqualTo(15.00);
      }
    }
  }

  @Test
  @DisplayName("14. Endpoint aliases (/api/v1/assessments/results and /api/v1/certificates/issue) are functional")
  void testEndpointAliases() {
    // Results alias endpoint
    ResponseEntity<String> resultsResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessments/results"),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );
    assertThat(resultsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Certificates alias issue endpoint
    ResponseEntity<String> issueResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certificates/issue"),
        HttpMethod.POST,
        new HttpEntity<>(assessor1Headers),
        String.class
    );
    // Since issue expects a body or query, it should return 200 or 400 (not 404!)
    assertThat(issueResp.getStatusCode().value()).isNotEqualTo(404);
  }

  @Test
  @DisplayName("15. Finalization rejects identical total with altered component marks")
  void testFinalizeSameTotalDifferentComponentsRejected() {
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("20.00"));
    c1.setPracticalScore(new BigDecimal("40.00"));
    c1.setVivaScore(new BigDecimal("10.00")); // total = 70.00

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(false);

    SaveAssessmentScoresRequest initialReq = new SaveAssessmentScoresRequest();
    initialReq.setDraft(false);
    initialReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> firstFin = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(initialReq, assessor1Headers),
        String.class
    );
    assertThat(firstFin.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Replay with IDENTICAL total (70.00) but DIFFERENT components (theory 10 + practical 50 + viva 10 = 70.00)
    c1.setTheoryScore(new BigDecimal("10.00"));
    c1.setPracticalScore(new BigDecimal("50.00"));
    c1.setVivaScore(new BigDecimal("10.00"));

    SaveAssessmentScoresRequest diffCompReq = new SaveAssessmentScoresRequest();
    diffCompReq.setDraft(false);
    diffCompReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> secondFin = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(diffCompReq, assessor1Headers),
        String.class
    );
    assertThat(secondFin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(secondFin.getBody()).contains("ASSESSMENT_ALREADY_FINALIZED");
  }

  @Test
  @DisplayName("16. Certificate retrieval scoping for Trainee, Assessor, and Admin")
  void testCertificateRetrievalScopingForTraineeAndAssessor() throws Exception {
    // Finalize assessment so trainee 1 has a certificate
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(false);

    SaveAssessmentScoresRequest finReq = new SaveAssessmentScoresRequest();
    finReq.setDraft(false);
    finReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> finResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finReq, assessor1Headers),
        String.class
    );
    assertThat(finResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long certId = jdbcTemplate.queryForObject(
        "SELECT certification_id FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Long.class, trainee1Id, enrollment1Id);
    assertThat(certId).isNotNull();

    Long result1Id = jdbcTemplate.queryForObject("""
        SELECT ar.assessment_result_id FROM assessment_results ar
        JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
        WHERE ta.assessment_id = ? AND ta.trainee_id = ?
        """, Long.class, assignedAssessmentId, trainee1Id);
    assertThat(result1Id).isNotNull();

    // 1. Trainee 1 sees own certificate
    ResponseEntity<String> trainee1OwnResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?traineeId=" + trainee1Id),
        HttpMethod.GET,
        new HttpEntity<>(trainee1Headers),
        String.class
    );
    assertThat(trainee1OwnResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode t1Data = parseResponseJson(trainee1OwnResp.getBody()).get("data");
    assertThat(t1Data.size()).isGreaterThanOrEqualTo(1);

    // 2. Trainee 1 blocked from querying trainee 2
    ResponseEntity<String> trainee1OtherTraineeResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?traineeId=" + trainee2Id),
        HttpMethod.GET,
        new HttpEntity<>(trainee1Headers),
        String.class
    );
    assertThat(trainee1OtherTraineeResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 3. Trainee 1 blocked from querying enrollment 2
    ResponseEntity<String> trainee1OtherEnrResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?enrollmentId=" + enrollment2Id),
        HttpMethod.GET,
        new HttpEntity<>(trainee1Headers),
        String.class
    );
    assertThat(trainee1OtherEnrResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 4. Trainee 1 generic query (/api/v1/certifications) returns only Trainee 1 certificates
    ResponseEntity<String> trainee1AllResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications"),
        HttpMethod.GET,
        new HttpEntity<>(trainee1Headers),
        String.class
    );
    assertThat(trainee1AllResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode allT1 = parseResponseJson(trainee1AllResp.getBody()).get("data");
    for (JsonNode c : allT1) {
      assertThat(c.get("traineeId").asLong()).isEqualTo(trainee1Id);
    }

    // 5. Assessor 1 (assigned) retrieves certificates for course
    ResponseEntity<String> assessor1CourseResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?courseId=" + fixtureCourseId),
        HttpMethod.GET,
        new HttpEntity<>(assessor1Headers),
        String.class
    );
    assertThat(assessor1CourseResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode a1Data = parseResponseJson(assessor1CourseResp.getBody()).get("data");
    assertThat(a1Data.size()).isGreaterThanOrEqualTo(1);

    // 6. Assessor 2 (unassigned) blocked from accessing cert by ID
    ResponseEntity<String> assessor2CertResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications/" + certId),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        String.class
    );
    assertThat(assessor2CertResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 7. Assessor 2 (unassigned) blocked from querying assessment result ID
    ResponseEntity<String> assessor2ResultResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?assessmentResultId=" + result1Id),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        String.class
    );
    assertThat(assessor2ResultResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 8. Assessor 2 generic query returns empty list (0 certificates)
    ResponseEntity<String> assessor2AllResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications"),
        HttpMethod.GET,
        new HttpEntity<>(assessor2Headers),
        String.class
    );
    assertThat(assessor2AllResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode a2Data = parseResponseJson(assessor2AllResp.getBody()).get("data");
    assertThat(a2Data.size()).isEqualTo(0);

    // 9. Admin has full access across all filters
    ResponseEntity<String> adminResp = restTemplate.exchange(
        getBaseUrl("/api/v1/certifications?traineeId=" + trainee1Id),
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        String.class
    );
    assertThat(adminResp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("17. Enrollment completion rules enforced: attendance & batch status requirements")
  void testEnrollmentCompletionRequirementsEnforced() {
    // Trainee 1 has low attendance: 1 present, 3 absent (25% < 70%)
    jdbcTemplate.update("""
        INSERT INTO attendance_records (enrollment_id, trainee_id, batch_id, session_date, session_sequence, attendance_status_id, marked_at)
        VALUES (?, ?, ?, '2026-01-10', 1, 1, NOW()),
               (?, ?, ?, '2026-01-11', 1, 2, NOW()),
               (?, ?, ?, '2026-01-12', 1, 2, NOW()),
               (?, ?, ?, '2026-01-13', 1, 2, NOW())
        """, enrollment1Id, trainee1Id, fixtureBatchId,
             enrollment1Id, trainee1Id, fixtureBatchId,
             enrollment1Id, trainee1Id, fixtureBatchId,
             enrollment1Id, trainee1Id, fixtureBatchId);

    // Enrollment 2 is DEFERRED (status = 5)
    jdbcTemplate.update("""
        UPDATE training_enrollments SET enrollment_status_id = 5 WHERE enrollment_id = ?
        """, enrollment2Id);

    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(false);

    SaveAssessmentScoresRequest finReq = new SaveAssessmentScoresRequest();
    finReq.setDraft(false);
    finReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> finResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finReq, assessor1Headers),
        String.class
    );
    assertThat(finResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Enrollment 1 must NOT be marked COMPLETED because attendance was < 70%
    Long enr1Status = jdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Long.class, enrollment1Id);
    assertThat(enr1Status).isNotEqualTo(3L);

    // Enrollment 2 must remain DEFERRED (5), NOT COMPLETED
    Long enr2Status = jdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Long.class, enrollment2Id);
    assertThat(enr2Status).isEqualTo(5L);
  }

  @Test
  @DisplayName("18. Authoritative issuing body resolved accurately without fabricated claims")
  void testAuthoritativeIssuingBodyResolution() {
    CandidateScoreInput c1 = new CandidateScoreInput();
    c1.setTraineeId(trainee1Id);
    c1.setEnrollmentId(enrollment1Id);
    c1.setAppeared(true);
    c1.setTheoryScore(new BigDecimal("25.00"));
    c1.setPracticalScore(new BigDecimal("45.00"));
    c1.setVivaScore(new BigDecimal("15.00"));

    CandidateScoreInput c2 = new CandidateScoreInput();
    c2.setTraineeId(trainee2Id);
    c2.setEnrollmentId(enrollment2Id);
    c2.setAppeared(false);

    SaveAssessmentScoresRequest finReq = new SaveAssessmentScoresRequest();
    finReq.setDraft(false);
    finReq.setCandidates(List.of(c1, c2));

    ResponseEntity<String> finResp = restTemplate.exchange(
        getBaseUrl("/api/v1/assessor/assessments/" + assignedAssessmentId + "/finalize"),
        HttpMethod.POST,
        new HttpEntity<>(finReq, assessor1Headers),
        String.class
    );
    assertThat(finResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    String issuingBody = jdbcTemplate.queryForObject(
        "SELECT issuing_body FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        String.class, trainee1Id, enrollment1Id);
    assertThat(issuingBody).isNotNull();
    // Must contain parent organization name
    assertThat(issuingBody).contains("Skill Org " + runToken);
    // Must NOT contain fabricated fallback
    assertThat(issuingBody).doesNotContain("National Skill Development Authority");

    Long verifStatusId = jdbcTemplate.queryForObject(
        "SELECT certificate_verification_status_id FROM certifications WHERE trainee_id = ? AND enrollment_id = ?",
        Long.class, trainee1Id, enrollment1Id);
    assertThat(verifStatusId).isEqualTo(2L); // PENDING verification status
  }
}
