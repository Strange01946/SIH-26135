package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateSurveyResponseRequest {

  private Long surveyId;
  private Long surveyTemplateVersionId;
  private Long traineeId;
  private Long enrollmentId;
  private Long followupTaskId;
  private Integer attemptNumber;
  private Long surveyResponseStatusId;
  private LocalDateTime startedAt;
  private LocalDateTime submittedAt;

  public CreateSurveyResponseRequest() {
  }

  public CreateSurveyResponseRequest(
      Long surveyId,
      Long surveyTemplateVersionId,
      Long traineeId,
      Integer attemptNumber,
      Long surveyResponseStatusId,
      LocalDateTime startedAt) {
    this.surveyId = surveyId;
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.traineeId = traineeId;
    this.attemptNumber = attemptNumber;
    this.surveyResponseStatusId = surveyResponseStatusId;
    this.startedAt = startedAt;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public Long getSurveyTemplateVersionId() {
    return surveyTemplateVersionId;
  }

  public void setSurveyTemplateVersionId(Long surveyTemplateVersionId) {
    this.surveyTemplateVersionId = surveyTemplateVersionId;
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
}
