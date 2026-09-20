package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_employment_exit_fact")
@Immutable
public class EmploymentExitFact {

  @Id
  @Column(name = "employment_exit_event_id", nullable = false)
  private Long employmentExitEventId;

  @Column(name = "employment_id", nullable = false)
  private Long employmentId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "separation_date", nullable = false)
  private LocalDate separationDate;

  @Column(name = "employment_exit_reason_id", nullable = false)
  private Long employmentExitReasonId;

  @Column(name = "exit_reason_code", length = 32, nullable = false)
  private String exitReasonCode;

  @Column(name = "separation_nature_id", nullable = false)
  private Long separationNatureId;

  @Column(name = "separation_nature_code", length = 32, nullable = false)
  private String separationNatureCode;

  @Column(name = "is_voluntary_flag", nullable = false)
  private Boolean isVoluntaryFlag;

  @Column(name = "is_involuntary_flag", nullable = false)
  private Boolean isInvoluntaryFlag;

  @Column(name = "employment_info_source_id", nullable = false)
  private Long employmentInfoSourceId;

  @Column(name = "info_source_code", length = 32, nullable = false)
  private String infoSourceCode;

  @Column(name = "record_verification_status_id", nullable = false)
  private Long recordVerificationStatusId;

  @Column(name = "record_verification_status_code", length = 32, nullable = false)
  private String recordVerificationStatusCode;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public EmploymentExitFact() {
  }

  public Long getEmploymentExitEventId() {
    return employmentExitEventId;
  }

  public void setEmploymentExitEventId(Long employmentExitEventId) {
    this.employmentExitEventId = employmentExitEventId;
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

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public LocalDate getSeparationDate() {
    return separationDate;
  }

  public void setSeparationDate(LocalDate separationDate) {
    this.separationDate = separationDate;
  }

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
  }

  public String getExitReasonCode() {
    return exitReasonCode;
  }

  public void setExitReasonCode(String exitReasonCode) {
    this.exitReasonCode = exitReasonCode;
  }

  public Long getSeparationNatureId() {
    return separationNatureId;
  }

  public void setSeparationNatureId(Long separationNatureId) {
    this.separationNatureId = separationNatureId;
  }

  public String getSeparationNatureCode() {
    return separationNatureCode;
  }

  public void setSeparationNatureCode(String separationNatureCode) {
    this.separationNatureCode = separationNatureCode;
  }

  public Boolean getIsVoluntaryFlag() {
    return isVoluntaryFlag;
  }

  public void setIsVoluntaryFlag(Boolean isVoluntaryFlag) {
    this.isVoluntaryFlag = isVoluntaryFlag;
  }

  public Boolean getIsInvoluntaryFlag() {
    return isInvoluntaryFlag;
  }

  public void setIsInvoluntaryFlag(Boolean isInvoluntaryFlag) {
    this.isInvoluntaryFlag = isInvoluntaryFlag;
  }

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public String getInfoSourceCode() {
    return infoSourceCode;
  }

  public void setInfoSourceCode(String infoSourceCode) {
    this.infoSourceCode = infoSourceCode;
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
    if (!(o instanceof EmploymentExitFact that)) {
      return false;
    }
    return Objects.equals(employmentExitEventId, that.employmentExitEventId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentExitEventId);
  }
}
