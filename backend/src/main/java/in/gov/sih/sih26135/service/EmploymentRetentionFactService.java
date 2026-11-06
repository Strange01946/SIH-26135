package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EmploymentRetentionFactService {

  PagedResponse<EmploymentRetentionFactResponse> getAllEmploymentRetentions(Pageable pageable);

  List<EmploymentRetentionFactResponse> getAllEmploymentRetentions();

  EmploymentRetentionFactResponse getEmploymentRetentionById(Long employmentId);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByTraineeId(Long traineeId);

  PagedResponse<EmploymentRetentionFactResponse> getEmploymentRetentionsByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEnrollmentId(Long enrollmentId);

  PagedResponse<EmploymentRetentionFactResponse> getEmploymentRetentionsByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEngagementTypeId(Long engagementTypeId);

  PagedResponse<EmploymentRetentionFactResponse> getEmploymentRetentionsByEngagementTypeId(
      Long engagementTypeId, Pageable pageable);

  List<EmploymentRetentionFactResponse> getCurrentEmploymentRetentions();

  PagedResponse<EmploymentRetentionFactResponse> getCurrentEmploymentRetentions(Pageable pageable);
}
