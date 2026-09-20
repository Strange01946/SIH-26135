package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_followup_fact")
@Immutable
public class FollowupFact {

  @Id
  @Column(name = "followup_task_id", nullable = false)
  private Long followupTaskId;

  @Column(name = "followup_campaign_id", nullable = false)
  private Long followupCampaignId;

  @Column(name = "followup_type_id", nullable = false)
  private Long followupTypeId;

  @Column(name = "followup_type_code", length = 32, nullable = false)
  private String followupTypeCode;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "followup_offset_months", columnDefinition = "SMALLINT UNSIGNED")
  private Integer followupOffsetMonths;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "employment_id")
  private Long employmentId;

  @Column(name = "survey_id")
  private Long surveyId;

  @Column(name = "scheduled_date", nullable = false)
  private LocalDate scheduledDate;

  @Column(name = "next_followup_date")
  private LocalDate nextFollowupDate;

  @Column(name = "followup_status_id", nullable = false)
  private Long followupStatusId;

  @Column(name = "followup_status_code", length = 32, nullable = false)
  private String followupStatusCode;

  @Column(name = "is_open_flag", nullable = false)
  private Boolean isOpenFlag;

  @Column(name = "followup_outcome_id")
  private Long followupOutcomeId;

  @Column(name = "followup_outcome_code", length = 32)
  private String followupOutcomeCode;

  @Column(name = "is_success_flag")
  private Boolean isSuccessFlag;

  @Column(name = "is_unreachable_flag")
  private Boolean isUnreachableFlag;

  @Column(name = "is_no_response_flag")
  private Boolean isNoResponseFlag;

  @Column(name = "non_response_reason_id")
  private Long nonResponseReasonId;

  @Column(name = "last_channel_id")
  private Long lastChannelId;

  @Column(name = "last_channel_code", length = 32)
  private String lastChannelCode;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public FollowupFact() {
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Long getFollowupCampaignId() {
    return followupCampaignId;
  }

  public void setFollowupCampaignId(Long followupCampaignId) {
    this.followupCampaignId = followupCampaignId;
  }

  public Long getFollowupTypeId() {
    return followupTypeId;
  }

  public void setFollowupTypeId(Long followupTypeId) {
    this.followupTypeId = followupTypeId;
  }

  public String getFollowupTypeCode() {
    return followupTypeCode;
  }

  public void setFollowupTypeCode(String followupTypeCode) {
    this.followupTypeCode = followupTypeCode;
  }

  public Integer getFollowupOffsetMonths() {
    return followupOffsetMonths;
  }

  public void setFollowupOffsetMonths(Integer followupOffsetMonths) {
    this.followupOffsetMonths = followupOffsetMonths;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
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

  public String getFollowupStatusCode() {
    return followupStatusCode;
  }

  public void setFollowupStatusCode(String followupStatusCode) {
    this.followupStatusCode = followupStatusCode;
  }

  public Boolean getIsOpenFlag() {
    return isOpenFlag;
  }

  public void setIsOpenFlag(Boolean isOpenFlag) {
    this.isOpenFlag = isOpenFlag;
  }

  public Long getFollowupOutcomeId() {
    return followupOutcomeId;
  }

  public void setFollowupOutcomeId(Long followupOutcomeId) {
    this.followupOutcomeId = followupOutcomeId;
  }

  public String getFollowupOutcomeCode() {
    return followupOutcomeCode;
  }

  public void setFollowupOutcomeCode(String followupOutcomeCode) {
    this.followupOutcomeCode = followupOutcomeCode;
  }

  public Boolean getIsSuccessFlag() {
    return isSuccessFlag;
  }

  public void setIsSuccessFlag(Boolean isSuccessFlag) {
    this.isSuccessFlag = isSuccessFlag;
  }

  public Boolean getIsUnreachableFlag() {
    return isUnreachableFlag;
  }

  public void setIsUnreachableFlag(Boolean isUnreachableFlag) {
    this.isUnreachableFlag = isUnreachableFlag;
  }

  public Boolean getIsNoResponseFlag() {
    return isNoResponseFlag;
  }

  public void setIsNoResponseFlag(Boolean isNoResponseFlag) {
    this.isNoResponseFlag = isNoResponseFlag;
  }

  public Long getNonResponseReasonId() {
    return nonResponseReasonId;
  }

  public void setNonResponseReasonId(Long nonResponseReasonId) {
    this.nonResponseReasonId = nonResponseReasonId;
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

  public Long getTraineeStateId() {
    return traineeStateId;
  }

  public void setTraineeStateId(Long traineeStateId) {
    this.traineeStateId = traineeStateId;
  }

  public Long getTraineeDistrictId() {
    return traineeDistrictId;
  }

  public void setTraineeDistrictId(Long traineeDistrictId) {
    this.traineeDistrictId = traineeDistrictId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowupFact that)) {
      return false;
    }
    return Objects.equals(followupTaskId, that.followupTaskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(followupTaskId);
  }
}
