package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SystemEventSeverityResponse;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import org.springframework.stereotype.Component;

@Component
public class SystemEventSeverityMapper {

  public SystemEventSeverityResponse toResponse(RefSystemEventSeverity entity) {
    if (entity == null) {
      return null;
    }
    return new SystemEventSeverityResponse(
        entity.getId(),
        entity.getSeverityCode(),
        entity.getSeverityName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
