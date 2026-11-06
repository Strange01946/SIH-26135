package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SkillGapFactService {

  PagedResponse<SkillGapFactResponse> getAllSkillGaps(Pageable pageable);

  List<SkillGapFactResponse> getAllSkillGaps();

  SkillGapFactResponse getSkillGapById(Long skillGapId);

  SkillGapFactResponse getSkillGapByNumber(String skillGapNumber);

  List<SkillGapFactResponse> getSkillGapsByTraineeId(Long traineeId);

  PagedResponse<SkillGapFactResponse> getSkillGapsByTraineeId(Long traineeId, Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsBySkillId(Long skillId);

  PagedResponse<SkillGapFactResponse> getSkillGapsBySkillId(Long skillId, Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsByCourseId(Long courseId);

  PagedResponse<SkillGapFactResponse> getSkillGapsByCourseId(Long courseId, Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsByJobRoleId(Long jobRoleId);

  PagedResponse<SkillGapFactResponse> getSkillGapsByJobRoleId(Long jobRoleId, Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsBySeverityId(Long severityId);

  PagedResponse<SkillGapFactResponse> getSkillGapsBySeverityId(Long severityId, Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsByStatusId(Long statusId);

  PagedResponse<SkillGapFactResponse> getSkillGapsByStatusId(Long statusId, Pageable pageable);

  List<SkillGapFactResponse> getCurrentSkillGaps();

  PagedResponse<SkillGapFactResponse> getCurrentSkillGaps(Pageable pageable);

  List<SkillGapFactResponse> getSkillGapsByTraineeDistrictId(Long traineeDistrictId);

  PagedResponse<SkillGapFactResponse> getSkillGapsByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
