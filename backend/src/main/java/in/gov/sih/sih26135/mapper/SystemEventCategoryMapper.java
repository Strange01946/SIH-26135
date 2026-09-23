package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SystemEventCategoryResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import org.springframework.stereotype.Component;

@Component
public class SystemEventCategoryMapper {

  public SystemEventCategoryResponse toResponse(RefSystemEventCategory entity) {
    if (entity == null) {
      return null;
    }
    return new SystemEventCategoryResponse(
        entity.getId(),
        entity.getCategoryCode(),
        entity.getCategoryName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
