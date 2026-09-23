package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.SkillGapSummary;
import org.springframework.stereotype.Component;

@Component
public class SkillGapSummaryMapper {

  public SkillGapSummaryResponse toResponse(SkillGapSummary entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapSummaryResponse(
        entity.getSkillId(),
        entity.getSkillCode(),
        entity.getSkillGapSeverityId(),
        entity.getSeverityCode(),
        entity.getGapCount(),
        entity.getTraineeCount(),
        entity.getCurrentGapCount(),
        entity.getAvgGapLevelDelta()
    );
  }
}
