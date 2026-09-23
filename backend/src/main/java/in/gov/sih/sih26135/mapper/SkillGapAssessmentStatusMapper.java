package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapAssessmentStatusResponse;
import in.gov.sih.sih26135.entity.RefSkillGapAssessmentStatus;
import org.springframework.stereotype.Component;

@Component
public class SkillGapAssessmentStatusMapper {

  public SkillGapAssessmentStatusResponse toResponse(RefSkillGapAssessmentStatus entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapAssessmentStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsCompletedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
