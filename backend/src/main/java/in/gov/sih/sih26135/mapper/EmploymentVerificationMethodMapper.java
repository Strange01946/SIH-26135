package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationMethodResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationMethodMapper {

  public EmploymentVerificationMethodResponse toResponse(RefEmploymentVerificationMethod entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationMethodResponse(
        entity.getId(),
        entity.getMethodCode(),
        entity.getMethodName(),
        entity.getIsEmployerSide(),
        entity.getIsTraineeSelfReported(),
        entity.getIsOfficialFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
