package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CertificateStatusResponse;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import org.springframework.stereotype.Component;

@Component
public class CertificateStatusMapper {

  public CertificateStatusResponse toResponse(RefCertificateStatus entity) {
    if (entity == null) {
      return null;
    }

    return new CertificateStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
