package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentExitReasonResponse;
import in.gov.sih.sih26135.entity.RefEmploymentExitReason;
import org.springframework.stereotype.Component;

@Component
public class EmploymentExitReasonMapper {

  public EmploymentExitReasonResponse toResponse(RefEmploymentExitReason entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentExitReasonResponse(
        entity.getId(),
        entity.getReasonCode(),
        entity.getReasonName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
