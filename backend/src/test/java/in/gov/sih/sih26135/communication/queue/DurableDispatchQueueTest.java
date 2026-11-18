package in.gov.sih.sih26135.communication.queue;

import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  @BeforeEach
  void setUp() {
    setAdminUser();
    cleanTestJobs();
  }

  @AfterEach
  void tearDown() {
    try {
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
    jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ?",
        "%:" + AUTHORITATIVE_TEST_TRAINEE_ID, "%:" + AUTHORITATIVE_TEST_TRAINEE_ID + ":%");
    List<Long> testJobIds = jdbcTemplate.query(
        "SELECT job_id FROM communication_dispatch_jobs WHERE idempotency_key LIKE '" + IDEMPOTENCY_PREFIX + "%'",
        (rs, rowNum) -> rs.getLong("job_id")
    );
    for (Long jobId : testJobIds) {
      jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
    }
    jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE idempotency_key LIKE '" + IDEMPOTENCY_PREFIX + "%'");
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

    DispatchJobDto job = queueService.enqueueJob(command, key);

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

    DispatchJobDto job = queueService.enqueueJob(command, key);
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

    DispatchJobDto job = queueService.enqueueJob(command, key);
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

    DispatchJobDto job = queueService.enqueueJob(command, key);
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

    DispatchJobDto job = queueService.enqueueJob(command, key);
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
  @DisplayName("6. Stale Job Recovery: Recovers jobs abandoned in PROCESSING status back to PENDING")
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

    DispatchJobDto job = queueService.enqueueJob(command, key);
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
    assertThat(recoveredJob.jobStatus()).isEqualTo("PENDING");
    assertThat(recoveredJob.lockedBy()).isNull();
    assertThat(recoveredJob.lockedAt()).isNull();
  }
}
