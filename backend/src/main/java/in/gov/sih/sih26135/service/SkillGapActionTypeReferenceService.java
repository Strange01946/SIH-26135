package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapActionTypeResponse;
import java.util.List;

public interface SkillGapActionTypeReferenceService {

  List<SkillGapActionTypeResponse> getAllActionTypes();

  SkillGapActionTypeResponse getById(Long id);

  SkillGapActionTypeResponse getByCode(String actionCode);
}
