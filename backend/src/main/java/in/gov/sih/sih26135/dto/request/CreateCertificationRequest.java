package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateCertificationRequest {

  private String certificateNumber;
  private Long traineeId;
  private Long enrollmentId;
  private Long courseId;
  private Long programId;
  private Long assessmentResultId;
  private String issuingBody;
  private LocalDate issueDate;
  private LocalDate expiryDate;
  private Long certificateStatusId;
  private Long certificateVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;

  public CreateCertificationRequest() {
  }

  public CreateCertificationRequest(
      String certificateNumber,
      LocalDate issueDate,
      Long certificateStatusId,
      Long certificateVerificationStatusId) {
    this.certificateNumber = certificateNumber;
    this.issueDate = issueDate;
    this.certificateStatusId = certificateStatusId;
    this.certificateVerificationStatusId = certificateVerificationStatusId;
  }

  public CreateCertificationRequest(
      String certificateNumber,
      Long traineeId,
      Long enrollmentId,
      Long courseId,
      Long programId,
      Long assessmentResultId,
      String issuingBody,
      LocalDate issueDate,
      LocalDate expiryDate,
      Long certificateStatusId,
      Long certificateVerificationStatusId,
      LocalDateTime verifiedAt,
      Long verifiedByUserId) {
    this.certificateNumber = certificateNumber;
    this.traineeId = traineeId;
    this.enrollmentId = enrollmentId;
    this.courseId = courseId;
    this.programId = programId;
    this.assessmentResultId = assessmentResultId;
    this.issuingBody = issuingBody;
    this.issueDate = issueDate;
    this.expiryDate = expiryDate;
    this.certificateStatusId = certificateStatusId;
    this.certificateVerificationStatusId = certificateVerificationStatusId;
    this.verifiedAt = verifiedAt;
    this.verifiedByUserId = verifiedByUserId;
  }

  public String getCertificateNumber() {
    return certificateNumber;
  }

  public void setCertificateNumber(String certificateNumber) {
    this.certificateNumber = certificateNumber;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getAssessmentResultId() {
    return assessmentResultId;
  }

  public void setAssessmentResultId(Long assessmentResultId) {
    this.assessmentResultId = assessmentResultId;
  }

  public String getIssuingBody() {
    return issuingBody;
  }

  public void setIssuingBody(String issuingBody) {
    this.issuingBody = issuingBody;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public Long getCertificateStatusId() {
    return certificateStatusId;
  }

  public void setCertificateStatusId(Long certificateStatusId) {
    this.certificateStatusId = certificateStatusId;
  }

  public Long getCertificateVerificationStatusId() {
    return certificateVerificationStatusId;
  }

  public void setCertificateVerificationStatusId(Long certificateVerificationStatusId) {
    this.certificateVerificationStatusId = certificateVerificationStatusId;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }
}
