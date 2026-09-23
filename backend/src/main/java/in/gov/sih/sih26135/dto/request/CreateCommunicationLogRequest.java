package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateCommunicationLogRequest {

  private Long traineeId;
  private Long followupTaskId;
  private Long surveyId;
  private Long surveyResponseId;
  private Long communicationChannelId;
  private Long communicationDirectionId;
  private Long communicationPurposeId;
  private Long communicationStatusId;
  private Long consentTypeId;
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

  public CreateCommunicationLogRequest() {
  }

  public CreateCommunicationLogRequest(
      Long traineeId,
      Long communicationChannelId,
      Long communicationDirectionId,
      Long communicationPurposeId,
      Long communicationStatusId) {
    this.traineeId = traineeId;
    this.communicationChannelId = communicationChannelId;
    this.communicationDirectionId = communicationDirectionId;
    this.communicationPurposeId = communicationPurposeId;
    this.communicationStatusId = communicationStatusId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
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

  public Long getCommunicationDirectionId() {
    return communicationDirectionId;
  }

  public void setCommunicationDirectionId(Long communicationDirectionId) {
    this.communicationDirectionId = communicationDirectionId;
  }

  public Long getCommunicationPurposeId() {
    return communicationPurposeId;
  }

  public void setCommunicationPurposeId(Long communicationPurposeId) {
    this.communicationPurposeId = communicationPurposeId;
  }

  public Long getCommunicationStatusId() {
    return communicationStatusId;
  }

  public void setCommunicationStatusId(Long communicationStatusId) {
    this.communicationStatusId = communicationStatusId;
  }

  public Long getConsentTypeId() {
    return consentTypeId;
  }

  public void setConsentTypeId(Long consentTypeId) {
    this.consentTypeId = consentTypeId;
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
}
