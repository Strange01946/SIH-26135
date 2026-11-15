package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing a Data Quality alert or issue transition
 * (e.g., rule violation detected, issue assigned for remediation, issue resolved, or an anomaly spike flagged).
 */
public record DataQualityAlertEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    String ruleCode,
    String severity,
    String targetEntityType,
    Long targetEntityId,
    Long stateId,
    Long trainingProviderId,
    String issueStatus,
    String summary
) implements DomainEvent {

  public DataQualityAlertEvent {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "ANOMALY_DETECTED";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = CorrelationContextHolder.getCorrelationId();
    }
  }

  /**
   * Convenience factory for creating a data quality anomaly event.
   */
  public static DataQualityAlertEvent of(
      String eventType,
      Long issueId,
      Long actorUserId,
      String ruleCode,
      String severity,
      String targetEntityType,
      Long targetEntityId,
      Long trainingProviderId,
      String issueStatus,
      String summary) {
    return new DataQualityAlertEvent(
        UUID.randomUUID().toString(),
        eventType,
        issueId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        ruleCode,
        severity,
        targetEntityType,
        targetEntityId,
        null,
        trainingProviderId,
        issueStatus,
        summary
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
    return "DataQualityIssue";
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
