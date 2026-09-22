package in.gov.sih.sih26135.dto.request;

public class UpdateSkillRequest {

  private String skillName;
  private String description;
  private Long skillCategoryId;
  private Long lifecycleStatusId;

  public UpdateSkillRequest() {
  }

  public UpdateSkillRequest(String skillName, String description, Long skillCategoryId, Long lifecycleStatusId) {
    this.skillName = skillName;
    this.description = description;
    this.skillCategoryId = skillCategoryId;
    this.lifecycleStatusId = lifecycleStatusId;
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
