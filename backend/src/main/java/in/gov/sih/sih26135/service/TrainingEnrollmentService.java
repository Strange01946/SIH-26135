package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

  Page<TrainingEnrollmentResponse> getAllEnrollments(Pageable pageable);

  Page<TrainingEnrollmentResponse> getByTraineeId(Long traineeId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByBatchId(Long batchId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByProgramId(Long programId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByCourseId(Long courseId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByProviderId(Long providerId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByCenterId(Long centerId, Pageable pageable);

  Page<TrainingEnrollmentResponse> getByStatusId(Long statusId, Pageable pageable);

  TrainingEnrollmentResponse createEnrollment(CreateTrainingEnrollmentRequest request);

  TrainingEnrollmentResponse updateEnrollment(Long id, UpdateTrainingEnrollmentRequest request);

  void deleteEnrollment(Long id);
}
