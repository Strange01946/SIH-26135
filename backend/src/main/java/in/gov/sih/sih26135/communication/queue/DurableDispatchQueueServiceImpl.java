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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
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

  @Autowired(required = false)
  private in.gov.sih.sih26135.actuator.metrics.CommunicationMetricsRecorder communicationMetricsRecorder;

  private final ScheduledExecutorService leaseRenewer = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "dispatch-lease-renewer");
    t.setDaemon(true);
    return t;
  });

  @PreDestroy
  public void shutdown() {
    leaseRenewer.shutdownNow();
  }

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

    // 4. Resolve multi-tenant provider ID & actor user ID
    Long providerId = null;
    if (rateLimiter != null) {
      providerId = rateLimiter.resolveAuthoritativeProviderId(trainee.getId(), null);
    }
    Long effectiveUserId = resolveEffectiveUserId(command.initiatedByUserId());

    // 5. Check for existing job by idempotency key and enforce strict tenant & payload binding
    DispatchJobDto existing = findJobByIdempotencyKey(effectiveKey);
    if (existing != null) {
      return validateAndReturnIdempotentJob(existing, command, providerId);
    }

    // 6. Serialize parameters to JSON
    String paramsJson = serializeParameters(command.templateParameters());

    // 7. Resolve channel and purpose numeric IDs
    long channelId = mapChannelToId(command.channel());
    long purposeId = mapPurposeToId(command.purpose());
    String deliveryMode = properties.getEffectiveModeName();

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
      DispatchJobDto racedJob = findJobByIdempotencyKey(effectiveKey);
      if (racedJob != null) {
        return validateAndReturnIdempotentJob(racedJob, command, providerId);
      }
      throw new IllegalStateException("Failed to resolve existing job after duplicate key collision on " + effectiveKey);
    }

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to retrieve generated primary key for dispatch job");
    }
    Long jobId = key.longValue();

    if (communicationMetricsRecorder != null && command.channel() != null) {
      communicationMetricsRecorder.recordDispatch(command.channel().name(), "QUEUED");
    }

    log.info("[DispatchQueue] Enqueued dispatch job {}: traineeId={}, channel={}, mode={}, idempotencyKey={}",
        jobId, trainee.getId(), command.channel(), deliveryMode, effectiveKey);

    return getJobById(jobId);
  }

  private DispatchJobDto validateAndReturnIdempotentJob(DispatchJobDto existing, CommunicationDispatchCommand command, Long callerProviderId) {
    if (existing == null) {
      return null;
    }

    // 1. Cross-tenant isolation check: never return another tenant's job
    if (!Objects.equals(existing.trainingProviderId(), callerProviderId)) {
      log.warn("[DispatchQueue] Cross-tenant idempotency conflict: key '{}' requested by provider {} but owned by provider {}",
          existing.idempotencyKey(), callerProviderId, existing.trainingProviderId());
      throw new BadRequestException("The specified idempotency key is already in use by another operation", "IDEMPOTENCY_KEY_CONFLICT");
    }

    // 2. Request Content & Identity Binding check:
    boolean traineeMatches = Objects.equals(existing.traineeId(), command.traineeId());
    boolean channelMatches = existing.channel() == command.channel();
    boolean purposeMatches = existing.purpose() == command.purpose();
    boolean templateMatches = Objects.equals(existing.templateCode(), command.templateCode());
    boolean paramsMatch = Objects.equals(existing.templateParameters(), command.templateParameters());

    if (!traineeMatches || !channelMatches || !purposeMatches || !templateMatches || !paramsMatch) {
      log.warn("[DispatchQueue] Idempotency key payload mismatch for key '{}'", existing.idempotencyKey());
      throw new BadRequestException("The specified idempotency key was previously used with different request parameters", "IDEMPOTENCY_KEY_PAYLOAD_MISMATCH");
    }

    log.info("[DispatchQueue] Returning existing dispatch job {} for matching authorized idempotent request (key={})",
        existing.jobId(), existing.idempotencyKey());
    return existing;
  }

  public boolean renewWorkerLease(Long jobId, int leaseVersion) {
    if (jobId == null) {
      return false;
    }
    int rows = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET locked_at = NOW(), updated_at = NOW() "
            + "WHERE job_id = ? AND lease_version = ? AND job_status = ?",
        jobId,
        leaseVersion,
        STATUS_PROCESSING
    );
    return rows > 0;
  }

  @Override
  public NotificationDispatchResponse claimAndProcessJob(Long jobId, String workerId) {
    if (jobId == null) {
      return null;
    }
    String effectiveWorkerId = (workerId != null && !workerId.isBlank()) ? workerId : "worker-" + Thread.currentThread().getName();

    // Bind the claim to the original mode so a demo job can never acquire a live worker.
    int rowsClaimed = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET job_status = ?, lease_version = lease_version + 1, locked_at = NOW(), locked_by = ?, updated_at = NOW() "
            + "WHERE job_id = ? AND (scheduled_at IS NULL OR scheduled_at <= NOW()) AND job_status = ? AND delivery_mode = ?",
        STATUS_PROCESSING,
        effectiveWorkerId,
        jobId,
        STATUS_PENDING,
        properties.getEffectiveModeName()
    );

    if (rowsClaimed == 0) {
      log.debug("[DispatchQueue] Job {} could not be claimed by worker {}", jobId, effectiveWorkerId);
      return null;
    }

    DispatchJobDto job = getJobById(jobId);
    if (job == null) {
      return null;
    }
    final int workerLeaseVersion = job.leaseVersion();

    int currentAttemptNumber = job.attemptCount() + 1;
    log.info("[DispatchQueue] Claimed job {}: attempt {}/{} (lease={}) by worker {}",
        jobId, currentAttemptNumber, job.maxAttempts(), workerLeaseVersion, effectiveWorkerId);

    // 2. Insert attempt audit row
    Long attemptId = recordDispatchAttempt(jobId, currentAttemptNumber, STATUS_PROCESSING, null, null, null, null);

    // 3. Start active worker lease renewal heartbeat to prevent active execution from appearing stale
    ScheduledFuture<?> renewalTask = null;
    try {
      renewalTask = leaseRenewer.scheduleAtFixedRate(() -> {
        try {
          renewWorkerLease(jobId, workerLeaseVersion);
        } catch (Exception ex) {
          log.debug("[DispatchQueue] Lease renewal notice for job {}: {}", jobId, ex.getMessage());
        }
      }, 15, 15, TimeUnit.SECONDS);

      // 4. Pre-dispatch DPDP Consent Revalidation & Outbound Dispatch under authorized nationwide internal service context
      InternalServicePrincipal principal = internalSecurityExecutionSupport.createAuthorizedNationwidePrincipal("COMMUNICATION_DISPATCH_QUEUE_WORKER");

      NotificationDispatchResponse response = null;
      try {
        response = internalSecurityExecutionSupport.runAsInternalService(principal, () -> {
          // 4.1 DPDP Consent Revalidation
          String channelCode = job.channel().name();
          String purposeCode = job.purpose() != null ? job.purpose().name() : CommunicationPurpose.OPERATIONAL.name();
          consentValidator.validateCommunicationConsent(job.traineeId(), channelCode, purposeCode);

          // 4.2 Outbound dispatch with authoritative automatic retry context
          boolean isAutomaticRetry = currentAttemptNumber > 1;
          CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
              job.traineeId(),
              job.channel(),
              job.purpose(),
              job.templateCode(),
              job.templateParameters(),
              job.initiatedByUserId(),
              null,
              null,
              isAutomaticRetry
          );
          return notificationDispatcherService.dispatchNotification(cmd);
        });
      } catch (CommunicationConsentException ex) {
        log.warn("[DispatchQueue] DPDP consent revoked or missing for queued job {}: {}", jobId, ex.getErrorCode());
        boolean marked = markJobDeadLetter(jobId, currentAttemptNumber, null, "CONSENT_REVOKED", ex.getErrorCode(), ex.getMessage(), workerLeaseVersion);
        if (marked) {
          updateAttempt(attemptId, "FAILED", null, null, ex.getErrorCode(), ex.getMessage());
        } else {
          updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during consent revocation handling.");
        }
        return null;
      } catch (CommunicationRateLimitException rateEx) {
        log.warn("[DispatchQueue] Job {} hit rate limit during dispatch: {} (retryAfter={})",
            jobId, rateEx.getErrorCode(), rateEx.getRetryAfterSeconds());
        handleRateLimitRetry(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, rateEx, workerLeaseVersion);
        return null;
      } catch (Exception ex) {
        handleDispatchException(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, ex, workerLeaseVersion);
        return null;
      }

      // 5. Evaluate outcome
      if (response != null && (response.accepted() || "SIMULATED".equalsIgnoreCase(response.status()))) {
        // Success / Simulation completed
        boolean marked = markJobCompleted(jobId, currentAttemptNumber, response.communicationLogId(), workerLeaseVersion);
        if (marked) {
          updateAttempt(attemptId, STATUS_COMPLETED, response.providerName(), response.providerMessageId(), null, null);
          log.info("[DispatchQueue] Job {} completed successfully: commLogId={}, status={}",
              jobId, response.communicationLogId(), response.status());
          return response;
        } else {
          // Fencing conflict: worker was superseded by another worker or recovery!
          log.warn("[DispatchQueue] Fencing rejected completion for job {}: worker lease {} was superseded. Recording SUPERSEDED attempt.",
              jobId, workerLeaseVersion);
          updateAttempt(attemptId, "SUPERSEDED", response.providerName(), response.providerMessageId(),
              "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during provider dispatch. Completion discarded to prevent split-brain state overwrite.");
          return response;
        }
      }

      // Provider rejected or failed dispatch
      String errCode = response != null && response.errorCode() != null ? response.errorCode() : "PROVIDER_DELIVERY_FAILED";
      String errMsg = response != null && response.errorExplanation() != null ? response.errorExplanation() : "Provider rejected message dispatch";
      Long commLogId = response != null ? response.communicationLogId() : null;
      handleProviderFailure(jobId, currentAttemptNumber, job.maxAttempts(), attemptId, response != null ? response.providerName() : null, commLogId, errCode, errMsg, workerLeaseVersion);
      return response;
    } finally {
      if (renewalTask != null) {
        renewalTask.cancel(true);
      }
    }
  }

  private boolean markJobCompleted(Long jobId, int attemptCount, Long communicationLogId, int leaseVersion) {
    int updated = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, communication_log_id = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ? AND lease_version = ?",
        STATUS_COMPLETED,
        attemptCount,
        communicationLogId,
        jobId,
        leaseVersion
    );
    if (updated == 0) {
      log.warn("[DispatchQueue] Fencing conflict on completion for job {}: worker lease {} was superseded. Discarding update.", jobId, leaseVersion);
      return false;
    }
    return true;
  }

  private boolean markJobDeadLetter(Long jobId, int attemptCount, Long communicationLogId, String reason, String errorCode, String errorMessage, int leaseVersion) {
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    int updated = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, communication_log_id = COALESCE(?, communication_log_id), dead_letter_reason = ?, dead_lettered_at = NOW(), "
            + "last_error_code = ?, last_error_message = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ? AND lease_version = ?",
        STATUS_DEAD_LETTER,
        attemptCount,
        communicationLogId,
        reason,
        errorCode,
        truncatedMessage,
        jobId,
        leaseVersion
    );
    if (updated == 0) {
      log.warn("[DispatchQueue] Fencing conflict on dead-letter for job {}: worker lease {} was superseded. Discarding update.", jobId, leaseVersion);
      return false;
    }
    return true;
  }

  private boolean scheduleJobRetry(Long jobId, int attemptCount, Long communicationLogId, int delaySeconds, String errorCode, String errorMessage, int leaseVersion) {
    String truncatedMessage = errorMessage != null && errorMessage.length() > 490 ? errorMessage.substring(0, 490) : errorMessage;
    int updated = jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, attempt_count = ?, communication_log_id = COALESCE(?, communication_log_id), scheduled_at = NOW() + INTERVAL ? SECOND, "
            + "last_error_code = ?, last_error_message = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ? AND lease_version = ?",
        STATUS_PENDING,
        attemptCount,
        communicationLogId,
        delaySeconds,
        errorCode,
        truncatedMessage,
        jobId,
        leaseVersion
    );
    if (updated == 0) {
      log.warn("[DispatchQueue] Fencing conflict on retry schedule for job {}: worker lease {} was superseded. Discarding update.", jobId, leaseVersion);
      return false;
    }
    return true;
  }

  private void handleRateLimitRetry(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, CommunicationRateLimitException rateEx, int leaseVersion) {
    if (currentAttempt < maxAttempts) {
      long delay = rateEx.getRetryAfterSeconds() > 0 ? rateEx.getRetryAfterSeconds() : 30L;
      boolean scheduled = scheduleJobRetry(jobId, currentAttempt, null, (int) delay, rateEx.getErrorCode(), rateEx.getMessage(), leaseVersion);
      if (scheduled) {
        updateAttempt(attemptId, "RATE_LIMITED_RETRY_SCHEDULED", null, null, rateEx.getErrorCode(), rateEx.getMessage());
        log.info("[DispatchQueue] Scheduled retry for rate-limited job {} in {} seconds", jobId, delay);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during rate-limit retry scheduling.");
      }
    } else {
      boolean marked = markJobDeadLetter(jobId, currentAttempt, null, "RATE_LIMIT_EXCEEDED_EXHAUSTED", rateEx.getErrorCode(), rateEx.getMessage(), leaseVersion);
      if (marked) {
        updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, rateEx.getErrorCode(), rateEx.getMessage());
        log.warn("[DispatchQueue] Rate-limited job {} exhausted all {} attempts. Moved to DEAD_LETTER", jobId, maxAttempts);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during dead-letter transition.");
      }
    }
  }

  private void handleDispatchException(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, Exception ex, int leaseVersion) {
    String errorCode = (ex instanceof in.gov.sih.sih26135.exception.BaseException be) ? be.getErrorCode() : "DISPATCH_EXCEPTION";
    String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

    // Ambiguous timeout check: do NOT auto-retry to prevent blind duplicate sends
    if (isAmbiguousTimeout(ex, errorCode, errorMsg)) {
      log.warn("[DispatchQueue] Ambiguous provider timeout on job {}: {}. Moving to DEAD_LETTER to prevent blind duplicate dispatch", jobId, errorCode);
      boolean marked = markJobDeadLetter(jobId, currentAttempt, null, "AMBIGUOUS_PROVIDER_TIMEOUT", "AMBIGUOUS_PROVIDER_TIMEOUT",
          "Provider request timed out; delivery status is ambiguous. Manual verification required before replay to prevent duplicate external dispatch", leaseVersion);
      if (marked) {
        updateAttempt(attemptId, "AMBIGUOUS_TIMEOUT", null, null, "AMBIGUOUS_PROVIDER_TIMEOUT", errorMsg);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during ambiguous timeout handling.");
      }
      return;
    }

    if (isPermanentFailure(errorCode) || ex instanceof BadRequestException || ex instanceof IllegalArgumentException) {
      log.warn("[DispatchQueue] Permanent failure on job {}: {}. Transitioning to DEAD_LETTER", jobId, errorCode);
      boolean marked = markJobDeadLetter(jobId, currentAttempt, null, errorCode, errorCode, errorMsg, leaseVersion);
      if (marked) {
        updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, errorCode, errorMsg);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during permanent failure handling.");
      }
      return;
    }

    if (currentAttempt < maxAttempts) {
      int backoff = calculateExponentialBackoffWithJitter(currentAttempt);
      boolean scheduled = scheduleJobRetry(jobId, currentAttempt, null, backoff, errorCode, errorMsg, leaseVersion);
      if (scheduled) {
        updateAttempt(attemptId, "FAILED_RETRY_SCHEDULED", null, null, errorCode, errorMsg);
        log.info("[DispatchQueue] Transient exception on job {}: {}. Scheduled retry #{} in {}s",
            jobId, errorCode, currentAttempt + 1, backoff);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during retry scheduling.");
      }
    } else {
      boolean marked = markJobDeadLetter(jobId, currentAttempt, null, "MAX_RETRIES_EXCEEDED", errorCode, errorMsg, leaseVersion);
      if (marked) {
        updateAttempt(attemptId, STATUS_DEAD_LETTER, null, null, errorCode, errorMsg);
        log.warn("[DispatchQueue] Job {} exhausted max retries ({}). Moved to DEAD_LETTER: {}", jobId, maxAttempts, errorCode);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", null, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during dead-letter transition.");
      }
    }
  }

  private void handleProviderFailure(Long jobId, int currentAttempt, int maxAttempts, Long attemptId, String providerName, Long communicationLogId, String errorCode, String errorMessage, int leaseVersion) {
    if (isAmbiguousTimeout(null, errorCode, errorMessage)) {
      log.warn("[DispatchQueue] Ambiguous provider timeout reported for job {}: {}. Moving to DEAD_LETTER to prevent duplicate dispatch", jobId, errorCode);
      boolean marked = markJobDeadLetter(jobId, currentAttempt, communicationLogId, "AMBIGUOUS_PROVIDER_TIMEOUT", errorCode, errorMessage, leaseVersion);
      if (marked) {
        updateAttempt(attemptId, "AMBIGUOUS_TIMEOUT", providerName, null, "AMBIGUOUS_PROVIDER_TIMEOUT", errorMessage);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", providerName, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during ambiguous timeout handling.");
      }
      return;
    }

    if (isPermanentFailure(errorCode)) {
      log.warn("[DispatchQueue] Permanent provider rejection on job {}: {}. Transitioning to DEAD_LETTER", jobId, errorCode);
      boolean marked = markJobDeadLetter(jobId, currentAttempt, communicationLogId, errorCode, errorCode, errorMessage, leaseVersion);
      if (marked) {
        updateAttempt(attemptId, STATUS_DEAD_LETTER, providerName, null, errorCode, errorMessage);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", providerName, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during permanent failure handling.");
      }
      return;
    }

    if (currentAttempt < maxAttempts) {
      int backoff = calculateExponentialBackoffWithJitter(currentAttempt);
      boolean scheduled = scheduleJobRetry(jobId, currentAttempt, communicationLogId, backoff, errorCode, errorMessage, leaseVersion);
      if (scheduled) {
        updateAttempt(attemptId, "FAILED_RETRY_SCHEDULED", providerName, null, errorCode, errorMessage);
        log.info("[DispatchQueue] Transient failure on job {}: {}. Scheduled retry #{} in {}s",
            jobId, errorCode, currentAttempt + 1, backoff);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", providerName, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during retry scheduling.");
      }
    } else {
      boolean marked = markJobDeadLetter(jobId, currentAttempt, communicationLogId, "MAX_RETRIES_EXCEEDED", errorCode, errorMessage, leaseVersion);
      if (marked) {
        updateAttempt(attemptId, STATUS_DEAD_LETTER, providerName, null, errorCode, errorMessage);
        log.warn("[DispatchQueue] Job {} exhausted max retries ({}). Moved to DEAD_LETTER: {}", jobId, maxAttempts, errorCode);
      } else {
        updateAttempt(attemptId, "SUPERSEDED", providerName, null, "WORKER_LEASE_SUPERSEDED", "Worker lease was superseded during dead-letter transition.");
      }
    }
  }

  public static boolean isAmbiguousTimeout(Throwable ex, String errorCode, String errorMessage) {
    if (ex != null) {
      if (ex instanceof java.net.SocketTimeoutException) {
        return true;
      }
      if (ex instanceof org.springframework.web.client.ResourceAccessException rae) {
        if (rae.getCause() instanceof java.net.SocketTimeoutException) {
          return true;
        }
        String msg = rae.getMessage() != null ? rae.getMessage().toLowerCase() : "";
        if (msg.contains("read timed out") || msg.contains("read timeout") || msg.contains("socket timeout")) {
          return true;
        }
      }
      if (ex instanceof org.springframework.web.client.HttpServerErrorException.GatewayTimeout) {
        return true;
      }
      Throwable cause = ex.getCause();
      while (cause != null) {
        if (cause instanceof java.net.SocketTimeoutException) {
          return true;
        }
        cause = cause.getCause();
      }
      String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
      if (msg.contains("read timed out") || msg.contains("socket timeout") || msg.contains("gateway timeout")
          || (msg.contains("timed out") && !msg.contains("connect")) || (msg.contains("timeout") && !msg.contains("connect"))) {
        return true;
      }
    }
    if (errorCode != null) {
      String ec = errorCode.toUpperCase();
      if ("AMBIGUOUS_PROVIDER_TIMEOUT".equals(ec) || "PROVIDER_TIMEOUT".equals(ec)
          || "SOCKET_TIMEOUT".equals(ec) || "GATEWAY_TIMEOUT".equals(ec) || "504".equals(ec)
          || (ec.contains("TIMEOUT") && !ec.contains("CONNECT"))) {
        return true;
      }
    }
    if (errorMessage != null) {
      String em = errorMessage.toLowerCase();
      if (em.contains("read timed out") || em.contains("socket timeout") || em.contains("gateway timeout")
          || em.contains("ambiguous provider timeout") || em.contains("response timed out")
          || (em.contains("timeout") && !em.contains("connect"))) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAmbiguousTimeout(Exception ex, String errorCode) {
    return isAmbiguousTimeout(ex, errorCode, ex != null ? ex.getMessage() : null);
  }

  public static boolean isDefiniteConnectionFailure(Throwable ex, String errorCode) {
    if (ex != null) {
      if (ex instanceof java.net.ConnectException
          || ex instanceof java.net.UnknownHostException
          || ex instanceof java.net.NoRouteToHostException) {
        return true;
      }
      if (ex instanceof org.springframework.web.client.ResourceAccessException rae) {
        Throwable cause = rae.getCause();
        if (cause instanceof java.net.ConnectException
            || cause instanceof java.net.UnknownHostException
            || cause instanceof java.net.NoRouteToHostException) {
          return true;
        }
        String msg = rae.getMessage() != null ? rae.getMessage().toLowerCase() : "";
        if (msg.contains("connection refused") || msg.contains("connect timed out") || msg.contains("unknown host")) {
          return true;
        }
      }
      Throwable cause = ex.getCause();
      while (cause != null) {
        if (cause instanceof java.net.ConnectException
            || cause instanceof java.net.UnknownHostException
            || cause instanceof java.net.NoRouteToHostException) {
          return true;
        }
        cause = cause.getCause();
      }
      String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
      if (msg.contains("connection refused") || msg.contains("connect timed out") || msg.contains("unknown host")) {
        return true;
      }
    }
    if (errorCode != null) {
      String ec = errorCode.toUpperCase();
      return "PROVIDER_CONNECT_FAILURE".equals(ec) || "CONNECT_EXCEPTION".equals(ec)
          || "CONNECTION_REFUSED".equals(ec) || "CONNECT_TIMED_OUT".equals(ec)
          || "TWILIO_CONNECTION_ERROR".equals(ec) || "SENDGRID_CONNECTION_ERROR".equals(ec);
    }
    return false;
  }

  public static boolean isPermanentFailure(String errorCode) {
    if (errorCode == null) {
      return false;
    }
    String ec = errorCode.toUpperCase();
    if (ec.startsWith("4") && ec.length() == 3) {
      return true; // Any 4xx HTTP status code (400, 401, 403, 404, 422, etc.)
    }
    return switch (ec) {
      case "CONSENT_REVOKED", "NO_ACTIVE_CONSENT", "BLOCKED_NO_CONSENT",
           "INVALID_TEMPLATE", "TEMPLATE_CODE_REQUIRED", "UNKNOWN_TEMPLATE",
           "INVALID_PARAMETER", "PARAMETER_ALLOWLIST_VIOLATION", "UNSAFE_CONTENT",
           "UNTRUSTED_URL", "SURVEY_MILESTONE_INVALID",
           "MISSING_RECIPIENT_PHONE", "MISSING_RECIPIENT_EMAIL", "TRAINEE_NOT_FOUND",
           "NON_TRAINEE_RECIPIENT_NOT_SUPPORTED", "UNSUPPORTED_CHANNEL",
           "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "ACTOR_USER_MISMATCH", "AUTHENTICATED_USER_UNRESOLVED",
           "RECIPIENT_UNREACHABLE", "INVALID_PHONE_NUMBER",
           "21211", "21610", "21614", "21408" -> true;
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
            + "WHERE attempt_id = ? AND (completed_at IS NULL OR attempt_status = 'PROCESSING')",
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
  public List<DispatchJobDto> getDeadLetterJobsByProvider(Long providerId, int offset, int limit) {
    if (providerId == null) {
      return Collections.emptyList();
    }
    int safeOffset = Math.max(0, offset);
    int safeLimit = (limit > 0 && limit <= 100) ? limit : 20;
    return jdbcTemplate.query(
        "SELECT * FROM communication_dispatch_jobs WHERE job_status = ? AND training_provider_id = ? ORDER BY dead_lettered_at DESC LIMIT ? OFFSET ?",
        jobRowMapper(),
        STATUS_DEAD_LETTER,
        providerId,
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
    if (!properties.getEffectiveModeName().equals(job.deliveryMode())) {
      throw new BadRequestException("Job delivery mode does not match the configured worker", "JOB_DELIVERY_MODE_MISMATCH");
    }

    // 1. Authoritative DPDP consent check before re-enqueuing
    String channelCode = job.channel().name();
    String purposeCode = job.purpose() != null ? job.purpose().name() : CommunicationPurpose.OPERATIONAL.name();
    consentValidator.validateCommunicationConsent(job.traineeId(), channelCode, purposeCode);

    // Rate limit quota reservation will occur during actual dispatch execution in NotificationDispatcherServiceImpl.
    // Intentionally skipped here to prevent double-counting quota reservations during dead-letter replay.

    // 2. Reset job status to PENDING with incremented lease version
    jdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET "
            + "job_status = ?, lease_version = lease_version + 1, attempt_count = 0, scheduled_at = NOW(), dead_letter_reason = NULL, "
            + "dead_lettered_at = NULL, last_error_code = NULL, last_error_message = NULL, "
            + "locked_by = NULL, locked_at = NULL, updated_at = NOW() "
            + "WHERE job_id = ?",
        STATUS_PENDING,
        jobId
    );

    log.info("[DispatchQueue] Operator '{}' replayed dead-letter job {}. Re-enqueued in PENDING status with incremented lease version",
        operatorUsername, jobId);

    // 3. Trigger asynchronous execution
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
    int threshold = staleThresholdMinutes >= 0 ? staleThresholdMinutes : DEFAULT_STALE_THRESHOLD_MINUTES;
    List<Long> staleJobIds = jdbcTemplate.query(
        "SELECT job_id FROM communication_dispatch_jobs "
            + "WHERE job_status = ? AND locked_at < NOW() - INTERVAL ? MINUTE",
        (rs, rowNum) -> rs.getLong("job_id"),
        STATUS_PROCESSING,
        threshold
    );

    int recovered = 0;
    for (Long jobId : staleJobIds) {
      // Stale worker lease expired while in PROCESSING.
      // Because the previous worker was in PROCESSING, an external provider request may have succeeded.
      // Classify its outcome as ambiguous and route to DEAD_LETTER for reconciliation rather than blindly resetting to PENDING!
      int updated = jdbcTemplate.update(
          "UPDATE communication_dispatch_jobs SET "
              + "job_status = ?, lease_version = lease_version + 1, dead_letter_reason = ?, dead_lettered_at = NOW(), "
              + "last_error_code = ?, last_error_message = ?, locked_by = NULL, locked_at = NULL, updated_at = NOW() "
              + "WHERE job_id = ? AND job_status = ? AND locked_at < NOW() - INTERVAL ? MINUTE",
          STATUS_DEAD_LETTER,
          "AMBIGUOUS_STALE_PROCESSING",
          "AMBIGUOUS_STALE_PROCESSING",
          "Stale worker lease expired while in PROCESSING. Outbound delivery status is ambiguous; manual provider reconciliation required before replay to prevent duplicate external send.",
          jobId,
          STATUS_PROCESSING,
          threshold
      );
      if (updated > 0) {
        recovered++;
        log.warn("[DispatchQueue] Stale processing job {} recovered to DEAD_LETTER (AMBIGUOUS_STALE_PROCESSING) with incremented lease version to prevent duplicate send", jobId);
      }
    }
    if (recovered > 0) {
      log.warn("[DispatchQueue] Recovered {} stale dispatch jobs locked longer than {} minutes (incremented lease version)", recovered, threshold);
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
          "SELECT job_id FROM communication_dispatch_jobs WHERE job_status = ? AND delivery_mode = ? "
              + "AND scheduled_at <= NOW() ORDER BY scheduled_at ASC LIMIT 20",
          (rs, rowNum) -> rs.getLong("job_id"),
          STATUS_PENDING,
          properties.getEffectiveModeName()
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

      int leaseVersion = 1;
      try {
        leaseVersion = rs.getInt("lease_version");
        if (leaseVersion <= 0) {
          leaseVersion = 1;
        }
      } catch (Exception ignored) {
      }

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
          updatedAt,
          leaseVersion
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
