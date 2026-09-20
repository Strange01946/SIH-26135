package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_enrollment_outcome_fact")
@Immutable
public class EnrollmentOutcomeFact {

  @Id
  @Column(name = "enrollment_id", nullable = false)
  private Long enrollmentId;

  @Column(name = "enrollment_number", length = 32, nullable = false)
  private String enrollmentNumber;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "program_id", nullable = false)
  private Long programId;

  @Column(name = "scheme_id", nullable = false)
  private Long schemeId;

  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Column(name = "provider_id", nullable = false)
  private Long providerId;

  @Column(name = "center_id", nullable = false)
  private Long centerId;

  @Column(name = "batch_id", nullable = false)
  private Long batchId;

  @Column(name = "enrollment_date", nullable = false)
  private LocalDate enrollmentDate;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "actual_completion_date")
  private LocalDate actualCompletionDate;

  @Column(name = "enrollment_status_id", nullable = false)
  private Long enrollmentStatusId;

  @Column(name = "enrollment_status_code", length = 32, nullable = false)
  private String enrollmentStatusCode;

  @Column(name = "is_completed_flag", nullable = false)
  private Boolean isCompletedFlag;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  @Column(name = "issued_certificate_count")
  private Long issuedCertificateCount;

  @Column(name = "has_issued_certificate_flag")
  private Integer hasIssuedCertificateFlag;

  @Column(name = "placement_count")
  private Long placementCount;

  @Column(name = "joined_placement_count")
  private BigDecimal joinedPlacementCount;

  @Column(name = "has_joined_placement_flag")
  private Integer hasJoinedPlacementFlag;

  @Column(name = "employment_count")
  private Long employmentCount;

  @Column(name = "current_employment_count")
  private BigDecimal currentEmploymentCount;

  @Column(name = "wage_employment_count")
  private BigDecimal wageEmploymentCount;

  @Column(name = "self_employment_count")
  private BigDecimal selfEmploymentCount;

  @Column(name = "apprenticeship_count")
  private BigDecimal apprenticeshipCount;

  @Column(name = "has_employment_flag")
  private Integer hasEmploymentFlag;

  public EnrollmentOutcomeFact() {
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public String getEnrollmentNumber() {
    return enrollmentNumber;
  }

  public void setEnrollmentNumber(String enrollmentNumber) {
    this.enrollmentNumber = enrollmentNumber;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(Long schemeId) {
    this.schemeId = schemeId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public Long getCenterId() {
    return centerId;
  }

  public void setCenterId(Long centerId) {
    this.centerId = centerId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public LocalDate getEnrollmentDate() {
    return enrollmentDate;
  }

  public void setEnrollmentDate(LocalDate enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getActualCompletionDate() {
    return actualCompletionDate;
  }

  public void setActualCompletionDate(LocalDate actualCompletionDate) {
    this.actualCompletionDate = actualCompletionDate;
  }

  public Long getEnrollmentStatusId() {
    return enrollmentStatusId;
  }

  public void setEnrollmentStatusId(Long enrollmentStatusId) {
    this.enrollmentStatusId = enrollmentStatusId;
  }

  public String getEnrollmentStatusCode() {
    return enrollmentStatusCode;
  }

  public void setEnrollmentStatusCode(String enrollmentStatusCode) {
    this.enrollmentStatusCode = enrollmentStatusCode;
  }

  public Boolean getIsCompletedFlag() {
    return isCompletedFlag;
  }

  public void setIsCompletedFlag(Boolean isCompletedFlag) {
    this.isCompletedFlag = isCompletedFlag;
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

  public Long getIssuedCertificateCount() {
    return issuedCertificateCount;
  }

  public void setIssuedCertificateCount(Long issuedCertificateCount) {
    this.issuedCertificateCount = issuedCertificateCount;
  }

  public Integer getHasIssuedCertificateFlag() {
    return hasIssuedCertificateFlag;
  }

  public void setHasIssuedCertificateFlag(Integer hasIssuedCertificateFlag) {
    this.hasIssuedCertificateFlag = hasIssuedCertificateFlag;
  }

  public Long getPlacementCount() {
    return placementCount;
  }

  public void setPlacementCount(Long placementCount) {
    this.placementCount = placementCount;
  }

  public BigDecimal getJoinedPlacementCount() {
    return joinedPlacementCount;
  }

  public void setJoinedPlacementCount(BigDecimal joinedPlacementCount) {
    this.joinedPlacementCount = joinedPlacementCount;
  }

  public Integer getHasJoinedPlacementFlag() {
    return hasJoinedPlacementFlag;
  }

  public void setHasJoinedPlacementFlag(Integer hasJoinedPlacementFlag) {
    this.hasJoinedPlacementFlag = hasJoinedPlacementFlag;
  }

  public Long getEmploymentCount() {
    return employmentCount;
  }

  public void setEmploymentCount(Long employmentCount) {
    this.employmentCount = employmentCount;
  }

  public BigDecimal getCurrentEmploymentCount() {
    return currentEmploymentCount;
  }

  public void setCurrentEmploymentCount(BigDecimal currentEmploymentCount) {
    this.currentEmploymentCount = currentEmploymentCount;
  }

  public BigDecimal getWageEmploymentCount() {
    return wageEmploymentCount;
  }

  public void setWageEmploymentCount(BigDecimal wageEmploymentCount) {
    this.wageEmploymentCount = wageEmploymentCount;
  }

  public BigDecimal getSelfEmploymentCount() {
    return selfEmploymentCount;
  }

  public void setSelfEmploymentCount(BigDecimal selfEmploymentCount) {
    this.selfEmploymentCount = selfEmploymentCount;
  }

  public BigDecimal getApprenticeshipCount() {
    return apprenticeshipCount;
  }

  public void setApprenticeshipCount(BigDecimal apprenticeshipCount) {
    this.apprenticeshipCount = apprenticeshipCount;
  }

  public Integer getHasEmploymentFlag() {
    return hasEmploymentFlag;
  }

  public void setHasEmploymentFlag(Integer hasEmploymentFlag) {
    this.hasEmploymentFlag = hasEmploymentFlag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EnrollmentOutcomeFact that)) {
      return false;
    }
    return Objects.equals(enrollmentId, that.enrollmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enrollmentId);
  }
}
