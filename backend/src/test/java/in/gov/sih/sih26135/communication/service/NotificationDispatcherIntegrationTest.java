package in.gov.sih.sih26135.communication.service;

import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestResult;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockEmailProvider;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.provider.MockWhatsAppProvider;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spring Boot database-backed integration test suite for Phase 29.3 Notification Dispatcher,
 * DPDP Act Consent Enactment, Multi-channel Routing, and Communication Logging on SIH26135_TEST.
 */
@SpringBootTest
@DisplayName("Phase 29.3 — Notification Dispatcher & Communication Infrastructure Integration Tests (SIH26135_TEST)")
class NotificationDispatcherIntegrationTest {

  private static final Long FIXTURE_STATE_ID = 9983L;
  private static final Long FIXTURE_DISTRICT_ID = 9983L;
  private static final Long FIXTURE_TRAINEE_ID = 9983L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private NotificationDispatcherService notificationDispatcherService;

  @Autowired
  private MockSmsProvider mockSmsProvider;

  @Autowired
  private MockWhatsAppProvider mockWhatsAppProvider;

  @Autowired
  private MockEmailProvider mockEmailProvider;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() throws Exception {
    jdbcTemplate = new JdbcTemplate(dataSource);
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
    }

    cleanUpFixtures();
    insertFixtures();
    setAdminAuthentication();

