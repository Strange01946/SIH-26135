package in.gov.sih.sih26135.dto.request;

public class UpdateCourseSkillRequest {

  private Long taughtSkillLevelId;
  private Long skillImportanceId;
  private Boolean isCoreSkill;

  public UpdateCourseSkillRequest() {
  }

  public UpdateCourseSkillRequest(Long taughtSkillLevelId, Long skillImportanceId, Boolean isCoreSkill) {
    this.taughtSkillLevelId = taughtSkillLevelId;
    this.skillImportanceId = skillImportanceId;
    this.isCoreSkill = isCoreSkill;
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
    isCoreSkill = coreSkill;
  }
}
