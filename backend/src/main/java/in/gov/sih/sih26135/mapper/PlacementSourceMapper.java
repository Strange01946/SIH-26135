package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.PlacementSourceResponse;
import in.gov.sih.sih26135.entity.RefPlacementSource;
import org.springframework.stereotype.Component;

@Component
public class PlacementSourceMapper {

  public PlacementSourceResponse toResponse(RefPlacementSource entity) {
    if (entity == null) {
      return null;
    }
    return new PlacementSourceResponse(
        entity.getId(),
        entity.getSourceCode(),
        entity.getSourceName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
