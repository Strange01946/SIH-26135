package in.gov.sih.sih26135.event.listener;

import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.dto.request.RemediationTriggerRequest;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.event.DataQualityAlertEvent;
import in.gov.sih.sih26135.event.EmploymentVerificationEvent;
import in.gov.sih.sih26135.event.PlacementLifecycleEvent;
import in.gov.sih.sih26135.event.TraineeLifecycleEvent;
import in.gov.sih.sih26135.service.CandidateLifecycleOrchestratorService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
 *   <li>Safe automated remediation connection on committed employment exit or verification failure.</li>
 *   <li>Safe fallback execution in non-transactional test environments.</li>
 * </ul>
 */
@Component
public class CandidateLifecycleEventListener {

  private static final Logger log = LoggerFactory.getLogger(CandidateLifecycleEventListener.class);

  private final CandidateLifecycleOrchestratorService orchestratorService;
  private final NotificationDispatcherService notificationDispatcherService;
  private final Set<String> processedRemediationEvents = ConcurrentHashMap.newKeySet();

  public CandidateLifecycleEventListener() {
    this(null, null);
  }

  public CandidateLifecycleEventListener(CandidateLifecycleOrchestratorService orchestratorService) {
    this(orchestratorService, null);
  }

  @Autowired
  public CandidateLifecycleEventListener(
      @Autowired(required = false) @Lazy CandidateLifecycleOrchestratorService orchestratorService,
      @Autowired(required = false) @Lazy NotificationDispatcherService notificationDispatcherService) {
    this.orchestratorService = orchestratorService;
    this.notificationDispatcherService = notificationDispatcherService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onTraineeLifecycleEvent(TraineeLifecycleEvent event) {
    if (event == null) {
      return;
    }
    log.info("Committed TraineeLifecycleEvent: eventId={}, eventType={}, entityId={}, newStatus={}, correlationId={}",
        event.getEventId(), event.getEventType(), event.getEntityId(), event.newStatus(), event.getCorrelationId());

    // Connect committed employment exits to automated remediation evaluation advisory
    if (orchestratorService != null && isEmploymentExit(event)) {
      if (event.getEventId() == null || processedRemediationEvents.add(event.getEventId())) {
        Long traineeId = event.getEntityId();
        try {
          if (traineeId != null) {
            Long exitEventId = extractIdFromMetadata(event.metadataJson(), "employmentExitEventId");
            orchestratorService.triggerSkillGapRemediation(new RemediationTriggerRequest(
                traineeId,
                "EMPLOYMENT_EXIT",
                exitEventId,
                null,
                "Automated remediation evaluation advisory triggered following committed employment exit"
            ));
          }
        } catch (Exception ex) {
          if (event.getEventId() != null) {
            processedRemediationEvents.remove(event.getEventId());
          }
          log.error("Post-commit automated remediation trigger failed for exit traineeId={}, eventId={}: {}",
              traineeId, event.getEventId(), ex.getMessage(), ex);
        }
      }
    }
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

    // Connect committed verification rejection/failure to automated remediation evaluation advisory
    if (orchestratorService != null && isVerificationFailure(event)) {
      if (event.getEventId() == null || processedRemediationEvents.add(event.getEventId())) {
        Long traineeId = event.traineeId();
        try {
          if (traineeId != null) {
            orchestratorService.triggerSkillGapRemediation(new RemediationTriggerRequest(
                traineeId,
                "VERIFICATION_FAILURE",
                event.getEntityId(),
                null,
                "Automated remediation evaluation advisory triggered following committed verification rejection"
            ));
          }
        } catch (Exception ex) {
          if (event.getEventId() != null) {
            processedRemediationEvents.remove(event.getEventId());
          }
          log.error("Post-commit automated remediation trigger failed for verification failure traineeId={}, eventId={}: {}",
              traineeId, event.getEventId(), ex.getMessage(), ex);
        }
      }
    }
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

    if (notificationDispatcherService != null && event.entityId() != null) {
      try {
        notificationDispatcherService.dispatchFromTriggerEvent(event);
      } catch (Exception ex) {
        log.error("Post-commit notification dispatch failed for eventId={}: {}", event.getEventId(), ex.getMessage(), ex);
      }
    }
  }

  private boolean isEmploymentExit(TraineeLifecycleEvent event) {
    return "EMPLOYMENT_EXIT".equalsIgnoreCase(event.getEventType())
        || "EMPLOYMENT_EXIT".equalsIgnoreCase(event.eventType());
  }

  private boolean isVerificationFailure(EmploymentVerificationEvent event) {
    String status = event.verificationStatus();
    return "REJECTED".equalsIgnoreCase(status)
        || "FAILED".equalsIgnoreCase(status)
        || "UNVERIFIED".equalsIgnoreCase(status);
  }

  private Long extractIdFromMetadata(String metadataJson, String key) {
    if (metadataJson == null || !metadataJson.contains(key)) {
      return null;
    }
    try {
      Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
      Matcher matcher = pattern.matcher(metadataJson);
      if (matcher.find()) {
        return Long.parseLong(matcher.group(1));
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  public boolean isRemediationEventProcessed(String eventId) {
    return eventId != null && processedRemediationEvents.contains(eventId);
  }

  public void clearProcessedRemediationEvents() {
    processedRemediationEvents.clear();
  }
}
