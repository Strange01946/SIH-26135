package in.gov.sih.sih26135.dto.request;

public class RecordEngagementAttemptRequest {

  private Long followupTaskId;
  private CreateCommunicationLogRequest communicationLogRequest;
  private Long followupTaskStatusId;
  private Long followupTaskOutcomeId;

  public RecordEngagementAttemptRequest() {
  }

  public RecordEngagementAttemptRequest(
      Long followupTaskId,
      CreateCommunicationLogRequest communicationLogRequest,
      Long followupTaskStatusId,
      Long followupTaskOutcomeId) {
    this.followupTaskId = followupTaskId;
    this.communicationLogRequest = communicationLogRequest;
    this.followupTaskStatusId = followupTaskStatusId;
    this.followupTaskOutcomeId = followupTaskOutcomeId;
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public CreateCommunicationLogRequest getCommunicationLogRequest() {
    return communicationLogRequest;
  }

  public void setCommunicationLogRequest(CreateCommunicationLogRequest communicationLogRequest) {
    this.communicationLogRequest = communicationLogRequest;
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
