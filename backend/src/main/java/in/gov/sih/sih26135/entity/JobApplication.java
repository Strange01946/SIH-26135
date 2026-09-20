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
@Table(name = "job_applications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_applications_trainee_posting", columnNames = {"trainee_id", "job_posting_id"})
})
public class JobApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "job_application_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_posting_id", nullable = false)
  private JobPosting jobPosting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_status_id", nullable = false)
  private RefApplicationStatus applicationStatus;

  @Column(name = "applied_date", nullable = false)
  private LocalDate appliedDate;

  @Column(name = "referred_by_user_id")
  private Long referredByUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "non_selection_reason_id")
  private RefNonSelectionReason nonSelectionReason;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public JobApplication() {
  }

  public JobApplication(Trainee trainee, JobPosting jobPosting, RefApplicationStatus applicationStatus,
      LocalDate appliedDate) {
    this.trainee = trainee;
    this.jobPosting = jobPosting;
    this.applicationStatus = applicationStatus;
    this.appliedDate = appliedDate;
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

  public TrainingEnrollment getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(TrainingEnrollment enrollment) {
    this.enrollment = enrollment;
  }

  public JobPosting getJobPosting() {
    return jobPosting;
  }

  public void setJobPosting(JobPosting jobPosting) {
    this.jobPosting = jobPosting;
  }

  public RefApplicationStatus getApplicationStatus() {
    return applicationStatus;
  }

  public void setApplicationStatus(RefApplicationStatus applicationStatus) {
    this.applicationStatus = applicationStatus;
  }

  public LocalDate getAppliedDate() {
    return appliedDate;
  }

  public void setAppliedDate(LocalDate appliedDate) {
    this.appliedDate = appliedDate;
  }

  public Long getReferredByUserId() {
    return referredByUserId;
  }

  public void setReferredByUserId(Long referredByUserId) {
    this.referredByUserId = referredByUserId;
  }

  public RefNonSelectionReason getNonSelectionReason() {
    return nonSelectionReason;
  }

  public void setNonSelectionReason(RefNonSelectionReason nonSelectionReason) {
    this.nonSelectionReason = nonSelectionReason;
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
    if (!(o instanceof JobApplication that)) {
      return false;
    }
    return Objects.equals(trainee != null ? trainee.getId() : null, that.trainee != null ? that.trainee.getId() : null)
        && Objects.equals(jobPosting != null ? jobPosting.getId() : null, that.jobPosting != null ? that.jobPosting.getId() : null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trainee != null ? trainee.getId() : null, jobPosting != null ? jobPosting.getId() : null);
  }

  @Override
  public String toString() {
    return "JobApplication{" +
        "id=" + id +
        ", appliedDate=" + appliedDate +
        '}';
  }
}
