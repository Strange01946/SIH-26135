package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignCourseProgramRequest;
import in.gov.sih.sih26135.dto.response.CourseProgramResponse;
import java.util.List;

public interface CourseProgramService {

  CourseProgramResponse assignCourseToProgram(AssignCourseProgramRequest request);

  void removeCourseFromProgram(Long courseId, Long programId);

  List<CourseProgramResponse> getProgramsForCourse(Long courseId);

  List<CourseProgramResponse> getCoursesForProgram(Long programId);
}
