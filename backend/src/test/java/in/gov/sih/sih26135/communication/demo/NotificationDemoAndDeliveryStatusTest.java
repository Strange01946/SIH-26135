package in.gov.sih.sih26135.communication.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockEmailProvider;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.provider.MockWhatsAppProvider;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.dto.request.DispatchNotificationRequest;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.InternalAuthenticationToken;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authoritative integration test battery for Phase 29.3 Demo Mode,
 * Delivery Status Accuracy & API Readiness.
 *
 * <p>Verifies:
 * <ol>
 *   <li>DEMO mode execution: returns SIMULATED, simulated=true, physicallyDelivered=false, zero external network calls.</li>
 *   <li>DEMO deterministic failure simulation with sanitized error codes and explanations.</li>
 *   <li>LIVE mode status transitions: QUEUED (accepted), DELIVERED (physically confirmed), FAILED (rejected).</li>
 *   <li>Provider acceptance (QUEUED) does NOT imply confirmed delivery (physicallyDelivered=false).</li>
 *   <li>Mandatory statutory DPDP consent validation across both DEMO and LIVE modes (fail-closed, logs BLOCKED_NO_CONSENT).</li>
 *   <li>Production environment safety guard: prohibits DEMO/MOCK mode in production catalog, profile, or environment.</li>
 *   <li>Analytics isolation: simulated records have is_success_flag=0 and sentAt/deliveredAt=null, preventing KPI inflation.</li>
 *   <li>API presentation flow via POST /api/v1/communication-logs/dispatch and GET /api/v1/communication-logs/{id}.</li>
 *   <li>Server-side trusted delivery mode derivation: untrusted caller parameters cannot choose mode.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 29.3 - Notification Demo Mode, Delivery Status Accuracy & API Readiness Tests")
class NotificationDemoAndDeliveryStatusTest {

  private static final Logger log = LoggerFactory.getLogger(NotificationDemoAndDeliveryStatusTest.class);
  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;

  private Long fixtureTraineeId;
  private String fixtureTraineeRegNo;
  private String fixtureTraineePhone;
  private String fixtureTraineePhoneLast4;
  private String fixtureTraineeEmail;

  private final List<Long> testOwnedLogIds = new CopyOnWriteArrayList<>();
  private final List<Long> testOwnedConsentIds = new CopyOnWriteArrayList<>();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private NotificationDispatcherService notificationDispatcherService;

  @Autowired
  private CommunicationProperties communicationProperties;

  @Autowired
  private MockSmsProvider mockSmsProvider;

  @Autowired
  private MockWhatsAppProvider mockWhatsAppProvider;

  @Autowired
  private MockEmailProvider mockEmailProvider;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String adminToken;

  @BeforeEach
  void setUp() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    mockSmsProvider.clear();
    mockWhatsAppProvider.clear();
    mockEmailProvider.clear();

    if (fixtureTraineeId != null) {
      cleanUpFixtures();
    }
    testOwnedLogIds.clear();
    testOwnedConsentIds.clear();

