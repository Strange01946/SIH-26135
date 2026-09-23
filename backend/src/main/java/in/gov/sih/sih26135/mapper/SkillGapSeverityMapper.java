package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapSeverityResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSeverity;
import org.springframework.stereotype.Component;

@Component
public class SkillGapSeverityMapper {

  public SkillGapSeverityResponse toResponse(RefSkillGapSeverity entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapSeverityResponse(
        entity.getId(),
        entity.getSeverityCode(),
        entity.getSeverityName(),
        entity.getSeverityRank(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
