package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class UpdateFollowupCampaignRequest {

  private String campaignName;
  private Long followupTypeId;
  private Long surveyId;
  private Long programId;
  private Long courseId;
  private LocalDate scheduledStartDate;
  private LocalDate scheduledEndDate;
  private Long lifecycleStatusId;

  public UpdateFollowupCampaignRequest() {
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
}
