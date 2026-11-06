package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EmploymentFactService {

  PagedResponse<EmploymentFactResponse> getAllEmployments(Pageable pageable);

  List<EmploymentFactResponse> getAllEmployments();

  EmploymentFactResponse getEmploymentById(Long employmentId);

  EmploymentFactResponse getEmploymentByEmploymentNumber(String employmentNumber);

  List<EmploymentFactResponse> getEmploymentsByTraineeId(Long traineeId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByEmployerId(Long employerId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByEmployerId(Long employerId, Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByEnrollmentId(Long enrollmentId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByPlacementId(Long placementId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByPlacementId(Long placementId, Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByJobRoleId(Long jobRoleId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByJobRoleId(Long jobRoleId, Pageable pageable);

  List<EmploymentFactResponse> getCurrentEmployments();

  PagedResponse<EmploymentFactResponse> getCurrentEmployments(Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByEngagementTypeId(Long engagementTypeId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByEngagementTypeId(Long engagementTypeId, Pageable pageable);

  List<EmploymentFactResponse> getEmploymentsByTraineeDistrictId(Long traineeDistrictId);

  PagedResponse<EmploymentFactResponse> getEmploymentsByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
