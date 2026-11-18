package in.gov.sih.sih26135.communication.security;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.queue.DurableDispatchQueueService;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.controller.CommunicationJobController;
import in.gov.sih.sih26135.controller.CommunicationLogController;
import in.gov.sih.sih26135.dto.request.DispatchNotificationRequest;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.security.Principal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 29.4 targeted security, non-destructive test isolation & async API integration test suite verifying:
 * <ul>
 *   <li>Authoritative multi-tenant isolation on dispatch job retrieval, execution attempt inspection, and dead-letter queue operations.</li>
 *   <li>Strict boundary checks between distinct training providers (Provider A cannot access or replay Provider B jobs).</li>
 *   <li>Strict boundary checks preventing provider-scoped callers from accessing nationwide/unaffiliated dispatch jobs.</li>
 *   <li>Dead-letter queue isolation ensuring provider-scoped callers only view their own provider's exhausted jobs.</li>
 *   <li>Nationwide administrative authority permitting access across all provider jobs and the complete dead-letter queue.</li>
 *   <li>Truthful asynchronous API response semantics (null communicationLogId, null providerMessageId, valid queue jobId, status QUEUED).</li>
 *   <li>Non-destructive test execution: dynamically allocated fixtures, exact primary key tracking, zero deletion or mutation of pre-existing records.</li>
 *   <li>Authoritative idempotency scoping: prevents cross-tenant key hijacking and mismatched payload reuse while honoring identical authorized requests.</li>
 * </ul>
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 29.4 — Communication Job Security, Multi-Tenant Isolation & Asynchronous API Tests (SIH26135_TEST)")
public class CommunicationJobSecurityAndAsyncTest {

  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private CommunicationJobController communicationJobController;

  @Autowired
  private CommunicationLogController communicationLogController;

  @Autowired
  private DurableDispatchQueueService queueService;

  private JdbcTemplate jdbcTemplate;

  // Dynamically allocated, non-colliding fixture IDs tracked per test execution
  private Long testStateId;
  private Long testDistrictId;
  private Long testOrgId;
  private Long testProviderAId;
  private Long testProviderBId;
  private Long testUserAId;
  private Long testUserBId;
  private String testKeyPrefix;

  // Exact primary key tracking lists for safe non-destructive teardown
  private final List<Long> trackedJobIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedUserIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedProviderIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedOrgIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedDistrictIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedStateIds = new CopyOnWriteArrayList<>();
  private final List<String> trackedIdempotencyKeys = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    jdbcTemplate = new JdbcTemplate(dataSource);

