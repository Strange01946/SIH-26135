package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_employment_fact")
@Immutable
public class EmploymentFact {

  @Id
  @Column(name = "employment_id", nullable = false)
  private Long employmentId;

  @Column(name = "employment_number", length = 32, nullable = false)
  private String employmentNumber;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "employer_id")
  private Long employerId;

  @Column(name = "job_role_id")
  private Long jobRoleId;

  @Column(name = "engagement_type_id", nullable = false)
  private Long engagementTypeId;

  @Column(name = "engagement_type_code", length = 32, nullable = false)
  private String engagementTypeCode;

  @Column(name = "is_wage_employment", nullable = false)
  private Boolean isWageEmployment;

  @Column(name = "is_self_employment", nullable = false)
  private Boolean isSelfEmployment;

  @Column(name = "is_apprenticeship", nullable = false)
  private Boolean isApprenticeship;

  @Column(name = "is_internship", nullable = false)
  private Boolean isInternship;

  @Column(name = "employment_spell_status_id", nullable = false)
  private Long employmentSpellStatusId;

  @Column(name = "employment_spell_status_code", length = 32, nullable = false)
  private String employmentSpellStatusCode;

  @Column(name = "is_active_flag", nullable = false)
  private Boolean isActiveFlag;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "duration_days")
  private Integer durationDays;

  @Column(name = "starting_salary", precision = 12, scale = 2)
  private BigDecimal startingSalary;

  @Column(name = "salary_frequency_id")
  private Long salaryFrequencyId;

  @Column(name = "salary_frequency_code", length = 32)
  private String salaryFrequencyCode;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
  private String currencyCode;

  @Column(name = "work_state_id")
  private Long workStateId;

  @Column(name = "work_district_id")
  private Long workDistrictId;

  @Column(name = "employment_info_source_id", nullable = false)
  private Long employmentInfoSourceId;

  @Column(name = "employment_info_source_code", length = 32, nullable = false)
  private String employmentInfoSourceCode;

  @Column(name = "is_self_reported_flag", nullable = false)
  private Boolean isSelfReportedFlag;

  @Column(name = "record_verification_status_id", nullable = false)
  private Long recordVerificationStatusId;

  @Column(name = "record_verification_status_code", length = 32, nullable = false)
  private String recordVerificationStatusCode;

  @Column(name = "is_verified_flag", nullable = false)
  private Boolean isVerifiedFlag;

  @Column(name = "employment_exit_reason_id")
  private Long employmentExitReasonId;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public EmploymentFact() {
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
  }

  public String getEmploymentNumber() {
    return employmentNumber;
  }

  public void setEmploymentNumber(String employmentNumber) {
    this.employmentNumber = employmentNumber;
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

  public Long getEmployerId() {
    return employerId;
  }

  public void setEmployerId(Long employerId) {
    this.employerId = employerId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getEngagementTypeId() {
    return engagementTypeId;
  }

  public void setEngagementTypeId(Long engagementTypeId) {
    this.engagementTypeId = engagementTypeId;
  }

  public String getEngagementTypeCode() {
    return engagementTypeCode;
  }

  public void setEngagementTypeCode(String engagementTypeCode) {
    this.engagementTypeCode = engagementTypeCode;
  }

  public Boolean getIsWageEmployment() {
    return isWageEmployment;
  }

  public void setIsWageEmployment(Boolean isWageEmployment) {
    this.isWageEmployment = isWageEmployment;
  }

  public Boolean getIsSelfEmployment() {
    return isSelfEmployment;
  }

  public void setIsSelfEmployment(Boolean isSelfEmployment) {
    this.isSelfEmployment = isSelfEmployment;
  }

  public Boolean getIsApprenticeship() {
    return isApprenticeship;
  }

  public void setIsApprenticeship(Boolean isApprenticeship) {
    this.isApprenticeship = isApprenticeship;
  }

  public Boolean getIsInternship() {
    return isInternship;
  }

  public void setIsInternship(Boolean isInternship) {
    this.isInternship = isInternship;
  }

  public Long getEmploymentSpellStatusId() {
    return employmentSpellStatusId;
  }

  public void setEmploymentSpellStatusId(Long employmentSpellStatusId) {
    this.employmentSpellStatusId = employmentSpellStatusId;
  }

  public String getEmploymentSpellStatusCode() {
    return employmentSpellStatusCode;
  }

  public void setEmploymentSpellStatusCode(String employmentSpellStatusCode) {
    this.employmentSpellStatusCode = employmentSpellStatusCode;
  }

  public Boolean getIsActiveFlag() {
    return isActiveFlag;
  }

  public void setIsActiveFlag(Boolean isActiveFlag) {
    this.isActiveFlag = isActiveFlag;
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

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
  }

  public BigDecimal getStartingSalary() {
    return startingSalary;
  }

  public void setStartingSalary(BigDecimal startingSalary) {
    this.startingSalary = startingSalary;
  }

  public Long getSalaryFrequencyId() {
    return salaryFrequencyId;
  }

  public void setSalaryFrequencyId(Long salaryFrequencyId) {
    this.salaryFrequencyId = salaryFrequencyId;
  }

  public String getSalaryFrequencyCode() {
    return salaryFrequencyCode;
  }

  public void setSalaryFrequencyCode(String salaryFrequencyCode) {
    this.salaryFrequencyCode = salaryFrequencyCode;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
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

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public String getEmploymentInfoSourceCode() {
    return employmentInfoSourceCode;
  }

  public void setEmploymentInfoSourceCode(String employmentInfoSourceCode) {
    this.employmentInfoSourceCode = employmentInfoSourceCode;
  }

  public Boolean getIsSelfReportedFlag() {
    return isSelfReportedFlag;
  }

  public void setIsSelfReportedFlag(Boolean isSelfReportedFlag) {
    this.isSelfReportedFlag = isSelfReportedFlag;
  }

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
  }

  public String getRecordVerificationStatusCode() {
    return recordVerificationStatusCode;
  }

  public void setRecordVerificationStatusCode(String recordVerificationStatusCode) {
    this.recordVerificationStatusCode = recordVerificationStatusCode;
  }

  public Boolean getIsVerifiedFlag() {
    return isVerifiedFlag;
  }

  public void setIsVerifiedFlag(Boolean isVerifiedFlag) {
    this.isVerifiedFlag = isVerifiedFlag;
  }

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
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
    if (!(o instanceof EmploymentFact that)) {
      return false;
    }
    return Objects.equals(employmentId, that.employmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentId);
  }
}
