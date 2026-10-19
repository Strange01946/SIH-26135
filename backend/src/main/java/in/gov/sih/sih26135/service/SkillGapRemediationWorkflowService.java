package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;

public interface SkillGapRemediationWorkflowService {

  RecordSkillGapAssessmentWithGapsResponse recordAssessmentWithGapsAndRecommendations(
      RecordSkillGapAssessmentWithGapsRequest request);

  SkillGapRecommendationResponse acceptRecommendation(
      Long recommendationId, TraineeRecommendationDecisionRequest request);

  SkillGapRecommendationResponse rejectRecommendation(
      Long recommendationId, TraineeRecommendationDecisionRequest request);

  SkillGapRemediationEnrollmentResponse enrollTraineeInRemediationCourse(
      SkillGapRemediationEnrollmentRequest request);

  TraineeSkillGapProfileResponse getTraineeSkillGapProfile(Long traineeId);
}
