package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillRequest;
import in.gov.sih.sih26135.dto.response.SkillResponse;
import java.util.List;

public interface SkillService {

  SkillResponse getById(Long id);

  SkillResponse getByCode(String skillCode);

  List<SkillResponse> getAllSkills();

  List<SkillResponse> getSkillsByCategoryId(Long categoryId);

  List<SkillResponse> getSkillsByLifecycleStatusId(Long lifecycleStatusId);

  SkillResponse createSkill(CreateSkillRequest request);

  SkillResponse updateSkill(Long id, UpdateSkillRequest request);

  void deleteSkill(Long id);
}
