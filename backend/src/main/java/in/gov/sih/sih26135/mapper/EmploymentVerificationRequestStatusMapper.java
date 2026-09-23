package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationRequestStatusMapper {

  public EmploymentVerificationRequestStatusResponse toResponse(RefEmploymentVerificationRequestStatus entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationRequestStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsOpenFlag(),
        entity.getIsCompletedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
