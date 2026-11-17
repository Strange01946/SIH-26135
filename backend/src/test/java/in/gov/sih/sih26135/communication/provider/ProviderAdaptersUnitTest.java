package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.template.CommunicationTemplateRegistry;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Provider Adapters & Template Registry Unit Tests")
class ProviderAdaptersUnitTest {

  private CommunicationProperties properties;
  private MockSmsProvider mockSmsProvider;
  private MockWhatsAppProvider mockWhatsAppProvider;
  private MockEmailProvider mockEmailProvider;
  private CommunicationTemplateRegistry templateRegistry;

  @BeforeEach
  void setUp() {
    properties = new CommunicationProperties();
    mockSmsProvider = new MockSmsProvider();
    mockWhatsAppProvider = new MockWhatsAppProvider();
    mockEmailProvider = new MockEmailProvider();
    templateRegistry = new CommunicationTemplateRegistry();
  }

  // --- Mock Provider Tests ---

  @Test
  @DisplayName("MockSmsProvider delivers deterministically and captures dispatched messages")
  void testMockSmsProvider() {
    ProviderDeliveryResult result = mockSmsProvider.sendSms("+919876543210", "Hello Test SMS", "TEST_TEMPLATE");

    assertThat(result.accepted()).isTrue();
    assertThat(result.channel()).isEqualTo(CommunicationChannel.SMS);
    assertThat(result.providerName()).isEqualTo("MockSmsProvider");
    assertThat(result.providerMessageId()).startsWith("SM_MOCK_");
    assertThat(result.statusCode()).isEqualTo("SENT");

    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);
    MockSmsProvider.DispatchedSms dispatched = mockSmsProvider.getLastMessage();
    assertThat(dispatched.recipientPhone()).isEqualTo("+919876543210");
    assertThat(dispatched.messageText()).isEqualTo("Hello Test SMS");

    mockSmsProvider.clear();
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @DisplayName("MockSmsProvider simulated failure returns ProviderDeliveryResult.failure")
  void testMockSmsProviderFailureSimulation() {
    mockSmsProvider.setSimulateFailure(true);
    mockSmsProvider.setFailureDetails("SIMULATED_21608", "Unverified trial destination number");

    ProviderDeliveryResult result = mockSmsProvider.sendSms("+919876543210", "Hello", "TEST");

    assertThat(result.accepted()).isFalse();
    assertThat(result.statusCode()).isEqualTo("FAILED");
    assertThat(result.errorCode()).isEqualTo("SIMULATED_21608");
    assertThat(result.errorMessage()).contains("Unverified trial destination number");
  }

  @Test
  @DisplayName("MockWhatsAppProvider delivers deterministically and captures messages")
  void testMockWhatsAppProvider() {
    ProviderDeliveryResult result = mockWhatsAppProvider.sendWhatsApp("+919876543210", "Hello WhatsApp", "TEST");

    assertThat(result.accepted()).isTrue();
    assertThat(result.channel()).isEqualTo(CommunicationChannel.WHATSAPP);
    assertThat(result.providerMessageId()).startsWith("WA_MOCK_");
    assertThat(mockWhatsAppProvider.getDispatchedMessages()).hasSize(1);
  }

  @Test
  @DisplayName("MockEmailProvider delivers deterministically and captures emails")
  void testMockEmailProvider() {
    ProviderDeliveryResult result = mockEmailProvider.sendEmail(
        "candidate@example.com", "Welcome", "Plain text body", "<p>HTML body</p>"
    );

    assertThat(result.accepted()).isTrue();
    assertThat(result.channel()).isEqualTo(CommunicationChannel.EMAIL);
    assertThat(result.providerMessageId()).startsWith("EM_MOCK_");
    assertThat(mockEmailProvider.getDispatchedEmails()).hasSize(1);
  }

  // --- Real Provider Configuration Guard Tests ---

