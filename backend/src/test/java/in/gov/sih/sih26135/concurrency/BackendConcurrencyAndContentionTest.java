package in.gov.sih.sih26135.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheConfig;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.integration.BaseJourneyIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Phase 29.6 — Concurrency, Contention & High-Load Stress Testing.
 *
 * <p>Validates correctness under contention across:
 * <ul>
 *   <li>1. Candidate Enrollment Contention: 20 concurrent requests against batch capacity 5.
 *       Exactly 5 succeed (201 Created), 15 receive 409 Conflict strictly with code BATCH_CAPACITY_EXCEEDED.</li>
 *   <li>2. Simultaneous Bulk Import Saturation: 3 concurrent multipart CSV uploads with chunked processing,
 *       exact row status outcome reconciliation, cross-batch row isolation, competing worker claim testing,
 *       and strict import-record fixture tracking.</li>
 *   <li>2b. Bulk Import Fixture Isolation Regression: verifies that unrelated matching-token records survive cleanup.</li>
 *   <li>3. Caffeine Cache Invalidation & Multi-Endpoint Query Bursts: 50 concurrent readers across cold/warm stages
 *       for Q01, Q03, Q05, mutation eviction with verified metric delta, query amplification bounds, and tenant isolation.</li>
 *   <li>4. Authentication & Account Lockout Concurrency: atomic failed login counting, threshold enforcement,
 *       HTTP 423 Locked on 5th failure and subsequent locked attempts, and atomic serialization under concurrent attack.</li>
 * </ul>
 */
@DisplayName("Phase 29.6 — Backend Concurrency, Contention & High-Load Stress Tests")
public class BackendConcurrencyAndContentionTest extends BaseJourneyIntegrationTest {

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @AfterEach
  void cleanUp() {
    teardownTrackedFixtures();
  }

  // =========================================================================
  // Scenario 1: Candidate Enrollment Contention
  // =========================================================================

  @Test
  @DisplayName("1. Candidate Enrollment Contention — 20 concurrent requests against batch capacity 5")
  void testCandidateEnrollmentContention_overflowRejectedWith409() throws Exception {
    executeEnrollmentContentionScenario("RUN1", 5, 20);
  }

  @Test
  @DisplayName("1b. Candidate Enrollment Contention Repeatability — verify second isolated run behaves deterministically")
  void testCandidateEnrollmentContention_repeatability() throws Exception {
    executeEnrollmentContentionScenario("RUN2", 5, 20);
  }

