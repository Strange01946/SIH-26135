package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CommunicationChannelResponse {

  private Long id;
  private String channelCode;
  private String channelName;
  private Long requiredConsentTypeId;
  private String requiredConsentTypeCode;
  private String requiredConsentTypeName;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CommunicationChannelResponse() {
  }

  public CommunicationChannelResponse(
      Long id,
      String channelCode,
      String channelName,
      Long requiredConsentTypeId,
      String requiredConsentTypeCode,
      String requiredConsentTypeName,
      Integer sortOrder,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.channelCode = channelCode;
    this.channelName = channelName;
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

  public String getChannelCode() {
    return channelCode;
  }

  public void setChannelCode(String channelCode) {
    this.channelCode = channelCode;
  }

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
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
