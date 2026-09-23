package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityDetectionSourceResponse;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import org.springframework.stereotype.Component;

@Component
public class DataQualityDetectionSourceMapper {

  public DataQualityDetectionSourceResponse toResponse(RefDataQualityDetectionSource entity) {
    if (entity == null) {
      return null;
    }
    return new DataQualityDetectionSourceResponse(
        entity.getId(),
        entity.getSourceCode(),
        entity.getSourceName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
