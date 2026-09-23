package in.gov.sih.sih26135.dto.request;

public class RecordFollowupSurveyResponseRequest {

  private Long followupTaskId;
  private CreateSurveyResponseRequest surveyResponseRequest;
  private Long followupTaskStatusId;
  private Long followupTaskOutcomeId;

  public RecordFollowupSurveyResponseRequest() {
  }

  public RecordFollowupSurveyResponseRequest(
      Long followupTaskId,
      CreateSurveyResponseRequest surveyResponseRequest,
      Long followupTaskStatusId,
      Long followupTaskOutcomeId) {
    this.followupTaskId = followupTaskId;
    this.surveyResponseRequest = surveyResponseRequest;
    this.followupTaskStatusId = followupTaskStatusId;
    this.followupTaskOutcomeId = followupTaskOutcomeId;
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public CreateSurveyResponseRequest getSurveyResponseRequest() {
    return surveyResponseRequest;
  }

  public void setSurveyResponseRequest(CreateSurveyResponseRequest surveyResponseRequest) {
    this.surveyResponseRequest = surveyResponseRequest;
  }

  public Long getFollowupTaskStatusId() {
    return followupTaskStatusId;
  }

  public void setFollowupTaskStatusId(Long followupTaskStatusId) {
    this.followupTaskStatusId = followupTaskStatusId;
  }

  public Long getFollowupTaskOutcomeId() {
    return followupTaskOutcomeId;
  }

  public void setFollowupTaskOutcomeId(Long followupTaskOutcomeId) {
    this.followupTaskOutcomeId = followupTaskOutcomeId;
  }
}
