package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FollowupTaskResponse {

  private Long id;
  private Long followupCampaignId;
  private String campaignCode;
  private String campaignName;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFirstName;
  private String traineeLastName;
  private Long enrollmentId;
  private String enrollmentNumber;
  private Long placementId;
  private String placementNumber;
  private Long employmentId;
  private Long surveyId;
  private LocalDate scheduledDate;
  private LocalDate nextFollowupDate;
  private Long followupStatusId;
  private Long followupOutcomeId;
  private Long nonResponseReasonId;
  private Long assignedUserId;
  private Long lastChannelId;
  private String lastChannelCode;
  private String lastChannelName;
  private String remarks;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public FollowupTaskResponse() {
  }

  public FollowupTaskResponse(
      Long id,
      Long followupCampaignId,
      String campaignCode,
      String campaignName,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFirstName,
      String traineeLastName,
      Long enrollmentId,
      String enrollmentNumber,
      Long placementId,
      String placementNumber,
      Long employmentId,
      Long surveyId,
      LocalDate scheduledDate,
      LocalDate nextFollowupDate,
      Long followupStatusId,
      Long followupOutcomeId,
      Long nonResponseReasonId,
      Long assignedUserId,
      Long lastChannelId,
      String lastChannelCode,
      String lastChannelName,
      String remarks,
      LocalDateTime completedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.followupCampaignId = followupCampaignId;
    this.campaignCode = campaignCode;
    this.campaignName = campaignName;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFirstName = traineeFirstName;
    this.traineeLastName = traineeLastName;
    this.enrollmentId = enrollmentId;
    this.enrollmentNumber = enrollmentNumber;
    this.placementId = placementId;
    this.placementNumber = placementNumber;
    this.employmentId = employmentId;
    this.surveyId = surveyId;
    this.scheduledDate = scheduledDate;
    this.nextFollowupDate = nextFollowupDate;
    this.followupStatusId = followupStatusId;
    this.followupOutcomeId = followupOutcomeId;
    this.nonResponseReasonId = nonResponseReasonId;
    this.assignedUserId = assignedUserId;
    this.lastChannelId = lastChannelId;
    this.lastChannelCode = lastChannelCode;
    this.lastChannelName = lastChannelName;
    this.remarks = remarks;
    this.completedAt = completedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getFollowupCampaignId() {
    return followupCampaignId;
  }

  public void setFollowupCampaignId(Long followupCampaignId) {
    this.followupCampaignId = followupCampaignId;
  }

  public String getCampaignCode() {
    return campaignCode;
  }

  public void setCampaignCode(String campaignCode) {
    this.campaignCode = campaignCode;
  }

  public String getCampaignName() {
    return campaignName;
  }

  public void setCampaignName(String campaignName) {
    this.campaignName = campaignName;
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

  public String getTraineeFirstName() {
    return traineeFirstName;
  }

  public void setTraineeFirstName(String traineeFirstName) {
    this.traineeFirstName = traineeFirstName;
  }

  public String getTraineeLastName() {
    return traineeLastName;
  }

  public void setTraineeLastName(String traineeLastName) {
    this.traineeLastName = traineeLastName;
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

  public Long getPlacementId() {
    return placementId;
  }

  public void setPlacementId(Long placementId) {
    this.placementId = placementId;
  }

  public String getPlacementNumber() {
    return placementNumber;
  }

  public void setPlacementNumber(String placementNumber) {
    this.placementNumber = placementNumber;
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

  public String getLastChannelCode() {
    return lastChannelCode;
  }

  public void setLastChannelCode(String lastChannelCode) {
    this.lastChannelCode = lastChannelCode;
  }

  public String getLastChannelName() {
    return lastChannelName;
  }

  public void setLastChannelName(String lastChannelName) {
    this.lastChannelName = lastChannelName;
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
}
