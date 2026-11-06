package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EnrollmentOutcomeFactService {

  PagedResponse<EnrollmentOutcomeFactResponse> getAllEnrollmentOutcomes(Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getAllEnrollmentOutcomes();

  EnrollmentOutcomeFactResponse getEnrollmentOutcomeById(Long enrollmentId);

  EnrollmentOutcomeFactResponse getEnrollmentOutcomeByEnrollmentNumber(String enrollmentNumber);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeId(Long traineeId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeId(Long traineeId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProgramId(Long programId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProgramId(Long programId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByCourseId(Long courseId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByCourseId(Long courseId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProviderId(Long providerId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProviderId(Long providerId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByBatchId(Long batchId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByBatchId(Long batchId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeDistrictId(Long traineeDistrictId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeDistrictId(
      Long traineeDistrictId, Pageable pageable);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeStateId(Long traineeStateId);

  PagedResponse<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeStateId(
      Long traineeStateId, Pageable pageable);
}
