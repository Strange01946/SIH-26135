package in.gov.sih.sih26135.dto.request;

public class SkillGapRemediationEnrollmentRequest {

  private Long recommendationId;
  private CreateTrainingEnrollmentRequest enrollmentRequest;
  private UpdateSkillGapRecommendationRequest recommendationStatusUpdate;

  public SkillGapRemediationEnrollmentRequest() {
  }

  public SkillGapRemediationEnrollmentRequest(
      Long recommendationId,
      CreateTrainingEnrollmentRequest enrollmentRequest,
      UpdateSkillGapRecommendationRequest recommendationStatusUpdate) {
    this.recommendationId = recommendationId;
    this.enrollmentRequest = enrollmentRequest;
    this.recommendationStatusUpdate = recommendationStatusUpdate;
  }

  public Long getRecommendationId() {
    return recommendationId;
  }

  public void setRecommendationId(Long recommendationId) {
    this.recommendationId = recommendationId;
  }

  public CreateTrainingEnrollmentRequest getEnrollmentRequest() {
    return enrollmentRequest;
  }

  public void setEnrollmentRequest(CreateTrainingEnrollmentRequest enrollmentRequest) {
    this.enrollmentRequest = enrollmentRequest;
  }

  public UpdateSkillGapRecommendationRequest getRecommendationStatusUpdate() {
    return recommendationStatusUpdate;
  }

  public void setRecommendationStatusUpdate(UpdateSkillGapRecommendationRequest recommendationStatusUpdate) {
    this.recommendationStatusUpdate = recommendationStatusUpdate;
  }
}
