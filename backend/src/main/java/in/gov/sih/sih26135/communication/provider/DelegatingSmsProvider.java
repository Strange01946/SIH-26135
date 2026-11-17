package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Primary delegating SMS provider that routes dispatches to {@link TwilioSmsProvider}
 * when in {@code LIVE} mode, or to {@link MockSmsProvider} when in {@code MOCK} test mode.
 */
@Component
@Primary
public class DelegatingSmsProvider implements SmsCommunicationProvider {

  private final CommunicationProperties properties;
  private final TwilioSmsProvider twilioSmsProvider;
  private final MockSmsProvider mockSmsProvider;

  @Autowired
  public DelegatingSmsProvider(
      CommunicationProperties properties,
      TwilioSmsProvider twilioSmsProvider,
      MockSmsProvider mockSmsProvider) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    this.twilioSmsProvider = Objects.requireNonNull(twilioSmsProvider, "TwilioSmsProvider is required");
    this.mockSmsProvider = Objects.requireNonNull(mockSmsProvider, "MockSmsProvider is required");
  }

  @Override
  public ProviderDeliveryResult sendSms(String recipientPhone, String messageText, String templateCode) {
    if (properties.isLiveMode()) {
      return twilioSmsProvider.sendSms(recipientPhone, messageText, templateCode);
    }
    return mockSmsProvider.sendSms(recipientPhone, messageText, templateCode);
  }

  @Override
  public String getProviderName() {
    return properties.isLiveMode() ? twilioSmsProvider.getProviderName() : mockSmsProvider.getProviderName();
  }
}
