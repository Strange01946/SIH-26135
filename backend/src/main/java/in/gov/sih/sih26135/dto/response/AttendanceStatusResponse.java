package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class AttendanceStatusResponse {

  private Long id;
  private String statusCode;
  private String statusName;
  private Boolean countsAsPresent;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AttendanceStatusResponse() {
  }

  public AttendanceStatusResponse(
      Long id,
      String statusCode,
      String statusName,
      Boolean countsAsPresent,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.countsAsPresent = countsAsPresent;
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

  public Boolean getCountsAsPresent() {
    return countsAsPresent;
  }

  public void setCountsAsPresent(Boolean countsAsPresent) {
    this.countsAsPresent = countsAsPresent;
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
