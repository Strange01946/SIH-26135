package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CompanySizeResponse {

  private Long id;
  private String sizeCode;
  private String sizeName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CompanySizeResponse() {
  }

  public CompanySizeResponse(
      Long id,
      String sizeCode,
      String sizeName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.sizeCode = sizeCode;
    this.sizeName = sizeName;
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

  public String getSizeCode() {
    return sizeCode;
  }

  public void setSizeCode(String sizeCode) {
    this.sizeCode = sizeCode;
  }

  public String getSizeName() {
    return sizeName;
  }

  public void setSizeName(String sizeName) {
    this.sizeName = sizeName;
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
