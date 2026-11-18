package in.gov.sih.sih26135.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@DisplayName("Phase 29.5 — Training Provider Operational & Data Management Journey Integration Test")
public class TrainingProviderOperationalJourneyIntegrationTest extends BaseJourneyIntegrationTest {

  @Autowired
  private ImportFileStagingService fileStagingService;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  private Long fixtureUserId;
  private Long fixtureTpUserId;
  private Long fixtureTpBUserId;
  private Long fixtureTpId;
  private Long fixtureTpBId;
  private Long fixtureTpCenterId;
  private Long fixtureTpBatchId;
  private Long fixtureTpCourseId;
  private Long fixtureTpSectorId;
  private Long fixtureTpOrgId;
  private Long fixtureTpDeptId;
  private Long fixtureTpSchemeId;
  private Long fixtureTpProgramId;
  private String runToken;

  private String adminToken;
  private HttpHeaders adminHeaders;
  private String providerToken;
  private HttpHeaders providerHeaders;
  private String providerBToken;
  private HttpHeaders providerBHeaders;

  @BeforeEach
  void setUpOperationalFixtures() {
    runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // 1. Provision default Data Quality rules
    catalogService.ensureDefaultRulesProvisioned();

    // 2. Training Domain Fixtures
    fixtureTpSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-TP-" + runToken, "Healthcare & Life Sciences " + runToken);
    trackedSectorIds.add(fixtureTpSectorId);

    fixtureTpCourseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-TP-" + runToken, "Emergency Medical EMT " + runToken, fixtureTpSectorId);
    trackedCourseIds.add(fixtureTpCourseId);

    fixtureTpOrgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-TP-" + runToken, "Paramedical Council " + runToken);
    trackedOrgIds.add(fixtureTpOrgId);

    fixtureTpDeptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, fixtureTpOrgId, "DEP-TP-" + runToken, "Health Division " + runToken);
    trackedDeptIds.add(fixtureTpDeptId);

    fixtureTpSchemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-TP-" + runToken, "National Paramedic Mission " + runToken, fixtureTpDeptId);
    trackedSchemeIds.add(fixtureTpSchemeId);

    fixtureTpProgramId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-TP-" + runToken, "Paramedic Accelerated Program " + runToken, fixtureTpDeptId, fixtureTpSchemeId);
    trackedProgramIds.add(fixtureTpProgramId);

    // Primary Provider (Provider A)
    fixtureTpId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-TP-295A-" + runToken, "LifeCare Institute " + runToken, "REG-PRV-TP-295A-" + runToken, fixtureTpOrgId);
    trackedProviderIds.add(fixtureTpId);

    // Cross-tenant Provider (Provider B)
    fixtureTpBId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-TP-295B-" + runToken, "City Care Skills " + runToken, "REG-PRV-TP-295B-" + runToken, fixtureTpOrgId);
    trackedProviderIds.add(fixtureTpBId);

