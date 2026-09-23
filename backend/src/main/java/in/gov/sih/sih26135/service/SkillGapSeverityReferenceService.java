package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapSeverityResponse;
import java.util.List;

public interface SkillGapSeverityReferenceService {

  List<SkillGapSeverityResponse> getAllSeverities();

  SkillGapSeverityResponse getById(Long id);

  SkillGapSeverityResponse getByCode(String severityCode);

  SkillGapSeverityResponse getByRank(Integer severityRank);
}
