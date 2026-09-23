package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SeparationNatureResponse;
import in.gov.sih.sih26135.entity.RefSeparationNature;
import org.springframework.stereotype.Component;

@Component
public class SeparationNatureMapper {

  public SeparationNatureResponse toResponse(RefSeparationNature entity) {
    if (entity == null) {
      return null;
    }
    return new SeparationNatureResponse(
        entity.getId(),
        entity.getNatureCode(),
        entity.getNatureName(),
        entity.getIsVoluntaryFlag(),
        entity.getIsInvoluntaryFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
