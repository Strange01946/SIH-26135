package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AccreditationStatusResponse;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import org.springframework.stereotype.Component;

@Component
public class AccreditationStatusMapper {

  public AccreditationStatusResponse toResponse(RefAccreditationStatus entity) {
    if (entity == null) {
      return null;
    }

    return new AccreditationStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
