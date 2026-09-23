package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import java.util.List;

public interface EmploymentRetentionFactService {

  List<EmploymentRetentionFactResponse> getAllEmploymentRetentions();

  EmploymentRetentionFactResponse getEmploymentRetentionById(Long employmentId);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByTraineeId(Long traineeId);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEnrollmentId(Long enrollmentId);

  List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEngagementTypeId(Long engagementTypeId);

  List<EmploymentRetentionFactResponse> getCurrentEmploymentRetentions();
}