  @Test
  @DisplayName("TwilioSmsProvider fails closed when credentials are missing")
  void testTwilioSmsProviderFailsClosedWhenUnconfigured() {
    // properties.getTwilio() is empty by default
    TwilioSmsProvider twilioSmsProvider = new TwilioSmsProvider(properties);

    assertThatThrownBy(() -> twilioSmsProvider.sendSms("+919876543210", "Test body", "TEST"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_SMS_FROM");
  }

  @Test
  @DisplayName("TwilioWhatsAppProvider fails closed when credentials are missing")
  void testTwilioWhatsAppProviderFailsClosedWhenUnconfigured() {
    TwilioWhatsAppProvider twilioWhatsAppProvider = new TwilioWhatsAppProvider(properties);

    assertThatThrownBy(() -> twilioWhatsAppProvider.sendWhatsApp("+919876543210", "Test body", "TEST"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN");
  }

  @Test
  @DisplayName("SendGridEmailProvider fails closed when credentials are missing")
  void testSendGridEmailProviderFailsClosedWhenUnconfigured() {
    SendGridEmailProvider sendGridEmailProvider = new SendGridEmailProvider(properties);

    assertThatThrownBy(() -> sendGridEmailProvider.sendEmail("test@example.com", "Subject", "Body", null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SENDGRID_API_KEY, SENDGRID_FROM_EMAIL");
  }

  @Test
  @DisplayName("SmtpEmailProvider fails closed when credentials are missing")
  void testSmtpEmailProviderFailsClosedWhenUnconfigured() {
    SmtpEmailProvider smtpEmailProvider = new SmtpEmailProvider(properties);

    assertThatThrownBy(() -> smtpEmailProvider.sendEmail("test@example.com", "Subject", "Body", null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SMTP_USERNAME, SMTP_APP_PASSWORD, SMTP_FROM_EMAIL");
  }

  @Test
  @DisplayName("SmtpEmailProvider sends email successfully when JavaMailSender is injected")
  void testSmtpEmailProviderWithInjectedMailSender() {
    JavaMailSender mockMailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mockMailSender.createMimeMessage()).thenReturn(mimeMessage);

    SmtpEmailProvider smtpEmailProvider = new SmtpEmailProvider(properties);
    smtpEmailProvider.setMailSender(mockMailSender);

    ProviderDeliveryResult result = smtpEmailProvider.sendEmail(
        "candidate@example.com", "Test Subject", "Test Body", "<p>Test Body</p>"
    );

    assertThat(result.accepted()).isTrue();
    assertThat(result.channel()).isEqualTo(CommunicationChannel.EMAIL);
    assertThat(result.providerName()).isEqualTo("SmtpEmailProvider");
    assertThat(result.providerMessageId()).startsWith("SMTP_");
    verify(mockMailSender).send(any(MimeMessage.class));
  }

  // --- Delegating Provider Routing Tests ---

  @Test
  @DisplayName("Delegating providers route to MOCK by default and LIVE when configured")
  void testDelegatingProvidersRouting() {
    TwilioSmsProvider twilioSmsProvider = new TwilioSmsProvider(properties);
    DelegatingSmsProvider delegatingSms = new DelegatingSmsProvider(properties, twilioSmsProvider, mockSmsProvider);

    // Default mode is MOCK
    assertThat(properties.getMode()).isEqualTo(CommunicationProperties.ProviderMode.MOCK);
    assertThat(delegatingSms.getProviderName()).isEqualTo("MockSmsProvider");

    ProviderDeliveryResult mockResult = delegatingSms.sendSms("+919876543210", "Test", "TPL");
    assertThat(mockResult.providerName()).isEqualTo("MockSmsProvider");

    // Switch to LIVE mode without credentials -> calls Twilio and fails closed
    properties.setMode(CommunicationProperties.ProviderMode.LIVE);
    assertThat(delegatingSms.getProviderName()).isEqualTo("TwilioSmsProvider");

    assertThatThrownBy(() -> delegatingSms.sendSms("+919876543210", "Test", "TPL"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TWILIO_ACCOUNT_SID");
  }

  // --- Template Registry & Sanitization Tests ---

  @Test
  @DisplayName("Template registry renders allowlisted template with parameters")
  void testTemplateRegistryRendersAllowlistedTemplate() {
    Trainee trainee = new Trainee();
    trainee.setFirstName("Amit");
    trainee.setLastName("Kumar");

    CommunicationTemplateRegistry.RenderedTemplate rendered = templateRegistry.render(
        "TRAINEE_ENROLLMENT_CONFIRMATION",
        Map.of("batchCode", "BATCH-2026-A", "courseName", "Solar Technician", "startDate", "2026-12-01"),
        trainee
    );

    assertThat(rendered.templateCode()).isEqualTo("TRAINEE_ENROLLMENT_CONFIRMATION");
    assertThat(rendered.subject()).isEqualTo("SIH 26135 - Training Enrollment Confirmation");
    assertThat(rendered.bodyText()).contains("Dear Amit Kumar");
    assertThat(rendered.bodyText()).contains("batch BATCH-2026-A");
    assertThat(rendered.bodyText()).contains("Solar Technician");
    assertThat(rendered.bodyHtml()).contains("Dear Amit Kumar");
  }

  @Test
  @DisplayName("Template registry rejects unallowlisted template code")
  void testTemplateRegistryRejectsUnknownTemplate() {
    assertThatThrownBy(() -> templateRegistry.render("UNKNOWN_UNVETTED_TEMPLATE", Map.of(), null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unsupported template code");
  }

  @Test
  @DisplayName("Template registry rejects parameters containing sensitive Aadhaar numbers")
  void testTemplateRegistryRejectsSensitiveAadhaarParameter() {
    Trainee trainee = new Trainee();
    trainee.setFirstName("Priya");

    assertThatThrownBy(() -> templateRegistry.render(
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "9876 5432 1098"), // 12-digit spaced Aadhaar
        trainee
    )).isInstanceOf(BadRequestException.class)
      .hasMessageContaining("contains sensitive data (Aadhaar/PAN/Token)");
  }

  @Test
  @DisplayName("Template registry rejects parameters containing sensitive PAN identifiers")
  void testTemplateRegistryRejectsSensitivePanParameter() {
    Trainee trainee = new Trainee();
    trainee.setFirstName("Priya");

    assertThatThrownBy(() -> templateRegistry.render(
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "ABCDE1234F"), // PAN format
        trainee
    )).isInstanceOf(BadRequestException.class)
      .hasMessageContaining("contains sensitive data (Aadhaar/PAN/Token)");
  }

  @Test
  @DisplayName("Template registry rejects unexpected parameters not in allowlisted template definition")
  void testTemplateRegistryRejectsUnexpectedParameter() {
    Trainee trainee = new Trainee();
    trainee.setFirstName("Priya");

    assertThatThrownBy(() -> templateRegistry.render(
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-1001", "unexpectedParam", "malicious_injection"),
        trainee
    )).isInstanceOf(BadRequestException.class)
      .satisfies(ex -> {
        BadRequestException bre = (BadRequestException) ex;
        assertThat(bre.getErrorCode()).isEqualTo("UNEXPECTED_PARAMETER");
        assertThat(bre.getMessage()).contains("Unexpected parameter 'unexpectedParam'");
      });
  }

  @Test
  @DisplayName("Template registry rejects missing required parameters")
  void testTemplateRegistryRejectsMissingRequiredParameter() {
    Trainee trainee = new Trainee();
    trainee.setFirstName("Priya");

    // TRAINEE_ENROLLMENT_CONFIRMATION requires batchCode, courseName, startDate
    assertThatThrownBy(() -> templateRegistry.render(
        "TRAINEE_ENROLLMENT_CONFIRMATION",
        Map.of("batchCode", "BAT-01"), // Missing courseName and startDate
        trainee
    )).isInstanceOf(BadRequestException.class)
      .satisfies(ex -> {
        BadRequestException bre = (BadRequestException) ex;
        assertThat(bre.getErrorCode()).isEqualTo("MISSING_TEMPLATE_PARAMETER");
      });
  }

  @Test
  @DisplayName("Twilio status mapping correctly translates Twilio statuses without claiming DELIVERED prematurely")
  void testTwilioStatusMapping() {
    assertThat(TwilioSmsProvider.mapTwilioStatus("queued")).isEqualTo("QUEUED");
    assertThat(TwilioSmsProvider.mapTwilioStatus("accepted")).isEqualTo("QUEUED");
    assertThat(TwilioSmsProvider.mapTwilioStatus("sending")).isEqualTo("ATTEMPTED");
    assertThat(TwilioSmsProvider.mapTwilioStatus("sent")).isEqualTo("SENT");
    assertThat(TwilioSmsProvider.mapTwilioStatus("delivered")).isEqualTo("DELIVERED");
    assertThat(TwilioSmsProvider.mapTwilioStatus("undelivered")).isEqualTo("FAILED");
    assertThat(TwilioSmsProvider.mapTwilioStatus("failed")).isEqualTo("FAILED");
    assertThat(TwilioSmsProvider.mapTwilioStatus(null)).isEqualTo("QUEUED");
  }

  @Test
  @DisplayName("Provider delivery result with status FAILED is correctly reflected")
  void testProviderDeliveryResultWithStatusFailed() {
    ProviderDeliveryResult result = ProviderDeliveryResult.failure(
        CommunicationChannel.SMS,
        "TwilioSmsProvider",
        "SM123456",
        "30008",
        "Unknown destination error",
        "{\"status\":\"failed\"}"
    );

    assertThat(result.accepted()).isFalse();
    assertThat(result.statusCode()).isEqualTo("FAILED");
    assertThat(result.providerMessageId()).isEqualTo("SM123456");
  }
}
