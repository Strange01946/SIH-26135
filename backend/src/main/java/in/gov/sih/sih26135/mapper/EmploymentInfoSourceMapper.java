package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentInfoSourceResponse;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import org.springframework.stereotype.Component;

@Component
public class EmploymentInfoSourceMapper {

  public EmploymentInfoSourceResponse toResponse(RefEmploymentInfoSource entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentInfoSourceResponse(
        entity.getId(),
        entity.getSourceCode(),
        entity.getSourceName(),
        entity.getIsSelfReportedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
