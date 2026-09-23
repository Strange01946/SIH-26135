package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualitySeverityResponse;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import org.springframework.stereotype.Component;

@Component
public class DataQualitySeverityMapper {

  public DataQualitySeverityResponse toResponse(RefDataQualitySeverity entity) {
    if (entity == null) {
      return null;
    }
    return new DataQualitySeverityResponse(
        entity.getId(),
        entity.getSeverityCode(),
        entity.getSeverityName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