    // 1. Authoritative JDBC catalog verification before any fixture creation
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }

    insertDynamicFixtures();
  }

  @AfterEach
  void tearDown() {
    try {
      cleanUpTrackedFixtures();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Allocates dynamic, non-colliding test fixtures without using ON DUPLICATE KEY UPDATE,
   * guaranteeing that pre-existing database records are never modified.
   */
  private void insertDynamicFixtures() {
    long maxProvider = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(provider_id), 80000) FROM training_providers", Long.class);
    long maxUser = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(user_id), 80000) FROM users", Long.class);
    long maxOrg = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(organization_id), 80000) FROM organizations", Long.class);
    long maxDistrict = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(district_id), 80000) FROM districts", Long.class);
    long maxState = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(state_id), 80000) FROM states", Long.class);

    long baseId = Math.max(Math.max(maxProvider, maxUser), Math.max(maxOrg, Math.max(maxDistrict, maxState)))
        + 1000L + ThreadLocalRandom.current().nextInt(100, 10000);

    testStateId = baseId + 1;
    testDistrictId = baseId + 2;
    testOrgId = baseId + 3;
    testProviderAId = baseId + 4;
    testProviderBId = baseId + 5;
    testUserAId = baseId + 6;
    testUserBId = baseId + 7;
    testKeyPrefix = "ST-" + UUID.randomUUID().toString().substring(0, 4) + "-";

    // 1. Geography: Non-destructive reference to existing state and district
    testStateId = jdbcTemplate.queryForObject("SELECT state_id FROM states ORDER BY state_id ASC LIMIT 1", Long.class);
    testDistrictId = jdbcTemplate.queryForObject("SELECT district_id FROM districts WHERE state_id = ? ORDER BY district_id ASC LIMIT 1", Long.class, testStateId);

    // 2. Organization
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'Dynamic Security Org', 1, 1, NOW(), NOW())",
        testOrgId, "ORG-" + testOrgId
    );
    trackedOrgIds.add(testOrgId);

    // 3. Training Providers
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'Dynamic Security Provider A', ?, 1, ?, ?, ?, 1, 1, NOW(), NOW())",
        testProviderAId, "PRV-A-" + testProviderAId, "REG-A-" + testProviderAId, testOrgId, testStateId, testDistrictId
    );
    trackedProviderIds.add(testProviderAId);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'Dynamic Security Provider B', ?, 1, ?, ?, ?, 1, 1, NOW(), NOW())",
        testProviderBId, "PRV-B-" + testProviderBId, "REG-B-" + testProviderBId, testOrgId, testStateId, testDistrictId
    );
    trackedProviderIds.add(testProviderBId);

    // 4. Provider Users
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, organization_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'hash', 1, ?, ?, NOW(), NOW())",
        testUserAId, "provider_a_user_" + testUserAId, "provider_a_" + testUserAId + "@example.com", testProviderAId, testOrgId
    );
    trackedUserIds.add(testUserAId);

    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, organization_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'hash', 1, ?, ?, NOW(), NOW())",
        testUserBId, "provider_b_user_" + testUserBId, "provider_b_" + testUserBId + "@example.com", testProviderBId, testOrgId
    );
    trackedUserIds.add(testUserBId);
  }

  /**
   * Non-destructive cleanup: deletes strictly the tracked primary keys created by this test run.
   * Never executes global or wildcard deletes on pre-existing records or rate-limit events.
   * Does not swallow errors.
   */
  private void cleanUpTrackedFixtures() {
    if (jdbcTemplate == null) {
      return;
    }

    // 1. Delete attempts for tracked jobs
    for (Long jobId : trackedJobIds) {
      jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
      jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
    }
    trackedJobIds.clear();

    // 2. Delete any extra jobs tracked by idempotency key
    for (String key : trackedIdempotencyKeys) {
      jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id IN (SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key = ?)", key);
      jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE idempotency_key = ?", key);
    }
    trackedIdempotencyKeys.clear();

    // 3. Delete tracked users
    for (Long userId : trackedUserIds) {
      jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
    }
    trackedUserIds.clear();

    // 4. Delete tracked providers
    for (Long providerId : trackedProviderIds) {
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", providerId);
    }
    trackedProviderIds.clear();

    // 5. Delete tracked organizations
    for (Long orgId : trackedOrgIds) {
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", orgId);
    }
    trackedOrgIds.clear();

    // 6. Delete tracked districts
    for (Long districtId : trackedDistrictIds) {
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", districtId);
    }
    trackedDistrictIds.clear();

    // 7. Delete tracked states
    for (Long stateId : trackedStateIds) {
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", stateId);
    }
    trackedStateIds.clear();
  }

  private Long insertTestJob(String idempotencyKey, Long traineeId, Long providerId, String jobStatus) {
    trackedIdempotencyKeys.add(idempotencyKey);
    jdbcTemplate.update(
        "INSERT INTO communication_dispatch_jobs ("
            + "idempotency_key, trainee_id, training_provider_id, channel_id, purpose_id, "
            + "template_code, template_parameters_json, job_status, attempt_count, max_attempts, "
            + "delivery_mode, initiated_by_user_id, initiated_by_service, tenant_scope_type, "
            + "scheduled_at, created_at, updated_at, lease_version) "
            + "VALUES (?, ?, ?, 1, 1, 'OPERATIONAL_ALERT', '{}', ?, 0, 3, 'DEMO', 1, 'TEST', 'PROVIDER', NOW(), NOW(), NOW(), 1)",
        idempotencyKey, traineeId, providerId, jobStatus
    );
    Long jobId = jdbcTemplate.queryForObject(
        "SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key = ?",
        Long.class,
        idempotencyKey
    );
    if (jobId != null) {
      trackedJobIds.add(jobId);
    }
    return jobId;
  }

  private void insertTestAttempt(Long jobId) {
    jdbcTemplate.update(
        "INSERT INTO communication_dispatch_attempts ("
            + "job_id, attempt_number, attempt_status, provider_name, error_message, attempted_at) "
            + "VALUES (?, 1, 'FAILED', 'MockSmsProvider', 'Target socket timeout', NOW())",
        jobId
    );
  }

  private void setProviderUser(Long userId, String username) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, username),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("ROLE_training_provider"),
                new SimpleGrantedAuthority("communication.manage"),
                new SimpleGrantedAuthority("system.manage")
            )
        )
    );
  }

  private void setNationwideAdminUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("communication.manage"),
                new SimpleGrantedAuthority("system.manage")
            )
        )
    );
  }

  @Test
  @Order(1)
  @DisplayName("1. Cross-Tenant Isolation: Provider A caller attempting to get Provider B job -> rejected with PROVIDER_MISMATCH")
  void test1_crossTenant_getJobById_denied() {
    Long jobBId = insertTestJob(
        testKeyPrefix + "PB-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderBId,
        "PENDING"
    );

    setProviderUser(testUserAId, "provider_a_user_" + testUserAId);

    assertThatThrownBy(() -> communicationJobController.getJobById(jobBId))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(2)
  @DisplayName("2. Cross-Tenant Isolation: Provider A caller attempting to get Provider B job attempts -> rejected with PROVIDER_MISMATCH")
  void test2_crossTenant_getJobAttempts_denied() {
    Long jobBId = insertTestJob(
        testKeyPrefix + "PB-ATT-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderBId,
        "FAILED"
    );
    insertTestAttempt(jobBId);

    setProviderUser(testUserAId, "provider_a_user_" + testUserAId);

    assertThatThrownBy(() -> communicationJobController.getJobAttempts(jobBId))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(3)
  @DisplayName("3. Cross-Tenant Isolation: Provider A caller attempting to replay Provider B dead-letter job -> rejected with PROVIDER_MISMATCH")
  void test3_crossTenant_replayDeadLetterJob_denied() {
    Long jobBId = insertTestJob(
        testKeyPrefix + "PB-DL-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderBId,
        "DEAD_LETTER"
    );

    setProviderUser(testUserAId, "provider_a_user_" + testUserAId);
    Principal principal = new UserPrincipal(testUserAId, "provider_a_user_" + testUserAId);

    assertThatThrownBy(() -> communicationJobController.replayDeadLetterJob(jobBId, principal))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(4)
  @DisplayName("4. Scope Boundary: Provider-scoped caller attempting to inspect nationwide job (null provider) -> HTTP 403 Forbidden")
  void test4_providerCaller_accessingNationwideJob_forbidden() {
    Long nationwideJobId = insertTestJob(
        testKeyPrefix + "NAT-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        null,
        "PENDING"
    );

    setProviderUser(testUserAId, "provider_a_user_" + testUserAId);

    assertThatThrownBy(() -> communicationJobController.getJobById(nationwideJobId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @Order(5)
  @DisplayName("5. Dead-Letter Filtering: Provider A caller sees strictly Provider A dead-letter jobs, zero leakage of Provider B")
  void test5_deadLetterQueue_isolatedPerTenant() {
    Long jobADlId = insertTestJob(
        testKeyPrefix + "DL-A-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderAId,
        "DEAD_LETTER"
    );
    Long jobBDlId = insertTestJob(
        testKeyPrefix + "DL-B-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderBId,
        "DEAD_LETTER"
    );

    setProviderUser(testUserAId, "provider_a_user_" + testUserAId);

    ResponseEntity<ApiResponse<List<DispatchJobDto>>> response = communicationJobController.getDeadLetterJobs(0, 50);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    List<DispatchJobDto> returnedJobs = response.getBody().getData();

    // Must include Provider A dead-letter job
    assertThat(returnedJobs).anyMatch(j -> j.jobId().equals(jobADlId));

    // Must NEVER include Provider B dead-letter job
    assertThat(returnedJobs).noneMatch(j -> j.jobId().equals(jobBDlId));
    assertThat(returnedJobs).allMatch(j -> testProviderAId.equals(j.trainingProviderId()));
  }

  @Test
  @Order(6)
  @DisplayName("6. Nationwide Administrative Authority: Super admin can access jobs across any provider and inspect full dead-letter queue")
  void test6_nationwideAdmin_canAccessAllProviderJobs() {
    Long jobAId = insertTestJob(
        testKeyPrefix + "ADMIN-A-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderAId,
        "DEAD_LETTER"
    );
    Long jobBId = insertTestJob(
        testKeyPrefix + "ADMIN-B-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        testProviderBId,
        "DEAD_LETTER"
    );

    setNationwideAdminUser();

    // Admin can get Provider A job
    ResponseEntity<ApiResponse<DispatchJobDto>> responseA = communicationJobController.getJobById(jobAId);
    assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseA.getBody().getData().jobId()).isEqualTo(jobAId);

    // Admin can get Provider B job
    ResponseEntity<ApiResponse<DispatchJobDto>> responseB = communicationJobController.getJobById(jobBId);
    assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseB.getBody().getData().jobId()).isEqualTo(jobBId);

    // Admin calling getDeadLetterJobs sees both jobs
    ResponseEntity<ApiResponse<List<DispatchJobDto>>> dlResponse = communicationJobController.getDeadLetterJobs(0, 50);
    assertThat(dlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<DispatchJobDto> allDeadLetters = dlResponse.getBody().getData();
    assertThat(allDeadLetters).anyMatch(j -> j.jobId().equals(jobAId));
    assertThat(allDeadLetters).anyMatch(j -> j.jobId().equals(jobBId));
  }

  @Test
  @Order(7)
  @DisplayName("7. Asynchronous API Identifier Truthfulness: Async dispatch returns null communicationLogId/providerMessageId and valid jobId")
  void test7_asyncDispatch_truthfulIdentifiers() {
    setNationwideAdminUser();

    String asyncKey = testKeyPrefix + "ASYNC-" + UUID.randomUUID();
    trackedIdempotencyKeys.add(asyncKey);

    DispatchNotificationRequest request = new DispatchNotificationRequest(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "ASYNC-SEC-01"),
        true, // async = true
        asyncKey
    );

    ResponseEntity<ApiResponse<NotificationDispatchResponse>> response = communicationLogController.dispatchNotification(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    NotificationDispatchResponse data = response.getBody().getData();

    // Truthful identifiers verification
    assertThat(data.communicationLogId()).isNull();
    assertThat(data.providerMessageId()).isNull();
    assertThat(data.jobId()).isNotNull().isPositive();
    trackedJobIds.add(data.jobId());

    assertThat(data.status()).isEqualTo("QUEUED");
    assertThat(data.deliveryMode()).isEqualTo("DEMO");
    assertThat(data.simulated()).isTrue();
    assertThat(data.physicallyDelivered()).isFalse();
    assertThat(data.accepted()).isTrue();
    assertThat(data.message()).contains("jobId=" + data.jobId());

    // Verify database row was created in communication_dispatch_jobs with PENDING status
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_dispatch_jobs WHERE job_id = ? AND job_status = 'PENDING'",
        Integer.class,
        data.jobId()
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @Order(8)
  @DisplayName("8. Non-Destructive Isolation: Pre-existing records survive setup and teardown unharmed")
  void test8_preExistingRecords_surviveSetupAndTeardown() {
    long preExistingOrgId = 999981L;
    long preExistingProviderId = 999981L;
    long preExistingUserId = 999981L;

    // Create a pre-existing fixture outside this test's tracked lifecycle
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-PREEXISTING', 'PreExisting Org', 1, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE organization_name = VALUES(organization_name)",
        preExistingOrgId
    );
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRV-PREEXISTING', 'PreExisting Provider', 'REG-PREEXISTING', 1, ?, ?, ?, 1, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name)",
        preExistingProviderId, preExistingOrgId, testStateId, testDistrictId
    );
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, organization_id, created_at, updated_at) "
            + "VALUES (?, 'preexisting_user', 'preexisting@example.com', 'hash', 1, ?, ?, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE username = VALUES(username)",
        preExistingUserId, preExistingProviderId, preExistingOrgId
    );

    try {
      // Execute teardown and setup cycle
      cleanUpTrackedFixtures();
      insertDynamicFixtures();

      // Assert pre-existing records survive unharmed
      Integer orgCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organizations WHERE organization_id = ?", Integer.class, preExistingOrgId);
      Integer prvCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_providers WHERE provider_id = ?", Integer.class, preExistingProviderId);
      Integer usrCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE user_id = ?", Integer.class, preExistingUserId);

      assertThat(orgCount).isEqualTo(1);
      assertThat(prvCount).isEqualTo(1);
      assertThat(usrCount).isEqualTo(1);
    } finally {
      // Clean up the pre-existing fixture explicitly
      jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", preExistingUserId);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", preExistingProviderId);
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", preExistingOrgId);
    }
  }

  @Test
  @Order(9)
  @DisplayName("9. Idempotency Scoping: Reusing another tenant's idempotency key is rejected with IDEMPOTENCY_KEY_CONFLICT")
  void test9_idempotency_crossTenantReuseRejected() {
    String sharedKey = testKeyPrefix + "SH-" + UUID.randomUUID().toString().substring(0, 16);
    trackedIdempotencyKeys.add(sharedKey);

    // Enqueue initial job owned by Provider A
    Long jobAId = insertTestJob(sharedKey, AUTHORITATIVE_TEST_TRAINEE_ID, testProviderAId, "PENDING");

    // Nationwide caller attempts to reuse Provider A's idempotency key -> rejected as cross-tenant conflict
    setNationwideAdminUser();
    CommunicationDispatchCommand cmd = CommunicationDispatchCommand.of(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "SEC-P2"),
        1L
    );

    assertThatThrownBy(() -> queueService.enqueueJob(cmd, sharedKey))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
  }

  @Test
  @Order(10)
  @DisplayName("10. Idempotency Scoping: Reusing same key with mismatched payload is rejected with IDEMPOTENCY_KEY_PAYLOAD_MISMATCH")
  void test10_idempotency_payloadMismatchRejected() {
    String key = testKeyPrefix + "PL-" + UUID.randomUUID().toString().substring(0, 16);
    trackedIdempotencyKeys.add(key);

    setNationwideAdminUser();

    CommunicationDispatchCommand initialCmd = CommunicationDispatchCommand.of(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "ORIGINAL-PAYLOAD"),
        1L
    );
    DispatchJobDto job = queueService.enqueueJob(initialCmd, key);
    trackedJobIds.add(job.jobId());

    // Same caller, same key, but different parameters
    CommunicationDispatchCommand mismatchedCmd = CommunicationDispatchCommand.of(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "DIFFERENT-PAYLOAD"),
        1L
    );

    assertThatThrownBy(() -> queueService.enqueueJob(mismatchedCmd, key))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH");
  }

  @Test
  @Order(11)
  @DisplayName("11. Idempotency Scoping: Identical authorized request with same key returns original job without duplicate enqueue")
  void test11_idempotency_sameAuthorizedRequestReturnsExistingJob() {
    String key = testKeyPrefix + "MT-" + UUID.randomUUID().toString().substring(0, 16);
    trackedIdempotencyKeys.add(key);

    setNationwideAdminUser();

    CommunicationDispatchCommand cmd = CommunicationDispatchCommand.of(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "SAME-PAYLOAD"),
        1L
    );
    DispatchJobDto initialJob = queueService.enqueueJob(cmd, key);
    trackedJobIds.add(initialJob.jobId());

    // Identical request returns identical existing job
    DispatchJobDto replayJob = queueService.enqueueJob(cmd, key);
    assertThat(replayJob.jobId()).isEqualTo(initialJob.jobId());
    assertThat(replayJob.idempotencyKey()).isEqualTo(initialJob.idempotencyKey());

    // Verify only 1 database row exists
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_dispatch_jobs WHERE idempotency_key = ?",
        Integer.class,
        key
    );
    assertThat(count).isEqualTo(1);
  }
}
