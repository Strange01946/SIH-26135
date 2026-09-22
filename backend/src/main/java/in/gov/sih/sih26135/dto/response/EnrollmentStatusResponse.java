package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class EnrollmentStatusResponse {

  private Long id;
  private String statusCode;
  private String statusName;
  private Boolean isTerminal;
  private Boolean isCompletedFlag;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public EnrollmentStatusResponse() {
  }

  public EnrollmentStatusResponse(
      Long id,
      String statusCode,
      String statusName,
      Boolean isTerminal,
      Boolean isCompletedFlag,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.isTerminal = isTerminal;
    this.isCompletedFlag = isCompletedFlag;
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

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getStatusName() {
    return statusName;
  }

  public void setStatusName(String statusName) {
    this.statusName = statusName;
  }

  public Boolean getIsTerminal() {
    return isTerminal;
  }

  public void setIsTerminal(Boolean terminal) {
    isTerminal = terminal;
  }

  public Boolean getIsCompletedFlag() {
    return isCompletedFlag;
  }

  public void setIsCompletedFlag(Boolean completedFlag) {
    isCompletedFlag = completedFlag;
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
