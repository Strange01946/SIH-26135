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
@Table(name = "vw_placement_fact")
@Immutable
public class PlacementFact {

  @Id
  @Column(name = "placement_id", nullable = false)
  private Long placementId;

  @Column(name = "placement_number", length = 32, nullable = false)
  private String placementNumber;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id", nullable = false)
  private Long enrollmentId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "program_id", nullable = false)
  private Long programId;

  @Column(name = "provider_id", nullable = false)
  private Long providerId;

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

  @Column(name = "placement_source_id", nullable = false)
  private Long placementSourceId;

  @Column(name = "placement_source_code", length = 32, nullable = false)
  private String placementSourceCode;

  @Column(name = "placement_status_id", nullable = false)
  private Long placementStatusId;

  @Column(name = "placement_status_code", length = 32, nullable = false)
  private String placementStatusCode;

  @Column(name = "placement_status_joined_flag", nullable = false)
  private Boolean placementStatusJoinedFlag;

  @Column(name = "is_unsuccessful_flag", nullable = false)
  private Boolean isUnsuccessfulFlag;

  @Column(name = "joining_status_id", nullable = false)
  private Long joiningStatusId;

  @Column(name = "joining_status_code", length = 32, nullable = false)
  private String joiningStatusCode;

  @Column(name = "is_joined_flag", nullable = false)
  private Boolean isJoinedFlag;

  @Column(name = "joined_flag")
  private Integer joinedFlag;

  @Column(name = "offered_salary", precision = 12, scale = 2)
  private BigDecimal offeredSalary;

  @Column(name = "joining_salary", precision = 12, scale = 2)
  private BigDecimal joiningSalary;

  @Column(name = "salary_frequency_id")
  private Long salaryFrequencyId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
  private String currencyCode;

  @Column(name = "offer_date")
  private LocalDate offerDate;

  @Column(name = "actual_joining_date")
  private LocalDate actualJoiningDate;

  @Column(name = "work_state_id")
  private Long workStateId;

  @Column(name = "work_district_id")
  private Long workDistrictId;

  @Column(name = "record_verification_status_id", nullable = false)
  private Long recordVerificationStatusId;

  @Column(name = "record_verification_status_code", length = 32, nullable = false)
  private String recordVerificationStatusCode;

  @Column(name = "is_verified_flag", nullable = false)
  private Boolean isVerifiedFlag;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public PlacementFact() {
  }

  public Long getPlacementId() {
    return placementId;
  }

  public void setPlacementId(Long placementId) {
    this.placementId = placementId;
  }

  public String getPlacementNumber() {
    return placementNumber;
  }

  public void setPlacementNumber(String placementNumber) {
    this.placementNumber = placementNumber;
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

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
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

  public Long getPlacementSourceId() {
    return placementSourceId;
  }

  public void setPlacementSourceId(Long placementSourceId) {
    this.placementSourceId = placementSourceId;
  }

  public String getPlacementSourceCode() {
    return placementSourceCode;
  }

  public void setPlacementSourceCode(String placementSourceCode) {
    this.placementSourceCode = placementSourceCode;
  }

  public Long getPlacementStatusId() {
    return placementStatusId;
  }

  public void setPlacementStatusId(Long placementStatusId) {
    this.placementStatusId = placementStatusId;
  }

  public String getPlacementStatusCode() {
    return placementStatusCode;
  }

  public void setPlacementStatusCode(String placementStatusCode) {
    this.placementStatusCode = placementStatusCode;
  }

  public Boolean getPlacementStatusJoinedFlag() {
    return placementStatusJoinedFlag;
  }

  public void setPlacementStatusJoinedFlag(Boolean placementStatusJoinedFlag) {
    this.placementStatusJoinedFlag = placementStatusJoinedFlag;
  }

  public Boolean getIsUnsuccessfulFlag() {
    return isUnsuccessfulFlag;
  }

  public void setIsUnsuccessfulFlag(Boolean isUnsuccessfulFlag) {
    this.isUnsuccessfulFlag = isUnsuccessfulFlag;
  }

  public Long getJoiningStatusId() {
    return joiningStatusId;
  }

  public void setJoiningStatusId(Long joiningStatusId) {
    this.joiningStatusId = joiningStatusId;
  }

  public String getJoiningStatusCode() {
    return joiningStatusCode;
  }

  public void setJoiningStatusCode(String joiningStatusCode) {
    this.joiningStatusCode = joiningStatusCode;
  }

  public Boolean getIsJoinedFlag() {
    return isJoinedFlag;
  }

  public void setIsJoinedFlag(Boolean isJoinedFlag) {
    this.isJoinedFlag = isJoinedFlag;
  }

  public Integer getJoinedFlag() {
    return joinedFlag;
  }

  public void setJoinedFlag(Integer joinedFlag) {
    this.joinedFlag = joinedFlag;
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

  public Long getSalaryFrequencyId() {
    return salaryFrequencyId;
  }

  public void setSalaryFrequencyId(Long salaryFrequencyId) {
    this.salaryFrequencyId = salaryFrequencyId;
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

  public LocalDate getActualJoiningDate() {
    return actualJoiningDate;
  }

  public void setActualJoiningDate(LocalDate actualJoiningDate) {
    this.actualJoiningDate = actualJoiningDate;
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
    if (!(o instanceof PlacementFact that)) {
      return false;
    }
    return Objects.equals(placementId, that.placementId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(placementId);
  }
}
