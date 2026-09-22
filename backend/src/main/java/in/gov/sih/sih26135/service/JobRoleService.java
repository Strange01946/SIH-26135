package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateJobRoleRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobRoleRequest;
import in.gov.sih.sih26135.dto.response.JobRoleResponse;
import java.util.List;

public interface JobRoleService {

  JobRoleResponse getById(Long id);

  JobRoleResponse getByCode(String jobRoleCode);

  List<JobRoleResponse> getAllJobRoles();

  List<JobRoleResponse> getJobRolesBySectorId(Long sectorId);

  List<JobRoleResponse> getJobRolesByIndustryId(Long industryId);

  List<JobRoleResponse> getJobRolesByQualificationLevelId(Long qualificationLevelId);

  JobRoleResponse createJobRole(CreateJobRoleRequest request);

  JobRoleResponse updateJobRole(Long id, UpdateJobRoleRequest request);

  void deleteJobRole(Long id);
}
