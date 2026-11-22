package in.gov.sih.sih26135.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Phase 30.5 — Training Provider Data Quality Remediation & Center Scorecards Integration Test")
public class ProviderDataQualityIntegrationTest extends BaseJourneyIntegrationTest {

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  private Long providerAId;
  private Long providerBId;
  private Long centerAId;
  private Long centerBId;
  private Long batchAId;
  private Long traineeId;
  private Long enrollmentAId;
  private Long issueAId;
  private Long issueBId;
  private Long centerIssueAId;
  private Long uncorrectedIssueId;
  private Long privilegedIssueId;
  private Long invalidCenterIssueId;
  private Long unknownRuleIssueId;

  private String providerAToken;
  private HttpHeaders providerAHeaders;
  private String providerBToken;
  private HttpHeaders providerBHeaders;
  private String traineeToken;
  private HttpHeaders traineeHeaders;

  @BeforeEach
  void setUpFixtures() {
    String runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    catalogService.ensureDefaultRulesProvisioned();

    // 1. Organizations
    Long orgAId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-A-" + runToken, "Training Org A " + runToken);
    trackedOrgIds.add(orgAId);

    Long orgBId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-B-" + runToken, "Training Org B " + runToken);
    trackedOrgIds.add(orgBId);

    // 2. Training Providers
    providerAId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_id, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 5, 101, 1001, 1, 1, NOW(), NOW())
        """, "TP-A-" + runToken, "Provider A " + runToken, "REG-A-" + runToken, orgAId);
    trackedProviderIds.add(providerAId);

    providerBId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_id, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 5, 101, 1001, 1, 1, NOW(), NOW())
        """, "TP-B-" + runToken, "Provider B " + runToken, "REG-B-" + runToken, orgBId);
    trackedProviderIds.add(providerBId);

    // 3. Training Centers
    centerAId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, providerAId, "TC-A-" + runToken, "Center A " + runToken);
    trackedCenterIds.add(centerAId);

    centerBId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, providerBId, "TC-B-" + runToken, "Center B " + runToken);
    trackedCenterIds.add(centerBId);

    // 4. Sector & Course
    Long sectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-" + runToken, "Sector " + runToken);
    trackedSectorIds.add(sectorId);

    Long courseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-" + runToken, "Course " + runToken, sectorId);
    trackedCourseIds.add(courseId);

    // 5. Training Batches
    batchAId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, '2026-01-01', 30, 1, NOW(), NOW())
        """, "BT-A-" + runToken, courseId, providerAId, centerAId);
    trackedBatchIds.add(batchAId);

    // 6. Trainee (phone and email are initially NULL to model DQ-TRAINEE-001 anomaly)
    traineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, date_of_birth, state_id, district_id, phone, email, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'John', 'Doe', 1, '1995-05-15', 101, 1001, NULL, NULL, 1, 1, NOW(), NOW())
        """, "REG-TR-" + runToken);
    trackedTraineeIds.add(traineeId);

    // 7. Enrollment for Provider A
    enrollmentAId = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, start_date, expected_completion_date, enrollment_status_id, created_at, updated_at)
        VALUES (?, ?, 1, ?, ?, ?, ?, '2026-01-02', '2026-01-02', '2026-04-01', 1, NOW(), NOW())
        """, "ENR-A-" + runToken, traineeId, courseId, providerAId, centerAId, batchAId);
    trackedEnrollmentIds.add(enrollmentAId);

    // 8. Data Quality Issues
    Long contactRuleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-TRAINEE-001' LIMIT 1",
        Long.class);

    Long duplicateRuleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-DUPLICATE-005' LIMIT 1",
        Long.class);

    Long attendanceRuleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-ATTENDANCE-004' LIMIT 1",
        Long.class);

    Long attendanceAId = insertAndGetId("""
        INSERT INTO attendance_records (enrollment_id, trainee_id, batch_id, session_date, attendance_status_id, marked_at, created_at, updated_at)
        VALUES (?, ?, ?, '2026-01-10', 1, NOW(), NOW(), NOW())
        """, enrollmentAId, traineeId, batchAId);
    trackedAttendanceIds.add(attendanceAId);

    issueAId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'AttendanceRecord', ?, 'Anomaly detected in Provider A attendance', NOW(), NOW(), NOW())
        """, attendanceRuleId, attendanceAId);
    trackedDqIssueIds.add(issueAId);

    // Uncorrected issue on Trainee missing contact details
    uncorrectedIssueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'Trainee', ?, 'Missing contact information: both phone and email are missing', NOW(), NOW(), NOW())
        """, contactRuleId, traineeId);
    trackedDqIssueIds.add(uncorrectedIssueId);

    // Privileged issue: cross-provider duplicate candidate registration
    privilegedIssueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 1, 4, 1, 1, 'Trainee', ?, 'Duplicate trainee registration detected across multiple providers', NOW(), NOW(), NOW())
        """, duplicateRuleId, traineeId);
    trackedDqIssueIds.add(privilegedIssueId);

    centerIssueAId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Anomaly detected in Provider A center', NOW(), NOW(), NOW())
        """, contactRuleId, centerAId);
    trackedDqIssueIds.add(centerIssueAId);

    issueBId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Anomaly detected in Provider B center', NOW(), NOW(), NOW())
        """, contactRuleId, centerBId);
    trackedDqIssueIds.add(issueBId);

    Long centerRuleId = jdbcTemplate.query(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-CENTER-001'",
        (rs, rowNum) -> rs.getLong(1)
    ).stream().findFirst().orElseGet(() -> {
      return insertAndGetId("""
          INSERT INTO data_quality_rules (rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at)
          VALUES ('DQ-CENTER-001', 'Training Center Profile and Capacity Validity', 'Validates training center profile', 'TrainingCenter', 2, 3, 1, NOW(), NOW())
          """);
    });

    // Issue for an invalid training center (blank name)
    Long invalidCenterId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, operational_capacity, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, '   ', 10, 101, 1001, 1, NOW(), NOW())
        """, providerAId, "TC-INV-" + runToken);
    trackedCenterIds.add(invalidCenterId);

    invalidCenterIssueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Center profile has blank center_name', NOW(), NOW(), NOW())
        """, centerRuleId, invalidCenterId);
    trackedDqIssueIds.add(invalidCenterIssueId);

    // Issue with null/unknown rule code on an otherwise valid trainee
    unknownRuleIssueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (NULL, 2, 3, 1, 1, 'Trainee', ?, 'Issue missing rule code specification', NOW(), NOW(), NOW())
        """, traineeId);
    trackedDqIssueIds.add(unknownRuleIssueId);

    // Users and Tokens
    Long userAId = insertAndGetId("""
        INSERT INTO users (username, password_hash, password_algo, email, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'argon2id', ?, 2, ?, NOW(), NOW())
        """, "user_tp_a_" + runToken, "user_tp_a_" + runToken + "@example.com", providerAId);
    trackedUserIds.add(userAId);

    Long userBId = insertAndGetId("""
        INSERT INTO users (username, password_hash, password_algo, email, user_status_id, training_provider_id, created_at, updated_at)
        VALUES (?, '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'argon2id', ?, 2, ?, NOW(), NOW())
        """, "user_tp_b_" + runToken, "user_tp_b_" + runToken + "@example.com", providerBId);
    trackedUserIds.add(userBId);

    Long userTraineeId = insertAndGetId("""
        INSERT INTO users (username, password_hash, password_algo, email, user_status_id, created_at, updated_at)
        VALUES (?, '$2a$10$abcdefghijklmnopqrstuvwxyz123456', 'argon2id', ?, 2, NOW(), NOW())
        """, "user_trainee_" + runToken, "user_trainee_" + runToken + "@example.com");
    trackedUserIds.add(userTraineeId);
    jdbcTemplate.update("UPDATE trainees SET user_id = ? WHERE trainee_id = ?", userTraineeId, traineeId);

    providerAToken = generateBearerToken(userAId, "user_tp_a_" + runToken, List.of("ROLE_training_provider", "center.manage", "enrollment.manage"));
    providerAHeaders = createAuthHeaders(providerAToken);

    providerBToken = generateBearerToken(userBId, "user_tp_b_" + runToken, List.of("ROLE_training_provider", "center.manage", "enrollment.manage"));
    providerBHeaders = createAuthHeaders(providerBToken);

    traineeToken = generateBearerToken(userTraineeId, "user_trainee_" + runToken, List.of("ROLE_trainee"));
    traineeHeaders = createAuthHeaders(traineeToken);
  }

  @Test
  @DisplayName("Scenario 1 & 25: Unauthenticated request is rejected with 401")
  void unauthenticatedRequest_returns401() {
    ResponseEntity<String> response = restTemplate.getForEntity(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues",
        String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Scenario 2 & 25: Trainee caller without provider authority is rejected with 403")
  void traineeAccount_returns403() {
    HttpEntity<Void> entity = new HttpEntity<>(traineeHeaders);
    ResponseEntity<String> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues",
        HttpMethod.GET,
        entity,
        String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Scenario 3 & 10: Provider lists only own issues including TrainingCenter issues, never cross-provider issues")
  void providerListsOwnIssues_tenancyEnforced() {
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues?activeOnly=true",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = response.getBody();
    assertThat(body).isNotNull();
    JsonNode content = body.path("data").path("content");
    assertThat(content.isArray()).isTrue();

    // Verify issueA and centerIssueA are present and issueB is strictly absent
    List<Long> returnedIssueIds = new ArrayList<>();
    content.forEach(node -> returnedIssueIds.add(node.path("id").asLong()));
    assertThat(returnedIssueIds).contains(issueAId, centerIssueAId);
    assertThat(returnedIssueIds).doesNotContain(issueBId);
  }

  @Test
  @DisplayName("Scenario 8, 11 & 12: Provider cannot inspect or resolve issues belonging to another provider")
  void crossProviderIssueAccess_rejected() {
    // 1. Provider A requesting Provider B's issue detail -> 404/403
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<String> getResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueBId,
        HttpMethod.GET,
        entity,
        String.class);
    assertThat(getResponse.getStatusCode().value()).isIn(403, 404);

    // 2. Provider A resolving Provider B's issue -> 403/404
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Attempted cross-provider tampering"),
        providerAHeaders);
    ResponseEntity<String> resolveResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueBId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        String.class);
    assertThat(resolveResponse.getStatusCode().value()).isIn(403, 404);
  }

  @Test
  @DisplayName("Requirement 2: Unchanged invalid record CANNOT be marked RESOLVED merely by supplying resolution notes")
  void unchangedInvalidRecord_resolveRejected() {
    // traineeId still has phone = null and email = null
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Attempting resolution without updating trainee contact details"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + uncorrectedIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    // MUST be rejected with 400 Bad Request
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.path("code").asText()).isEqualTo("REMEDIATION_VALIDATION_FAILED");

    // Correcting the record in the database without calling /resolve does NOT automatically resolve it
    jdbcTemplate.update(
        "UPDATE trainees SET phone = '9876543210' WHERE trainee_id = ?",
        traineeId);

    // Verify issue is STILL open in the database
    Integer statusId = jdbcTemplate.queryForObject(
        "SELECT data_quality_issue_status_id FROM data_quality_issues WHERE data_quality_issue_id = ?",
        Integer.class, uncorrectedIssueId);
    assertThat(statusId).isEqualTo(1); // OPEN

    // Now call /resolve after actual correction: MUST succeed
    ResponseEntity<JsonNode> successResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + uncorrectedIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(successResponse.getBody().path("data").path("statusCode").asText()).isEqualTo("RESOLVED");
  }

  @Test
  @DisplayName("Requirement 2: Privileged cross-provider duplicate issue cannot be unilaterally resolved by training provider")
  void privilegedIssueResolution_rejected() {
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Provider trying to unilaterally resolve duplicate trainee"),
        providerAHeaders);

    ResponseEntity<String> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + privilegedIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Requirement 1: Cross-provider re-evaluation rejection and provider-scoped scan")
  void providerScopedReEvaluation_tenancyEnforced() {
    // 1. Unauthenticated scan -> 401
    ResponseEntity<String> unauthResponse = restTemplate.postForEntity(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        null,
        String.class);
    assertThat(unauthResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // 2. Trainee scan -> 403
    HttpEntity<Void> traineeEntity = new HttpEntity<>(traineeHeaders);
    ResponseEntity<String> traineeResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        HttpMethod.POST,
        traineeEntity,
        String.class);
    assertThat(traineeResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 3. Provider A requesting scan on Provider B's issue -> 403/404
    HttpEntity<Void> providerEntity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<String> crossResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate?issueId=" + issueBId,
        HttpMethod.POST,
        providerEntity,
        String.class);
    assertThat(crossResponse.getStatusCode().value()).isIn(403, 404);

    // 4. Provider A requesting scan on own supported issue -> 200 OK
    ResponseEntity<JsonNode> validResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate?issueId=" + uncorrectedIssueId,
        HttpMethod.POST,
        providerEntity,
        JsonNode.class);
    assertThat(validResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 5. Provider A requesting scan on unsupported entity type (centerIssueAId) -> 400 Bad Request
    ResponseEntity<String> unsupportedResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate?issueId=" + centerIssueAId,
        HttpMethod.POST,
        providerEntity,
        String.class);
    assertThat(unsupportedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // 6. Provider A requesting overall provider re-evaluation without issueId -> 200 OK
    ResponseEntity<JsonNode> overallResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        HttpMethod.POST,
        providerEntity,
        JsonNode.class);
    assertThat(overallResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Requirement 4: Database-side event pagination across multiple pages and bounds enforcement")
  void databaseSideEventPagination_multiPage() {
    // Insert 3 additional events for issueAId
    jdbcTemplate.update("""
        INSERT INTO data_quality_issue_events (data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, changed_at, remarks, created_at)
        VALUES (?, 1, 1, '2026-01-01 10:00:00', 'Event 1 - Initial detection', '2026-01-01 10:00:00')
        """, issueAId);
    jdbcTemplate.update("""
        INSERT INTO data_quality_issue_events (data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, changed_at, remarks, created_at)
        VALUES (?, 3, 1, '2026-01-02 11:00:00', 'Event 2 - Investigation started', '2026-01-02 11:00:00')
        """, issueAId);
    jdbcTemplate.update("""
        INSERT INTO data_quality_issue_events (data_quality_issue_id, data_quality_issue_status_id, changed_by_user_id, changed_at, remarks, created_at)
        VALUES (?, 4, 1, '2026-01-03 12:00:00', 'Event 3 - Verification complete', '2026-01-03 12:00:00')
        """, issueAId);

    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);

    // Page 0, size 2: returns exactly 2 events
    ResponseEntity<JsonNode> page0 = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/events?page=0&size=2",
        HttpMethod.GET,
        entity,
        JsonNode.class);
    assertThat(page0.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(page0.getBody().path("data").size()).isEqualTo(2);

    // Page 1, size 2: returns at least 1 event
    ResponseEntity<JsonNode> page1 = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/events?page=1&size=2",
        HttpMethod.GET,
        entity,
        JsonNode.class);
    assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(page1.getBody().path("data").size()).isGreaterThanOrEqualTo(1);

    // Page 10, size 2: returns 0 events (empty page)
    ResponseEntity<JsonNode> emptyPage = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/events?page=10&size=2",
        HttpMethod.GET,
        entity,
        JsonNode.class);
    assertThat(emptyPage.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(emptyPage.getBody().path("data").size()).isEqualTo(0);

    // Invalid size > 100: returns 400 Bad Request
    ResponseEntity<String> invalidSize = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/events?page=0&size=101",
        HttpMethod.GET,
        entity,
        String.class);
    assertThat(invalidSize.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Scenario 13 & 14 & Requirement 5: Center cleanliness scorecard and completion/dropout rate computation")
  void centerScorecardMetrics() {
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/centers/" + centerAId + "/scorecard",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = response.getBody();
    assertThat(body).isNotNull();
    JsonNode scorecard = body.path("data");
    assertThat(scorecard).isNotNull();
    assertThat(scorecard.path("centerId").asLong()).isEqualTo(centerAId);
    assertThat(scorecard.path("cleanlinessFormula").asText()).contains("eligiblePopulation");
    // Eligible population includes Center (1) + Batch (1) + Enrollments (1) + Trainees (1) = at least 4
    assertThat(scorecard.path("eligiblePopulation").asInt()).isGreaterThanOrEqualTo(4);
    assertThat(scorecard.path("distinctAffectedEntities").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(scorecard.path("cleanlinessIndexPct").asDouble()).isBetween(0.0, 100.0);
    // Completion and dropout rates are computed against totalEnrollments
    assertThat(scorecard.path("totalEnrollments").asInt()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("Scenario 18 & 19: Provider resolves issue with mandatory notes, issue leaves active queue")
  void resolveIssueWorkflow() {
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Record dates reconciled and validated against attendance roster."),
        providerAHeaders);

    ResponseEntity<JsonNode> resolveResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(resolveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode resolveBody = resolveResponse.getBody();
    assertThat(resolveBody).isNotNull();
    JsonNode updated = resolveBody.path("data");
    assertThat(updated).isNotNull();
    assertThat(updated.path("statusCode").asText()).isEqualTo("RESOLVED");
    assertThat(updated.path("isOpen").asBoolean()).isFalse();

    // Verify it disappears from Active Queue
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> queueResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues?activeOnly=true",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    List<Long> activeIds = new ArrayList<>();
    queueResponse.getBody().path("data").path("content").forEach(n -> activeIds.add(n.path("id").asLong()));
    assertThat(activeIds).doesNotContain(issueAId);

    // Verify it remains visible in Historical/All records
    ResponseEntity<JsonNode> allResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues?activeOnly=false",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    List<Long> allIds = new ArrayList<>();
    allResponse.getBody().path("data").path("content").forEach(n -> allIds.add(n.path("id").asLong()));
    assertThat(allIds).contains(issueAId);
  }

  @Test
  @DisplayName("Scenario 23: Blank resolution notes rejected with 400 Bad Request")
  void blankResolutionNotes_rejected() {
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "   "),
        providerAHeaders);

    ResponseEntity<String> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Requirement 3: Still-invalid TrainingCenter issue cannot be resolved (rejected with 400)")
  void stillInvalidTrainingCenterIssue_cannotBeResolved() {
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Attempting resolution while training center name is still blank"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + invalidCenterIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("code").asText()).isEqualTo("REMEDIATION_VALIDATION_FAILED");
  }

  @Test
  @DisplayName("Requirement 3: An unrelated field correction cannot resolve a different anomaly")
  void unrelatedFieldCorrection_cannotResolveAnomaly() {
    // Trainee currently has phone = NULL and email = NULL (DQ-TRAINEE-001)
    // Update unrelated field (last_name) while keeping phone and email NULL
    jdbcTemplate.update("UPDATE trainees SET last_name = 'UnrelatedLastNameChange' WHERE trainee_id = ?", traineeId);

    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Updated last name hoping to resolve missing contact anomaly"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + uncorrectedIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("code").asText()).isEqualTo("REMEDIATION_VALIDATION_FAILED");
  }

  @Test
  @DisplayName("Requirement 3: Unknown rule codes cannot be resolved without validation (400 UNSUPPORTED_RULE_VALIDATION)")
  void unknownRuleCode_cannotBeResolvedWithoutValidation() {
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Attempting to resolve issue with null or unknown rule code"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + unknownRuleIssueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("code").asText()).isEqualTo("UNSUPPORTED_RULE_VALIDATION");
  }

  @Test
  @DisplayName("Requirement 3: Unknown entity type or entity mismatch cannot bypass verification (400 UNSUPPORTED_RULE_VALIDATION)")
  void entityTypeMismatch_cannotBypassVerification() {
    // centerIssueAId has rule DQ-TRAINEE-001 but target entity is TrainingCenter
    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Attempting to resolve trainee rule on a center entity"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + centerIssueAId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("code").asText()).isEqualTo("UNSUPPORTED_RULE_VALIDATION");
  }

  @Test
  @DisplayName("Requirement 1 & 3: Provider A scan cannot inspect, alter, or create issues for Provider B")
  void providerACannotScanProviderBData() {
    // Provider A triggers a provider-wide scan
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> scanResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        HttpMethod.POST,
        entity,
        JsonNode.class);

    assertThat(scanResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Verify Provider B issues are strictly unchanged and uninspected
    HttpEntity<Void> providerBEntity = new HttpEntity<>(providerBHeaders);
    ResponseEntity<JsonNode> bIssuesResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues?activeOnly=true",
        HttpMethod.GET,
        providerBEntity,
        JsonNode.class);

    assertThat(bIssuesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode bContent = bIssuesResponse.getBody().path("data").path("content");
    List<Long> bIssueIds = new ArrayList<>();
    bContent.forEach(node -> bIssueIds.add(node.path("id").asLong()));
    assertThat(bIssueIds).contains(issueBId);
    assertThat(bIssueIds).doesNotContain(issueAId, centerIssueAId, uncorrectedIssueId);
  }

  @Test
  @DisplayName("Requirement 1 & 3: Provider-wide scans with null issueId cannot become nationwide scans")
  void providerWideScanCannotBecomeNationwideScan() {
    // Null issueId provider scan
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        HttpMethod.POST,
        entity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Verify Provider A's active queue only has Provider A issues
    ResponseEntity<JsonNode> aIssuesResponse = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues?activeOnly=true",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    assertThat(aIssuesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode aContent = aIssuesResponse.getBody().path("data").path("content");
    List<Long> aIssueIds = new ArrayList<>();
    aContent.forEach(node -> aIssueIds.add(node.path("id").asLong()));
    assertThat(aIssueIds).doesNotContain(issueBId);
  }

  @Test
  @DisplayName("Regression Fix: Unknown DQ-CENTER-999 on a valid TrainingCenter is rejected with 400 UNSUPPORTED_RULE_VALIDATION")
  void unknownCenterRule_cannotBeResolved_returnsUnsupportedRuleValidation() {
    Long ruleId = insertAndGetId("""
        INSERT INTO data_quality_rules (rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at)
        VALUES ('DQ-CENTER-999', 'Unknown Center Rule', 'Unknown rule', 'TrainingCenter', 2, 3, 1, NOW(), NOW())
        """);
    try {
      Long issueId = insertAndGetId("""
          INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
          VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Unknown rule on center', NOW(), NOW(), NOW())
          """, ruleId, centerAId);
      trackedDqIssueIds.add(issueId);

      HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
          Map.of("resolutionNotes", "Attempting resolution for unsupported rule DQ-CENTER-999"),
          providerAHeaders);

      ResponseEntity<JsonNode> response = restTemplate.exchange(
          "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueId + "/resolve",
          HttpMethod.POST,
          resolveEntity,
          JsonNode.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().path("code").asText()).isEqualTo("UNSUPPORTED_RULE_VALIDATION");
    } finally {
      jdbcTemplate.update("DELETE FROM data_quality_rules WHERE data_quality_rule_id = ?", ruleId);
    }
  }

  @Test
  @DisplayName("Regression Fix: Unknown DQ-ENROLLMENT-999 on a valid TrainingEnrollment is rejected with 400 UNSUPPORTED_RULE_VALIDATION")
  void unknownEnrollmentRule_cannotBeResolved_returnsUnsupportedRuleValidation() {
    Long ruleId = insertAndGetId("""
        INSERT INTO data_quality_rules (rule_code, rule_name, description, target_entity_type, data_quality_category_id, data_quality_severity_id, lifecycle_status_id, created_at, updated_at)
        VALUES ('DQ-ENROLLMENT-999', 'Unknown Enrollment Rule', 'Unknown rule', 'TrainingEnrollment', 2, 3, 1, NOW(), NOW())
        """);
    try {
      Long issueId = insertAndGetId("""
          INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
          VALUES (?, 2, 3, 1, 1, 'TrainingEnrollment', ?, 'Unknown rule on enrollment', NOW(), NOW(), NOW())
          """, ruleId, enrollmentAId);
      trackedDqIssueIds.add(issueId);

      HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
          Map.of("resolutionNotes", "Attempting resolution for unsupported rule DQ-ENROLLMENT-999"),
          providerAHeaders);

      ResponseEntity<JsonNode> response = restTemplate.exchange(
          "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueId + "/resolve",
          HttpMethod.POST,
          resolveEntity,
          JsonNode.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().path("code").asText()).isEqualTo("UNSUPPORTED_RULE_VALIDATION");
    } finally {
      jdbcTemplate.update("DELETE FROM data_quality_rules WHERE data_quality_rule_id = ?", ruleId);
    }
  }

  @Test
  @DisplayName("Regression Fix: Valid DQ-CENTER-001 with corrected record is accepted and marked RESOLVED")
  void validCenter001_withCorrectedRecord_isAccepted() {
    // Ensure centerAId has a valid non-blank name and valid capacity
    jdbcTemplate.update("UPDATE training_centers SET center_name = 'Corrected Valid Center Name', operational_capacity = 50 WHERE center_id = ?", centerAId);

    Long centerRuleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-CENTER-001' LIMIT 1",
        Long.class);

    Long issueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Valid center rule on corrected center', NOW(), NOW(), NOW())
        """, centerRuleId, centerAId);
    trackedDqIssueIds.add(issueId);

    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Resolved center profile anomaly with valid center name and operational capacity"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("data").path("statusCode").asText()).isEqualTo("RESOLVED");
  }

  @Test
  @DisplayName("Regression Fix: Valid DQ-ENROLLMENT-001 with corrected record is accepted and marked RESOLVED")
  void validEnrollment001_withCorrectedRecord_isAccepted() {
    // Ensure enrollmentAId has valid chronological milestones
    jdbcTemplate.update(
        "UPDATE training_enrollments SET enrollment_date = '2026-01-02', start_date = '2026-01-05', expected_completion_date = '2026-04-01', actual_completion_date = '2026-04-02' WHERE enrollment_id = ?",
        enrollmentAId);

    Long enrollmentRuleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE rule_code = 'DQ-ENROLLMENT-001' LIMIT 1",
        Long.class);

    Long issueId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingEnrollment', ?, 'Chronological validation for enrollment', NOW(), NOW(), NOW())
        """, enrollmentRuleId, enrollmentAId);
    trackedDqIssueIds.add(issueId);

    HttpEntity<Map<String, String>> resolveEntity = new HttpEntity<>(
        Map.of("resolutionNotes", "Resolved enrollment chronology anomaly with valid dates"),
        providerAHeaders);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueId + "/resolve",
        HttpMethod.POST,
        resolveEntity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("data").path("statusCode").asText()).isEqualTo("RESOLVED");
  }

  @Test
  @DisplayName("Regression Fix: Provider-wide scan with missing detector does not falsely report full success")
  void providerWideScanWithMissingDetector_reportsPartialCompletion() {
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/re-evaluate",
        HttpMethod.POST,
        entity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path("success").asBoolean()).isTrue();
    // Verify it reports partial completion message rather than unconditional full success
    assertThat(response.getBody().path("message").asText())
        .isEqualTo("Rule re-evaluation partially completed: some rules were skipped due to unavailable detectors");

    // Verify summaries are returned and contain SKIPPED for rules lacking detectors (such as DQ-CENTER-001)
    JsonNode dataNode = response.getBody().path("data");
    assertThat(dataNode.isArray()).isTrue();
    assertThat(dataNode.size()).isGreaterThan(0);
    boolean hasSkipped = false;
    for (JsonNode item : dataNode) {
      if ("SKIPPED".equalsIgnoreCase(item.path("status").asText())) {
        hasSkipped = true;
        break;
      }
    }
    assertThat(hasSkipped).isTrue();
  }
}
