package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AssessmentTypeResponse;
import in.gov.sih.sih26135.entity.RefAssessmentType;
import org.springframework.stereotype.Component;

@Component
public class AssessmentTypeMapper {

  public AssessmentTypeResponse toResponse(RefAssessmentType entity) {
    if (entity == null) {
      return null;
    }

    return new AssessmentTypeResponse(
        entity.getId(),
        entity.getTypeCode(),
        entity.getTypeName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
