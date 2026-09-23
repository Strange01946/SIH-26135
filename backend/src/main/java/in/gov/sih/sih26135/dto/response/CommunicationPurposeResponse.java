package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CommunicationPurposeResponse {

  private Long id;
  private String purposeCode;
  private String purposeName;
  private Long requiredConsentTypeId;
  private String requiredConsentTypeCode;
  private String requiredConsentTypeName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CommunicationPurposeResponse() {
  }

  public CommunicationPurposeResponse(
      Long id,
      String purposeCode,
      String purposeName,
      Long requiredConsentTypeId,
      String requiredConsentTypeCode,
      String requiredConsentTypeName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.purposeCode = purposeCode;
    this.purposeName = purposeName;
    this.requiredConsentTypeId = requiredConsentTypeId;
    this.requiredConsentTypeCode = requiredConsentTypeCode;
    this.requiredConsentTypeName = requiredConsentTypeName;
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

  public String getPurposeCode() {
    return purposeCode;
  }

  public void setPurposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public String getPurposeName() {
    return purposeName;
  }

  public void setPurposeName(String purposeName) {
    this.purposeName = purposeName;
  }

  public Long getRequiredConsentTypeId() {
    return requiredConsentTypeId;
  }

  public void setRequiredConsentTypeId(Long requiredConsentTypeId) {
    this.requiredConsentTypeId = requiredConsentTypeId;
  }

  public String getRequiredConsentTypeCode() {
    return requiredConsentTypeCode;
  }

  public void setRequiredConsentTypeCode(String requiredConsentTypeCode) {
    this.requiredConsentTypeCode = requiredConsentTypeCode;
  }

  public String getRequiredConsentTypeName() {
    return requiredConsentTypeName;
  }

  public void setRequiredConsentTypeName(String requiredConsentTypeName) {
    this.requiredConsentTypeName = requiredConsentTypeName;
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
