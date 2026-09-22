package in.gov.sih.sih26135.dto.request;

public class CreateSkillRequest {

  private String skillCode;
  private String skillName;
  private String description;
  private Long skillCategoryId;
  private Long lifecycleStatusId;

  public CreateSkillRequest() {
  }

  public CreateSkillRequest(String skillCode, String skillName, Long skillCategoryId, Long lifecycleStatusId) {
    this.skillCode = skillCode;
    this.skillName = skillName;
    this.skillCategoryId = skillCategoryId;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public CreateSkillRequest(
      String skillCode,
      String skillName,
      String description,
      Long skillCategoryId,
      Long lifecycleStatusId) {
    this.skillCode = skillCode;
    this.skillName = skillName;
    this.description = description;
    this.skillCategoryId = skillCategoryId;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getSkillCategoryId() {
    return skillCategoryId;
  }

  public void setSkillCategoryId(Long skillCategoryId) {
    this.skillCategoryId = skillCategoryId;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
