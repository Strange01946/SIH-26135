package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

import in.gov.sih.sih26135.validation.DateSequence;

@DateSequence(startField = "scheduledStartDate", endField = "scheduledEndDate")
public class UpdateFollowupCampaignRequest {

  @Size(max = 200, message = "campaignName cannot exceed 200 characters")
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
