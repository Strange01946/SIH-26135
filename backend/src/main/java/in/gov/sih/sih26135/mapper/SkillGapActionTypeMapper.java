package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapActionTypeResponse;
import in.gov.sih.sih26135.entity.RefSkillGapActionType;
import org.springframework.stereotype.Component;

@Component
public class SkillGapActionTypeMapper {

  public SkillGapActionTypeResponse toResponse(RefSkillGapActionType entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapActionTypeResponse(
        entity.getId(),
        entity.getActionCode(),
        entity.getActionName(),
        entity.getRequiresCourseFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
