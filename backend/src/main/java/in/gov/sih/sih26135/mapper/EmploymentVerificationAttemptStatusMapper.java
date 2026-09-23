package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationAttemptStatus;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationAttemptStatusMapper {

  public EmploymentVerificationAttemptStatusResponse toResponse(RefEmploymentVerificationAttemptStatus entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationAttemptStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsTerminalFlag(),
        entity.getIsSuccessFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
