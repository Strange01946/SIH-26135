package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "communication_logs")
public class CommunicationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "communication_log_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "followup_task_id")
  private FollowupTask followupTask;

  @Column(name = "survey_id")
  private Long surveyId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_response_id")
  private SurveyResponse surveyResponse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "communication_channel_id", nullable = false)
  private RefCommunicationChannel communicationChannel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "communication_direction_id", nullable = false)
  private RefCommunicationDirection communicationDirection;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "communication_purpose_id", nullable = false)
  private RefCommunicationPurpose communicationPurpose;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "communication_status_id", nullable = false)
  private RefCommunicationStatus communicationStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consent_type_id")
  private RefConsentType consentType;

  @Column(name = "consent_checked_at")
  private LocalDateTime consentCheckedAt;

  @Column(name = "initiated_by_user_id")
  private Long initiatedByUserId;

  @Column(name = "provider_message_id", length = 64, unique = true)
  private String providerMessageId;

  @Column(name = "message_template_code", length = 64)
  private String messageTemplateCode;

  @Column(name = "failure_reason", length = 255)
  private String failureReason;

  @Column(name = "queued_at")
  private LocalDateTime queuedAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "failed_at")
  private LocalDateTime failedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public CommunicationLog() {
  }

  public CommunicationLog(Trainee trainee, RefCommunicationChannel communicationChannel,
      RefCommunicationDirection communicationDirection, RefCommunicationPurpose communicationPurpose,
      RefCommunicationStatus communicationStatus) {
    this.trainee = trainee;
    this.communicationChannel = communicationChannel;
    this.communicationDirection = communicationDirection;
    this.communicationPurpose = communicationPurpose;
    this.communicationStatus = communicationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public FollowupTask getFollowupTask() {
    return followupTask;
  }

  public void setFollowupTask(FollowupTask followupTask) {
    this.followupTask = followupTask;
  }

  public Long getSurveyId() {
    return surveyId;
  }

  public void setSurveyId(Long surveyId) {
    this.surveyId = surveyId;
  }

  public SurveyResponse getSurveyResponse() {
    return surveyResponse;
  }

  public void setSurveyResponse(SurveyResponse surveyResponse) {
    this.surveyResponse = surveyResponse;
  }

  public RefCommunicationChannel getCommunicationChannel() {
    return communicationChannel;
  }

  public void setCommunicationChannel(RefCommunicationChannel communicationChannel) {
    this.communicationChannel = communicationChannel;
  }

  public RefCommunicationDirection getCommunicationDirection() {
    return communicationDirection;
  }

  public void setCommunicationDirection(RefCommunicationDirection communicationDirection) {
    this.communicationDirection = communicationDirection;
  }

  public RefCommunicationPurpose getCommunicationPurpose() {
    return communicationPurpose;
  }

  public void setCommunicationPurpose(RefCommunicationPurpose communicationPurpose) {
    this.communicationPurpose = communicationPurpose;
  }

  public RefCommunicationStatus getCommunicationStatus() {
    return communicationStatus;
  }

  public void setCommunicationStatus(RefCommunicationStatus communicationStatus) {
    this.communicationStatus = communicationStatus;
  }

  public RefConsentType getConsentType() {
    return consentType;
  }

  public void setConsentType(RefConsentType consentType) {
    this.consentType = consentType;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommunicationLog that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "CommunicationLog{" +
        "id=" + id +
        ", providerMessageId='" + providerMessageId + '\'' +
        ", sentAt=" + sentAt +
        '}';
  }
}
