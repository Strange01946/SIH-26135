package in.gov.sih.sih26135.communication.dto;

import java.time.LocalDateTime;

/**
 * Sanitized response contract returned after an outbound notification dispatch attempt.
 *
 * <p>Preserves privacy by returning masked recipient contact references and never disclosing
 * provider authentication credentials, raw API tokens, or unmasked personal identifiers.
 *
 * <p>Accurately distinguishes:
 * <ul>
 *   <li>DEMO simulated dispatch ({@code deliveryMode: DEMO}, {@code status: SIMULATED}, {@code simulated: true}, {@code physicallyDelivered: false})</li>
 *   <li>LIVE provider acceptance ({@code deliveryMode: LIVE}, {@code status: QUEUED}, {@code simulated: false}, {@code physicallyDelivered: false})</li>
 *   <li>LIVE provider sent ({@code deliveryMode: LIVE}, {@code status: SENT}, {@code simulated: false}, {@code physicallyDelivered: false})</li>
 *   <li>LIVE confirmed handset delivery ({@code deliveryMode: LIVE}, {@code status: DELIVERED}, {@code simulated: false}, {@code physicallyDelivered: true})</li>
 *   <li>Provider failure ({@code status: FAILED}, with sanitized {@code errorCode} and {@code errorExplanation})</li>
 * </ul>
 */
public record NotificationDispatchResponse(
    Long communicationLogId,
    String channel,
    String purpose,
    String deliveryMode,
    String status,
    boolean simulated,
    boolean physicallyDelivered,
    String recipientMasked,
    String providerName,
    String providerMessageId,
    boolean accepted,
    String message,
    String errorCode,
    String errorExplanation,
    LocalDateTime dispatchedAt,
    LocalDateTime deliveredAt
) {

  /**
   * Backward-compatible 10-parameter constructor for existing callers and legacy test suites.
   */
  public NotificationDispatchResponse(
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
    this(
        communicationLogId,
        channel,
        purpose,
        "SIMULATED".equalsIgnoreCase(status) ? "DEMO" : "LIVE",
        status,
        "SIMULATED".equalsIgnoreCase(status),
        "DELIVERED".equalsIgnoreCase(status),
        recipientMasked,
        providerName,
        providerMessageId,
        accepted,
        message,
        null,
        null,
        dispatchedAt,
        "DELIVERED".equalsIgnoreCase(status) ? dispatchedAt : null
    );
  }
}
