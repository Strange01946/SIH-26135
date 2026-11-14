package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Phase 28.8 - AuditPayloadMasker Unit Tests")
class AuditPayloadMaskerUnitTest {

  private static final String SAMPLE_JWT =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwicm9sZSI6InN1cGVyX2FkbWluIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

  @Test
  @DisplayName("Masks passwords, hashes, JWTs, Bearer/Basic headers, API keys, secrets, and JDBC URLs")
  void masksCredentialsTokensAndConnectionStrings() {
    String raw = "password=SecretPass#123, refresh_token=tok_live_987654, access_token: acc_998877, "
        + "apiKey=\"key_abc_123456\", clientSecret=cs_999111, "
        + "Authorization=Bearer " + SAMPLE_JWT + ", basicAuth=\"Basic YWRtaW46c2VjcmV0\", "
        + "hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy, "
        + "argon=$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$aGFzaA, "
        + "db=jdbc:mysql://localhost:3306/SIH26135?useSSL=false";

    String masked = AuditPayloadMasker.mask(raw);

    assertThat(masked)
        .doesNotContain("SecretPass#123")
        .doesNotContain("tok_live_987654")
        .doesNotContain("acc_998877")
        .doesNotContain("key_abc_123456")
        .doesNotContain("cs_999111")
        .doesNotContain(SAMPLE_JWT)
        .doesNotContain("YWRtaW46c2VjcmV0")
        .doesNotContain("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
        .doesNotContain("$argon2id$")
        .doesNotContain("jdbc:mysql://")
        .contains("[REDACTED]");
  }

  @Test
  @DisplayName("Masks spaced, hyphenated, continuous, and labeled Aadhaar identifiers while preserving last 4 digits")
  void masksAllAadhaarFormats() {
    String raw = "Hyphen=1234-5678-1234, Spaced=2345 6789 0123, "
        + "Continuous=345678901234, Labeled AadhaarNo: 109876543210, phone=9876543210";

    String masked = AuditPayloadMasker.mask(raw);

    assertThat(masked)
        .contains("XXXX-XXXX-1234")
        .contains("XXXX-XXXX-0123")
        .contains("XXXX-XXXX-1234")
        .contains("XXXX-XXXX-3210")
        .contains("phone=9876543210")
        .doesNotContain("1234-5678-1234")
        .doesNotContain("2345 6789 0123")
        .doesNotContain("345678901234")
        .doesNotContain("109876543210");
  }

  @Test
  @DisplayName("Masks standard and labeled PAN values cleanly without corrupting ordinary text")
  void masksPanValuesAndPreservesNonSensitiveText() {
    String raw = "Trainee PAN ABCDE1234F updated with pan_no: pqrst5678z; batch BATCH-2026-01 verified OK.";

    String masked = AuditPayloadMasker.mask(raw);

    assertThat(masked)
        .contains("XXXXXX123X")
        .contains("pan_no: XXXXXX567X")
        .contains("batch BATCH-2026-01 verified OK.")
        .doesNotContain("ABCDE1234F")
        .doesNotContain("pqrst5678z");
  }

  @Test
  @DisplayName("Recursively masks nested JSON objects and arrays and falls back safely on malformed JSON")
  void masksNestedJsonAndMalformedJsonFallback() {
    String json = "{\"user\":{\"name\":\"Rohan\",\"aadhaar\":\"234567890123\",\"pan\":\"ABCDE1234F\"},"
        + "\"auth\":{\"password\":\"MySecret!\",\"refreshToken\":\"ref_xyz_123\",\"nested\":{\"client_secret\":\"cs_77\"}},"
        + "\"tokens\":[{\"accessToken\":\"acc_111\"}]}";

    String maskedJson = AuditPayloadMasker.mask(json);

    assertThat(maskedJson)
        .contains("\"name\":\"Rohan\"")
        .contains("\"aadhaar\":\"XXXX-XXXX-0123\"")
        .contains("\"pan\":\"XXXXXX123X\"")
        .contains("\"password\":\"[REDACTED]\"")
        .contains("\"refreshToken\":\"[REDACTED]\"")
        .contains("\"client_secret\":\"[REDACTED]\"")
        .contains("\"accessToken\":\"[REDACTED]\"")
        .doesNotContain("234567890123")
        .doesNotContain("ABCDE1234F")
        .doesNotContain("MySecret!")
        .doesNotContain("ref_xyz_123")
        .doesNotContain("cs_77")
        .doesNotContain("acc_111");

    // Non-scalar sensitive keys: object and array values redacted entirely
    String nonScalarJson = "{\"password\":{\"value\":\"PlainSecret123\"},\"accessToken\":[\"secretA\",\"secretB\"]}";
    String maskedNonScalar = AuditPayloadMasker.mask(nonScalarJson);

    assertThat(maskedNonScalar)
        .contains("\"password\":\"[REDACTED]\"")
        .contains("\"accessToken\":\"[REDACTED]\"")
        .doesNotContain("PlainSecret123")
        .doesNotContain("secretA")
        .doesNotContain("secretB");

    // Nesting deeper than 16 levels fails closed to [REDACTED]
    StringBuilder deepJson = new StringBuilder();
    for (int i = 0; i < 20; i++) {
      deepJson.append("{\"layer").append(i).append("\":");
    }
    deepJson.append("{\"deepSecret\":\"SuperHidden\"}");
    for (int i = 0; i < 20; i++) {
      deepJson.append("}");
    }
    String maskedDeep = AuditPayloadMasker.mask(deepJson.toString());
    assertThat(maskedDeep)
        .contains("[REDACTED]")
        .doesNotContain("SuperHidden");

    // Malformed JSON-looking payloads that cannot be inspected safely fail closed to [REDACTED]
    String malformed = "{\"password\":\"BrokenPass\",\"aadhaar\":\"2345-6789-0123\",\"pan\":\"ABCDE1234F\"";
    String maskedMalformed = AuditPayloadMasker.mask(malformed);
    assertThat(maskedMalformed)
        .isEqualTo("[REDACTED]")
        .doesNotContain("BrokenPass")
        .doesNotContain("2345-6789-0123")
        .doesNotContain("ABCDE1234F");

    // Adversarial: Malformed nested password object
    String malformedPasswordObj = "{\"password\":{\"value\":\"PlainSecret123\"";
    assertThat(AuditPayloadMasker.mask(malformedPasswordObj))
        .isEqualTo("[REDACTED]")
        .doesNotContain("PlainSecret123");

    // Adversarial: Malformed token array
    String malformedTokenArray = "{\"accessToken\":[\"secretA\",\"secretB\"";
    assertThat(AuditPayloadMasker.mask(malformedTokenArray))
        .isEqualTo("[REDACTED]")
        .doesNotContain("secretA")
        .doesNotContain("secretB");

    // Adversarial: Excessively deep malformed JSON
    String malformedDeep = "{{{{{{{{{{{{{{{{{\"password\":{\"value\":\"deepSecret\"";
    assertThat(AuditPayloadMasker.mask(malformedDeep))
        .isEqualTo("[REDACTED]")
        .doesNotContain("deepSecret");

    // Free text containing sensitive container (object or array)
    String freeTextWithContainer = "Log event with password: {\"value\":\"PlainSecret123\"} for user";
    assertThat(AuditPayloadMasker.mask(freeTextWithContainer))
        .contains("password: [REDACTED]")
        .doesNotContain("PlainSecret123");

    String freeTextWithMalformedContainer = "Log event with password: {\"value\":\"PlainSecret123\"";
    assertThat(AuditPayloadMasker.mask(freeTextWithMalformedContainer))
        .contains("password: [REDACTED]")
        .doesNotContain("PlainSecret123");

    // Free text containing sensitive array container
    String freeTextWithArray = "Log event with accessToken: [\"secretA\",\"secretB\"] for user";
    assertThat(AuditPayloadMasker.mask(freeTextWithArray))
        .contains("accessToken: [REDACTED]")
        .doesNotContain("secretA")
        .doesNotContain("secretB");

    // Preserve ordinary non-JSON log sanitization
    String plainText = "User rohan with aadhaar=2345-6789-0123 and pan=ABCDE1234F logged in with password=MyPass123";
    assertThat(AuditPayloadMasker.mask(plainText))
        .contains("XXXX-XXXX-0123")
        .contains("XXXXXX123X")
        .contains("password=[REDACTED]")
        .doesNotContain("MyPass123");
  }

  @Test
  @DisplayName("Null-safe, idempotent, length-bounded, and preserves Phase 28.6 correlation tags")
  void nullSafeIdempotentBoundedAndCorrelationPreserving() {
    assertThat(AuditPayloadMasker.mask(null)).isNull();
    assertThat(AuditPayloadMasker.mask("")).isEmpty();
    assertThat(AuditPayloadMasker.mask("   ")).isEqualTo("   ");

    String correlated = "[cid=trace-288|rid=req-288] Failed login for aadhaar=234567890123, token=" + SAMPLE_JWT;
    String once = AuditPayloadMasker.maskPreservingCorrelationBounded(correlated, 500);
    String twice = AuditPayloadMasker.maskPreservingCorrelationBounded(once, 500);

    assertThat(once).startsWith("[cid=trace-288|rid=req-288] ");
    assertThat(once).contains("XXXX-XXXX-0123").contains("[REDACTED]").doesNotContain(SAMPLE_JWT);
    assertThat(twice).isEqualTo(once);

    String longInput = "password=SecretLongValue " + "x".repeat(300);
    String bounded = AuditPayloadMasker.maskBounded(longInput, 64);
    assertThat(bounded).hasSize(64).startsWith("password=[REDACTED]");
  }
}
