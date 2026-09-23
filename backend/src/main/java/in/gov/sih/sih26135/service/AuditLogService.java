package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

  AuditLogResponse recordAuditLog(CreateAuditLogRequest request);

  AuditLogResponse getAuditLogById(Long id);

  List<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId);

  List<AuditLogResponse> getAuditLogsByActionId(Long auditActionId);

  List<AuditLogResponse> getAuditLogsByEntityType(String entityType);

  List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId);

  List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId);

  List<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
}
