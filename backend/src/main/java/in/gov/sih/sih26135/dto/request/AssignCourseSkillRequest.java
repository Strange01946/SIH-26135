package in.gov.sih.sih26135.dto.request;

public class AssignCourseSkillRequest {

  private Long courseId;
  private Long skillId;
  private Long taughtSkillLevelId;
  private Long skillImportanceId;
  private Boolean isCoreSkill = false;

  public AssignCourseSkillRequest() {
  }

  public AssignCourseSkillRequest(
      Long courseId,
      Long skillId,
      Long taughtSkillLevelId,
      Long skillImportanceId,
      Boolean isCoreSkill) {
    this.courseId = courseId;
    this.skillId = skillId;
    this.taughtSkillLevelId = taughtSkillLevelId;
    this.skillImportanceId = skillImportanceId;
    this.isCoreSkill = isCoreSkill != null ? isCoreSkill : false;
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

  public Long getTaughtSkillLevelId() {
    return taughtSkillLevelId;
  }

  public void setTaughtSkillLevelId(Long taughtSkillLevelId) {
    this.taughtSkillLevelId = taughtSkillLevelId;
  }

  public Long getSkillImportanceId() {
    return skillImportanceId;
  }

  public void setSkillImportanceId(Long skillImportanceId) {
    this.skillImportanceId = skillImportanceId;
  }

  public Boolean getIsCoreSkill() {
    return isCoreSkill;
  }

  public void setIsCoreSkill(Boolean coreSkill) {
    isCoreSkill = coreSkill != null ? coreSkill : false;
  }
}
