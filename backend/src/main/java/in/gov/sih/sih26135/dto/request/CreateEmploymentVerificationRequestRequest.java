package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateEmploymentVerificationRequestRequest {

  private String requestNumber;
  private Long employmentRecordId;
  private Long traineeId;
  private Long placementRecordId;
  private Long employerId;
  private Integer cycleNumber;
  private Boolean isReverification;
  private Long statusId;
  private Long preferredMethodId;
  private Long employmentInfoSourceId;
  private Long requestedByUserId;
  private Long assignedVerifierUserId;
  private Long followupTaskId;
  private Long surveyResponseId;
  private LocalDateTime requestedAt;
  private LocalDateTime dueAt;
  private String remarks;

  public CreateEmploymentVerificationRequestRequest() {}

  public CreateEmploymentVerificationRequestRequest(
      String requestNumber,
      Long employmentRecordId,
      Long statusId,
      LocalDateTime requestedAt) {
    this.requestNumber = requestNumber;
    this.employmentRecordId = employmentRecordId;
    this.statusId = statusId;
    this.requestedAt = requestedAt;
  }

  public String getRequestNumber() {
    return requestNumber;
  }

  public void setRequestNumber(String requestNumber) {
    this.requestNumber = requestNumber;
  }

  public Long getEmploymentRecordId() {
    return employmentRecordId;
  }

  public void setEmploymentRecordId(Long employmentRecordId) {
    this.employmentRecordId = employmentRecordId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getPlacementRecordId() {
    return placementRecordId;
  }

  public void setPlacementRecordId(Long placementRecordId) {
    this.placementRecordId = placementRecordId;
  }

  public Long getEmployerId() {
    return employerId;
  }

  public void setEmployerId(Long employerId) {
    this.employerId = employerId;
  }

  public Integer getCycleNumber() {
    return cycleNumber;
  }

  public void setCycleNumber(Integer cycleNumber) {
    this.cycleNumber = cycleNumber;
  }

  public Boolean getIsReverification() {
    return isReverification;
  }

  public void setIsReverification(Boolean reverification) {
    isReverification = reverification;
  }

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

  public Long getRequestedByUserId() {
    return requestedByUserId;
  }

  public void setRequestedByUserId(Long requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
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

  public LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(LocalDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public LocalDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(LocalDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
