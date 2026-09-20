package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_employment_verification_fact")
@Immutable
public class EmploymentVerificationFact {

  @Id
  @Column(name = "employment_verification_id", nullable = false)
  private Long employmentVerificationId;

  @Column(name = "verification_number", length = 32, nullable = false)
  private String verificationNumber;

  @Column(name = "employment_id", nullable = false)
  private Long employmentId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "employer_id")
  private Long employerId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "cycle_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer cycleNumber;

  @Column(name = "is_reverification", nullable = false)
  private Boolean isReverification;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "record_verification_status_id", nullable = false)
  private Long recordVerificationStatusId;

  @Column(name = "record_verification_status_code", length = 32, nullable = false)
  private String recordVerificationStatusCode;

  @Column(name = "is_verified_flag", nullable = false)
  private Boolean isVerifiedFlag;

  @Column(name = "employment_verification_method_id", nullable = false)
  private Long employmentVerificationMethodId;

  @Column(name = "verification_method_code", length = 32, nullable = false)
  private String verificationMethodCode;

  @Column(name = "is_trainee_self_reported", nullable = false)
  private Boolean isTraineeSelfReported;

  @Column(name = "is_official_flag", nullable = false)
  private Boolean isOfficialFlag;

  @Column(name = "employment_info_source_id", nullable = false)
  private Long employmentInfoSourceId;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public EmploymentVerificationFact() {
  }

  public Long getEmploymentVerificationId() {
    return employmentVerificationId;
  }

  public void setEmploymentVerificationId(Long employmentVerificationId) {
    this.employmentVerificationId = employmentVerificationId;
  }

  public String getVerificationNumber() {
    return verificationNumber;
  }

  public void setVerificationNumber(String verificationNumber) {
    this.verificationNumber = verificationNumber;
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
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

  public Long getEmploymentVerificationMethodId() {
    return employmentVerificationMethodId;
  }

  public void setEmploymentVerificationMethodId(Long employmentVerificationMethodId) {
    this.employmentVerificationMethodId = employmentVerificationMethodId;
  }

  public String getVerificationMethodCode() {
    return verificationMethodCode;
  }

  public void setVerificationMethodCode(String verificationMethodCode) {
    this.verificationMethodCode = verificationMethodCode;
  }

  public Boolean getIsTraineeSelfReported() {
    return isTraineeSelfReported;
  }

  public void setIsTraineeSelfReported(Boolean isTraineeSelfReported) {
    this.isTraineeSelfReported = isTraineeSelfReported;
  }

  public Boolean getIsOfficialFlag() {
    return isOfficialFlag;
  }

  public void setIsOfficialFlag(Boolean isOfficialFlag) {
    this.isOfficialFlag = isOfficialFlag;
  }

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
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
    if (!(o instanceof EmploymentVerificationFact that)) {
      return false;
    }
    return Objects.equals(employmentVerificationId, that.employmentVerificationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentVerificationId);
  }
}
