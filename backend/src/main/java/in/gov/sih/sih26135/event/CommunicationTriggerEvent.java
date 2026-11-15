package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing a communication notification trigger
 * (e.g., trainee verification reminder, survey follow-up dispatch, or administrative alert).
 *
 * <p>Transports masked contact references and template identifiers without exposing raw plaintext
 * credentials, Aadhaar tokens, or unmasked personal identifiers.
 */
public record CommunicationTriggerEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    String channel,
    String recipientMasked,
    String templateCode,
    Long stateId,
    Long trainingProviderId,
    String priority,
    String payloadSummary
) implements DomainEvent {

  public CommunicationTriggerEvent {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "COMMUNICATION_DISPATCH";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = CorrelationContextHolder.getCorrelationId();
    }
    if (priority == null || priority.isBlank()) {
      priority = "NORMAL";
    }
  }

  /**
   * Convenience factory for creating a communication trigger event.
   */
  public static CommunicationTriggerEvent of(
      String eventType,
      Long recipientEntityId,
      Long actorUserId,
      String channel,
      String recipientMasked,
      String templateCode,
      Long trainingProviderId,
      String priority,
      String payloadSummary) {
    return new CommunicationTriggerEvent(
        UUID.randomUUID().toString(),
        eventType,
        recipientEntityId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        channel,
        recipientMasked,
        templateCode,
        null,
        trainingProviderId,
        priority,
        payloadSummary
    );
  }

  @Override
  public String getEventId() {
    return eventId();
  }

  @Override
  public String getEventType() {
    return eventType();
  }

  @Override
  public String getEntityType() {
    return "CommunicationTrigger";
  }

  @Override
  public Long getEntityId() {
    return entityId();
  }

  @Override
  public Long getActorUserId() {
    return actorUserId();
  }

  @Override
  public String getCorrelationId() {
    return correlationId();
  }

  @Override
  public LocalDateTime getOccurredAt() {
    return occurredAt();
  }
}
