package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import in.gov.sih.sih26135.entity.EmploymentVerificationEvidence;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationEvidenceMapper {

  public EmploymentVerificationEvidenceResponse toResponse(EmploymentVerificationEvidence entity) {
    if (entity == null) {
      return null;
    }

    Long verificationId = entity.getEmploymentVerification() != null ? entity.getEmploymentVerification().getId() : null;
    String verificationNumber = entity.getEmploymentVerification() != null ? entity.getEmploymentVerification().getVerificationNumber() : null;

    Long attemptId = entity.getEmploymentVerificationAttempt() != null ? entity.getEmploymentVerificationAttempt().getId() : null;

    Long typeId = entity.getEmploymentVerificationEvidenceType() != null ? entity.getEmploymentVerificationEvidenceType().getId() : null;
    String typeCode = entity.getEmploymentVerificationEvidenceType() != null ? entity.getEmploymentVerificationEvidenceType().getTypeCode() : null;
    String typeName = entity.getEmploymentVerificationEvidenceType() != null ? entity.getEmploymentVerificationEvidenceType().getTypeName() : null;

    return new EmploymentVerificationEvidenceResponse(
        entity.getId(),
        verificationId,
        verificationNumber,
        attemptId,
        typeId,
        typeCode,
        typeName,
        entity.getDocumentReferenceCode(),
        entity.getContentSha256(),
        entity.getOriginalFilename(),
        entity.getCapturedAt(),
        entity.getUploadedByUserId(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
