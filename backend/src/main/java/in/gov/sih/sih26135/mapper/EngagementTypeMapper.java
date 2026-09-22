package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EngagementTypeResponse;
import in.gov.sih.sih26135.entity.RefEngagementType;
import org.springframework.stereotype.Component;

@Component
public class EngagementTypeMapper {

  public EngagementTypeResponse toResponse(RefEngagementType entity) {
    if (entity == null) {
      return null;
    }
    return new EngagementTypeResponse(
        entity.getId(),
        entity.getTypeCode(),
        entity.getTypeName(),
        entity.getIsWageEmployment(),
        entity.getIsSelfEmployment(),
        entity.getIsApprenticeship(),
        entity.getIsInternship(),
        entity.getRequiresEmployer(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
