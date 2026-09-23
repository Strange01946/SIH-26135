package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.SystemEventLog;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SystemEventLogMapper;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.SystemEventLogRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SystemEventLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemEventLogServiceImpl implements SystemEventLogService {

  private static final Logger log = LoggerFactory.getLogger(SystemEventLogServiceImpl.class);

  private final SystemEventLogRepository systemEventLogRepository;
  private final RefSystemEventCategoryRepository refSystemEventCategoryRepository;
  private final RefSystemEventSeverityRepository refSystemEventSeverityRepository;
  private final UserRepository userRepository;
  private final SystemEventLogMapper mapper;

  public SystemEventLogServiceImpl(
      SystemEventLogRepository systemEventLogRepository,
      RefSystemEventCategoryRepository refSystemEventCategoryRepository,
      RefSystemEventSeverityRepository refSystemEventSeverityRepository,
      UserRepository userRepository,
      SystemEventLogMapper mapper) {
    this.systemEventLogRepository = systemEventLogRepository;
    this.refSystemEventCategoryRepository = refSystemEventCategoryRepository;
    this.refSystemEventSeverityRepository = refSystemEventSeverityRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public SystemEventLogResponse recordSystemEvent(CreateSystemEventLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getSystemEventCategoryId() == null) {
      throw new BadRequestException("System event category ID is required", "CATEGORY_ID_REQUIRED");
    }
    if (request.getSystemEventSeverityId() == null) {
      throw new BadRequestException("System event severity ID is required", "SEVERITY_ID_REQUIRED");
    }
    if (request.getEventCode() == null || request.getEventCode().isBlank()) {
      throw new BadRequestException("Event code is required", "EVENT_CODE_REQUIRED");
    }
    if (request.getEventCode().trim().length() > 64) {
      throw new BadRequestException("Event code must not exceed 64 characters", "EVENT_CODE_TOO_LONG");
    }
    if (request.getEventSummary() == null || request.getEventSummary().isBlank()) {
      throw new BadRequestException("Event summary is required", "EVENT_SUMMARY_REQUIRED");
    }
    if (request.getEventSummary().trim().length() > 500) {
      throw new BadRequestException("Event summary must not exceed 500 characters", "EVENT_SUMMARY_TOO_LONG");
    }
    if (request.getEntityType() != null && request.getEntityType().trim().length() > 64) {
      throw new BadRequestException("Entity type must not exceed 64 characters", "ENTITY_TYPE_TOO_LONG");
    }
    if (request.getSourceComponent() != null && request.getSourceComponent().trim().length() > 64) {
      throw new BadRequestException("Source component must not exceed 64 characters", "SOURCE_COMPONENT_TOO_LONG");
    }

    RefSystemEventCategory category = refSystemEventCategoryRepository.findById(request.getSystemEventCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "id"));

    RefSystemEventSeverity severity = refSystemEventSeverityRepository.findById(request.getSystemEventSeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "id"));

    if (request.getActorUserId() != null && !userRepository.existsById(request.getActorUserId())) {
      throw new ResourceNotFoundException("User", "id");
    }

    LocalDateTime occurredAt = request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now();

    SystemEventLog logEntry = new SystemEventLog(
        category,
        severity,
        request.getEventCode().trim(),
        request.getEventSummary().trim(),
        occurredAt
    );
    logEntry.setActorUserId(request.getActorUserId());
    logEntry.setEntityType(request.getEntityType() != null ? request.getEntityType().trim() : null);
    logEntry.setEntityId(request.getEntityId());
    logEntry.setSourceComponent(request.getSourceComponent() != null ? request.getSourceComponent().trim() : null);

    SystemEventLog saved = systemEventLogRepository.save(logEntry);
    log.info("Recorded system event log id={}, eventCode={}", saved.getId(), saved.getEventCode());
    return mapper.toResponse(saved);
  }

  @Override
  public SystemEventLogResponse getEventById(Long id) {
    if (id == null) {
      throw new BadRequestException("Event ID is required");
    }
    SystemEventLog event = systemEventLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SystemEventLog", "id"));
    return mapper.toResponse(event);
  }

  @Override
  public List<SystemEventLogResponse> getEventsByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    return systemEventLogRepository.findBySystemEventCategoryId(categoryId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsBySeverityId(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return systemEventLogRepository.findBySystemEventSeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEventCode(String eventCode) {
    if (eventCode == null || eventCode.isBlank()) {
      throw new BadRequestException("Event code is required");
    }
    return systemEventLogRepository.findByEventCode(eventCode.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId) {
    if (actorUserId == null) {
      throw new BadRequestException("Actor user ID is required");
    }
    return systemEventLogRepository.findByActorUserId(actorUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return systemEventLogRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return systemEventLogRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent) {
    if (sourceComponent == null || sourceComponent.isBlank()) {
      throw new BadRequestException("Source component is required");
    }
    return systemEventLogRepository.findBySourceComponent(sourceComponent.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Start and end dates are required");
    }
    if (start.isAfter(end)) {
      throw new BadRequestException("Start date cannot be after end date");
    }
    return systemEventLogRepository.findByOccurredAtBetween(start, end).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
