package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapAssessmentStatusResponse;
import java.util.List;

public interface SkillGapAssessmentStatusReferenceService {

  List<SkillGapAssessmentStatusResponse> getAllStatuses();

  SkillGapAssessmentStatusResponse getById(Long id);

  SkillGapAssessmentStatusResponse getByCode(String statusCode);
}
