package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceTypeResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationEvidenceType;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationEvidenceTypeMapper {

  public EmploymentVerificationEvidenceTypeResponse toResponse(RefEmploymentVerificationEvidenceType entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationEvidenceTypeResponse(
        entity.getId(),
        entity.getTypeCode(),
        entity.getTypeName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
