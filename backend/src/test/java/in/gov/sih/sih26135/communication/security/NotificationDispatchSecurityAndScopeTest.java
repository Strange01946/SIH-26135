package in.gov.sih.sih26135.communication.security;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockEmailProvider;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.provider.MockWhatsAppProvider;
import in.gov.sih.sih26135.communication.runner.LiveCommunicationSmokeTestCliRunner;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.InternalSecurityExecutionSupport;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Targeted regression and adversarial security test battery for Phase 29.3
 * verifying permanent authentication, internal execution identity, multi-tenant boundaries,
 * and fail-closed consent enforcement on SIH26135_TEST.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 29.3 — Communication Dispatch Security, Tenancy & Scope Enforcement Tests (SIH26135_TEST)")
public class NotificationDispatchSecurityAndScopeTest {

  private static final Long TEST_STATE_ID = 9984L;
  private static final Long TEST_DISTRICT_ID = 9984L;
  private static final Long TEST_TRAINEE_ID = 9984L;
  private static final Long TEST_PROVIDER_A_ID = 7777L;
  private static final Long TEST_PROVIDER_B_ID = 8888L;
  private static final Long TEST_USER_PROVIDER_B_ID = 7788L;
  private static final Long TEST_USER_ANALYST_ID = 7799L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private NotificationDispatcherService notificationDispatcherService;

  @Autowired
  private WorkflowSecurityScopeValidator workflowSecurityScopeValidator;

  @Autowired
  private InternalSecurityExecutionSupport internalSecurityExecutionSupport;