    insertFixtures();
    setAdminAuthentication();

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "ROLE_ADMIN",
            "ROLE_super_admin",
            "communication.manage",
            "survey.manage",
            "system.manage",
            "admin.nationwide"
        )
    );
  }

  @AfterEach
  void tearDown() {
    try {
      cleanUpFixtures();
    } finally {
      SecurityContextHolder.clearContext();
      communicationProperties.setMode(CommunicationProperties.ProviderMode.MOCK);
      mockSmsProvider.clear();
      mockWhatsAppProvider.clear();
      mockEmailProvider.clear();
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
    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    this.fixtureTraineeRegNo = "REG-DEMO-" + uniqueSuffix;
    int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
    this.fixtureTraineePhoneLast4 = String.valueOf(randomSuffix);
    this.fixtureTraineePhone = "+91980000" + fixtureTraineePhoneLast4;
    this.fixtureTraineeEmail = "demo." + uniqueSuffix + "@example.com";

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainees (registration_number, first_name, last_name, phone, email, date_of_birth, "
              + "gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) "
              + "VALUES (?, 'Ananya', 'Sharma', ?, ?, '2001-02-14', 2, 101, 1001, 1, 3, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, fixtureTraineeRegNo);
      ps.setString(2, fixtureTraineePhone);
      ps.setString(3, fixtureTraineeEmail);
      return ps;
    }, keyHolder);

    Number generatedKey = keyHolder.getKey();
    if (generatedKey == null) {
      throw new IllegalStateException("Failed to insert exclusive test fixture trainee: no generated key returned");
    }
    this.fixtureTraineeId = generatedKey.longValue();
    if (this.fixtureTraineeId.equals(AUTHORITATIVE_TEST_TRAINEE_ID)) {
      throw new IllegalStateException("Generated fixture trainee ID collided with authoritative test trainee " + AUTHORITATIVE_TEST_TRAINEE_ID);
    }
  }

  private void cleanUpFixtures() {
    if (jdbcTemplate == null || this.fixtureTraineeId == null) {
      return;
    }
    try {
      // 1. Delete ONLY communication logs explicitly tracked and owned by this test execution
      if (!testOwnedLogIds.isEmpty()) {
        for (Long logId : testOwnedLogIds) {
          jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
        }
        testOwnedLogIds.clear();
      }

      // 2. Delete any remaining communication logs specifically for this test's fixture trainee
      jdbcTemplate.update(
          "DELETE FROM communication_logs WHERE trainee_id = ? AND trainee_id != ?",
          this.fixtureTraineeId, AUTHORITATIVE_TEST_TRAINEE_ID
      );

      // 3. Delete ONLY trainee consent records explicitly tracked and owned by this test execution
      if (!testOwnedConsentIds.isEmpty()) {
        for (Long consentId : testOwnedConsentIds) {
          jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_consent_id = ?", consentId);
        }
        testOwnedConsentIds.clear();
      }

      // 4. Delete any remaining consents specifically for this test's fixture trainee
      jdbcTemplate.update(
          "DELETE FROM trainee_consents WHERE trainee_id = ? AND trainee_id != ?",
          this.fixtureTraineeId, AUTHORITATIVE_TEST_TRAINEE_ID
      );

      // 5. Delete fixture trainee only, never deleting authoritative trainee 1005143
      jdbcTemplate.update(
          "DELETE FROM trainees WHERE trainee_id = ? AND trainee_id != ?",
          this.fixtureTraineeId, AUTHORITATIVE_TEST_TRAINEE_ID
      );
      this.fixtureTraineeId = null;
    } catch (Exception ex) {
      log.error("[NotificationDemoAndDeliveryStatusTest] Teardown fixture cleanup failed: {}", ex.getMessage(), ex);
      throw new RuntimeException("Teardown fixture cleanup failed: " + ex.getMessage(), ex);
    }
  }

  private void trackLog(Long logId) {
    if (logId != null && !testOwnedLogIds.contains(logId)) {
      testOwnedLogIds.add(logId);
    }
  }

  private void trackBlockedLogsForFixtureTrainee() {
    if (this.fixtureTraineeId != null) {
      List<Long> blockedIds = jdbcTemplate.queryForList(
          "SELECT communication_log_id FROM communication_logs WHERE trainee_id = ? AND communication_status_id = 8",
          Long.class,
          this.fixtureTraineeId
      );
      for (Long id : blockedIds) {
        trackLog(id);
      }
    }
  }

  private Long grantConsent(Long consentTypeId) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, created_at, updated_at) "
              + "VALUES (?, ?, 'Test Consent', 1, 'v1.0', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, this.fixtureTraineeId);
      ps.setLong(2, consentTypeId);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    if (key != null) {
      Long consentId = key.longValue();
      testOwnedConsentIds.add(consentId);
      return consentId;
    }
    return null;
  }

  private Long revokeConsent(Long consentTypeId) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at, expires_at, created_at, updated_at) "
              + "VALUES (?, ?, 'Test Consent', 2, 'v1.0', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, this.fixtureTraineeId);
      ps.setLong(2, consentTypeId);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    if (key != null) {
      Long consentId = key.longValue();
      testOwnedConsentIds.add(consentId);
      return consentId;
    }
    return null;
  }

  // =========================================================================
  // 1. DEMO MODE EXECUTION & DELIVERY STATUS ACCURACY
  // =========================================================================

  @Test
  @DisplayName("1. DEMO mode dispatch returns SIMULATED outcome, simulated=true, physicallyDelivered=false with zero network calls")
  void testDemoModeSimulatedDispatchReturnsSimulatedOutcome() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TRAINEE_ENROLLMENT_CONFIRMATION",
        Map.of("batchCode", "BAT-DEMO-2026", "courseName", "Full Stack Development", "startDate", "2026-12-01"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);
    trackLog(response.communicationLogId());

    // Verify response contract
    assertThat(response).isNotNull();
    assertThat(response.deliveryMode()).isEqualTo("DEMO");
    assertThat(response.status()).isEqualTo("SIMULATED");
    assertThat(response.simulated()).isTrue();
    assertThat(response.physicallyDelivered()).isFalse();
    assertThat(response.deliveredAt()).isNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.providerName()).isEqualTo("MockSmsProvider");
    assertThat(response.providerMessageId()).startsWith("SM_MOCK_");
    assertThat(response.recipientMasked()).contains("XXXX");
    assertThat(response.message()).contains("DEMO mode");

    // Verify database record has SIMULATED status (ID 9) and null timestamps
    Map<String, Object> logRow = jdbcTemplate.queryForMap(
        "SELECT communication_log_id, communication_status_id, sent_at, delivered_at, failed_at, failure_reason "
            + "FROM communication_logs WHERE communication_log_id = ?",
        response.communicationLogId()
    );
    assertThat(((Number) logRow.get("communication_status_id")).longValue()).isEqualTo(9L);
    assertThat(logRow.get("sent_at")).isNull();
    assertThat(logRow.get("delivered_at")).isNull();
    assertThat(logRow.get("failed_at")).isNull();

    // Verify mock captured message in memory with zero external transmission
    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);
    assertThat(mockSmsProvider.getLastMessage().messageText()).contains("Full Stack Development");
  }

  @Test
  @DisplayName("2. DEMO mode deterministic failure returns FAILED with sanitized error code and explanation")
  void testDemoModeDeterministicFailureSimulation() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS

    mockSmsProvider.setSimulateFailure(true);
    mockSmsProvider.setFailureDetails("SIMULATED_21211", "Simulated invalid phone number in demo environment");

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-DEMO-FAIL"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);
    trackLog(response.communicationLogId());

    assertThat(response).isNotNull();
    assertThat(response.deliveryMode()).isEqualTo("DEMO");
    assertThat(response.status()).isEqualTo("FAILED");
    assertThat(response.simulated()).isTrue();
    assertThat(response.physicallyDelivered()).isFalse();
    assertThat(response.accepted()).isFalse();
    assertThat(response.errorCode()).isEqualTo("SIMULATED_21211");
    assertThat(response.errorExplanation()).contains("Simulated invalid phone number");
    assertThat(response.message()).contains("Simulated notification failed");

    // Database check: communication_status_id = 7 (FAILED)
    Map<String, Object> logRow = jdbcTemplate.queryForMap(
        "SELECT communication_status_id, failed_at, failure_reason, sent_at, delivered_at "
            + "FROM communication_logs WHERE communication_log_id = ?",
        response.communicationLogId()
    );
    assertThat(((Number) logRow.get("communication_status_id")).longValue()).isEqualTo(7L);
    assertThat(logRow.get("failed_at")).isNotNull();
    assertThat((String) logRow.get("failure_reason")).contains("Simulated invalid phone number");
    assertThat(logRow.get("sent_at")).isNull();
    assertThat(logRow.get("delivered_at")).isNull();
  }

  // =========================================================================
  // 2. LIVE MODE PROVIDER STATUS DISTINCTION & DELIVERY ASSURANCE
  // =========================================================================

  @Test
  @DisplayName("3. LIVE mode provider acceptance (QUEUED) does NOT imply confirmed handset delivery (physicallyDelivered=false)")
  void testLiveModeProviderAcceptanceDoesNotImplyDelivery() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.LIVE);
    grantConsent(1L);
    grantConsent(5L);

    // Mock provider configured with custom QUEUED outcome (simulating Twilio async acceptance)
    mockSmsProvider.setCustomResult("QUEUED", true, null);

    Trainee trainee = traineeRepository.findById(this.fixtureTraineeId).orElseThrow();
    ProviderDeliveryResult deliveryResult = new ProviderDeliveryResult(
        CommunicationChannel.SMS,
        "TwilioSmsProvider",
        "SM_TWILIO_QUEUED_123",
        "QUEUED",
        true,
        "{\"status\":\"queued\"}",
        null,
        null,
        LocalDateTime.now()
    );

    // Persist via service logic directly
    CommunicationLog logEntity = ((in.gov.sih.sih26135.communication.service.NotificationDispatcherServiceImpl) notificationDispatcherService)
        .persistCommunicationLog(trainee, "SMS", "OPERATIONAL", "OPERATIONAL_ALERT", 1L, deliveryResult);
    trackLog(logEntity.getId());

    assertThat(logEntity.getCommunicationStatus().getStatusCode()).isEqualTo("QUEUED");
    assertThat(logEntity.getSentAt()).isNull();
    assertThat(logEntity.getDeliveredAt()).isNull();

    // Verify constructed response
    boolean isPhysicallyDelivered = "DELIVERED".equalsIgnoreCase(logEntity.getCommunicationStatus().getStatusCode());
    assertThat(isPhysicallyDelivered).isFalse();
  }

  @Test
  @DisplayName("4. LIVE mode confirmed delivery (DELIVERED) sets physicallyDelivered=true and records deliveredAt")
  void testLiveModeConfirmedDelivery() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.LIVE);
    grantConsent(1L);
    grantConsent(5L);

    Trainee trainee = traineeRepository.findById(this.fixtureTraineeId).orElseThrow();
    ProviderDeliveryResult deliveryResult = new ProviderDeliveryResult(
        CommunicationChannel.SMS,
        "TwilioSmsProvider",
        "SM_TWILIO_DELIVERED_123",
        "DELIVERED",
        true,
        "{\"status\":\"delivered\"}",
        null,
        null,
        LocalDateTime.now()
    );

    CommunicationLog logEntity = ((in.gov.sih.sih26135.communication.service.NotificationDispatcherServiceImpl) notificationDispatcherService)
        .persistCommunicationLog(trainee, "SMS", "OPERATIONAL", "OPERATIONAL_ALERT", 1L, deliveryResult);
    trackLog(logEntity.getId());

    assertThat(logEntity.getCommunicationStatus().getStatusCode()).isEqualTo("DELIVERED");
    assertThat(logEntity.getSentAt()).isNotNull();
    assertThat(logEntity.getDeliveredAt()).isNotNull();
  }

  // =========================================================================
  // 3. STATUTORY CONSENT BOUNDARY (BOTH DEMO & LIVE)
  // =========================================================================

  @Test
  @DisplayName("5. Missing consent strictly blocks dispatch and logs BLOCKED_NO_CONSENT (HTTP 403) with zero network dispatches")
  void testMissingConsentBlocksDispatchAndPersistsBlockedLog() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    // Neither DATA_PROCESSING nor SMS consent granted

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-BLOCKED-01"),
        1L,
        null,
        null
    );

    try {
      assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
          .isInstanceOf(CommunicationConsentException.class)
          .satisfies(ex -> {
            CommunicationConsentException cce = (CommunicationConsentException) ex;
            assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          });
    } finally {
      trackBlockedLogsForFixtureTrainee();
    }

    // Zero messages sent to provider
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();

    // Verify BLOCKED_NO_CONSENT record persisted in MySQL communication_logs (status ID 8)
    Integer blockedCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_status_id = 8",
        Integer.class,
        this.fixtureTraineeId
    );
    assertThat(blockedCount).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("6. Revoked consent strictly blocks dispatch and logs BLOCKED_NO_CONSENT (CONSENT_WITHDRAWN)")
  void testRevokedConsentBlocksDispatch() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L); // DATA_PROCESSING
    revokeConsent(5L); // Revoked COMMUNICATION_SMS

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-BLOCKED-02"),
        1L,
        null,
        null
    );

    try {
      assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
          .isInstanceOf(CommunicationConsentException.class)
          .satisfies(ex -> {
            CommunicationConsentException cce = (CommunicationConsentException) ex;
            assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(cce.getErrorCode()).isEqualTo("CONSENT_WITHDRAWN");
          });
    } finally {
      trackBlockedLogsForFixtureTrainee();
    }

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  // =========================================================================
  // 4. PRODUCTION SAFETY GUARD TESTS
  // =========================================================================

  @Test
  @DisplayName("7. Production guard strictly prohibits DEMO/MOCK mode when running against production environment")
  void testProductionGuardProhibitsDemoModeInProduction() throws Exception {
    CommunicationProperties props = new CommunicationProperties();
    props.setMode(CommunicationProperties.ProviderMode.DEMO);

    // 1. Prohibit when APP_ENVIRONMENT=production
    MockEnvironment prodEnv1 = new MockEnvironment();
    prodEnv1.setProperty("APP_ENVIRONMENT", "production");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEMO/MOCK communication mode is strictly prohibited in production environment");

    // 2. Prohibit when active profile is 'prod'
    MockEnvironment prodEnv2 = new MockEnvironment();
    prodEnv2.setActiveProfiles("prod");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEMO/MOCK communication mode is strictly prohibited in production environment");

    // 3. Prohibit when datasource URL targets production catalog SIH26135 (uppercase)
    MockEnvironment prodEnv3 = new MockEnvironment();
    prodEnv3.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/SIH26135?useUnicode=true");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv3))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 4. Prohibit when datasource URL uses lowercase catalog 'sih26135'
    MockEnvironment prodEnv4 = new MockEnvironment();
    prodEnv4.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/sih26135");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv4))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 5. Prohibit when datasource URL uses mixed-case catalog 'Sih26135'
    MockEnvironment prodEnv5 = new MockEnvironment();
    prodEnv5.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/Sih26135?serverTimezone=UTC");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 6. Prohibit when DB_NAME targets production catalog (mixed case)
    MockEnvironment prodEnv6 = new MockEnvironment();
    prodEnv6.setProperty("DB_NAME", "sih26135");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv6))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 7. Prohibit on conflicting DB configuration: spring.datasource.url is test, but DB_NAME is production
    MockEnvironment prodEnv7 = new MockEnvironment();
    prodEnv7.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/SIH26135_TEST");
    prodEnv7.setProperty("DB_NAME", "sih26135");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv7))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 8. Prohibit on conflicting DB configuration: spring.datasource.url is production, but DB_NAME is test
    MockEnvironment prodEnv8 = new MockEnvironment();
    prodEnv8.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/sih26135");
    prodEnv8.setProperty("DB_NAME", "SIH26135_TEST");
    assertThatThrownBy(() -> props.validateProductionSafety(prodEnv8))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("catalog: SIH26135");

    // 9. Permit when targeting test catalog SIH26135_TEST (uppercase or lowercase)
    MockEnvironment testEnv1 = new MockEnvironment();
    testEnv1.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/SIH26135_TEST?useUnicode=true");
    testEnv1.setProperty("DB_NAME", "SIH26135_TEST");
    props.validateProductionSafety(testEnv1);

    MockEnvironment testEnv2 = new MockEnvironment();
    testEnv2.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/sih26135_test");
    testEnv2.setProperty("DB_NAME", "sih26135_test");
    props.validateProductionSafety(testEnv2);

    // 10. Authoritative DataSource metadata check: mock DataSource reporting production catalog
    DataSource mockProdDs = mock(DataSource.class);
    Connection mockConn = mock(Connection.class);
    when(mockProdDs.getConnection()).thenReturn(mockConn);
    when(mockConn.getCatalog()).thenReturn("sih26135");
    assertThatThrownBy(() -> props.validateProductionSafety(mockProdDs, testEnv1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("detected via connection metadata: 'sih26135'");

    // 11. LIVE mode is completely permitted in production
    props.setMode(CommunicationProperties.ProviderMode.LIVE);
    props.validateProductionSafety(mockProdDs, prodEnv1);
    props.validateProductionSafety(prodEnv1);
    props.validateProductionSafety(prodEnv2);
    props.validateProductionSafety(prodEnv3);
  }

  // =========================================================================
  // 5. AUDIT & ANALYTICS ISOLATION
  // =========================================================================

  @Test
  @DisplayName("8. Analytics isolation: simulated records (ID 9) have is_success_flag=0 and never inflate delivery KPIs")
  void testAnalyticsIsolationSimulatedRecordsDoNotInflateKpi() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L);
    grantConsent(5L);

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-ANALYTICS-TEST"),
        1L,
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);
    trackLog(response.communicationLogId());
    assertThat(response.status()).isEqualTo("SIMULATED");

    // Query 1: Real successful deliveries count (is_success_flag = 1) -> must be 0!
    Integer successfulDeliveries = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs cl "
            + "JOIN ref_communication_status rcs ON cl.communication_status_id = rcs.communication_status_id "
            + "WHERE cl.communication_log_id = ? AND rcs.is_success_flag = 1",
        Integer.class,
        response.communicationLogId()
    );
    assertThat(successfulDeliveries).isEqualTo(0);

    // Query 2: Physically delivered records count (delivered_at IS NOT NULL) -> must be 0!
    Integer deliveredCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ? AND delivered_at IS NOT NULL",
        Integer.class,
        response.communicationLogId()
    );
    assertThat(deliveredCount).isEqualTo(0);

    // Query 3: Sent records count (sent_at IS NOT NULL) -> must be 0!
    Integer sentCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE communication_log_id = ? AND sent_at IS NOT NULL",
        Integer.class,
        response.communicationLogId()
    );
    assertThat(sentCount).isEqualTo(0);
  }

  // =========================================================================
  // 6. REST API PRESENTATION FLOW
  // =========================================================================

  @Test
  @DisplayName("9. API Presentation: POST /api/v1/communication-logs/dispatch dispatches and GET /{id} retrieves matching SIMULATED log")
  void testApiPresentationFlowPostDispatchAndGetById() throws Exception {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L);
    grantConsent(5L);

    DispatchNotificationRequest request = new DispatchNotificationRequest(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "ASSESSMENT_CERTIFICATION_NOTICE",
        Map.of("certificateNumber", "CERT-DEMO-2026")
    );

    // 1. POST /api/v1/communication-logs/dispatch
    String responseContent = mockMvc.perform(post("/api/v1/communication-logs/dispatch")
            .header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.channel").value("SMS"))
        .andExpect(jsonPath("$.data.deliveryMode").value("DEMO"))
        .andExpect(jsonPath("$.data.status").value("SIMULATED"))
        .andExpect(jsonPath("$.data.simulated").value(true))
        .andExpect(jsonPath("$.data.physicallyDelivered").value(false))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.recipientMasked").value("+XXXXXXXX" + fixtureTraineePhoneLast4))
        .andExpect(jsonPath("$.data.providerName").value("MockSmsProvider"))
        .andReturn().getResponse().getContentAsString();

    Map<String, Object> rootNode = objectMapper.readValue(responseContent, Map.class);
    Map<String, Object> dataNode = (Map<String, Object>) rootNode.get("data");
    Long logId = ((Number) dataNode.get("communicationLogId")).longValue();
    trackLog(logId);

    // 2. GET /api/v1/communication-logs/{id}
    mockMvc.perform(get("/api/v1/communication-logs/" + logId)
            .header("Authorization", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(logId))
        .andExpect(jsonPath("$.data.statusCode").value("SIMULATED"))
        .andExpect(jsonPath("$.data.statusName").value("Simulated (Demo Mode)"))
        .andExpect(jsonPath("$.data.isSuccessFlag").value(false))
        .andExpect(jsonPath("$.data.isFailureFlag").value(false))
        .andExpect(jsonPath("$.data.sentAt").doesNotExist())
        .andExpect(jsonPath("$.data.deliveredAt").doesNotExist())
        .andExpect(jsonPath("$.data.channelCode").value("SMS"));
  }

  @Test
  @DisplayName("10. API Presentation: POST /api/v1/communication-logs/dispatch fails closed on missing consent (HTTP 403)")
  void testApiPresentationRejectsOnMissingConsent() throws Exception {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    // No consent granted

    DispatchNotificationRequest request = new DispatchNotificationRequest(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-NO-CONSENT")
    );

    try {
      mockMvc.perform(post("/api/v1/communication-logs/dispatch")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.code").value("CONSENT_NOT_GRANTED"));
    } finally {
      trackBlockedLogsForFixtureTrainee();
    }
  }

  // =========================================================================
  // 7. FIXTURE ISOLATION & TRUTHFUL AUDIT IDENTITY
  // =========================================================================

  @Test
  @DisplayName("11. Authoritative test trainee 1005143 and its 5 active valid consents remain strictly preserved in SIH26135_TEST")
  void testAuthoritativeTestTrainee1005143Preserved() {
    Integer traineeCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM trainees WHERE trainee_id = ? AND deleted_at IS NULL",
        Integer.class,
        AUTHORITATIVE_TEST_TRAINEE_ID
    );
    assertThat(traineeCount).isEqualTo(1);

    Integer activeConsentCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = ? AND consent_status_id = 1 AND revoked_at IS NULL",
        Integer.class,
        AUTHORITATIVE_TEST_TRAINEE_ID
    );
    assertThat(activeConsentCount).isEqualTo(5);
  }

  @Test
  @DisplayName("12. Internal service execution with null commandUserId stores null initiated_by_user_id and logs service identity")
  void testInternalServiceExecutionWithNullCommandUserIdStoresNull() {
    communicationProperties.setMode(CommunicationProperties.ProviderMode.DEMO);
    grantConsent(1L);
    grantConsent(5L);

    // Establish authoritative InternalAuthenticationToken
    InternalServicePrincipal servicePrincipal = new InternalServicePrincipal(
        "DEMO_BATCH_JOB", "Demo Batch Communication Service", null, TenantScopeType.NATIONWIDE);
    InternalAuthenticationToken internalAuth = new InternalAuthenticationToken(
        servicePrincipal, List.of(new SimpleGrantedAuthority("admin.nationwide")));
    SecurityContextHolder.getContext().setAuthentication(internalAuth);

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        this.fixtureTraineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-INTERNAL-NULL-USER"),
        null, // commandUserId is explicitly null for automated background executions
        null,
        null
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);
    trackLog(response.communicationLogId());
    assertThat(response.status()).isEqualTo("SIMULATED");

    // Verify in database that initiated_by_user_id is truthfully NULL (never defaulted to 1)
    Map<String, Object> logRow = jdbcTemplate.queryForMap(
        "SELECT initiated_by_user_id FROM communication_logs WHERE communication_log_id = ?",
        response.communicationLogId()
    );
    assertThat(logRow.get("initiated_by_user_id")).isNull();
  }

  @Test
  @DisplayName("13. Unrelated records created before and during test execution survive setup, execution, and teardown completely unchanged")
  void testUnrelatedRecordsCreatedBeforeAndDuringExecutionSurviveTeardown() {
    // 1. Provision an unrelated trainee
    String unrelatedSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String unrelatedRegNo = "REG-UNRELATED-" + unrelatedSuffix;
    KeyHolder unrelatedTraineeKh = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainees (registration_number, first_name, last_name, phone, email, date_of_birth, "
              + "gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) "
              + "VALUES (?, 'Vikram', 'Mehta', '+919900112233', 'vikram.unrelated@example.com', '1998-05-20', 1, 101, 1001, 1, 3, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, unrelatedRegNo);
      return ps;
    }, unrelatedTraineeKh);
    Long unrelatedTraineeId = unrelatedTraineeKh.getKey().longValue();

    // 2. Provision an unrelated consent for the unrelated trainee
    KeyHolder unrelatedConsentKh = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, created_at, updated_at) "
              + "VALUES (?, 3, 'Unrelated Pre-existing Consent', 1, 'v0.9', NOW() - INTERVAL 30 DAY, NOW() + INTERVAL 2 YEAR, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY)",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, unrelatedTraineeId);
      return ps;
    }, unrelatedConsentKh);
    Long unrelatedConsentId = unrelatedConsentKh.getKey().longValue();

    // 3. Provision an unrelated communication log created BEFORE test dispatch
    KeyHolder preLogKh = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO communication_logs (trainee_id, communication_channel_id, communication_direction_id, communication_purpose_id, communication_status_id, consent_type_id, provider_message_id, created_at, updated_at) "
              + "VALUES (?, 1, 1, 1, 9, 3, 'MSG_UNRELATED_PRE_EXISTING', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY)",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, unrelatedTraineeId);
      return ps;
    }, preLogKh);
    Long unrelatedPreLogId = preLogKh.getKey().longValue();

    Long unrelatedConcurrentLogId = null;

    try {
      // 4. Execute test operations for this test's fixture trainee
      grantConsent(1L); // DATA_PROCESSING
      grantConsent(5L); // COMMUNICATION_SMS

      // 5. Simulate an unrelated concurrent communication log created by an external job DURING execution
      KeyHolder concurrentLogKh = new GeneratedKeyHolder();
      jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO communication_logs (trainee_id, communication_channel_id, communication_direction_id, communication_purpose_id, communication_status_id, consent_type_id, provider_message_id, created_at, updated_at) "
                + "VALUES (?, 2, 1, 2, 9, 3, 'MSG_UNRELATED_CONCURRENT', NOW(), NOW())",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setLong(1, unrelatedTraineeId);
        return ps;
      }, concurrentLogKh);
      unrelatedConcurrentLogId = concurrentLogKh.getKey().longValue();

      // 6. Dispatch test notification for this test's fixture trainee
      CommunicationDispatchCommand command = new CommunicationDispatchCommand(
          this.fixtureTraineeId,
          CommunicationChannel.SMS,
          CommunicationPurpose.OPERATIONAL,
          "OPERATIONAL_ALERT",
          Map.of("referenceId", "REF-ISOLATION-REGRESSION"),
          1L,
          null,
          null
      );
      NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);
      trackLog(response.communicationLogId());

      assertThat(response.status()).isEqualTo("SIMULATED");
      assertThat(response.communicationLogId()).isNotEqualTo(unrelatedPreLogId);
      assertThat(response.communicationLogId()).isNotEqualTo(unrelatedConcurrentLogId);

      // 7. Trigger teardown cleanup for this test's fixtures
      cleanUpFixtures();

      // 8. Assert that the unrelated trainee SURVIVED completely untouched
      Integer unrelatedTraineeCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = ?",
          Integer.class,
          unrelatedTraineeId
      );
      assertThat(unrelatedTraineeCount).isEqualTo(1);

      // 9. Assert that the unrelated consent SURVIVED completely untouched
      Map<String, Object> consentRow = jdbcTemplate.queryForMap(
          "SELECT trainee_consent_id, purpose, policy_version, consent_status_id FROM trainee_consents WHERE trainee_consent_id = ?",
          unrelatedConsentId
      );
      assertThat(consentRow).isNotNull();
      assertThat((String) consentRow.get("purpose")).isEqualTo("Unrelated Pre-existing Consent");
      assertThat((String) consentRow.get("policy_version")).isEqualTo("v0.9");
      assertThat(((Number) consentRow.get("consent_status_id")).longValue()).isEqualTo(1L);

      // 10. Assert that the pre-existing unrelated communication log SURVIVED
      Map<String, Object> preLogRow = jdbcTemplate.queryForMap(
          "SELECT communication_log_id, provider_message_id, communication_status_id FROM communication_logs WHERE communication_log_id = ?",
          unrelatedPreLogId
      );
      assertThat(preLogRow).isNotNull();
      assertThat((String) preLogRow.get("provider_message_id")).isEqualTo("MSG_UNRELATED_PRE_EXISTING");

      // 11. Assert that the concurrent unrelated communication log SURVIVED
      Map<String, Object> concurrentLogRow = jdbcTemplate.queryForMap(
          "SELECT communication_log_id, provider_message_id, communication_status_id FROM communication_logs WHERE communication_log_id = ?",
          unrelatedConcurrentLogId
      );
      assertThat(concurrentLogRow).isNotNull();
      assertThat((String) concurrentLogRow.get("provider_message_id")).isEqualTo("MSG_UNRELATED_CONCURRENT");

      // 12. Assert that this test's fixture trainee was cleanly deleted
      Integer fixtureTraineeCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number = ?",
          Integer.class,
          this.fixtureTraineeRegNo
      );
      assertThat(fixtureTraineeCount).isEqualTo(0);

    } finally {
      // Clean up ONLY the specific unrelated test records created in this test method
      if (unrelatedConcurrentLogId != null) {
        jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", unrelatedConcurrentLogId);
      }
      jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", unrelatedPreLogId);
      jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_consent_id = ?", unrelatedConsentId);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", unrelatedTraineeId);
    }
  }
}
