package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import java.util.List;

public interface SalaryProgressionFactService {

  List<SalaryProgressionFactResponse> getAllSalaryProgressions();

  SalaryProgressionFactResponse getSalaryProgressionById(Long employmentId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByTraineeId(Long traineeId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(Long enrollmentId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByCourseId(Long courseId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(Long providerId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByEngagementTypeId(Long engagementTypeId);
}
