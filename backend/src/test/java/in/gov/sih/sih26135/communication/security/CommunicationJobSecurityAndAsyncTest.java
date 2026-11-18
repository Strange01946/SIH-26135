package in.gov.sih.sih26135.communication.security;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
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
 * Phase 29.4 targeted security & async API integration test suite verifying:
 * <ul>
 *   <li>Authoritative multi-tenant isolation on dispatch job retrieval, execution attempt inspection, and dead-letter queue operations.</li>
 *   <li>Strict boundary checks between distinct training providers (Provider 7771 cannot access or replay Provider 7772 jobs).</li>
 *   <li>Strict boundary checks preventing provider-scoped callers from accessing nationwide/unaffiliated dispatch jobs.</li>
 *   <li>Dead-letter queue isolation ensuring provider-scoped callers only view their own provider's exhausted jobs.</li>
 *   <li>Nationwide administrative authority permitting access across all provider jobs and the complete dead-letter queue.</li>
 *   <li>Truthful asynchronous API response semantics (null communicationLogId, null providerMessageId, valid queue jobId, status QUEUED).</li>
 * </ul>
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 29.4 — Communication Job Security, Multi-Tenant Isolation & Asynchronous API Tests (SIH26135_TEST)")
public class CommunicationJobSecurityAndAsyncTest {

  private static final Long TEST_STATE_ID = 9985L;
  private static final Long TEST_DISTRICT_ID = 9985L;
  private static final Long TEST_ORG_ID = 9985L;
  private static final Long TEST_PROVIDER_7771_ID = 7771L;
  private static final Long TEST_PROVIDER_7772_ID = 7772L;
  private static final Long TEST_USER_7771_ID = 7771L;
  private static final Long TEST_USER_7772_ID = 7772L;
  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;
  private static final String TEST_KEY_PREFIX = "SEC-TEST-JOB-";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private CommunicationJobController communicationJobController;

  @Autowired
  private CommunicationLogController communicationLogController;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() throws Exception {
    jdbcTemplate = new JdbcTemplate(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }

    cleanUpFixtures();
    insertFixtures();
  }

