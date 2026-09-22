package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CertificateVerificationStatusResponse;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class CertificateVerificationStatusMapper {

  public CertificateVerificationStatusResponse toResponse(RefCertificateVerificationStatus entity) {
    if (entity == null) {
      return null;
    }

    return new CertificateVerificationStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
