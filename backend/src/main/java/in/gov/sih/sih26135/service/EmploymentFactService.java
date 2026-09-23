package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import java.util.List;

public interface EmploymentFactService {

  List<EmploymentFactResponse> getAllEmployments();

  EmploymentFactResponse getEmploymentById(Long employmentId);

  EmploymentFactResponse getEmploymentByEmploymentNumber(String employmentNumber);

  List<EmploymentFactResponse> getEmploymentsByTraineeId(Long traineeId);

  List<EmploymentFactResponse> getEmploymentsByEmployerId(Long employerId);

  List<EmploymentFactResponse> getEmploymentsByEnrollmentId(Long enrollmentId);

  List<EmploymentFactResponse> getEmploymentsByPlacementId(Long placementId);

  List<EmploymentFactResponse> getEmploymentsByJobRoleId(Long jobRoleId);

  List<EmploymentFactResponse> getCurrentEmployments();

  List<EmploymentFactResponse> getEmploymentsByEngagementTypeId(Long engagementTypeId);

  List<EmploymentFactResponse> getEmploymentsByTraineeDistrictId(Long traineeDistrictId);
}
