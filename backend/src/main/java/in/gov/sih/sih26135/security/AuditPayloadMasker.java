package in.gov.sih.sih26135.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Centralized, null-safe, idempotent payload masker for audit trails, operational issues,
 * remarks, system event summaries, and diagnostic outputs.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Redacts passwords, password hashes, JWTs, Bearer/Basic headers, refresh/access tokens,
 *       API keys, client secrets, and JDBC URLs using {@value #REDACTED_MARKER}.</li>
 *   <li>Masks Aadhaar numbers (spaced, hyphenated, or continuous) as {@code XXXX-XXXX-1234}.</li>
 *   <li>Masks PAN identifiers as {@code XXXXXX123X} without altering ordinary words or IDs.</li>
 *   <li>Handles nested JSON objects/arrays as well as plain-text or malformed payloads safely.</li>
 *   <li>Preserves canonical Phase 28.6 {@code [cid=...|rid=...]} correlation tags cleanly.</li>
 * </ul>
 */
@Component
public class AuditPayloadMasker {

  public static final String REDACTED_MARKER = "[REDACTED]";
  public static final int DEFAULT_MAX_LENGTH = 8192;
  private static final int MAX_JSON_DEPTH = 16;

  private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();

  private static final Set<String> SENSITIVE_JSON_KEYS = Set.of(
      "password",
      "passwd",
      "pwd",
      "passwordhash",
      "newpassword",
      "oldpassword",
      "confirmpassword",
      "secret",
      "secretkey",
      "clientsecret",
      "apikey",
      "accesstoken",
      "refreshtoken",
      "idtoken",
      "authtoken",
      "token",
      "jwt",
      "bearer",
      "authorization",
      "privatekey",
      "credentials",
      "credential",
      "sessiontoken",
      "csrftoken",
      "xapikey",
      "passphrase"
  );

  private static final Pattern CORRELATION_PREFIX_PATTERN =
      Pattern.compile("^(\\[cid=[^\\]|]+\\|rid=[^\\]]+\\]\\s*|\\[cid=[^\\]]+\\]\\s*)(.*)$", Pattern.DOTALL);

  private static final Pattern JWT_PATTERN =
      Pattern.compile("(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{6,}\\.[A-Za-z0-9_-]{6,}(?:\\.[A-Za-z0-9_-]*)?");

  private static final Pattern BEARER_BASIC_PATTERN =
      Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9_+./=-]{6,}(?![A-Za-z0-9_+./=-])");

  private static final Pattern PASSWORD_HASH_PATTERN =
      Pattern.compile("(?:\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{20,}|\\$argon2(?:id|i|d)?\\$[^\\s\"',;}\\]]+)");

  private static final Pattern JDBC_URL_PATTERN =
      Pattern.compile("(?i)\\bjdbc:[a-z0-9:]+://[^\\s\"',;}\\]]+");

  private static final Pattern QUOTED_JSON_SECRET_PATTERN = Pattern.compile(
      "(?i)([\"'](?:password(?:_?hash)?|passwd|pwd|new_?password|old_?password|confirm_?password|"
          + "client_?secret|secret(?:_?key)?|api_?key|x-?api-?key|access_?token|refresh_?token|"
          + "id_?token|auth_?token|session_?token|csrf_?token|token|jwt|authorization|bearer|"
          + "private_?key|credentials?|passphrase)[\"']\\s*:\\s*)([\"'])([^\"']*)\\2");

  private static final Pattern SENSITIVE_CONTAINER_PATTERN = Pattern.compile(
      "(?i)([\"']?(?:password(?:_?hash)?|passwd|pwd|new_?password|old_?password|confirm_?password|"
          + "client_?secret|secret(?:_?key)?|api_?key|x-?api-?key|access_?token|refresh_?token|"
          + "id_?token|auth_?token|session_?token|csrf_?token|token|jwt|authorization|bearer|"
          + "private_?key|credentials?|passphrase)[\"']?\\s*[:=]\\s*)(?:\\{[^}\\r\\n]*\\}|\\[(?!REDACTED\\])[^\\]\\r\\n]*\\]|\\{[^\\r\\n]*|\\[(?!REDACTED\\])[^\\r\\n]*)");

  private static final Pattern KV_SECRET_PATTERN = Pattern.compile(
      "(?i)\\b(password(?:_?hash)?|passwd|pwd|new_?password|old_?password|confirm_?password|"
          + "client_?secret|secret(?:_?key)?|api_?key|x-?api-?key|access_?token|refresh_?token|"
          + "id_?token|auth_?token|session_?token|csrf_?token|token|jwt|authorization|"
          + "private_?key|credentials?|passphrase)(\\s*[:=]\\s*)"
          + "(?:([\"'])([^\"'\\r\\n]{0,512})\\3|(\\[REDACTED\\]|\\*\\*\\*REDACTED\\*\\*\\*|(?:Bearer|Basic)\\s+\\[REDACTED\\]|(?:(?:Bearer|Basic)\\s+)?[^\\s,;\\]}\"']+))");

  // Separated 4-4-4 Aadhaar numbers (hyphen or whitespace), e.g., 1234-5678-1234 or 2345 6789 0123
  private static final Pattern AADHAAR_SEPARATED_PATTERN =
      Pattern.compile("(?<![\\d-])(\\d{4})[-\\s](\\d{4})[-\\s](\\d{4})(?![\\d-])");

  // Unseparated 12-digit Aadhaar (leading digit 2-9)
  private static final Pattern AADHAAR_CONTINUOUS_PATTERN =
      Pattern.compile("(?<![\\d-])([2-9]\\d{3})(\\d{4})(\\d{4})(?![\\d-])");

  // Explicitly labeled Aadhaar (allows any leading digit when unseparated)
  private static final Pattern AADHAAR_LABELED_PATTERN = Pattern.compile(
      "(?i)(\\b(?:aadhaar|aadhar)(?:[\\s_-]*(?:no|num|number|id))?\\s*[:=#]\\s*)(\\d{4})[-\\s]?(\\d{4})[-\\s]?(\\d{4})(?![\\d-])");

  // Standard 10-character PAN: ABCDE1234F -> XXXXXX123X
  private static final Pattern PAN_STANDARD_PATTERN =
      Pattern.compile("(?<![A-Za-z0-9])([A-Z]{5})(\\d{3})(\\d)([A-Z])(?![A-Za-z0-9])");

  // Labeled PAN (case-insensitive when preceded by explicit PAN label)
  private static final Pattern PAN_LABELED_PATTERN = Pattern.compile(
      "(?i)(\\bpan(?:[\\s_-]*(?:no|num|number|card|id))?\\s*[:=#]\\s*)([A-Za-z]{5})(\\d{3})(\\d)([A-Za-z])(?![A-Za-z0-9])");

  /**
   * Masks sensitive credentials, tokens, JWTs, Aadhaar numbers, and PAN values in structured JSON
   * or free-text payloads. Null-safe, idempotent, and bounded.
   */
  public static String mask(String input) {
    return maskBounded(input, DEFAULT_MAX_LENGTH);
  }

  /**
   * Masks sensitive content while strictly bounding output length to {@code maxLength}.
   */
  public static String maskBounded(String input, int maxLength) {
    if (input == null || input.isBlank()) {
      return input;
    }
    int effectiveLimit = maxLength > 0 ? maxLength : DEFAULT_MAX_LENGTH;
    String masked = maskInternal(input);
    if (masked.length() > effectiveLimit) {
      return masked.substring(0, effectiveLimit);
    }
    return masked;
  }

  /**
   * Preserves a leading canonical Phase 28.6 {@code [cid=...|rid=...]} correlation tag while masking
   * all body content cleanly.
   */
  public static String maskPreservingCorrelation(String input) {
    return maskPreservingCorrelationBounded(input, DEFAULT_MAX_LENGTH);
  }

  public static String maskPreservingCorrelationBounded(String input, int maxLength) {
    if (input == null || input.isBlank()) {
      return input;
    }
    Matcher matcher = CORRELATION_PREFIX_PATTERN.matcher(input);
    if (matcher.matches()) {
      String prefix = matcher.group(1);
      String body = matcher.group(2);
      String maskedBody = (body == null || body.isBlank()) ? body : maskInternal(body);
      String combined = prefix + (maskedBody != null ? maskedBody : "");
      int effectiveLimit = maxLength > 0 ? maxLength : DEFAULT_MAX_LENGTH;
      return combined.length() > effectiveLimit ? combined.substring(0, effectiveLimit) : combined;
    }
    return maskBounded(input, maxLength);
  }

  private static String maskInternal(String input) {
    String working = input;
    String trimmed = working.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        JsonNode root = SHARED_MAPPER.readTree(trimmed);
        JsonNode maskedNode = maskJsonNode(root, 0);
        return SHARED_MAPPER.writeValueAsString(maskedNode);
      } catch (JsonProcessingException | RuntimeException ignored) {
        // Malformed JSON-looking payload that cannot be inspected safely: fail closed
        return REDACTED_MARKER;
      }
    }
    return maskPlainText(working);
  }

  private static JsonNode maskJsonNode(JsonNode node, int depth) {
    if (node == null) {
      return null;
    }
    if (depth >= MAX_JSON_DEPTH) {
      return TextNode.valueOf(REDACTED_MARKER);
    }
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        JsonNode val = entry.getValue();
        if (isSensitiveKey(key)) {
          if (val != null && !val.isNull()) {
            obj.put(key, REDACTED_MARKER);
          }
        } else {
          obj.set(key, maskJsonNode(val, depth + 1));
        }
      }
      return obj;
    }
    if (node.isArray()) {
      ArrayNode arr = (ArrayNode) node;
      for (int i = 0; i < arr.size(); i++) {
        arr.set(i, maskJsonNode(arr.get(i), depth + 1));
      }
      return arr;
    }
    if (node.isTextual()) {
      return TextNode.valueOf(maskPlainText(node.asText()));
    }
    return node;
  }

  private static boolean isSensitiveKey(String rawKey) {
    if (rawKey == null || rawKey.isBlank()) {
      return false;
    }
    String normalized = rawKey.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    return SENSITIVE_JSON_KEYS.contains(normalized);
  }

  public static String maskPlainText(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }
    String result = text;

    // 1. Redact JDBC URLs, raw JWTs, Bearer/Basic headers, password hashes
    result = JDBC_URL_PATTERN.matcher(result).replaceAll(REDACTED_MARKER);
    result = JWT_PATTERN.matcher(result).replaceAll(REDACTED_MARKER);
    result = BEARER_BASIC_PATTERN.matcher(result).replaceAll("$1 " + REDACTED_MARKER);
    result = PASSWORD_HASH_PATTERN.matcher(result).replaceAll(REDACTED_MARKER);

    // 2. Redact quoted embedded JSON-like key/value secrets without double-masking
    Matcher quotedMatcher = QUOTED_JSON_SECRET_PATTERN.matcher(result);
    StringBuilder quotedSb = new StringBuilder();
    while (quotedMatcher.find()) {
      String val = quotedMatcher.group(3);
      String replacement = isAlreadyRedacted(val)
          ? quotedMatcher.group(0)
          : quotedMatcher.group(1) + quotedMatcher.group(2) + REDACTED_MARKER + quotedMatcher.group(2);
      quotedMatcher.appendReplacement(quotedSb, Matcher.quoteReplacement(replacement));
    }
    quotedMatcher.appendTail(quotedSb);
    result = quotedSb.toString();

    // 3. Redact sensitive keys mapped to uninspected object or array containers in free-text logs
    Matcher containerMatcher = SENSITIVE_CONTAINER_PATTERN.matcher(result);
    if (containerMatcher.find()) {
      result = containerMatcher.replaceAll("$1" + Matcher.quoteReplacement(REDACTED_MARKER));
    }

    // 4. Redact plain-text key=value or key: value secrets without double-masking
    Matcher kvMatcher = KV_SECRET_PATTERN.matcher(result);
    StringBuilder kvSb = new StringBuilder();
    while (kvMatcher.find()) {
      String quote = kvMatcher.group(3);
      String val = quote != null ? kvMatcher.group(4) : kvMatcher.group(5);
      String replacement;
      if (isAlreadyRedacted(val)) {
        replacement = kvMatcher.group(0);
      } else if (quote != null) {
        replacement = kvMatcher.group(1) + kvMatcher.group(2) + quote + REDACTED_MARKER + quote;
      } else {
        replacement = kvMatcher.group(1) + kvMatcher.group(2) + REDACTED_MARKER;
      }
      kvMatcher.appendReplacement(kvSb, Matcher.quoteReplacement(replacement));
    }
    kvMatcher.appendTail(kvSb);
    result = kvSb.toString();

    // 5. Mask Aadhaar identifiers (preserving final 4 digits: XXXX-XXXX-1234)
    result = AADHAAR_LABELED_PATTERN.matcher(result).replaceAll("$1XXXX-XXXX-$4");
    result = AADHAAR_SEPARATED_PATTERN.matcher(result).replaceAll("XXXX-XXXX-$3");
    result = AADHAAR_CONTINUOUS_PATTERN.matcher(result).replaceAll("XXXX-XXXX-$3");

    // 6. Mask PAN identifiers (preserving digits 1..3 as XXXXXX123X)
    result = PAN_LABELED_PATTERN.matcher(result).replaceAll("$1XXXXXX$3X");
    result = PAN_STANDARD_PATTERN.matcher(result).replaceAll("XXXXXX$2X");

    return result;
  }

  private static boolean isAlreadyRedacted(String value) {
    if (value == null) {
      return false;
    }
    String trimmed = value.trim();
    return REDACTED_MARKER.equals(trimmed)
        || "***REDACTED***".equals(trimmed)
        || ("Bearer " + REDACTED_MARKER).equalsIgnoreCase(trimmed)
        || ("Basic " + REDACTED_MARKER).equalsIgnoreCase(trimmed);
  }
}
