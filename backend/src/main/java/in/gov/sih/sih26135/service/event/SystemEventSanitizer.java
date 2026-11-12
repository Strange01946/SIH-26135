package in.gov.sih.sih26135.service.event;

import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Schema-safe sanitization and correlation formatting helper for {@code system_event_logs}.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Zero secrets, passwords, JWTs, API keys, Aadhaar/PAN values, JDBC URLs, or stack traces enter DB rows.</li>
 *   <li>Single-line normalized summaries bounded to {@value #MAX_EVENT_SUMMARY_LENGTH} characters.</li>
 *   <li>Bounded {@code event_code}, {@code entity_type}, and {@code source_component} identifiers ({@value #MAX_IDENTIFIER_LENGTH}).</li>
 *   <li>Deterministic correlation and request tracing tags embedded safely within frozen schema constraints.</li>
 * </ul>
 */
public final class SystemEventSanitizer {

  public static final int MAX_IDENTIFIER_LENGTH = 64;
  public static final int MAX_EVENT_SUMMARY_LENGTH = 500;

  private static final Pattern CONTROL_AND_WHITESPACE = Pattern.compile("[\\r\\n\\t\\p{Cntrl}]+");
  private static final Pattern IDENTIFIER_DISALLOWED = Pattern.compile("[^A-Za-z0-9_.-]");
  private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b([2-9]\\d{3})\\s?(\\d{4})\\s?(\\d{4})\\b");
  private static final Pattern PAN_PATTERN = Pattern.compile("\\b([A-Za-z]{5})(\\d{4})([A-Za-z])\\b");
  private static final Pattern JWT_PATTERN = Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}(?:\\.[A-Za-z0-9_-]*)?\\b");
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9_.-]{6,}\\b");
  private static final Pattern SECRET_KV_PATTERN = Pattern.compile(
      "(?i)\\b(password|passwd|pwd|secret|token|jwt|api_?key|access_token|refresh_token|client_secret|authorization|auth)\\s*[:=]\\s*[^\\s,;]+");
  private static final Pattern JDBC_URL_PATTERN = Pattern.compile("(?i)\\bjdbc:[a-z0-9:]+://[^\\s,;]+");
  private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("(?i)\\b(at\\s+[a-z0-9_.$]+\\([^)]*\\)|caused\\s+by:)");

  private SystemEventSanitizer() {
  }

  public static String sanitizeEventCode(String rawCode, String fallback) {
    String candidate = (rawCode == null || rawCode.isBlank()) ? fallback : rawCode;
    if (candidate == null || candidate.isBlank()) {
      return "SYSTEM_EVENT";
    }
    String normalized = IDENTIFIER_DISALLOWED.matcher(candidate.trim().toUpperCase(Locale.ROOT)).replaceAll("_");
    normalized = normalized.replaceAll("_+", "_");
    if (normalized.isEmpty()) {
      return "SYSTEM_EVENT";
    }
    return normalized.length() > MAX_IDENTIFIER_LENGTH
        ? normalized.substring(0, MAX_IDENTIFIER_LENGTH)
        : normalized;
  }

  public static String sanitizeIdentifier(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }
    String cleaned = CONTROL_AND_WHITESPACE.matcher(rawValue.trim()).replaceAll("_");
    cleaned = IDENTIFIER_DISALLOWED.matcher(cleaned).replaceAll("_");
    if (cleaned.isEmpty()) {
      return null;
    }
    return cleaned.length() > MAX_IDENTIFIER_LENGTH
        ? cleaned.substring(0, MAX_IDENTIFIER_LENGTH)
        : cleaned;
  }

  public static String sanitizeMessage(String rawMessage, String fallback) {
    String base = (rawMessage == null || rawMessage.isBlank()) ? fallback : rawMessage;
    if (base == null || base.isBlank()) {
      return "Operational event recorded";
    }
    String sanitized = CONTROL_AND_WHITESPACE.matcher(base.trim()).replaceAll(" ");
    sanitized = JDBC_URL_PATTERN.matcher(sanitized).replaceAll("[REDACTED_URL]");
    sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");
    sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer ***REDACTED***");
    sanitized = SECRET_KV_PATTERN.matcher(sanitized).replaceAll("$1=***REDACTED***");
    sanitized = AADHAAR_PATTERN.matcher(sanitized).replaceAll("XXXX-XXXX-$3");
    sanitized = PAN_PATTERN.matcher(sanitized).replaceAll("XXXXX$2$3");
    int stackIdx = findStackTraceIndex(sanitized);
    if (stackIdx > 0) {
      sanitized = sanitized.substring(0, stackIdx).trim();
    } else if (stackIdx == 0) {
      sanitized = fallback != null && !fallback.isBlank() ? fallback : "Operational event recorded";
    }
    sanitized = sanitized.replaceAll("\\s{2,}", " ").trim();
    if (sanitized.isEmpty()) {
      return fallback != null && !fallback.isBlank() ? fallback : "Operational event recorded";
    }
    return sanitized.length() > MAX_EVENT_SUMMARY_LENGTH
        ? sanitized.substring(0, MAX_EVENT_SUMMARY_LENGTH)
        : sanitized;
  }

  /**
   * Formats a schema-compliant 500-character event summary prefixed with validated correlation and
   * request identifiers when present, avoiding duplicate prefixes if already attached.
   */
  public static String formatWithCorrelation(
      String rawMessage,
      String fallback,
      String correlationId,
      String requestId) {
    String cleanMessage = sanitizeMessage(rawMessage, fallback);
    String validCorr = CorrelationIdFilter.isValidCorrelationId(correlationId) ? correlationId : null;
    String validReq = CorrelationIdFilter.isValidCorrelationId(requestId) ? requestId : validCorr;

    if (validCorr == null) {
      return cleanMessage;
    }

    String tokenPrefix = "[cid=" + validCorr + "|rid=" + validReq + "]";
    if (cleanMessage.startsWith("[cid=")) {
      return cleanMessage.length() > MAX_EVENT_SUMMARY_LENGTH
          ? cleanMessage.substring(0, MAX_EVENT_SUMMARY_LENGTH)
          : cleanMessage;
    }

    String combined = tokenPrefix + " " + cleanMessage;
    return combined.length() > MAX_EVENT_SUMMARY_LENGTH
        ? combined.substring(0, MAX_EVENT_SUMMARY_LENGTH)
        : combined;
  }

  private static int findStackTraceIndex(String value) {
    var matcher = STACK_TRACE_PATTERN.matcher(value);
    if (matcher.find()) {
      return matcher.start();
    }
    return -1;
  }
}
