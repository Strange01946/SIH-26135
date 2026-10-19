package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Size;

public class TraineeRecommendationDecisionRequest {

  private Long traineeId;

  @Size(max = 500, message = "Remarks cannot exceed 500 characters")
  private String remarks;

  private Long actorUserId;

  public TraineeRecommendationDecisionRequest() {}

  public TraineeRecommendationDecisionRequest(Long traineeId, String remarks, Long actorUserId) {
    this.traineeId = traineeId;
    this.remarks = remarks;
    this.actorUserId = actorUserId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public Long getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(Long actorUserId) {
    this.actorUserId = actorUserId;
  }
}
