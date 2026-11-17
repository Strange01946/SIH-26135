package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Concrete SMTP Email provider implementing real outbound email delivery
 * via authenticated Gmail SMTP (or configured custom SMTP host).
 *
 * <p>Standard Port: 587 with STARTTLS enabled.
 * <p>Authentication: Gmail Username and App Password.
 */
@Component
public class SmtpEmailProvider implements EmailCommunicationProvider {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

  private final CommunicationProperties properties;
  private JavaMailSender customMailSender;

  @Autowired
  public SmtpEmailProvider(CommunicationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
  }

  /**
   * Package-private or test setter to inject a mocked/configured JavaMailSender.
   */
  void setMailSender(JavaMailSender mailSender) {
    this.customMailSender = mailSender;
  }

  private JavaMailSender getMailSender() {
    if (customMailSender != null) {
      return customMailSender;
    }

    CommunicationProperties.SmtpConfig config = properties.getSmtp();
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(config.getHost());
    sender.setPort(config.getPort());
    sender.setUsername(config.getUsername());
    sender.setPassword(config.getPassword());
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", String.valueOf(config.isAuth()));
    props.put("mail.smtp.starttls.enable", String.valueOf(config.isStarttls()));
    props.put("mail.smtp.starttls.required", String.valueOf(config.isStarttls()));
    props.put("mail.smtp.connectiontimeout", config.getConnectionTimeoutMs());
    props.put("mail.smtp.timeout", config.getTimeoutMs());
    props.put("mail.smtp.writetimeout", config.getWriteTimeoutMs());

    return sender;
  }

  @Override
  public ProviderDeliveryResult sendEmail(String recipientEmail, String subject, String bodyText, String bodyHtml) {
    CommunicationProperties.SmtpConfig config = properties.getSmtp();

    if (!config.isConfigured() && customMailSender == null) {
      String msg = "SMTP credentials (SMTP_USERNAME, SMTP_APP_PASSWORD, SMTP_FROM_EMAIL) are not configured";
      log.error("[SmtpEmailProvider] Live delivery failed: {}", msg);
      throw new IllegalStateException(msg);
    }

    if (recipientEmail == null || recipientEmail.isBlank()) {
      throw new BadRequestException("Recipient email address is required for Email delivery", "RECIPIENT_EMAIL_REQUIRED");
    }

    String targetEmail = recipientEmail.trim();
    if (properties.isLiveRecipientOverrideEnabled()) {
      String overrideEmail = config.getLiveTestEmail();
      if (overrideEmail == null || overrideEmail.isBlank()) {
        log.error("[SmtpEmailProvider] FATAL: Live recipient override is enabled, but required test email is missing (SIH_LIVE_TEST_EMAIL)");
        throw new IllegalStateException("Live recipient override is enabled, but required override test email is missing or blank. Refusing to dispatch to real candidate contact.");
      }
      targetEmail = overrideEmail.trim();
      log.info("[SmtpEmailProvider] Safe trial override: routing dispatch from {} to authorized test email {}",
          CommunicationTriggerEvent.maskRecipient(recipientEmail),
          CommunicationTriggerEvent.maskRecipient(targetEmail));
    }

    log.info("[SmtpEmailProvider] Initiating outbound SMTP email to {} with subject '{}'",
        CommunicationTriggerEvent.maskRecipient(targetEmail), subject);

    String fromAddress = (config.getFromEmail() != null && !config.getFromEmail().isBlank())
        ? config.getFromEmail().trim()
        : config.getUsername();

    String messageId = "SMTP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

    try {
      JavaMailSender mailSender = getMailSender();
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

      helper.setFrom(fromAddress, "Skill India Platform");
      helper.setTo(targetEmail);
      helper.setSubject(subject);

      if (bodyHtml != null && !bodyHtml.isBlank()) {
        helper.setText(bodyText != null ? bodyText : "", bodyHtml);
      } else {
        helper.setText(bodyText != null ? bodyText : "", false);
      }

      mimeMessage.setHeader("X-Message-ID", messageId);
      mailSender.send(mimeMessage);

      log.info("[SmtpEmailProvider] Outbound email accepted by SMTP server: messageId={}", messageId);
      return ProviderDeliveryResult.success(
          CommunicationChannel.EMAIL,
          getProviderName(),
          messageId,
          "SENT",
          "{\"status\":250,\"messageId\":\"" + messageId + "\",\"protocol\":\"SMTP\"}"
      );

    } catch (Exception ex) {
      String sanitizedMsg = CommunicationTriggerEvent.maskRecipient(ex.getMessage());
      log.error("[SmtpEmailProvider] SMTP delivery error: {}", sanitizedMsg, ex);
      return ProviderDeliveryResult.failure(
          CommunicationChannel.EMAIL,
          getProviderName(),
          null,
          "SMTP_DELIVERY_FAILURE",
          sanitizedMsg,
          null
      );
    }
  }

  @Override
  public String getProviderName() {
    return "SmtpEmailProvider";
  }
}
