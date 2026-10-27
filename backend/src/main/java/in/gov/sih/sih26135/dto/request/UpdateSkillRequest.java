package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class UpdateSkillRequest {

  @Size(max = 150, message = "skillName cannot exceed 150 characters")
  private String skillName;
  @Size(max = 1000, message = "description cannot exceed 1000 characters")
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