    fixtureTpCenterId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, fixtureTpId, "CTR-TP-" + runToken, "LifeCare Center " + runToken);
    trackedCenterIds.add(fixtureTpCenterId);

    fixtureTpBatchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, '2026-01-01', 500, 1, NOW(), NOW())
        """, "BAT-TP-" + runToken, fixtureTpCourseId, fixtureTpId, fixtureTpCenterId, fixtureTpProgramId);
    trackedBatchIds.add(fixtureTpBatchId);

    // Admin user
    fixtureUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NULL, NULL, NOW(), NOW())
        """, "superadmin_tp_" + runToken, "superadmin_tp_" + runToken + "@test.gov.in");
    trackedUserIds.add(fixtureUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        """, fixtureUserId);

    adminToken = generateBearerToken(
        fixtureUserId,
        "superadmin_tp_" + runToken,
        List.of("ROLE_super_admin", "system.manage", "audit.read", "trainee.write", "placement.manage", "analytics.read")
    );
    adminHeaders = createAuthHeaders(adminToken);

    // Provider A user
    fixtureTpUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NULL, NULL, ?, NOW(), NOW())
        """, "tp_user_" + runToken, "tp295a_" + runToken + "@test.gov.in", fixtureTpId);
    trackedUserIds.add(fixtureTpUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'training_provider' LIMIT 1
        """, fixtureTpUserId);

    providerToken = generateBearerToken(
        fixtureTpUserId,
        "tp_user_" + runToken,
        List.of("ROLE_training_provider", "trainee.write", "trainee.read", "placement.manage", "center.manage")
    );
    providerHeaders = createAuthHeaders(providerToken);

    // Provider B user
    fixtureTpBUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NULL, NULL, ?, NOW(), NOW())
        """, "tp_user_b_" + runToken, "tp295b_" + runToken + "@test.gov.in", fixtureTpBId);
    trackedUserIds.add(fixtureTpBUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'training_provider' LIMIT 1
        """, fixtureTpBUserId);

    providerBToken = generateBearerToken(
        fixtureTpBUserId,
        "tp_user_b_" + runToken,
        List.of("ROLE_training_provider", "trainee.write", "trainee.read", "placement.manage", "center.manage")
    );
    providerBHeaders = createAuthHeaders(providerBToken);
  }

  @Test
  @DisplayName("Journey 2: Training Provider Operational, Ingestion & Data Quality LifeCycle")
  void testCompleteTrainingProviderOperationalJourney() throws Exception {
    // -------------------------------------------------------------------------
    // Step 1: In-Memory 500-Row CSV Generation
    // 450 valid records, 50 intentional quarantine records:
    //   - 15 missing required fields
    //   - 15 duplicate registration numbers
    //   - 10 invalid foreign keys
    //   - 10 malformed/cross-tenant geographic references
    // -------------------------------------------------------------------------
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n");

    long timestamp = System.currentTimeMillis();
    String dupRegNo = "REG-TP500-DUP-001-" + timestamp;

    // Pre-insert an authoritative duplicate record so all 15 CSV rows matching dupRegNo are classified as DUPLICATE
    Long dupTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'PreExistingDupCandidate', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, dupRegNo);
    trackedTraineeIds.add(dupTraineeId);

    // 450 Valid Rows
    for (int i = 1; i <= 450; i++) {
      csvBuilder.append(String.format("REG-TP500-%03d-%d,Candidate%d,1,101,1001,1,1\n", i, timestamp, i));
    }

    // 15 Missing Required Fields (missing firstName or registrationNumber)
    for (int i = 1; i <= 8; i++) {
      // Empty firstName
      csvBuilder.append(String.format("REG-TP500-MISSFN-%03d-%d,,1,101,1001,1,1\n", i, timestamp));
    }
    for (int i = 9; i <= 15; i++) {
      // Empty registrationNumber
      csvBuilder.append(String.format(",CandidateNoReg%d,1,101,1001,1,1\n", i));
    }

    // 15 Duplicate Registration Numbers (repeating REG-TP500-DUP-001)
    for (int i = 1; i <= 15; i++) {
      csvBuilder.append(String.format("%s,DuplicateCandidate%d,1,101,1001,1,1\n", dupRegNo, i));
    }

    // 10 Invalid Foreign Keys (non-existent stateId or districtId)
    for (int i = 1; i <= 5; i++) {
      csvBuilder.append(String.format("REG-TP500-BADST-%03d-%d,BadStateTrainee,1,999999,1001,1,1\n", i, timestamp));
    }
    for (int i = 6; i <= 10; i++) {
      csvBuilder.append(String.format("REG-TP500-BADDIST-%03d-%d,BadDistTrainee,1,101,999999,1,1\n", i, timestamp));
    }

    // 10 Malformed/Cross-Tenant References (district belonging to wrong state or malformed values)
    for (int i = 1; i <= 10; i++) {
      csvBuilder.append(String.format("REG-TP500-XSTATE-%03d-%d,CrossStateTrainee,1,9988,1001,1,1\n", i, timestamp));
    }

    byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

    // -------------------------------------------------------------------------
    // Step 2: Multipart Upload via POST /api/v1/import-batches
    // -------------------------------------------------------------------------
    MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
    ByteArrayResource fileResource = new ByteArrayResource(csvBytes) {
      @Override
      public String getFilename() {
        return "batch_trainees_500.csv";
      }
    };
    uploadBody.add("file", fileResource);
    uploadBody.add("entityType", "Trainee");
    uploadBody.add("sourceSystem", "TP_INTEG_TEST");

    HttpHeaders uploadHeaders = createAuthMultipartHeaders(providerToken);
    HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(uploadBody, uploadHeaders);

    ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
        getBaseUrl("/api/v1/import-batches"),
        uploadEntity,
        String.class
    );
    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode uploadJson = parseResponseJson(uploadResponse.getBody());
    assertThat(uploadJson.get("success").asBoolean()).isTrue();
    Long batchId = uploadJson.get("data").get("id").asLong();
    String batchCode = uploadJson.get("data").get("batchCode").asText();
    trackedImportBatchIds.add(batchId);
    assertThat(uploadJson.get("data").get("importBatchStatusCode").asText()).isEqualTo("PENDING");

    // -------------------------------------------------------------------------
    // Step 3: Chunked Processing via POST /api/v1/import-batches/{id}/process
    // -------------------------------------------------------------------------
    long processStart = System.currentTimeMillis();
    ResponseEntity<String> processResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/import-batches/" + batchId + "/process"),
        HttpMethod.POST,
        new HttpEntity<>(providerHeaders),
        String.class
    );
    long processDuration = System.currentTimeMillis() - processStart;
    assertThat(processResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode processJson = parseResponseJson(processResponse.getBody());
    assertThat(processJson.get("success").asBoolean()).isTrue();
    JsonNode progressData = processJson.get("data");

    // Assert batch completion and chunked row outcome numbers
    assertThat(progressData.get("status").asText()).isEqualTo("COMPLETED");
    assertThat(progressData.get("rowCount").asInt()).isEqualTo(500);
    assertThat(progressData.get("successCount").asInt()).isEqualTo(450);
    assertThat(progressData.get("failureCount").asInt()).isEqualTo(35);
    assertThat(progressData.get("duplicateCount").asInt()).isEqualTo(15);

    // Track successfully created trainees for cleanup using exact import batch ID
    List<Long> importedTraineeIds = jdbcTemplate.queryForList(
        "SELECT entity_id FROM import_records WHERE import_batch_id = ? AND entity_type = 'Trainee' AND entity_id IS NOT NULL",
        Long.class, batchId
    );
    trackedTraineeIds.addAll(importedTraineeIds);

    // Clean up staged file
    fileStagingService.deleteStagedFileQuietly(batchCode);

    // -------------------------------------------------------------------------
    // Step 4: Quarantine Inspection via GET /api/v1/import-batches/{id}/quarantine
    // -------------------------------------------------------------------------
    ResponseEntity<String> quarantineResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/import-batches/" + batchId + "/quarantine?includeDuplicates=true&size=100"),
        HttpMethod.GET,
        new HttpEntity<>(providerHeaders),
        String.class
    );
    assertThat(quarantineResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode quarantineJson = parseResponseJson(quarantineResponse.getBody());
    assertThat(quarantineJson.get("success").asBoolean()).isTrue();
    JsonNode pagedQuarantine = quarantineJson.get("data");
    assertThat(pagedQuarantine.get("totalElements").asInt()).isEqualTo(50);

    JsonNode quarantinedItems = pagedQuarantine.get("content");
    assertThat(quarantinedItems.isArray()).isTrue();
    assertThat(quarantinedItems.size()).isEqualTo(50);

    // Verify error categorization in quarantined rows
    List<String> errorCodes = new ArrayList<>();
    for (JsonNode item : quarantinedItems) {
      if (item.has("errorCode") && !item.get("errorCode").isNull()) {
        errorCodes.add(item.get("errorCode").asText());
      }
    }
    assertThat(errorCodes).isNotEmpty();
    assertThat(errorCodes).containsAnyOf(
        "MISSING_REQUIRED_FIELD",
        "DUPLICATE_REGISTRATION",
        "FOREIGN_KEY_NOT_FOUND",
        "CROSS_TENANT_REFERENCE",
        "GEOGRAPHIC_MISMATCH"
    );

    // -------------------------------------------------------------------------
    // Step 5: Automated Data Quality Engine Scan via POST /api/v1/data-quality-rules/scan
    // -------------------------------------------------------------------------
    ResponseEntity<String> scanResponse = restTemplate.exchange(
        getBaseUrl("/api/v1/data-quality-rules/scan"),
        HttpMethod.POST,
        new HttpEntity<>(adminHeaders),
        String.class
    );
    assertThat(scanResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode scanJson = parseResponseJson(scanResponse.getBody());
    assertThat(scanJson.get("success").asBoolean()).isTrue();
    JsonNode summaries = scanJson.get("data");
    assertThat(summaries.isArray()).isTrue();

    // -------------------------------------------------------------------------
    // Step 6: Data Quality Issue Lifecycle: Assign -> Investigate -> Resolve
    // -------------------------------------------------------------------------
    CreateDataQualityIssueRequest createIssueReq = new CreateDataQualityIssueRequest(
        1L, // CATEGORY: COMPLETENESS
        2L, // SEVERITY: MEDIUM
        1L, // DETECTION SOURCE: AUTOMATED_RULE
        "Trainee",
        "Anomalous batch import records detected during operational review"
    );
    createIssueReq.setEntityId(batchId);
    createIssueReq.setDetectedAt(LocalDateTime.now().minusMinutes(5));

    ResponseEntity<String> createIssueResp = restTemplate.exchange(
        getBaseUrl("/api/v1/data-quality-issues"),
        HttpMethod.POST,
        new HttpEntity<>(createIssueReq, adminHeaders),
        String.class
    );
    assertThat(createIssueResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long dqIssueId = parseResponseJson(createIssueResp.getBody()).get("data").get("id").asLong();
    trackedDqIssueIds.add(dqIssueId);

    // 1. Assign Issue
    AssignDataQualityIssueRequest assignReq = new AssignDataQualityIssueRequest(fixtureUserId, "Assigned to data quality steward");
    ResponseEntity<String> assignResp = restTemplate.exchange(
        getBaseUrl("/api/v1/data-quality-issues/" + dqIssueId + "/assign"),
        HttpMethod.POST,
        new HttpEntity<>(assignReq, adminHeaders),
        String.class
    );
    assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode assignJson = parseResponseJson(assignResp.getBody());
    assertThat(assignJson.get("data").get("dataQualityIssueStatusId").asLong()).isEqualTo(2L); // ASSIGNED

    // 2. Start Investigation
    StartInvestigationRequest startReq = new StartInvestigationRequest("Root cause analysis initiated on batch rows");
    ResponseEntity<String> startResp = restTemplate.exchange(
        getBaseUrl("/api/v1/data-quality-issues/" + dqIssueId + "/start-investigation"),
        HttpMethod.POST,
        new HttpEntity<>(startReq, adminHeaders),
        String.class
    );
    assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode startJson = parseResponseJson(startResp.getBody());
    assertThat(startJson.get("data").get("dataQualityIssueStatusId").asLong()).isEqualTo(3L); // IN_PROGRESS

    // 3. Resolve Issue
    ResolveDataQualityIssueRequest resolveReq = new ResolveDataQualityIssueRequest(
        "Invalid rows quarantined and source dataset sanitized", false);
    ResponseEntity<String> resolveResp = restTemplate.exchange(
        getBaseUrl("/api/v1/data-quality-issues/" + dqIssueId + "/resolve"),
        HttpMethod.POST,
        new HttpEntity<>(resolveReq, adminHeaders),
        String.class
    );
    assertThat(resolveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode resolveJson = parseResponseJson(resolveResp.getBody());
    assertThat(resolveJson.get("data").get("dataQualityIssueStatusId").asLong()).isEqualTo(4L); // RESOLVED

    // -------------------------------------------------------------------------
    // Step 7: Provider Scorecard & Outcomes Verification
    // -------------------------------------------------------------------------
    // National Administrator reads Provider Leaderboard
    ResponseEntity<String> leaderboardResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/providers"),
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        String.class
    );
    assertThat(leaderboardResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode leaderboardJson = parseResponseJson(leaderboardResp.getBody());
    assertThat(leaderboardJson.get("success").asBoolean()).isTrue();

    // Provider reads own centers
    ResponseEntity<String> centersResp = restTemplate.exchange(
        getBaseUrl("/api/v1/training-centers?providerId=" + fixtureTpId),
        HttpMethod.GET,
        new HttpEntity<>(providerHeaders),
        String.class
    );
    assertThat(centersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Negative Security: Cross-Tenant Provider Cannot Process Another Provider's Batch")
  void testCrossTenantProviderCannotProcessOrReadBatch() throws Exception {
    // 1. Provider A stages a batch
    String csv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
        + "REG-TP-CROSS-01,ValidCandidate,1,101,1001,1,1\n";

    MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
    ByteArrayResource fileResource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return "batch_cross.csv";
      }
    };
    uploadBody.add("file", fileResource);
    uploadBody.add("entityType", "Trainee");
    uploadBody.add("sourceSystem", "TP_CROSS_TEST");

    HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(
        uploadBody, createAuthMultipartHeaders(providerToken));

    ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
        getBaseUrl("/api/v1/import-batches"),
        uploadEntity,
        String.class
    );
    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode uploadJson = parseResponseJson(uploadResponse.getBody());
    Long batchId = uploadJson.get("data").get("id").asLong();
    String batchCode = uploadJson.get("data").get("batchCode").asText();
    trackedImportBatchIds.add(batchId);

    // 2. Provider B attempts to process Provider A's batch -> MUST be rejected (403 Forbidden)
    ResponseEntity<String> unauthorizedProcessResp = restTemplate.exchange(
        getBaseUrl("/api/v1/import-batches/" + batchId + "/process"),
        HttpMethod.POST,
        new HttpEntity<>(providerBHeaders),
        String.class
    );
    assertThat(unauthorizedProcessResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // 3. Provider B attempts to inspect Provider A's quarantine -> MUST be rejected (403 Forbidden)
    ResponseEntity<String> unauthorizedQuarantineResp = restTemplate.exchange(
        getBaseUrl("/api/v1/import-batches/" + batchId + "/quarantine"),
        HttpMethod.GET,
        new HttpEntity<>(providerBHeaders),
        String.class
    );
    assertThat(unauthorizedQuarantineResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // Clean up staged file
    fileStagingService.deleteStagedFileQuietly(batchCode);
  }
}
