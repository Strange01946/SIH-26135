package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SectorResponse;
import in.gov.sih.sih26135.entity.Sector;
import org.springframework.stereotype.Component;

@Component
public class SectorMapper {

  public SectorResponse toResponse(Sector entity) {
    if (entity == null) {
      return null;
    }

    return new SectorResponse(
        entity.getId(),
        entity.getSectorCode(),
        entity.getSectorName(),
        entity.getDescription(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }
}
