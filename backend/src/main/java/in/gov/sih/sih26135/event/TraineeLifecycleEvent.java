package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing a lifecycle transition in a trainee's journey
 * (e.g., registration, batch enrollment, attendance milestone, assessment, certification, or dropout).
 *
 * <p>Transports detached scalar identifiers and tenancy scoping to support asynchronous event processing
 * without holding open Hibernate sessions or exposing mutable JPA entities.
 */
public record TraineeLifecycleEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    Long stateId,
    Long districtId,
    Long trainingProviderId,
    Long trainingCenterId,
    Long batchId,
    Long courseId,
    String previousStatus,
    String newStatus,
    String metadataJson
) implements DomainEvent {

  public TraineeLifecycleEvent {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "STATUS_CHANGE";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = CorrelationContextHolder.getCorrelationId();
    }
  }

  /**
   * Convenience factory for creating a basic trainee lifecycle event with automatic UUID and timestamp.
   */
  public static TraineeLifecycleEvent of(
      String eventType,
      Long traineeId,
      Long actorUserId,
      Long stateId,
      Long trainingProviderId,
      Long batchId,
      String previousStatus,
      String newStatus) {
    return new TraineeLifecycleEvent(
        UUID.randomUUID().toString(),
        eventType,
        traineeId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        stateId,
        null,
        trainingProviderId,
        null,
        batchId,
        null,
        previousStatus,
        newStatus,
        null
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
    return "Trainee";
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
