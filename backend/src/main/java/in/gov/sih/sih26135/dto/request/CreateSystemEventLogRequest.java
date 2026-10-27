package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CreateSystemEventLogRequest {

  private Long systemEventCategoryId;
  private Long systemEventSeverityId;
  @NotBlank(message = "eventCode is required")
  @Size(max = 64, message = "eventCode cannot exceed 64 characters")
  private String eventCode;
  private Long actorUserId;
  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;
  private Long entityId;
  @Size(max = 64, message = "sourceComponent cannot exceed 64 characters")
  private String sourceComponent;
  @Size(max = 500, message = "eventSummary cannot exceed 500 characters")
  private String eventSummary;
  private LocalDateTime occurredAt;

  public CreateSystemEventLogRequest() {}

  public CreateSystemEventLogRequest(
      Long systemEventCategoryId,
      Long systemEventSeverityId,
      String eventCode,
      String eventSummary,
      LocalDateTime occurredAt) {
    this.systemEventCategoryId = systemEventCategoryId;
    this.systemEventSeverityId = systemEventSeverityId;
    this.eventCode = eventCode;
    this.eventSummary = eventSummary;
    this.occurredAt = occurredAt;
  }

  public Long getSystemEventCategoryId() {
    return systemEventCategoryId;
  }

  public void setSystemEventCategoryId(Long systemEventCategoryId) {
    this.systemEventCategoryId = systemEventCategoryId;
  }

  public Long getSystemEventSeverityId() {
    return systemEventSeverityId;
  }

  public void setSystemEventSeverityId(Long systemEventSeverityId) {
    this.systemEventSeverityId = systemEventSeverityId;
  }

  public String getEventCode() {
    return eventCode;
  }

  public void setEventCode(String eventCode) {
    this.eventCode = eventCode;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getSourceComponent() {
    return sourceComponent;
  }

  public void setSourceComponent(String sourceComponent) {
    this.sourceComponent = sourceComponent;
  }

  public String getEventSummary() {
    return eventSummary;
  }

  public void setEventSummary(String eventSummary) {
    this.eventSummary = eventSummary;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }
}
