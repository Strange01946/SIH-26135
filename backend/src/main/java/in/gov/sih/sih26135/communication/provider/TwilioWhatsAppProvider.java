package in.gov.sih.sih26135.communication.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.time.Duration;
import java.util.Map;
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
 * Concrete Twilio WhatsApp provider implementing real outbound WhatsApp messaging
 * via Twilio Messaging API and Twilio WhatsApp Sandbox.
 *
 * <p>API Endpoint: {@code POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json}
 * <p>Addressing Convention: {@code whatsapp:+14155238886} (Sender) and {@code whatsapp:+91...} (Recipient).
 */
@Component
public class TwilioWhatsAppProvider implements WhatsAppCommunicationProvider {

  private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppProvider.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CommunicationProperties properties;
  private RestClient restClient;

  @Autowired
  public TwilioWhatsAppProvider(CommunicationProperties properties) {
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
  public ProviderDeliveryResult sendWhatsApp(String recipientPhone, String messageText, String templateCode) {
    CommunicationProperties.TwilioConfig config = properties.getTwilio();

    String accountSid = config.getAccountSid();
    String authToken = config.getAuthToken();
    String fromWhatsApp = config.getWhatsappFrom();

    if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
      String msg = "Twilio WhatsApp credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN) are not configured";
      log.error("[TwilioWhatsAppProvider] Live delivery failed: {}", msg);
      throw new IllegalStateException(msg);
    }

    if (fromWhatsApp == null || fromWhatsApp.isBlank()) {
      String msg = "Twilio WhatsApp sender (TWILIO_WHATSAPP_FROM / communication.twilio.whatsapp-from) is not configured";
      log.error("[TwilioWhatsAppProvider] Live delivery failed: {}", msg);
      throw new IllegalStateException(msg);
    }

    if (recipientPhone == null || recipientPhone.isBlank()) {
      throw new BadRequestException("Recipient phone number is required for WhatsApp delivery", "RECIPIENT_PHONE_REQUIRED");
    }

    String formattedRecipient = formatWhatsAppAddress(recipientPhone);
    String targetAddress = formattedRecipient;

    if (properties.isLiveRecipientOverrideEnabled()) {
      String overridePhone = config.getLiveTestWhatsapp();
      if (overridePhone == null || overridePhone.isBlank()) {
        log.error("[TwilioWhatsAppProvider] FATAL: Live recipient override is enabled, but required test WhatsApp number is missing (SIH_LIVE_TEST_WHATSAPP)");
        throw new IllegalStateException("Live recipient override is enabled, but required override test WhatsApp number is missing or blank. Refusing to dispatch to real candidate contact.");
      }
      targetAddress = formatWhatsAppAddress(overridePhone);
      log.info("[TwilioWhatsAppProvider] Safe trial override: routing dispatch from {} to authorized test WhatsApp {}",
          CommunicationTriggerEvent.maskRecipient(formattedRecipient),
          CommunicationTriggerEvent.maskRecipient(targetAddress));
    }

    String formattedSender = fromWhatsApp.startsWith("whatsapp:") ? fromWhatsApp : "whatsapp:" + fromWhatsApp;

    log.info("[TwilioWhatsAppProvider] Initiating outbound WhatsApp message to {} using template {}",
        CommunicationTriggerEvent.maskRecipient(targetAddress), templateCode);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("From", formattedSender);
    formData.add("To", targetAddress);

    String contentSid = config.getWhatsappContentSid();
    if (contentSid != null && !contentSid.isBlank()) {
      formData.add("ContentSid", contentSid);
      String customVars = config.getWhatsappContentVariables();
      if (customVars != null && !customVars.isBlank()) {
        formData.add("ContentVariables", customVars.trim());
      } else {
        Map<String, String> defaultVariables = Map.of(
            "1", "Tue Oct 29, 3:00 PM"
        );
        try {
          formData.add("ContentVariables", OBJECT_MAPPER.writeValueAsString(defaultVariables));
        } catch (Exception ex) {
          log.warn("[TwilioWhatsAppProvider] Could not serialize ContentVariables: {}", ex.getMessage());
        }
      }
      log.info("[TwilioWhatsAppProvider] Dispatching via Content API template: contentSid={}", contentSid);
    } else {
      formData.add("Body", messageText);
    }

    try {
      String responseBody = restClient.post()
          .uri("/Accounts/{accountSid}/Messages.json", accountSid)
          .headers(h -> h.setBasicAuth(accountSid, authToken))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(String.class);

      JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
      String sid = jsonNode.hasNonNull("sid") ? jsonNode.get("sid").asText() : null;
      String rawStatus = jsonNode.hasNonNull("status") ? jsonNode.get("status").asText() : "queued";
      String mappedStatus = TwilioSmsProvider.mapTwilioStatus(rawStatus);

      log.info("[TwilioWhatsAppProvider] Outbound WhatsApp message accepted by Twilio API: sid={}, rawStatus={}, mappedStatus={}", sid, rawStatus, mappedStatus);
      return ProviderDeliveryResult.success(
          CommunicationChannel.WHATSAPP,
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
      log.error("[TwilioWhatsAppProvider] Twilio API rejected WhatsApp request: httpStatus={}, twilioCode={}, errorMsg={}",
          ex.getStatusCode(), parsedErrorCode, sanitizedErrorMsg);

      return ProviderDeliveryResult.failure(
          CommunicationChannel.WHATSAPP,
          getProviderName(),
          null,
          parsedErrorCode,
          sanitizedErrorMsg,
          sanitizeTwilioResponse(rawBody)
      );

    } catch (Exception ex) {
      String sanitizedMsg = CommunicationTriggerEvent.maskRecipient(ex.getMessage());
      log.error("[TwilioWhatsAppProvider] Network or connection error during WhatsApp dispatch: {}", sanitizedMsg, ex);
      return ProviderDeliveryResult.failure(
          CommunicationChannel.WHATSAPP,
          getProviderName(),
          null,
          "TWILIO_WHATSAPP_CONNECTION_ERROR",
          sanitizedMsg,
          null
      );
    }
  }

  @Override
  public String getProviderName() {
    return "TwilioWhatsAppProvider";
  }

  private String formatWhatsAppAddress(String phone) {
    if (phone == null) {
      return "";
    }
    String cleaned = phone.trim();
    if (cleaned.startsWith("whatsapp:")) {
      cleaned = cleaned.substring("whatsapp:".length());
    }
    cleaned = cleaned.replaceAll("[^0-9+]", "");
    if (!cleaned.startsWith("+")) {
      if (cleaned.length() == 10) {
        cleaned = "+91" + cleaned;
      } else {
        cleaned = "+" + cleaned;
      }
    }
    return "whatsapp:" + cleaned;
  }

  private String sanitizeTwilioResponse(String rawJson) {
    if (rawJson == null) {
      return null;
    }
    return rawJson.replaceAll("(?i)(\"auth_token\"\\s*:\\s*\")[^\"]+(\")", "$1[REDACTED]$2");
  }
}
