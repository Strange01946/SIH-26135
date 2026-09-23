package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityCategoryResponse;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import org.springframework.stereotype.Component;

@Component
public class DataQualityCategoryMapper {

  public DataQualityCategoryResponse toResponse(RefDataQualityCategory entity) {
    if (entity == null) {
      return null;
    }
    return new DataQualityCategoryResponse(
        entity.getId(),
        entity.getCategoryCode(),
        entity.getCategoryName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
