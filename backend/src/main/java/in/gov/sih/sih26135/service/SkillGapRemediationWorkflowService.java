package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;

public interface SkillGapRemediationWorkflowService {

  RecordSkillGapAssessmentWithGapsResponse recordAssessmentWithGapsAndRecommendations(
      RecordSkillGapAssessmentWithGapsRequest request);

  SkillGapRemediationEnrollmentResponse enrollTraineeInRemediationCourse(
      SkillGapRemediationEnrollmentRequest request);

  TraineeSkillGapProfileResponse getTraineeSkillGapProfile(Long traineeId);
}
