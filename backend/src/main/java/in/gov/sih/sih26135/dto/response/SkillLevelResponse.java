package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SkillLevelResponse {

  private Long id;
  private String levelCode;
  private String levelName;
  private Integer levelRank;
  private String description;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SkillLevelResponse() {
  }

  public SkillLevelResponse(
      Long id,
      String levelCode,
      String levelName,
      Integer levelRank,
      String description,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.levelCode = levelCode;
    this.levelName = levelName;
    this.levelRank = levelRank;
    this.description = description;
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

  public String getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(String levelCode) {
    this.levelCode = levelCode;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public Integer getLevelRank() {
    return levelRank;
  }

  public void setLevelRank(Integer levelRank) {
    this.levelRank = levelRank;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
