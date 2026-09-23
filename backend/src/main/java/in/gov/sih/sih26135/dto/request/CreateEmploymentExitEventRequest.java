package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateEmploymentExitEventRequest {

  private Long employmentId;
  private Long traineeId;
  private Long enrollmentId;
  private LocalDate separationDate;
  private Long employmentExitReasonId;
  private Long separationNatureId;
  private Long employmentInfoSourceId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;
  private String remarks;

  public CreateEmploymentExitEventRequest() {}

  public CreateEmploymentExitEventRequest(
      Long employmentId,
      LocalDate separationDate,
      Long employmentExitReasonId,
      Long separationNatureId,
      Long employmentInfoSourceId,
      Long recordVerificationStatusId) {
    this.employmentId = employmentId;
    this.separationDate = separationDate;
    this.employmentExitReasonId = employmentExitReasonId;
    this.separationNatureId = separationNatureId;
    this.employmentInfoSourceId = employmentInfoSourceId;
    this.recordVerificationStatusId = recordVerificationStatusId;
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
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

  public LocalDate getSeparationDate() {
    return separationDate;
  }

  public void setSeparationDate(LocalDate separationDate) {
    this.separationDate = separationDate;
  }

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
  }

  public Long getSeparationNatureId() {
    return separationNatureId;
  }

  public void setSeparationNatureId(Long separationNatureId) {
    this.separationNatureId = separationNatureId;
  }

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
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

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
