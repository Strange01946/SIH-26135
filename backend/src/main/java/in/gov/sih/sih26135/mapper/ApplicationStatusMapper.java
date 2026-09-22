package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ApplicationStatusResponse;
import in.gov.sih.sih26135.entity.RefApplicationStatus;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStatusMapper {

  public ApplicationStatusResponse toResponse(RefApplicationStatus entity) {
    if (entity == null) {
      return null;
    }
    return new ApplicationStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
