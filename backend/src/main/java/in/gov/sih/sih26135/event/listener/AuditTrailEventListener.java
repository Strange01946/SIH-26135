package in.gov.sih.sih26135.event.listener;

import in.gov.sih.sih26135.entity.AuditLog;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.event.AuditTrailEvent;
import in.gov.sih.sih26135.repository.AuditLogRepository;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Asynchronous event listener that persists {@link AuditTrailEvent} instances after
 * primary business transactions commit successfully.
 *
 * <p>Executes in an independent transaction boundary ({@code REQUIRES_NEW}) on a bounded
 * thread pool ({@code auditTaskExecutor}). Any failure during audit persistence is logged safely
 * and isolated so it cannot roll back or corrupt committed primary transactions.
 */
@Component
public class AuditTrailEventListener {

  private static final Logger log = LoggerFactory.getLogger(AuditTrailEventListener.class);

  private final AuditLogRepository auditLogRepository;
  private final RefAuditActionRepository refAuditActionRepository;
  private final UserRepository userRepository;

  @Autowired
  public AuditTrailEventListener(
      AuditLogRepository auditLogRepository,
      RefAuditActionRepository refAuditActionRepository,
      @Autowired(required = false) UserRepository userRepository) {
    this.auditLogRepository = auditLogRepository;
    this.refAuditActionRepository = refAuditActionRepository;
    this.userRepository = userRepository;
  }

  @Async("auditTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleAuditTrailEvent(AuditTrailEvent event) {
    if (event == null) {
      log.warn("Received null AuditTrailEvent, ignoring");
      return;
    }

    try {
      String actionCode = event.actionCode() != null ? event.actionCode() : "STATUS_CHANGE";

      RefAuditAction refAction = refAuditActionRepository.findByActionCode(actionCode)
          .orElseGet(() -> refAuditActionRepository.findByActionCode("STATUS_CHANGE")
              .orElse(null));

      if (refAction == null) {
        log.error("Unable to resolve RefAuditAction for actionCode: {}", actionCode);
        return;
      }

      Long actorUserId = event.actorUserId();
      if (actorUserId != null && userRepository != null) {
        if (!userRepository.existsById(actorUserId)) {
          log.debug("Actor user id {} does not exist in users table; attributing as null to satisfy FK constraint", actorUserId);
          actorUserId = null;
        }
      }

      String sanitizedSummary = sanitizeChangeSummary(event.changeSummary());

      AuditLog auditLog = new AuditLog(refAction, event.entityType(), event.occurredAt());
      auditLog.setActorUserId(actorUserId);
      auditLog.setEntityId(event.entityId());
      auditLog.setCorrelationId(event.correlationId());
      auditLog.setChangeSummary(sanitizedSummary);
      auditLog.setCreatedAt(LocalDateTime.now());

      auditLogRepository.save(auditLog);

      log.debug("Persisted audit log record: id={}, entityType={}, entityId={}, action={}",
          auditLog.getId(), auditLog.getEntityType(), auditLog.getEntityId(), refAction.getActionCode());

    } catch (Exception ex) {
      log.error("Failed to persist audit trail event asynchronously for entityType={} entityId={}: {}",
          event.entityType(), event.entityId(), ex.getMessage());
      // Primary business transaction was already committed; do not rethrow to prevent corrupting state
    }
  }

  /**
   * Redacts sensitive patterns (Aadhaar, PAN, credentials) from change summaries.
   */
  public static String sanitizeChangeSummary(String summary) {
    if (summary == null || summary.isBlank()) {
      return summary;
    }
    // Mask 12-digit Aadhaar numbers: XXXX-XXXX-1234
    String sanitized = summary.replaceAll("\\b([2-9]\\d{3})\\s?(\\d{4})\\s?(\\d{4})\\b", "XXXX-XXXX-$3");
    // Mask 10-character PAN cards: XXXXX1234F
    sanitized = sanitized.replaceAll("\\b([A-Z]{5})(\\d{4})([A-Z])\\b", "XXXXX$2$3");
    // Redact password, token, secret values
    sanitized = sanitized.replaceAll("(?i)(password|token|secret|jwt|bearer)\\s*[:=]\\s*[^\\s,;]+", "$1=***REDACTED***");
    return sanitized;
  }
}
