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
@Table(name = "vw_survey_response_fact")
@Immutable
public class SurveyResponseFact {

  @Id
  @Column(name = "survey_response_id", nullable = false)
  private Long surveyResponseId;

  @Column(name = "survey_id", nullable = false)
  private Long surveyId;

  @Column(name = "survey_purpose_id", nullable = false)
  private Long surveyPurposeId;

  @Column(name = "survey_purpose_code", length = 32, nullable = false)
  private String surveyPurposeCode;

  @Column(name = "program_id")
  private Long programId;

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "batch_id")
  private Long batchId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "followup_offset_months", columnDefinition = "SMALLINT UNSIGNED")
  private Integer followupOffsetMonths;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "followup_task_id")
  private Long followupTaskId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "attempt_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer attemptNumber;

  @Column(name = "survey_response_status_id", nullable = false)
  private Long surveyResponseStatusId;

  @Column(name = "response_status_code", length = 32, nullable = false)
  private String responseStatusCode;

  @Column(name = "is_submitted_flag", nullable = false)
  private Boolean isSubmittedFlag;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "trainee_state_id", nullable = false)
  private Long traineeStateId;

  @Column(name = "trainee_district_id", nullable = false)
  private Long traineeDistrictId;

  public SurveyResponseFact() {
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public Long getSurveyPurposeId() {
    return surveyPurposeId;
  }

  public void setSurveyPurposeId(Long surveyPurposeId) {
    this.surveyPurposeId = surveyPurposeId;
  }

  public String getSurveyPurposeCode() {
    return surveyPurposeCode;
  }

  public void setSurveyPurposeCode(String surveyPurposeCode) {
    this.surveyPurposeCode = surveyPurposeCode;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public Integer getFollowupOffsetMonths() {
    return followupOffsetMonths;
  }

  public void setFollowupOffsetMonths(Integer followupOffsetMonths) {
    this.followupOffsetMonths = followupOffsetMonths;
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

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Integer getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public Long getSurveyResponseStatusId() {
    return surveyResponseStatusId;
  }

  public void setSurveyResponseStatusId(Long surveyResponseStatusId) {
    this.surveyResponseStatusId = surveyResponseStatusId;
  }

  public String getResponseStatusCode() {
    return responseStatusCode;
  }

  public void setResponseStatusCode(String responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  public Boolean getIsSubmittedFlag() {
    return isSubmittedFlag;
  }

  public void setIsSubmittedFlag(Boolean isSubmittedFlag) {
    this.isSubmittedFlag = isSubmittedFlag;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
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
    if (!(o instanceof SurveyResponseFact that)) {
      return false;
    }
    return Objects.equals(surveyResponseId, that.surveyResponseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(surveyResponseId);
  }
}