    mockSmsProvider.clear();
    mockWhatsAppProvider.clear();
    mockEmailProvider.clear();
  }

  @AfterEach
  void tearDown() {
    try {
      cleanUpFixtures();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void setAdminAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("admin.nationwide"),
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("communication.manage"),
                new SimpleGrantedAuthority("survey.manage"),
                new SimpleGrantedAuthority("system.manage")
            )
        )
    );
  }

  private void insertFixtures() {
    // 1. Geography
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'T9', 'Test State 9983', 1, NOW(), NOW())",
        FIXTURE_STATE_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'TD93', 'Test District 9983', 1, NOW(), NOW())",
        FIXTURE_DISTRICT_ID, FIXTURE_STATE_ID);

    // 2. Trainee
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'REG-TR-9983', 'Vikram', 'Aditya', '+919800009983', 'vikram.aditya9983@example.com', '2000-01-01', 1, ?, ?, 1, 3, NOW(), NOW(), NULL)",
        FIXTURE_TRAINEE_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);
  }

  private void cleanUpFixtures() {
    if (jdbcTemplate == null) {
      return;
    }
    try {
      jdbcTemplate.update("DELETE FROM communication_logs WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", FIXTURE_DISTRICT_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", FIXTURE_STATE_ID);
    } catch (Exception ignored) {
    }
  }

  private void grantConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, created_at, updated_at) "
            + "VALUES (?, ?, 'Test Consent', 1, 'v1.0', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
        FIXTURE_TRAINEE_ID, consentTypeId);
  }

  private void revokeConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at, expires_at, created_at, updated_at) "
            + "VALUES (?, ?, 'Test Consent', 2, 'v1.0', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
        FIXTURE_TRAINEE_ID, consentTypeId);
  }

  @Test
  @DisplayName("1. Dedicated test catalog guard strictly asserts SIH26135_TEST database")
  void testDedicatedTestCatalogGuard() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn.getCatalog())
          .as("Integration tests must strictly execute against SIH26135_TEST")
          .isEqualTo("SIH26135_TEST");
    }
  }

  @Test
  @DisplayName("2. NotificationDispatcherService is wired and available")
  void testServiceWiring() {
    assertThat(notificationDispatcherService).isNotNull();
  }

  @Test
  @DisplayName("3. End-to-end SMS dispatch with valid consent persists communication log with SENT status")
  void testEndToEndSmsDispatch() {
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TRAINEE_ENROLLMENT_CONFIRMATION",
        Map.of("batchCode", "BAT-2026-X", "courseName", "Electrician", "startDate", "2026-12-01"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);

    assertThat(response).isNotNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.channel()).isEqualTo("SMS");
    assertThat(response.status()).isEqualTo("SENT");
    assertThat(response.providerName()).isEqualTo("MockSmsProvider");
    assertThat(response.providerMessageId()).startsWith("SM_MOCK_");
    assertThat(response.recipientMasked()).contains("XXXX");

    // Verify row persisted in MySQL communication_logs
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_channel_id = 1 AND communication_status_id = 3",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isEqualTo(1);

    // Verify dispatched message captured by mock
    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);
    assertThat(mockSmsProvider.getLastMessage().messageText()).contains("Electrician");
  }

  @Test
  @DisplayName("4. End-to-end WhatsApp dispatch with valid consent persists communication log")
  void testEndToEndWhatsAppDispatch() {
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(6L); // COMMUNICATION_WHATSAPP

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.WHATSAPP,
        CommunicationPurpose.OPERATIONAL,
        "ASSESSMENT_CERTIFICATION_NOTICE",
        Map.of("certificateNumber", "CERT-2026-9983"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);

    assertThat(response).isNotNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.channel()).isEqualTo("WHATSAPP");
    assertThat(response.status()).isEqualTo("SENT");
    assertThat(response.providerName()).isEqualTo("MockWhatsAppProvider");
    assertThat(response.providerMessageId()).startsWith("WA_MOCK_");

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_channel_id = 2 AND communication_status_id = 3",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isEqualTo(1);
    assertThat(mockWhatsAppProvider.getDispatchedMessages()).hasSize(1);
  }

  @Test
  @DisplayName("5. End-to-end Email dispatch with valid consent persists communication log")
  void testEndToEndEmailDispatch() {
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(7L); // COMMUNICATION_EMAIL

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.EMAIL,
        CommunicationPurpose.OPERATIONAL,
        "PLACEMENT_OFFER_ALERT",
        Map.of("companyName", "Tata Power", "jobTitle", "Solar Engineer"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);

    assertThat(response).isNotNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.channel()).isEqualTo("EMAIL");
    assertThat(response.status()).isEqualTo("SENT");
    assertThat(response.providerName()).isEqualTo("MockEmailProvider");
    assertThat(response.providerMessageId()).startsWith("EM_MOCK_");

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_channel_id = 3 AND communication_status_id = 3",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isEqualTo(1);
    assertThat(mockEmailProvider.getDispatchedEmails()).hasSize(1);
  }

  @Test
  @DisplayName("6. Consent blocked dispatch creates BLOCKED_NO_CONSENT record in MySQL and fails closed (HTTP 403)")
  void testConsentBlockedDispatchCreatesBlockedRecordAndFailsClosed() {
    grantConsent(1L); // DATA_PROCESSING
    revokeConsent(5L); // Revoked COMMUNICATION_SMS

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-9983"),
        1L,
        null,
        null
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_WITHDRAWN");
        });

    // Zero messages dispatched to provider
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();

    // Exactly 1 BLOCKED_NO_CONSENT log created in MySQL communication_logs (status_id = 8)
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_status_id = 8",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("7. Purpose-specific consent is enforced: FOLLOWUP_REMINDER requires EMPLOYMENT_FOLLOW_UP consent")
  void testPurposeSpecificConsentEnforcedInDispatcher() {
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // Has SMS consent, but NOT EMPLOYMENT_FOLLOW_UP (id 2)

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        FIXTURE_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.FOLLOWUP_REMINDER,
        "FOLLOWUP_REMINDER",
        Map.of("milestoneMonth", "6"),
        1L,
        null,
        null
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
        });

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @DisplayName("8. After-commit domain event choreography triggers notification dispatch and log persistence")
  void testPostCommitDomainEventDispatch() {
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS

    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    tx.execute(status -> {
      CommunicationTriggerEvent event = CommunicationTriggerEvent.of(
          "COMMUNICATION_DISPATCH",
          FIXTURE_TRAINEE_ID,
          1L,
          "SMS",
          "+919800009983",
          "OPERATIONAL_ALERT",
          null,
          "NORMAL",
          "Notice ref REF-EVT-9983"
      );
      eventPublisher.publishEvent(event);
      return null;
    });

    // Verify listener picked up after commit and dispatched
    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND message_template_code = 'OPERATIONAL_ALERT'",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("9. Live smoke-test workflow execution verifies delivery acceptance and proof summary")
  void testLiveSmokeTestWorkflow() {
    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.SMS,
        FIXTURE_TRAINEE_ID,
        null,
        "Live smoke test message body"
    );

    LiveSmokeTestResult result = notificationDispatcherService.executeLiveSmokeTest(request);

    assertThat(result).isNotNull();
    assertThat(result.deliveryAccepted()).isTrue();
    assertThat(result.channel()).isEqualTo("SMS");
    assertThat(result.providerName()).isEqualTo("MockSmsProvider");
    assertThat(result.providerMessageId()).startsWith("SM_MOCK_");
    assertThat(result.deliveryProofSummary()).contains("Channel=SMS");
    assertThat(result.deliveryProofSummary()).contains("Status=SENT");
    assertThat(result.communicationLogId()).isNotNull();

    // Verify log entry exists
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ?",
        Integer.class,
        result.communicationLogId()
    );
    assertThat(count).isEqualTo(1);
  }
}
