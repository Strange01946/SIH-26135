package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.PlacementStatusResponse;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import org.springframework.stereotype.Component;

@Component
public class PlacementStatusMapper {

  public PlacementStatusResponse toResponse(RefPlacementStatus entity) {
    if (entity == null) {
      return null;
    }
    return new PlacementStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsOfferFlag(),
        entity.getIsJoinedFlag(),
        entity.getIsUnsuccessfulFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
