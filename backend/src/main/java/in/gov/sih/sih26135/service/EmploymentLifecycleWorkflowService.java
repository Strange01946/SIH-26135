package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentToEmploymentTransitionResponse;

public interface EmploymentLifecycleWorkflowService {

  EmploymentExitAndUnemploymentResponse recordExitAndInitiateUnemployment(
      EmploymentExitAndUnemploymentRequest request);

  UnemploymentToEmploymentTransitionResponse transitionUnemploymentToEmployment(
      UnemploymentToEmploymentTransitionRequest request);

  TraineeCareerTimelineResponse getTraineeCareerTimeline(Long traineeId);
}
