package in.gov.sih.sih26135.dto.request;

public class AssignCourseProgramRequest {

  private Long courseId;
  private Long programId;

  public AssignCourseProgramRequest() {
  }

  public AssignCourseProgramRequest(Long courseId, Long programId) {
    this.courseId = courseId;
    this.programId = programId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }
}
