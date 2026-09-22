package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentSpellStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentSpellStatus;
import org.springframework.stereotype.Component;

@Component
public class EmploymentSpellStatusMapper {

  public EmploymentSpellStatusResponse toResponse(RefEmploymentSpellStatus entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentSpellStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsActiveFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
