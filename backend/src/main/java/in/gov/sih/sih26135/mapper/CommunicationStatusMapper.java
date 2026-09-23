package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CommunicationStatusResponse;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import org.springframework.stereotype.Component;

@Component
public class CommunicationStatusMapper {

  public CommunicationStatusResponse toResponse(RefCommunicationStatus entity) {
    if (entity == null) {
      return null;
    }

    return new CommunicationStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsSuccessFlag(),
        entity.getIsFailureFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
