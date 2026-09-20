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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "trainee_unemployment_events", uniqueConstraints = {
    @UniqueConstraint(name = "uk_unemployment_events_trainee_period", columnNames = {
        "trainee_id", "period_number"
    }),
    @UniqueConstraint(name = "uk_unemployment_events_trainee_start", columnNames = {
        "trainee_id", "start_date"
    }),
    @UniqueConstraint(name = "uk_unemployment_events_current", columnNames = {
        "trainee_id", "current_period_key"
    }),
    @UniqueConstraint(name = "uk_unemployment_events_exit", columnNames = {
        "employment_exit_event_id"
    })
})
public class TraineeUnemploymentEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "unemployment_event_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "period_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer periodNumber;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent = true;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "current_period_key", insertable = false, updatable = false)
  private Integer currentPeriodKey;

  @Column(name = "labour_status_id", nullable = false)
  private Long labourStatusId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unemployment_reason_id", nullable = false)
  private RefUnemploymentReason unemploymentReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preceding_employment_id")
  private EmploymentRecord precedingEmployment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_exit_event_id", unique = true)
  private EmploymentExitEvent employmentExitEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "succeeding_employment_id")
  private EmploymentRecord succeedingEmployment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id")
  private PlacementRecord placementRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_task_id")
  private FollowupTask followupTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_response_id")
  private SurveyResponse surveyResponse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_info_source_id", nullable = false)
  private RefEmploymentInfoSource employmentInfoSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_verification_status_id", nullable = false)
  private RefRecordVerificationStatus recordVerificationStatus;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public TraineeUnemploymentEvent() {
  }

  public TraineeUnemploymentEvent(Trainee trainee, Integer periodNumber, LocalDate startDate,
      Long labourStatusId, RefUnemploymentReason unemploymentReason,
      RefEmploymentInfoSource employmentInfoSource,
      RefRecordVerificationStatus recordVerificationStatus) {
    this.trainee = trainee;
    this.periodNumber = periodNumber;
    this.startDate = startDate;
    this.labourStatusId = labourStatusId;
    this.unemploymentReason = unemploymentReason;
    this.employmentInfoSource = employmentInfoSource;
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public Integer getPeriodNumber() {
    return periodNumber;
  }

  public void setPeriodNumber(Integer periodNumber) {
    this.periodNumber = periodNumber;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public Integer getCurrentPeriodKey() {
    return currentPeriodKey;
  }

  public void setCurrentPeriodKey(Integer currentPeriodKey) {
    this.currentPeriodKey = currentPeriodKey;
  }

  public Long getLabourStatusId() {
    return labourStatusId;
  }

  public void setLabourStatusId(Long labourStatusId) {
    this.labourStatusId = labourStatusId;
  }

  public RefUnemploymentReason getUnemploymentReason() {
    return unemploymentReason;
  }

  public void setUnemploymentReason(RefUnemploymentReason unemploymentReason) {
    this.unemploymentReason = unemploymentReason;
  }

  public EmploymentRecord getPrecedingEmployment() {
    return precedingEmployment;
  }

  public void setPrecedingEmployment(EmploymentRecord precedingEmployment) {
    this.precedingEmployment = precedingEmployment;
  }

  public EmploymentExitEvent getEmploymentExitEvent() {
    return employmentExitEvent;
  }

  public void setEmploymentExitEvent(EmploymentExitEvent employmentExitEvent) {
    this.employmentExitEvent = employmentExitEvent;
  }

  public EmploymentRecord getSucceedingEmployment() {
    return succeedingEmployment;
  }

  public void setSucceedingEmployment(EmploymentRecord succeedingEmployment) {
    this.succeedingEmployment = succeedingEmployment;
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

  public FollowupTask getFollowupTask() {
    return followupTask;
  }

  public void setFollowupTask(FollowupTask followupTask) {
    this.followupTask = followupTask;
  }

  public SurveyResponse getSurveyResponse() {
    return surveyResponse;
  }

  public void setSurveyResponse(SurveyResponse surveyResponse) {
    this.surveyResponse = surveyResponse;
  }

  public RefEmploymentInfoSource getEmploymentInfoSource() {
    return employmentInfoSource;
  }

  public void setEmploymentInfoSource(RefEmploymentInfoSource employmentInfoSource) {
    this.employmentInfoSource = employmentInfoSource;
  }

  public RefRecordVerificationStatus getRecordVerificationStatus() {
    return recordVerificationStatus;
  }

  public void setRecordVerificationStatus(
      RefRecordVerificationStatus recordVerificationStatus) {
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
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
    if (!(o instanceof TraineeUnemploymentEvent that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TraineeUnemploymentEvent{" +
        "id=" + id +
        ", periodNumber=" + periodNumber +
        ", startDate=" + startDate +
        ", endDate=" + endDate +
        ", isCurrent=" + isCurrent +
        '}';
  }
}
