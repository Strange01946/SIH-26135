package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateEmploymentVerificationAttemptRequest {

  private Long employmentVerificationAttemptStatusId;
  private Long employerRespondentUserId;
  private Long communicationLogId;
  private LocalDateTime completedAt;
  private String notes;

  public UpdateEmploymentVerificationAttemptRequest() {}

  public Long getEmploymentVerificationAttemptStatusId() {
    return employmentVerificationAttemptStatusId;
  }

  public void setEmploymentVerificationAttemptStatusId(Long employmentVerificationAttemptStatusId) {
    this.employmentVerificationAttemptStatusId = employmentVerificationAttemptStatusId;
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
