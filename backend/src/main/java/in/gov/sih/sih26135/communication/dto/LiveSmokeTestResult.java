package in.gov.sih.sih26135.communication.dto;

import java.time.LocalDateTime;

/**
 * Result contract containing delivery proof and verification details for live smoke tests.
 * Explicitly exposes active providerMode (LIVE vs MOCK) to prevent silent fallback claims.
 */
public record LiveSmokeTestResult(
    String channel,
    String providerName,
    String providerMode,
    String recipientMasked,
    String providerMessageId,
    String status,
    boolean deliveryAccepted,
    Long communicationLogId,
    String deliveryProofSummary,
    LocalDateTime verifiedAt
) {

  public LiveSmokeTestResult(
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
    this(
        channel,
        providerName,
        "LIVE",
        recipientMasked,
        providerMessageId,
        status,
        deliveryAccepted,
        communicationLogId,
        deliveryProofSummary,
        verifiedAt
    );
  }
}
