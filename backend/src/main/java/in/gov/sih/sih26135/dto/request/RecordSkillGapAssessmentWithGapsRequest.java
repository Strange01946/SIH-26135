package in.gov.sih.sih26135.dto.request;

import java.util.ArrayList;
import java.util.List;

public class RecordSkillGapAssessmentWithGapsRequest {

  private CreateSkillGapAssessmentRequest assessmentRequest;
  private List<CreateSkillGapRequest> gapRequests = new ArrayList<>();
  private List<CreateSkillGapRecommendationRequest> recommendationRequests = new ArrayList<>();

  public RecordSkillGapAssessmentWithGapsRequest() {
  }

  public RecordSkillGapAssessmentWithGapsRequest(
      CreateSkillGapAssessmentRequest assessmentRequest,
      List<CreateSkillGapRequest> gapRequests,
      List<CreateSkillGapRecommendationRequest> recommendationRequests) {
    this.assessmentRequest = assessmentRequest;
    if (gapRequests != null) {
      this.gapRequests = gapRequests;
    }
    if (recommendationRequests != null) {
      this.recommendationRequests = recommendationRequests;
    }
  }

  public CreateSkillGapAssessmentRequest getAssessmentRequest() {
    return assessmentRequest;
  }

  public void setAssessmentRequest(CreateSkillGapAssessmentRequest assessmentRequest) {
    this.assessmentRequest = assessmentRequest;
  }

  public List<CreateSkillGapRequest> getGapRequests() {
    return gapRequests;
  }

  public void setGapRequests(List<CreateSkillGapRequest> gapRequests) {
    this.gapRequests = gapRequests != null ? gapRequests : new ArrayList<>();
  }

  public List<CreateSkillGapRecommendationRequest> getRecommendationRequests() {
    return recommendationRequests;
  }

  public void setRecommendationRequests(List<CreateSkillGapRecommendationRequest> recommendationRequests) {
    this.recommendationRequests = recommendationRequests != null ? recommendationRequests : new ArrayList<>();
  }
}
