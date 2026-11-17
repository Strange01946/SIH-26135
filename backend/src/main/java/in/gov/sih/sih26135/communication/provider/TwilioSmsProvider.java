package in.gov.sih.sih26135.communication.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Concrete Twilio SMS provider implementing real outbound SMS delivery
 * via Twilio Programmable Messaging API.
 *
 * <p>API Endpoint: {@code POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json}
 * <p>Authentication: HTTP Basic Authentication using Account SID and Auth Token.
 */
@Component
public class TwilioSmsProvider implements SmsCommunicationProvider {

  private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CommunicationProperties properties;
  private RestClient restClient;

  @Autowired
  public TwilioSmsProvider(CommunicationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    initRestClient();
  }

  private void initRestClient() {
    CommunicationProperties.TwilioConfig config = properties.getTwilio();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()));
    factory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMs()));

    this.restClient = RestClient.builder()
        .baseUrl("https://api.twilio.com/2010-04-01")
        .requestFactory(factory)
        .build();
  }

  @Override
  public ProviderDeliveryResult sendSms(String recipientPhone, String messageText, String templateCode) {
    CommunicationProperties.TwilioConfig config = properties.getTwilio();

    if (!config.isConfigured()) {
      String msg = "Twilio SMS credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_SMS_FROM) are not configured";
      log.error("[TwilioSmsProvider] Live delivery failed: {}", msg);
      throw new IllegalStateException(msg);
    }

    if (recipientPhone == null || recipientPhone.isBlank()) {
      throw new BadRequestException("Recipient phone number is required for SMS delivery", "RECIPIENT_PHONE_REQUIRED");
    }

    String formattedRecipient = formatE164(recipientPhone);
    String targetPhone = formattedRecipient;

    if (properties.isLiveRecipientOverrideEnabled()) {
      String overridePhone = config.getLiveTestPhone();
      if (overridePhone == null || overridePhone.isBlank()) {
        log.error("[TwilioSmsProvider] FATAL: Live recipient override is enabled, but required test phone is missing (SIH_LIVE_TEST_PHONE)");
        throw new IllegalStateException("Live recipient override is enabled, but required override test phone is missing or blank. Refusing to dispatch to real candidate contact.");
      }
      targetPhone = formatE164(overridePhone);
      log.info("[TwilioSmsProvider] Safe trial override: routing dispatch from {} to authorized test phone {}",
          CommunicationTriggerEvent.maskRecipient(formattedRecipient),
          CommunicationTriggerEvent.maskRecipient(targetPhone));
    }

    log.info("[TwilioSmsProvider] Initiating outbound SMS dispatch to {} using template {}",
        CommunicationTriggerEvent.maskRecipient(targetPhone), templateCode);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("From", config.getSmsFrom().trim());
    formData.add("To", targetPhone);
    formData.add("Body", messageText);

    try {
      String responseBody = restClient.post()
          .uri("/Accounts/{accountSid}/Messages.json", config.getAccountSid())
          .headers(h -> h.setBasicAuth(config.getAccountSid(), config.getAuthToken()))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(String.class);

      JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
      String sid = jsonNode.hasNonNull("sid") ? jsonNode.get("sid").asText() : null;
      String rawStatus = jsonNode.hasNonNull("status") ? jsonNode.get("status").asText() : "queued";
      String mappedStatus = mapTwilioStatus(rawStatus);

      log.info("[TwilioSmsProvider] Outbound SMS accepted by Twilio API: sid={}, rawStatus={}, mappedStatus={}", sid, rawStatus, mappedStatus);
      return ProviderDeliveryResult.success(
          CommunicationChannel.SMS,
          getProviderName(),
          sid,
          mappedStatus,
          sanitizeTwilioResponse(responseBody)
      );

    } catch (RestClientResponseException ex) {
      String rawBody = ex.getResponseBodyAsString();
      String parsedErrorCode = String.valueOf(ex.getStatusCode().value());
      String parsedErrorMsg = ex.getStatusText();

      try {
        JsonNode errJson = OBJECT_MAPPER.readTree(rawBody);
        if (errJson.hasNonNull("code")) {
          parsedErrorCode = errJson.get("code").asText();
        }
        if (errJson.hasNonNull("message")) {
          parsedErrorMsg = errJson.get("message").asText();
        }
      } catch (Exception ignored) {
      }

      String sanitizedErrorMsg = CommunicationTriggerEvent.maskRecipient(parsedErrorMsg);
      log.error("[TwilioSmsProvider] Twilio API rejected SMS request: httpStatus={}, twilioCode={}, errorMsg={}",
          ex.getStatusCode(), parsedErrorCode, sanitizedErrorMsg);

      return ProviderDeliveryResult.failure(
          CommunicationChannel.SMS,
          getProviderName(),
          null,
          parsedErrorCode,
          sanitizedErrorMsg,
          sanitizeTwilioResponse(rawBody)
      );

    } catch (Exception ex) {
      String sanitizedMsg = CommunicationTriggerEvent.maskRecipient(ex.getMessage());
      log.error("[TwilioSmsProvider] Network or connection error during SMS dispatch: {}", sanitizedMsg, ex);
      return ProviderDeliveryResult.failure(
          CommunicationChannel.SMS,
          getProviderName(),
          null,
          "TWILIO_CONNECTION_ERROR",
          sanitizedMsg,
          null
      );
    }
  }

  @Override
  public String getProviderName() {
    return "TwilioSmsProvider";
  }

  public static String mapTwilioStatus(String twilioStatus) {
    if (twilioStatus == null) {
      return "QUEUED";
    }
    return switch (twilioStatus.trim().toLowerCase()) {
      case "queued", "accepted" -> "QUEUED";
      case "sending" -> "ATTEMPTED";
      case "sent" -> "SENT";
      case "delivered" -> "DELIVERED";
      case "failed", "undelivered" -> "FAILED";
      default -> "QUEUED";
    };
  }

  private String formatE164(String phone) {
    if (phone == null) {
      return "";
    }
    String cleaned = phone.replaceAll("[^0-9+]", "");
    if (cleaned.startsWith("+")) {
      return cleaned;
    }
    if (cleaned.length() == 10) {
      return "+91" + cleaned; // Standard Indian E.164 country code
    }
    return "+" + cleaned;
  }

  private String sanitizeTwilioResponse(String rawJson) {
    if (rawJson == null) {
      return null;
    }
    // Mask sensitive fields if present
    return rawJson.replaceAll("(?i)(\"auth_token\"\\s*:\\s*\")[^\"]+(\")", "$1[REDACTED]$2");
  }
}
