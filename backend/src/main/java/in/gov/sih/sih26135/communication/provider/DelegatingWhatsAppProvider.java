package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Primary delegating WhatsApp provider that routes dispatches to {@link TwilioWhatsAppProvider}
 * when in {@code LIVE} mode, or to {@link MockWhatsAppProvider} when in {@code MOCK} test mode.
 */
@Component
@Primary
public class DelegatingWhatsAppProvider implements WhatsAppCommunicationProvider {

  private final CommunicationProperties properties;
  private final TwilioWhatsAppProvider twilioWhatsAppProvider;
  private final MockWhatsAppProvider mockWhatsAppProvider;

  @Autowired
  public DelegatingWhatsAppProvider(
      CommunicationProperties properties,
      TwilioWhatsAppProvider twilioWhatsAppProvider,
      MockWhatsAppProvider mockWhatsAppProvider) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    this.twilioWhatsAppProvider = Objects.requireNonNull(twilioWhatsAppProvider, "TwilioWhatsAppProvider is required");
    this.mockWhatsAppProvider = Objects.requireNonNull(mockWhatsAppProvider, "MockWhatsAppProvider is required");
  }

  @Override
  public ProviderDeliveryResult sendWhatsApp(String recipientPhone, String messageText, String templateCode) {
    if (properties.isLiveMode()) {
      return twilioWhatsAppProvider.sendWhatsApp(recipientPhone, messageText, templateCode);
    }
    return mockWhatsAppProvider.sendWhatsApp(recipientPhone, messageText, templateCode);
  }

  @Override
  public String getProviderName() {
    return properties.isLiveMode() ? twilioWhatsAppProvider.getProviderName() : mockWhatsAppProvider.getProviderName();
  }
}
