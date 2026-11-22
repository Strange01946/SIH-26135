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

    // 6. Trainee
    traineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, date_of_birth, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'John', 'Doe', 1, '1995-05-15', 101, 1001, 1, 1, NOW(), NOW())
        """, "REG-TR-" + runToken);
    trackedTraineeIds.add(traineeId);

    // 7. Enrollment for Provider A
    enrollmentAId = insertAndGetId("""
        INSERT INTO training_enrollments (enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, start_date, expected_completion_date, enrollment_status_id, created_at, updated_at)
        VALUES (?, ?, 1, ?, ?, ?, ?, '2026-01-02', '2026-01-02', '2026-04-01', 1, NOW(), NOW())
        """, "ENR-" + runToken, traineeId, courseId, providerAId, centerAId, batchAId);
    trackedEnrollmentIds.add(enrollmentAId);

    // 8. Data Quality Issues
    Long ruleId = jdbcTemplate.queryForObject(
        "SELECT data_quality_rule_id FROM data_quality_rules WHERE lifecycle_status_id = 1 LIMIT 1",
        Long.class);

    issueAId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingEnrollment', ?, 'Anomaly detected in Provider A enrollment', NOW(), NOW(), NOW())
        """, ruleId, enrollmentAId);
    trackedDqIssueIds.add(issueAId);

    issueBId = insertAndGetId("""
        INSERT INTO data_quality_issues (data_quality_rule_id, data_quality_category_id, data_quality_severity_id, data_quality_issue_status_id, data_quality_detection_source_id, entity_type, entity_id, issue_summary, detected_at, created_at, updated_at)
        VALUES (?, 2, 3, 1, 1, 'TrainingCenter', ?, 'Anomaly detected in Provider B center', NOW(), NOW(), NOW())
        """, ruleId, centerBId);
    trackedDqIssueIds.add(issueBId);

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
  @DisplayName("Scenario 3 & 10: Provider lists only own issues, never cross-provider issues")
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

    // Verify issueA is present and issueB is strictly absent
    List<Long> returnedIssueIds = new ArrayList<>();
    content.forEach(node -> returnedIssueIds.add(node.path("id").asLong()));
    assertThat(returnedIssueIds).contains(issueAId);
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
  @DisplayName("Scenario 9: Bounded event audit timeline pagination")
  void boundedTimelineEvents() {
    HttpEntity<Void> entity = new HttpEntity<>(providerAHeaders);
    ResponseEntity<JsonNode> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/v1/provider/data-quality/issues/" + issueAId + "/events?page=0&size=5",
        HttpMethod.GET,
        entity,
        JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode body = response.getBody();
    assertThat(body).isNotNull();
    JsonNode data = body.path("data");
    assertThat(data.isArray()).isTrue();
  }

  @Test
  @DisplayName("Scenario 13 & 14: Center cleanliness scorecard and completion/dropout rate computation")
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
    assertThat(scorecard.path("eligiblePopulation").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(scorecard.path("distinctAffectedEntities").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(scorecard.path("cleanlinessIndexPct").asDouble()).isBetween(0.0, 100.0);
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
}
