package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.entity.AuditLog;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AuditLogMapper;
import in.gov.sih.sih26135.repository.AuditLogRepository;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final RefAuditActionRepository refAuditActionRepository;
  private final UserRepository userRepository;
  private final AuditLogMapper mapper;

  public AuditLogServiceImpl(
      AuditLogRepository auditLogRepository,
      RefAuditActionRepository refAuditActionRepository,
      UserRepository userRepository,
      AuditLogMapper mapper) {
    this.auditLogRepository = auditLogRepository;
    this.refAuditActionRepository = refAuditActionRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public AuditLogResponse recordAuditLog(CreateAuditLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getAuditActionId() == null) {
      throw new BadRequestException("Audit action ID is required", "AUDIT_ACTION_REQUIRED");
    }
    RefAuditAction auditAction = refAuditActionRepository.findById(request.getAuditActionId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "auditActionId"));

    if (request.getEntityType() == null || request.getEntityType().isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }

    if (request.getActorUserId() != null) {
      if (!userRepository.existsById(request.getActorUserId())) {
        throw new ResourceNotFoundException("User", "actorUserId");
      }
    }

    LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();

    AuditLog auditLog = new AuditLog(auditAction, request.getEntityType().trim(), occurredAt);
    auditLog.setActorUserId(request.getActorUserId());
    auditLog.setEntityId(request.getEntityId());
    auditLog.setCorrelationId(request.getCorrelationId());
    auditLog.setChangeSummary(request.getChangeSummary());

    AuditLog saved = auditLogRepository.save(auditLog);
    return mapper.toResponse(saved);
  }

  @Override
  public AuditLogResponse getAuditLogById(Long id) {
    if (id == null) {
      throw new BadRequestException("Audit log ID is required");
    }
    AuditLog entity = auditLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActorUserId(Long actorUserId) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    return auditLogRepository.findByActorUserId(actorUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByActionId(Long auditActionId) {
    if (auditActionId == null) {
      throw new BadRequestException("Audit action ID is required");
    }
    return auditLogRepository.findByAuditActionId(auditActionId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return auditLogRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return auditLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
    if (correlationId == null || correlationId.isBlank()) {
      throw new BadRequestException("Correlation ID is required");
    }
    return auditLogRepository.findByCorrelationId(correlationId.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AuditLogResponse> getAuditLogsByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end timestamps are required");
    }
    return auditLogRepository.findByOccurredAtBetween(start, end).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
