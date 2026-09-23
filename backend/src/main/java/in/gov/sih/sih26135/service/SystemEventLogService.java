package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface SystemEventLogService {

  SystemEventLogResponse recordSystemEvent(CreateSystemEventLogRequest request);

  SystemEventLogResponse getEventById(Long id);

  List<SystemEventLogResponse> getEventsByCategoryId(Long categoryId);

  List<SystemEventLogResponse> getEventsBySeverityId(Long severityId);

  List<SystemEventLogResponse> getEventsByEventCode(String eventCode);

  List<SystemEventLogResponse> getEventsByActorUserId(Long actorUserId);

  List<SystemEventLogResponse> getEventsByEntityType(String entityType);

  List<SystemEventLogResponse> getEventsByEntity(String entityType, Long entityId);

  List<SystemEventLogResponse> getEventsBySourceComponent(String sourceComponent);

  List<SystemEventLogResponse> getEventsByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
}
