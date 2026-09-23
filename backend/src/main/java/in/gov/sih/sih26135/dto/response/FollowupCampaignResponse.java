package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FollowupCampaignResponse {

  private Long id;
  private String campaignCode;
  private String campaignName;
  private Long followupTypeId;
  private Long surveyId;
  private Long programId;
  private String programCode;
  private String programName;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private LocalDate scheduledStartDate;
  private LocalDate scheduledEndDate;
  private Long lifecycleStatusId;
  private Long createdByUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public FollowupCampaignResponse() {
  }

  public FollowupCampaignResponse(
      Long id,
      String campaignCode,
      String campaignName,
      Long followupTypeId,
      Long surveyId,
      Long programId,
      String programCode,
      String programName,
      Long courseId,
      String courseCode,
      String courseName,
      LocalDate scheduledStartDate,
      LocalDate scheduledEndDate,
      Long lifecycleStatusId,
      Long createdByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.campaignCode = campaignCode;
    this.campaignName = campaignName;
    this.followupTypeId = followupTypeId;
    this.surveyId = surveyId;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.scheduledStartDate = scheduledStartDate;
    this.scheduledEndDate = scheduledEndDate;
    this.lifecycleStatusId = lifecycleStatusId;
    this.createdByUserId = createdByUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getProgramCode() {
    return programCode;
  }

  public void setProgramCode(String programCode) {
    this.programCode = programCode;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
