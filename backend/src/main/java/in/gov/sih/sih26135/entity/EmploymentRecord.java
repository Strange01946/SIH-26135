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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employment_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employment_records_number", columnNames = {"employment_number"}),
    @UniqueConstraint(name = "uk_employment_records_placement", columnNames = {"placement_id"}),
    @UniqueConstraint(name = "uk_employment_records_trainee_employer_start", columnNames = {"trainee_id", "employer_id", "start_date"}),
    @UniqueConstraint(name = "uk_employment_records_current_per_type", columnNames = {"trainee_id", "engagement_type_id", "current_spell_key"})
})
public class EmploymentRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "employment_number", length = 32, nullable = false, unique = true)
  private String employmentNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id")
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id", unique = true)
  private PlacementRecord placementRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_branch_id")
  private EmployerBranch employerBranch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id")
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engagement_type_id", nullable = false)
  private RefEngagementType engagementType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_spell_status_id", nullable = false)
  private RefEmploymentSpellStatus employmentSpellStatus;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent = true;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "current_spell_key", insertable = false, updatable = false)
  private Integer currentSpellKey;

  @Column(name = "starting_salary", precision = 12, scale = 2)
  private BigDecimal startingSalary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salary_frequency_id")
  private RefSalaryFrequency salaryFrequency;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  private String currencyCode = "INR";

  @Column(name = "work_location_id")
  private Long workLocationId;

  @Column(name = "work_state_id")
  private Long workStateId;

  @Column(name = "work_district_id")
  private Long workDistrictId;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_exit_reason_id")
  private RefEmploymentExitReason employmentExitReason;

  @Column(name = "exit_remarks", length = 500)
  private String exitRemarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public EmploymentRecord() {
  }

  public EmploymentRecord(String employmentNumber, Trainee trainee, RefEngagementType engagementType,
      RefEmploymentSpellStatus employmentSpellStatus, LocalDate startDate,
      RefEmploymentInfoSource employmentInfoSource, RefRecordVerificationStatus recordVerificationStatus) {
    this.employmentNumber = employmentNumber;
    this.trainee = trainee;
    this.engagementType = engagementType;
    this.employmentSpellStatus = employmentSpellStatus;
    this.startDate = startDate;
    this.employmentInfoSource = employmentInfoSource;
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmploymentNumber() {
    return employmentNumber;
  }

  public void setEmploymentNumber(String employmentNumber) {
    this.employmentNumber = employmentNumber;
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

  public Employer getEmployer() {
    return employer;
  }

  public void setEmployer(Employer employer) {
    this.employer = employer;
  }

  public EmployerBranch getEmployerBranch() {
    return employerBranch;
  }

  public void setEmployerBranch(EmployerBranch employerBranch) {
    this.employerBranch = employerBranch;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public void setJobRole(JobRole jobRole) {
    this.jobRole = jobRole;
  }

  public RefEngagementType getEngagementType() {
    return engagementType;
  }

  public void setEngagementType(RefEngagementType engagementType) {
    this.engagementType = engagementType;
  }

  public RefEmploymentSpellStatus getEmploymentSpellStatus() {
    return employmentSpellStatus;
  }

  public void setEmploymentSpellStatus(RefEmploymentSpellStatus employmentSpellStatus) {
    this.employmentSpellStatus = employmentSpellStatus;
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

  public void setIsCurrent(Boolean current) {
    isCurrent = current != null ? current : true;
  }

  public Integer getCurrentSpellKey() {
    return currentSpellKey;
  }

  public void setCurrentSpellKey(Integer currentSpellKey) {
    this.currentSpellKey = currentSpellKey;
  }

  public BigDecimal getStartingSalary() {
    return startingSalary;
  }

  public void setStartingSalary(BigDecimal startingSalary) {
    this.startingSalary = startingSalary;
  }

  public RefSalaryFrequency getSalaryFrequency() {
    return salaryFrequency;
  }

  public void setSalaryFrequency(RefSalaryFrequency salaryFrequency) {
    this.salaryFrequency = salaryFrequency;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public Long getWorkLocationId() {
    return workLocationId;
  }

  public void setWorkLocationId(Long workLocationId) {
    this.workLocationId = workLocationId;
  }

  public Long getWorkStateId() {
    return workStateId;
  }

  public void setWorkStateId(Long workStateId) {
    this.workStateId = workStateId;
  }

  public Long getWorkDistrictId() {
    return workDistrictId;
  }

  public void setWorkDistrictId(Long workDistrictId) {
    this.workDistrictId = workDistrictId;
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

  public void setRecordVerificationStatus(RefRecordVerificationStatus recordVerificationStatus) {
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

  public RefEmploymentExitReason getEmploymentExitReason() {
    return employmentExitReason;
  }

  public void setEmploymentExitReason(RefEmploymentExitReason employmentExitReason) {
    this.employmentExitReason = employmentExitReason;
  }

  public String getExitRemarks() {
    return exitRemarks;
  }

  public void setExitRemarks(String exitRemarks) {
    this.exitRemarks = exitRemarks;
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
    if (!(o instanceof EmploymentRecord that)) {
      return false;
    }
    return Objects.equals(employmentNumber, that.employmentNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentNumber);
  }

  @Override
  public String toString() {
    return "EmploymentRecord{" +
        "id=" + id +
        ", employmentNumber='" + employmentNumber + '\'' +
        ", startDate=" + startDate +
        ", isCurrent=" + isCurrent +
        '}';
  }
}
