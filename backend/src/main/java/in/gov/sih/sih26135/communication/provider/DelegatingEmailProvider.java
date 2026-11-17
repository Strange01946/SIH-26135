package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Primary delegating Email provider that routes dispatches to {@link SendGridEmailProvider} or
 * {@link SmtpEmailProvider} when in {@code LIVE} mode (based on configuration and preference),
 * or to {@link MockEmailProvider} when in {@code MOCK} test mode.
 */
@Component
@Primary
public class DelegatingEmailProvider implements EmailCommunicationProvider {

  private static final Logger log = LoggerFactory.getLogger(DelegatingEmailProvider.class);

  private final CommunicationProperties properties;
  private final SendGridEmailProvider sendGridEmailProvider;
  private final SmtpEmailProvider smtpEmailProvider;
  private final MockEmailProvider mockEmailProvider;

  @Autowired
  public DelegatingEmailProvider(
      CommunicationProperties properties,
      SendGridEmailProvider sendGridEmailProvider,
      SmtpEmailProvider smtpEmailProvider,
      MockEmailProvider mockEmailProvider) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    this.sendGridEmailProvider = Objects.requireNonNull(sendGridEmailProvider, "SendGridEmailProvider is required");
    this.smtpEmailProvider = Objects.requireNonNull(smtpEmailProvider, "SmtpEmailProvider is required");
    this.mockEmailProvider = Objects.requireNonNull(mockEmailProvider, "MockEmailProvider is required");
  }

  @PostConstruct
  public void init() {
    log.info("[CommunicationEngine] Initialized EmailProvider: mode={}, activeProvider={}, liveRecipientOverrideEnabled={}",
        properties.getMode(), getProviderName(), properties.isLiveRecipientOverrideEnabled());
  }

  @Override
  public ProviderDeliveryResult sendEmail(String recipientEmail, String subject, String bodyText, String bodyHtml) {
    if (properties.isLiveMode()) {
      CommunicationProperties.EmailProviderPreference pref = properties.getEmailProviderType();
      if (pref == CommunicationProperties.EmailProviderPreference.SENDGRID) {
        return sendGridEmailProvider.sendEmail(recipientEmail, subject, bodyText, bodyHtml);
      } else if (pref == CommunicationProperties.EmailProviderPreference.SMTP) {
        return smtpEmailProvider.sendEmail(recipientEmail, subject, bodyText, bodyHtml);
      } else { // AUTO
        if (properties.getSendgrid().isConfigured()) {
          return sendGridEmailProvider.sendEmail(recipientEmail, subject, bodyText, bodyHtml);
        } else {
          return smtpEmailProvider.sendEmail(recipientEmail, subject, bodyText, bodyHtml);
        }
      }
    }
    return mockEmailProvider.sendEmail(recipientEmail, subject, bodyText, bodyHtml);
  }

  @Override
  public String getProviderName() {
    if (properties.isLiveMode()) {
      CommunicationProperties.EmailProviderPreference pref = properties.getEmailProviderType();
      if (pref == CommunicationProperties.EmailProviderPreference.SENDGRID) {
        return sendGridEmailProvider.getProviderName();
      } else if (pref == CommunicationProperties.EmailProviderPreference.SMTP) {
        return smtpEmailProvider.getProviderName();
      } else {
        return properties.getSendgrid().isConfigured()
            ? sendGridEmailProvider.getProviderName()
            : smtpEmailProvider.getProviderName();
      }
    }
    return mockEmailProvider.getProviderName();
  }
}
