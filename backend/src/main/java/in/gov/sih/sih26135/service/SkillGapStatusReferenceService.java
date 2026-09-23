package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapStatusResponse;
import java.util.List;

public interface SkillGapStatusReferenceService {

  List<SkillGapStatusResponse> getAllStatuses();

  SkillGapStatusResponse getById(Long id);

  SkillGapStatusResponse getByCode(String statusCode);
}
