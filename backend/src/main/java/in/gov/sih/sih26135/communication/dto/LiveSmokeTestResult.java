package in.gov.sih.sih26135.communication.dto;

import java.time.LocalDateTime;

/**
 * Result contract containing delivery proof and verification details for live smoke tests.
 */
public record LiveSmokeTestResult(
    String channel,
    String providerName,
    String recipientMasked,
    String providerMessageId,
    String status,
    boolean deliveryAccepted,
    Long communicationLogId,
    String deliveryProofSummary,
    LocalDateTime verifiedAt
) {
}
