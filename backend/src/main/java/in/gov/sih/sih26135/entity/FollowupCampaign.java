package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "followup_campaigns")
public class FollowupCampaign {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "followup_campaign_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "campaign_code", length = 32, nullable = false, unique = true)
  private String campaignCode;

  @Column(name = "campaign_name", length = 200, nullable = false)
  private String campaignName;

  @Column(name = "followup_type_id", nullable = false)
  private Long followupTypeId;

  @Column(name = "survey_id")
  private Long surveyId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id")
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @Column(name = "scheduled_start_date", nullable = false)
  private LocalDate scheduledStartDate;

  @Column(name = "scheduled_end_date")
  private LocalDate scheduledEndDate;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public FollowupCampaign() {
  }

  public FollowupCampaign(String campaignCode, String campaignName, Long followupTypeId,
      LocalDate scheduledStartDate, Long lifecycleStatusId) {
    this.campaignCode = campaignCode;
    this.campaignName = campaignName;
    this.followupTypeId = followupTypeId;
    this.scheduledStartDate = scheduledStartDate;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowupCampaign that)) {
      return false;
    }
    return Objects.equals(campaignCode, that.campaignCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(campaignCode);
  }

  @Override
  public String toString() {
    return "FollowupCampaign{" +
        "id=" + id +
        ", campaignCode='" + campaignCode + '\'' +
        ", campaignName='" + campaignName + '\'' +
        ", scheduledStartDate=" + scheduledStartDate +
        '}';
  }
}
