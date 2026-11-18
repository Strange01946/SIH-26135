package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Representation of a persistent, durable communication dispatch job
 * with optimistic worker lease fencing token support.
 */
public record DispatchJobDto(
    Long jobId,
    String idempotencyKey,
    Long traineeId,
    Long trainingProviderId,
    CommunicationChannel channel,
    CommunicationPurpose purpose,
    String templateCode,
    Map<String, String> templateParameters,
    String jobStatus,
    int attemptCount,
    int maxAttempts,
    String deliveryMode,
    Long initiatedByUserId,
    String initiatedByService,
    String tenantScopeType,
    LocalDateTime scheduledAt,
    LocalDateTime lockedAt,
    String lockedBy,
    Long communicationLogId,
    String lastErrorCode,
    String lastErrorMessage,
    String deadLetterReason,
    LocalDateTime deadLetteredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int leaseVersion
) {

  /**
   * Backward compatible 25-parameter constructor defaulting leaseVersion to 1.
   */
  public DispatchJobDto(
      Long jobId,
      String idempotencyKey,
      Long traineeId,
      Long trainingProviderId,
      CommunicationChannel channel,
      CommunicationPurpose purpose,
      String templateCode,
      Map<String, String> templateParameters,
      String jobStatus,
      int attemptCount,
      int maxAttempts,
      String deliveryMode,
      Long initiatedByUserId,
      String initiatedByService,
      String tenantScopeType,
      LocalDateTime scheduledAt,
      LocalDateTime lockedAt,
      String lockedBy,
      Long communicationLogId,
      String lastErrorCode,
      String lastErrorMessage,
      String deadLetterReason,
      LocalDateTime deadLetteredAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
    this(
        jobId, idempotencyKey, traineeId, trainingProviderId, channel, purpose,
        templateCode, templateParameters, jobStatus, attemptCount, maxAttempts,
        deliveryMode, initiatedByUserId, initiatedByService, tenantScopeType,
        scheduledAt, lockedAt, lockedBy, communicationLogId, lastErrorCode,
        lastErrorMessage, deadLetterReason, deadLetteredAt, createdAt, updatedAt, 1
    );
  }
}
