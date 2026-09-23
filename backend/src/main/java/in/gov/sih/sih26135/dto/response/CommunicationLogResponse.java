package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class CommunicationLogResponse {

  private Long id;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private String traineeFirstName;
  private String traineeLastName;
  private Long followupTaskId;
  private Long surveyId;
  private Long surveyResponseId;
  private Long communicationChannelId;
  private String channelCode;
  private String channelName;
  private Long communicationDirectionId;
  private String directionCode;
  private String directionName;
  private Long communicationPurposeId;
  private String purposeCode;
  private String purposeName;
  private Long communicationStatusId;
  private String statusCode;
  private String statusName;
  private Boolean isSuccessFlag;
  private Boolean isFailureFlag;
  private Long consentTypeId;
  private String consentTypeCode;
  private String consentTypeName;
  private LocalDateTime consentCheckedAt;
  private Long initiatedByUserId;
  private String providerMessageId;
  private String messageTemplateCode;
  private String failureReason;
  private LocalDateTime queuedAt;
  private LocalDateTime sentAt;
  private LocalDateTime deliveredAt;
  private LocalDateTime readAt;
  private LocalDateTime failedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public CommunicationLogResponse() {
  }

  public CommunicationLogResponse(
      Long id,
      Long traineeId,
      String traineeRegistrationNumber,
      String traineeFirstName,
      String traineeLastName,
      Long followupTaskId,
      Long surveyId,
      Long surveyResponseId,
      Long communicationChannelId,
      String channelCode,
      String channelName,
      Long communicationDirectionId,
      String directionCode,
      String directionName,
      Long communicationPurposeId,
      String purposeCode,
      String purposeName,
      Long communicationStatusId,
      String statusCode,
      String statusName,
      Boolean isSuccessFlag,
      Boolean isFailureFlag,
      Long consentTypeId,
      String consentTypeCode,
      String consentTypeName,
      LocalDateTime consentCheckedAt,
      Long initiatedByUserId,
      String providerMessageId,
      String messageTemplateCode,
      String failureReason,
      LocalDateTime queuedAt,
      LocalDateTime sentAt,
      LocalDateTime deliveredAt,
      LocalDateTime readAt,
      LocalDateTime failedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.traineeFirstName = traineeFirstName;
    this.traineeLastName = traineeLastName;
    this.followupTaskId = followupTaskId;
    this.surveyId = surveyId;
    this.surveyResponseId = surveyResponseId;
    this.communicationChannelId = communicationChannelId;
    this.channelCode = channelCode;
    this.channelName = channelName;
    this.communicationDirectionId = communicationDirectionId;
    this.directionCode = directionCode;
    this.directionName = directionName;
    this.communicationPurposeId = communicationPurposeId;
    this.purposeCode = purposeCode;
    this.purposeName = purposeName;
    this.communicationStatusId = communicationStatusId;
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.isSuccessFlag = isSuccessFlag;
    this.isFailureFlag = isFailureFlag;
    this.consentTypeId = consentTypeId;
    this.consentTypeCode = consentTypeCode;
    this.consentTypeName = consentTypeName;
    this.consentCheckedAt = consentCheckedAt;
    this.initiatedByUserId = initiatedByUserId;
    this.providerMessageId = providerMessageId;
    this.messageTemplateCode = messageTemplateCode;
    this.failureReason = failureReason;
    this.queuedAt = queuedAt;
    this.sentAt = sentAt;
    this.deliveredAt = deliveredAt;
    this.readAt = readAt;
    this.failedAt = failedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public String getTraineeFirstName() {
    return traineeFirstName;
  }

  public void setTraineeFirstName(String traineeFirstName) {
    this.traineeFirstName = traineeFirstName;
  }

  public String getTraineeLastName() {
    return traineeLastName;
  }

  public void setTraineeLastName(String traineeLastName) {
    this.traineeLastName = traineeLastName;
  }

  public Long getFollowupTaskId() {
    return followupTaskId;
  }

  public void setFollowupTaskId(Long followupTaskId) {
    this.followupTaskId = followupTaskId;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public Long getCommunicationChannelId() {
    return communicationChannelId;
  }

  public void setCommunicationChannelId(Long communicationChannelId) {
    this.communicationChannelId = communicationChannelId;
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

  public Long getCommunicationDirectionId() {
    return communicationDirectionId;
  }

  public void setCommunicationDirectionId(Long communicationDirectionId) {
    this.communicationDirectionId = communicationDirectionId;
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

  public Long getCommunicationPurposeId() {
    return communicationPurposeId;
  }

  public void setCommunicationPurposeId(Long communicationPurposeId) {
    this.communicationPurposeId = communicationPurposeId;
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

  public Long getCommunicationStatusId() {
    return communicationStatusId;
  }

  public void setCommunicationStatusId(Long communicationStatusId) {
    this.communicationStatusId = communicationStatusId;
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

  public Boolean getIsSuccessFlag() {
    return isSuccessFlag;
  }

  public void setIsSuccessFlag(Boolean successFlag) {
    isSuccessFlag = successFlag;
  }

  public Boolean getIsFailureFlag() {
    return isFailureFlag;
  }

  public void setIsFailureFlag(Boolean failureFlag) {
    isFailureFlag = failureFlag;
  }

  public Long getConsentTypeId() {
    return consentTypeId;
  }

  public void setConsentTypeId(Long consentTypeId) {
    this.consentTypeId = consentTypeId;
  }

  public String getConsentTypeCode() {
    return consentTypeCode;
  }

  public void setConsentTypeCode(String consentTypeCode) {
    this.consentTypeCode = consentTypeCode;
  }

  public String getConsentTypeName() {
    return consentTypeName;
  }

  public void setConsentTypeName(String consentTypeName) {
    this.consentTypeName = consentTypeName;
  }

  public LocalDateTime getConsentCheckedAt() {
    return consentCheckedAt;
  }

  public void setConsentCheckedAt(LocalDateTime consentCheckedAt) {
    this.consentCheckedAt = consentCheckedAt;
  }

  public Long getInitiatedByUserId() {
    return initiatedByUserId;
  }

  public void setInitiatedByUserId(Long initiatedByUserId) {
    this.initiatedByUserId = initiatedByUserId;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public void setProviderMessageId(String providerMessageId) {
    this.providerMessageId = providerMessageId;
  }

  public String getMessageTemplateCode() {
    return messageTemplateCode;
  }

  public void setMessageTemplateCode(String messageTemplateCode) {
    this.messageTemplateCode = messageTemplateCode;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public LocalDateTime getQueuedAt() {
    return queuedAt;
  }

  public void setQueuedAt(LocalDateTime queuedAt) {
    this.queuedAt = queuedAt;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public LocalDateTime getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(LocalDateTime deliveredAt) {
    this.deliveredAt = deliveredAt;
  }

  public LocalDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(LocalDateTime readAt) {
    this.readAt = readAt;
  }

  public LocalDateTime getFailedAt() {
    return failedAt;
  }

  public void setFailedAt(LocalDateTime failedAt) {
    this.failedAt = failedAt;
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
