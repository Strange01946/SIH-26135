package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.OfferStatusResponse;
import in.gov.sih.sih26135.entity.RefOfferStatus;
import org.springframework.stereotype.Component;

@Component
public class OfferStatusMapper {

  public OfferStatusResponse toResponse(RefOfferStatus entity) {
    if (entity == null) {
      return null;
    }
    return new OfferStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
