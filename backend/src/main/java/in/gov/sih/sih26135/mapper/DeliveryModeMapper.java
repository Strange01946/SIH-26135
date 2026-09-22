package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DeliveryModeResponse;
import in.gov.sih.sih26135.entity.RefDeliveryMode;
import org.springframework.stereotype.Component;

@Component
public class DeliveryModeMapper {

  public DeliveryModeResponse toResponse(RefDeliveryMode entity) {
    if (entity == null) {
      return null;
    }

    return new DeliveryModeResponse(
        entity.getId(),
        entity.getModeCode(),
        entity.getModeName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
