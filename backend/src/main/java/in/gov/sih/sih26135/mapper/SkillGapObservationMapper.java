package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapObservationResponse;
import in.gov.sih.sih26135.entity.SkillGapObservation;
import org.springframework.stereotype.Component;

@Component
public class SkillGapObservationMapper {

  public SkillGapObservationResponse toResponse(SkillGapObservation entity) {
    if (entity == null) {
      return null;
    }

    Long skillGapId = entity.getSkillGap() != null ? entity.getSkillGap().getId() : null;
    String skillGapNumber = entity.getSkillGap() != null ? entity.getSkillGap().getSkillGapNumber() : null;

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

    return new SkillGapObservationResponse(
        entity.getId(),
        skillGapId,
        skillGapNumber,
        entity.getObservationNumber(),
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
        entity.getObservedOn(),
        entity.getObservedByUserId(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
