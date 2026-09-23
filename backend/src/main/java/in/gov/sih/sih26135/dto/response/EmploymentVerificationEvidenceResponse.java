package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationEvidenceResponse(
    Long id,
    Long employmentVerificationId,
    String verificationNumber,
    Long employmentVerificationAttemptId,
    Long evidenceTypeId,
    String evidenceTypeCode,
    String evidenceTypeName,
    String documentReferenceCode,
    String contentSha256,
    String originalFilename,
    LocalDateTime capturedAt,
    Long uploadedByUserId,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
