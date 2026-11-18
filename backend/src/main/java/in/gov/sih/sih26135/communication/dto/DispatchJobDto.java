package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Representation of a persistent, durable communication dispatch job.
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
    LocalDateTime updatedAt
) {
}
