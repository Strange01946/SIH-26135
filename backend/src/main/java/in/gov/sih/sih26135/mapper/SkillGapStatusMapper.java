package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapStatusResponse;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import org.springframework.stereotype.Component;

@Component
public class SkillGapStatusMapper {

  public SkillGapStatusResponse toResponse(RefSkillGapStatus entity) {
    if (entity == null) {
      return null;
    }
    return new SkillGapStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsOpenFlag(),
        entity.getIsResolvedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
