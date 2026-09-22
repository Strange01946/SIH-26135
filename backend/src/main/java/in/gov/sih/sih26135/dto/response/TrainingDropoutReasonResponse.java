package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class TrainingDropoutReasonResponse {

  private Long id;
  private String reasonCode;
  private String reasonName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public TrainingDropoutReasonResponse() {
  }

  public TrainingDropoutReasonResponse(
      Long id,
      String reasonCode,
      String reasonName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.reasonCode = reasonCode;
    this.reasonName = reasonName;
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

  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getReasonName() {
    return reasonName;
  }

  public void setReasonName(String reasonName) {
    this.reasonName = reasonName;
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
