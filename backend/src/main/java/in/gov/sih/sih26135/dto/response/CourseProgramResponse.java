package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CourseProgramResponse {

  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long programId;
  private String programCode;
  private String programName;
  private LocalDateTime createdAt;

  public CourseProgramResponse() {
  }

  public CourseProgramResponse(
      Long courseId,
      String courseCode,
      String courseName,
      Long programId,
      String programCode,
      String programName,
      LocalDateTime createdAt) {
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.createdAt = createdAt;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public String getProgramCode() {
    return programCode;
  }

  public void setProgramCode(String programCode) {
    this.programCode = programCode;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
