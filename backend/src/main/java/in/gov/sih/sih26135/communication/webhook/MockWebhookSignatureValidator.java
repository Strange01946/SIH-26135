package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deterministic test and demonstration webhook signature validator supporting HMAC-SHA256
 * and designated test signatures, strictly restricted to authorized non-production test environments.
 */
@Component
public class MockWebhookSignatureValidator implements WebhookSignatureValidator {

  private static final Logger log = LoggerFactory.getLogger(MockWebhookSignatureValidator.class);

  public static final String HEADER_MOCK_SIGNATURE = "X-Mock-Signature";
  public static final String HEADER_WEBHOOK_SIGNATURE = "X-Webhook-Signature";
  public static final String TEST_MOCK_VALID_SIGNATURE = "mock-valid-signature";
  public static final String DEFAULT_MOCK_SECRET = "sih26135-mock-webhook-secret";

  private final CommunicationProperties properties;

  @Autowired
  public MockWebhookSignatureValidator(CommunicationProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getSupportedProvider() {
    return "mock";
  }

  @Override
  public boolean validateSignature(HttpServletRequest request, byte[] bodyBytes) {
    if (request == null) {
      return false;
    }

    // Fail closed: mock webhook authentication is strictly restricted to authorized non-production test environments
    if (!properties.isSyntheticTestSignatureAllowed()) {
      log.warn("[MockWebhook] Mock webhook authentication is disabled outside authorized test environments");
      return false;
    }

    String sig = request.getHeader(HEADER_MOCK_SIGNATURE);
    if (sig == null || sig.isBlank()) {
      sig = request.getHeader(HEADER_WEBHOOK_SIGNATURE);
    }

    if (sig == null || sig.isBlank()) {
      log.warn("[MockWebhook] Missing signature header (expected {} or {})",
          HEADER_MOCK_SIGNATURE, HEADER_WEBHOOK_SIGNATURE);
      return false;
    }

    // Direct match for test fixture
    if (TEST_MOCK_VALID_SIGNATURE.equalsIgnoreCase(sig.trim())) {
      return true;
    }

    // Validate HMAC-SHA256 of bodyBytes
    if (bodyBytes != null && bodyBytes.length > 0) {
      try {
        String computed = computeHmacSha256Hex(bodyBytes, DEFAULT_MOCK_SECRET);
        return MessageDigest.isEqual(
            sig.trim().getBytes(StandardCharsets.UTF_8),
            computed.getBytes(StandardCharsets.UTF_8)
        );
      } catch (Exception ex) {
        log.warn("[MockWebhook] Failed to compute HMAC-SHA256: {}", ex.getMessage());
        return false;
      }
    }

    return false;
  }

  public static String computeHmacSha256Hex(byte[] data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] hash = mac.doFinal(data);
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
