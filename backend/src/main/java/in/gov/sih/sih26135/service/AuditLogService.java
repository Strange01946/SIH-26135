package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

  AuditLogResponse recordAuditLog(CreateAuditLogRequest request);

  AuditLogResponse getAuditLogById(Long id);

  Page<AuditLogResponse> getAllAuditLogs(Pageable pageable);

  List<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId);

  Page<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId, Pageable pageable);

  List<AuditLogResponse> getAuditLogsByActionId(Long auditActionId);

  Page<AuditLogResponse> getAuditLogsByActionId(Long auditActionId, Pageable pageable);

  List<AuditLogResponse> getAuditLogsByEntityType(String entityType);

  Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable);

  List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId);

  Page<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable);

  List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId);

  Page<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId, Pageable pageable);

  List<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

  Page<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
