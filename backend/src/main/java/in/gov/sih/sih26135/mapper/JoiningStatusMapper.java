package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.JoiningStatusResponse;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import org.springframework.stereotype.Component;

@Component
public class JoiningStatusMapper {

  public JoiningStatusResponse toResponse(RefJoiningStatus entity) {
    if (entity == null) {
      return null;
    }
    return new JoiningStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsJoinedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
