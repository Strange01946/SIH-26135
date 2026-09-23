package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapSourceResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSource;
import org.springframework.stereotype.Component;

@Component
public class SkillGapSourceMapper {

  public SkillGapSourceResponse toResponse(RefSkillGapSource entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapSourceResponse(
        entity.getId(),
        entity.getSourceCode(),
        entity.getSourceName(),
        entity.getIsSelfReportedFlag(),
        entity.getIsEmployerFlag(),
        entity.getIsOfficialFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
