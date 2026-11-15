package in.gov.sih.sih26135.event.listener;

import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.event.DataQualityAlertEvent;
import in.gov.sih.sih26135.event.EmploymentVerificationEvent;
import in.gov.sih.sih26135.event.PlacementLifecycleEvent;
import in.gov.sih.sih26135.event.TraineeLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transaction-aware domain event listener that processes committed candidate lifecycle transitions,
 * verification events, data quality alerts, and communication notification triggers.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Guaranteed {@code AFTER_COMMIT} execution: side effects never observe rolled-back transactions.</li>
 *   <li>Detached immutable event consumption without mutating JPA entity state or reopening transactions.</li>
 *   <li>Structured logging with correlation continuity and sanitized payload representations.</li>
 *   <li>Safe fallback execution in non-transactional test environments.</li>
 * </ul>
 */
@Component
public class CandidateLifecycleEventListener {

  private static final Logger log = LoggerFactory.getLogger(CandidateLifecycleEventListener.class);

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onTraineeLifecycleEvent(TraineeLifecycleEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed TraineeLifecycleEvent: eventId={}, eventType={}, entityId={}, newStatus={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.getEntityId(), event.newStatus(), event.getCorrelationId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onPlacementLifecycleEvent(PlacementLifecycleEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed PlacementLifecycleEvent: eventId={}, eventType={}, entityId={}, placementStatus={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.getEntityId(), event.placementStatusCode(), event.getCorrelationId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onEmploymentVerificationEvent(EmploymentVerificationEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed EmploymentVerificationEvent: eventId={}, eventType={}, entityId={}, verificationStatus={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.getEntityId(), event.verificationStatus(), event.getCorrelationId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onDataQualityAlertEvent(DataQualityAlertEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed DataQualityAlertEvent: eventId={}, eventType={}, ruleCode={}, severity={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.ruleCode(), event.severity(), event.getCorrelationId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onCommunicationTriggerEvent(CommunicationTriggerEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed CommunicationTriggerEvent: eventId={}, eventType={}, templateCode={}, recipientMasked={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.templateCode(), event.recipientMasked(), event.getCorrelationId());
  }
}
