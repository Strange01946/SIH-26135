package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import org.springframework.stereotype.Component;

@Component
public class SkillGapRecommendationMapper {

  public SkillGapRecommendationResponse toResponse(SkillGapRecommendation entity) {
    if (entity == null) {
      return null;
    }

    Long skillGapId = entity.getSkillGap() != null ? entity.getSkillGap().getId() : null;
    String skillGapNumber = entity.getSkillGap() != null ? entity.getSkillGap().getSkillGapNumber() : null;

    Long actionTypeId = entity.getSkillGapActionType() != null ? entity.getSkillGapActionType().getId() : null;
    String actionTypeCode = entity.getSkillGapActionType() != null ? entity.getSkillGapActionType().getActionCode() : null;
    String actionTypeName = entity.getSkillGapActionType() != null ? entity.getSkillGapActionType().getActionName() : null;

    Long recommendedCourseId = entity.getRecommendedCourse() != null ? entity.getRecommendedCourse().getId() : null;
    String recommendedCourseCode = entity.getRecommendedCourse() != null ? entity.getRecommendedCourse().getCourseCode() : null;
    String recommendedCourseName = entity.getRecommendedCourse() != null ? entity.getRecommendedCourse().getCourseName() : null;

    Long recommendedSkillId = entity.getRecommendedSkill() != null ? entity.getRecommendedSkill().getId() : null;
    String recommendedSkillCode = entity.getRecommendedSkill() != null ? entity.getRecommendedSkill().getSkillCode() : null;
    String recommendedSkillName = entity.getRecommendedSkill() != null ? entity.getRecommendedSkill().getSkillName() : null;

    return new SkillGapRecommendationResponse(
        entity.getId(),
        skillGapId,
        skillGapNumber,
        entity.getRecommendationNumber(),
        actionTypeId,
        actionTypeCode,
        actionTypeName,
        recommendedCourseId,
        recommendedCourseCode,
        recommendedCourseName,
        recommendedSkillId,
        recommendedSkillCode,
        recommendedSkillName,
        entity.getIsAcceptedFlag(),
        entity.getAcceptedOn(),
        entity.getRemarks(),
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
