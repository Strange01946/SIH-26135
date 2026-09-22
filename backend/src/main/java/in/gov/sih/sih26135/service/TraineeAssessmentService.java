package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import java.time.LocalDate;
import java.util.List;

public interface TraineeAssessmentService {

  TraineeAssessmentResponse getById(Long id);

  TraineeAssessmentResponse getByAssessmentAndTraineeAndAttempt(
      Long assessmentId, Long traineeId, Integer attemptNumber);

  List<TraineeAssessmentResponse> getAllTraineeAssessments();

  List<TraineeAssessmentResponse> getByAssessmentId(Long assessmentId);

  List<TraineeAssessmentResponse> getByEnrollmentId(Long enrollmentId);

  List<TraineeAssessmentResponse> getByTraineeId(Long traineeId);

  List<TraineeAssessmentResponse> getByAssessmentDate(LocalDate assessmentDate);

  TraineeAssessmentResponse createTraineeAssessment(CreateTraineeAssessmentRequest request);

  TraineeAssessmentResponse updateTraineeAssessment(Long id, UpdateTraineeAssessmentRequest request);

  void deleteTraineeAssessment(Long id);
}
