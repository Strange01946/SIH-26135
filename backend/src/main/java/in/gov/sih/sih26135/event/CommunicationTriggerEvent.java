package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Strongly typed immutable domain event representing a communication notification trigger
 * (e.g., trainee verification reminder, survey follow-up dispatch, or administrative alert).
 *
 * <p>Transports masked contact references and template identifiers without exposing raw plaintext
 * credentials, Aadhaar tokens, or unmasked personal identifiers. Enforces PII masking on recipient
 * identifiers (email, phone) at creation to prevent data exposure in fields or toString().
 */
public record CommunicationTriggerEvent(
    String eventId,
    String eventType,
    Long entityId,
    Long actorUserId,
    String correlationId,
    LocalDateTime occurredAt,
    String channel,
    String recipientMasked,
    String templateCode,
    Long stateId,
    Long trainingProviderId,
    String priority,
    String payloadSummary
) implements DomainEvent {

  public CommunicationTriggerEvent {
    if (eventId == null || eventId.isBlank()) {
      eventId = UUID.randomUUID().toString();
    }
    if (eventType == null || eventType.isBlank()) {
      eventType = "COMMUNICATION_DISPATCH";
    }
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = CorrelationContextHolder.getCorrelationId();
    }
    if (priority == null || priority.isBlank()) {
      priority = "NORMAL";
    }
    recipientMasked = maskRecipient(recipientMasked);
    payloadSummary = AuditPayloadMasker.mask(payloadSummary);
  }

  /**
   * Convenience factory for creating a communication trigger event.
   */
  public static CommunicationTriggerEvent of(
      String eventType,
      Long recipientEntityId,
      Long actorUserId,
      String channel,
      String recipientMasked,
      String templateCode,
      Long trainingProviderId,
      String priority,
      String payloadSummary) {
    return new CommunicationTriggerEvent(
        UUID.randomUUID().toString(),
        eventType,
        recipientEntityId,
        actorUserId,
        CorrelationContextHolder.getCorrelationId(),
        LocalDateTime.now(),
        channel,
        recipientMasked,
        templateCode,
        null,
        trainingProviderId,
        priority,
        payloadSummary
    );
  }

  /**
   * Enforces masking on recipient identifiers (email address or phone number)
   * while idempotently preserving already-masked tokens.
   */
  public static String maskRecipient(String recipient) {
    if (recipient == null || recipient.isBlank()) {
      return recipient;
    }
    String trimmed = recipient.trim();
    if (trimmed.contains("***") || trimmed.contains("XXXX") || trimmed.equals(AuditPayloadMasker.REDACTED_MARKER)) {
      return trimmed;
    }
    // Mask email address: localPart@domain -> firstChar***lastChar@domain
    int atIndex = trimmed.indexOf('@');
    if (atIndex > 0 && atIndex < trimmed.length() - 1 && trimmed.indexOf('@', atIndex + 1) == -1) {
      String localPart = trimmed.substring(0, atIndex);
      String domainPart = trimmed.substring(atIndex);
      if (localPart.contains("*") || localPart.contains("X")) {
        return trimmed;
      }
      if (localPart.length() <= 1) {
        return localPart + "***" + domainPart;
      } else if (localPart.length() == 2) {
        return localPart.charAt(0) + "***" + localPart.charAt(1) + domainPart;
      } else {
        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
      }
    }
    // Mask phone number (at least 7 digits): mask all digits preceding the last 4 digits
    int digitCount = 0;
    for (int i = 0; i < trimmed.length(); i++) {
      if (Character.isDigit(trimmed.charAt(i))) {
        digitCount++;
      }
    }
    if (digitCount >= 7) {
      int digitsToKeep = Math.min(4, digitCount);
      int digitsToMask = digitCount - digitsToKeep;
      StringBuilder sb = new StringBuilder(trimmed.length());
      int maskedCount = 0;
      for (int i = 0; i < trimmed.length(); i++) {
        char c = trimmed.charAt(i);
        if (Character.isDigit(c)) {
          if (maskedCount < digitsToMask) {
            sb.append('X');
            maskedCount++;
          } else {
            sb.append(c);
          }
        } else {
          sb.append(c);
        }
      }
      return sb.toString();
    }
    // Fallback: run through AuditPayloadMasker for any credentials/tokens/Aadhaar/PAN
    return AuditPayloadMasker.mask(trimmed);
  }

  @Override
  public String getEventId() {
    return eventId();
  }

  @Override
  public String getEventType() {
    return eventType();
  }

  @Override
  public String getEntityType() {
    return "CommunicationTrigger";
  }

  @Override
  public Long getEntityId() {
    return entityId();
  }

  @Override
  public Long getActorUserId() {
    return actorUserId();
  }

  @Override
  public String getCorrelationId() {
    return correlationId();
  }

  @Override
  public LocalDateTime getOccurredAt() {
    return occurredAt();
  }
}
