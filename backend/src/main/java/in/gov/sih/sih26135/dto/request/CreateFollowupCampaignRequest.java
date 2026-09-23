package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class CreateFollowupCampaignRequest {

  private String campaignCode;
  private String campaignName;
  private Long followupTypeId;
  private Long surveyId;
  private Long programId;
  private Long courseId;
  private LocalDate scheduledStartDate;
  private LocalDate scheduledEndDate;
  private Long lifecycleStatusId;
  private Long createdByUserId;

  public CreateFollowupCampaignRequest() {
  }

  public CreateFollowupCampaignRequest(
      String campaignCode,
      String campaignName,
      Long followupTypeId,
      LocalDate scheduledStartDate,
      Long lifecycleStatusId) {
    this.campaignCode = campaignCode;
    this.campaignName = campaignName;
    this.followupTypeId = followupTypeId;
    this.scheduledStartDate = scheduledStartDate;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public Long getFollowupTypeId() {
    return followupTypeId;
  }

  public void setFollowupTypeId(Long followupTypeId) {
    this.followupTypeId = followupTypeId;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public LocalDate getScheduledStartDate() {
    return scheduledStartDate;
  }

  public void setScheduledStartDate(LocalDate scheduledStartDate) {
    this.scheduledStartDate = scheduledStartDate;
  }

  public LocalDate getScheduledEndDate() {
    return scheduledEndDate;
  }

  public void setScheduledEndDate(LocalDate scheduledEndDate) {
    this.scheduledEndDate = scheduledEndDate;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
  }
}
