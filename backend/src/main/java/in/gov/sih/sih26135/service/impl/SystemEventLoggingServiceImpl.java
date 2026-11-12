package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.config.SystemEventLoggingProperties;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.SystemEventLog;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SystemEventLogMapper;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.SystemEventLogRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SystemEventLoggingService;
import in.gov.sih.sih26135.service.event.SystemEventSanitizer;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Production implementation of {@link SystemEventLoggingService}.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Server-resolved category, severity, timestamp, verified actor attribution, and correlation tags.</li>
 *   <li>Independent {@code REQUIRES_NEW} transaction isolation so failed operations preserve diagnostic events.</li>
 *   <li>Thread-local recursion guard preventing infinite logging loops on persistence failures.</li>
 *   <li>Fail-safe fallback SLF4J structured logging without masking or reversing business exceptions.</li>
 *   <li>Bounded in-memory deduplication and anti-storm cooldown windows.</li>
 * </ul>
 */
@Service
public class SystemEventLoggingServiceImpl implements SystemEventLoggingService {

  private static final Logger log = LoggerFactory.getLogger(SystemEventLoggingServiceImpl.class);

  private static final ThreadLocal<Boolean> IN_EVENT_LOGGING = new ThreadLocal<>();

  private final SystemEventLogRepository systemEventLogRepository;
  private final RefSystemEventCategoryRepository categoryRepository;
  private final RefSystemEventSeverityRepository severityRepository;
  private final UserRepository userRepository;
  private final SystemEventLogMapper mapper;
  private final SystemEventLoggingProperties properties;
  private final TransactionTemplate requiresNewTxTemplate;

  private final ConcurrentMap<String, Long> dedupTimestamps = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> cooldownTimestamps = new ConcurrentHashMap<>();

