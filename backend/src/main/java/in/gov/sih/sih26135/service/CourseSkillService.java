package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignCourseSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseSkillRequest;
import in.gov.sih.sih26135.dto.response.CourseSkillResponse;
import java.util.List;

public interface CourseSkillService {

  CourseSkillResponse assignSkillToCourse(AssignCourseSkillRequest request);

  CourseSkillResponse updateCourseSkill(Long courseId, Long skillId, UpdateCourseSkillRequest request);

  void removeSkillFromCourse(Long courseId, Long skillId);

  List<CourseSkillResponse> getSkillsForCourse(Long courseId);

  List<CourseSkillResponse> getCoreSkillsForCourse(Long courseId);

  List<CourseSkillResponse> getCoursesForSkill(Long skillId);
}
