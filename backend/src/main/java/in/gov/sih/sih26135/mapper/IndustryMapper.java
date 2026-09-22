package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.IndustryResponse;
import in.gov.sih.sih26135.entity.Industry;
import org.springframework.stereotype.Component;

@Component
public class IndustryMapper {

  public IndustryResponse toResponse(Industry entity) {
    if (entity == null) {
      return null;
    }

    Long sectorId = null;
    String sectorCode = null;
    String sectorName = null;
    if (entity.getSector() != null) {
      sectorId = entity.getSector().getId();
      sectorCode = entity.getSector().getSectorCode();
      sectorName = entity.getSector().getSectorName();
    }

    return new IndustryResponse(
        entity.getId(),
        entity.getIndustryCode(),
        entity.getIndustryName(),
        sectorId,
        sectorCode,
        sectorName,
        entity.getDescription(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }
}
