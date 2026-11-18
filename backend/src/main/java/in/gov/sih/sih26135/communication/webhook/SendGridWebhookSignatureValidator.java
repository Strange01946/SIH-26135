package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates SendGrid webhook delivery event authenticity and timestamp freshness
 * using official ECDSA signature verification (SHA256withECDSA on NIST P-256 curve).
 *
 * <p>Enforces:
 * <ul>
 *   <li>Official ECDSA signed-event verification over {@code timestamp + payload} using configured public key.</li>
 *   <li>Verification of timestamp freshness within a 10-minute sliding window (replay attack mitigation).</li>
 *   <li>Fail-closed behavior when verification key is missing or unparseable.</li>
 *   <li>Test signature acceptance strictly gated behind {@code allowTestWebhookSignatures}.</li>
 * </ul>
 */
@Component
public class SendGridWebhookSignatureValidator implements WebhookSignatureValidator {

  private static final Logger log = LoggerFactory.getLogger(SendGridWebhookSignatureValidator.class);

  public static final String HEADER_SENDGRID_SIGNATURE = "X-Twilio-Email-Event-Webhook-Signature";
  public static final String HEADER_SENDGRID_TIMESTAMP = "X-Twilio-Email-Event-Webhook-Timestamp";
  public static final String HEADER_SENDGRID_TOKEN = "X-SendGrid-Token";
  public static final String TEST_SENDGRID_SIGNATURE = "test-sendgrid-signature";
  public static final long MAX_ALLOWED_TIMESTAMP_DRIFT_SECONDS = 600L;

  private final CommunicationProperties properties;

  @Autowired
  public SendGridWebhookSignatureValidator(CommunicationProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getSupportedProvider() {
    return "sendgrid";
  }

  @Override
  public boolean validateSignature(HttpServletRequest request, byte[] bodyBytes) {
    if (request == null) {
      return false;
    }

    String headerSig = request.getHeader(HEADER_SENDGRID_SIGNATURE);
    String headerToken = request.getHeader(HEADER_SENDGRID_TOKEN);
    String headerTimestamp = request.getHeader(HEADER_SENDGRID_TIMESTAMP);

    // 1. Verify timestamp freshness (replay attack mitigation)
    if (headerTimestamp != null && !headerTimestamp.isBlank()) {
      try {
        long eventTs = Long.parseLong(headerTimestamp.trim());
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - eventTs) > MAX_ALLOWED_TIMESTAMP_DRIFT_SECONDS) {
          log.warn("[SendGridWebhook] Webhook timestamp drift exceeded window: eventTs={}, now={}", eventTs, now);
          return false;
        }
      } catch (NumberFormatException ex) {
        log.warn("[SendGridWebhook] Malformed timestamp header: {}", headerTimestamp);
        return false;
      }
    }

    // 2. Strict gating of test signatures: strictly permitted ONLY in authorized automated test environments
    if (properties.isSyntheticTestSignatureAllowed()) {
      if (TEST_SENDGRID_SIGNATURE.equalsIgnoreCase(headerSig) || TEST_SENDGRID_SIGNATURE.equalsIgnoreCase(headerToken)) {
        log.debug("[SendGridWebhook] Accepted test signature under authorized test environment");
        return true;
      }
    }

    if (headerSig == null || headerSig.isBlank()) {
      log.warn("[SendGridWebhook] Missing {} header", HEADER_SENDGRID_SIGNATURE);
      return false;
    }

    if (headerTimestamp == null || headerTimestamp.isBlank()) {
      log.warn("[SendGridWebhook] Missing {} header required for ECDSA signature verification", HEADER_SENDGRID_TIMESTAMP);
      return false;
    }

    String verificationKeyStr = resolveSendGridWebhookVerificationKey();
    if (verificationKeyStr == null || verificationKeyStr.isBlank()) {
      log.error("[SendGridWebhook] Fail-closed: SendGrid webhook verification public key is not configured (set communication.sendgrid.webhookVerificationKey or SENDGRID_WEBHOOK_VERIFICATION_KEY)");
      return false;
    }

    return verifySendGridEcdsaSignature(bodyBytes, headerTimestamp.trim(), headerSig.trim(), verificationKeyStr);
  }

  /**
   * Cryptographically verifies SendGrid's ECDSA SHA256 signature against {@code timestamp + bodyBytes}.
   */
  public boolean verifySendGridEcdsaSignature(byte[] bodyBytes, String timestamp, String signatureBase64, String publicKeyStr) {
    try {
      PublicKey publicKey = parseEcdsaPublicKey(publicKeyStr);
      Signature verifier = Signature.getInstance("SHA256withECDSA");
      verifier.initVerify(publicKey);
      verifier.update(timestamp.getBytes(StandardCharsets.UTF_8));
      if (bodyBytes != null && bodyBytes.length > 0) {
        verifier.update(bodyBytes);
      }
      byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
      boolean verified = verifier.verify(sigBytes);
      if (!verified) {
        log.warn("[SendGridWebhook] Cryptographic ECDSA signature verification returned false");
      }
      return verified;
    } catch (Exception ex) {
      log.warn("[SendGridWebhook] ECDSA signature verification error: {}", ex.getMessage());
      return false;
    }
  }

  /**
   * Parses an ECDSA public key from Base64 SubjectPublicKeyInfo (X.509) format
   * or raw 65-byte uncompressed EC point (0x04 || X || Y) on curve secp256r1.
   */
  public static PublicKey parseEcdsaPublicKey(String rawKey) throws Exception {
    if (rawKey == null || rawKey.isBlank()) {
      throw new IllegalArgumentException("SendGrid public key string is null or empty");
    }
    String cleanKey = rawKey.trim()
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s+", "");

    byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
    KeyFactory keyFactory = KeyFactory.getInstance("EC");

    try {
      return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (InvalidKeySpecException ex) {
      if (keyBytes.length == 65 && keyBytes[0] == 0x04) {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParams = params.getParameterSpec(ECParameterSpec.class);
        byte[] xBytes = Arrays.copyOfRange(keyBytes, 1, 33);
        byte[] yBytes = Arrays.copyOfRange(keyBytes, 33, 65);
        ECPoint point = new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes));
        return keyFactory.generatePublic(new ECPublicKeySpec(point, ecParams));
      }
      throw ex;
    }
  }

  private String resolveSendGridWebhookVerificationKey() {
    if (properties.getSendgrid() != null
        && properties.getSendgrid().getWebhookVerificationKey() != null
        && !properties.getSendgrid().getWebhookVerificationKey().isBlank()) {
      return properties.getSendgrid().getWebhookVerificationKey().trim();
    }
    String envKey = System.getenv("SENDGRID_WEBHOOK_VERIFICATION_KEY");
    if (envKey != null && !envKey.isBlank()) {
      return envKey.trim();
    }
    return null;
  }
}
