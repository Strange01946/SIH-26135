package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillLevelResponse;
import java.util.List;

public interface SkillLevelReferenceService {

  List<SkillLevelResponse> getAllSkillLevels();

  SkillLevelResponse getById(Long id);

  SkillLevelResponse getByCode(String levelCode);

  SkillLevelResponse getByRank(Integer levelRank);
}
