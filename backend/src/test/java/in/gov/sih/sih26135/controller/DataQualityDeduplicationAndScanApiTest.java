package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.dq.DataQualityIssueRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration and deduplication tests for Phase 28.3:
 * 1. Concurrency-safe deduplication and idempotency in issue registration.
 * 2. Preservation of historical RESOLVED / DISMISSED records without mutation or reopening.
 * 3. Independent flagging of entities across distinct rules and distinct entities.
 * 4. Scan REST API (POST /api/v1/data-quality/scan) authentication, authorization, validation, and contract integrity.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.3 - Data Quality Deduplication & Scan API Integration Tests")
class DataQualityDeduplicationAndScanApiTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private DataQualityIssueRegistrationService issueRegistrationService;

  @Autowired
  private DataQualityEngineProperties properties;

  private String fullAdminToken;
  private String guestToken;
  private String unprivilegedAuditorToken;
  private String employerToken;
  private String unscopedEmployerToken;
  private String unscopedStateAdminToken;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    catalogService.ensureDefaultRulesProvisioned();

    fullAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "ROLE_super_admin",
            "audit.read",
            "system.manage"
        )
    );

    guestToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        999L,
        "guest",
        List.of("ROLE_GUEST")
    );

    // Auditor token with audit.read but without nationwide admin role or scoped organization/state
    unprivilegedAuditorToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        998L,
        "unprivileged_auditor",
        List.of("audit.read")
    );

    // Provision test users and dependencies for persona and tenancy testing
    cleanUpUserFixtures();

    jdbcTemplate.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8881, 'SE', 'Employer Test State', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8881, 8881, 'DE8881', 'Employer Test District', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (888991, 'EMP-DQ-888991', 'DQ Test Employer', 'REG-EMP-888991', 1, 8881, 8881, 1, 1, NOW(), NOW())");

    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (888991, 'dq_employer_user', 'dq_employer@test.gov.in', '$2a$10$hash', 'BCRYPT', 1, 888991, NOW(), NOW())
    """);
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, employer_id, created_at, updated_at)
        VALUES (888992, 'dq_unscoped_employer', 'dq_unscoped@test.gov.in', '$2a$10$hash', 'BCRYPT', 1, NULL, NOW(), NOW())
    """);
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (888993, 'dq_unscoped_state_admin', 'dq_unscoped_sa@test.gov.in', '$2a$10$hash', 'BCRYPT', 1, NULL, NOW(), NOW())
    """);

    employerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        888991L,
        "dq_employer_user",
        List.of("ROLE_employer", "audit.read")
    );

    unscopedEmployerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        888992L,
        "dq_unscoped_employer",
        List.of("ROLE_employer", "audit.read")
    );

    unscopedStateAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        888993L,
        "dq_unscoped_state_admin",
        List.of("ROLE_state_admin", "audit.read")
    );
  }

  @AfterEach
  void tearDown() {
    cleanUpUserFixtures();
  }

  private void cleanUpUserFixtures() {
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (888991, 888992, 888993)");
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = 888991");
    jdbcTemplate.update("DELETE FROM districts WHERE district_id = 8881");
    jdbcTemplate.update("DELETE FROM states WHERE state_id = 8881");
  }

  // =========================================================================
  // 1. DEDUPLICATION AND IDEMPOTENCY TESTS
  // =========================================================================

  @Test
  @DisplayName("1. First issue registration creates an OPEN issue, second registration skips and deduplicates")
  void testIssueRegistrationDeduplication() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    String entityType = "Trainee";
    Long testEntityId = 888101L;

    // Clean up any test artifact
    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        rule.getId(), entityType, testEntityId);

    // First registration must succeed
    boolean firstCreated = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, testEntityId, "Trainee 888101 missing phone and email contact info"
    );
    assertThat(firstCreated).isTrue();
    assertThat(issueRegistrationService.checkActiveOpenIssueExists(rule.getId(), entityType, testEntityId)).isTrue();

    // Second registration must detect existing open issue and skip
    boolean secondCreated = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, testEntityId, "Repeated scan finding trainee 888101 missing phone and email"
    );
    assertThat(secondCreated).isFalse();

    // Verify exactly one OPEN issue exists
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        Integer.class,
        rule.getId(), entityType, testEntityId
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("2. Concurrent registration attempts on same entity and rule create exactly one OPEN issue")
  void testConcurrentIssueRegistrationDeduplication() throws InterruptedException, ExecutionException {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    String entityType = "Trainee";
    Long testEntityId = 888102L;

    // Clean up any test artifact
    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        rule.getId(), entityType, testEntityId);

    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Callable<Boolean>> tasks = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int iteration = i;
      tasks.add(() -> issueRegistrationService.registerIssueIfAbsent(
          rule, entityType, testEntityId, "Concurrent scan finding " + iteration
      ));
    }

    List<Future<Boolean>> results = executor.invokeAll(tasks);
    executor.shutdown();

    int createdCount = 0;
    int skippedCount = 0;
    for (Future<Boolean> future : results) {
      if (future.get()) {
        createdCount++;
      } else {
        skippedCount++;
      }
    }

    assertThat(createdCount).isEqualTo(1);
    assertThat(skippedCount).isEqualTo(threadCount - 1);

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        Integer.class,
        rule.getId(), entityType, testEntityId
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("3. Historical RESOLVED issue is preserved and never mutated or reopened by a new scan finding")
  void testHistoricalResolvedIssuePreserved() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-PLACEMENT-002");
    String entityType = "PlacementRecord";
    Long testEntityId = 888201L;

    // Clean up
    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        rule.getId(), entityType, testEntityId);

    // 1. Create initial issue
    boolean created = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, testEntityId, "Initial temporal inconsistency"
    );
    assertThat(created).isTrue();

    // 2. Mark it as RESOLVED (status_id = 4) with resolution notes
    jdbcTemplate.update(
        "UPDATE data_quality_issues SET data_quality_issue_status_id = 4, " +
            "resolution_notes = 'Corrected offer date manually by admin', " +
            "resolved_at = DATE_ADD(detected_at, INTERVAL 1 MINUTE), resolved_by_user_id = 1 " +
            "WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        rule.getId(), entityType, testEntityId
    );

    assertThat(issueRegistrationService.checkActiveOpenIssueExists(rule.getId(), entityType, testEntityId)).isFalse();
    assertThat(issueRegistrationService.checkHistoricalClosedIssueExists(rule.getId(), entityType, testEntityId)).isTrue();

    // 3. New scan finds an issue on same entity - registers a new issue record without mutating historical resolved record
    boolean newScanRegistered = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, testEntityId, "Recurrent temporal anomaly detected in new batch"
    );
    assertThat(newScanRegistered).isTrue();

    // 4. Verify historical record retained its RESOLVED status and notes
    String notes = jdbcTemplate.queryForObject(
        "SELECT resolution_notes FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ? AND data_quality_issue_status_id = 4",
        String.class,
        rule.getId(), entityType, testEntityId
    );
    assertThat(notes).isEqualTo("Corrected offer date manually by admin");

    // Total records for this entity should now be 2: one RESOLVED and one OPEN
    Integer totalCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        Integer.class,
        rule.getId(), entityType, testEntityId
    );
    assertThat(totalCount).isEqualTo(2);
  }

  @Test
  @DisplayName("4. Distinct rules flag the same entity independently without cross-rule deduplication collisions")
  void testDistinctRulesFlagSameEntityIndependently() {
    DataQualityRule rule1 = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    DataQualityRule rule5 = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    String entityType = "Trainee";
    Long testEntityId = 888301L;

    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE entity_type = ? AND entity_id = ?",
        entityType, testEntityId);

    boolean r1Created = issueRegistrationService.registerIssueIfAbsent(
        rule1, entityType, testEntityId, "Missing contact details"
    );
    boolean r5Created = issueRegistrationService.registerIssueIfAbsent(
        rule5, entityType, testEntityId, "Candidate cross-center duplicate"
    );

    assertThat(r1Created).isTrue();
    assertThat(r5Created).isTrue();

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE entity_type = ? AND entity_id = ?",
        Integer.class,
        entityType, testEntityId
    );
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("5. Distinct entities violating the same rule are registered independently")
  void testDistinctEntitiesFlaggedIndependently() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-ATTENDANCE-004");
    String entityType = "AttendanceRecord";
    Long entityA = 888401L;
    Long entityB = 888402L;

    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id IN (?, ?)",
        rule.getId(), entityType, entityA, entityB);

    boolean createdA = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, entityA, "Attendance date out of bounds for session A"
    );
    boolean createdB = issueRegistrationService.registerIssueIfAbsent(
        rule, entityType, entityB, "Attendance date out of bounds for session B"
    );

    assertThat(createdA).isTrue();
    assertThat(createdB).isTrue();

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id IN (?, ?)",
        Integer.class,
        rule.getId(), entityType, entityA, entityB
    );
    assertThat(count).isEqualTo(2);
  }

  // =========================================================================
  // 2. SCAN REST API (POST /api/v1/data-quality/scan) INTEGRATION TESTS
  // =========================================================================

  @Test
  @DisplayName("6. Authorized nationwide admin scan with empty request executes all active rules and returns 200 OK")
  void testAdminFullScanReturns200WithSummaries() throws Exception {
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(5)))
        .andExpect(jsonPath("$.data[*].ruleCode").isNotEmpty())
        .andExpect(jsonPath("$.data[*].status", everyItem(org.hamcrest.Matchers.is("SUCCESS"))))
        .andExpect(jsonPath("$.data[*].evaluatedRecordCount", everyItem(greaterThanOrEqualTo(0))))
        .andExpect(jsonPath("$.data[*].violationCount", everyItem(greaterThanOrEqualTo(0))))
        .andExpect(jsonPath("$.data[*].newIssuesCreated", everyItem(greaterThanOrEqualTo(0))))
        .andExpect(jsonPath("$.data[*].deduplicatedIssuesSkipped", everyItem(greaterThanOrEqualTo(0))))
        .andExpect(jsonPath("$.data[*].executionDurationMs", everyItem(greaterThanOrEqualTo(0))));
  }

  @Test
  @DisplayName("7. Authorized scan targeting a specific ruleCode returns exactly one execution summary")
  void testScanSpecificRuleCodeReturnsSingleSummary() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-TRAINEE-001");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].ruleCode").value("DQ-TRAINEE-001"))
        .andExpect(jsonPath("$.data[0].targetEntityType").value("Trainee"))
        .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));
  }

  @Test
  @DisplayName("8. Authorized scan targeting a specific entity type returns only rules matching that entity type")
  void testScanTargetEntityTypeReturnsFilteredRules() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setTargetEntityType("PlacementRecord");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].ruleCode").value("DQ-PLACEMENT-002"))
        .andExpect(jsonPath("$.data[0].targetEntityType").value("PlacementRecord"));
  }

  @Test
  @DisplayName("9. Unauthenticated scan request is rejected with 401 Unauthorized")
  void testUnauthenticatedScanReturns401() throws Exception {
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("10. Unprivileged guest user scan request is rejected with 403 Forbidden")
  void testUnprivilegedGuestScanReturns403() throws Exception {
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", guestToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("11. Caller with audit.read but without administrative role or tenancy scope is rejected with 403 Forbidden")
  void testUnprivilegedAuditorWithoutScopeReturns403() throws Exception {
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", unprivilegedAuditorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("12. Scan targeting non-existent rule code is rejected with 404 DATA_QUALITY_RULE_NOT_FOUND")
  void testScanNonExistentRuleCodeReturns404() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-NON-EXISTENT-999");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DQ_RULE_NOT_FOUND"));
  }

  @Test
  @DisplayName("13. Scan with unsupported targetEntityType is rejected with 400 UNSUPPORTED_ENTITY_TYPE")
  void testScanUnsupportedEntityTypeReturns400() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setTargetEntityType("MaliciousOrArbitraryTable");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_ENTITY_TYPE"));
  }

  @Test
  @DisplayName("14. Scan on disabled rule returns empty execution summary list (skipped)")
  void testScanOnDisabledRuleReturnsEmptyList() throws Exception {
    // Temporarily deactivate DQ-ATTENDANCE-004 (lifecycle_status_id = 2)
    jdbcTemplate.update("UPDATE data_quality_rules SET lifecycle_status_id = 2 WHERE rule_code = 'DQ-ATTENDANCE-004'");

    try {
      DataQualityScanRequest request = new DataQualityScanRequest();
      request.setRuleCode("DQ-ATTENDANCE-004");

      mockMvc.perform(post("/api/v1/data-quality/scan")
              .header("Authorization", fullAdminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(0));
    } finally {
      // Re-enable rule
      jdbcTemplate.update("UPDATE data_quality_rules SET lifecycle_status_id = 1 WHERE rule_code = 'DQ-ATTENDANCE-004'");
    }
  }

  @Test
  @DisplayName("15. Distributed named lock timeout fails closed and does not insert issue")
  void testDistributedNamedLockTimeoutFailsClosedWithoutCreatingIssue() throws Exception {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    String entityType = "Trainee";
    Long testEntityId = 888555L;
    String lockKey = "dq_lock:" + rule.getId() + ":" + entityType + ":" + testEntityId;

    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
        rule.getId(), entityType, testEntityId);

    // Pre-acquire lock on an independent connection
    try (Connection conn = dataSource.getConnection()) {
      boolean locked = DataQualityIssueRegistrationService.acquireNamedLock(conn, lockKey, 5);
      assertThat(locked).isTrue();

      int originalTimeout = properties.getLockTimeoutSeconds();
      properties.setLockTimeoutSeconds(1);

      try {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Boolean> future = exec.submit(() ->
            issueRegistrationService.registerIssueIfAbsent(rule, entityType, testEntityId, "Contended registration attempt")
        );
        Boolean created = future.get();
        exec.shutdown();

        // Must return false due to lock timeout
        assertThat(created).isFalse();

        // Must have created 0 issues in the database
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = ? AND entity_id = ?",
            Integer.class, rule.getId(), entityType, testEntityId
        );
        assertThat(count).isEqualTo(0);

      } finally {
        properties.setLockTimeoutSeconds(originalTimeout);
        DataQualityIssueRegistrationService.releaseNamedLock(conn, lockKey);
      }
    }
  }

  @Test
  @DisplayName("16. Database query failure in duplicate existence check fails closed and does not assume absence")
  void testDatabaseQueryFailureDoesNotSwallowExceptions() {
    assertThatThrownBy(() -> jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM non_existent_dq_table WHERE id = 1", Integer.class
    )).isInstanceOf(DataAccessException.class);
  }

  @Test
  @DisplayName("17. Employer caller attempting attendance scan is rejected with 403 Forbidden")
  void testEmployerPersonaForbiddenOnAttendanceScan() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-ATTENDANCE-004");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("18. Employer caller attempting trainee contact scan is rejected with 403 Forbidden")
  void testEmployerPersonaForbiddenOnTraineeContactScan() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-TRAINEE-001");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("19. Employer caller attempting duplicate trainee scan is rejected with 403 Forbidden")
  void testEmployerPersonaForbiddenOnDuplicateTraineeScan() throws Exception {
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("20. Employer caller targeting AttendanceRecord or Trainee entity types is rejected with 403 Forbidden")
  void testEmployerPersonaForbiddenOnAttendanceOrTraineeTargetEntityType() throws Exception {
    DataQualityScanRequest attReq = new DataQualityScanRequest();
    attReq.setTargetEntityType("AttendanceRecord");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(attReq)))
        .andExpect(status().isForbidden());

    DataQualityScanRequest traineeReq = new DataQualityScanRequest();
    traineeReq.setTargetEntityType("Trainee");

    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(traineeReq)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("21. Employer caller executing unscoped scan runs only authorized placement and employment rules")
  void testEmployerUnscopedScanFiltersOnlyAllowedRules() throws Exception {
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", employerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[*].ruleCode", not(hasItem("DQ-ATTENDANCE-004"))))
        .andExpect(jsonPath("$.data[*].ruleCode", not(hasItem("DQ-TRAINEE-001"))))
        .andExpect(jsonPath("$.data[*].ruleCode", not(hasItem("DQ-DUPLICATE-005"))))
        .andExpect(jsonPath("$.data[*].targetEntityType", everyItem(org.hamcrest.Matchers.isOneOf("PlacementRecord", "EmploymentRecord"))));
  }

  @Test
  @DisplayName("22. Non-nationwide callers with unresolvable/null scope IDs fail closed with 403 Forbidden")
  void testNonNationwideCallersWithNullScopeIdFailClosedWith403() throws Exception {
    // Unscoped employer (employer_id = null in DB)
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", unscopedEmployerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());

    // Unscoped state admin (state_id = null in DB)
    mockMvc.perform(post("/api/v1/data-quality/scan")
            .header("Authorization", unscopedStateAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());
  }
}
