package in.gov.sih.sih26135.communication.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Concrete SendGrid Email provider implementing real outbound email delivery
 * via Twilio SendGrid v3 Mail Send API.
 *
 * <p>API Endpoint: {@code POST https://api.sendgrid.com/v3/mail/send}
 * <p>Authentication: {@code Authorization: Bearer <SENDGRID_API_KEY>}
 */
@Component
public class SendGridEmailProvider implements EmailCommunicationProvider {

  private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CommunicationProperties properties;
  private RestClient restClient;

  @Autowired
  public SendGridEmailProvider(CommunicationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    initRestClient();
  }

  public void initRestClient() {
    CommunicationProperties.SendGridConfig config = properties.getSendgrid();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()));
    factory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMs()));

    this.restClient = RestClient.builder()
        .baseUrl(config.getBaseUrl())
        .requestFactory(factory)
        .build();
  }

  @Override
  public ProviderDeliveryResult sendEmail(String recipientEmail, String subject, String bodyText, String bodyHtml) {
    CommunicationProperties.SendGridConfig config = properties.getSendgrid();

    if (!config.isConfigured()) {
      String msg = "SendGrid credentials (SENDGRID_API_KEY, SENDGRID_FROM_EMAIL) are not configured";
      log.error("[SendGridEmailProvider] Live delivery failed: {}", msg);
      throw new IllegalStateException(msg);
    }

    if (recipientEmail == null || recipientEmail.isBlank()) {
      throw new BadRequestException("Recipient email address is required for Email delivery", "RECIPIENT_EMAIL_REQUIRED");
    }

    String targetEmail = recipientEmail.trim();
    if (properties.isLiveRecipientOverrideEnabled()) {
      String overrideEmail = config.getLiveTestEmail();
      if (overrideEmail == null || overrideEmail.isBlank()) {
        log.error("[SendGridEmailProvider] FATAL: Live recipient override is enabled, but required test email is missing (SIH_LIVE_TEST_EMAIL)");
        throw new IllegalStateException("Live recipient override is enabled, but required override test email is missing or blank. Refusing to dispatch to real candidate contact.");
      }
      targetEmail = overrideEmail.trim();
      log.info("[SendGridEmailProvider] Safe trial override: routing dispatch from {} to authorized test email {}",
          CommunicationTriggerEvent.maskRecipient(recipientEmail),
          CommunicationTriggerEvent.maskRecipient(targetEmail));
    }

    log.info("[SendGridEmailProvider] Initiating outbound email to {} with subject '{}'",
        CommunicationTriggerEvent.maskRecipient(targetEmail), subject);

    ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();

    // personalizations
    ArrayNode personalizations = rootNode.putArray("personalizations");
    ObjectNode personalization = personalizations.addObject();
    ArrayNode toList = personalization.putArray("to");
    toList.addObject().put("email", targetEmail);

    // from
    rootNode.putObject("from").put("email", config.getFromEmail().trim());

    // subject
    rootNode.put("subject", subject);

    // content
    ArrayNode contentList = rootNode.putArray("content");
    contentList.addObject()
        .put("type", "text/plain")
        .put("value", bodyText != null ? bodyText : "");

    if (bodyHtml != null && !bodyHtml.isBlank()) {
      contentList.addObject()
          .put("type", "text/html")
          .put("value", bodyHtml);
    }

    try {
      ResponseEntity<Void> response = restClient.post()
          .uri("/v3/mail/send")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey().trim())
          .contentType(MediaType.APPLICATION_JSON)
          .body(rootNode.toString())
          .retrieve()
          .toBodilessEntity();

      String messageId = null;
      if (response.getHeaders().containsKey("X-Message-Id")) {
        messageId = response.getHeaders().getFirst("X-Message-Id");
      }
      if (messageId == null || messageId.isBlank()) {
        messageId = "SG_" + UUID.randomUUID().toString().replace("-", "");
      }

      log.info("[SendGridEmailProvider] Outbound email accepted by SendGrid: httpStatus={}, messageId={}",
          response.getStatusCode(), messageId);

      return ProviderDeliveryResult.success(
          CommunicationChannel.EMAIL,
          getProviderName(),
          messageId,
          "QUEUED",
          "{\"status\":202,\"messageId\":\"" + messageId + "\"}"
      );

    } catch (RestClientResponseException ex) {
      String rawBody = ex.getResponseBodyAsString();
      String sanitizedBody = CommunicationTriggerEvent.maskRecipient(rawBody);
      log.error("[SendGridEmailProvider] SendGrid API rejected request: httpStatus={}, errorBody={}",
          ex.getStatusCode(), sanitizedBody);

      return ProviderDeliveryResult.failure(
          CommunicationChannel.EMAIL,
          getProviderName(),
          null,
          String.valueOf(ex.getStatusCode().value()),
          sanitizedBody,
          sanitizedBody
      );

    } catch (Exception ex) {
      String sanitizedMsg = CommunicationTriggerEvent.maskRecipient(ex.getMessage());
      log.error("[SendGridEmailProvider] Network or connection error during SendGrid dispatch: {}", sanitizedMsg, ex);
      return ProviderDeliveryResult.failure(
          CommunicationChannel.EMAIL,
          getProviderName(),
          null,
          "SENDGRID_CONNECTION_ERROR",
          sanitizedMsg,
          null
      );
    }
  }

  @Override
  public String getProviderName() {
    return "SendGridEmailProvider";
  }
}
