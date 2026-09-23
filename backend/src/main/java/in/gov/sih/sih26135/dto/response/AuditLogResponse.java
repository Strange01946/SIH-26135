package in.gov.sih.sih26135.dto.response;

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
    LocalDateTime createdAt
) {}
