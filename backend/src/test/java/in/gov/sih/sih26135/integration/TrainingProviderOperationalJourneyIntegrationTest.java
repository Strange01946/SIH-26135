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

  private static final Long FIXTURE_USER_ID = 9200L;
  private static final Long FIXTURE_DUP_TRAINEE_ID = 9299L;
  private static final Long FIXTURE_TP_USER_ID = 9201L;
  private static final Long FIXTURE_TP_B_USER_ID = 9202L;
  private static final Long FIXTURE_TP_ID = 9102L;
  private static final Long FIXTURE_TP_B_ID = 9103L;
  private static final Long FIXTURE_TP_CENTER_ID = 9102L;
  private static final Long FIXTURE_TP_BATCH_ID = 9102L;
  private static final Long FIXTURE_TP_COURSE_ID = 9102L;
  private static final Long FIXTURE_TP_SECTOR_ID = 9102L;
  private static final Long FIXTURE_TP_ORG_ID = 9102L;
  private static final Long FIXTURE_TP_DEPT_ID = 9102L;
  private static final Long FIXTURE_TP_SCHEME_ID = 9102L;
  private static final Long FIXTURE_TP_PROGRAM_ID = 9102L;

  private String adminToken;
  private HttpHeaders adminHeaders;
  private String providerToken;
  private HttpHeaders providerHeaders;
  private String providerBToken;
  private HttpHeaders providerBHeaders;

  @BeforeEach
  void setUpOperationalFixtures() {
    // 1. Provision default Data Quality rules
    catalogService.ensureDefaultRulesProvisioned();

    // 2. Build authentication tokens
    adminToken = generateBearerToken(
        FIXTURE_USER_ID,
        "superadmin",
        List.of("ROLE_super_admin", "system.manage", "audit.read", "trainee.write", "placement.manage", "analytics.read")
    );
    adminHeaders = createAuthHeaders(adminToken);

    providerToken = generateBearerToken(
        FIXTURE_TP_USER_ID,
        "tp_user_295",
        List.of("ROLE_training_provider", "trainee.write", "trainee.read", "placement.manage", "center.manage")
    );
    providerHeaders = createAuthHeaders(providerToken);

    providerBToken = generateBearerToken(
        FIXTURE_TP_B_USER_ID,
        "tp_user_b_295",
        List.of("ROLE_training_provider", "trainee.write", "trainee.read", "placement.manage", "center.manage")
    );
    providerBHeaders = createAuthHeaders(providerBToken);

    // 3. Setup domain fixtures
    jdbcTemplate.update("""
        INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SEC-TP-295', 'Healthcare & Life Sciences', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_SECTOR_ID);
    trackedSectorIds.add(FIXTURE_TP_SECTOR_ID);

    jdbcTemplate.update("""
        INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'CRS-TP-295', 'Advanced Emergency Medical Technician', ?, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_COURSE_ID, FIXTURE_TP_SECTOR_ID);
    trackedCourseIds.add(FIXTURE_TP_COURSE_ID);

    jdbcTemplate.update("""
        INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'ORG-TP-295', 'Paramedical Council', 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_ORG_ID);
    trackedOrgIds.add(FIXTURE_TP_ORG_ID);

    jdbcTemplate.update("""
        INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DEP-TP-295', 'Health Training Division', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_DEPT_ID, FIXTURE_TP_ORG_ID);
    trackedDeptIds.add(FIXTURE_TP_DEPT_ID);

    jdbcTemplate.update("""
        INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'SCH-TP-295', 'National Paramedic Mission', ?, 'INR', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_SCHEME_ID, FIXTURE_TP_DEPT_ID);
    trackedSchemeIds.add(FIXTURE_TP_SCHEME_ID);

    jdbcTemplate.update("""
        INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRG-TP-295', 'Certified Paramedic Accelerated Program', ?, ?, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_PROGRAM_ID, FIXTURE_TP_DEPT_ID, FIXTURE_TP_SCHEME_ID);
    trackedProgramIds.add(FIXTURE_TP_PROGRAM_ID);

    // Primary Provider (Provider A)
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-TP-295A', 'LifeCare Training Institute', 'REG-PRV-TP-295A', 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_ID, FIXTURE_TP_ORG_ID);
    trackedProviderIds.add(FIXTURE_TP_ID);

    // Cross-tenant Provider (Provider B)
    jdbcTemplate.update("""
        INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'PRV-TP-295B', 'City Care Skills', 'REG-PRV-TP-295B', 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_B_ID, FIXTURE_TP_ORG_ID);
    trackedProviderIds.add(FIXTURE_TP_B_ID);

    jdbcTemplate.update("""
        INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'CTR-TP-295', 'Connaught Place LifeCare Center', 101, 1001, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_CENTER_ID, FIXTURE_TP_ID);
    trackedCenterIds.add(FIXTURE_TP_CENTER_ID);

    jdbcTemplate.update("""
        INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, 'BAT-TP-295', ?, ?, ?, ?, '2026-01-01', 500, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_BATCH_ID, FIXTURE_TP_COURSE_ID, FIXTURE_TP_ID, FIXTURE_TP_CENTER_ID, FIXTURE_TP_PROGRAM_ID);
    trackedBatchIds.add(FIXTURE_TP_BATCH_ID);

    // Admin user
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'superadmin', 'superadmin_tp@test.gov.in', 'hash', 'argon2id', 1, NULL, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_USER_ID);
    trackedUserIds.add(FIXTURE_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_USER_ID);

    // Provider A user
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'tp_user_295', 'tp295a@test.gov.in', 'hash', 'argon2id', 1, NULL, NULL, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_USER_ID, FIXTURE_TP_ID);
    trackedUserIds.add(FIXTURE_TP_USER_ID);

    // Provider B user
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, created_at, updated_at)
        VALUES (?, 'tp_user_b_295', 'tp295b@test.gov.in', 'hash', 'argon2id', 1, NULL, NULL, ?, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_TP_B_USER_ID, FIXTURE_TP_B_ID);
    trackedUserIds.add(FIXTURE_TP_B_USER_ID);
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
    jdbcTemplate.update("""
        INSERT INTO trainees (trainee_id, registration_number, first_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, ?, 'PreExistingDupCandidate', 1, 101, 1001, 1, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_DUP_TRAINEE_ID, dupRegNo);
    trackedTraineeIds.add(FIXTURE_DUP_TRAINEE_ID);

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

    // Track successfully created trainees for cleanup
    List<Long> importedTraineeIds = jdbcTemplate.queryForList(
        "SELECT trainee_id FROM trainees WHERE registration_number LIKE 'REG-TP500-%'",
        Long.class
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
    AssignDataQualityIssueRequest assignReq = new AssignDataQualityIssueRequest(FIXTURE_USER_ID, "Assigned to data quality steward");
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
        getBaseUrl("/api/v1/training-centers?providerId=" + FIXTURE_TP_ID),
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
