package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class SalaryFrequencyResponse {

  private Long id;
  private String frequencyCode;
  private String frequencyName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public SalaryFrequencyResponse() {
  }

  public SalaryFrequencyResponse(
      Long id,
      String frequencyCode,
      String frequencyName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.frequencyCode = frequencyCode;
    this.frequencyName = frequencyName;
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

  public String getFrequencyCode() {
    return frequencyCode;
  }

  public void setFrequencyCode(String frequencyCode) {
    this.frequencyCode = frequencyCode;
  }

  public String getFrequencyName() {
    return frequencyName;
  }

  public void setFrequencyName(String frequencyName) {
    this.frequencyName = frequencyName;
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
