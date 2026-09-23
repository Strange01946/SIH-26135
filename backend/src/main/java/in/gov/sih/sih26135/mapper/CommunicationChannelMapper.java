package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CommunicationChannelResponse;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import org.springframework.stereotype.Component;

@Component
public class CommunicationChannelMapper {

  public CommunicationChannelResponse toResponse(RefCommunicationChannel entity) {
    if (entity == null) {
      return null;
    }

    Long consentTypeId = null;
    String consentTypeCode = null;
    String consentTypeName = null;
    if (entity.getRequiredConsentType() != null) {
      consentTypeId = entity.getRequiredConsentType().getId();
      consentTypeCode = entity.getRequiredConsentType().getConsentCode();
      consentTypeName = entity.getRequiredConsentType().getConsentName();
    }

    return new CommunicationChannelResponse(
        entity.getId(),
        entity.getChannelCode(),
        entity.getChannelName(),
        consentTypeId,
        consentTypeCode,
        consentTypeName,
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
