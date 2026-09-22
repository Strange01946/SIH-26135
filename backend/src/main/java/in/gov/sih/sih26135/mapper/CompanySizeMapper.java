package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CompanySizeResponse;
import in.gov.sih.sih26135.entity.RefCompanySize;
import org.springframework.stereotype.Component;

@Component
public class CompanySizeMapper {

  public CompanySizeResponse toResponse(RefCompanySize entity) {
    if (entity == null) {
      return null;
    }
    return new CompanySizeResponse(
        entity.getId(),
        entity.getSizeCode(),
        entity.getSizeName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
