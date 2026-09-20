package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_unemployment_fact")
@Immutable
public class UnemploymentFact {

  @Id
  @Column(name = "unemployment_event_id", nullable = false)
  private Long unemploymentEventId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "period_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer periodNumber;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "duration_days")
  private Integer durationDays;

  @Column(name = "labour_status_id", nullable = false)
  private Long labourStatusId;

  @Column(name = "labour_status_code", length = 32, nullable = false)
  private String labourStatusCode;

  @Column(name = "unemployment_reason_id", nullable = false)
  private Long unemploymentReasonId;

  @Column(name = "unemployment_reason_code", length = 32, nullable = false)
  private String unemploymentReasonCode;

  @Column(name = "preceding_employment_id")
  private Long precedingEmploymentId;

  @Column(name = "employment_exit_event_id")
  private Long employmentExitEventId;

  @Column(name = "succeeding_employment_id")
  private Long succeedingEmploymentId;

  @Column(name = "reemployed_flag")
  private Integer reemployedFlag;

  @Column(name = "never_preceded_by_employment_flag")
  private Integer neverPrecededByEmploymentFlag;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "followup_task_id")
  private Long followupTaskId;

  @Column(name = "survey_response_id")
  private Long surveyResponseId;

  @Column(name = "employment_info_source_id", nullable = false)
  private Long employmentInfoSourceId;

  @Column(name = "info_source_code", length = 32, nullable = false)
  private String infoSourceCode;

  @Column(name = "is_self_reported_flag", nullable = false)
  private Boolean isSelfReportedFlag;

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

  public UnemploymentFact() {
  }

  public Long getUnemploymentEventId() {
    return unemploymentEventId;
  }

  public void setUnemploymentEventId(Long unemploymentEventId) {
    this.unemploymentEventId = unemploymentEventId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Integer getPeriodNumber() {
    return periodNumber;
  }

  public void setPeriodNumber(Integer periodNumber) {
    this.periodNumber = periodNumber;
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

  public Long getLabourStatusId() {
    return labourStatusId;
  }

  public void setLabourStatusId(Long labourStatusId) {
    this.labourStatusId = labourStatusId;
  }

  public String getLabourStatusCode() {
    return labourStatusCode;
  }

  public void setLabourStatusCode(String labourStatusCode) {
    this.labourStatusCode = labourStatusCode;
  }

  public Long getUnemploymentReasonId() {
    return unemploymentReasonId;
  }

  public void setUnemploymentReasonId(Long unemploymentReasonId) {
    this.unemploymentReasonId = unemploymentReasonId;
  }

  public String getUnemploymentReasonCode() {
    return unemploymentReasonCode;
  }

  public void setUnemploymentReasonCode(String unemploymentReasonCode) {
    this.unemploymentReasonCode = unemploymentReasonCode;
  }

  public Long getPrecedingEmploymentId() {
    return precedingEmploymentId;
  }

  public void setPrecedingEmploymentId(Long precedingEmploymentId) {
    this.precedingEmploymentId = precedingEmploymentId;
  }

  public Long getEmploymentExitEventId() {
    return employmentExitEventId;
  }

  public void setEmploymentExitEventId(Long employmentExitEventId) {
    this.employmentExitEventId = employmentExitEventId;
  }

  public Long getSucceedingEmploymentId() {
    return succeedingEmploymentId;
  }

  public void setSucceedingEmploymentId(Long succeedingEmploymentId) {
    this.succeedingEmploymentId = succeedingEmploymentId;
  }

  public Integer getReemployedFlag() {
    return reemployedFlag;
  }

  public void setReemployedFlag(Integer reemployedFlag) {
    this.reemployedFlag = reemployedFlag;
  }

  public Integer getNeverPrecededByEmploymentFlag() {
    return neverPrecededByEmploymentFlag;
  }

  public void setNeverPrecededByEmploymentFlag(Integer neverPrecededByEmploymentFlag) {
    this.neverPrecededByEmploymentFlag = neverPrecededByEmploymentFlag;
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

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
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
    if (!(o instanceof UnemploymentFact that)) {
      return false;
    }
    return Objects.equals(unemploymentEventId, that.unemploymentEventId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(unemploymentEventId);
  }
}
