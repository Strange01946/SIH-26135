package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.QualificationLevelResponse;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import org.springframework.stereotype.Component;

@Component
public class QualificationLevelMapper {

  public QualificationLevelResponse toResponse(RefQualificationLevel entity) {
    if (entity == null) {
      return null;
    }

    return new QualificationLevelResponse(
        entity.getId(),
        entity.getLevelCode(),
        entity.getLevelName(),
        entity.getNsqfLevel(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
