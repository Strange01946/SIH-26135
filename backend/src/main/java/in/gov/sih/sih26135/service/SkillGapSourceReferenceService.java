package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapSourceResponse;
import java.util.List;

public interface SkillGapSourceReferenceService {

  List<SkillGapSourceResponse> getAllSources();

  SkillGapSourceResponse getById(Long id);

  SkillGapSourceResponse getByCode(String sourceCode);
}
