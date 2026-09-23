package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import in.gov.sih.sih26135.entity.analytics.SkillGapFact;
import org.springframework.stereotype.Component;

@Component
public class SkillGapFactMapper {

  public SkillGapFactResponse toResponse(SkillGapFact entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapFactResponse(
        entity.getSkillGapId(),
        entity.getSkillGapNumber(),
        entity.getSkillGapAssessmentId(),
        entity.getTraineeId(),
        entity.getSkillId(),
        entity.getSkillCode(),
        entity.getEnrollmentId(),
        entity.getCourseId(),
        entity.getJobRoleId(),
        entity.getEmploymentId(),
        entity.getPlacementId(),
        entity.getObservedSkillLevelId(),
        entity.getRequiredSkillLevelId(),
        entity.getGapLevelDelta(),
        entity.getSkillGapSeverityId(),
        entity.getSeverityCode(),
        entity.getSkillGapStatusId(),
        entity.getSkillGapStatusCode(),
        entity.getSkillGapSourceId(),
        entity.getSkillGapSourceCode(),
        entity.getIsCurrent(),
        entity.getIdentifiedOn(),
        entity.getResolvedOn(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId(),
        entity.getRecommendationCount(),
        entity.getAcceptedRecommendationCount()
    );
  }
}
