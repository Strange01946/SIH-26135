package in.gov.sih.sih26135.communication.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.consent.CommunicationConsentValidator;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.exception.CommunicationRateLimitException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.ratelimit.CommunicationRateLimiter;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.InternalSecurityExecutionSupport;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Authoritative production implementation of {@link DurableDispatchQueueService}.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Database-backed asynchronous queuing surviving application restarts.</li>
 *   <li>Atomic worker claiming via conditional SQL updates.</li>
 *   <li>Bounded retries with exponential backoff and jitter (up to 3 total attempts).</li>
 *   <li>Permanent failure fail-closed dead-lettering with zero retries.</li>
 *   <li>DPDP consent revalidation before every execution attempt.</li>
 *   <li>Multi-tenant rate limiting and anti-spam guardrails.</li>
 *   <li>Authorized operator inspection and manual replay of dead-lettered jobs.</li>
 *   <li>Automatic recovery of stale/crashed worker jobs.</li>
 * </ul>
 */
@Service
public class DurableDispatchQueueServiceImpl implements DurableDispatchQueueService {

  private static final Logger log = LoggerFactory.getLogger(DurableDispatchQueueServiceImpl.class);

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_PROCESSING = "PROCESSING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_DEAD_LETTER = "DEAD_LETTER";
  public static final String STATUS_CANCELLED = "CANCELLED";

  public static final int DEFAULT_MAX_ATTEMPTS = 3;
  public static final int DEFAULT_STALE_THRESHOLD_MINUTES = 5;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor dispatchExecutor;
  private final InternalSecurityExecutionSupport internalSecurityExecutionSupport;
  private final NotificationDispatcherService notificationDispatcherService;
  private final CommunicationProperties properties;
  private final CommunicationConsentValidator consentValidator;
  private final CommunicationRateLimiter rateLimiter;
  private final TraineeRepository traineeRepository;
  private final WorkflowSecurityScopeValidator securityScopeValidator;

