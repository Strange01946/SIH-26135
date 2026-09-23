package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRejectionReasonResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRejectionReason;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationRejectionReasonMapper {

  public EmploymentVerificationRejectionReasonResponse toResponse(RefEmploymentVerificationRejectionReason entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationRejectionReasonResponse(
        entity.getId(),
        entity.getReasonCode(),
        entity.getReasonName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
