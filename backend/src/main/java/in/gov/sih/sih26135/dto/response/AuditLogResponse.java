package in.gov.sih.sih26135.dto.response;

import in.gov.sih.sih26135.service.audit.AuditSnapshot;
import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    Long auditActionId,
    String auditActionCode,
    String auditActionName,
    String entityType,
    Long entityId,
    LocalDateTime occurredAt,
    String correlationId,
    String changeSummary,
    LocalDateTime createdAt,
    AuditStateChange stateChange
) {
  public AuditLogResponse(Long id, Long actorUserId, Long auditActionId, String auditActionCode,
      String auditActionName, String entityType, Long entityId, LocalDateTime occurredAt,
      String correlationId, String storedSummary, LocalDateTime createdAt) {
    this(id, actorUserId, auditActionId, auditActionCode, auditActionName, entityType, entityId,
        occurredAt, correlationId, AuditSnapshot.decode(storedSummary), createdAt);
  }

  private AuditLogResponse(Long id, Long actorUserId, Long auditActionId, String auditActionCode,
      String auditActionName, String entityType, Long entityId, LocalDateTime occurredAt,
      String correlationId, AuditSnapshot.StoredSummary summary, LocalDateTime createdAt) {
    this(id, actorUserId, auditActionId, auditActionCode, auditActionName, entityType, entityId,
        occurredAt, correlationId, summary.text(), createdAt, summary.change());
  }
}
