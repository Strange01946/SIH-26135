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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "followup_tasks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_followup_tasks_campaign_trainee", columnNames = {"followup_campaign_id", "trainee_id"})
})
public class FollowupTask {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "followup_task_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_campaign_id", nullable = false)
  private FollowupCampaign followupCampaign;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id")
  private PlacementRecord placementRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_id")
  private EmploymentRecord employmentRecord;

  @Column(name = "survey_id")
  private Long surveyId;

  @Column(name = "scheduled_date", nullable = false)
  private LocalDate scheduledDate;

  @Column(name = "next_followup_date")
  private LocalDate nextFollowupDate;

  @Column(name = "followup_status_id", nullable = false)
  private Long followupStatusId;

  @Column(name = "followup_outcome_id")
  private Long followupOutcomeId;

  @Column(name = "non_response_reason_id")
  private Long nonResponseReasonId;

  @Column(name = "assigned_user_id")
  private Long assignedUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_channel_id")
  private RefCommunicationChannel lastChannel;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public FollowupTask() {
  }

  public FollowupTask(FollowupCampaign followupCampaign, Trainee trainee, LocalDate scheduledDate, Long followupStatusId) {
    this.followupCampaign = followupCampaign;
    this.trainee = trainee;
    this.scheduledDate = scheduledDate;
    this.followupStatusId = followupStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public FollowupCampaign getFollowupCampaign() {
    return followupCampaign;
  }

  public void setFollowupCampaign(FollowupCampaign followupCampaign) {
    this.followupCampaign = followupCampaign;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public TrainingEnrollment getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(TrainingEnrollment enrollment) {
    this.enrollment = enrollment;
  }

  public PlacementRecord getPlacementRecord() {
    return placementRecord;
  }

  public void setPlacementRecord(PlacementRecord placementRecord) {
    this.placementRecord = placementRecord;
  }

  public EmploymentRecord getEmploymentRecord() {
    return employmentRecord;
  }

  public void setEmploymentRecord(EmploymentRecord employmentRecord) {
    this.employmentRecord = employmentRecord;
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

  public RefCommunicationChannel getLastChannel() {
    return lastChannel;
  }

  public void setLastChannel(RefCommunicationChannel lastChannel) {
    this.lastChannel = lastChannel;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowupTask that)) {
      return false;
    }
    return Objects.equals(followupCampaign != null ? followupCampaign.getId() : null, that.followupCampaign != null ? that.followupCampaign.getId() : null)
        && Objects.equals(trainee != null ? trainee.getId() : null, that.trainee != null ? that.trainee.getId() : null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(followupCampaign != null ? followupCampaign.getId() : null, trainee != null ? trainee.getId() : null);
  }

  @Override
  public String toString() {
    return "FollowupTask{" +
        "id=" + id +
        ", scheduledDate=" + scheduledDate +
        ", followupStatusId=" + followupStatusId +
        '}';
  }
}
