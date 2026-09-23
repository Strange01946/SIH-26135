package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SurveyResponseResponse {

  private Long id;
  private Long surveyId;
  private Long surveyTemplateVersionId;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFirstName;
  private String traineeLastName;
  private Long enrollmentId;
  private String enrollmentNumber;
  private Long followupTaskId;
  private Integer attemptNumber;
  private Long surveyResponseStatusId;
  private LocalDateTime startedAt;
  private LocalDateTime submittedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SurveyResponseResponse() {
  }

  public SurveyResponseResponse(
      Long id,
      Long surveyId,
      Long surveyTemplateVersionId,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFirstName,
      String traineeLastName,
      Long enrollmentId,
      String enrollmentNumber,
      Long followupTaskId,
      Integer attemptNumber,
      Long surveyResponseStatusId,
      LocalDateTime startedAt,
      LocalDateTime submittedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.surveyId = surveyId;
    this.surveyTemplateVersionId = surveyTemplateVersionId;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFirstName = traineeFirstName;
    this.traineeLastName = traineeLastName;
    this.enrollmentId = enrollmentId;
    this.enrollmentNumber = enrollmentNumber;
    this.followupTaskId = followupTaskId;
    this.attemptNumber = attemptNumber;
    this.surveyResponseStatusId = surveyResponseStatusId;
    this.startedAt = startedAt;
    this.submittedAt = submittedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public String getTraineeFirstName() {
    return traineeFirstName;
  }

  public void setTraineeFirstName(String traineeFirstName) {
    this.traineeFirstName = traineeFirstName;
  }

  public String getTraineeLastName() {
    return traineeLastName;
  }

  public void setTraineeLastName(String traineeLastName) {
    this.traineeLastName = traineeLastName;
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
}
