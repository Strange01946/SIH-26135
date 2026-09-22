package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CertificationResponse {

  private Long id;
  private String certificateNumber;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFullName;
  private Long enrollmentId;
  private String enrollmentNumber;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long programId;
  private String programCode;
  private String programName;
  private Long assessmentResultId;
  private String issuingBody;
  private LocalDate issueDate;
  private LocalDate expiryDate;
  private Long certificateStatusId;
  private String certificateStatusCode;
  private String certificateStatusName;
  private Long certificateVerificationStatusId;
  private String certificateVerificationStatusCode;
  private String certificateVerificationStatusName;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public CertificationResponse() {
  }

  public CertificationResponse(
      Long id,
      String certificateNumber,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFullName,
      Long enrollmentId,
      String enrollmentNumber,
      Long courseId,
      String courseCode,
      String courseName,
      Long programId,
      String programCode,
      String programName,
      Long assessmentResultId,
      String issuingBody,
      LocalDate issueDate,
      LocalDate expiryDate,
      Long certificateStatusId,
      String certificateStatusCode,
      String certificateStatusName,
      Long certificateVerificationStatusId,
      String certificateVerificationStatusCode,
      String certificateVerificationStatusName,
      LocalDateTime verifiedAt,
      Long verifiedByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.certificateNumber = certificateNumber;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFullName = traineeFullName;
    this.enrollmentId = enrollmentId;
    this.enrollmentNumber = enrollmentNumber;
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.assessmentResultId = assessmentResultId;
    this.issuingBody = issuingBody;
    this.issueDate = issueDate;
    this.expiryDate = expiryDate;
    this.certificateStatusId = certificateStatusId;
    this.certificateStatusCode = certificateStatusCode;
    this.certificateStatusName = certificateStatusName;
    this.certificateVerificationStatusId = certificateVerificationStatusId;
    this.certificateVerificationStatusCode = certificateVerificationStatusCode;
    this.certificateVerificationStatusName = certificateVerificationStatusName;
    this.verifiedAt = verifiedAt;
    this.verifiedByUserId = verifiedByUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public String getTraineeFullName() {
    return traineeFullName;
  }

  public void setTraineeFullName(String traineeFullName) {
    this.traineeFullName = traineeFullName;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public String getProgramCode() {
    return programCode;
  }

  public void setProgramCode(String programCode) {
    this.programCode = programCode;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
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

  public String getCertificateStatusCode() {
    return certificateStatusCode;
  }

  public void setCertificateStatusCode(String certificateStatusCode) {
    this.certificateStatusCode = certificateStatusCode;
  }

  public String getCertificateStatusName() {
    return certificateStatusName;
  }

  public void setCertificateStatusName(String certificateStatusName) {
    this.certificateStatusName = certificateStatusName;
  }

  public Long getCertificateVerificationStatusId() {
    return certificateVerificationStatusId;
  }

  public void setCertificateVerificationStatusId(Long certificateVerificationStatusId) {
    this.certificateVerificationStatusId = certificateVerificationStatusId;
  }

  public String getCertificateVerificationStatusCode() {
    return certificateVerificationStatusCode;
  }

  public void setCertificateVerificationStatusCode(String certificateVerificationStatusCode) {
    this.certificateVerificationStatusCode = certificateVerificationStatusCode;
  }

  public String getCertificateVerificationStatusName() {
    return certificateVerificationStatusName;
  }

  public void setCertificateVerificationStatusName(String certificateVerificationStatusName) {
    this.certificateVerificationStatusName = certificateVerificationStatusName;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
