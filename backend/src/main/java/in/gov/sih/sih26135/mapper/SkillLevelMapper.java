package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillLevelResponse;
import in.gov.sih.sih26135.entity.SkillLevel;
import org.springframework.stereotype.Component;

@Component
public class SkillLevelMapper {

  public SkillLevelResponse toResponse(SkillLevel entity) {
    if (entity == null) {
      return null;
    }

    return new SkillLevelResponse(
        entity.getId(),
        entity.getLevelCode(),
        entity.getLevelName(),
        entity.getLevelRank(),
        entity.getDescription(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