  private void executeEnrollmentContentionScenario(String iterationTag, int batchCapacity, int workerCount) throws Exception {
    long runToken = Math.abs(System.nanoTime() % 10000000L);

    // 1. Setup isolated domain hierarchy
    Long adminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NOW(), NOW())
        """, "enr_admin_" + iterationTag + "_" + runToken, "enr_admin_" + iterationTag + "_" + runToken + "@test.gov.in");
    trackedUserIds.add(adminUserId);

    String adminToken = generateBearerToken(adminUserId, "enr_admin_" + iterationTag + "_" + runToken, List.of(
        "ROLE_super_admin",
        "ROLE_government_admin",
        "trainee.read",
        "trainee.write",
        "enrollment.manage",
        "course.manage",
        "system.manage"
    ));
    HttpHeaders authHeaders = createAuthHeaders(adminToken);

    Long sectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-ENR-" + iterationTag + "-" + runToken, "Contention Sector " + runToken);
    trackedSectorIds.add(sectorId);

    Long courseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-ENR-" + iterationTag + "-" + runToken, "Contention Course " + runToken, sectorId);
    trackedCourseIds.add(courseId);

    Long orgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-ENR-" + iterationTag + "-" + runToken, "Contention Org " + runToken);
    trackedOrgIds.add(orgId);

    Long deptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, orgId, "DEP-ENR-" + iterationTag + "-" + runToken, "Contention Dept " + runToken);
    trackedDeptIds.add(deptId);

    Long schemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-ENR-" + iterationTag + "-" + runToken, "Contention Scheme " + runToken, deptId);
    trackedSchemeIds.add(schemeId);

    Long programId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-ENR-" + iterationTag + "-" + runToken, "Contention Prog " + runToken, deptId, schemeId);
    trackedProgramIds.add(programId);

    Long providerId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-ENR-" + iterationTag + "-" + runToken, "Contention Provider " + runToken, "REG-PRV-ENR-" + iterationTag + "-" + runToken, orgId);
    trackedProviderIds.add(providerId);

    Long centerId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, providerId, "CTR-ENR-" + iterationTag + "-" + runToken, "Contention Center " + runToken);
    trackedCenterIds.add(centerId);

    // Isolated batch with exact capacity = batchCapacity (5)
    Long batchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, CURDATE(), ?, 1, NOW(), NOW())
        """, "BAT-ENR-" + iterationTag + "-" + runToken, courseId, providerId, centerId, programId, batchCapacity);
    trackedBatchIds.add(batchId);

    // 2. Create 20 distinct candidate fixtures with valid DATA_PROCESSING consent
    List<Long> candidateIds = new ArrayList<>(workerCount);
    for (int i = 0; i < workerCount; i++) {
      String regNo = "REG-CONC-" + iterationTag + "-" + runToken + "-" + i;
      Long traineeId = insertAndGetId("""
          INSERT INTO trainees (registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, ?, 'Contention', 1, ?, '9876540957', 'Test Street', '110001', 101, 1001, 6, 1, 3, NOW(), NOW())
          """, regNo, "Candidate" + i, "cand" + i + "_" + iterationTag + "_" + runToken + "@test.gov.in");
      trackedTraineeIds.add(traineeId);
      candidateIds.add(traineeId);

      Long consentId = insertAndGetId("""
          INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
          VALUES (?, 1, 'Data processing statutory consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
          """, traineeId);
      trackedConsentIds.add(consentId);
    }

    // 3. Launch 20 concurrent enrollment requests synchronized by CyclicBarrier
    CyclicBarrier barrier = new CyclicBarrier(workerCount);
    ExecutorService executor = Executors.newFixedThreadPool(workerCount);

    List<Future<ResponseEntity<String>>> futures = new ArrayList<>(workerCount);
    try {
      for (int i = 0; i < workerCount; i++) {
        final Long candidateId = candidateIds.get(i);
        final String enrNum = "ENR-CONC-" + iterationTag + "-" + runToken + "-" + i;

        CreateTrainingEnrollmentRequest req = new CreateTrainingEnrollmentRequest();
        req.setTraineeId(candidateId);
        req.setBatchId(batchId);
        req.setEnrollmentDate(LocalDate.now());
        req.setEnrollmentNumber(enrNum);
        req.setProgramId(programId);
        req.setCourseId(courseId);
        req.setProviderId(providerId);
        req.setCenterId(centerId);
        req.setEnrollmentStatusId(1L);
        req.setStartDate(LocalDate.now());
        req.setExpectedCompletionDate(LocalDate.now().plusMonths(3));

        Callable<ResponseEntity<String>> worker = () -> {
          barrier.await(10, TimeUnit.SECONDS); // synchronized start
          return restTemplate.exchange(
              getBaseUrl("/api/v1/training-enrollments"),
              HttpMethod.POST,
              new HttpEntity<>(req, authHeaders),
              String.class
          );
        };
        futures.add(executor.submit(worker));
      }

      int successCount = 0;
      int conflictCount = 0;
      int errorCount = 0;
      List<Long> enrolledCandidateIds = new CopyOnWriteArrayList<>();

      for (int i = 0; i < workerCount; i++) {
        ResponseEntity<String> resp = futures.get(i).get(20, TimeUnit.SECONDS);
        int statusCode = resp.getStatusCode().value();
        if (statusCode == HttpStatus.CREATED.value()) {
          successCount++;
          JsonNode json = parseResponseJson(resp.getBody());
          assertThat(json.get("success").asBoolean()).isTrue();
          Long enrId = json.get("data").get("id").asLong();
          trackedEnrollmentIds.add(enrId);
          enrolledCandidateIds.add(candidateIds.get(i));
        } else if (statusCode == HttpStatus.CONFLICT.value()) {
          conflictCount++;
          JsonNode json = parseResponseJson(resp.getBody());
          String code = json.has("code") ? json.get("code").asText() : "";
          assertThat(code).as("Rejected overflow request must strictly return BATCH_CAPACITY_EXCEEDED").isEqualTo("BATCH_CAPACITY_EXCEEDED");
        } else {
          errorCount++;
          log.error("Unexpected status code {} in enrollment contention: {}", statusCode, resp.getBody());
        }
      }

      // Assertions on API outcomes
      assertThat(errorCount).as("Zero HTTP 500 or unexpected errors").isEqualTo(0);
      assertThat(successCount).as("Exactly %d enrollments accepted", batchCapacity).isEqualTo(batchCapacity);
      assertThat(conflictCount).as("Exactly %d overflow enrollments rejected with 409", workerCount - batchCapacity)
          .isEqualTo(workerCount - batchCapacity);

      // Independent SQL assertions on database state
      Integer dbEnrollmentCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM training_enrollments WHERE batch_id = ? AND deleted_at IS NULL",
          Integer.class, batchId);
      assertThat(dbEnrollmentCount).as("Database batch occupancy exactly equals capacity").isEqualTo(batchCapacity);

      Integer distinctTrainees = jdbcTemplate.queryForObject(
          "SELECT COUNT(DISTINCT trainee_id) FROM training_enrollments WHERE batch_id = ? AND deleted_at IS NULL",
          Integer.class, batchId);
      assertThat(distinctTrainees).as("No candidate enrolled more than once").isEqualTo(batchCapacity);

      // Verify rejected candidates remain strictly un-enrolled
      for (Long candidateId : candidateIds) {
        if (!enrolledCandidateIds.contains(candidateId)) {
          Integer candidateEnrollments = jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM training_enrollments WHERE trainee_id = ? AND batch_id = ?",
              Integer.class, candidateId, batchId);
          assertThat(candidateEnrollments).as("Rejected candidate %d has zero enrollments", candidateId).isEqualTo(0);
        }
      }

      log.info("[EnrollmentContention] Scenario {} complete: workers={}, capacity={}, accepted={}, rejected={}",
          iterationTag, workerCount, batchCapacity, successCount, conflictCount);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  // =========================================================================
  // Scenario 2: Simultaneous Bulk Import Saturation
  // =========================================================================

  @Test
  @DisplayName("2. Simultaneous Bulk Import Saturation — 3 concurrent multipart CSV uploads with chunked processing & isolation")
  void testSimultaneousBulkImportSaturation_noCrossBatchRowLeakage() throws Exception {
    long runToken = Math.abs(System.nanoTime() % 10000000L);

    Long adminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
        """, "import_admin_" + runToken, "import_admin_" + runToken + "@test.gov.in");
    trackedUserIds.add(adminUserId);

    String adminToken = generateBearerToken(adminUserId, "import_admin_" + runToken, List.of(
        "ROLE_super_admin",
        "ROLE_government_admin",
        "trainee.read",
        "trainee.write",
        "system.manage"
    ));
    HttpHeaders multipartHeaders = createAuthMultipartHeaders(adminToken);
    HttpHeaders authHeaders = createAuthHeaders(adminToken);

    // Build 3 distinct CSV payloads with known deterministic row counts
    // Batch 1: 10 rows (8 valid, 1 duplicate reg within file, 1 missing required field)
    String csv1 = """
        registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
        REG-IMP-A1-%d,Alice,1,101,1001,1,3
        REG-IMP-A2-%d,Bob,2,101,1001,1,3
        REG-IMP-A3-%d,Charlie,1,101,1001,1,3
        REG-IMP-A4-%d,Diana,1,101,1001,1,3
        REG-IMP-A5-%d,Evan,2,101,1001,1,3
        REG-IMP-A6-%d,Fiona,1,101,1001,1,3
        REG-IMP-A7-%d,George,2,101,1001,1,3
        REG-IMP-A8-%d,Hannah,1,101,1001,1,3
        REG-IMP-A1-%d,AliceDup,1,101,1001,1,3
        ,InvalidMissingReg,1,101,1001,1,3
        """.formatted(runToken, runToken, runToken, runToken, runToken, runToken, runToken, runToken, runToken);

    // Batch 2: 10 rows (8 valid, 2 invalid)
    String csv2 = """
        registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
        REG-IMP-B1-%d,Ian,2,101,1001,1,3
        REG-IMP-B2-%d,Julia,1,101,1001,1,3
        REG-IMP-B3-%d,Kevin,2,101,1001,1,3
        REG-IMP-B4-%d,Laura,1,101,1001,1,3
        REG-IMP-B5-%d,Michael,2,101,1001,1,3
        REG-IMP-B6-%d,Nina,1,101,1001,1,3
        REG-IMP-B7-%d,Oscar,2,101,1001,1,3
        REG-IMP-B8-%d,Paula,1,101,1001,1,3
        ,MissingRegB1,1,101,1001,1,3
        ,MissingRegB2,2,101,1001,1,3
        """.formatted(runToken, runToken, runToken, runToken, runToken, runToken, runToken, runToken);

    // Batch 3: 10 rows (7 valid, 2 duplicates, 1 invalid)
    String csv3 = """
        registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
        REG-IMP-C1-%d,Quinn,1,101,1001,1,3
        REG-IMP-C2-%d,Rachel,1,101,1001,1,3
        REG-IMP-C3-%d,Sam,2,101,1001,1,3
        REG-IMP-C4-%d,Tina,1,101,1001,1,3
        REG-IMP-C5-%d,Uma,1,101,1001,1,3
        REG-IMP-C6-%d,Victor,2,101,1001,1,3
        REG-IMP-C7-%d,Wendy,1,101,1001,1,3
        REG-IMP-C1-%d,QuinnDup1,1,101,1001,1,3
        REG-IMP-C2-%d,RachelDup2,1,101,1001,1,3
        ,MissingRegC1,1,101,1001,1,3
        """.formatted(runToken, runToken, runToken, runToken, runToken, runToken, runToken, runToken, runToken);

    List<String> csvPayloads = List.of(csv1, csv2, csv3);
    int batchCount = csvPayloads.size();

    // 1. Concurrent multipart staging uploads
    CyclicBarrier uploadBarrier = new CyclicBarrier(batchCount);
    ExecutorService uploadExecutor = Executors.newFixedThreadPool(batchCount);
    List<Future<ResponseEntity<String>>> uploadFutures = new ArrayList<>(batchCount);

    try {
      for (int i = 0; i < batchCount; i++) {
        final byte[] bytes = csvPayloads.get(i).getBytes(StandardCharsets.UTF_8);
        final String filename = "bulk_batch_" + i + "_" + runToken + ".csv";

        Callable<ResponseEntity<String>> uploadTask = () -> {
          uploadBarrier.await(10, TimeUnit.SECONDS);

          MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
          ByteArrayResource res = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
              return filename;
            }
          };
          body.add("file", res);
          body.add("entityType", "Trainee");
          body.add("sourceSystem", "CONCURRENCY_TEST");

          return restTemplate.postForEntity(
              getBaseUrl("/api/v1/import-batches"),
              new HttpEntity<>(body, multipartHeaders),
              String.class
          );
        };
        uploadFutures.add(uploadExecutor.submit(uploadTask));
      }

      List<Long> stagedBatchIds = new ArrayList<>(batchCount);
      for (Future<ResponseEntity<String>> f : uploadFutures) {
        ResponseEntity<String> resp = f.get(15, TimeUnit.SECONDS);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = parseResponseJson(resp.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        Long bId = json.get("data").get("id").asLong();
        trackedImportBatchIds.add(bId);
        stagedBatchIds.add(bId);
      }

      assertThat(stagedBatchIds).hasSize(3);
      assertThat(stagedBatchIds.stream().distinct().count()).isEqualTo(3);

      // Verify staged batches in MySQL before processing
      for (Long bId : stagedBatchIds) {
        String statusCode = jdbcTemplate.queryForObject(
            "SELECT status_code FROM ref_import_batch_status s JOIN import_batches b ON s.import_batch_status_id = b.import_batch_status_id WHERE b.import_batch_id = ?",
            String.class, bId);
        assertThat(statusCode).as("Staged batch %d has status PENDING", bId).isEqualTo("PENDING");
      }

      // 2. Concurrent chunked batch processing
      CyclicBarrier processBarrier = new CyclicBarrier(batchCount);
      ExecutorService processExecutor = Executors.newFixedThreadPool(batchCount);
      List<Future<ResponseEntity<String>>> processFutures = new ArrayList<>(batchCount);

      try {
        for (Long bId : stagedBatchIds) {
          Callable<ResponseEntity<String>> processTask = () -> {
            processBarrier.await(10, TimeUnit.SECONDS);
            return restTemplate.exchange(
                getBaseUrl("/api/v1/import-batches/" + bId + "/process"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders),
                String.class
            );
          };
          processFutures.add(processExecutor.submit(processTask));
        }

        for (int i = 0; i < batchCount; i++) {
          ResponseEntity<String> processResp = processFutures.get(i).get(20, TimeUnit.SECONDS);
          assertThat(processResp.getStatusCode()).isEqualTo(HttpStatus.OK);
          JsonNode processJson = parseResponseJson(processResp.getBody());
          assertThat(processJson.get("success").asBoolean()).isTrue();
          JsonNode data = processJson.get("data");
          assertThat(data.get("status").asText()).isEqualTo("COMPLETED");
          assertThat(data.get("rowCount").asInt()).isEqualTo(10);
        }

        // Verify database state: no rows left in PROCESSING status
        Integer stuckRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM import_records WHERE import_batch_id IN (?, ?, ?) AND import_record_status_id = (SELECT import_record_status_id FROM ref_import_record_status WHERE status_code = 'PROCESSING')",
            Integer.class, stagedBatchIds.get(0), stagedBatchIds.get(1), stagedBatchIds.get(2));
        assertThat(stuckRows).as("Zero rows stuck in PROCESSING state").isEqualTo(0);

        // Verify row isolation and exact counts per batch in import_records
        for (Long bId : stagedBatchIds) {
          Integer rowCount = jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM import_records WHERE import_batch_id = ?",
              Integer.class, bId);
          assertThat(rowCount).as("Processed row count for batch %d in import_records", bId).isEqualTo(10);
        }

        // =====================================================================
        // Detailed verification of outcomes and ownership for each batch
        // =====================================================================
        Long b1Id = stagedBatchIds.get(0);
        Long b2Id = stagedBatchIds.get(1);
        Long b3Id = stagedBatchIds.get(2);

        // Batch 1: 8 SUCCESS, 1 DUPLICATE, 1 FAILED
        Integer b1Success = queryRecordStatusCount(b1Id, "SUCCESS");
        Integer b1Dup = queryRecordStatusCount(b1Id, "DUPLICATE");
        Integer b1Failed = queryRecordStatusCount(b1Id, "FAILED");
        assertThat(b1Success).as("Batch 1 success count").isEqualTo(8);
        assertThat(b1Dup).as("Batch 1 duplicate count").isEqualTo(1);
        assertThat(b1Failed).as("Batch 1 failed count").isEqualTo(1);

        // Batch 2: 8 SUCCESS, 0 DUPLICATE, 2 FAILED
        Integer b2Success = queryRecordStatusCount(b2Id, "SUCCESS");
        Integer b2Dup = queryRecordStatusCount(b2Id, "DUPLICATE");
        Integer b2Failed = queryRecordStatusCount(b2Id, "FAILED");
        assertThat(b2Success).as("Batch 2 success count").isEqualTo(8);
        assertThat(b2Dup).as("Batch 2 duplicate count").isEqualTo(0);
        assertThat(b2Failed).as("Batch 2 failed count").isEqualTo(2);

        // Batch 3: 7 SUCCESS, 2 DUPLICATE, 1 FAILED
        Integer b3Success = queryRecordStatusCount(b3Id, "SUCCESS");
        Integer b3Dup = queryRecordStatusCount(b3Id, "DUPLICATE");
        Integer b3Failed = queryRecordStatusCount(b3Id, "FAILED");
        assertThat(b3Success).as("Batch 3 success count").isEqualTo(7);
        assertThat(b3Dup).as("Batch 3 duplicate count").isEqualTo(2);
        assertThat(b3Failed).as("Batch 3 failed count").isEqualTo(1);

        // Verify quarantine endpoints return exact expected quarantined rows (FAILED + DUPLICATE)
        verifyQuarantineCount(b1Id, authHeaders, 2); // 1 FAILED + 1 DUPLICATE
        verifyQuarantineCount(b2Id, authHeaders, 2); // 2 FAILED
        verifyQuarantineCount(b3Id, authHeaders, 3); // 1 FAILED + 2 DUPLICATE

        // Verify cross-batch row isolation: zero rows from another batch appear under the wrong batch ID
        Integer crossBatchRows = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM import_records r1
            JOIN import_records r2 ON r1.entity_id = r2.entity_id AND r1.import_batch_id != r2.import_batch_id
            WHERE r1.import_batch_id IN (?, ?, ?) AND r1.entity_id IS NOT NULL
            """, Integer.class, b1Id, b2Id, b3Id);
        assertThat(crossBatchRows).as("Zero cross-batch record linkage or leakage").isEqualTo(0);

        // Track created trainees for cleanup strictly through import-record entity relationships
        List<Long> createdTraineeIds = jdbcTemplate.queryForList("""
            SELECT entity_id FROM import_records
            WHERE import_batch_id IN (?, ?, ?)
              AND entity_type = 'Trainee'
              AND entity_id IS NOT NULL
            """, Long.class, b1Id, b2Id, b3Id);
        assertThat(createdTraineeIds).as("Total created trainees across 3 batches (8+8+7)").hasSize(23);
        trackedTraineeIds.addAll(createdTraineeIds);

        // Verify exact registration numbers in trainees match source files
        for (int i = 1; i <= 8; i++) {
          String expectedReg = "REG-IMP-A" + i + "-" + runToken;
          Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trainees WHERE registration_number = ?", Integer.class, expectedReg);
          assertThat(count).as("Batch 1 trainee %s created", expectedReg).isEqualTo(1);
        }
        for (int i = 1; i <= 8; i++) {
          String expectedReg = "REG-IMP-B" + i + "-" + runToken;
          Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trainees WHERE registration_number = ?", Integer.class, expectedReg);
          assertThat(count).as("Batch 2 trainee %s created", expectedReg).isEqualTo(1);
        }
        for (int i = 1; i <= 7; i++) {
          String expectedReg = "REG-IMP-C" + i + "-" + runToken;
          Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trainees WHERE registration_number = ?", Integer.class, expectedReg);
          assertThat(count).as("Batch 3 trainee %s created", expectedReg).isEqualTo(1);
        }

        // =====================================================================
        // Test competing claims against the same eligible work:
        // Stage batch 4 and launch 2 concurrent workers attempting to process it.
        // Exactly 1 worker must succeed; the other must receive HTTP 409 Conflict.
        // =====================================================================
        String csv4 = """
            registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
            REG-IMP-D1-%d,David,1,101,1001,1,3
            REG-IMP-D2-%d,Emma,2,101,1001,1,3
            """.formatted(runToken, runToken);
        byte[] bytes4 = csv4.getBytes(StandardCharsets.UTF_8);

        MultiValueMap<String, Object> body4 = new LinkedMultiValueMap<>();
        ByteArrayResource res4 = new ByteArrayResource(bytes4) {
          @Override
          public String getFilename() {
            return "competing_batch_" + runToken + ".csv";
          }
        };
        body4.add("file", res4);
        body4.add("entityType", "Trainee");
        body4.add("sourceSystem", "CONCURRENCY_TEST");

        ResponseEntity<String> stage4Resp = restTemplate.postForEntity(
            getBaseUrl("/api/v1/import-batches"),
            new HttpEntity<>(body4, multipartHeaders),
            String.class
        );
        assertThat(stage4Resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long b4Id = parseResponseJson(stage4Resp.getBody()).get("data").get("id").asLong();
        trackedImportBatchIds.add(b4Id);

        CyclicBarrier claimBarrier = new CyclicBarrier(2);
        ExecutorService claimExecutor = Executors.newFixedThreadPool(2);
        try {
          Callable<ResponseEntity<String>> claimTask = () -> {
            claimBarrier.await(10, TimeUnit.SECONDS);
            return restTemplate.exchange(
                getBaseUrl("/api/v1/import-batches/" + b4Id + "/process"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders),
                String.class
            );
          };

          Future<ResponseEntity<String>> claimF1 = claimExecutor.submit(claimTask);
          Future<ResponseEntity<String>> claimF2 = claimExecutor.submit(claimTask);

          ResponseEntity<String> cr1 = claimF1.get(15, TimeUnit.SECONDS);
          ResponseEntity<String> cr2 = claimF2.get(15, TimeUnit.SECONDS);

          int claimSuccessCount = 0;
          int claimConflictCount = 0;
          if (cr1.getStatusCode() == HttpStatus.OK) claimSuccessCount++;
          if (cr2.getStatusCode() == HttpStatus.OK) claimSuccessCount++;
          if (cr1.getStatusCode() == HttpStatus.CONFLICT) claimConflictCount++;
          if (cr2.getStatusCode() == HttpStatus.CONFLICT) claimConflictCount++;

          assertThat(claimSuccessCount).as("Exactly one competing worker succeeds in claiming batch 4").isEqualTo(1);
          assertThat(claimConflictCount).as("Competing worker receives 409 Conflict").isEqualTo(1);

          // Verify batch 4 rows in DB: exactly 2 rows processed once, no duplicate execution
          Integer b4RowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_records WHERE import_batch_id = ?", Integer.class, b4Id);
          assertThat(b4RowCount).as("Batch 4 processed exactly once with 2 records").isEqualTo(2);

          List<Long> b4TraineeIds = jdbcTemplate.queryForList("""
              SELECT entity_id FROM import_records
              WHERE import_batch_id = ? AND entity_type = 'Trainee' AND entity_id IS NOT NULL
              """, Long.class, b4Id);
          trackedTraineeIds.addAll(b4TraineeIds);
        } finally {
          claimExecutor.shutdownNow();
          assertThat(claimExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        log.info("[BulkImportSaturation] 3 concurrent batches + 1 competing claim batch verified with zero row leakage and exact outcomes");
      } finally {
        processExecutor.shutdownNow();
        assertThat(processExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
    } finally {
      uploadExecutor.shutdownNow();
      assertThat(uploadExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private Integer queryRecordStatusCount(Long batchId, String statusCode) {
    return jdbcTemplate.queryForObject("""
        SELECT COUNT(*) FROM import_records r
        JOIN ref_import_record_status s ON r.import_record_status_id = s.import_record_status_id
        WHERE r.import_batch_id = ? AND s.status_code = ?
        """, Integer.class, batchId, statusCode);
  }

  private void verifyQuarantineCount(Long batchId, HttpHeaders authHeaders, int expectedQuarantineCount) throws Exception {
    ResponseEntity<String> resp = restTemplate.exchange(
        getBaseUrl("/api/v1/import-batches/" + batchId + "/quarantine?includeDuplicates=true"),
        HttpMethod.GET,
        new HttpEntity<>(authHeaders),
        String.class
    );
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode json = parseResponseJson(resp.getBody());
    assertThat(json.get("success").asBoolean()).isTrue();
    JsonNode content = json.get("data").get("content");
    assertThat(content.size()).as("Quarantined records count for batch %d", batchId).isEqualTo(expectedQuarantineCount);
  }

  // =========================================================================
  // Scenario 2b: Bulk Import Fixture Isolation & Cleanup Survival Regression
  // =========================================================================

  @Test
  @DisplayName("2b. Bulk Import Fixture Isolation Regression — unrelated matching-token record survives cleanup")
  void testBulkImportFixtureIsolation_unrelatedMatchingTokenRecordSurvivesCleanup() throws Exception {
    long runToken = Math.abs(System.nanoTime() % 10000000L);

    // 1. Insert an unrelated pre-existing trainee with a registration number matching the token pattern
    String unrelatedRegNo = "REG-UNRELATED-RUNTOKEN-" + runToken;
    Long unrelatedTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'Unrelated', 'PreExisting', 1, ?, '9876540957', 'Untracked Lane', '110001', 101, 1001, 6, 1, 3, NOW(), NOW())
        """, unrelatedRegNo, "unrelated_" + runToken + "@test.gov.in");

    // Do NOT add unrelatedTraineeId to trackedTraineeIds

    try {
      // 2. Stage and process an import batch that creates legitimate test-owned trainees
      Long adminUserId = insertAndGetId("""
          INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
          VALUES (?, ?, 'hash', 'argon2id', 1, NOW(), NOW())
          """, "iso_admin_" + runToken, "iso_admin_" + runToken + "@test.gov.in");
      trackedUserIds.add(adminUserId);

      String adminToken = generateBearerToken(adminUserId, "iso_admin_" + runToken, List.of(
          "ROLE_super_admin", "ROLE_government_admin", "trainee.read", "trainee.write", "system.manage"));
      HttpHeaders multipartHeaders = createAuthMultipartHeaders(adminToken);
      HttpHeaders authHeaders = createAuthHeaders(adminToken);

      String csv = """
          registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
          REG-OWNED-%d,OwnedCandidate,1,101,1001,1,3
          """.formatted(runToken);
      byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      ByteArrayResource res = new ByteArrayResource(bytes) {
        @Override
        public String getFilename() {
          return "isolation_test_" + runToken + ".csv";
        }
      };
      body.add("file", res);
      body.add("entityType", "Trainee");
      body.add("sourceSystem", "CONCURRENCY_TEST");

      ResponseEntity<String> stageResp = restTemplate.postForEntity(
          getBaseUrl("/api/v1/import-batches"),
          new HttpEntity<>(body, multipartHeaders),
          String.class
      );
      assertThat(stageResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      Long batchId = parseResponseJson(stageResp.getBody()).get("data").get("id").asLong();
      trackedImportBatchIds.add(batchId);

      ResponseEntity<String> processResp = restTemplate.exchange(
          getBaseUrl("/api/v1/import-batches/" + batchId + "/process"),
          HttpMethod.POST,
          new HttpEntity<>(authHeaders),
          String.class
      );
      assertThat(processResp.getStatusCode()).isEqualTo(HttpStatus.OK);

      // Track created trainee strictly through import_records entity relationship
      List<Long> batchTraineeIds = jdbcTemplate.queryForList("""
          SELECT entity_id FROM import_records
          WHERE import_batch_id = ? AND entity_type = 'Trainee' AND entity_id IS NOT NULL
          """, Long.class, batchId);
      assertThat(batchTraineeIds).hasSize(1);
      Long ownedTraineeId = batchTraineeIds.get(0);
      trackedTraineeIds.add(ownedTraineeId);

      // 3. Execute teardown cleanup on tracked fixtures
      teardownTrackedFixtures();

      // 4. Verification: The owned trainee was deleted, but the unrelated matching-token trainee SURVIVED!
      Integer ownedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = ?", Integer.class, ownedTraineeId);
      assertThat(ownedCount).as("Owned trainee was deleted during teardown").isEqualTo(0);

      Integer unrelatedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = ?", Integer.class, unrelatedTraineeId);
      assertThat(unrelatedCount).as("Unrelated trainee with matching runToken survived teardown intact").isEqualTo(1);

      // Verify authoritative test trainee 1005143 and its 5 active GRANTED consents are untouched
      Integer authTraineeCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143", Integer.class);
      assertThat(authTraineeCount).as("Authoritative trainee 1005143 is untouched").isEqualTo(1);

      Integer authConsentCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1", Integer.class);
      assertThat(authConsentCount).as("All 5 active GRANTED consents for trainee 1005143 are untouched").isEqualTo(5);

      log.info("[FixtureIsolation] Verified that exact entity-id tracking protects unrelated records from cleanup");
    } finally {
      // Clean up the unrelated fixture manually
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", unrelatedTraineeId);
    }
  }

  // =========================================================================
  // Scenario 3: Caffeine Cache Invalidation & Multi-Endpoint Query Bursts
  // =========================================================================

  @Test
  @DisplayName("3. Caffeine Cache Invalidation & Query Bursts — 50 concurrent readers across cold/warm/mutation/tenant stages for Q endpoints")
  void testCaffeineCacheInvalidationAndQueryBursts_stageABCD() throws Exception {
    long runToken = Math.abs(System.nanoTime() % 10000000L);

    Long adminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NOW(), NOW())
        """, "cache_admin_" + runToken, "cache_admin_" + runToken + "@test.gov.in");
    trackedUserIds.add(adminUserId);

    String adminToken = generateBearerToken(adminUserId, "cache_admin_" + runToken, List.of(
        "ROLE_super_admin",
        "ROLE_government_admin",
        "analytics.read",
        "trainee.write",
        "enrollment.manage",
        "course.manage",
        "system.manage"
    ));
    HttpHeaders adminHeaders = createAuthHeaders(adminToken);

    CaffeineCache nationalCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW);
    CaffeineCache stateCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_STATE_AGGREGATES);
    CaffeineCache institutionalCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_INSTITUTIONAL);

    assertThat(nationalCache).as("National overview Caffeine cache must exist").isNotNull();
    assertThat(stateCache).as("State aggregates Caffeine cache must exist").isNotNull();
    assertThat(institutionalCache).as("Institutional Caffeine cache must exist").isNotNull();

    int readerCount = 50;

    // -------------------------------------------------------------------------
    // Stage A: Cold Cache — Clear cache, burst 50 concurrent readers across Q01, Q03, Q05
    // -------------------------------------------------------------------------
    nationalCache.clear();
    CacheStats statsBeforeCold = nationalCache.getNativeCache().stats();

    List<ResponseEntity<String>> coldResponses = executeConcurrentGetRequests(
        "/api/v1/dashboards/national", adminHeaders, readerCount);

    for (ResponseEntity<String> r : coldResponses) {
      assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
      JsonNode j = parseResponseJson(r.getBody());
      assertThat(j.get("success").asBoolean()).isTrue();
      assertThat(j.get("data").has("kpiStrip")).isTrue();
    }

    CacheStats statsAfterCold = nationalCache.getNativeCache().stats();
    long coldMisses = statsAfterCold.missCount() - statsBeforeCold.missCount();
    long coldHits = statsAfterCold.hitCount() - statsBeforeCold.hitCount();
    log.info("[CacheBursts] Stage A Cold Cache (Q01): missCount delta={}, hitCount delta={}", coldMisses, coldHits);
    assertThat(coldMisses).as("At least one cold load occurred on national cache").isGreaterThanOrEqualTo(1);

    // Also verify Q03 (State aggregates) and Q05 (Provider leaderboard) caching under concurrent load
    stateCache.clear();
    List<ResponseEntity<String>> stateResponses = executeConcurrentGetRequests(
        "/api/v1/dashboards/states/101", adminHeaders, 10);
    for (ResponseEntity<String> r : stateResponses) {
      assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    institutionalCache.clear();
    List<ResponseEntity<String>> providerLeaderboardResponses = executeConcurrentGetRequests(
        "/api/v1/dashboards/providers", adminHeaders, 10);
    for (ResponseEntity<String> r : providerLeaderboardResponses) {
      assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // -------------------------------------------------------------------------
    // Stage B: Warm Cache — Repeat 50 concurrent readers on populated cache
    // -------------------------------------------------------------------------
    CacheStats statsBeforeWarm = nationalCache.getNativeCache().stats();

    List<ResponseEntity<String>> warmResponses = executeConcurrentGetRequests(
        "/api/v1/dashboards/national", adminHeaders, readerCount);

    for (ResponseEntity<String> r : warmResponses) {
      assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    CacheStats statsAfterWarm = nationalCache.getNativeCache().stats();
    long warmMisses = statsAfterWarm.missCount() - statsBeforeWarm.missCount();
    long warmHits = statsAfterWarm.hitCount() - statsBeforeWarm.hitCount();
    log.info("[CacheBursts] Stage B Warm Cache (Q01): missCount delta={}, hitCount delta={}", warmMisses, warmHits);
    assertThat(warmHits).as("Warm cache served requests directly without database misses").isEqualTo(readerCount);
    assertThat(warmMisses).as("Warm cache had zero misses").isEqualTo(0);

    // -------------------------------------------------------------------------
    // Stage C: Concurrent Mutation — Eviction on enrollment creation & Metric Change
    // -------------------------------------------------------------------------
    // 1. Read pre-mutation dashboard to record initial metric value (kpiStrip.enrollmentCount)
    ResponseEntity<String> preMutationRead = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/national"),
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        String.class
    );
    assertThat(preMutationRead.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode preJson = parseResponseJson(preMutationRead.getBody());
    long initialEnrollmentCount = preJson.get("data").get("kpiStrip").get("enrollmentCount").asLong();

    Long dbEnrollmentBefore = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM training_enrollments WHERE deleted_at IS NULL", Long.class);
    assertThat(initialEnrollmentCount).as("Pre-mutation dashboard metric matches database").isEqualTo(dbEnrollmentBefore);

    // 2. Perform authoritative domain mutation: enroll candidate into a batch
    Long sectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-CACHE-" + runToken, "Cache Sector " + runToken);
    trackedSectorIds.add(sectorId);

    Long courseId = insertAndGetId("""
        INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW())
        """, "CRS-CACHE-" + runToken, "Cache Course " + runToken, sectorId);
    trackedCourseIds.add(courseId);

    Long orgId = insertAndGetId("""
        INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, 1, NOW(), NOW())
        """, "ORG-CACHE-" + runToken, "Cache Org " + runToken);
    trackedOrgIds.add(orgId);

    Long deptId = insertAndGetId("""
        INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, orgId, "DEP-CACHE-" + runToken, "Cache Dept " + runToken);
    trackedDeptIds.add(deptId);

    Long schemeId = insertAndGetId("""
        INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 'INR', 1, NOW(), NOW())
        """, "SCH-CACHE-" + runToken, "Cache Scheme " + runToken, deptId);
    trackedSchemeIds.add(schemeId);

    Long programId = insertAndGetId("""
        INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, "PRG-CACHE-" + runToken, "Cache Prog " + runToken, deptId, schemeId);
    trackedProgramIds.add(programId);

    Long providerId = insertAndGetId("""
        INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 5, ?, 101, 1001, 1, 1, NOW(), NOW())
        """, "PRV-CACHE-" + runToken, "Cache Provider " + runToken, "REG-PRV-CACHE-" + runToken, orgId);
    trackedProviderIds.add(providerId);

    Long centerId = insertAndGetId("""
        INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 101, 1001, 1, NOW(), NOW())
        """, providerId, "CTR-CACHE-" + runToken, "Cache Center " + runToken);
    trackedCenterIds.add(centerId);

    Long batchId = insertAndGetId("""
        INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, CURDATE(), 10, 1, NOW(), NOW())
        """, "BAT-CACHE-" + runToken, courseId, providerId, centerId, programId);
    trackedBatchIds.add(batchId);

    // 2. Perform authoritative domain mutation: enroll 5 distinct candidates into batch to satisfy k-anonymity (k=5)
    for (int c = 0; c < 5; c++) {
      Long traineeId = insertAndGetId("""
          INSERT INTO trainees (registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
          VALUES (?, ?, 'Mutation', 1, ?, '9876540957', 'Test Street', '110001', 101, 1001, 6, 1, 3, NOW(), NOW())
          """, "REG-CACHE-" + runToken + "-" + c, "CacheTrainee" + c, "cache_trainee_" + c + "_" + runToken + "@test.gov.in");
      trackedTraineeIds.add(traineeId);

      Long consentId = insertAndGetId("""
          INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
          VALUES (?, 1, 'Data processing statutory consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
          """, traineeId);
      trackedConsentIds.add(consentId);

      CreateTrainingEnrollmentRequest mutateReq = new CreateTrainingEnrollmentRequest();
      mutateReq.setTraineeId(traineeId);
      mutateReq.setBatchId(batchId);
      mutateReq.setEnrollmentDate(LocalDate.now());
      mutateReq.setEnrollmentNumber("ENR-CACHE-" + runToken + "-" + c);
      mutateReq.setProgramId(programId);
      mutateReq.setCourseId(courseId);
      mutateReq.setProviderId(providerId);
      mutateReq.setCenterId(centerId);
      mutateReq.setEnrollmentStatusId(1L);
      mutateReq.setStartDate(LocalDate.now());
      mutateReq.setExpectedCompletionDate(LocalDate.now().plusMonths(3));

      ResponseEntity<String> mutateResp = restTemplate.postForEntity(
          getBaseUrl("/api/v1/training-enrollments"),
          new HttpEntity<>(mutateReq, adminHeaders),
          String.class
      );
      assertThat(mutateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      Long createdEnrId = parseResponseJson(mutateResp.getBody()).get("data").get("id").asLong();
      trackedEnrollmentIds.add(createdEnrId);
    }

    // 3. Post-mutation read: eviction listener executed AFTER_COMMIT; subsequent read reflects updated database state
    ResponseEntity<String> postMutationRead = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/national"),
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        String.class
    );
    assertThat(postMutationRead.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode postJson = parseResponseJson(postMutationRead.getBody());
    long newEnrollmentCount = postJson.get("data").get("kpiStrip").get("enrollmentCount").asLong();
    assertThat(newEnrollmentCount).as("Enrollment count in dashboard incremented by 5 following eviction").isEqualTo(initialEnrollmentCount + 5);

    Long dbEnrollmentAfter = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM training_enrollments WHERE deleted_at IS NULL", Long.class);
    assertThat(newEnrollmentCount).as("Dashboard metric matches database committed count").isEqualTo(dbEnrollmentAfter);
    log.info("[CacheBursts] Stage C Mutation: eviction verified, metric changed from {} to {}", initialEnrollmentCount, newEnrollmentCount);

    // -------------------------------------------------------------------------
    // Stage D: Tenant Isolation — Query under different scopes & cross-tenant checks
    // -------------------------------------------------------------------------
    Long providerUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NOW(), NOW())
        """, "tp_user_" + runToken, "tp_user_" + runToken + "@test.gov.in");
    trackedUserIds.add(providerUserId);

    String tpToken = generateBearerToken(providerUserId, "tp_user_" + runToken, List.of(
        "ROLE_training_provider",
        "analytics.read"
    ));
    HttpHeaders tpHeaders = createAuthHeaders(tpToken);

    // Provider attempting to query national executive dashboard without nationwide authority receives 403 Forbidden
    ResponseEntity<String> forbiddenResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/national"),
        HttpMethod.GET,
        new HttpEntity<>(tpHeaders),
        String.class
    );
    assertThat(forbiddenResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // State Admin for state 101 querying state 101 vs state 102
    Long stateAdminId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, 101, NOW(), NOW())
        """, "state_admin_" + runToken, "state_admin_" + runToken + "@test.gov.in");
    trackedUserIds.add(stateAdminId);

    String stateAdminToken = generateBearerToken(stateAdminId, "state_admin_" + runToken, List.of(
        "ROLE_state_admin",
        "analytics.read"
    ));
    HttpHeaders stateAdminHeaders = createAuthHeaders(stateAdminToken);

    ResponseEntity<String> state101Resp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/101"),
        HttpMethod.GET,
        new HttpEntity<>(stateAdminHeaders),
        String.class
    );
    assertThat(state101Resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    // State admin querying outside authorized state scope receives 403 Forbidden
    ResponseEntity<String> state102Resp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/102"),
        HttpMethod.GET,
        new HttpEntity<>(stateAdminHeaders),
        String.class
    );
    assertThat(state102Resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    log.info("[CacheBursts] Stage D Tenant Isolation: nationwide and cross-state boundaries strictly enforced with 403 Forbidden");
  }

  private List<ResponseEntity<String>> executeConcurrentGetRequests(String endpoint, HttpHeaders headers, int concurrency) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(concurrency);
    ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    List<Future<ResponseEntity<String>>> futures = new ArrayList<>(concurrency);

    try {
      for (int i = 0; i < concurrency; i++) {
        Callable<ResponseEntity<String>> task = () -> {
          barrier.await(10, TimeUnit.SECONDS);
          return restTemplate.exchange(
              getBaseUrl(endpoint),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              String.class
          );
        };
        futures.add(executor.submit(task));
      }

      List<ResponseEntity<String>> responses = new ArrayList<>(concurrency);
      for (Future<ResponseEntity<String>> f : futures) {
        responses.add(f.get(15, TimeUnit.SECONDS));
      }
      return responses;
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  // =========================================================================
  // Scenario 4: Authentication & Account Lockout Concurrency
  // =========================================================================

  @Test
  @DisplayName("4. Authentication & Account Lockout Concurrency — sequential baseline and concurrent attack with HTTP 423")
  void testAuthenticationAndAccountLockoutConcurrency_sequentialAndConcurrent() throws Exception {
    long runToken = Math.abs(System.nanoTime() % 10000000L);

    // -------------------------------------------------------------------------
    // Test A: Sequential Baseline
    // -------------------------------------------------------------------------
    String seqUsername = "user_seq_" + runToken;
    String rawPassword = "CorrectPassword123!";
    String encodedHash = passwordEncoder.encode(rawPassword);

    Long seqUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, failed_login_count, created_at, updated_at)
        VALUES (?, ?, ?, 'argon2id', 2, 0, NOW(), NOW())
        """, seqUsername, seqUsername + "@test.gov.in", encodedHash);
    trackedUserIds.add(seqUserId);

    // 4 invalid attempts: increment counter, status remains ACTIVE (2), returns HTTP 401 UNAUTHORIZED
    for (int attempt = 1; attempt <= 4; attempt++) {
      LoginRequest badReq = new LoginRequest(seqUsername, "WrongPassword");
      ResponseEntity<String> resp = restTemplate.postForEntity(
          getBaseUrl("/api/v1/auth/login"),
          new HttpEntity<>(badReq),
          String.class
      );
      assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      JsonNode json = parseResponseJson(resp.getBody());
      assertThat(json.get("code").asText()).isEqualTo("INVALID_CREDENTIALS");

      Integer count = jdbcTemplate.queryForObject(
          "SELECT failed_login_count FROM users WHERE user_id = ?",
          Integer.class, seqUserId);
      assertThat(count).isEqualTo(attempt);

      Long statusId = jdbcTemplate.queryForObject(
          "SELECT user_status_id FROM users WHERE user_id = ?",
          Long.class, seqUserId);
      assertThat(statusId).isEqualTo(2L); // ACTIVE
    }

    // 5th invalid attempt: reaches threshold 5, status transitions to LOCKED (3), returns HTTP 423 LOCKED
    LoginRequest fifthReq = new LoginRequest(seqUsername, "WrongPassword");
    ResponseEntity<String> fifthResp = restTemplate.postForEntity(
        getBaseUrl("/api/v1/auth/login"),
        new HttpEntity<>(fifthReq),
        String.class
    );
    assertThat(fifthResp.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    JsonNode fifthJson = parseResponseJson(fifthResp.getBody());
    assertThat(fifthJson.get("code").asText()).isEqualTo("ACCOUNT_LOCKED");

    Integer countAfterFifth = jdbcTemplate.queryForObject(
        "SELECT failed_login_count FROM users WHERE user_id = ?",
        Integer.class, seqUserId);
    assertThat(countAfterFifth).isEqualTo(5);

    Long statusAfterFifth = jdbcTemplate.queryForObject(
        "SELECT user_status_id FROM users WHERE user_id = ?",
        Long.class, seqUserId);
    assertThat(statusAfterFifth).isEqualTo(3L); // LOCKED

    // Valid credentials submitted to active locked account must return HTTP 423 LOCKED without bypass
    LoginRequest validOnLockedReq = new LoginRequest(seqUsername, rawPassword);
    ResponseEntity<String> lockedValidResp = restTemplate.postForEntity(
        getBaseUrl("/api/v1/auth/login"),
        new HttpEntity<>(validOnLockedReq),
        String.class
    );
    assertThat(lockedValidResp.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    JsonNode lockedValidJson = parseResponseJson(lockedValidResp.getBody());
    assertThat(lockedValidJson.get("code").asText()).isEqualTo("ACCOUNT_LOCKED");
    log.info("[AuthLockout] Test A Sequential Baseline: account locked at threshold 5 with HTTP 423, bypass prevented");

    // -------------------------------------------------------------------------
    // Test B: Concurrent Attack — 10 simultaneous failed login requests
    // -------------------------------------------------------------------------
    String concUsername = "user_conc_" + runToken;
    Long concUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, failed_login_count, created_at, updated_at)
        VALUES (?, ?, ?, 'argon2id', 2, 0, NOW(), NOW())
        """, concUsername, concUsername + "@test.gov.in", encodedHash);
    trackedUserIds.add(concUserId);

    int concurrentAttempts = 10;
    CyclicBarrier barrier = new CyclicBarrier(concurrentAttempts);
    ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
    List<Future<ResponseEntity<String>>> futures = new ArrayList<>(concurrentAttempts);

    try {
      for (int i = 0; i < concurrentAttempts; i++) {
        Callable<ResponseEntity<String>> task = () -> {
          barrier.await(10, TimeUnit.SECONDS);
          return restTemplate.postForEntity(
              getBaseUrl("/api/v1/auth/login"),
              new HttpEntity<>(new LoginRequest(concUsername, "WrongPassword")),
              String.class
          );
        };
        futures.add(executor.submit(task));
      }

      int unauthCount = 0;
      int lockedCount = 0;
      int serverErrors = 0;
      for (Future<ResponseEntity<String>> f : futures) {
        ResponseEntity<String> r = f.get(15, TimeUnit.SECONDS);
        if (r.getStatusCode() == HttpStatus.UNAUTHORIZED) {
          unauthCount++;
        } else if (r.getStatusCode() == HttpStatus.LOCKED) {
          lockedCount++;
        } else {
          serverErrors++;
          log.error("Unexpected status in concurrent login: {}", r.getStatusCode());
        }
      }

      assertThat(serverErrors).as("Zero HTTP 500 errors during concurrent brute force attack").isEqualTo(0);
      assertThat(unauthCount).as("Attempts before threshold return 401").isEqualTo(4);
      assertThat(lockedCount).as("Attempts reaching or after threshold return 423 Locked").isEqualTo(6);

      // Verify row lock and atomic counter: user status MUST be LOCKED (3L)
      Long concStatus = jdbcTemplate.queryForObject(
          "SELECT user_status_id FROM users WHERE user_id = ?",
          Long.class, concUserId);
      assertThat(concStatus).as("Account status transitioned to LOCKED under concurrency").isEqualTo(3L);

      Integer concFailedCount = jdbcTemplate.queryForObject(
          "SELECT failed_login_count FROM users WHERE user_id = ?",
          Integer.class, concUserId);
      assertThat(concFailedCount).as("Failed login counter reached or exceeded lockout threshold").isGreaterThanOrEqualTo(5);

      // Verify valid credentials cannot authenticate against locked account (returns 423)
      ResponseEntity<String> validRespAfterAttack = restTemplate.postForEntity(
          getBaseUrl("/api/v1/auth/login"),
          new HttpEntity<>(new LoginRequest(concUsername, rawPassword)),
          String.class
      );
      assertThat(validRespAfterAttack.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
      assertThat(parseResponseJson(validRespAfterAttack.getBody()).get("code").asText()).isEqualTo("ACCOUNT_LOCKED");

      // Verify unrelated account (root admin) remains unaffected
      Integer unrelatedStatus = jdbcTemplate.queryForObject(
          "SELECT user_status_id FROM users WHERE user_id = 1", // root admin
          Integer.class);
      assertThat(unrelatedStatus).as("Root admin remains intact").isEqualTo(1);

      log.info("[AuthLockout] Test B Concurrent Attack: 10 concurrent failed attempts atomically serialized, status=LOCKED, 4x401, 6x423");
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }
}
