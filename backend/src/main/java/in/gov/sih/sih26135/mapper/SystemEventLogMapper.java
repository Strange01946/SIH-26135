package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.SystemEventLog;
import org.springframework.stereotype.Component;

@Component
public class SystemEventLogMapper {

  public SystemEventLogResponse toResponse(SystemEventLog entity) {
    if (entity == null) {
      return null;
    }

    Long categoryId = null;
    String categoryCode = null;
    String categoryName = null;
    if (entity.getSystemEventCategory() != null) {
      categoryId = entity.getSystemEventCategory().getId();
      categoryCode = entity.getSystemEventCategory().getCategoryCode();
      categoryName = entity.getSystemEventCategory().getCategoryName();
    }

    Long severityId = null;
    String severityCode = null;
    String severityName = null;
    if (entity.getSystemEventSeverity() != null) {
      severityId = entity.getSystemEventSeverity().getId();
      severityCode = entity.getSystemEventSeverity().getSeverityCode();
      severityName = entity.getSystemEventSeverity().getSeverityName();
    }

    return new SystemEventLogResponse(
        entity.getId(),
        categoryId,
        categoryCode,
        categoryName,
        severityId,
        severityCode,
        severityName,
        entity.getEventCode(),
        entity.getActorUserId(),
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getSourceComponent(),
        entity.getEventSummary(),
        entity.getOccurredAt(),
        entity.getCreatedAt()
    );
  }
}
