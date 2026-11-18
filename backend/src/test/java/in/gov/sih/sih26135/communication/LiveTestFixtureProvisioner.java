package in.gov.sih.sih26135.communication;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.InternalSecurityExecutionSupport;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.TraineeService;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated fixture provisioner for initial local/CI test environment bootstrap.
 *
 * <p>Separated from {@link LiveTestTraineeSetupRunner} to ensure that readiness verification
 * is strictly non-mutating and read-only.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Only targets dedicated SIH26135_TEST catalog.</li>
 *   <li>Preserves existing test trainee (1005143) and valid consents without overwriting.</li>
 *   <li>Never automatically restores revoked consents.</li>
 * </ul>
 */
@Component
public class LiveTestFixtureProvisioner {

  private static final Logger log = LoggerFactory.getLogger(LiveTestFixtureProvisioner.class);

  public static final Long TEST_TRAINEE_ID = 1005143L;
  public static final String REGISTRATION_NUMBER = "REG-LIVE-VERIFY-001";
  public static final Long STATE_ID = 101L;
  public static final Long DISTRICT_ID = 1001L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private TraineeService traineeService;

  @Autowired
  private TraineeConsentService traineeConsentService;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private InternalSecurityExecutionSupport internalSecurityExecutionSupport;

  /**
   * Explicit opt-in method to provision required geography, trainee, and consent fixtures if absent.
   */
  public void provisionFixturesIfAbsent() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
      assertThat(conn.getCatalog()).isEqualTo("SIH26135_TEST");
    }

    InternalServicePrincipal principal = internalSecurityExecutionSupport
        .createAuthorizedNationwidePrincipal("LIVE_TEST_DATA_SETUP");

    internalSecurityExecutionSupport.runAsInternalService(principal, () -> {
      // 1. Geography Dependencies
      Integer stateCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM states WHERE state_id = ?", Integer.class, STATE_ID);
      if (stateCount == null || stateCount == 0) {
        jdbcTemplate.update(
            "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
                + "VALUES (?, 'DL', 'Delhi', 1, NOW(), NOW())",
            STATE_ID);
        log.info("[FixtureProvisioner] Created state DL (Delhi) with ID {}", STATE_ID);
      }

      Integer districtCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM districts WHERE district_id = ?", Integer.class, DISTRICT_ID);
      if (districtCount == null || districtCount == 0) {
        jdbcTemplate.update(
            "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
                + "VALUES (?, ?, 'ND', 'New Delhi', 1, NOW(), NOW())",
            DISTRICT_ID, STATE_ID);
        log.info("[FixtureProvisioner] Created district ND (New Delhi) with ID {}", DISTRICT_ID);
      }

      // 2. Trainee Provisioning (Only if completely absent)
      Trainee trainee = traineeRepository.findById(TEST_TRAINEE_ID)
          .orElseGet(() -> traineeRepository.findByRegistrationNumber(REGISTRATION_NUMBER).orElse(null));

      Long traineeId;
      if (trainee == null) {
        String testPhone = System.getenv("SIH_LIVE_TEST_PHONE");
        if (testPhone == null || testPhone.isBlank()) {
          throw new IllegalStateException("SIH_LIVE_TEST_PHONE environment variable is required to provision authorized test trainee");
        }
        String testEmail = System.getenv("SIH_LIVE_TEST_EMAIL");
        if (testEmail == null || testEmail.isBlank()) {
          testEmail = "authorized.tester@example.gov.in";
        }

        CreateTraineeRequest createReq = new CreateTraineeRequest(
            null,
            REGISTRATION_NUMBER,
            "Authorized",
            null,
            "Tester",
            LocalDate.of(2000, 1, 1),
            2L,
            testEmail,
            testPhone,
            "Live Verification Office",
            null,
            "110001",
            null,
            STATE_ID,
            DISTRICT_ID,
            null,
            null,
            1L,
            3L
        );

        TraineeResponse created = traineeService.createTrainee(createReq, 1L);
        traineeId = created.getId();
        log.info("[FixtureProvisioner] Created authorized test trainee: ID={}, RegNo={}", traineeId, REGISTRATION_NUMBER);
      } else {
        traineeId = trainee.getId();
        log.info("[FixtureProvisioner] Preserved existing test trainee: ID={}, RegNo={}", traineeId, REGISTRATION_NUMBER);
      }

      // 3. Consents: Only provision if completely missing. Never restore revoked consents.
      List<Long> requiredConsentTypeIds = List.of(1L, 5L, 6L, 7L, 2L);
      String policyVersion = "v1.0";
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime oneYearLater = now.plusYears(1);

      for (Long consentTypeId : requiredConsentTypeIds) {
        // Check if any record exists (including revoked or expired)
        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = ? AND consent_type_id = ?",
            Integer.class, traineeId, consentTypeId);

        if (totalCount == null || totalCount == 0) {
          String purpose = switch (consentTypeId.intValue()) {
            case 1 -> "DPDP Act 2023 foundational data processing consent for candidate management";
            case 5 -> "Explicit consent for outbound SMS notifications via Twilio";
            case 6 -> "Explicit consent for outbound WhatsApp notifications via Twilio";
            case 7 -> "Explicit consent for outbound Email notifications via SendGrid/SMTP";
            case 2 -> "Explicit consent for employment follow-up and verification milestone notices";
            default -> "Operational consent";
          };

          CreateTraineeConsentRequest consentReq = new CreateTraineeConsentRequest(
              traineeId,
              consentTypeId,
              1L, // GRANTED
              purpose,
              policyVersion,
              now,
              oneYearLater,
              "SYSTEM"
          );

          TraineeConsentResponse consentRes = traineeConsentService.createConsent(consentReq, 1L);
          log.info("[FixtureProvisioner] Created initial consent: TraineeID={}, ConsentType={}, Status=GRANTED",
              traineeId, consentRes.getConsentTypeCode());
        }
      }
    });
  }
}