  @AfterEach
  void tearDown() {
    try {
      cleanUpFixtures();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void insertFixtures() {
    // 1. Geography
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'S5', 'Security Test State 5', 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE state_name = VALUES(state_name)",
        TEST_STATE_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'D5', 'Security Test District 5', 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE district_name = VALUES(district_name)",
        TEST_DISTRICT_ID, TEST_STATE_ID);

    // 2. Organization
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-SEC-9985', 'Security Org 9985', 1, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE organization_name = VALUES(organization_name)",
        TEST_ORG_ID);

    // 3. Training Providers
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRV-SEC-7771', 'Security Provider 7771', 'REG-SEC-7771', 1, ?, ?, ?, 1, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name)",
        TEST_PROVIDER_7771_ID, TEST_ORG_ID, TEST_STATE_ID, TEST_DISTRICT_ID);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRV-SEC-7772', 'Security Provider 7772', 'REG-SEC-7772', 1, ?, ?, ?, 1, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name)",
        TEST_PROVIDER_7772_ID, TEST_ORG_ID, TEST_STATE_ID, TEST_DISTRICT_ID);

    // 4. Provider Users
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, organization_id, created_at, updated_at) "
            + "VALUES (?, 'provider_7771_user', 'provider_7771@example.com', 'hash', 1, ?, ?, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE training_provider_id = VALUES(training_provider_id)",
        TEST_USER_7771_ID, TEST_PROVIDER_7771_ID, TEST_ORG_ID);

    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, organization_id, created_at, updated_at) "
            + "VALUES (?, 'provider_7772_user', 'provider_7772@example.com', 'hash', 1, ?, ?, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE training_provider_id = VALUES(training_provider_id)",
        TEST_USER_7772_ID, TEST_PROVIDER_7772_ID, TEST_ORG_ID);
  }

  private void cleanUpFixtures() {
    if (jdbcTemplate == null) {
      return;
    }
    try {
      jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ?",
          "%:" + AUTHORITATIVE_TEST_TRAINEE_ID, "%:" + AUTHORITATIVE_TEST_TRAINEE_ID + ":%");
      jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id IN (SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key LIKE '" + TEST_KEY_PREFIX + "%')");
      jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE idempotency_key LIKE '" + TEST_KEY_PREFIX + "%'");
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?)", TEST_USER_7771_ID, TEST_USER_7772_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)", TEST_PROVIDER_7771_ID, TEST_PROVIDER_7772_ID);
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", TEST_ORG_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", TEST_DISTRICT_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", TEST_STATE_ID);
    } catch (Exception ignored) {
    }
  }

  private Long insertTestJob(String idempotencyKey, Long traineeId, Long providerId, String jobStatus) {
    jdbcTemplate.update(
        "INSERT INTO communication_dispatch_jobs ("
            + "idempotency_key, trainee_id, training_provider_id, channel_id, purpose_id, "
            + "template_code, template_parameters_json, job_status, attempt_count, max_attempts, "
            + "delivery_mode, initiated_by_user_id, initiated_by_service, tenant_scope_type, "
            + "scheduled_at, created_at, updated_at, lease_version) "
            + "VALUES (?, ?, ?, 1, 1, 'OPERATIONAL_ALERT', '{}', ?, 0, 3, 'DEMO', 1, 'TEST', 'PROVIDER', NOW(), NOW(), NOW(), 1)",
        idempotencyKey, traineeId, providerId, jobStatus
    );
    return jdbcTemplate.queryForObject(
        "SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key = ?",
        Long.class,
        idempotencyKey
    );
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
  @DisplayName("1. Cross-Tenant Isolation: Provider 7771 caller attempting to get Provider 7772 job -> rejected with PROVIDER_MISMATCH")
  void test1_crossTenant_getJobById_denied() {
    Long job7772Id = insertTestJob(
        TEST_KEY_PREFIX + "P7772-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7772_ID,
        "PENDING"
    );

    setProviderUser(TEST_USER_7771_ID, "provider_7771_user");

    assertThatThrownBy(() -> communicationJobController.getJobById(job7772Id))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(2)
  @DisplayName("2. Cross-Tenant Isolation: Provider 7771 caller attempting to get Provider 7772 job attempts -> rejected with PROVIDER_MISMATCH")
  void test2_crossTenant_getJobAttempts_denied() {
    Long job7772Id = insertTestJob(
        TEST_KEY_PREFIX + "P7772-ATT-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7772_ID,
        "FAILED"
    );
    insertTestAttempt(job7772Id);

    setProviderUser(TEST_USER_7771_ID, "provider_7771_user");

    assertThatThrownBy(() -> communicationJobController.getJobAttempts(job7772Id))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(3)
  @DisplayName("3. Cross-Tenant Isolation: Provider 7771 caller attempting to replay Provider 7772 dead-letter job -> rejected with PROVIDER_MISMATCH")
  void test3_crossTenant_replayDeadLetterJob_denied() {
    Long job7772Id = insertTestJob(
        TEST_KEY_PREFIX + "P7772-DL-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7772_ID,
        "DEAD_LETTER"
    );

    setProviderUser(TEST_USER_7771_ID, "provider_7771_user");
    Principal principal = new UserPrincipal(TEST_USER_7771_ID, "provider_7771_user");

    assertThatThrownBy(() -> communicationJobController.replayDeadLetterJob(job7772Id, principal))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
  }

  @Test
  @Order(4)
  @DisplayName("4. Scope Boundary: Provider-scoped caller attempting to inspect nationwide job (null provider) -> HTTP 403 Forbidden")
  void test4_providerCaller_accessingNationwideJob_forbidden() {
    Long nationwideJobId = insertTestJob(
        TEST_KEY_PREFIX + "NAT-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        null,
        "PENDING"
    );

    setProviderUser(TEST_USER_7771_ID, "provider_7771_user");

    assertThatThrownBy(() -> communicationJobController.getJobById(nationwideJobId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @Order(5)
  @DisplayName("5. Dead-Letter Filtering: Provider 7771 caller sees strictly Provider 7771 dead-letter jobs, zero leakage of Provider 7772")
  void test5_deadLetterQueue_isolatedPerTenant() {
    Long job7771DlId = insertTestJob(
        TEST_KEY_PREFIX + "DL-7771-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7771_ID,
        "DEAD_LETTER"
    );
    Long job7772DlId = insertTestJob(
        TEST_KEY_PREFIX + "DL-7772-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7772_ID,
        "DEAD_LETTER"
    );

    setProviderUser(TEST_USER_7771_ID, "provider_7771_user");

    ResponseEntity<ApiResponse<List<DispatchJobDto>>> response = communicationJobController.getDeadLetterJobs(0, 50);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    List<DispatchJobDto> returnedJobs = response.getBody().getData();

    // Must include Provider 7771 dead-letter job
    assertThat(returnedJobs).anyMatch(j -> j.jobId().equals(job7771DlId));

    // Must NEVER include Provider 7772 dead-letter job
    assertThat(returnedJobs).noneMatch(j -> j.jobId().equals(job7772DlId));
    assertThat(returnedJobs).allMatch(j -> TEST_PROVIDER_7771_ID.equals(j.trainingProviderId()));
  }

  @Test
  @Order(6)
  @DisplayName("6. Nationwide Administrative Authority: Super admin can access jobs across any provider and inspect full dead-letter queue")
  void test6_nationwideAdmin_canAccessAllProviderJobs() {
    Long job7771Id = insertTestJob(
        TEST_KEY_PREFIX + "ADMIN-7771-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7771_ID,
        "DEAD_LETTER"
    );
    Long job7772Id = insertTestJob(
        TEST_KEY_PREFIX + "ADMIN-7772-" + UUID.randomUUID(),
        AUTHORITATIVE_TEST_TRAINEE_ID,
        TEST_PROVIDER_7772_ID,
        "DEAD_LETTER"
    );

    setNationwideAdminUser();

    // Admin can get Provider 7771 job
    ResponseEntity<ApiResponse<DispatchJobDto>> response7771 = communicationJobController.getJobById(job7771Id);
    assertThat(response7771.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response7771.getBody().getData().jobId()).isEqualTo(job7771Id);

    // Admin can get Provider 7772 job
    ResponseEntity<ApiResponse<DispatchJobDto>> response7772 = communicationJobController.getJobById(job7772Id);
    assertThat(response7772.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response7772.getBody().getData().jobId()).isEqualTo(job7772Id);

    // Admin calling getDeadLetterJobs sees both jobs
    ResponseEntity<ApiResponse<List<DispatchJobDto>>> dlResponse = communicationJobController.getDeadLetterJobs(0, 50);
    assertThat(dlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<DispatchJobDto> allDeadLetters = dlResponse.getBody().getData();
    assertThat(allDeadLetters).anyMatch(j -> j.jobId().equals(job7771Id));
    assertThat(allDeadLetters).anyMatch(j -> j.jobId().equals(job7772Id));
  }

  @Test
  @Order(7)
  @DisplayName("7. Asynchronous API Identifier Truthfulness: Async dispatch returns null communicationLogId/providerMessageId and valid jobId")
  void test7_asyncDispatch_truthfulIdentifiers() {
    setNationwideAdminUser();

    String asyncKey = TEST_KEY_PREFIX + "ASYNC-" + UUID.randomUUID();
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
}
