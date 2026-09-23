package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.entity.SkillGap;
import org.springframework.stereotype.Component;

@Component
public class SkillGapMapper {

  public SkillGapResponse toResponse(SkillGap entity) {
    if (entity == null) {
      return null;
    }

    Long assessmentId = entity.getSkillGapAssessment() != null ? entity.getSkillGapAssessment().getId() : null;
    String assessmentNumber = entity.getSkillGapAssessment() != null ? entity.getSkillGapAssessment().getAssessmentNumber() : null;

    Long traineeId = entity.getTrainee() != null ? entity.getTrainee().getId() : null;
    String traineeRegistrationNumber = entity.getTrainee() != null ? entity.getTrainee().getRegistrationNumber() : null;
    String traineeFirstName = entity.getTrainee() != null ? entity.getTrainee().getFirstName() : null;
    String traineeLastName = entity.getTrainee() != null ? entity.getTrainee().getLastName() : null;

    Long skillId = entity.getSkill() != null ? entity.getSkill().getId() : null;
    String skillCode = entity.getSkill() != null ? entity.getSkill().getSkillCode() : null;
    String skillName = entity.getSkill() != null ? entity.getSkill().getSkillName() : null;

    Long observedSkillLevelId = entity.getObservedSkillLevel() != null ? entity.getObservedSkillLevel().getId() : null;
    String observedSkillLevelCode = entity.getObservedSkillLevel() != null ? entity.getObservedSkillLevel().getLevelCode() : null;
    String observedSkillLevelName = entity.getObservedSkillLevel() != null ? entity.getObservedSkillLevel().getLevelName() : null;
    Integer observedSkillLevelRank = entity.getObservedSkillLevel() != null ? entity.getObservedSkillLevel().getLevelRank() : null;

    Long requiredSkillLevelId = entity.getRequiredSkillLevel() != null ? entity.getRequiredSkillLevel().getId() : null;
    String requiredSkillLevelCode = entity.getRequiredSkillLevel() != null ? entity.getRequiredSkillLevel().getLevelCode() : null;
    String requiredSkillLevelName = entity.getRequiredSkillLevel() != null ? entity.getRequiredSkillLevel().getLevelName() : null;
    Integer requiredSkillLevelRank = entity.getRequiredSkillLevel() != null ? entity.getRequiredSkillLevel().getLevelRank() : null;

    Long targetSkillLevelId = entity.getTargetSkillLevel() != null ? entity.getTargetSkillLevel().getId() : null;
    String targetSkillLevelCode = entity.getTargetSkillLevel() != null ? entity.getTargetSkillLevel().getLevelCode() : null;
    String targetSkillLevelName = entity.getTargetSkillLevel() != null ? entity.getTargetSkillLevel().getLevelName() : null;
    Integer targetSkillLevelRank = entity.getTargetSkillLevel() != null ? entity.getTargetSkillLevel().getLevelRank() : null;

    Long skillImportanceId = entity.getSkillImportance() != null ? entity.getSkillImportance().getId() : null;
    String skillImportanceCode = entity.getSkillImportance() != null ? entity.getSkillImportance().getImportanceCode() : null;
    String skillImportanceName = entity.getSkillImportance() != null ? entity.getSkillImportance().getImportanceName() : null;

    Long skillGapSeverityId = entity.getSkillGapSeverity() != null ? entity.getSkillGapSeverity().getId() : null;
    String skillGapSeverityCode = entity.getSkillGapSeverity() != null ? entity.getSkillGapSeverity().getSeverityCode() : null;
    String skillGapSeverityName = entity.getSkillGapSeverity() != null ? entity.getSkillGapSeverity().getSeverityName() : null;
    Integer skillGapSeverityRank = entity.getSkillGapSeverity() != null ? entity.getSkillGapSeverity().getSeverityRank() : null;

    Long skillGapStatusId = entity.getSkillGapStatus() != null ? entity.getSkillGapStatus().getId() : null;
    String skillGapStatusCode = entity.getSkillGapStatus() != null ? entity.getSkillGapStatus().getStatusCode() : null;
    String skillGapStatusName = entity.getSkillGapStatus() != null ? entity.getSkillGapStatus().getStatusName() : null;

    Long skillGapSourceId = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getId() : null;
    String skillGapSourceCode = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getSourceCode() : null;
    String skillGapSourceName = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getSourceName() : null;

    return new SkillGapResponse(
        entity.getId(),
        entity.getSkillGapNumber(),
        assessmentId,
        assessmentNumber,
        traineeId,
        traineeRegistrationNumber,
        traineeFirstName,
        traineeLastName,
        skillId,
        skillCode,
        skillName,
        observedSkillLevelId,
        observedSkillLevelCode,
        observedSkillLevelName,
        observedSkillLevelRank,
        requiredSkillLevelId,
        requiredSkillLevelCode,
        requiredSkillLevelName,
        requiredSkillLevelRank,
        targetSkillLevelId,
        targetSkillLevelCode,
        targetSkillLevelName,
        targetSkillLevelRank,
        entity.getGapLevelDelta(),
        skillImportanceId,
        skillImportanceCode,
        skillImportanceName,
        skillGapSeverityId,
        skillGapSeverityCode,
        skillGapSeverityName,
        skillGapSeverityRank,
        skillGapStatusId,
        skillGapStatusCode,
        skillGapStatusName,
        skillGapSourceId,
        skillGapSourceCode,
        skillGapSourceName,
        entity.getIsCurrent(),
        entity.getCurrentGapKey(),
        entity.getIdentifiedOn(),
        entity.getResolvedOn(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
