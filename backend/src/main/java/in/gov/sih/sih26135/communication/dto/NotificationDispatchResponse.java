package in.gov.sih.sih26135.communication.dto;

import java.time.LocalDateTime;

/**
 * Sanitized response contract returned after an outbound notification dispatch attempt.
 *
 * <p>Preserves privacy by returning masked recipient contact references and never disclosing
 * provider authentication credentials, raw API tokens, or unmasked personal identifiers.
 */
public record NotificationDispatchResponse(
    Long communicationLogId,
    String channel,
    String purpose,
    String recipientMasked,
    String providerName,
    String providerMessageId,
    String status,
    boolean accepted,
    String message,
    LocalDateTime dispatchedAt
) {
}
