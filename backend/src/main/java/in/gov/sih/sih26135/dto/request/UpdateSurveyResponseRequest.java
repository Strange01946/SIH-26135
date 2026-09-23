package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateSurveyResponseRequest {

  private Long enrollmentId;
  private Long followupTaskId;
  private Long surveyResponseStatusId;
  private LocalDateTime submittedAt;

  public UpdateSurveyResponseRequest() {
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

  public Long getSurveyResponseStatusId() {
    return surveyResponseStatusId;
  }

  public void setSurveyResponseStatusId(Long surveyResponseStatusId) {
    this.surveyResponseStatusId = surveyResponseStatusId;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }
}
