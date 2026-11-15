package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing a placement transition
 * (e.g., placement offer extended, accepted, rejected, joined, no-show, or transitioned to employment).
 */
public record PlacementLifecycleEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long traineeId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    Long stateId,
    Long trainingProviderId,
    Long employerId,
    Long jobPostingId,
    String placementStatusCode,
    String joiningStatusCode,
    BigDecimal offeredSalary,
    String metadataJson
) implements DomainEvent {

  public PlacementLifecycleEvent {
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
   * Convenience factory for creating a placement transition event.
   */
  public static PlacementLifecycleEvent of(
      String eventType,
      Long placementRecordId,
      Long traineeId,
      Long actorUserId,
      Long trainingProviderId,
      Long employerId,
      String placementStatusCode,
      String joiningStatusCode,
      BigDecimal offeredSalary) {
    return new PlacementLifecycleEvent(
        UUID.randomUUID().toString(),
        eventType,
        placementRecordId,
        traineeId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        null,
        trainingProviderId,
        employerId,
        null,
        placementStatusCode,
        joiningStatusCode,
        offeredSalary,
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
    return "Placement";
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
