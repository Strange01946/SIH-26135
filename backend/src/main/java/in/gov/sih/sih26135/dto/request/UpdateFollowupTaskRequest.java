package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdateFollowupTaskRequest {

  private Long enrollmentId;
  private Long placementId;
  private Long employmentId;
  private Long surveyId;
  private LocalDate scheduledDate;
  private LocalDate nextFollowupDate;
  private Long followupStatusId;
  private Long followupOutcomeId;
  private Long nonResponseReasonId;
  private Long assignedUserId;
  private Long lastChannelId;
  private String remarks;
  private LocalDateTime completedAt;

  public UpdateFollowupTaskRequest() {
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getPlacementId() {
    return placementId;
  }

  public void setPlacementId(Long placementId) {
    this.placementId = placementId;
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public LocalDate getScheduledDate() {
    return scheduledDate;
  }

  public void setScheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public LocalDate getNextFollowupDate() {
    return nextFollowupDate;
  }

  public void setNextFollowupDate(LocalDate nextFollowupDate) {
    this.nextFollowupDate = nextFollowupDate;
  }

  public Long getFollowupStatusId() {
    return followupStatusId;
  }

  public void setFollowupStatusId(Long followupStatusId) {
    this.followupStatusId = followupStatusId;
  }

  public Long getFollowupOutcomeId() {
    return followupOutcomeId;
  }

  public void setFollowupOutcomeId(Long followupOutcomeId) {
    this.followupOutcomeId = followupOutcomeId;
  }

  public Long getNonResponseReasonId() {
    return nonResponseReasonId;
  }

  public void setNonResponseReasonId(Long nonResponseReasonId) {
    this.nonResponseReasonId = nonResponseReasonId;
  }

  public Long getAssignedUserId() {
    return assignedUserId;
  }

  public void setAssignedUserId(Long assignedUserId) {
    this.assignedUserId = assignedUserId;
  }

  public Long getLastChannelId() {
    return lastChannelId;
  }

  public void setLastChannelId(Long lastChannelId) {
    this.lastChannelId = lastChannelId;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
