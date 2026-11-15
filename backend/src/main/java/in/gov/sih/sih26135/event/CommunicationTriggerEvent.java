package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.security.AuditPayloadMasker;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strongly typed immutable domain event representing a communication notification trigger
 * (e.g., trainee verification reminder, survey follow-up dispatch, or administrative alert).
 *
 * <p>Transports masked contact references and template identifiers without exposing raw plaintext
 * credentials, Aadhaar tokens, or unmasked personal identifiers. Enforces PII masking on recipient
 * identifiers (email, phone) and payload summaries at creation to prevent data exposure in fields or toString().
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

  private static final Pattern EMBEDDED_EMAIL_PATTERN =
      Pattern.compile("(?i)(?<![A-Za-z0-9._%*+-])([A-Za-z0-9._%*+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

  private static final Pattern EMBEDDED_PHONE_PATTERN =
      Pattern.compile("(?<![A-Za-z0-9])(\\+?\\d[\\d\\s-]{8,14}\\d)(?![A-Za-z0-9])");

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
    payloadSummary = sanitizePayloadSummary(payloadSummary);
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
   * while idempotently preserving genuinely masked tokens.
   */
  public static String maskRecipient(String recipient) {
    if (recipient == null || recipient.isBlank()) {
      return recipient;
    }
    String trimmed = recipient.trim();
    if (trimmed.equals(AuditPayloadMasker.REDACTED_MARKER) || trimmed.equals("***REDACTED***")) {
      return trimmed;
    }

    // 1. Check if recipient is an email address
    int atIndex = trimmed.indexOf('@');
    if (atIndex > 0 && atIndex < trimmed.length() - 1 && trimmed.indexOf('@', atIndex + 1) == -1) {
      return maskEmailAddress(trimmed, atIndex);
    }

    // 2. Check if recipient is a phone number or already-masked phone number
    if (isPhoneCandidate(trimmed)) {
      return maskPhoneNumber(trimmed);
    }

    // 3. Fallback: run through AuditPayloadMasker for any credentials/tokens/Aadhaar/PAN
    return AuditPayloadMasker.mask(trimmed);
  }

  /**
   * Masks email address local part while idempotently preserving already-masked emails.
   */
  private static String maskEmailAddress(String email, int atIndex) {
    String localPart = email.substring(0, atIndex);
    String domainPart = email.substring(atIndex);

    // Genuinely already masked with asterisks or REDACTED
    if (localPart.contains("***") || localPart.equals(AuditPayloadMasker.REDACTED_MARKER)) {
      return email;
    }

    if (localPart.length() <= 1) {
      return localPart + "***" + domainPart;
    } else if (localPart.length() == 2) {
      return localPart.charAt(0) + "***" + localPart.charAt(1) + domainPart;
    } else {
      return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
    }
  }

  /**
   * Checks whether the string has the character profile of a phone number (masked or unmasked).
   * Valid phone candidates consist solely of digits, mask characters ('X', '*'), and formatting ("+- ()./").
   */
  private static boolean isPhoneCandidate(String s) {
    int digitOrMaskCount = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isDigit(c) || c == 'X' || c == '*') {
        digitOrMaskCount++;
      } else if (c != '+' && c != '-' && c != ' ' && c != '(' && c != ')' && c != '.' && c != '/') {
        return false;
      }
    }
    return digitOrMaskCount >= 7;
  }

  /**
   * Masks unmasked phone numbers, while preserving already-masked phone formats idempotently.
   */
  private static String maskPhoneNumber(String s) {
    int maskCharCount = 0;
    int digitCount = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == 'X' || c == '*') {
        maskCharCount++;
      } else if (Character.isDigit(c)) {
        digitCount++;
      }
    }
    if (maskCharCount >= 3) {
      return s; // Genuinely already masked, preserve idempotently
    }
    return maskPhoneDigits(s, digitCount);
  }

  /**
   * Masks all digits preceding the last 4 digits with 'X', preserving non-digit formatting characters.
   */
  private static String maskPhoneDigits(String candidate, int digitCount) {
    int digitsToKeep = Math.min(4, digitCount);
    int digitsToMask = digitCount - digitsToKeep;
    StringBuilder sb = new StringBuilder(candidate.length());
    int maskedCount = 0;
    for (int i = 0; i < candidate.length(); i++) {
      char c = candidate.charAt(i);
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

  /**
   * Sanitizes payload summaries by first running through global AuditPayloadMasker
   * (for secrets, tokens, Aadhaar, PAN) and then masking any embedded email addresses and phone numbers.
   */
  public static String sanitizePayloadSummary(String summary) {
    if (summary == null || summary.isBlank()) {
      return summary;
    }
    // 1. First run through global AuditPayloadMasker for secrets, tokens, Aadhaar, PAN
    String result = AuditPayloadMasker.mask(summary);

    // 2. Mask embedded email addresses
    Matcher emailMatcher = EMBEDDED_EMAIL_PATTERN.matcher(result);
    if (emailMatcher.find()) {
      StringBuilder sb = new StringBuilder();
      do {
        String localPart = emailMatcher.group(1);
        String domain = emailMatcher.group(2);
        String replacement;
        if (localPart.contains("***") || localPart.equals(AuditPayloadMasker.REDACTED_MARKER)) {
          replacement = emailMatcher.group(0);
        } else if (localPart.length() <= 1) {
          replacement = localPart + "***@" + domain;
        } else if (localPart.length() == 2) {
          replacement = localPart.charAt(0) + "***" + localPart.charAt(1) + "@" + domain;
        } else {
          replacement = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
        }
        emailMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      } while (emailMatcher.find());
      emailMatcher.appendTail(sb);
      result = sb.toString();
    }

    // 3. Mask embedded phone numbers (10 to 15 digits)
    Matcher phoneMatcher = EMBEDDED_PHONE_PATTERN.matcher(result);
    if (phoneMatcher.find()) {
      StringBuilder sb = new StringBuilder();
      do {
        String candidate = phoneMatcher.group(1);
        int digitCount = 0;
        for (int i = 0; i < candidate.length(); i++) {
          if (Character.isDigit(candidate.charAt(i))) {
            digitCount++;
          }
        }
        String replacement;
        if (digitCount >= 10 && digitCount <= 15) {
          replacement = maskPhoneDigits(candidate, digitCount);
        } else {
          replacement = candidate;
        }
        phoneMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      } while (phoneMatcher.find());
      phoneMatcher.appendTail(sb);
      result = sb.toString();
    }

    return result;
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
