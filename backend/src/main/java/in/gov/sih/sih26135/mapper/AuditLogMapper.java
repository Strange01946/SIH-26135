package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

  public AuditLogResponse toResponse(AuditLog entity) {
    if (entity == null) {
      return null;
    }

    Long actionId = null;
    String actionCode = null;
    String actionName = null;
    if (entity.getAuditAction() != null) {
      actionId = entity.getAuditAction().getId();
      actionCode = entity.getAuditAction().getActionCode();
      actionName = entity.getAuditAction().getActionName();
    }

    return new AuditLogResponse(
        entity.getId(),
        entity.getActorUserId(),
        actionId,
        actionCode,
        actionName,
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getOccurredAt(),
        entity.getCorrelationId(),
        entity.getChangeSummary(),
        entity.getCreatedAt()
    );
  }
}
