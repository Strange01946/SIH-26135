package in.gov.sih.sih26135.entity;

import java.io.Serializable;
import java.util.Objects;

public class CourseProgramId implements Serializable {

  private Long courseId;
  private Long programId;

  public CourseProgramId() {
  }

  public CourseProgramId(Long courseId, Long programId) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CourseProgramId that)) {
      return false;
    }
    return Objects.equals(courseId, that.courseId) && Objects.equals(programId, that.programId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, programId);
  }
}
