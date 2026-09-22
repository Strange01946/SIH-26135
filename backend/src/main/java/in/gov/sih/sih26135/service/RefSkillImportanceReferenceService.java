package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillImportanceResponse;
import java.util.List;

public interface RefSkillImportanceReferenceService {

  List<SkillImportanceResponse> getAllSkillImportances();

  SkillImportanceResponse getById(Long id);

  SkillImportanceResponse getByCode(String importanceCode);
}
