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
@Table(name = "employment_exit_events", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employment_exit_events_employment", columnNames = {"employment_id"})
})
public class EmploymentExitEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_exit_event_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_id", nullable = false, unique = true)
  private EmploymentRecord employmentRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @Column(name = "separation_date", nullable = false)
  private LocalDate separationDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_exit_reason_id", nullable = false)
  private RefEmploymentExitReason employmentExitReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "separation_nature_id", nullable = false)
  private RefSeparationNature separationNature;

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

  public EmploymentExitEvent() {
  }

  public EmploymentExitEvent(EmploymentRecord employmentRecord, Trainee trainee,
      LocalDate separationDate, RefEmploymentExitReason employmentExitReason,
      RefSeparationNature separationNature, RefEmploymentInfoSource employmentInfoSource,
      RefRecordVerificationStatus recordVerificationStatus) {
    this.employmentRecord = employmentRecord;
    this.trainee = trainee;
    this.separationDate = separationDate;
    this.employmentExitReason = employmentExitReason;
    this.separationNature = separationNature;
    this.employmentInfoSource = employmentInfoSource;
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public TrainingEnrollment getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(TrainingEnrollment enrollment) {
    this.enrollment = enrollment;
  }

  public LocalDate getSeparationDate() {
    return separationDate;
  }

  public void setSeparationDate(LocalDate separationDate) {
    this.separationDate = separationDate;
  }

  public RefEmploymentExitReason getEmploymentExitReason() {
    return employmentExitReason;
  }

  public void setEmploymentExitReason(RefEmploymentExitReason employmentExitReason) {
    this.employmentExitReason = employmentExitReason;
  }

  public RefSeparationNature getSeparationNature() {
    return separationNature;
  }

  public void setSeparationNature(RefSeparationNature separationNature) {
    this.separationNature = separationNature;
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
    if (!(o instanceof EmploymentExitEvent that)) {
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
    return "EmploymentExitEvent{" +
        "id=" + id +
        ", separationDate=" + separationDate +
        ", verifiedAt=" + verifiedAt +
        '}';
  }
}
