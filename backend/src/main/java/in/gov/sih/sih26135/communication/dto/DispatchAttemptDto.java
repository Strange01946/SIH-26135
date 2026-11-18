package in.gov.sih.sih26135.communication.dto;

import java.time.LocalDateTime;

/**
 * Representation of an individual dispatch attempt within a durable job lifecycle.
 */
public record DispatchAttemptDto(
    Long attemptId,
    Long jobId,
    int attemptNumber,
    String attemptStatus,
    String providerName,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    LocalDateTime attemptedAt,
    LocalDateTime completedAt
) {
}
