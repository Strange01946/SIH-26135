package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateEmploymentVerificationAttemptRequest {

  private Long employmentVerificationRequestId;
  private Integer attemptNumber;
  private Long employmentVerificationMethodId;
  private Long employmentVerificationAttemptStatusId;
  private Long attemptedByUserId;
  private Long employerRespondentUserId;
  private Long communicationLogId;
  private LocalDateTime attemptedAt;
  private LocalDateTime completedAt;
  private String notes;

  public CreateEmploymentVerificationAttemptRequest() {}

  public CreateEmploymentVerificationAttemptRequest(
      Long employmentVerificationRequestId,
      Long employmentVerificationMethodId,
      Long employmentVerificationAttemptStatusId,
      LocalDateTime attemptedAt) {
    this.employmentVerificationRequestId = employmentVerificationRequestId;
    this.employmentVerificationMethodId = employmentVerificationMethodId;
    this.employmentVerificationAttemptStatusId = employmentVerificationAttemptStatusId;
    this.attemptedAt = attemptedAt;
  }

  public Long getEmploymentVerificationRequestId() {
    return employmentVerificationRequestId;
  }

  public void setEmploymentVerificationRequestId(Long employmentVerificationRequestId) {
    this.employmentVerificationRequestId = employmentVerificationRequestId;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public Long getEmploymentVerificationMethodId() {
    return employmentVerificationMethodId;
  }

  public void setEmploymentVerificationMethodId(Long employmentVerificationMethodId) {
    this.employmentVerificationMethodId = employmentVerificationMethodId;
  }

  public Long getEmploymentVerificationAttemptStatusId() {
    return employmentVerificationAttemptStatusId;
  }

  public void setEmploymentVerificationAttemptStatusId(Long employmentVerificationAttemptStatusId) {
    this.employmentVerificationAttemptStatusId = employmentVerificationAttemptStatusId;
  }

  public Long getAttemptedByUserId() {
    return attemptedByUserId;
  }

  public void setAttemptedByUserId(Long attemptedByUserId) {
    this.attemptedByUserId = attemptedByUserId;
  }

  public Long getEmployerRespondentUserId() {
    return employerRespondentUserId;
  }

  public void setEmployerRespondentUserId(Long employerRespondentUserId) {
    this.employerRespondentUserId = employerRespondentUserId;
  }

  public Long getCommunicationLogId() {
    return communicationLogId;
  }

  public void setCommunicationLogId(Long communicationLogId) {
    this.communicationLogId = communicationLogId;
  }

  public LocalDateTime getAttemptedAt() {
    return attemptedAt;
  }

  public void setAttemptedAt(LocalDateTime attemptedAt) {
    this.attemptedAt = attemptedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
