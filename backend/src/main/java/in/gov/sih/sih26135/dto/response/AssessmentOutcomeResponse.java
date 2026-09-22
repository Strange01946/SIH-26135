package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class AssessmentOutcomeResponse {

  private Long id;
  private String outcomeCode;
  private String outcomeName;
  private Boolean isPassFlag;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AssessmentOutcomeResponse() {
  }

  public AssessmentOutcomeResponse(
      Long id,
      String outcomeCode,
      String outcomeName,
      Boolean isPassFlag,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.outcomeCode = outcomeCode;
    this.outcomeName = outcomeName;
    this.isPassFlag = isPassFlag;
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

  public String getOutcomeCode() {
    return outcomeCode;
  }

  public void setOutcomeCode(String outcomeCode) {
    this.outcomeCode = outcomeCode;
  }

  public String getOutcomeName() {
    return outcomeName;
  }

  public void setOutcomeName(String outcomeName) {
    this.outcomeName = outcomeName;
  }

  public Boolean getIsPassFlag() {
    return isPassFlag;
  }

  public void setIsPassFlag(Boolean passFlag) {
    isPassFlag = passFlag;
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
