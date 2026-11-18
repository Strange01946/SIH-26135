package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates Twilio webhook authenticity using standard Twilio HMAC-SHA1 signature verification.
 *
 * <p>Twilio HMAC-SHA1 algorithm:
 * <ol>
 *   <li>Take the full requested URL including query parameters.</li>
 *   <li>Sort POST parameters alphabetically by name.</li>
 *   <li>Append key and value to the URL string without delimiters.</li>
 *   <li>Compute HMAC-SHA1 using the Twilio Auth Token as the secret key.</li>
 *   <li>Base64-encode and compare against the {@code X-Twilio-Signature} header using constant-time comparison.</li>
 * </ol>
 */
@Component
public class TwilioWebhookSignatureValidator implements WebhookSignatureValidator {

  private static final Logger log = LoggerFactory.getLogger(TwilioWebhookSignatureValidator.class);
  public static final String HEADER_TWILIO_SIGNATURE = "X-Twilio-Signature";
  public static final String TEST_TWILIO_SIGNATURE = "test-twilio-signature";

  private final CommunicationProperties properties;

  @Autowired
  public TwilioWebhookSignatureValidator(CommunicationProperties properties) {
    this.properties = properties;
  }

  @Override
  public String getSupportedProvider() {
    return "twilio";
  }

  @Override
  public boolean validateSignature(HttpServletRequest request, byte[] bodyBytes) {
    if (request == null) {
      return false;
    }

    String headerSignature = request.getHeader(HEADER_TWILIO_SIGNATURE);
    if (headerSignature == null || headerSignature.isBlank()) {
      log.warn("[TwilioWebhook] Missing {} header", HEADER_TWILIO_SIGNATURE);
      return false;
    }

    // Support deterministic test signature in DEMO mode or test environments
    if (properties.isDemoMode() && TEST_TWILIO_SIGNATURE.equalsIgnoreCase(headerSignature.trim())) {
      log.debug("[TwilioWebhook] Accepted test signature in DEMO mode");
      return true;
    }

    String authToken = resolveTwilioAuthToken();
    if (authToken == null || authToken.isBlank()) {
      if (properties.isDemoMode()) {
        log.warn("[TwilioWebhook] No Twilio Auth Token configured in DEMO mode; rejecting non-test signature");
        return false;
      }
      log.error("[TwilioWebhook] Fail-closed: Twilio Auth Token is not configured for signature verification in LIVE mode");
      return false;
    }

    try {
      String expectedSignature = computeTwilioSignature(request, authToken);
      boolean valid = MessageDigest.isEqual(
          headerSignature.trim().getBytes(StandardCharsets.UTF_8),
          expectedSignature.getBytes(StandardCharsets.UTF_8)
      );

      if (!valid) {
        log.warn("[TwilioWebhook] Invalid signature received from {}", request.getRemoteAddr());
      }
      return valid;
    } catch (Exception ex) {
      log.error("[TwilioWebhook] Error computing HMAC-SHA1 signature: {}", ex.getMessage(), ex);
      return false;
    }
  }

  public String computeTwilioSignature(HttpServletRequest request, String authToken) throws Exception {
    // 1. Build URL
    StringBuffer urlBuffer = request.getRequestURL();
    String queryString = request.getQueryString();
    if (queryString != null && !queryString.isBlank()) {
      urlBuffer.append("?").append(queryString);
    }
    StringBuilder dataToSign = new StringBuilder(urlBuffer.toString());

    // 2. Sort POST parameters
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap != null && !parameterMap.isEmpty()) {
      List<String> sortedKeys = new ArrayList<>(parameterMap.keySet());
      Collections.sort(sortedKeys);
      for (String key : sortedKeys) {
        String[] values = parameterMap.get(key);
        if (values != null && values.length > 0) {
          dataToSign.append(key).append(values[0]);
        }
      }
    }

    // 3. HMAC-SHA1 computation
    Mac mac = Mac.getInstance("HmacSHA1");
    SecretKeySpec secretKey = new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
    mac.init(secretKey);
    byte[] rawHmac = mac.doFinal(dataToSign.toString().getBytes(StandardCharsets.UTF_8));

    // 4. Base64 encode
    return Base64.getEncoder().encodeToString(rawHmac);
  }

  private String resolveTwilioAuthToken() {
    if (properties.getTwilio() != null && properties.getTwilio().getAuthToken() != null && !properties.getTwilio().getAuthToken().isBlank()) {
      return properties.getTwilio().getAuthToken().trim();
    }
    String envToken = System.getenv("TWILIO_AUTH_TOKEN");
    if (envToken != null && !envToken.isBlank()) {
      return envToken.trim();
    }
    return null;
  }
}
