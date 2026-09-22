package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import java.util.List;

public interface TrainingEnrollmentService {

  TrainingEnrollmentResponse getById(Long id);

  TrainingEnrollmentResponse getByEnrollmentNumber(String enrollmentNumber);

  List<TrainingEnrollmentResponse> getAllEnrollments();

  List<TrainingEnrollmentResponse> getByTraineeId(Long traineeId);

  List<TrainingEnrollmentResponse> getByBatchId(Long batchId);

  List<TrainingEnrollmentResponse> getByProgramId(Long programId);

  List<TrainingEnrollmentResponse> getByCourseId(Long courseId);

  List<TrainingEnrollmentResponse> getByProviderId(Long providerId);

  List<TrainingEnrollmentResponse> getByCenterId(Long centerId);

  List<TrainingEnrollmentResponse> getByStatusId(Long statusId);

  TrainingEnrollmentResponse createEnrollment(CreateTrainingEnrollmentRequest request);

  TrainingEnrollmentResponse updateEnrollment(Long id, UpdateTrainingEnrollmentRequest request);

  void deleteEnrollment(Long id);
}
