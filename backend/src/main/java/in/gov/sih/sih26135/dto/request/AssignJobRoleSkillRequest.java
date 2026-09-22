package in.gov.sih.sih26135.dto.request;

public class AssignJobRoleSkillRequest {

  private Long jobRoleId;
  private Long skillId;
  private Long requiredSkillLevelId;
  private Long skillImportanceId;

  public AssignJobRoleSkillRequest() {
  }

  public AssignJobRoleSkillRequest(
      Long jobRoleId,
      Long skillId,
      Long requiredSkillLevelId,
      Long skillImportanceId) {
    this.jobRoleId = jobRoleId;
    this.skillId = skillId;
    this.requiredSkillLevelId = requiredSkillLevelId;
    this.skillImportanceId = skillImportanceId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public Long getRequiredSkillLevelId() {
    return requiredSkillLevelId;
  }

  public void setRequiredSkillLevelId(Long requiredSkillLevelId) {
    this.requiredSkillLevelId = requiredSkillLevelId;
  }

  public Long getSkillImportanceId() {
    return skillImportanceId;
  }

  public void setSkillImportanceId(Long skillImportanceId) {
    this.skillImportanceId = skillImportanceId;
  }
}
