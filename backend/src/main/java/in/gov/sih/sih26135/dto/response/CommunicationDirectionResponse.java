package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CommunicationDirectionResponse {

  private Long id;
  private String directionCode;
  private String directionName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CommunicationDirectionResponse() {
  }

  public CommunicationDirectionResponse(
      Long id,
      String directionCode,
      String directionName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.directionCode = directionCode;
    this.directionName = directionName;
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

  public String getDirectionCode() {
    return directionCode;
  }

  public void setDirectionCode(String directionCode) {
    this.directionCode = directionCode;
  }

  public String getDirectionName() {
    return directionName;
  }

  public void setDirectionName(String directionName) {
    this.directionName = directionName;
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
