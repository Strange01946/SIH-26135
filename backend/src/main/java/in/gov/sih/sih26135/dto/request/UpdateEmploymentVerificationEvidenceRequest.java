package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class UpdateEmploymentVerificationEvidenceRequest {

  private Long employmentVerificationAttemptId;
  private Long employmentVerificationEvidenceTypeId;
  @Size(max = 64, message = "documentReferenceCode cannot exceed 64 characters")
  private String documentReferenceCode;
  @Size(max = 64, message = "contentSha256 cannot exceed 64 characters")
  private String contentSha256;
  @Size(max = 200, message = "originalFilename cannot exceed 200 characters")
  private String originalFilename;
  private LocalDateTime capturedAt;
  private Long uploadedByUserId;
  @Size(max = 500, message = "notes cannot exceed 500 characters")
  private String notes;

  public UpdateEmploymentVerificationEvidenceRequest() {}

  public Long getEmploymentVerificationAttemptId() {
    return employmentVerificationAttemptId;
  }

  public void setEmploymentVerificationAttemptId(Long employmentVerificationAttemptId) {
    this.employmentVerificationAttemptId = employmentVerificationAttemptId;
  }

  public Long getEmploymentVerificationEvidenceTypeId() {
    return employmentVerificationEvidenceTypeId;
  }

  public void setEmploymentVerificationEvidenceTypeId(Long employmentVerificationEvidenceTypeId) {
    this.employmentVerificationEvidenceTypeId = employmentVerificationEvidenceTypeId;
  }

  public String getDocumentReferenceCode() {
    return documentReferenceCode;
  }

  public void setDocumentReferenceCode(String documentReferenceCode) {
    this.documentReferenceCode = documentReferenceCode;
  }

  public String getContentSha256() {
    return contentSha256;
  }

  public void setContentSha256(String contentSha256) {
    this.contentSha256 = contentSha256;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public LocalDateTime getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(LocalDateTime capturedAt) {
    this.capturedAt = capturedAt;
  }

  public Long getUploadedByUserId() {
    return uploadedByUserId;
  }

  public void setUploadedByUserId(Long uploadedByUserId) {
    this.uploadedByUserId = uploadedByUserId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
