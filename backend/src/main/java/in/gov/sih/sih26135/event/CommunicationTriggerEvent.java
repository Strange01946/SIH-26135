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

  private static final Pattern EMBEDDED_PHONE_PATTERN = Pattern.compile(
      "(?<![A-Za-z0-9.-])(?!\\d{4}-\\d{2}-\\d{2})(?!\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:"
          + "\\+?\\d{1,3}[-. ]\\([0-9X*]{2,5}\\)[-. ]?[0-9X*]{1,5}[-. ]?[0-9X*]{4}|"
          + "\\([0-9X*]{2,5}\\)[-. ]?[0-9X*]{1,5}[-. ]?[0-9X*]{4}|"
          + "\\+?\\d{1,3}[-. ]\\d{2,5}[-. ]\\d{1,5}[-. ]?\\d{4}|"
          + "[0-9X*]{3}[-. ][0-9X*]{3}[-. ][0-9X*]{4}|"
          + "\\d{2,5}[.-]\\d{2,5}[.-]\\d{4}|"
          + "[X*]{2,5}\\.[X*]{2,5}\\.[X*0-9]{4}|"
          + "[0-9X*]{5}[- ][0-9X*]{5}|"
          + "[0-9X*]{4}[- ][0-9X*]{6}|"
          + "\\+?\\d{1,3}[-. ]\\d{7,10}|"
          + "\\+[X*]{1,3}[-. ][X*0-9]{6,12}|"
          + "[X*]{3,}\\d{4,10}|"
          + "\\d{4,10}[X*]{3,}|"
          + "\\+?\\d{1,3}[-. ][X*]{3,}[-. ][X*0-9]{2,10}|"
          + "(?i)(?<=\\b(?:phone|contact|mobile|tel(?:ephone)?|call|sms|whatsapp)[:#\\s-]{1,5})\\d{13,15}|"
          + "\\+\\d{11,15}|"
          + "\\d{10,12}"
          + ")(?![A-Za-z0-9.])"
  );

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
   * Checks whether the string has the character profile of a phone number (masked, unmasked, or partially masked).
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
   * Checks whether a phone number is genuinely and safely masked.
   * Requires at least 3 mask characters, no more than 4 unmasked trailing digits after the last mask char,
   * no more than 3 unmasked leading digits before the first mask char (country code),
   * and no unmasked run longer than 4 digits anywhere.
   */
  public static boolean isGenuinelyMaskedPhone(String s) {
    if (s == null) {
      return false;
    }
    int firstMaskIdx = -1;
    int lastMaskIdx = -1;
    int maskCharCount = 0;

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == 'X' || c == '*') {
        maskCharCount++;
        if (firstMaskIdx == -1) {
          firstMaskIdx = i;
        }
        lastMaskIdx = i;
      }
    }

    if (maskCharCount < 3) {
      return false; // Not masked enough
    }

    // Count unmasked digits before the first mask character (allowed: country code <= 3 digits)
    int leadingDigits = 0;
    for (int i = 0; i < firstMaskIdx; i++) {
      if (Character.isDigit(s.charAt(i))) {
        leadingDigits++;
      }
    }
    if (leadingDigits > 3) {
      return false; // Sensitive subscriber digits exposed before mask (e.g. 9876543210XXX)
    }

    // Count unmasked digits after the last mask character (allowed: subscriber suffix <= 4 digits)
    int trailingDigits = 0;
    for (int i = lastMaskIdx + 1; i < s.length(); i++) {
      if (Character.isDigit(s.charAt(i))) {
        trailingDigits++;
      }
    }
    if (trailingDigits > 4) {
      return false; // Sensitive subscriber digits exposed after mask (e.g. XXX9876543210)
    }

    // Check maximum contiguous unmasked digit run anywhere in the string
    int currentRun = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isDigit(c)) {
        currentRun++;
        if (currentRun > 4) {
          return false;
        }
      } else if (c == 'X' || c == '*') {
        currentRun = 0;
      }
    }

    return true;
  }

  /**
   * Masks unmasked or partially masked phone numbers, while preserving genuinely masked phone formats idempotently.
   */
  private static String maskPhoneNumber(String s) {
    if (isGenuinelyMaskedPhone(s)) {
      return s; // Genuinely already masked, preserve idempotently
    }
    int digitCount = countDigits(s);
    if (digitCount > 15) {
      return sanitizeOversizedCandidate(s);
    }
    return maskPhoneDigits(s, digitCount);
  }

  private static int countDigits(String s) {
    int count = 0;
    for (int i = 0; i < s.length(); i++) {
      if (Character.isDigit(s.charAt(i))) {
        count++;
      }
    }
    return count;
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

    // 3. Mask embedded phone numbers (common domestic, formatted, and international phone formats)
    Matcher phoneMatcher = EMBEDDED_PHONE_PATTERN.matcher(result);
    if (phoneMatcher.find()) {
      StringBuilder sb = new StringBuilder();
      do {
        String candidate = phoneMatcher.group(0);
        String replacement = processPhoneCandidate(candidate);
        phoneMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
      } while (phoneMatcher.find());
      phoneMatcher.appendTail(sb);
      result = sb.toString();
    }

    return result;
  }

  /**
   * Processes a candidate phone string embedded in payload summaries, sanitizing unmasked phone numbers
   * while preserving IP addresses, decimal numbers, and genuinely masked contacts.
   */
  private static String processPhoneCandidate(String candidate) {
    if (isGenuinelyMaskedPhone(candidate)) {
      return candidate; // Genuinely already masked, preserve idempotently
    }

    int digitCount = countDigits(candidate);

    if (digitCount > 15) {
      return sanitizeOversizedCandidate(candidate);
    }

    // Phone numbers contain between 10 and 15 digits
    if (digitCount < 10) {
      return candidate;
    }

    // Guard against IPv4 addresses (e.g. 192.168.1.100)
    if (candidate.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
      return candidate;
    }

    // Guard against single-period decimal numbers (e.g. 12345678.90)
    if (candidate.indexOf('.') != -1
        && candidate.indexOf('.') == candidate.lastIndexOf('.')
        && !candidate.contains("+") && !candidate.contains("-")
        && !candidate.contains("(") && !candidate.contains(")")
        && !candidate.contains("/") && !candidate.contains(" ")) {
      return candidate;
    }

    return maskPhoneDigits(candidate, digitCount);
  }

  /**
   * Sanitizes oversized candidate strings (> 15 digits) by decomposing them into discrete valid phone
   * numbers, dates, and identifiers, ensuring phone numbers are safely masked without corrupting
   * adjacent dates, IPs, or delimiters.
   */
  public static String sanitizeOversizedCandidate(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return candidate;
    }
    Matcher m = EMBEDDED_PHONE_PATTERN.matcher(candidate);
    if (!m.find()) {
      return candidate;
    }
    StringBuilder sb = new StringBuilder();
    do {
      String sub = m.group(0);
      String replacement;
      if (isGenuinelyMaskedPhone(sub)) {
        replacement = sub;
      } else {
        int subDigits = countDigits(sub);
        if (subDigits >= 10 && subDigits <= 15) {
          replacement = maskPhoneDigits(sub, subDigits);
        } else {
          replacement = sub;
        }
      }
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    } while (m.find());
    m.appendTail(sb);
    return sb.toString();
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
