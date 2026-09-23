package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRequest;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import java.util.List;

public interface SkillGapService {

  SkillGapResponse createSkillGap(CreateSkillGapRequest request);

  SkillGapResponse updateSkillGap(Long id, UpdateSkillGapRequest request);

  SkillGapResponse getSkillGapById(Long id);

  SkillGapResponse getSkillGapByNumber(String skillGapNumber);

  SkillGapResponse getSkillGapByAssessmentAndSkill(Long assessmentId, Long skillId);

  List<SkillGapResponse> getSkillGapsByAssessment(Long assessmentId);

  List<SkillGapResponse> getSkillGapsByTrainee(Long traineeId);

  List<SkillGapResponse> getSkillGapsBySkill(Long skillId);

  List<SkillGapResponse> getCurrentSkillGapsByTrainee(Long traineeId);

  List<SkillGapResponse> getCurrentSkillGapsBySkill(Long skillId);

  List<SkillGapResponse> getSkillGapsBySeverity(Long severityId);

  List<SkillGapResponse> getSkillGapsByStatus(Long statusId);

  List<SkillGapResponse> getSkillGapsBySource(Long sourceId);

  void deleteSkillGap(Long id);
}
