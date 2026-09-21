package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ConsentStatusResponse;
import in.gov.sih.sih26135.dto.response.ConsentTypeResponse;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import org.springframework.stereotype.Component;

@Component
public class ConsentReferenceMapper {

  public ConsentTypeResponse toTypeResponse(RefConsentType entity) {
    if (entity == null) {
      return null;
    }

    return new ConsentTypeResponse(
        entity.getId(),
        entity.getConsentCode(),
        entity.getConsentName(),
        entity.getPurpose(),
        entity.getAllowsEmploymentFollowup(),
        entity.getIsRequired(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public ConsentStatusResponse toStatusResponse(RefConsentStatus entity) {
    if (entity == null) {
      return null;
    }

    return new ConsentStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
