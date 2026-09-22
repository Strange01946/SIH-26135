package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignJobRoleSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobRoleSkillRequest;
import in.gov.sih.sih26135.dto.response.JobRoleSkillResponse;
import java.util.List;

public interface JobRoleSkillService {

  JobRoleSkillResponse assignSkillToJobRole(AssignJobRoleSkillRequest request);

  JobRoleSkillResponse updateJobRoleSkill(Long jobRoleId, Long skillId, UpdateJobRoleSkillRequest request);

  void removeSkillFromJobRole(Long jobRoleId, Long skillId);

  List<JobRoleSkillResponse> getSkillsForJobRole(Long jobRoleId);

  List<JobRoleSkillResponse> getJobRolesForSkill(Long skillId);
}
