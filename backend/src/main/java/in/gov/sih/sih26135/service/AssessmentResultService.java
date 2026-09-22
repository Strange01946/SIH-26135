package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import java.util.List;

public interface AssessmentResultService {

  AssessmentResultResponse getById(Long id);

  AssessmentResultResponse getByTraineeAssessmentId(Long traineeAssessmentId);

  List<AssessmentResultResponse> getAllAssessmentResults();

  List<AssessmentResultResponse> getByTraineeId(Long traineeId);

  List<AssessmentResultResponse> getByAssessmentOutcomeId(Long outcomeId);

  AssessmentResultResponse createAssessmentResult(CreateAssessmentResultRequest request);

  AssessmentResultResponse updateAssessmentResult(Long id, UpdateAssessmentResultRequest request);

  void deleteAssessmentResult(Long id);
}
