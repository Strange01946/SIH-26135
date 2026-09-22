package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CourseSkillResponse {

  private Long courseId;
  private String courseCode;
  private String courseName;
  private Long skillId;
  private String skillCode;
  private String skillName;
  private Long taughtSkillLevelId;
  private String taughtSkillLevelCode;
  private String taughtSkillLevelName;
  private Integer taughtSkillLevelRank;
  private Long skillImportanceId;
  private String skillImportanceCode;
  private String skillImportanceName;
  private Integer skillImportanceWeight;
  private Boolean isCoreSkill;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CourseSkillResponse() {
  }

  public CourseSkillResponse(
      Long courseId,
      String courseCode,
      String courseName,
      Long skillId,
      String skillCode,
      String skillName,
      Long taughtSkillLevelId,
      String taughtSkillLevelCode,
      String taughtSkillLevelName,
      Integer taughtSkillLevelRank,
      Long skillImportanceId,
      String skillImportanceCode,
      String skillImportanceName,
      Integer skillImportanceWeight,
      Boolean isCoreSkill,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.courseId = courseId;
    this.courseCode = courseCode;
    this.courseName = courseName;
    this.skillId = skillId;
    this.skillCode = skillCode;
    this.skillName = skillName;
    this.taughtSkillLevelId = taughtSkillLevelId;
    this.taughtSkillLevelCode = taughtSkillLevelCode;
    this.taughtSkillLevelName = taughtSkillLevelName;
    this.taughtSkillLevelRank = taughtSkillLevelRank;
    this.skillImportanceId = skillImportanceId;
    this.skillImportanceCode = skillImportanceCode;
    this.skillImportanceName = skillImportanceName;
    this.skillImportanceWeight = skillImportanceWeight;
    this.isCoreSkill = isCoreSkill;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public String getSkillCode() {
    return skillCode;
  }

  public void setSkillCode(String skillCode) {
    this.skillCode = skillCode;
  }

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }

  public Long getTaughtSkillLevelId() {
    return taughtSkillLevelId;
  }

  public void setTaughtSkillLevelId(Long taughtSkillLevelId) {
    this.taughtSkillLevelId = taughtSkillLevelId;
  }

  public String getTaughtSkillLevelCode() {
    return taughtSkillLevelCode;
  }

  public void setTaughtSkillLevelCode(String taughtSkillLevelCode) {
    this.taughtSkillLevelCode = taughtSkillLevelCode;
  }

  public String getTaughtSkillLevelName() {
    return taughtSkillLevelName;
  }

  public void setTaughtSkillLevelName(String taughtSkillLevelName) {
    this.taughtSkillLevelName = taughtSkillLevelName;
  }

  public Integer getTaughtSkillLevelRank() {
    return taughtSkillLevelRank;
  }

  public void setTaughtSkillLevelRank(Integer taughtSkillLevelRank) {
    this.taughtSkillLevelRank = taughtSkillLevelRank;
  }

  public Long getSkillImportanceId() {
    return skillImportanceId;
  }

  public void setSkillImportanceId(Long skillImportanceId) {
    this.skillImportanceId = skillImportanceId;
  }

  public String getSkillImportanceCode() {
    return skillImportanceCode;
  }

  public void setSkillImportanceCode(String skillImportanceCode) {
    this.skillImportanceCode = skillImportanceCode;
  }

  public String getSkillImportanceName() {
    return skillImportanceName;
  }

  public void setSkillImportanceName(String skillImportanceName) {
    this.skillImportanceName = skillImportanceName;
  }

  public Integer getSkillImportanceWeight() {
    return skillImportanceWeight;
  }

  public void setSkillImportanceWeight(Integer skillImportanceWeight) {
    this.skillImportanceWeight = skillImportanceWeight;
  }

  public Boolean getIsCoreSkill() {
    return isCoreSkill;
  }

  public void setIsCoreSkill(Boolean coreSkill) {
    isCoreSkill = coreSkill;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
