package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SkillResponse {

  private Long id;
  private String skillCode;
  private String skillName;
  private String description;
  private Long skillCategoryId;
  private String categoryCode;
  private String categoryName;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public SkillResponse() {
  }

  public SkillResponse(
      Long id,
      String skillCode,
      String skillName,
      String description,
      Long skillCategoryId,
      String categoryCode,
      String categoryName,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.skillCode = skillCode;
    this.skillName = skillName;
    this.description = description;
    this.skillCategoryId = skillCategoryId;
    this.categoryCode = categoryCode;
    this.categoryName = categoryName;
    this.lifecycleStatusId = lifecycleStatusId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
