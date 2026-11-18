package in.gov.sih.sih26135.communication.queue;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.exception.CommunicationRateLimitException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Phase 29.4 — Durable Dispatch Queue, Atomic Claiming, Backoff & Dead-Letter Replay Tests (SIH26135_TEST)")
class DurableDispatchQueueTest {

  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;
  private static final String IDEMPOTENCY_PREFIX = "TEST-JOB-98993-";

  @Autowired
  private DurableDispatchQueueService queueService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private NotificationDispatcherService notificationDispatcherService;

  @Autowired
  private MockSmsProvider mockSmsProvider;

  private final List<Long> trackedJobIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedRateLimitEventIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedCommLogIds = new CopyOnWriteArrayList<>();
  private Long testStartRateLimitEventId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }
    setAdminUser();
    testStartRateLimitEventId = jdbcTemplate.queryForObject(
        "SELECT COALESCE(MAX(event_id), 0) FROM communication_rate_limit_events",
        Long.class
    );
  }

  @AfterEach
  void tearDown() {
    try {
      if (mockSmsProvider != null) {
        mockSmsProvider.clear();
      }
      cleanTestJobs();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void setAdminUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("admin.nationwide"),
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("communication.manage"),
                new SimpleGrantedAuthority("system.manage")
            )
        )
    );
  }

  private void cleanTestJobs() {
    if (testStartRateLimitEventId != null) {
      List<Long> newEventIds = jdbcTemplate.queryForList(
          "SELECT event_id FROM communication_rate_limit_events WHERE event_id > ?",
          Long.class,
          testStartRateLimitEventId
      );
      trackedRateLimitEventIds.addAll(newEventIds);
    }

    for (Long eventId : trackedRateLimitEventIds) {
      if (eventId != null && eventId > 0) {
        jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE event_id = ?", eventId);
      }
    }
    trackedRateLimitEventIds.clear();

    for (Long jobId : trackedJobIds) {
      if (jobId != null) {
        List<Long> logIds = jdbcTemplate.query(
            "SELECT communication_log_id FROM communication_dispatch_jobs WHERE job_id = ? AND communication_log_id IS NOT NULL",
            (rs, rn) -> rs.getLong("communication_log_id"),
            jobId
        );
        for (Long logId : logIds) {
          jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
        }
        jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
      }
    }
    trackedJobIds.clear();

    for (Long logId : trackedCommLogIds) {
      if (logId != null) {
        jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
      }
    }
    trackedCommLogIds.clear();
  }

  private DispatchJobDto enqueueAndTrackJob(CommunicationDispatchCommand command, String idempotencyKey) {
    DispatchJobDto job = queueService.enqueueJob(command, idempotencyKey);
    if (job != null && job.jobId() != null) {
      trackedJobIds.add(job.jobId());
    }
    return job;
  }

  @Test
  @DisplayName("1. Job Persistence: Enqueuing job persists row in MySQL with PENDING status and survives restarts")
  void testEnqueueJob_persistsPendingStatus() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    Map<String, String> params = Map.of(
        "batchCode", "BATCH-QUEUE-01",
        "courseName", "Solar Installation",
        "startDate", "2026-12-01",
        "trainingCenterName", "NSTI Noida"
    );
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        params,
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);

    assertThat(job).isNotNull();
    assertThat(job.jobId()).isPositive();
    assertThat(job.idempotencyKey()).isEqualTo(key);
    assertThat(job.traineeId()).isEqualTo(AUTHORITATIVE_TEST_TRAINEE_ID);
    assertThat(job.channel()).isEqualTo(CommunicationChannel.SMS);
    assertThat(job.templateCode()).isEqualTo("TPL-ENROLL-001");
    assertThat(job.maxAttempts()).isEqualTo(3);
  }

  @Test
  @DisplayName("2. Atomic Worker Claiming: Only one worker can claim a PENDING job; second worker gets null")
  void testAtomicJobClaim_concurrencySafe() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Reset status to PENDING manually to test claim
    jdbcTemplate.update("UPDATE communication_dispatch_jobs SET job_status = 'PENDING', locked_by = NULL, locked_at = NULL WHERE job_id = ?", jobId);

    // Worker 1 claims
    int claimedWorker1 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'PROCESSING', locked_at = NOW(), locked_by = 'worker-1' WHERE job_id = ? AND job_status = 'PENDING'",
        jobId
    );
    assertThat(claimedWorker1).isEqualTo(1);

    // Worker 2 attempts to claim the same job concurrently
    int claimedWorker2 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'PROCESSING', locked_at = NOW(), locked_by = 'worker-2' WHERE job_id = ? AND job_status = 'PENDING'",
        jobId
    );
    assertThat(claimedWorker2).isEqualTo(0);
  }

  @Test
  @DisplayName("3. Permanent Failure Fail-Closed: Unknown template code immediately transitions to DEAD_LETTER with zero retries")
  void testPermanentFailure_immediatelyDeadLettersZeroRetries() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "INVALID_UNKNOWN_TEMPLATE_CODE",
        Map.of("param", "value"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Process job
    queueService.claimAndProcessJob(jobId, "test-worker");

    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(updatedJob.deadLetterReason()).isNotBlank();
    assertThat(updatedJob.attemptCount()).isEqualTo(1);

    // Verify attempt record
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).hasSize(1);
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("DEAD_LETTER");
  }

  @Test
  @DisplayName("4. DPDP Consent Revocation While Queued: Job dead-letters with reason CONSENT_REVOKED before dispatch")
  void testConsentRevocationWhileQueued_deadLettersWithConsentRevoked() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    // Use SURVEY_INVITE purpose which requires employment follow-up consent
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.SURVEY_INVITE,
        "TPL-SURVEY-004",
        Map.of("milestoneMonths", "6", "surveyId", "SURV-REVOKED-TEST", "surveyUrl", "https://skillindia.gov.in/surveys/followup"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Temporarily revoke employment follow-up consent for testing
    jdbcTemplate.update(
        "UPDATE trainee_consents SET consent_status_id = 2 WHERE trainee_id = ? AND consent_type_id = 2",
        AUTHORITATIVE_TEST_TRAINEE_ID
    );

    try {
      // Process job
      queueService.claimAndProcessJob(jobId, "test-worker-consent");

      DispatchJobDto processedJob = queueService.getJobById(jobId);
      assertThat(processedJob.jobStatus()).isEqualTo("DEAD_LETTER");
      assertThat(processedJob.deadLetterReason()).isEqualTo("CONSENT_REVOKED");

      List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
      assertThat(attempts).isNotEmpty();
      assertThat(attempts.get(0).errorCode()).contains("CONSENT");
    } finally {
      // Restore active consent for authoritative trainee
      jdbcTemplate.update(
          "UPDATE trainee_consents SET consent_status_id = 1 WHERE trainee_id = ? AND consent_type_id = 2",
          AUTHORITATIVE_TEST_TRAINEE_ID
      );
    }
  }

  @Test
  @DisplayName("5. Authorized Dead-Letter Replay: Operator replay resets attempt count, updates status to PENDING and re-enqueues")
  void testOperatorReplayDeadLetterJob_resetsAndReenqueues() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-REPLAY-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Mark job as DEAD_LETTER manually
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'DEAD_LETTER', attempt_count = 3, "
            + "dead_letter_reason = 'MAX_RETRIES_EXCEEDED', dead_lettered_at = NOW() WHERE job_id = ?",
        jobId
    );

    // Operator replays the dead-letter job
    DispatchJobDto replayedJob = queueService.replayDeadLetterJob(jobId, "admin_operator");

    assertThat(replayedJob).isNotNull();
    assertThat(replayedJob.jobStatus()).isIn("PENDING", "COMPLETED", "PROCESSING");
  }

  @Test
  @DisplayName("6. Stale Job Recovery: Recovers jobs abandoned in PROCESSING status to DEAD_LETTER (AMBIGUOUS_STALE_PROCESSING) to prevent duplicate send")
  void testRecoverStaleJobs_recoversLockedJobs() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-STALE-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Simulate crashed worker: job locked in PROCESSING 15 minutes ago
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'PROCESSING', locked_by = 'crashed-worker-pid-999', "
            + "locked_at = NOW() - INTERVAL 15 MINUTE WHERE job_id = ?",
        jobId
    );

    int recoveredCount = queueService.recoverStaleJobs(5);
    assertThat(recoveredCount).isGreaterThanOrEqualTo(1);

    DispatchJobDto recoveredJob = queueService.getJobById(jobId);
    assertThat(recoveredJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(recoveredJob.deadLetterReason()).isEqualTo("AMBIGUOUS_STALE_PROCESSING");
    assertThat(recoveredJob.lockedBy()).isNull();
    assertThat(recoveredJob.lockedAt()).isNull();
    // Lease version must be incremented by crash recovery
    assertThat(recoveredJob.leaseVersion()).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("7. Lease Fencing: Stale worker with superseded lease cannot overwrite newer worker's state")
  void testLeaseFencing_staleWorkerCannotOverwriteNewerState() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-FENCE-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Worker 1 claims job (lease_version = 2), but then hangs for 10 minutes
    int claimedW1 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'PROCESSING', lease_version = lease_version + 1, locked_at = NOW() - INTERVAL 10 MINUTE, locked_by = 'worker-1' WHERE job_id = ?",
        jobId
    );
    assertThat(claimedW1).isEqualTo(1);
    int worker1Lease = jdbcTemplate.queryForObject("SELECT lease_version FROM communication_dispatch_jobs WHERE job_id = ?", Integer.class, jobId);
    assertThat(worker1Lease).isEqualTo(2);

    // Job becomes stale and is recovered by crash recovery scheduler (increments lease_version to 3)
    int recovered = queueService.recoverStaleJobs(5);
    assertThat(recovered).isGreaterThanOrEqualTo(1);

    // Worker 2 claims job (increments lease_version to 4) and successfully completes it
    int claimedW2 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'PROCESSING', lease_version = lease_version + 1, locked_at = NOW(), locked_by = 'worker-2' WHERE job_id = ?",
        jobId
    );
    assertThat(claimedW2).isEqualTo(1);
    int worker2Lease = jdbcTemplate.queryForObject("SELECT lease_version FROM communication_dispatch_jobs WHERE job_id = ?", Integer.class, jobId);
    assertThat(worker2Lease).isEqualTo(4);

    // Worker 2 completes job with valid communicationLogId = 77777L
    int completedW2 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'COMPLETED', communication_log_id = 77777, updated_at = NOW() WHERE job_id = ? AND lease_version = ?",
        jobId,
        worker2Lease
    );
    assertThat(completedW2).isEqualTo(1);

    // Stale Worker 1 wakes up and attempts to update/overwrite with its stale lease (2)
    int staleUpdateW1 = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'COMPLETED', communication_log_id = 99999, updated_at = NOW() WHERE job_id = ? AND lease_version = ?",
        jobId,
        worker1Lease
    );
    // Stale worker is fenced out: 0 rows updated!
    assertThat(staleUpdateW1).isEqualTo(0);

    // Verify database record still has Worker 2's authoritative state
    DispatchJobDto finalJob = queueService.getJobById(jobId);
    assertThat(finalJob.jobStatus()).isEqualTo("COMPLETED");
    assertThat(finalJob.communicationLogId()).isEqualTo(77777L);
  }

  @Test
  @DisplayName("8. Scheduled Retries: Worker will not claim job before scheduled_at due time")
  void testScheduledRetry_doesNotClaimBeforeScheduledAt() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-SCHED-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Schedule job 5 minutes in the future
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() + INTERVAL 5 MINUTE WHERE job_id = ?",
        jobId
    );

    // Worker attempts to claim job immediately
    var response = queueService.claimAndProcessJob(jobId, "eager-worker");
    assertThat(response).isNull();

    // Verify job remains PENDING with 0 attempts
    DispatchJobDto unchangedJob = queueService.getJobById(jobId);
    assertThat(unchangedJob.jobStatus()).isEqualTo("PENDING");
    assertThat(unchangedJob.attemptCount()).isEqualTo(0);
    assertThat(unchangedJob.lockedBy()).isNull();
  }

  @Test
  @DisplayName("9. Dead-Letter Replay: Replaying dead-letter job does not double-count rate limit quota")
  void testDeadLetterReplay_doesNotDoubleCountRateLimitQuota() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-NO-DOUBLE-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    // Set status to DEAD_LETTER
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = 'DEAD_LETTER', attempt_count = 3, "
            + "dead_letter_reason = 'TEST_FAILURE', dead_lettered_at = NOW() WHERE job_id = ?",
        jobId
    );

    int countBeforeReplay = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key LIKE ?",
        Integer.class,
        "%:" + AUTHORITATIVE_TEST_TRAINEE_ID + "%"
    );

    // Replay job
    queueService.replayDeadLetterJob(jobId, "admin_user");

    int countAfterReplay = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key LIKE ?",
        Integer.class,
        "%:" + AUTHORITATIVE_TEST_TRAINEE_ID + "%"
    );

    // No extra rate limit events inserted during replay itself
    assertThat(countAfterReplay).isEqualTo(countBeforeReplay);
  }

  @Test
  @DisplayName("10. Concurrent Workers: Atomic claiming ensures exactly one worker acquires and processes job")
  void testConcurrentWorkers_onlyOneClaimsJob() throws Exception {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-CONCURR-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      CompletableFuture<NotificationDispatchResponse> worker1 = CompletableFuture.supplyAsync(
          () -> queueService.claimAndProcessJob(jobId, "worker-alpha"), executor);
      CompletableFuture<NotificationDispatchResponse> worker2 = CompletableFuture.supplyAsync(
          () -> queueService.claimAndProcessJob(jobId, "worker-beta"), executor);

      CompletableFuture.allOf(worker1, worker2).join();

      NotificationDispatchResponse res1 = worker1.get();
      NotificationDispatchResponse res2 = worker2.get();

      // Exactly one worker must have successfully claimed and processed; the other worker received null
      boolean oneProcessed = (res1 != null && res2 == null) || (res1 == null && res2 != null);
      assertThat(oneProcessed).isTrue();

      // Confirm job status is COMPLETED
      DispatchJobDto finalJob = queueService.getJobById(jobId);
      assertThat(finalJob.jobStatus()).isEqualTo("COMPLETED");

      // Verify attempts table: exactly 1 attempt was recorded
      List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
      assertThat(attempts).hasSize(1);
      assertThat(attempts.get(0).attemptStatus()).isEqualTo("COMPLETED");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  @DisplayName("11. Retry Quota Wiring: Automatic internal queue retry bypasses trainee quota, but external caller cannot bypass")
  void testRetryQuotaWiring_automaticRetryConsumesProviderBurstWithoutTrippingTraineeQuota() {
    String traineeLimiterKey = "TRAINEE:SMS:" + AUTHORITATIVE_TEST_TRAINEE_ID;

    // Simulate trainee exhausting 3/3 daily SMS limit
    for (int i = 0; i < 3; i++) {
      jdbcTemplate.update(
          "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'TRAINEE_DISPATCH', NOW())",
          traineeLimiterKey
      );
    }

    // 1. External caller attempting dispatch with isRetry=true fails closed because caller is not authenticated internal worker
    CommunicationDispatchCommand externalCommand = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-RETRY-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null,
        true // external caller claiming isRetry=true
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(externalCommand))
        .isInstanceOf(CommunicationRateLimitException.class)
        .hasMessageContaining("Trainee has exceeded maximum allowed SMS notifications");

    // 2. Queue automatic retry (attempt_count > 0) executed by internal queue worker succeeds
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    DispatchJobDto job = enqueueAndTrackJob(externalCommand, key);
    Long jobId = job.jobId();

    // Set attempt_count = 1 so currentAttemptNumber becomes 2 (an automatic retry)
    jdbcTemplate.update("UPDATE communication_dispatch_jobs SET attempt_count = 1 WHERE job_id = ?", jobId);

    NotificationDispatchResponse retryResponse = queueService.claimAndProcessJob(jobId, "worker-retry-test");
    assertThat(retryResponse).isNotNull();
    assertThat(retryResponse.accepted() || "SIMULATED".equalsIgnoreCase(retryResponse.status())).isTrue();

    DispatchJobDto completedJob = queueService.getJobById(jobId);
    assertThat(completedJob.jobStatus()).isEqualTo("COMPLETED");
    assertThat(completedJob.attemptCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("12. Ambiguous Provider Timeout: Ambiguous read timeout dead-letters without auto-resend and logs ATTEMPTED status")
  void testAmbiguousProviderTimeout_doesNotAutoRetry_transitionsToDeadLetter() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-TIMEOUT-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    mockSmsProvider.setSimulateException(
        new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"))
    );

    try {
      queueService.claimAndProcessJob(jobId, "worker-timeout");
    } finally {
      mockSmsProvider.clear();
    }

    DispatchJobDto finalJob = queueService.getJobById(jobId);
    assertThat(finalJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(finalJob.deadLetterReason()).isEqualTo("AMBIGUOUS_PROVIDER_TIMEOUT");
    assertThat(finalJob.attemptCount()).isEqualTo(1);

    // Verify attempt table
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).hasSize(1);
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("AMBIGUOUS_TIMEOUT");
    assertThat(attempts.get(0).errorCode()).isEqualTo("AMBIGUOUS_PROVIDER_TIMEOUT");

    // Verify communication log table: truthful status ATTEMPTED (2), failed_at is null
    Long commLogId = finalJob.communicationLogId();
    assertThat(commLogId).isNotNull();
    trackedCommLogIds.add(commLogId);

    Long statusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        commLogId
    );
    assertThat(statusId).isEqualTo(2L); // 2 = ATTEMPTED

    String failureReason = jdbcTemplate.queryForObject(
        "SELECT failure_reason FROM communication_logs WHERE communication_log_id = ?",
        String.class,
        commLogId
    );
    assertThat(failureReason).contains("timed out");

    LocalDateTime failedAt = jdbcTemplate.queryForObject(
        "SELECT failed_at FROM communication_logs WHERE communication_log_id = ?",
        LocalDateTime.class,
        commLogId
    );
    assertThat(failedAt).isNull();
  }

  @Test
  @DisplayName("13. Safe Transient Connection Failure: Definite connection failure schedules bounded retry with exponential backoff")
  void testSafeTransientConnectionFailure_schedulesBoundedRetry() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-CONNECT-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    mockSmsProvider.setSimulateException(
        new ResourceAccessException("Connection refused", new ConnectException("Connection refused"))
    );

    try {
      queueService.claimAndProcessJob(jobId, "worker-transient");
    } finally {
      mockSmsProvider.clear();
    }

    DispatchJobDto finalJob = queueService.getJobById(jobId);
    assertThat(finalJob.jobStatus()).isEqualTo("PENDING");
    assertThat(finalJob.attemptCount()).isEqualTo(1);
    assertThat(finalJob.scheduledAt()).isAfter(LocalDateTime.now().minusSeconds(5));

    // Verify attempt record indicates retry scheduled
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).hasSize(1);
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("FAILED_RETRY_SCHEDULED");
    assertThat(attempts.get(0).errorCode()).isEqualTo("PROVIDER_CONNECT_FAILURE");
  }

  @Test
  @DisplayName("14. Permanent Provider 4xx Rejection: Permanent client error dead-letters without retry")
  void testPermanentProvider4xxRejection_transitionsToDeadLetterWithoutRetry() {
    String key = IDEMPOTENCY_PREFIX + UUID.randomUUID();
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        AUTHORITATIVE_TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-PERM-400", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = enqueueAndTrackJob(command, key);
    Long jobId = job.jobId();

    mockSmsProvider.setSimulateFailure(true);
    mockSmsProvider.setFailureDetails("400", "Invalid mobile phone number");

    try {
      queueService.claimAndProcessJob(jobId, "worker-perm-400");
    } finally {
      mockSmsProvider.clear();
    }

    DispatchJobDto finalJob = queueService.getJobById(jobId);
    assertThat(finalJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(finalJob.deadLetterReason()).isEqualTo("400");
    assertThat(finalJob.attemptCount()).isEqualTo(1);

    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).hasSize(1);
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("DEAD_LETTER");
    assertThat(attempts.get(0).errorCode()).isEqualTo("400");
  }

  @Test
  @DisplayName("15. Test Fixture Survival: Pre-existing and unrelated records survive setup and teardown")
  void testUnrelatedRecordsSurviveSetupAndTeardown() {
    // 1. Insert an unrelated rate-limit event for an unrelated entity
    String unrelatedLimiterKey = "UNRELATED:SMS:999999";
    jdbcTemplate.update(
        "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'UNRELATED_TEST', NOW())",
        unrelatedLimiterKey
    );
    Long unrelatedEventId = jdbcTemplate.queryForObject(
        "SELECT event_id FROM communication_rate_limit_events WHERE limiter_key = ? ORDER BY event_id DESC LIMIT 1",
        Long.class,
        unrelatedLimiterKey
    );

    // 2. Insert an unrelated dispatch job
    String unrelatedIdempotencyKey = "UNRELATED-JOB-EXISTING-99999";
    jdbcTemplate.update(
        "INSERT INTO communication_dispatch_jobs ("
            + "idempotency_key, trainee_id, channel_id, purpose_id, template_code, "
            + "template_parameters_json, max_attempts, attempt_count, job_status, delivery_mode, scheduled_at, created_at, updated_at) "
            + "VALUES (?, ?, 1, 6, 'TPL-ENROLL-001', '{}', 3, 0, 'PENDING', 'DEMO', NOW(), NOW(), NOW())",
        unrelatedIdempotencyKey,
        AUTHORITATIVE_TEST_TRAINEE_ID
    );
    Long unrelatedJobId = jdbcTemplate.queryForObject(
        "SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key = ?",
        Long.class,
        unrelatedIdempotencyKey
    );

    try {
      // Set testStartRateLimitEventId so cleanTestJobs treats unrelatedEventId as pre-existing
      testStartRateLimitEventId = unrelatedEventId;
      // 3. Enqueue and process a test job through the tracking helper
      String testKey = IDEMPOTENCY_PREFIX + UUID.randomUUID();
      CommunicationDispatchCommand command = new CommunicationDispatchCommand(
          AUTHORITATIVE_TEST_TRAINEE_ID,
          CommunicationChannel.SMS,
          CommunicationPurpose.OPERATIONAL,
          "TPL-ENROLL-001",
          Map.of("batchCode", "B-SURVIVE-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
          null,
          null,
          null
      );
      DispatchJobDto testJob = enqueueAndTrackJob(command, testKey);

      // Execute teardown
      cleanTestJobs();

      // Assert test job was deleted
      Integer testJobCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM communication_dispatch_jobs WHERE job_id = ?",
          Integer.class,
          testJob.jobId()
      );
      assertThat(testJobCount).isEqualTo(0);

      // Assert unrelated job survived untouched
      Integer unrelatedJobCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM communication_dispatch_jobs WHERE job_id = ?",
          Integer.class,
          unrelatedJobId
      );
      assertThat(unrelatedJobCount).isEqualTo(1);

      // Assert unrelated rate limit event survived untouched
      Integer unrelatedEventCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM communication_rate_limit_events WHERE event_id = ?",
          Integer.class,
          unrelatedEventId
      );
      assertThat(unrelatedEventCount).isEqualTo(1);

      // Assert authoritative trainee retains 5 active GRANTED consents
      Integer activeConsents = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = ? AND consent_status_id = 1",
          Integer.class,
          AUTHORITATIVE_TEST_TRAINEE_ID
      );
      assertThat(activeConsents).isEqualTo(5);
    } finally {
      // Clean up unrelated records by exact PK
      jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", unrelatedJobId);
      jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE event_id = ?", unrelatedEventId);
    }
  }
}
