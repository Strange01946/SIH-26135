package in.gov.sih.sih26135.dto.request;

public class UpdateJobRoleSkillRequest {

  private Long requiredSkillLevelId;
  private Long skillImportanceId;

  public UpdateJobRoleSkillRequest() {
  }

  public UpdateJobRoleSkillRequest(Long requiredSkillLevelId, Long skillImportanceId) {
    this.requiredSkillLevelId = requiredSkillLevelId;
    this.skillImportanceId = skillImportanceId;
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
