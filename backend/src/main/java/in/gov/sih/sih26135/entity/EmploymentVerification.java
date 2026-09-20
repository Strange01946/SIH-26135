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
@Table(name = "employment_verifications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_emp_verifications_number", columnNames = {"verification_number"}),
    @UniqueConstraint(name = "uk_emp_verifications_request", columnNames = {"employment_verification_request_id"}),
    @UniqueConstraint(name = "uk_emp_verifications_cycle", columnNames = {"employment_id", "cycle_number"}),
    @UniqueConstraint(name = "uk_emp_verifications_current", columnNames = {"employment_id", "current_verification_key"})
})
public class EmploymentVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "verification_number", length = 32, nullable = false, unique = true)
  private String verificationNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_request_id", unique = true)
  private EmploymentVerificationRequest employmentVerificationRequest;

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

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent = true;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "current_verification_key", insertable = false, updatable = false)
  private Integer currentVerificationKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_verification_status_id", nullable = false)
  private RefRecordVerificationStatus recordVerificationStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_method_id", nullable = false)
  private RefEmploymentVerificationMethod employmentVerificationMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_info_source_id")
  private RefEmploymentInfoSource employmentInfoSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_rejection_reason_id")
  private RefEmploymentVerificationRejectionReason employmentVerificationRejectionReason;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "employer_respondent_user_id")
  private Long employerRespondentUserId;

  @Column(name = "trainee_attested_flag", nullable = false)
  private Boolean traineeAttestedFlag = false;

  @Column(name = "employer_confirmed_flag", nullable = false)
  private Boolean employerConfirmedFlag = false;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "outcome_recorded_at", nullable = false)
  private LocalDateTime outcomeRecordedAt;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "rejection_notes", length = 500)
  private String rejectionNotes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public EmploymentVerification() {
  }

  public EmploymentVerification(String verificationNumber, EmploymentRecord employmentRecord,
      Trainee trainee, RefRecordVerificationStatus recordVerificationStatus,
      RefEmploymentVerificationMethod employmentVerificationMethod, LocalDateTime requestedAt,
      LocalDateTime outcomeRecordedAt) {
    this.verificationNumber = verificationNumber;
    this.employmentRecord = employmentRecord;
    this.trainee = trainee;
    this.recordVerificationStatus = recordVerificationStatus;
    this.employmentVerificationMethod = employmentVerificationMethod;
    this.requestedAt = requestedAt;
    this.outcomeRecordedAt = outcomeRecordedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getVerificationNumber() {
    return verificationNumber;
  }

  public void setVerificationNumber(String verificationNumber) {
    this.verificationNumber = verificationNumber;
  }

  public EmploymentVerificationRequest getEmploymentVerificationRequest() {
    return employmentVerificationRequest;
  }

  public void setEmploymentVerificationRequest(
      EmploymentVerificationRequest employmentVerificationRequest) {
    this.employmentVerificationRequest = employmentVerificationRequest;
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
    this.cycleNumber = cycleNumber;
  }

  public Boolean getIsReverification() {
    return isReverification;
  }

  public void setIsReverification(Boolean isReverification) {
    this.isReverification = isReverification;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public Integer getCurrentVerificationKey() {
    return currentVerificationKey;
  }

  public void setCurrentVerificationKey(Integer currentVerificationKey) {
    this.currentVerificationKey = currentVerificationKey;
  }

  public RefRecordVerificationStatus getRecordVerificationStatus() {
    return recordVerificationStatus;
  }

  public void setRecordVerificationStatus(
      RefRecordVerificationStatus recordVerificationStatus) {
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public RefEmploymentVerificationMethod getEmploymentVerificationMethod() {
    return employmentVerificationMethod;
  }

  public void setEmploymentVerificationMethod(
      RefEmploymentVerificationMethod employmentVerificationMethod) {
    this.employmentVerificationMethod = employmentVerificationMethod;
  }

  public RefEmploymentInfoSource getEmploymentInfoSource() {
    return employmentInfoSource;
  }

  public void setEmploymentInfoSource(RefEmploymentInfoSource employmentInfoSource) {
    this.employmentInfoSource = employmentInfoSource;
  }

  public RefEmploymentVerificationRejectionReason getEmploymentVerificationRejectionReason() {
    return employmentVerificationRejectionReason;
  }

  public void setEmploymentVerificationRejectionReason(
      RefEmploymentVerificationRejectionReason employmentVerificationRejectionReason) {
    this.employmentVerificationRejectionReason = employmentVerificationRejectionReason;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }

  public Long getEmployerRespondentUserId() {
    return employerRespondentUserId;
  }

  public void setEmployerRespondentUserId(Long employerRespondentUserId) {
    this.employerRespondentUserId = employerRespondentUserId;
  }

  public Boolean getTraineeAttestedFlag() {
    return traineeAttestedFlag;
  }

  public void setTraineeAttestedFlag(Boolean traineeAttestedFlag) {
    this.traineeAttestedFlag = traineeAttestedFlag;
  }

  public Boolean getEmployerConfirmedFlag() {
    return employerConfirmedFlag;
  }

  public void setEmployerConfirmedFlag(Boolean employerConfirmedFlag) {
    this.employerConfirmedFlag = employerConfirmedFlag;
  }

  public LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(LocalDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public LocalDateTime getOutcomeRecordedAt() {
    return outcomeRecordedAt;
  }

  public void setOutcomeRecordedAt(LocalDateTime outcomeRecordedAt) {
    this.outcomeRecordedAt = outcomeRecordedAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getRejectionNotes() {
    return rejectionNotes;
  }

  public void setRejectionNotes(String rejectionNotes) {
    this.rejectionNotes = rejectionNotes;
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
    if (!(o instanceof EmploymentVerification that)) {
      return false;
    }
    return Objects.equals(verificationNumber, that.verificationNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(verificationNumber);
  }

  @Override
  public String toString() {
    return "EmploymentVerification{" +
        "id=" + id +
        ", verificationNumber='" + verificationNumber + '\'' +
        ", cycleNumber=" + cycleNumber +
        ", isReverification=" + isReverification +
        ", isCurrent=" + isCurrent +
        ", requestedAt=" + requestedAt +
        ", outcomeRecordedAt=" + outcomeRecordedAt +
        '}';
  }
}
