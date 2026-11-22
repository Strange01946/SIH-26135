package in.gov.sih.sih26135.dto.registration;

import java.time.LocalDateTime;

public class ApplicationStatusResponse {

  private String applicationNumber;
  private String applicantType;
  private String organizationName;
  private String contactEmail;
  private String status;
  private String rejectionReason;
  private LocalDateTime submittedAt;
  private LocalDateTime reviewedAt;

  public ApplicationStatusResponse() {
  }

  public ApplicationStatusResponse(
      String applicationNumber,
      String applicantType,
      String organizationName,
      String contactEmail,
      String status,
      String rejectionReason,
      LocalDateTime submittedAt,
      LocalDateTime reviewedAt) {
    this.applicationNumber = applicationNumber;
    this.applicantType = applicantType;
    this.organizationName = organizationName;
    this.contactEmail = contactEmail;
    this.status = status;
    this.rejectionReason = rejectionReason;
    this.submittedAt = submittedAt;
    this.reviewedAt = reviewedAt;
  }

  public String getApplicationNumber() {
    return applicationNumber;
  }

  public void setApplicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public String getApplicantType() {
    return applicantType;
  }

  public void setApplicantType(String applicantType) {
    this.applicantType = applicantType;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(LocalDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
  }
}
