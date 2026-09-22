package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.AssignCourseProgramRequest;
import in.gov.sih.sih26135.dto.response.CourseProgramResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.CourseProgram;
import in.gov.sih.sih26135.entity.Program;
import org.springframework.stereotype.Component;

@Component
public class CourseProgramMapper {

  public CourseProgramResponse toResponse(CourseProgram entity, Course course, Program program) {
    if (entity == null) {
      return null;
    }

    String courseCode = course != null ? course.getCourseCode() : null;
    String courseName = course != null ? course.getCourseName() : null;
    String programCode = program != null ? program.getProgramCode() : null;
    String programName = program != null ? program.getProgramName() : null;

    return new CourseProgramResponse(
        entity.getCourseId(),
        courseCode,
        courseName,
        entity.getProgramId(),
        programCode,
        programName,
        entity.getCreatedAt()
    );
  }

  public CourseProgram toEntity(AssignCourseProgramRequest request) {
    if (request == null) {
      return null;
    }

    CourseProgram cp = new CourseProgram();
    cp.setCourseId(request.getCourseId());
    cp.setProgramId(request.getProgramId());
    return cp;
  }
}
