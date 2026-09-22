package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillImportanceResponse;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import org.springframework.stereotype.Component;

@Component
public class SkillImportanceMapper {

  public SkillImportanceResponse toResponse(RefSkillImportance entity) {
    if (entity == null) {
      return null;
    }

    return new SkillImportanceResponse(
        entity.getId(),
        entity.getImportanceCode(),
        entity.getImportanceName(),
        entity.getImportanceWeight(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
