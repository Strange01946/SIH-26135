package in.gov.sih.sih26135.entity;

import java.io.Serializable;
import java.util.Objects;

public class CourseSkillId implements Serializable {

  private Long courseId;
  private Long skillId;

  public CourseSkillId() {
  }

  public CourseSkillId(Long courseId, Long skillId) {
    this.courseId = courseId;
    this.skillId = skillId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CourseSkillId that)) {
      return false;
    }
    return Objects.equals(courseId, that.courseId) && Objects.equals(skillId, that.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, skillId);
  }
}
