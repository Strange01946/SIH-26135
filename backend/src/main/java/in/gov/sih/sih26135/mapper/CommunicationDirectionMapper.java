package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CommunicationDirectionResponse;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import org.springframework.stereotype.Component;

@Component
public class CommunicationDirectionMapper {

  public CommunicationDirectionResponse toResponse(RefCommunicationDirection entity) {
    if (entity == null) {
      return null;
    }

    return new CommunicationDirectionResponse(
        entity.getId(),
        entity.getDirectionCode(),
        entity.getDirectionName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
