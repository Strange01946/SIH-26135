package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CommunicationPurposeResponse;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import org.springframework.stereotype.Component;

@Component
public class CommunicationPurposeMapper {

  public CommunicationPurposeResponse toResponse(RefCommunicationPurpose entity) {
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

    return new CommunicationPurposeResponse(
        entity.getId(),
        entity.getPurposeCode(),
        entity.getPurposeName(),
        consentTypeId,
        consentTypeCode,
        consentTypeName,
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