  public SystemEventLoggingServiceImpl(
      SystemEventLogRepository systemEventLogRepository,
      RefSystemEventCategoryRepository categoryRepository,
      RefSystemEventSeverityRepository severityRepository,
      UserRepository userRepository,
      SystemEventLogMapper mapper,
      SystemEventLoggingProperties properties,
      PlatformTransactionManager transactionManager) {
    this.systemEventLogRepository = Objects.requireNonNull(systemEventLogRepository, "systemEventLogRepository is required");
    this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository is required");
    this.severityRepository = Objects.requireNonNull(severityRepository, "severityRepository is required");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
    this.mapper = Objects.requireNonNull(mapper, "mapper is required");
    this.properties = Objects.requireNonNull(properties, "properties is required");
    this.requiresNewTxTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager is required"));
    this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public Optional<SystemEventLogResponse> logEvent(
      EventCategory category,
      EventSeverity severity,
      String eventCode,
      Long actorUserId,
      String entityType,
      Long entityId,
      String sourceComponent,
      String rawMessage) {
    if (category == null) {
      throw new BadRequestException("Event category is required", "EVENT_CATEGORY_REQUIRED");
    }
    if (severity == null) {
      throw new BadRequestException("Event severity is required", "EVENT_SEVERITY_REQUIRED");
    }
    if (eventCode == null || eventCode.isBlank()) {
      throw new BadRequestException("Event code is required", "EVENT_CODE_REQUIRED");
    }

    String safeCode = SystemEventSanitizer.sanitizeEventCode(eventCode, "SYSTEM_EVENT");
    String safeEntityType = SystemEventSanitizer.sanitizeIdentifier(entityType);
    String safeSource = SystemEventSanitizer.sanitizeIdentifier(sourceComponent);
    CorrelationPair corr = resolveCorrelationPair();

    String requestDedupKey = String.join("|",
        category.name(),
        safeCode,
        safeEntityType != null ? safeEntityType : "",
        entityId != null ? String.valueOf(entityId) : "",
        corr.correlationId(),
        corr.requestId());
    if (isWithinWindow(dedupTimestamps, requestDedupKey, properties.getDedupWindowSeconds())) {
      log.debug("Suppressed duplicate system event within request context: key={}", requestDedupKey);
      return Optional.empty();
    }

    Optional<SystemEventLogResponse> persisted = persistIsolated(
        category, severity, safeCode, actorUserId, safeEntityType, entityId, safeSource, rawMessage, corr);
    if (persisted.isPresent()) {
      markWindow(dedupTimestamps, requestDedupKey);
    }
    return persisted;
  }

  @Override
  public Optional<SystemEventLogResponse> logRateLimitViolation(
      String eventCode,
      Long actorUserId,
      String sanitizedRoute,
      String sourceComponent,
      String rawMessage) {
    String safeCode = SystemEventSanitizer.sanitizeEventCode(eventCode, "RATE_LIMIT_EXCEEDED");
    String safeRoute = SystemEventSanitizer.sanitizeMessage(sanitizedRoute, "/api/unknown");
    String safeSource = SystemEventSanitizer.sanitizeIdentifier(
        sourceComponent != null ? sourceComponent : "EXPORT_RATE_LIMITER");

    String cooldownKey = "RL|" + (actorUserId != null ? actorUserId : "ANON") + "|" + safeCode + "|" + safeRoute;
    int cooldownSeconds = properties.getRateLimitAlertCooldownSeconds();
    if (cooldownSeconds > 0 && isWithinWindow(cooldownTimestamps, cooldownKey, cooldownSeconds)) {
      log.warn("Rate-limit DB event suppressed within {}s cooldown: actorUserId={}, code={}, route={}",
          cooldownSeconds, actorUserId, safeCode, safeRoute);
      return Optional.empty();
    }

    CorrelationPair corr = resolveCorrelationPair();
    String msg = (rawMessage != null && !rawMessage.isBlank() ? rawMessage : "Rate limit exceeded")
        + " route=" + safeRoute;
    Optional<SystemEventLogResponse> persisted = persistIsolated(
        EventCategory.ACCESS,
        EventSeverity.WARNING,
        safeCode,
        actorUserId,
        "RateLimit",
        actorUserId,
        safeSource,
        msg,
        corr);
    if (persisted.isPresent() && cooldownSeconds > 0) {
      markWindow(cooldownTimestamps, cooldownKey);
    }
    return persisted;
  }

  @Override
  public Optional<SystemEventLogResponse> logDataQualitySpike(
      EventSeverity severity,
      String eventCode,
      Long actorUserId,
      String tenantScopeKey,
      String spikeSignalKey,
      String entityType,
      Long entityId,
      int cooldownSeconds,
      String rawMessage) {
    EventSeverity effectiveSeverity = severity != null ? severity : EventSeverity.CRITICAL;
    String safeCode = SystemEventSanitizer.sanitizeEventCode(eventCode, "DATA_QUALITY_CRITICAL_SPIKE");
    String throttleKey = "DQ|" + (tenantScopeKey != null ? tenantScopeKey : "GLOBAL")
        + "|" + safeCode + "|" + (spikeSignalKey != null ? spikeSignalKey : "SCAN");

    if (cooldownSeconds > 0 && isWithinWindow(cooldownTimestamps, throttleKey, cooldownSeconds)) {
      log.info("Suppressed repeated data-quality spike alert within {}s cooldown: key={}",
          cooldownSeconds, throttleKey);
      return Optional.empty();
    }

    CorrelationPair corr = resolveCorrelationPair();
    Optional<SystemEventLogResponse> persisted = persistIsolated(
        EventCategory.INTEGRATION,
        effectiveSeverity,
        safeCode,
        actorUserId,
        SystemEventSanitizer.sanitizeIdentifier(entityType),
        entityId,
        "DATA_QUALITY_ENGINE",
        rawMessage,
        corr);
    if (persisted.isPresent() && cooldownSeconds > 0) {
      markWindow(cooldownTimestamps, throttleKey);
    }
    return persisted;
  }

  @Override
  public void resetThrottleState() {
    dedupTimestamps.clear();
    cooldownTimestamps.clear();
  }

  private Optional<SystemEventLogResponse> persistIsolated(
      EventCategory category,
      EventSeverity severity,
      String safeCode,
      Long actorUserId,
      String safeEntityType,
      Long entityId,
      String safeSource,
      String rawMessage,
      CorrelationPair corr) {
    if (Boolean.TRUE.equals(IN_EVENT_LOGGING.get())) {
      log.warn("Suppressed recursive system event logging invocation: code={}", safeCode);
      return Optional.empty();
    }

    String formattedSummary = SystemEventSanitizer.formatWithCorrelation(
        rawMessage, safeCode, corr.correlationId(), corr.requestId());
    emitStructuredApplicationLog(category, severity, safeCode, actorUserId, safeEntityType, entityId, safeSource, corr);

    IN_EVENT_LOGGING.set(true);
    try {
      SystemEventLogResponse response = requiresNewTxTemplate.execute(status -> {
        RefSystemEventCategory refCategory = categoryRepository.findByCategoryCode(category.name())
            .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "categoryCode=" + category.name()));
        RefSystemEventSeverity refSeverity = severityRepository.findBySeverityCode(severity.name())
            .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "severityCode=" + severity.name()));

        Long verifiedActorId = null;
        if (actorUserId != null && actorUserId > 0 && userRepository.existsById(actorUserId)) {
          verifiedActorId = actorUserId;
        }

        LocalDateTime now = LocalDateTime.now();
        SystemEventLog entry = new SystemEventLog(
            refCategory,
            refSeverity,
            safeCode,
            formattedSummary,
            now);
        entry.setActorUserId(verifiedActorId);
        entry.setEntityType(safeEntityType);
        entry.setEntityId(entityId);
        entry.setSourceComponent(safeSource);
        entry.setCreatedAt(now);

        SystemEventLog saved = systemEventLogRepository.saveAndFlush(entry);
        return mapper.toResponse(saved);
      });
      return Optional.ofNullable(response);
    } catch (Exception ex) {
      // Safe fallback logging only: do not mask or alter business failure semantics, and never recurse
      log.error("Fallback system event persistence failed: category={}, severity={}, eventCode={}, corrId={}, reqId={}, cause={}",
          category.name(), severity.name(), safeCode, corr.correlationId(), corr.requestId(), ex.getClass().getSimpleName());
      return Optional.empty();
    } finally {
      IN_EVENT_LOGGING.remove();
    }
  }

  private void emitStructuredApplicationLog(
      EventCategory category,
      EventSeverity severity,
      String code,
      Long actorUserId,
      String entityType,
      Long entityId,
      String sourceComponent,
      CorrelationPair corr) {
    String template = "event=SYSTEM_EVENT category={} severity={} code={} actorUserId={} entityType={} entityId={} source={} correlationId={} requestId={}";
    Object[] args = new Object[] {
        category.name(), severity.name(), code, actorUserId, entityType, entityId, sourceComponent,
        corr.correlationId(), corr.requestId()
    };
    switch (severity) {
      case CRITICAL, ERROR -> log.error(template, args);
      case WARNING -> log.warn(template, args);
      case INFO -> log.info(template, args);
    }
  }

  private record CorrelationPair(String correlationId, String requestId) {}

  private CorrelationPair resolveCorrelationPair() {
    String corrId = CorrelationContextHolder.getCorrelationId();
    if (!CorrelationIdFilter.isValidCorrelationId(corrId)) {
      String mdcCorr = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
      corrId = CorrelationIdFilter.isValidCorrelationId(mdcCorr) ? mdcCorr : UUID.randomUUID().toString();
    }
    String reqId = CorrelationContextHolder.getRequestId();
    if (!CorrelationIdFilter.isValidCorrelationId(reqId)) {
      String mdcReq = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      reqId = CorrelationIdFilter.isValidCorrelationId(mdcReq) ? mdcReq : corrId;
    }
    return new CorrelationPair(corrId, reqId);
  }

  private boolean isWithinWindow(ConcurrentMap<String, Long> store, String key, int windowSeconds) {
    if (windowSeconds <= 0) {
      return false;
    }
    Long lastRecordedAt = store.get(key);
    if (lastRecordedAt == null) {
      return false;
    }
    long elapsedMs = System.currentTimeMillis() - lastRecordedAt;
    return elapsedMs >= 0 && elapsedMs < (windowSeconds * 1000L);
  }

  private void markWindow(ConcurrentMap<String, Long> store, String key) {
    int maxEntries = properties.getMaxThrottleCacheEntries();
    if (store.size() >= maxEntries) {
      long cutoff = System.currentTimeMillis() - 60_000L;
      store.entrySet().removeIf(e -> e.getValue() < cutoff);
      if (store.size() >= maxEntries) {
        store.clear();
      }
    }
    store.put(key, System.currentTimeMillis());
  }
}
