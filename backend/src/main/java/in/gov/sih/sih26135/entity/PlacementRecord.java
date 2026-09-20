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
@Table(name = "placement_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_placement_records_number", columnNames = {"placement_number"}),
    @UniqueConstraint(name = "uk_placement_records_application", columnNames = {"job_application_id"}),
    @UniqueConstraint(name = "uk_placement_records_enrollment_posting", columnNames = {"enrollment_id", "job_posting_id"})
})
public class PlacementRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "placement_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "placement_number", length = 32, nullable = false, unique = true)
  private String placementNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private TrainingEnrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private TrainingProvider trainingProvider;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_branch_id")
  private EmployerBranch employerBranch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_posting_id")
  private JobPosting jobPosting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_application_id", unique = true)
  private JobApplication jobApplication;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id")
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engagement_type_id", nullable = false)
  private RefEngagementType engagementType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_source_id", nullable = false)
  private RefPlacementSource placementSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_status_id", nullable = false)
  private RefPlacementStatus placementStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "joining_status_id", nullable = false)
  private RefJoiningStatus joiningStatus;

  @Column(name = "offered_salary", precision = 12, scale = 2)
  private BigDecimal offeredSalary;

  @Column(name = "joining_salary", precision = 12, scale = 2)
  private BigDecimal joiningSalary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salary_frequency_id")
  private RefSalaryFrequency salaryFrequency;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  private String currencyCode = "INR";

  @Column(name = "offer_date")
  private LocalDate offerDate;

  @Column(name = "expected_joining_date")
  private LocalDate expectedJoiningDate;

  @Column(name = "actual_joining_date")
  private LocalDate actualJoiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "non_selection_reason_id")
  private RefNonSelectionReason nonSelectionReason;

  @Column(name = "outcome_remarks", length = 500)
  private String outcomeRemarks;

  @Column(name = "work_state_id")
  private Long workStateId;

  @Column(name = "work_district_id")
  private Long workDistrictId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_verification_status_id", nullable = false)
  private RefRecordVerificationStatus recordVerificationStatus;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public PlacementRecord() {
  }

  public PlacementRecord(String placementNumber, Trainee trainee, TrainingEnrollment enrollment,
      Course course, Program program, TrainingProvider trainingProvider,
      RefEngagementType engagementType, RefPlacementSource placementSource,
      RefPlacementStatus placementStatus, RefJoiningStatus joiningStatus,
      RefRecordVerificationStatus recordVerificationStatus) {
    this.placementNumber = placementNumber;
    this.trainee = trainee;
    this.enrollment = enrollment;
    this.course = course;
    this.program = program;
    this.trainingProvider = trainingProvider;
    this.engagementType = engagementType;
    this.placementSource = placementSource;
    this.placementStatus = placementStatus;
    this.joiningStatus = joiningStatus;
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPlacementNumber() {
    return placementNumber;
  }

  public void setPlacementNumber(String placementNumber) {
    this.placementNumber = placementNumber;
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

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public TrainingProvider getTrainingProvider() {
    return trainingProvider;
  }

  public void setTrainingProvider(TrainingProvider trainingProvider) {
    this.trainingProvider = trainingProvider;
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

  public JobPosting getJobPosting() {
    return jobPosting;
  }

  public void setJobPosting(JobPosting jobPosting) {
    this.jobPosting = jobPosting;
  }

  public JobApplication getJobApplication() {
    return jobApplication;
  }

  public void setJobApplication(JobApplication jobApplication) {
    this.jobApplication = jobApplication;
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

  public RefPlacementSource getPlacementSource() {
    return placementSource;
  }

  public void setPlacementSource(RefPlacementSource placementSource) {
    this.placementSource = placementSource;
  }

  public RefPlacementStatus getPlacementStatus() {
    return placementStatus;
  }

  public void setPlacementStatus(RefPlacementStatus placementStatus) {
    this.placementStatus = placementStatus;
  }

  public RefJoiningStatus getJoiningStatus() {
    return joiningStatus;
  }

  public void setJoiningStatus(RefJoiningStatus joiningStatus) {
    this.joiningStatus = joiningStatus;
  }

  public BigDecimal getOfferedSalary() {
    return offeredSalary;
  }

  public void setOfferedSalary(BigDecimal offeredSalary) {
    this.offeredSalary = offeredSalary;
  }

  public BigDecimal getJoiningSalary() {
    return joiningSalary;
  }

  public void setJoiningSalary(BigDecimal joiningSalary) {
    this.joiningSalary = joiningSalary;
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

  public LocalDate getOfferDate() {
    return offerDate;
  }

  public void setOfferDate(LocalDate offerDate) {
    this.offerDate = offerDate;
  }

  public LocalDate getExpectedJoiningDate() {
    return expectedJoiningDate;
  }

  public void setExpectedJoiningDate(LocalDate expectedJoiningDate) {
    this.expectedJoiningDate = expectedJoiningDate;
  }

  public LocalDate getActualJoiningDate() {
    return actualJoiningDate;
  }

  public void setActualJoiningDate(LocalDate actualJoiningDate) {
    this.actualJoiningDate = actualJoiningDate;
  }

  public RefNonSelectionReason getNonSelectionReason() {
    return nonSelectionReason;
  }

  public void setNonSelectionReason(RefNonSelectionReason nonSelectionReason) {
    this.nonSelectionReason = nonSelectionReason;
  }

  public String getOutcomeRemarks() {
    return outcomeRemarks;
  }

  public void setOutcomeRemarks(String outcomeRemarks) {
    this.outcomeRemarks = outcomeRemarks;
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
    if (!(o instanceof PlacementRecord that)) {
      return false;
    }
    return Objects.equals(placementNumber, that.placementNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(placementNumber);
  }

  @Override
  public String toString() {
    return "PlacementRecord{" +
        "id=" + id +
        ", placementNumber='" + placementNumber + '\'' +
        ", currencyCode='" + currencyCode + '\'' +
        '}';
  }
}