  @Autowired
  private CommunicationProperties properties;

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
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }

    cleanUpFixtures();
    insertFixtures();

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

  private void insertFixtures() {
    // 1. Geography
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'S4', 'Security Test State', 1, NOW(), NOW())",
        TEST_STATE_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'D4', 'Security Test District', 1, NOW(), NOW())",
        TEST_DISTRICT_ID, TEST_STATE_ID);

    // 2. Trainee
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'REG-SEC-9984', 'Security', 'Tester', '+919800009984', 'sec.tester@example.com', '2000-01-01', 1, ?, ?, 1, 3, NOW(), NOW(), NULL)",
        TEST_TRAINEE_ID, TEST_STATE_ID, TEST_DISTRICT_ID);

    // 3. Training Providers
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRV-SEC-7777', 'Security Provider A', 'REG-SEC-7777', 1, ?, ?, 1, 1, NOW(), NOW())",
        TEST_PROVIDER_A_ID, TEST_STATE_ID, TEST_DISTRICT_ID);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRV-SEC-8888', 'Security Provider B', 'REG-SEC-8888', 1, ?, ?, 1, 1, NOW(), NOW())",
        TEST_PROVIDER_B_ID, TEST_STATE_ID, TEST_DISTRICT_ID);

    // 4. Users: User affiliated with Provider B, User Analyst
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, user_status_id, training_provider_id, created_at, updated_at) "
            + "VALUES (?, 'provider_b_user', 'provider_b@example.com', 'hash', 1, ?, NOW(), NOW()), "
            + "(?, 'analyst_user', 'analyst@example.com', 'hash', 1, NULL, NOW(), NOW())",
        TEST_USER_PROVIDER_B_ID, TEST_PROVIDER_B_ID, TEST_USER_ANALYST_ID);
  }

  private void cleanUpFixtures() {
    if (jdbcTemplate == null) {
      return;
    }
    try {
      jdbcTemplate.update("DELETE FROM communication_logs WHERE trainee_id = ?", TEST_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", TEST_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE trainee_id = ?", TEST_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", TEST_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?)", TEST_USER_PROVIDER_B_ID, TEST_USER_ANALYST_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)", TEST_PROVIDER_A_ID, TEST_PROVIDER_B_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", TEST_DISTRICT_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", TEST_STATE_ID);
    } catch (Exception ignored) {
    }
  }

  private void grantConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, created_at, updated_at) "
            + "VALUES (?, ?, 'Test Consent', 1, 'v1.0', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
        TEST_TRAINEE_ID, consentTypeId);
  }

  private void revokeConsent(Long consentTypeId) {
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at, expires_at, created_at, updated_at) "
            + "VALUES (?, ?, 'Revoked Consent', 2, 'v1.0', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
        TEST_TRAINEE_ID, consentTypeId);
  }

  private void setAdminUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
        )
    );
  }

  @Test
  @Order(1)
  @DisplayName("1. Authenticated authorized user -> permitted dispatch")
  void test1_authenticatedAuthorizedUser_permittedDispatch() {
    setAdminUser();
    grantConsent(1L); // DATA_PROCESSING
    grantConsent(5L); // COMMUNICATION_SMS

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "AUTH-USER-001"),
        1L
    );

    NotificationDispatchResponse response = notificationDispatcherService.dispatchNotification(command);

    assertThat(response).isNotNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.status()).isEqualTo("SIMULATED");
    assertThat(response.deliveryMode()).isEqualTo("DEMO");
    assertThat(response.simulated()).isTrue();
    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);
  }

  @Test
  @Order(2)
  @DisplayName("2. Authenticated unauthorized user -> denied")
  void test2_authenticatedUnauthorizedUser_denied() {
    // Analyst user has no trainee assignment, provider assignment, or administrative role
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(TEST_USER_ANALYST_ID, "analyst_user"),
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_analyst"))
        )
    );
    grantConsent(1L);
    grantConsent(5L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "UNAUTH-001"),
        TEST_USER_ANALYST_ID
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("TRAINEE_ASSIGNMENT_REQUIRED");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(3)
  @DisplayName("3. Cross-tenant user -> denied")
  void test3_crossTenantUser_denied() {
    // User belongs to Provider B, but trainee is enrolled exclusively in Provider A
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(TEST_USER_PROVIDER_B_ID, "provider_b_user"),
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_training_provider"))
        )
    );
    grantConsent(1L);
    grantConsent(5L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "CROSS-TENANT-001"),
        TEST_USER_PROVIDER_B_ID
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("4. Missing user identity in user-initiated dispatch -> denied")
  void test4_missingUserIdentityInUserInitiatedDispatch_denied() {
    SecurityContextHolder.clearContext();
    grantConsent(1L);
    grantConsent(5L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "NO-AUTH-001"),
        1L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("AUTHENTICATED_USER_UNRESOLVED");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(5)
  @DisplayName("5. Properly authorized internal workflow -> permitted")
  void test5_properlyAuthorizedInternalWorkflow_permitted() {
    grantConsent(1L);
    grantConsent(5L);

    InternalServicePrincipal principal = new InternalServicePrincipal(
        "CLI_SMOKE_TEST",
        "CLI Smoke Test Runner",
        null,
        TenantScopeType.NATIONWIDE
    );

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "INTERNAL-001"),
        1L
    );

    NotificationDispatchResponse response = internalSecurityExecutionSupport.runAsInternalService(
        principal,
        () -> notificationDispatcherService.dispatchNotification(command)
    );

    assertThat(response).isNotNull();
    assertThat(response.accepted()).isTrue();
    assertThat(response.status()).isEqualTo("SIMULATED");
    assertThat(response.deliveryMode()).isEqualTo("DEMO");
    assertThat(response.simulated()).isTrue();
    assertThat(mockSmsProvider.getDispatchedMessages()).hasSize(1);
  }

  @Test
  @Order(6)
  @DisplayName("6. Untrusted caller claiming internal identity -> denied")
  void test6_untrustedCallerClaimingInternalIdentity_denied() {
    // Untrusted regular token presenting claimed internal role without InternalAuthenticationToken
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(999L, "attacker"),
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SYSTEM"))
        )
    );
    grantConsent(1L);
    grantConsent(5L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "UNTRUSTED-INTERNAL-001"),
        999L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Untrusted caller attempting to claim internal privileges");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(7)
  @DisplayName("7. Internal workflow targeting an unauthorized tenant -> denied")
  void test7_internalWorkflowTargetingUnauthorizedTenant_denied() {
    grantConsent(1L);
    grantConsent(5L);

    // Internal workflow explicitly scoped to Provider B (8888L), but trainee belongs to Provider A (7777L)
    InternalServicePrincipal principal = new InternalServicePrincipal(
        "PROVIDER_B_AUTOMATION",
        "Provider B Job",
        TEST_PROVIDER_B_ID,
        TenantScopeType.PROVIDER
    );

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "INTERNAL-UNAUTH-TENANT-001"),
        1L
    );

    assertThatThrownBy(() -> internalSecurityExecutionSupport.runAsInternalService(
        principal,
        () -> notificationDispatcherService.dispatchNotification(command)
    ))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Internal workflow training provider scope violation");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(8)
  @DisplayName("7a. Unregistered internal service identity -> denied")
  void test7a_unregisteredInternalServiceIdentity_denied() {
    InternalServicePrincipal arbitraryPrincipal = new InternalServicePrincipal(
        "ARBITRARY_UNKNOWN_SERVICE",
        "Arbitrary Caller",
        null,
        TenantScopeType.NATIONWIDE
    );

    assertThatThrownBy(() -> internalSecurityExecutionSupport.runAsInternalService(
        arbitraryPrincipal,
        () -> "EXECUTED"
    ))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unregistered or unauthorized internal service identity");
  }

  @Test
  @Order(9)
  @DisplayName("7b. Internal service not authorized for NATIONWIDE scope claiming NATIONWIDE -> denied")
  void test7b_internalServiceUnauthorizedNationwideClaimingNationwide_denied() {
    // PROVIDER_AUTOMATION is only authorized for PROVIDER scope, NOT NATIONWIDE
    InternalServicePrincipal invalidNationwidePrincipal = new InternalServicePrincipal(
        "PROVIDER_AUTOMATION",
        "Provider Automation Job",
        null,
        TenantScopeType.NATIONWIDE
    );

    assertThatThrownBy(() -> internalSecurityExecutionSupport.runAsInternalService(
        invalidNationwidePrincipal,
        () -> "EXECUTED"
    ))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("is not authorized for NATIONWIDE scope");
  }

  @Test
  @Order(10)
  @DisplayName("8. Missing DATA_PROCESSING consent -> blocked")
  void test8_missingDataProcessingConsent_blocked() {
    setAdminUser();
    // Only SMS consent granted; DATA_PROCESSING missing
    grantConsent(5L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "MISSING-DP-001"),
        1L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(CommunicationConsentException.class)
        .extracting("errorCode").isEqualTo("CONSENT_NOT_GRANTED");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();

    // Verify BLOCKED_NO_CONSENT persisted
    Integer blockedCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_logs WHERE trainee_id = ? AND communication_status_id = 8",
        Integer.class, TEST_TRAINEE_ID);
    assertThat(blockedCount).isEqualTo(1);
  }

  @Test
  @Order(9)
  @DisplayName("9. Missing channel consent -> blocked")
  void test9_missingChannelConsent_blocked() {
    setAdminUser();
    // Only DATA_PROCESSING granted; COMMUNICATION_SMS missing
    grantConsent(1L);

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "MISSING-SMS-001"),
        1L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(CommunicationConsentException.class)
        .extracting("errorCode").isEqualTo("CONSENT_NOT_GRANTED");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(10)
  @DisplayName("10. Revoked or expired consent -> blocked")
  void test10_revokedOrExpiredConsent_blocked() {
    setAdminUser();
    grantConsent(1L); // DATA_PROCESSING
    revokeConsent(5L); // COMMUNICATION_SMS revoked

    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REVOKED-001"),
        1L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(CommunicationConsentException.class)
        .extracting("errorCode").isEqualTo("CONSENT_WITHDRAWN");

    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
  }

  @Test
  @Order(11)
  @DisplayName("11. Live smoke test targeting a database other than SIH26135_TEST -> rejected")
  void test11_liveSmokeTestTargetingDatabaseOtherThanTest_rejected() throws Exception {
    DataSource mockDs = Mockito.mock(DataSource.class);
    Connection mockConn = Mockito.mock(Connection.class);
    Mockito.when(mockDs.getConnection()).thenReturn(mockConn);
    Mockito.when(mockConn.getCatalog()).thenReturn("SIH26135"); // Attempting production database

    LiveCommunicationSmokeTestCliRunner runner = new LiveCommunicationSmokeTestCliRunner(
        notificationDispatcherService,
        properties,
        mockDs,
        internalSecurityExecutionSupport
    );

    assertThatThrownBy(() -> runner.run())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Database catalog must be SIH26135_TEST but was: SIH26135");
  }

  @Test
  @Order(12)
  @DisplayName("12. Missing live recipient override -> rejected without dispatch")
  void test12_missingLiveRecipientOverride_rejectedWithoutDispatch() {
    grantConsent(1L);
    grantConsent(5L);

    // Live smoke test request with blank recipient override when live recipient override is enabled
    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        CommunicationChannel.SMS,
        TEST_TRAINEE_ID,
        "   ", // blank override
        "Test Message"
    );

    // Save and temporarily enable override + clear live test phone to verify fail-closed enforcement
    boolean origEnabled = properties.isLiveRecipientOverrideEnabled();
    String origPhone = properties.getTwilio().getLiveTestPhone();
    try {
      properties.setLiveRecipientOverrideEnabled(true);
      properties.getTwilio().setLiveTestPhone("");
      InternalServicePrincipal principal = new InternalServicePrincipal(
          "CLI_SMOKE_TEST",
          "Smoke Test",
          null,
          TenantScopeType.NATIONWIDE
      );

      assertThatThrownBy(() -> internalSecurityExecutionSupport.runAsInternalService(
          principal,
          () -> notificationDispatcherService.executeLiveSmokeTest(request)
      ))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECIPIENT_OVERRIDE_REQUIRED");

      assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
    } finally {
      properties.setLiveRecipientOverrideEnabled(origEnabled);
      properties.getTwilio().setLiveTestPhone(origPhone);
    }
  }

  @Test
  @Order(13)
  @DisplayName("13. No provider API invocation after security or consent failure")
  void test13_noProviderApiInvocationAfterSecurityOrConsentFailure() {
    // Cleared context -> security rejection
    SecurityContextHolder.clearContext();
    CommunicationDispatchCommand command = CommunicationDispatchCommand.of(
        TEST_TRAINEE_ID,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "FAIL-SAFE-001"),
        1L
    );

    assertThatThrownBy(() -> notificationDispatcherService.dispatchNotification(command))
        .isInstanceOf(BadRequestException.class);

    // Provider was strictly NOT invoked
    assertThat(mockSmsProvider.getDispatchedMessages()).isEmpty();
    assertThat(mockWhatsAppProvider.getDispatchedMessages()).isEmpty();
    assertThat(mockEmailProvider.getDispatchedEmails()).isEmpty();
  }

  @Test
  @Order(14)
  @DisplayName("14. Existing HTTP authentication and nationwide scope restrictions remain intact")
  void test14_existingHttpAuthenticationAndNationwideScopeRestrictionsIntact() {
    // Trainee user claiming admin authority is disqualified from nationwide admin
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(50L, "trainee_claimant"),
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"), new SimpleGrantedAuthority("ROLE_trainee"))
        )
    );

    assertThat(workflowSecurityScopeValidator.isNationwideAdmin()).isFalse();

    assertThatThrownBy(() -> workflowSecurityScopeValidator.validateNationwideScope())
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @Order(15)
  @DisplayName("15. Production database SIH26135 remains strictly untouched & test trainee intact")
  void test15_productionDatabaseStrictlyUntouchedAndTestTraineeIntact() {
    Integer prodTrainees = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM SIH26135.trainees", Integer.class);
    Integer prodConsents = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM SIH26135.trainee_consents", Integer.class);
    assertThat(prodTrainees).isZero();
    assertThat(prodConsents).isZero();

    // Verify authorized test trainee exists in SIH26135_TEST
    Integer testTraineeCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM SIH26135_TEST.trainees WHERE trainee_id = 1005143", Integer.class);
    Integer testConsentCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM SIH26135_TEST.trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1", Integer.class);
    assertThat(testTraineeCount).isEqualTo(1);
    assertThat(testConsentCount).isEqualTo(5);
  }
}
