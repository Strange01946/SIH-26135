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
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employment_verification_requests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_emp_verif_requests_number", columnNames = {"request_number"}),
    @UniqueConstraint(name = "uk_emp_verif_requests_cycle", columnNames = {"employment_id", "cycle_number"}),
    @UniqueConstraint(name = "uk_emp_verif_requests_open", columnNames = {"employment_id", "open_request_key"})
})
public class EmploymentVerificationRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_request_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "request_number", length = 32, nullable = false, unique = true)
  private String requestNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_id", nullable = false)
  private EmploymentRecord employmentRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id")
  private PlacementRecord placementRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "cycle_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer cycleNumber = 1;

  @Column(name = "is_reverification", nullable = false)
  private Boolean isReverification = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_request_status_id", nullable = false)
  private RefEmploymentVerificationRequestStatus employmentVerificationRequestStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "preferred_verification_method_id")
  private RefEmploymentVerificationMethod preferredVerificationMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_info_source_id")
  private RefEmploymentInfoSource employmentInfoSource;

  @Column(name = "requested_by_user_id")
  private Long requestedByUserId;

  @Column(name = "assigned_verifier_user_id")
  private Long assignedVerifierUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_task_id")
  private FollowupTask followupTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_response_id")
  private SurveyResponse surveyResponse;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "due_at")
  private LocalDateTime dueAt;

  @Column(name = "first_attempt_at")
  private LocalDateTime firstAttemptAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "open_request_key", insertable = false, updatable = false)
  private Integer openRequestKey;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public EmploymentVerificationRequest() {
  }

  public EmploymentVerificationRequest(String requestNumber, EmploymentRecord employmentRecord,
      Trainee trainee, RefEmploymentVerificationRequestStatus employmentVerificationRequestStatus,
      LocalDateTime requestedAt) {
    this.requestNumber = requestNumber;
    this.employmentRecord = employmentRecord;
    this.trainee = trainee;
    this.employmentVerificationRequestStatus = employmentVerificationRequestStatus;
    this.requestedAt = requestedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRequestNumber() {
    return requestNumber;
  }

  public void setRequestNumber(String requestNumber) {
    this.requestNumber = requestNumber;
  }

  public EmploymentRecord getEmploymentRecord() {
    return employmentRecord;
  }

  public void setEmploymentRecord(EmploymentRecord employmentRecord) {
    this.employmentRecord = employmentRecord;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public PlacementRecord getPlacementRecord() {
    return placementRecord;
  }

  public void setPlacementRecord(PlacementRecord placementRecord) {
    this.placementRecord = placementRecord;
  }

  public Employer getEmployer() {
    return employer;
  }

  public void setEmployer(Employer employer) {
    this.employer = employer;
  }

  public Integer getCycleNumber() {
    return cycleNumber;
  }

  public void setCycleNumber(Integer cycleNumber) {
    this.cycleNumber = cycleNumber != null ? cycleNumber : 1;
  }

  public Boolean getIsReverification() {
    return isReverification;
  }

  public void setIsReverification(Boolean reverification) {
    isReverification = reverification != null ? reverification : false;
  }

  public RefEmploymentVerificationRequestStatus getEmploymentVerificationRequestStatus() {
    return employmentVerificationRequestStatus;
  }

  public void setEmploymentVerificationRequestStatus(RefEmploymentVerificationRequestStatus employmentVerificationRequestStatus) {
    this.employmentVerificationRequestStatus = employmentVerificationRequestStatus;
  }

  public RefEmploymentVerificationMethod getPreferredVerificationMethod() {
    return preferredVerificationMethod;
  }

  public void setPreferredVerificationMethod(RefEmploymentVerificationMethod preferredVerificationMethod) {
    this.preferredVerificationMethod = preferredVerificationMethod;
  }

  public RefEmploymentInfoSource getEmploymentInfoSource() {
    return employmentInfoSource;
  }

  public void setEmploymentInfoSource(RefEmploymentInfoSource employmentInfoSource) {
    this.employmentInfoSource = employmentInfoSource;
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

  public LocalDateTime getFirstAttemptAt() {
    return firstAttemptAt;
  }

  public void setFirstAttemptAt(LocalDateTime firstAttemptAt) {
    this.firstAttemptAt = firstAttemptAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public Integer getOpenRequestKey() {
    return openRequestKey;
  }

  public void setOpenRequestKey(Integer openRequestKey) {
    this.openRequestKey = openRequestKey;
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
    if (!(o instanceof EmploymentVerificationRequest that)) {
      return false;
    }
    return Objects.equals(requestNumber, that.requestNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestNumber);
  }

  @Override
  public String toString() {
    return "EmploymentVerificationRequest{" +
        "id=" + id +
        ", requestNumber='" + requestNumber + '\'' +
        ", cycleNumber=" + cycleNumber +
        ", isReverification=" + isReverification +
        ", requestedAt=" + requestedAt +
        '}';
  }
}
