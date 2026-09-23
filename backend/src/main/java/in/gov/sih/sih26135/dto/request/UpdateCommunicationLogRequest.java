package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateCommunicationLogRequest {

  private Long communicationStatusId;
  private String failureReason;
  private LocalDateTime queuedAt;
  private LocalDateTime sentAt;
  private LocalDateTime deliveredAt;
  private LocalDateTime readAt;
  private LocalDateTime failedAt;

  public UpdateCommunicationLogRequest() {
  }

  public Long getCommunicationStatusId() {
    return communicationStatusId;
  }

  public void setCommunicationStatusId(Long communicationStatusId) {
    this.communicationStatusId = communicationStatusId;
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
