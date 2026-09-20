package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "course_programs")
@IdClass(CourseProgramId.class)
public class CourseProgram {

  @Id
  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Id
  @Column(name = "program_id", nullable = false)
  private Long programId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public CourseProgram() {
  }

  public CourseProgram(Long courseId, Long programId) {
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CourseProgram other)) {
      return false;
    }
    return Objects.equals(courseId, other.courseId) && Objects.equals(programId, other.programId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, programId);
  }

  @Override
  public String toString() {
    return "CourseProgram{" +
        "courseId=" + courseId +
        ", programId=" + programId +
        ", createdAt=" + createdAt +
        '}';
  }
}
