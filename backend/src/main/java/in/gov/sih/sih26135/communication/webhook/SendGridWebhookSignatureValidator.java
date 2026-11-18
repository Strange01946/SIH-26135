package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates SendGrid webhook delivery event authenticity and timestamp freshness.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Verification of timestamp freshness within a 10-minute sliding window (replay attack mitigation).</li>
 *   <li>Verification of webhook signature token header ({@code X-SendGrid-Token} or webhook verification key).</li>
 *   <li>Deterministic mock signature acceptance in DEMO mode.</li>
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

    // 1. Verify timestamp freshness if timestamp header is supplied (replay attack mitigation)
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

    // 2. Support test signature in DEMO mode
    if (properties.isDemoMode()) {
      if (TEST_SENDGRID_SIGNATURE.equalsIgnoreCase(headerSig) || TEST_SENDGRID_SIGNATURE.equalsIgnoreCase(headerToken)) {
        return true;
      }
    }

    String configuredSecret = resolveSendGridWebhookSecret();
    if (configuredSecret == null || configuredSecret.isBlank()) {
      if (properties.isDemoMode()) {
        log.warn("[SendGridWebhook] No SendGrid webhook secret configured in DEMO mode; rejecting non-test signature");
        return false;
      }
      log.error("[SendGridWebhook] Fail-closed: SendGrid webhook secret is not configured in LIVE mode");
      return false;
    }

    // Shared token or signature check
    if (headerToken != null && !headerToken.isBlank()) {
      return MessageDigest.isEqual(
          headerToken.trim().getBytes(StandardCharsets.UTF_8),
          configuredSecret.getBytes(StandardCharsets.UTF_8)
      );
    }

    if (headerSig != null && !headerSig.isBlank()) {
      return MessageDigest.isEqual(
          headerSig.trim().getBytes(StandardCharsets.UTF_8),
          configuredSecret.getBytes(StandardCharsets.UTF_8)
      );
    }

    log.warn("[SendGridWebhook] Missing both {} and {} headers", HEADER_SENDGRID_SIGNATURE, HEADER_SENDGRID_TOKEN);
    return false;
  }

  private String resolveSendGridWebhookSecret() {
    String envSecret = System.getenv("SENDGRID_WEBHOOK_VERIFICATION_KEY");
    if (envSecret != null && !envSecret.isBlank()) {
      return envSecret.trim();
    }
    String envApiKey = System.getenv("SENDGRID_API_KEY");
    if (envApiKey != null && !envApiKey.isBlank()) {
      return envApiKey.trim();
    }
    return null;
  }
}
