package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import java.util.List;

public interface EnrollmentOutcomeFactService {

  List<EnrollmentOutcomeFactResponse> getAllEnrollmentOutcomes();

  EnrollmentOutcomeFactResponse getEnrollmentOutcomeById(Long enrollmentId);

  EnrollmentOutcomeFactResponse getEnrollmentOutcomeByEnrollmentNumber(String enrollmentNumber);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeId(Long traineeId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProgramId(Long programId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByCourseId(Long courseId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProviderId(Long providerId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByBatchId(Long batchId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeDistrictId(Long traineeDistrictId);

  List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeStateId(Long traineeStateId);
}
