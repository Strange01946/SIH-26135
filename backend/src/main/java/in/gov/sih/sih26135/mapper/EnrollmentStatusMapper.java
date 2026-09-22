package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EnrollmentStatusResponse;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentStatusMapper {

  public EnrollmentStatusResponse toResponse(RefEnrollmentStatus entity) {
    if (entity == null) {
      return null;
    }

    return new EnrollmentStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsTerminal(),
        entity.getIsCompletedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
