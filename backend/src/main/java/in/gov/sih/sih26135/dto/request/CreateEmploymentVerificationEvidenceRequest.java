package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateEmploymentVerificationEvidenceRequest {

  private Long employmentVerificationId;
  private Long employmentVerificationAttemptId;
  private Long employmentVerificationEvidenceTypeId;
  private String documentReferenceCode;
  private String contentSha256;
  private String originalFilename;
  private LocalDateTime capturedAt;
  private Long uploadedByUserId;
  private String notes;

  public CreateEmploymentVerificationEvidenceRequest() {}

  public CreateEmploymentVerificationEvidenceRequest(
      Long employmentVerificationId,
      Long employmentVerificationEvidenceTypeId,
      String documentReferenceCode,
      LocalDateTime capturedAt) {
    this.employmentVerificationId = employmentVerificationId;
    this.employmentVerificationEvidenceTypeId = employmentVerificationEvidenceTypeId;
    this.documentReferenceCode = documentReferenceCode;
    this.capturedAt = capturedAt;
  }

  public Long getEmploymentVerificationId() {
    return employmentVerificationId;
  }

  public void setEmploymentVerificationId(Long employmentVerificationId) {
    this.employmentVerificationId = employmentVerificationId;
  }

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
