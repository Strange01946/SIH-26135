package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import java.util.List;

public interface SkillGapFactService {

  List<SkillGapFactResponse> getAllSkillGaps();

  SkillGapFactResponse getSkillGapById(Long skillGapId);

  SkillGapFactResponse getSkillGapByNumber(String skillGapNumber);

  List<SkillGapFactResponse> getSkillGapsByTraineeId(Long traineeId);

  List<SkillGapFactResponse> getSkillGapsBySkillId(Long skillId);

  List<SkillGapFactResponse> getSkillGapsByCourseId(Long courseId);

  List<SkillGapFactResponse> getSkillGapsByJobRoleId(Long jobRoleId);

  List<SkillGapFactResponse> getSkillGapsBySeverityId(Long severityId);

  List<SkillGapFactResponse> getSkillGapsByStatusId(Long statusId);

  List<SkillGapFactResponse> getCurrentSkillGaps();

  List<SkillGapFactResponse> getSkillGapsByTraineeDistrictId(Long traineeDistrictId);
}
