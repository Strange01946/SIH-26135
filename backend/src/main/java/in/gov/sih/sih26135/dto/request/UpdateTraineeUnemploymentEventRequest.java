package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdateTraineeUnemploymentEventRequest {

  private Integer periodNumber;
  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean isCurrent;
  private Long labourStatusId;
  private Long unemploymentReasonId;
  private Long precedingEmploymentId;
  private Long employmentExitEventId;
  private Long succeedingEmploymentId;
  private Long enrollmentId;
  private Long placementId;
  private Long followupTaskId;
  private Long surveyResponseId;
  private Long employmentInfoSourceId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;
  private String remarks;

  public UpdateTraineeUnemploymentEventRequest() {}

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

  public Long getLabourStatusId() {
    return labourStatusId;
  }

  public void setLabourStatusId(Long labourStatusId) {
    this.labourStatusId = labourStatusId;
  }

  public Long getUnemploymentReasonId() {
    return unemploymentReasonId;
  }

  public void setUnemploymentReasonId(Long unemploymentReasonId) {
    this.unemploymentReasonId = unemploymentReasonId;
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

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
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
}
