package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapSummaryResponse;
import java.util.List;

public interface SkillGapSummaryService {

  List<SkillGapSummaryResponse> getAllSkillGapSummaries();

  List<SkillGapSummaryResponse> getAllSkillGapSummariesOrderByGapCountDesc();

  SkillGapSummaryResponse getSkillGapSummaryById(Long skillId, Long skillGapSeverityId);

  List<SkillGapSummaryResponse> getSkillGapSummariesBySkillId(Long skillId);

  List<SkillGapSummaryResponse> getSkillGapSummariesBySeverityId(Long skillGapSeverityId);

  List<SkillGapSummaryResponse> getSkillGapSummariesBySeverityCode(String severityCode);
}
