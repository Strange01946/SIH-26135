package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SkillCategoryResponse {

  private Long id;
  private String categoryCode;
  private String categoryName;
  private String description;
  private Long parentCategoryId;
  private String parentCategoryCode;
  private String parentCategoryName;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public SkillCategoryResponse() {
  }

  public SkillCategoryResponse(
      Long id,
      String categoryCode,
      String categoryName,
      String description,
      Long parentCategoryId,
      String parentCategoryCode,
      String parentCategoryName,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.categoryCode = categoryCode;
    this.categoryName = categoryName;
    this.description = description;
    this.parentCategoryId = parentCategoryId;
    this.parentCategoryCode = parentCategoryCode;
    this.parentCategoryName = parentCategoryName;
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

  public String getParentCategoryCode() {
    return parentCategoryCode;
  }

  public void setParentCategoryCode(String parentCategoryCode) {
    this.parentCategoryCode = parentCategoryCode;
  }

  public String getParentCategoryName() {
    return parentCategoryName;
  }

  public void setParentCategoryName(String parentCategoryName) {
    this.parentCategoryName = parentCategoryName;
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