  @Autowired
  public DurableDispatchQueueServiceImpl(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Qualifier("communicationDispatchExecutor") ThreadPoolTaskExecutor dispatchExecutor,
      InternalSecurityExecutionSupport internalSecurityExecutionSupport,
      @Lazy NotificationDispatcherService notificationDispatcherService,
      CommunicationProperties properties,
      CommunicationConsentValidator consentValidator,
      @Autowired(required = false) CommunicationRateLimiter rateLimiter,
      TraineeRepository traineeRepository,
      @Autowired(required = false) WorkflowSecurityScopeValidator securityScopeValidator) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate is required");
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper is required");
    this.dispatchExecutor = Objects.requireNonNull(dispatchExecutor, "ThreadPoolTaskExecutor is required");
    this.internalSecurityExecutionSupport = Objects.requireNonNull(internalSecurityExecutionSupport, "InternalSecurityExecutionSupport is required");
    this.notificationDispatcherService = notificationDispatcherService;
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    this.consentValidator = Objects.requireNonNull(consentValidator, "CommunicationConsentValidator is required");
    this.rateLimiter = rateLimiter;
    this.traineeRepository = Objects.requireNonNull(traineeRepository, "TraineeRepository is required");
    this.securityScopeValidator = securityScopeValidator;
  }

  @Override
  public DispatchJobDto enqueueJob(CommunicationDispatchCommand command, String idempotencyKey) {
    if (command == null) {
      throw new BadRequestException("Communication dispatch command cannot be null", "COMMAND_REQUIRED");
    }
    if (command.traineeId() == null) {
      throw new BadRequestException("Trainee ID is required for communication dispatch", "TRAINEE_ID_REQUIRED");
    }
    if (command.channel() == null) {
      throw new BadRequestException("Communication channel is required", "CHANNEL_REQUIRED");
    }
    if (command.templateCode() == null || command.templateCode().isBlank()) {
      throw new BadRequestException("Template code is required for communication dispatch", "TEMPLATE_CODE_REQUIRED");
    }

    // 1. Authoritative scope check
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(command.traineeId());
    }

    // 2. Validate trainee exists
    Trainee trainee = traineeRepository.findById(command.traineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));

    // 3. Resolve or generate idempotency key
    String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
        ? idempotencyKey.trim()
        : UUID.randomUUID().toString();

    // 4. Check for existing job by idempotency key
    DispatchJobDto existing = findJobByIdempotencyKey(effectiveKey);
    if (existing != null) {
      log.info("[DispatchQueue] Returning existing dispatch job {} for idempotency key {}", existing.jobId(), effectiveKey);
      return existing;
    }

    // 5. Resolve multi-tenant provider ID & actor user ID
    Long providerId = null;
    if (rateLimiter != null) {
      providerId = rateLimiter.resolveAuthoritativeProviderId(trainee.getId(), null);
    }
    Long effectiveUserId = resolveEffectiveUserId(command.initiatedByUserId());

    // 6. Serialize parameters to JSON
    String paramsJson = serializeParameters(command.templateParameters());

    // 7. Resolve channel and purpose numeric IDs
    long channelId = mapChannelToId(command.channel());
    long purposeId = mapPurposeToId(command.purpose());
    String deliveryMode = properties.isDemoMode() ? "DEMO" : "LIVE";

    // 8. Insert PENDING job into database
    KeyHolder keyHolder = new GeneratedKeyHolder();
    try {
      final Long finalProviderId = providerId;
      jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO communication_dispatch_jobs ("
                + "idempotency_key, trainee_id, training_provider_id, channel_id, purpose_id, "
                + "template_code, template_parameters_json, job_status, attempt_count, max_attempts, "
                + "delivery_mode, initiated_by_user_id, initiated_by_service, tenant_scope_type, "
                + "scheduled_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, effectiveKey);
        ps.setLong(2, trainee.getId());
        if (finalProviderId != null) {
          ps.setLong(3, finalProviderId);
        } else {
          ps.setNull(3, java.sql.Types.BIGINT);
        }
        ps.setLong(4, channelId);
        ps.setLong(5, purposeId);
        ps.setString(6, command.templateCode().trim());
        ps.setString(7, paramsJson);
        ps.setString(8, STATUS_PENDING);
        ps.setInt(9, DEFAULT_MAX_ATTEMPTS);
        ps.setString(10, deliveryMode);
        if (effectiveUserId != null) {
          ps.setLong(11, effectiveUserId);
        } else {
          ps.setNull(11, java.sql.Types.BIGINT);
        }
        ps.setString(12, "COMMUNICATION_DISPATCH_QUEUE");
        ps.setString(13, finalProviderId != null ? "PROVIDER" : "NATIONWIDE");
        return ps;
      }, keyHolder);
    } catch (DuplicateKeyException ex) {
      log.info("[DispatchQueue] Concurrent insert hit duplicate idempotency key {}", effectiveKey);
      return findJobByIdempotencyKey(effectiveKey);
    }

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to retrieve generated primary key for dispatch job");
    }
    Long jobId = key.longValue();

    log.info("[DispatchQueue] Enqueued dispatch job {}: traineeId={}, channel={}, mode={}, idempotencyKey={}",
        jobId, trainee.getId(), command.channel(), deliveryMode, effectiveKey);

    return getJobById(jobId);
  }

  @Override
  public NotificationDispatchResponse claimAndProcessJob(Long jobId, String workerId) {
    if (jobId == null) {
      return null;
    }
    String effectiveWorkerId = (workerId != null && !workerId.isBlank()) ? workerId : "worker-" + Thread.currentThread().getName();

    // 1. Concurrency-safe atomic claim
    int rowsClaimed = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = ?, locked_at = NOW(), locked_by = ?, updated_at = NOW() "
            + "WHERE job_id = ? AND (job_status = ? OR (job_status = ? AND locked_at < NOW() - INTERVAL ? MINUTE))",
        STATUS_PROCESSING,
        effectiveWorkerId,
        jobId,
        STATUS_PENDING,
        STATUS_PROCESSING,
        DEFAULT_STALE_THRESHOLD_MINUTES
    );

    if (rowsClaimed == 0) {
      log.debug("[DispatchQueue] Job {} could not be claimed by worker {}", jobId, effectiveWorkerId);
      return null;
    }

    DispatchJobDto job = getJobById(jobId);
    if (job == null) {
      return null;
    }

    int currentAttemptNumber = job.attemptCount() + 1;
    log.info("[DispatchQueue] Claimed job {}: attempt {}/{} by worker {}",
        jobId, currentAttemptNumber, job.maxAttempts(), effectiveWorkerId);

    // 2. Insert attempt audit row
    Long attemptId = recordDispatchAttempt(jobId, currentAttemptNumber, STATUS_PROCESSING, null, null, null, null);

    // 3. Pre-dispatch DPDP Consent Revalidation & Outbound Dispatch under authorized nationwide internal service context
    InternalServicePrincipal principal = internalSecurityExecutionSupport.createAuthorizedNationwidePrincipal("COMMUNICATION_DISPATCH_QUEUE_WORKER");

    NotificationDispatchResponse response = null;
    try {
      response = internalSecurityExecutionSupport.runAsInternalService(principal, () -> {
        // 3.1 DPDP Consent Revalidation
        String channelCode = job.channel().name();
        String purposeCode = job.purpose() != null ? job.purpose().name() : CommunicationPurpose.OPERATIONAL.name();
        consentValidator.validateCommunicationConsent(job.traineeId(), channelCode, purposeCode);

        // 3.2 Outbound dispatch
        CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
            job.traineeId(),
            job.channel(),
            job.purpose(),
            job.templateCode(),
            job.templateParameters(),
            job.initiatedByUserId(),
            null,
            null
        );
        return notificationDispatcherService.dispatchNotification(cmd);
      });
    } catch (CommunicationConsentException ex) {
      log.warn("[DispatchQueue] DPDP consent revoked or missing for queued job {}: {}", jobId, ex.getErrorCode());
      markJobDeadLetter(jobId, currentAttemptNumber, "CONSENT_REVOKED", ex.getErrorCode(), ex.getMessage());
      updateAttempt(attemptId, "FAILED", null, null, ex.getErrorCode(), ex.getMessage());
      return null;
    } catch (CommunicationRateLimitException rateEx) {
      log.warn("[DispatchQueue] Job {} hit rate limit during dispatch: {} (retryAfter={})",
          jobId, rateEx.getErrorCode(), rateEx.getRetryAfterSeconds());
      handleRateLimitRetry(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, rateEx);
      return null;
    } catch (Exception ex) {
      handleDispatchException(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, ex);
      return null;
    }

    // 4. Evaluate outcome
    if (response != null && (response.accepted() || "SIMULATED".equalsIgnoreCase(response.status()))) {
      // Success / Simulation completed
      markJobCompleted(jobId, currentAttemptNumber, response.communicationLogId());
      updateAttempt(attemptId, STATUS_COMPLETED, response.providerName(), response.providerMessageId(), null, null);
      log.info("[DispatchQueue] Job {} completed successfully: commLogId={}, status={}",
          jobId, response.communicationLogId(), response.status());
      return response;
    }

    // Provider rejected or failed dispatch
    String errCode = response != null && response.errorCode() != null ? response.errorCode() : "PROVIDER_DELIVERY_FAILED";
    String errMsg = response != null && response.errorExplanation() != null ? response.errorExplanation() : "Provider rejected message dispatch";
    handleProviderFailure(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, response != null ? response.providerName() : null, errCode, errMsg);
    return response;
  }

  private void markJobCompleted(Long jobId, int attemptCount, Long communicationLogId) {
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, communication_log_id = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ?",
        STATUS_COMPLETED,
        attemptCount,
        communicationLogId,
        jobId
    );
  }

  private void markJobDeadLetter(Long jobId, int attemptCount, String reason, String errorCode, String errorMessage) {
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, dead_letter_reason = ?, dead_lettered_at = NOW(), "
            + "last_error_code = ?, last_error_message = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ?",
        STATUS_DEAD_LETTER,
        attemptCount,
        reason,
        errorCode,
        truncatedMessage,
        jobId
    );
  }

  private void scheduleJobRetry(Long jobId, int attemptCount, int delaySeconds, String errorCode, String errorMessage) {
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, scheduled_at = NOW() + INTERVAL ? SECOND, "
            + "last_error_code = ?, last_error_message = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ?",
        STATUS_PENDING,
        attemptCount,
        delaySeconds,
        errorCode,
        truncatedMessage,
        jobId
    );
  }

  private void handleRateLimitRetry(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, CommunicationRateLimitException rateEx) {
    if (currentAttempt < maxAttempts) {
      long delay = rateEx.getRetryAfterSeconds() > 0 ? rateEx.getRetryAfterSeconds() : 30L;
      scheduleJobRetry(jobId, currentAttempt, (int) delay, rateEx.getErrorCode(), rateEx.getMessage());
      updateAttempt(attemptId, "RATE_LIMITED_RETRY_SCHEDULED", null, null, rateEx.getErrorCode(), rateEx.getMessage());
      log.info("[DispatchQueue] Scheduled retry for rate-limited job {} in {} seconds", jobId, delay);
    } else {
      markJobDeadLetter(jobId, currentAttempt, "RATE_LIMIT_EXCEEDED_EXHAUSTED", rateEx.getErrorCode(), rateEx.getMessage());
      updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, rateEx.getErrorCode(), rateEx.getMessage());
      log.warn("[DispatchQueue] Rate-limited job {} exhausted all {} attempts. Moved to DEAD_LETTER", jobId, maxAttempts);
    }
  }

  private void handleDispatchException(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, Exception ex) {
    String errorCode = (ex instanceof in.gov.sih.sih26135.exception.BaseException be) ? be.getErrorCode() : "DISPATCH_EXCEPTION";
    String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

    if (isPermanentFailure(errorCode) || ex instanceof BadRequestException || ex instanceof IllegalArgumentException) {
      log.warn("[DispatchQueue] Permanent failure on job {}: {}. Transitioning to DEAD_LETTER", jobId, errorCode);
      markJobDeadLetter(jobId, currentAttempt, errorCode, errorCode, errorMsg);
      updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, errorCode, errorMsg);
      return;
    }

    if (currentAttempt < maxAttempts) {
      int backoff = calculateExponentialBackoffWithJitter(currentAttempt);
      scheduleJobRetry(jobId, currentAttempt, backoff, errorCode, errorMsg);
      updateAttempt(attemptId, "FAILED_RETRY_SCHEDULED", null, null, errorCode, errorMsg);
      log.info("[DispatchQueue] Transient exception on job {}: {}. Scheduled retry #{} in {}s",
          jobId, errorCode, currentAttempt + 1, backoff);
    } else {
      markJobDeadLetter(jobId, currentAttempt, "MAX_RETRIES_EXCEEDED", errorCode, errorMsg);
      updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, errorCode, errorMsg);
      log.warn("[DispatchQueue] Job {} exhausted max retries ({}). Moved to DEAD_LETTER: {}", jobId, maxAttempts, errorCode);
    }
  }

  private void handleProviderFailure(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, String providerName, String errorCode, String errorMessage) {
    if (isPermanentFailure(errorCode)) {
      log.warn("[DispatchQueue] Permanent provider rejection on job {}: {}. Transitioning to DEAD_LETTER", jobId, errorCode);
      markJobDeadLetter(jobId, currentAttempt, errorCode, errorCode, errorMessage);
      updateAttempt(attemptId, STATUS_DEAD_LETTER, providerName, null, errorCode, errorMessage);
      return;
    }

    if (currentAttempt < maxAttempts) {
      int backoff = calculateExponentialBackoffWithJitter(currentAttempt);
      scheduleJobRetry(jobId, currentAttempt, backoff, errorCode, errorMessage);
      updateAttempt(attemptId, "FAILED_RETRY_SCHEDULED", providerName, null, errorCode, errorMessage);
      log.info("[DispatchQueue] Transient failure on job {}: {}. Scheduled retry #{} in {}s",
          jobId, errorCode, currentAttempt + 1, backoff);
    } else {
      markJobDeadLetter(jobId, currentAttempt, "MAX_RETRIES_EXCEEDED", errorCode, errorMessage);
      updateAttempt(attemptId, STATUS_DEAD_LETTER, providerName, null, errorCode, errorMessage);
      log.warn("[DispatchQueue] Job {} exhausted max retries ({}). Moved to DEAD_LETTER: {}", jobId, maxAttempts, errorCode);
    }
  }

  public static boolean isPermanentFailure(String errorCode) {
    if (errorCode == null) {
      return false;
    }
    return switch (errorCode.toUpperCase()) {
      case "CONSENT_REVOKED", "NO_ACTIVE_CONSENT", "BLOCKED_NO_CONSENT",
           "INVALID_TEMPLATE", "TEMPLATE_CODE_REQUIRED", "UNKNOWN_TEMPLATE",
           "INVALID_PARAMETER", "PARAMETER_ALLOWLIST_VIOLATION", "UNSAFE_CONTENT",
           "UNTRUSTED_URL", "SURVEY_MILESTONE_INVALID",
           "MISSING_RECIPIENT_PHONE", "MISSING_RECIPIENT_EMAIL", "TRAINEE_NOT_FOUND",
           "NON_TRAINEE_RECIPIENT_NOT_SUPPORTED", "UNSUPPORTED_CHANNEL",
           "BAD_REQUEST", "UNAUTHORIZED", "ACTOR_USER_MISMATCH", "AUTHENTICATED_USER_UNRESOLVED",
           "RECIPIENT_UNREACHABLE", "INVALID_PHONE_NUMBER" -> true;
      default -> false;
    };
  }

  public static int calculateExponentialBackoffWithJitter(int attempt) {
    int base = switch (attempt) {
      case 1 -> 30;
      case 2 -> 120;
      default -> 300;
    };
    int jitter = ThreadLocalRandom.current().nextInt(0, 10 * attempt);
    return base + jitter;
  }

  private Long recordDispatchAttempt(Long jobId, int attemptNumber, String status, String providerName, String providerMessageId, String errorCode, String errorMessage) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO communication_dispatch_attempts ("
              + "job_id, attempt_number, attempt_status, provider_name, provider_message_id, "
              + "error_code, error_message, attempted_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, jobId);
      ps.setInt(2, attemptNumber);
      ps.setString(3, status);
      ps.setString(4, providerName);
      ps.setString(5, providerMessageId);
      ps.setString(6, errorCode);
      ps.setString(7, truncatedMessage);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    return key != null ? key.longValue() : null;
  }

  private void updateAttempt(Long attemptId, String status, String providerName, String providerMessageId, String errorCode, String errorMessage) {
    if (attemptId == null) {
      return;
    }
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    jdbcTemplate.update(
        "UPDATE communication_dispatch_attempts SET "
            + "attempt_status = ?, provider_name = COALESCE(?, provider_name), "
            + "provider_message_id = COALESCE(?, provider_message_id), error_code = ?, error_message = ?, completed_at = NOW() "
            + "WHERE attempt_id = ?",
        status,
        providerName,
        providerMessageId,
        errorCode,
        truncatedMessage,
        attemptId
    );
  }

  @Override
  public DispatchJobDto getJobById(Long jobId) {
    if (jobId == null) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM communication_dispatch_jobs WHERE job_id = ?",
          jobRowMapper(),
          jobId
      );
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  public DispatchJobDto findJobByIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM communication_dispatch_jobs WHERE idempotency_key = ?",
          jobRowMapper(),
          idempotencyKey.trim()
      );
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  @Override
  public List<DispatchAttemptDto> getAttemptsByJobId(Long jobId) {
    if (jobId == null) {
      return Collections.emptyList();
    }
    return jdbcTemplate.query(
        "SELECT * FROM communication_dispatch_attempts WHERE job_id = ? ORDER BY attempt_number ASC",
        attemptRowMapper(),
        jobId
    );
  }

  @Override
  public List<DispatchJobDto> getDeadLetterJobs(int offset, int limit) {
    int safeOffset = Math.max(0, offset);
    int safeLimit = (limit > 0 && limit <= 100) ? limit : 20;
    return jdbcTemplate.query(
        "SELECT * FROM communication_dispatch_jobs WHERE job_status = ? ORDER BY dead_lettered_at DESC LIMIT ? OFFSET ?",
        jobRowMapper(),
        STATUS_DEAD_LETTER,
        safeLimit,
        safeOffset
    );
  }

  @Override
  public DispatchJobDto replayDeadLetterJob(Long jobId, String operatorUsername) {
    if (jobId == null) {
      throw new BadRequestException("Job ID is required for dead-letter replay", "JOB_ID_REQUIRED");
    }

    DispatchJobDto job = getJobById(jobId);
    if (job == null) {
      throw new ResourceNotFoundException("DispatchJob", "id");
    }

    if (!STATUS_DEAD_LETTER.equalsIgnoreCase(job.jobStatus())) {
      throw new BadRequestException("Job is not in DEAD_LETTER status (current: " + job.jobStatus() + ")", "JOB_NOT_DEAD_LETTER");
    }

    // 1. Authoritative DPDP consent check before re-enqueuing
    String channelCode = job.channel().name();
    String purposeCode = job.purpose() != null ? job.purpose().name() : CommunicationPurpose.OPERATIONAL.name();
    consentValidator.validateCommunicationConsent(job.traineeId(), channelCode, purposeCode);

    // 2. Authoritative rate limiting quota reservation
    if (rateLimiter != null) {
      Long providerId = rateLimiter.resolveAuthoritativeProviderId(job.traineeId(), job.trainingProviderId());
      rateLimiter.checkAndReserveQuota(job.traineeId(), job.channel(), providerId, job.templateCode(), null);
    }

    // 3. Reset job status to PENDING
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = 0, scheduled_at = NOW(), dead_letter_reason = NULL, "
            + "dead_lettered_at = NULL, last_error_code = NULL, last_error_message = NULL, "
            + "locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ?",
        STATUS_PENDING,
        jobId
    );

    log.info("[DispatchQueue] Operator '{}' replayed dead-letter job {}. Re-enqueued in PENDING status",
        operatorUsername, jobId);

    // 4. Trigger asynchronous execution
    dispatchExecutor.execute(() -> {
      try {
        claimAndProcessJob(jobId, "replay-" + (operatorUsername != null ? operatorUsername : "operator"));
      } catch (Exception ex) {
        log.error("[DispatchQueue] Replay execution failed for job {}: {}", jobId, ex.getMessage(), ex);
      }
    });

    return getJobById(jobId);
  }

  @Override
  public int recoverStaleJobs(int staleThresholdMinutes) {
    int threshold = staleThresholdMinutes > 0 ? staleThresholdMinutes : DEFAULT_STALE_THRESHOLD_MINUTES;
    int recovered = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_status = ? AND locked_at < NOW() - INTERVAL ? MINUTE",
        STATUS_PENDING,
        STATUS_PROCESSING,
        threshold
    );
    if (recovered > 0) {
      log.warn("[DispatchQueue] Recovered {} stale dispatch jobs locked longer than {} minutes", recovered, threshold);
    }
    return recovered;
  }

  /**
   * Periodic scheduler that finds due PENDING jobs and stale locked jobs.
   */
  @Scheduled(fixedDelay = 5000)
  public void processPendingQueueScheduler() {
    if (!isQueueTableAvailable()) {
      return;
    }

    try {
      // 1. Recover stale locked jobs
      recoverStaleJobs(DEFAULT_STALE_THRESHOLD_MINUTES);

      // 2. Fetch due pending jobs
      List<Long> dueJobIds = jdbcTemplate.query(
          "SELECT job_id FROM communication_dispatch_jobs WHERE job_status = ? AND scheduled_at <= NOW() ORDER BY scheduled_at ASC LIMIT 20",
          (rs, rowNum) -> rs.getLong("job_id"),
          STATUS_PENDING
      );

      for (Long jobId : dueJobIds) {
        dispatchExecutor.execute(() -> {
          try {
            claimAndProcessJob(jobId, "scheduler-" + Thread.currentThread().getName());
          } catch (Exception ex) {
            log.error("[DispatchQueue] Scheduler execution error for job {}: {}", jobId, ex.getMessage());
          }
        });
      }
    } catch (Exception ex) {
      log.debug("[DispatchQueue] Notice during queue scheduler run: {}", ex.getMessage());
    }
  }

  private boolean isQueueTableAvailable() {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'communication_dispatch_jobs'",
          Integer.class
      );
      return count != null && count > 0;
    } catch (Exception ex) {
      return false;
    }
  }

  private RowMapper<DispatchJobDto> jobRowMapper() {
    return (rs, rowNum) -> {
      Long jobId = rs.getLong("job_id");
      String key = rs.getString("idempotency_key");
      Long traineeId = rs.getLong("trainee_id");
      Long providerId = getNullableLong(rs, "training_provider_id");
      long channelIdNum = rs.getLong("channel_id");
      CommunicationChannel channel = mapIdToChannel(channelIdNum);
      long purposeIdNum = rs.getLong("purpose_id");
      CommunicationPurpose purpose = mapIdToPurpose(purposeIdNum);
      String tplCode = rs.getString("template_code");
      String paramsJson = rs.getString("template_parameters_json");
      Map<String, String> params = deserializeParameters(paramsJson);
      String status = rs.getString("job_status");
      int attempts = rs.getInt("attempt_count");
      int maxAttempts = rs.getInt("max_attempts");
      String deliveryMode = rs.getString("delivery_mode");
      Long initiatedUserId = getNullableLong(rs, "initiated_by_user_id");
      String initiatedService = rs.getString("initiated_by_service");
      String tenantScopeType = rs.getString("tenant_scope_type");
      Timestamp schedTs = rs.getTimestamp("scheduled_at");
      LocalDateTime scheduledAt = schedTs != null ? schedTs.toLocalDateTime() : null;
      Timestamp lockedTs = rs.getTimestamp("locked_at");
      LocalDateTime lockedAt = lockedTs != null ? lockedTs.toLocalDateTime() : null;
      String lockedBy = rs.getString("locked_by");
      Long commLogId = getNullableLong(rs, "communication_log_id");
      String errCode = rs.getString("last_error_code");
      String errMsg = rs.getString("last_error_message");
      String dlReason = rs.getString("dead_letter_reason");
      Timestamp dlTs = rs.getTimestamp("dead_lettered_at");
      LocalDateTime deadLetteredAt = dlTs != null ? dlTs.toLocalDateTime() : null;
      Timestamp createdTs = rs.getTimestamp("created_at");
      LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
      Timestamp updatedTs = rs.getTimestamp("updated_at");
      LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;

      return new DispatchJobDto(
          jobId,
          key,
          traineeId,
          providerId,
          channel,
          purpose,
          tplCode,
          params,
          status,
          attempts,
          maxAttempts,
          deliveryMode,
          initiatedUserId,
          initiatedService,
          tenantScopeType,
          scheduledAt,
          lockedAt,
          lockedBy,
          commLogId,
          errCode,
          errMsg,
          dlReason,
          deadLetteredAt,
          createdAt,
          updatedAt
      );
    };
  }

  private RowMapper<DispatchAttemptDto> attemptRowMapper() {
    return (rs, rowNum) -> {
      Long attemptId = rs.getLong("attempt_id");
      Long jobId = rs.getLong("job_id");
      int attemptNum = rs.getInt("attempt_number");
      String status = rs.getString("attempt_status");
      String provName = rs.getString("provider_name");
      String provMsgId = rs.getString("provider_message_id");
      String errCode = rs.getString("error_code");
      String errMsg = rs.getString("error_message");
      Timestamp attTs = rs.getTimestamp("attempted_at");
      LocalDateTime attemptedAt = attTs != null ? attTs.toLocalDateTime() : null;
      Timestamp compTs = rs.getTimestamp("completed_at");
      LocalDateTime completedAt = compTs != null ? compTs.toLocalDateTime() : null;

      return new DispatchAttemptDto(
          attemptId,
          jobId,
          attemptNum,
          status,
          provName,
          provMsgId,
          errCode,
          errMsg,
          attemptedAt,
          completedAt
      );
    };
  }

  private String serializeParameters(Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(parameters);
    } catch (Exception ex) {
      log.warn("[DispatchQueue] Error serializing template parameters to JSON: {}", ex.getMessage());
      return "{}";
    }
  }

  private Map<String, String> deserializeParameters(String json) {
    if (json == null || json.isBlank() || "{}".equals(json.trim())) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (Exception ex) {
      log.warn("[DispatchQueue] Error deserializing template parameters from JSON: {}", ex.getMessage());
      return Collections.emptyMap();
    }
  }

  private Long getNullableLong(ResultSet rs, String columnLabel) throws SQLException {
    Object obj = rs.getObject(columnLabel);
    if (obj == null) {
      return null;
    }
    if (obj instanceof Number num) {
      return num.longValue();
    }
    return Long.valueOf(obj.toString());
  }

  private Long resolveEffectiveUserId(Long callerSuppliedUserId) {
    if (securityScopeValidator != null) {
      Long authUserId = securityScopeValidator.resolveAuthenticatedUserId();
      if (authUserId != null) {
        return authUserId;
      }
    }
    return callerSuppliedUserId;
  }

  private long mapChannelToId(CommunicationChannel channel) {
    if (channel == null) {
      return 1L; // SMS
    }
    return switch (channel) {
      case SMS -> 1L;
      case WHATSAPP -> 2L;
      case EMAIL -> 3L;
      case PHONE -> 4L;
    };
  }

  private CommunicationChannel mapIdToChannel(long id) {
    if (id == 1L) return CommunicationChannel.SMS;
    if (id == 2L) return CommunicationChannel.WHATSAPP;
    if (id == 3L) return CommunicationChannel.EMAIL;
    if (id == 4L) return CommunicationChannel.PHONE;
    return CommunicationChannel.SMS;
  }

  private long mapPurposeToId(CommunicationPurpose purpose) {
    if (purpose == null) {
      return 6L; // OPERATIONAL
    }
    return switch (purpose) {
      case SURVEY_INVITE -> 1L;
      case FOLLOWUP_REMINDER -> 2L;
      case EMPLOYMENT_CHECK -> 3L;
      case WAGE_CHECK -> 4L;
      case RETENTION_CHECK -> 5L;
      case OPERATIONAL, PLACEMENT_ALERT, VERIFICATION_NOTICE -> 6L;
    };
  }

  private CommunicationPurpose mapIdToPurpose(long id) {
    if (id == 1L) return CommunicationPurpose.SURVEY_INVITE;
    if (id == 2L) return CommunicationPurpose.FOLLOWUP_REMINDER;
    if (id == 3L) return CommunicationPurpose.EMPLOYMENT_CHECK;
    if (id == 4L) return CommunicationPurpose.WAGE_CHECK;
    if (id == 5L) return CommunicationPurpose.RETENTION_CHECK;
    return CommunicationPurpose.OPERATIONAL;
  }
}