package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SkillImportanceResponse {

  private Long id;
  private String importanceCode;
  private String importanceName;
  private Integer importanceWeight;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SkillImportanceResponse() {
  }

  public SkillImportanceResponse(
      Long id,
      String importanceCode,
      String importanceName,
      Integer importanceWeight,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.importanceCode = importanceCode;
    this.importanceName = importanceName;
    this.importanceWeight = importanceWeight;
    this.sortOrder = sortOrder;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getImportanceCode() {
    return importanceCode;
  }

  public void setImportanceCode(String importanceCode) {
    this.importanceCode = importanceCode;
  }

  public String getImportanceName() {
    return importanceName;
  }

  public void setImportanceName(String importanceName) {
    this.importanceName = importanceName;
  }

  public Integer getImportanceWeight() {
    return importanceWeight;
  }

  public void setImportanceWeight(Integer importanceWeight) {
    this.importanceWeight = importanceWeight;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
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
}
