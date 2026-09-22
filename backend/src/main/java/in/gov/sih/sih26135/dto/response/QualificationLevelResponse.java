package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class QualificationLevelResponse {

  private Long id;
  private String levelCode;
  private String levelName;
  private Integer nsqfLevel;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public QualificationLevelResponse() {
  }

  public QualificationLevelResponse(
      Long id,
      String levelCode,
      String levelName,
      Integer nsqfLevel,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.levelCode = levelCode;
    this.levelName = levelName;
    this.nsqfLevel = nsqfLevel;
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

  public Integer getNsqfLevel() {
    return nsqfLevel;
  }

  public void setNsqfLevel(Integer nsqfLevel) {
    this.nsqfLevel = nsqfLevel;
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
