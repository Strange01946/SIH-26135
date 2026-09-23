package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateEmploymentVerificationRequestRequest {

  private Long statusId;
  private Long preferredMethodId;
  private Long employmentInfoSourceId;
  private Long assignedVerifierUserId;
  private Long followupTaskId;
  private Long surveyResponseId;
  private LocalDateTime dueAt;
  private LocalDateTime completedAt;
  private String remarks;

  public UpdateEmploymentVerificationRequestRequest() {}

  public Long getStatusId() {
    return statusId;
  }

  public void setStatusId(Long statusId) {
    this.statusId = statusId;
  }

  public Long getPreferredMethodId() {
    return preferredMethodId;
  }

  public void setPreferredMethodId(Long preferredMethodId) {
    this.preferredMethodId = preferredMethodId;
  }

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public Long getAssignedVerifierUserId() {
    return assignedVerifierUserId;
  }

  public void setAssignedVerifierUserId(Long assignedVerifierUserId) {
    this.assignedVerifierUserId = assignedVerifierUserId;
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public LocalDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(LocalDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
