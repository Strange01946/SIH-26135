package in.gov.sih.sih26135.communication;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.communication.consent.CommunicationConsentValidator;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.InternalSecurityExecutionSupport;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authoritative, strictly non-mutating readiness verification runner for the authorized test trainee
 * in SIH26135_TEST for Phase 29.3 live carrier verification.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Strict database catalog isolation: SIH26135_TEST only (SIH26135 untouched).</li>
 *   <li>Strictly read-only inspection: zero INSERT, UPDATE, or DELETE statements executed.</li>
 *   <li>Zero automatic creation or restoration of missing, revoked, or expired consents.</li>
 *   <li>Zero hardcoded personal contact details in source.</li>
 *   <li>Verification of legitimate, unexpired DPDP Act consents via {@link CommunicationConsentValidator}.</li>
 * </ul>
 */
@SpringBootTest
@DisplayName("Phase 29.3 — Live Verification Trainee Readiness Verifier (SIH26135_TEST)")
public class LiveTestTraineeSetupRunner {

  private static final Logger log = LoggerFactory.getLogger(LiveTestTraineeSetupRunner.class);

  public static final Long TEST_TRAINEE_ID = 1005143L;
  public static final String REGISTRATION_NUMBER = "REG-LIVE-VERIFY-001";
  public static final Long STATE_ID = 101L;
  public static final Long DISTRICT_ID = 1001L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private CommunicationConsentValidator consentValidator;

  @Autowired
  private InternalSecurityExecutionSupport internalSecurityExecutionSupport;

  @Test
  @DisplayName("Verify authorized test trainee and genuine consents in SIH26135_TEST (Read-Only)")
  void setupAuthorizedTestTrainee() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    // 1. Strict Database Isolation Guard
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }

    // 2. Establish Authorized Internal Service Security Context from Trusted Registration
    InternalServicePrincipal principal = internalSecurityExecutionSupport
        .createAuthorizedNationwidePrincipal("LIVE_TEST_READINESS_VERIFIER");

    internalSecurityExecutionSupport.runAsInternalService(principal, () -> {
      // 3. Verify Geography Dependencies Exist in SIH26135_TEST (Read-Only)
      Integer stateCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM states WHERE state_id = ?", Integer.class, STATE_ID);
      if (stateCount == null || stateCount == 0) {
        throw new IllegalStateException("Required state DL (" + STATE_ID + ") does not exist in SIH26135_TEST. Non-mutating verification aborted.");
      }

      Integer districtCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM districts WHERE district_id = ?", Integer.class, DISTRICT_ID);
      if (districtCount == null || districtCount == 0) {
        throw new IllegalStateException("Required district ND (" + DISTRICT_ID + ") does not exist in SIH26135_TEST. Non-mutating verification aborted.");
      }

      // 4. Verify Existing Authorized Test Trainee in SIH26135_TEST (Read-Only)
      Trainee trainee = traineeRepository.findById(TEST_TRAINEE_ID)
          .orElseGet(() -> traineeRepository.findByRegistrationNumber(REGISTRATION_NUMBER).orElse(null));

      if (trainee == null) {
        throw new IllegalStateException("Authorized test trainee (ID=" + TEST_TRAINEE_ID
            + " / RegNo=" + REGISTRATION_NUMBER + ") does not exist in SIH26135_TEST. Non-mutating verification aborted.");
      }

      Long traineeId = trainee.getId();
      String testPhone = trainee.getPhone();
      String testEmail = trainee.getEmail();

      assertThat(traineeId).isEqualTo(TEST_TRAINEE_ID);

      // 5. Verify Legitimate Consents in SIH26135_TEST (Read-Only)
      // 1=DATA_PROCESSING, 5=COMMUNICATION_SMS, 6=COMMUNICATION_WHATSAPP, 7=COMMUNICATION_EMAIL, 2=EMPLOYMENT_FOLLOW_UP
      List<Long> requiredConsentTypeIds = List.of(1L, 5L, 6L, 7L, 2L);

      for (Long consentTypeId : requiredConsentTypeIds) {
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = ? AND consent_type_id = ? AND consent_status_id = 1 AND (expires_at IS NULL OR expires_at > NOW())",
            Integer.class, traineeId, consentTypeId);

        if (activeCount == null || activeCount == 0) {
          throw new IllegalStateException("Mandatory consent is missing, revoked, or expired: TraineeID="
              + traineeId + ", ConsentTypeID=" + consentTypeId
              + ". Automatic granting/restoring is strictly prohibited in read-only readiness verification.");
        }

        log.info("[ReadinessVerifier] Verified legitimate active consent: TraineeID={}, ConsentTypeID={}, Status=GRANTED",
            traineeId, consentTypeId);
      }

      // 6. Verify via CommunicationConsentValidator (Fail-Closed Enforcement)
      consentValidator.validateCommunicationConsent(traineeId, "SMS", "OPERATIONAL");
      consentValidator.validateCommunicationConsent(traineeId, "WHATSAPP", "OPERATIONAL");
      consentValidator.validateCommunicationConsent(traineeId, "EMAIL", "OPERATIONAL");
      consentValidator.validateCommunicationConsent(traineeId, "SMS", "FOLLOWUP_REMINDER");
      log.info("[ReadinessVerifier] All channel and purpose consents successfully validated via CommunicationConsentValidator!");

      // 7. Log Verification Summary (With Masked Recipients)
      log.info("================================================================================");
      log.info("[ReadinessVerifier] AUTHORIZED TEST TRAINEE READINESS CONFIRMED (READ-ONLY)");
      log.info("[ReadinessVerifier] Database Catalog: SIH26135_TEST (SIH26135 untouched)");
      log.info("[ReadinessVerifier] Trainee ID:       {}", traineeId);
      log.info("[ReadinessVerifier] Reg Number:       {}", trainee.getRegistrationNumber());
      log.info("[ReadinessVerifier] Masked Phone:     {}", CommunicationTriggerEvent.maskRecipient(testPhone));
      log.info("[ReadinessVerifier] Masked WhatsApp:  {}", CommunicationTriggerEvent.maskRecipient(testPhone));
      log.info("[ReadinessVerifier] Masked Email:     {}", CommunicationTriggerEvent.maskRecipient(testEmail));
      log.info("[ReadinessVerifier] Ready for:        SMS (LIVE), WHATSAPP (LIVE), EMAIL (LIVE)");
      log.info("================================================================================");
    });
  }
}

