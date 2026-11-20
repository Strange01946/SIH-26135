package in.gov.sih.sih26135.actuator.health;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Passive health indicator for the communication infrastructure (SMS, WhatsApp, Email).
 *
 * <p>Principles:
 * <ul>
 *   <li>Passive inspection of configured provider routing (MOCK, DEMO, LIVE).</li>
 *   <li>Strictly zero live outbound message transmissions, zero paid API credit consumption.</li>
 *   <li>Zero exposure of provider credentials (tokens, API keys, passwords, or recipient phone numbers).</li>
 *   <li>A disabled or mock provider is an expected operational mode and does not mark the application DOWN.</li>
 * </ul>
 */
@Component("communicationGateway")
public class CommunicationGatewayHealthIndicator implements HealthIndicator {

  private final CommunicationProperties communicationProperties;

  @Autowired
  public CommunicationGatewayHealthIndicator(CommunicationProperties communicationProperties) {
    this.communicationProperties = Objects.requireNonNull(communicationProperties, "CommunicationProperties is required");
  }

  @Override
  public Health health() {
    Map<String, Object> details = new LinkedHashMap<>();

    CommunicationProperties.ProviderMode mode = communicationProperties.getMode();
    details.put("mode", mode.name());
    details.put("emailProviderType", communicationProperties.getEmailProviderType().name());
    details.put("liveOverrideEnabled", communicationProperties.isLiveRecipientOverrideEnabled());

    Map<String, Object> providers = new LinkedHashMap<>();

    // SMS & WhatsApp Gateway (Twilio)
    boolean twilioConfigured = communicationProperties.getTwilio() != null
        && communicationProperties.getTwilio().getAccountSid() != null
        && !communicationProperties.getTwilio().getAccountSid().isBlank();
    providers.put("smsGatewayConfigured", twilioConfigured);
    providers.put("whatsAppGatewayConfigured", twilioConfigured);

    // Email Gateway (SendGrid / SMTP)
    boolean sendgridConfigured = communicationProperties.getSendgrid() != null
        && communicationProperties.getSendgrid().getApiKey() != null
        && !communicationProperties.getSendgrid().getApiKey().isBlank();
    boolean smtpConfigured = communicationProperties.getSmtp() != null
        && communicationProperties.getSmtp().getHost() != null
        && !communicationProperties.getSmtp().getHost().isBlank();
    providers.put("sendgridConfigured", sendgridConfigured);
    providers.put("smtpConfigured", smtpConfigured);

    details.put("providers", providers);

    return Health.up()
        .withDetails(details)
        .build();
  }
}
