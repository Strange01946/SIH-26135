package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SalaryProgressionFactService {

  PagedResponse<SalaryProgressionFactResponse> getAllSalaryProgressions(Pageable pageable);

  List<SalaryProgressionFactResponse> getAllSalaryProgressions();

  SalaryProgressionFactResponse getSalaryProgressionById(Long employmentId);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByTraineeId(Long traineeId);

  PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByTraineeId(Long traineeId, Pageable pageable);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(Long enrollmentId);

  PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByCourseId(Long courseId);

  PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByCourseId(Long courseId, Pageable pageable);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(Long providerId);

  PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(Long providerId, Pageable pageable);

  List<SalaryProgressionFactResponse> getSalaryProgressionsByEngagementTypeId(Long engagementTypeId);

  PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByEngagementTypeId(
      Long engagementTypeId, Pageable pageable);
}
