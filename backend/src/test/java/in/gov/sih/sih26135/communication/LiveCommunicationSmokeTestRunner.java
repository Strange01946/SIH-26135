package in.gov.sih.sih26135.communication;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestResult;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.provider.MockEmailProvider;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.provider.MockWhatsAppProvider;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Executable integration runner for validating backend live multi-channel smoke testing.
 *
 * <p>Can be executed directly via Maven:
 * {@code mvn test -f backend/pom.xml "-Dtest=LiveCommunicationSmokeTestRunner"}
 *
 * <p>Enforces:
 * <ul>
 *   <li>Strict database catalog isolation guard asserting {@code SIH26135_TEST}.</li>
 *   <li>Explicit consent enactment: smoke tests fail closed if consent is missing or revoked.</li>
 *   <li>Zero automatic consent granting or silent profile mutation.</li>
 *   <li>Comprehensive verification across SMS, WhatsApp, and Email channels.</li>
 * </ul>
 */
@SpringBootTest
@DisplayName("Live Communication Smoke Test Runner (SIH26135_TEST)")
public class LiveCommunicationSmokeTestRunner {

  private static final Logger log = LoggerFactory.getLogger(LiveCommunicationSmokeTestRunner.class);

  private static final Long FIXTURE_STATE_ID = 9984L;
  private static final Long FIXTURE_DISTRICT_ID = 9984L;
  private static final Long FIXTURE_TRAINEE_ID = 9984L;

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
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'S4', 'Smoke Test State 9984', 1, NOW(), NOW())",
        FIXTURE_STATE_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'SD94', 'Smoke District 9984', 1, NOW(), NOW())",
        FIXTURE_DISTRICT_ID, FIXTURE_STATE_ID);

    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'REG-SMOKE-9984', 'Aakash', 'Verma', '+919800009984', 'aakash.smoke9984@example.com', '2001-05-15', 1, ?, ?, 1, 3, NOW(), NOW(), NULL)",
        FIXTURE_TRAINEE_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    // Seed legitimately granted consents
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS
    grantConsent(6L); // COMMUNICATION_WHATSAPP
    grantConsent(7L); // COMMUNICATION_EMAIL
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
            + "VALUES (?, ?, 'Legitimate Smoke Test Consent', 1, 'v1.0', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
        FIXTURE_TRAINEE_ID, consentTypeId);
  }

  private void revokeConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "UPDATE trainee_consents SET consent_status_id = 2, revoked_at = NOW(), updated_at = NOW() WHERE trainee_id = ? AND consent_type_id = ?",
        FIXTURE_TRAINEE_ID, consentTypeId);
  }

  private void deleteConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "DELETE FROM trainee_consents WHERE trainee_id = ? AND consent_type_id = ?",
        FIXTURE_TRAINEE_ID, consentTypeId);
  }

  @Test
  @DisplayName("1. Dedicated test catalog guard strictly asserts SIH26135_TEST database")
  void testDedicatedTestCatalogGuard() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn.getCatalog())
          .as("Live smoke tests must strictly execute against SIH26135_TEST catalog")
          .isEqualTo("SIH26135_TEST");
    }
  }

  @Test
  @DisplayName("2. Live SMS smoke test dispatches successfully and creates audit log")
  void testLiveSmsSmokeTest() {
    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.SMS,
        FIXTURE_TRAINEE_ID,
        null,
        "SIH-26135 SMS Verification"
    );

    LiveSmokeTestResult result = notificationDispatcherService.executeLiveSmokeTest(request);

    assertThat(result).isNotNull();
    assertThat(result.deliveryAccepted()).isTrue();
    assertThat(result.channel()).isEqualTo("SMS");
    assertThat(result.providerMode()).isNotNull();
    assertThat(result.providerMessageId()).isNotBlank();
    assertThat(result.communicationLogId()).isNotNull();

    log.info("[SmokeRunner] SMS Result: Provider={}, Mode={}, SID={}, Proof={}",
        result.providerName(), result.providerMode(), result.providerMessageId(), result.deliveryProofSummary());

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ? AND communication_channel_id = 1",
        Integer.class,
        result.communicationLogId()
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("3. Live WhatsApp smoke test dispatches successfully and creates audit log")
  void testLiveWhatsAppSmokeTest() {
    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.WHATSAPP,
        FIXTURE_TRAINEE_ID,
        null,
        "SIH-26135 WhatsApp Verification"
    );

    LiveSmokeTestResult result = notificationDispatcherService.executeLiveSmokeTest(request);

    assertThat(result).isNotNull();
    assertThat(result.deliveryAccepted()).isTrue();
    assertThat(result.channel()).isEqualTo("WHATSAPP");
    assertThat(result.providerMessageId()).isNotBlank();
    assertThat(result.communicationLogId()).isNotNull();

    log.info("[SmokeRunner] WhatsApp Result: Provider={}, Mode={}, SID={}, Proof={}",
        result.providerName(), result.providerMode(), result.providerMessageId(), result.deliveryProofSummary());

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ? AND communication_channel_id = 2",
        Integer.class,
        result.communicationLogId()
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("4. Live Email smoke test dispatches successfully and creates audit log")
  void testLiveEmailSmokeTest() {
    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.EMAIL,
        FIXTURE_TRAINEE_ID,
        null,
        "SIH-26135 Email Verification"
    );

    LiveSmokeTestResult result = notificationDispatcherService.executeLiveSmokeTest(request);

    assertThat(result).isNotNull();
    assertThat(result.deliveryAccepted()).isTrue();
    assertThat(result.channel()).isEqualTo("EMAIL");
    assertThat(result.providerMessageId()).isNotBlank();
    assertThat(result.communicationLogId()).isNotNull();

    log.info("[SmokeRunner] Email Result: Provider={}, Mode={}, SID={}, Proof={}",
        result.providerName(), result.providerMode(), result.providerMessageId(), result.deliveryProofSummary());

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ? AND communication_channel_id = 3",
        Integer.class,
        result.communicationLogId()
    );
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("5. Smoke test strictly fails closed when channel consent is revoked (Zero provider calls)")
  void testSmokeTestFailsClosedWhenConsentRevoked() {
    revokeConsent(5L); // Revoke SMS consent

    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.SMS,
        FIXTURE_TRAINEE_ID,
        null,
        "Should be blocked by revoked consent"
    );

    assertThatThrownBy(() -> notificationDispatcherService.executeLiveSmokeTest(request))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_WITHDRAWN");
        });

    // Zero messages dispatched to provider
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();

    // Blocked audit record created in communication_logs
    Integer blockedCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_status_id = 8",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(blockedCount).isEqualTo(1);
  }

  @Test
  @DisplayName("6. Smoke test strictly fails closed when base DATA_PROCESSING consent is missing (Zero provider calls)")
  void testSmokeTestFailsClosedWhenDataProcessingConsentMissing() {
    deleteConsent(1L); // Delete mandatory DATA_PROCESSING consent

    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.SMS,
        FIXTURE_TRAINEE_ID,
        null,
        "Should be blocked by missing DATA_PROCESSING consent"
    );

    assertThatThrownBy(() -> notificationDispatcherService.executeLiveSmokeTest(request))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
        });

    // Zero messages dispatched to provider
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }
}
