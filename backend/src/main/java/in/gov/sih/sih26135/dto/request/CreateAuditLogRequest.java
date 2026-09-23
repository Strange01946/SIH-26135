package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateAuditLogRequest {

  private Long actorUserId;
  private Long auditActionId;
  private String entityType;
  private Long entityId;
  private LocalDateTime occurredAt;
  private String correlationId;
  private String changeSummary;

  public CreateAuditLogRequest() {}

  public CreateAuditLogRequest(Long auditActionId, String entityType, LocalDateTime occurredAt) {
    this.auditActionId = auditActionId;
    this.entityType = entityType;
    this.occurredAt = occurredAt;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }

  public Long getAuditActionId() {
    return auditActionId;
  }

  public void setAuditActionId(Long auditActionId) {
    this.auditActionId = auditActionId;
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

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getChangeSummary() {
    return changeSummary;
  }

  public void setChangeSummary(String changeSummary) {
    this.changeSummary = changeSummary;
  }
}
