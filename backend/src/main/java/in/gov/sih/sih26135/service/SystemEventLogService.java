package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemEventLogService {

  SystemEventLogResponse recordSystemEvent(CreateSystemEventLogRequest request);

  SystemEventLogResponse getEventById(Long id);

  Page<SystemEventLogResponse> getAllEvents(Pageable pageable);

  List<SystemEventLogResponse> getEventsByCategoryId(Long categoryId);

  Page<SystemEventLogResponse> getEventsByCategoryId(Long categoryId, Pageable pageable);

  List<SystemEventLogResponse> getEventsBySeverityId(Long severityId);

  Page<SystemEventLogResponse> getEventsBySeverityId(Long severityId, Pageable pageable);

  List<SystemEventLogResponse> getEventsByEventCode(String eventCode);

  Page<SystemEventLogResponse> getEventsByEventCode(String eventCode, Pageable pageable);

  List<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId);

  Page<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId, Pageable pageable);

  List<SystemEventLogResponse> getEventsByEntityType(String entityType);

  Page<SystemEventLogResponse> getEventsByEntityType(String entityType, Pageable pageable);

  List<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId);

  Page<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId, Pageable pageable);

  List<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent);

  Page<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent, Pageable pageable);

  List<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

  Page<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
