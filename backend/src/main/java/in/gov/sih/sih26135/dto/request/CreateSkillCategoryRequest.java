package in.gov.sih.sih26135.dto.request;

public class CreateSkillCategoryRequest {

  private String categoryCode;
  private String categoryName;
  private String description;
  private Long parentCategoryId;
  private Long lifecycleStatusId;

  public CreateSkillCategoryRequest() {
  }

  public CreateSkillCategoryRequest(String categoryCode, String categoryName, Long lifecycleStatusId) {
    this.categoryCode = categoryCode;
    this.categoryName = categoryName;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public CreateSkillCategoryRequest(
      String categoryCode,
      String categoryName,
      String description,
      Long parentCategoryId,
      Long lifecycleStatusId) {
    this.categoryCode = categoryCode;
    this.categoryName = categoryName;
    this.description = description;
    this.parentCategoryId = parentCategoryId;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getParentCategoryId() {
    return parentCategoryId;
  }

  public void setParentCategoryId(Long parentCategoryId) {
    this.parentCategoryId = parentCategoryId;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
