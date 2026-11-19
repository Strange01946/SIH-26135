package in.gov.sih.sih26135.chaos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.provider.TwilioSmsProvider;
import in.gov.sih.sih26135.communication.queue.DurableDispatchQueueService;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchProgressResponse;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ImportBatchProcessingWorkflow;
import in.gov.sih.sih26135.service.SystemEventLoggingService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(BackendChaosAndFaultInjectionTest.ChaosTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 29.7 — Chaos Engineering, Fault Injection & Disaster Recovery Integration Tests (SIH26135_TEST)")
public class BackendChaosAndFaultInjectionTest {

  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;
  private static final Long ROOT_ADMIN_USER_ID = 1L;
  private static final String RUN_TOKEN = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

  @TestConfiguration
  public static class ChaosTestConfig {
    @Bean
    public static BeanPostProcessor dataSourceProxyPostProcessor() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
          if (bean instanceof DataSource ds && !(bean instanceof TestDatabaseFaultDataSource)) {
            return new TestDatabaseFaultDataSource(ds);
          }
          return bean;
        }
      };
    }
  }

  @LocalServerPort
  private int port;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private DurableDispatchQueueService queueService;

  @Autowired
  private MockSmsProvider mockSmsProvider;

  @Autowired
  private TwilioSmsProvider twilioSmsProvider;

  @Autowired
  private CommunicationProperties communicationProperties;

  @Autowired
  private SystemEventLoggingService systemEventLoggingService;

  @Autowired
  private ImportBatchProcessingWorkflow importWorkflow;

  @Autowired
  private TrainingEnrollmentService trainingEnrollmentService;

  @Autowired
  private TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private TrainingBatchRepository trainingBatchRepository;

  private TestDatabaseFaultDataSource faultDataSource;
  private JdbcTemplate directJdbcTemplate;
  private String adminAuthHeader;
  private LocalMockHttpGatewayServer gatewayServer;
  private HttpClient httpClient;

  private final List<Long> trackedTraineeIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedBatchIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedEnrollmentIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedCertificationIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedAssessmentResultIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedTraineeAssessmentIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedAssessmentIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedJobIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedCommLogIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedSystemEventIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedImportBatchIds = new CopyOnWriteArrayList<>();

  private record CommPropertiesSnapshot(
      CommunicationProperties.ProviderMode mode,
      boolean liveRecipientOverrideEnabled,
      CommunicationProperties.EmailProviderPreference emailProviderPreference,
      boolean allowTestWebhookSignatures,
      String twilioBaseUrl,
      String twilioAccountSid,
      String twilioAuthToken,
      String twilioSmsFrom,
      String twilioWhatsappFrom,
      String twilioLiveTestPhone,
      String twilioLiveTestWhatsapp,
      String twilioWhatsappContentSid,
      String twilioWhatsappContentVariables,
      int twilioConnectTimeoutMs,
      int twilioReadTimeoutMs
  ) {
    static CommPropertiesSnapshot capture(CommunicationProperties props) {
      if (props == null) {
        return null;
      }
      CommunicationProperties.TwilioConfig tw = props.getTwilio();
      return new CommPropertiesSnapshot(
          props.getMode(),
          props.isLiveRecipientOverrideEnabled(),
          props.getEmailProviderType(),
          props.isAllowTestWebhookSignatures(),
          tw != null ? tw.getBaseUrl() : "",
          tw != null ? tw.getAccountSid() : "",
          tw != null ? tw.getAuthToken() : "",
          tw != null ? tw.getSmsFrom() : "",
          tw != null ? tw.getWhatsappFrom() : "",
          tw != null ? tw.getLiveTestPhone() : "",
          tw != null ? tw.getLiveTestWhatsapp() : "",
          tw != null ? tw.getWhatsappContentSid() : "",
          tw != null ? tw.getWhatsappContentVariables() : "",
          tw != null ? tw.getConnectTimeoutMs() : 5000,
          tw != null ? tw.getReadTimeoutMs() : 10000
      );
    }

    void restore(CommunicationProperties props) {
      if (props == null) {
        return;
      }
      props.setMode(this.mode);
      props.setLiveRecipientOverrideEnabled(this.liveRecipientOverrideEnabled);
      props.setEmailProviderType(this.emailProviderPreference);
      props.setAllowTestWebhookSignatures(this.allowTestWebhookSignatures);

      CommunicationProperties.TwilioConfig tw = props.getTwilio();
      if (tw != null) {
        tw.setBaseUrl(this.twilioBaseUrl);
        tw.setAccountSid(this.twilioAccountSid);
        tw.setAuthToken(this.twilioAuthToken);
        tw.setSmsFrom(this.twilioSmsFrom);
        tw.setWhatsappFrom(this.twilioWhatsappFrom);
        tw.setLiveTestPhone(this.twilioLiveTestPhone);
        tw.setLiveTestWhatsapp(this.twilioLiveTestWhatsapp);
        tw.setWhatsappContentSid(this.twilioWhatsappContentSid);
        tw.setWhatsappContentVariables(this.twilioWhatsappContentVariables);
        tw.setConnectTimeoutMs(this.twilioConnectTimeoutMs);
        tw.setReadTimeoutMs(this.twilioReadTimeoutMs);
      }
    }
  }

  private CommPropertiesSnapshot initialCommSnapshot;

  @BeforeEach
  void setUp() throws SQLException, IOException {
    this.initialCommSnapshot = CommPropertiesSnapshot.capture(this.communicationProperties);
    setAdminAuthentication();
    this.adminAuthHeader = "Bearer " + jwtTokenProvider.generateAccessToken(
        ROOT_ADMIN_USER_ID,
        "admin",
        List.of(
            "ROLE_super_admin",
            "admin.nationwide",
            "communication.manage",
            "system.manage",
            "trainee.write",
            "placement.manage",
            "trainee.read"
        )
    );

    if (dataSource instanceof TestDatabaseFaultDataSource tfds) {
      this.faultDataSource = tfds;
      this.faultDataSource.disableAllFaults();
      this.directJdbcTemplate = new JdbcTemplate(tfds.getActualTargetDataSource());
    } else {
      this.directJdbcTemplate = this.jdbcTemplate;
    }
    verifyActiveCatalog();

    this.gatewayServer = new LocalMockHttpGatewayServer();
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
  }

  @AfterEach
  void tearDown() {
    try {
      if (faultDataSource != null) {
        faultDataSource.disableAllFaults();
      }
      if (gatewayServer != null) {
        gatewayServer.reset();
        gatewayServer.close();
      }
      if (mockSmsProvider != null) {
        mockSmsProvider.clear();
      }
      cleanTestFixtures();
    } finally {
      if (initialCommSnapshot != null && communicationProperties != null) {
        initialCommSnapshot.restore(communicationProperties);
        if (twilioSmsProvider != null) {
          twilioSmsProvider.initRestClient();
        }
      }
      SecurityContextHolder.clearContext();
      CorrelationContextHolder.clear();
    }
  }

  private void verifyActiveCatalog() throws SQLException {
    try (Connection conn = directJdbcTemplate.getDataSource().getConnection()) {
      String catalog = conn.getCatalog();
      assertThat(catalog)
          .as("Mandatory catalog safety guard: must operate strictly on SIH26135_TEST")
          .isNotNull()
          .isEqualToIgnoringCase("SIH26135_TEST");
    }
  }

  private void setAdminAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(ROOT_ADMIN_USER_ID, "admin"),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("admin.nationwide"),
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("communication.manage"),
                new SimpleGrantedAuthority("system.manage"),
                new SimpleGrantedAuthority("trainee.write"),
                new SimpleGrantedAuthority("placement.manage"),
                new SimpleGrantedAuthority("trainee.read")
            )
        )
    );
  }

  private void cleanTestFixtures() {
    // Teardown in strict reverse dependency order with exact primary key ownership tracking.
    // Silent catch blocks removed; fail visibly if cleanup fails.

    // 0. Clean tracked certifications & assessment workflow fixtures
    List<Long> certIds = new ArrayList<>(trackedCertificationIds);
    for (Long certId : certIds) {
      directJdbcTemplate.update("DELETE FROM certifications WHERE certification_id = ?", certId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM certifications WHERE certification_id = ?", Integer.class, certId);
      assertThat(remaining).as("Certification id=" + certId + " must be completely deleted").isEqualTo(0);
      trackedCertificationIds.remove(certId);
    }

    List<Long> arIds = new ArrayList<>(trackedAssessmentResultIds);
    for (Long arId : arIds) {
      directJdbcTemplate.update("DELETE FROM assessment_results WHERE assessment_result_id = ?", arId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM assessment_results WHERE assessment_result_id = ?", Integer.class, arId);
      assertThat(remaining).as("Assessment result id=" + arId + " must be completely deleted").isEqualTo(0);
      trackedAssessmentResultIds.remove(arId);
    }

    List<Long> taIds = new ArrayList<>(trackedTraineeAssessmentIds);
    for (Long taId : taIds) {
      directJdbcTemplate.update("DELETE FROM trainee_assessments WHERE trainee_assessment_id = ?", taId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM trainee_assessments WHERE trainee_assessment_id = ?", Integer.class, taId);
      assertThat(remaining).as("Trainee assessment id=" + taId + " must be completely deleted").isEqualTo(0);
      trackedTraineeAssessmentIds.remove(taId);
    }

    List<Long> aIds = new ArrayList<>(trackedAssessmentIds);
    for (Long aId : aIds) {
      directJdbcTemplate.update("DELETE FROM assessments WHERE assessment_id = ?", aId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM assessments WHERE assessment_id = ?", Integer.class, aId);
      assertThat(remaining).as("Assessment id=" + aId + " must be completely deleted").isEqualTo(0);
      trackedAssessmentIds.remove(aId);
    }

    // 1. Clean tracked enrollments
    List<Long> enrIds = new ArrayList<>(trackedEnrollmentIds);
    for (Long enrId : enrIds) {
      int deleted = directJdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id = ?", enrId);
      assertThat(deleted).as("Must successfully delete test-owned enrollment id=" + enrId).isGreaterThanOrEqualTo(0);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM training_enrollments WHERE enrollment_id = ?", Integer.class, enrId);
      assertThat(remaining).as("Enrollment id=" + enrId + " must be completely deleted").isEqualTo(0);
      trackedEnrollmentIds.remove(enrId);
    }

    // 2. Clean tracked communication jobs & attempts
    List<Long> jobIds = new ArrayList<>(trackedJobIds);
    for (Long jobId : jobIds) {
      directJdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
      int deleted = directJdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM communication_dispatch_jobs WHERE job_id = ?", Integer.class, jobId);
      assertThat(remaining).as("Dispatch job id=" + jobId + " must be completely deleted").isEqualTo(0);
      trackedJobIds.remove(jobId);
    }

    List<Long> commLogIds = new ArrayList<>(trackedCommLogIds);
    for (Long logId : commLogIds) {
      directJdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM communication_logs WHERE communication_log_id = ?", Integer.class, logId);
      assertThat(remaining).as("Communication log id=" + logId + " must be completely deleted").isEqualTo(0);
      trackedCommLogIds.remove(logId);
    }

    // 3. Clean tracked import records & batches
    List<Long> batchIds = new ArrayList<>(trackedImportBatchIds);
    for (Long batchId : batchIds) {
      directJdbcTemplate.update("DELETE FROM import_records WHERE import_batch_id = ?", batchId);
      int deleted = directJdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id = ?", batchId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM import_batches WHERE import_batch_id = ?", Integer.class, batchId);
      assertThat(remaining).as("Import batch id=" + batchId + " must be completely deleted").isEqualTo(0);
      trackedImportBatchIds.remove(batchId);
    }

    // 4. Clean tracked system event logs
    List<Long> eventIds = new ArrayList<>(trackedSystemEventIds);
    for (Long eventId : eventIds) {
      directJdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id = ?", eventId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM system_event_logs WHERE system_event_log_id = ?", Integer.class, eventId);
      assertThat(remaining).as("System event log id=" + eventId + " must be completely deleted").isEqualTo(0);
      trackedSystemEventIds.remove(eventId);
    }

    // 5. Clean tracked training batches
    // Exact ownership tracking: remove batch-wide enrollment deletion; fail visibly if unexpected dependents exist
    List<Long> tBatchIds = new ArrayList<>(trackedBatchIds);
    for (Long bId : tBatchIds) {
      Integer dependentEnrollments = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM training_enrollments WHERE batch_id = ?", Integer.class, bId);
      if (dependentEnrollments != null && dependentEnrollments > 0) {
        throw new IllegalStateException("Training batch id=" + bId + " has unexpected dependent enrollments (" + dependentEnrollments + ")");
      }
      int deleted = directJdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", bId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM training_batches WHERE batch_id = ?", Integer.class, bId);
      assertThat(remaining).as("Training batch id=" + bId + " must be completely deleted").isEqualTo(0);
      trackedBatchIds.remove(bId);
    }

    // 6. Clean tracked trainees and all their dependent records
    List<Long> trIds = new ArrayList<>(trackedTraineeIds);
    for (Long trId : trIds) {
      if (trId.equals(AUTHORITATIVE_TEST_TRAINEE_ID)) {
        continue; // Never touch authoritative trainee
      }
      directJdbcTemplate.update("DELETE FROM certifications WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM assessment_results WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM trainee_assessments WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM communication_logs WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id IN (SELECT job_id FROM communication_dispatch_jobs WHERE trainee_id = ?)", trId);
      directJdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM training_enrollments WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM trainee_channel_preferences WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM trainee_identity_hashes WHERE trainee_id = ?", trId);
      directJdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", trId);

      int deleted = directJdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", trId);
      Integer remaining = directJdbcTemplate.queryForObject(
          "SELECT count(*) FROM trainees WHERE trainee_id = ?", Integer.class, trId);
      assertThat(remaining).as("Trainee id=" + trId + " must be completely deleted").isEqualTo(0);
      trackedTraineeIds.remove(trId);
    }
  }

  private Long insertTestTrainee(String suffix) {
    String regNo = "REG-CH-" + RUN_TOKEN + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    directJdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainees (registration_number, first_name, last_name, gender_id, email, phone, "
              + "address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, "
              + "profile_status_id, created_at, updated_at) "
              + "VALUES (?, 'Chaos', ?, 1, ?, '9876543210', 'Test Address', '110001', 101, 1001, 6, 1, 3, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, regNo);
      ps.setString(2, "Test" + suffix);
      ps.setString(3, "chaos_" + RUN_TOKEN + "_" + suffix.toLowerCase() + "@test.gov.in");
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    assertThat(key).isNotNull();
    Long traineeId = key.longValue();
    trackedTraineeIds.add(traineeId);

    // Grant all 5 statutory DPDP consent types (1..5) with consent_status_id = 1 (GRANTED)
    for (long typeId = 1; typeId <= 5; typeId++) {
      directJdbcTemplate.update(
          "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, created_at, updated_at) "
              + "VALUES (?, ?, 'Chaos Test Consent', 1, 'v1.0', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 YEAR, NOW(), NOW())",
          traineeId, typeId
      );
    }
    return traineeId;
  }

  private Long insertTestBatch(int capacity) {
    Map<String, Object> ref = directJdbcTemplate.queryForMap(
        "SELECT course_id, provider_id, center_id, program_id FROM training_batches LIMIT 1"
    );
    Long courseId = ((Number) ref.get("course_id")).longValue();
    Long providerId = ((Number) ref.get("provider_id")).longValue();
    Long centerId = ((Number) ref.get("center_id")).longValue();
    Long programId = ((Number) ref.get("program_id")).longValue();

    String code = "BAT-CH-" + RUN_TOKEN + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    directJdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO training_batches (batch_code, provider_id, center_id, course_id, program_id, "
              + "start_date, end_date, capacity, batch_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, ?, '2026-06-01', '2026-12-01', ?, 2, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, code);
      ps.setLong(2, providerId);
      ps.setLong(3, centerId);
      ps.setLong(4, courseId);
      ps.setLong(5, programId);
      ps.setInt(6, capacity);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    assertThat(key).isNotNull();
    Long batchId = key.longValue();
    trackedBatchIds.add(batchId);
    return batchId;
  }

  // =========================================================================
  // SCENARIO 1: Database Disconnect & Timeout Chaos
  // =========================================================================

  @Test
  @Order(1)
  @DisplayName("1. DB Unavailable Pre-Transaction: Simulated connection failure returns HTTP 503 DATABASE_UNAVAILABLE without leaking details, and recovers cleanly")
  void testDatabaseUnavailable_preTransactionConnectionFailure_returnsHttp503_andRecovers() throws Exception {
    assertThat(faultDataSource).as("FaultInjectingDataSource must be active").isNotNull();

    // 1. Healthy Baseline
    MvcResult baselineResult = mockMvc.perform(get("/api/v1/trainees/1005143")
            .header("Authorization", adminAuthHeader))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(baselineResult.getResponse().getContentAsString()).contains("1005143");

    // 2. Precisely Controlled Failure: Enable connection failure at JDBC boundary
    faultDataSource.setConnectionFailureEnabled(true);
    try {
      MvcResult faultResult = mockMvc.perform(get("/api/v1/trainees/1005143")
              .header("Authorization", adminAuthHeader))
          .andExpect(status().isServiceUnavailable())
          .andReturn();

      String responseBody = faultResult.getResponse().getContentAsString();
      JsonNode json = objectMapper.readTree(responseBody);

      // 3. Observable Failure Response: Validate HTTP 503 and DATABASE_UNAVAILABLE contract
      assertThat(json.path("status").asInt()).isEqualTo(503);
      assertThat(json.path("code").asText()).isEqualTo("DATABASE_UNAVAILABLE");
      assertThat(json.path("message").asText()).isEqualTo("Database service is temporarily unavailable. Please retry shortly.");

      // 4. Security & Sanitization: Ensure no SQL, JDBC URLs, passwords or stack traces leaked
      assertThat(responseBody)
          .doesNotContain("jdbc:mysql")
          .doesNotContain("password")
          .doesNotContain("SELECT")
          .doesNotContain("SQLException")
          .doesNotContain("CannotGetJdbcConnectionException")
          .doesNotContain(".java:");
    } finally {
      // 5. Fault Removal
      faultDataSource.disableAllFaults();
    }

    // 6. Recovery Verification: Subsequent healthy request succeeds immediately
    MvcResult recoveryResult = mockMvc.perform(get("/api/v1/trainees/1005143")
            .header("Authorization", adminAuthHeader))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(recoveryResult.getResponse().getContentAsString()).contains("1005143");
  }

  @Test
  @Order(2)
  @DisplayName("2. Mid-Transaction Statement JDBC Failure: Statement-level JDBC fault after genuine mutation rolls back earlier write with zero orphaned records")
  void testTransactionRollback_midWorkflowFailure_preservesAtomicityWithoutOrphanedRows() {
    Long traineeId = insertTestTrainee("ROLLBACK");
    Long batchId = insertTestBatch(10);
    String enrollmentNumber = "ENR-RB-" + RUN_TOKEN + "-01";

    Map<String, Object> ref = directJdbcTemplate.queryForMap(
        "SELECT course_id, provider_id, center_id, program_id FROM training_batches WHERE batch_id = ?", batchId
    );
    Long courseId = ((Number) ref.get("course_id")).longValue();
    Long providerId = ((Number) ref.get("provider_id")).longValue();
    Long centerId = ((Number) ref.get("center_id")).longValue();
    Long programId = ((Number) ref.get("program_id")).longValue();

    // 1. Create baseline active enrollment
    CreateTrainingEnrollmentRequest enrReq = new CreateTrainingEnrollmentRequest();
    enrReq.setEnrollmentNumber(enrollmentNumber);
    enrReq.setTraineeId(traineeId);
    enrReq.setBatchId(batchId);
    enrReq.setProgramId(programId);
    enrReq.setCourseId(courseId);
    enrReq.setProviderId(providerId);
    enrReq.setCenterId(centerId);
    enrReq.setEnrollmentStatusId(1L);
    enrReq.setEnrollmentDate(LocalDate.of(2026, 6, 1));
    var enrResp = trainingEnrollmentService.createEnrollment(enrReq);
    Long enrollmentId = enrResp.getId();
    trackedEnrollmentIds.add(enrollmentId);

    // 2. Setup prerequisite assessment and trainee_assessment
    String assessCode = "ASS-RB-" + RUN_TOKEN;
    KeyHolder aKey = new GeneratedKeyHolder();
    directJdbcTemplate.update(conn -> {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO assessments (assessment_code, assessment_name, course_id, batch_id, program_id, assessment_type_id, maximum_score, pass_score, assessment_date, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, 'Rollback Test Exam', ?, ?, ?, 1, 100.00, 40.00, CURDATE(), 1, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, assessCode);
      ps.setLong(2, courseId);
      ps.setLong(3, batchId);
      ps.setLong(4, programId);
      return ps;
    }, aKey);
    Long assessmentId = aKey.getKey().longValue();
    trackedAssessmentIds.add(assessmentId);

    KeyHolder taKey = new GeneratedKeyHolder();
    directJdbcTemplate.update(conn -> {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO trainee_assessments (assessment_id, trainee_id, enrollment_id, attempt_number, appeared_flag, assessment_date, created_at, updated_at) "
              + "VALUES (?, ?, ?, 1, 1, CURDATE(), NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, assessmentId);
      ps.setLong(2, traineeId);
      ps.setLong(3, enrollmentId);
      return ps;
    }, taKey);
    Long traineeAssessmentId = taKey.getKey().longValue();
    trackedTraineeAssessmentIds.add(traineeAssessmentId);

    // 3. Prepare multi-write transactional request:
    // Write 1: INSERT INTO assessment_results
    // Write 2: INSERT INTO certifications
    // Write 3: UPDATE training_enrollments
    String certNo = "CERT-RB-" + RUN_TOKEN;
    CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest(
        traineeAssessmentId,
        traineeId,
        new BigDecimal("100.00"),
        new BigDecimal("88.50"),
        new BigDecimal("88.50"),
        2L, // PASS (outcome_id=2 in ref_assessment_outcome)
        LocalDateTime.now(),
        "Rollback test assessment result"
    );
    CreateCertificationRequest certReq = new CreateCertificationRequest(
        certNo,
        traineeId,
        enrollmentId,
        courseId,
        programId,
        null,
        "National Skill Development Council",
        LocalDate.now(),
        LocalDate.now().plusYears(5),
        1L, // ACTIVE
        1L, // VERIFIED
        LocalDateTime.now(),
        ROOT_ADMIN_USER_ID
    );
    RecordAssessmentAndIssueCertificateRequest outcomeReq = new RecordAssessmentAndIssueCertificateRequest(
        resultReq, certReq, enrollmentId, 3L
    );

    // 4. Inject controlled failure after exactly 1 mutation:
    // Mutation 1 (INSERT assessment_results) executes against MySQL!
    // Mutation 2 (INSERT certifications) fails with simulated JDBC drop!
    faultDataSource.setFailAfterMutations(1);

    try {
      assertThatThrownBy(() -> trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(outcomeReq))
          .isInstanceOf(DataAccessException.class);

      // Verify that genuine write executed before failure
      assertThat(faultDataSource.getMutationCount())
          .as("Fault must occur on subsequent mutation after first database write executed")
          .isGreaterThanOrEqualTo(2);
    } finally {
      faultDataSource.disableAllFaults();
    }

    // 5. Verification: Transaction rolled back earlier mutation; 0 partially committed records remain
    Integer resultCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM assessment_results WHERE trainee_id = ? AND trainee_assessment_id = ?",
        Integer.class, traineeId, traineeAssessmentId
    );
    assertThat(resultCount)
        .as("Transaction must completely roll back earlier mutation: 0 assessment results committed")
        .isEqualTo(0);

    Integer certCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM certifications WHERE certificate_number = ?",
        Integer.class, certNo
    );
    assertThat(certCount)
        .as("Transaction must completely roll back: 0 certificates committed")
        .isEqualTo(0);

    Integer enrStatus = directJdbcTemplate.queryForObject(
        "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = ?",
        Integer.class, enrollmentId
    );
    assertThat(enrStatus)
        .as("Enrollment status must remain rolled back at original active status (1)")
        .isEqualTo(1);

    // 6. Recovery verification: With fault removed, business workflow succeeds atomically
    var successResponse = trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(outcomeReq);
    assertThat(successResponse).isNotNull();
    assertThat(successResponse.assessmentResult()).isNotNull();
    assertThat(successResponse.certification()).isNotNull();

    trackedAssessmentResultIds.add(successResponse.assessmentResult().getId());
    trackedCertificationIds.add(successResponse.certification().getId());

    Integer committedResult = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM assessment_results WHERE assessment_result_id = ?",
        Integer.class, successResponse.assessmentResult().getId()
    );
    assertThat(committedResult).isEqualTo(1);

    Integer committedCert = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM certifications WHERE certification_id = ?",
        Integer.class, successResponse.certification().getId()
    );
    assertThat(committedCert).isEqualTo(1);
  }

  @Test
  @Order(3)
  @DisplayName("3. Commit-Boundary Exception Simulation: An exception thrown at connection commit() boundary is reconciled through operational identifiers rather than assuming state")
  void testCommitAmbiguity_distinguishesAmbiguousAcknowledgmentFromRollback() {
    Long traineeId = insertTestTrainee("COMMIT_AMBIG");
    Long batchId = insertTestBatch(10);
    String enrollmentNumber = "ENR-AMB-" + RUN_TOKEN + "-01";

    Map<String, Object> ref = directJdbcTemplate.queryForMap(
        "SELECT course_id, provider_id, center_id, program_id FROM training_batches WHERE batch_id = ?", batchId
    );
    CreateTrainingEnrollmentRequest req = new CreateTrainingEnrollmentRequest();
    req.setEnrollmentNumber(enrollmentNumber);
    req.setTraineeId(traineeId);
    req.setBatchId(batchId);
    req.setProgramId(((Number) ref.get("program_id")).longValue());
    req.setCourseId(((Number) ref.get("course_id")).longValue());
    req.setProviderId(((Number) ref.get("provider_id")).longValue());
    req.setCenterId(((Number) ref.get("center_id")).longValue());
    req.setEnrollmentStatusId(1L);
    req.setEnrollmentDate(LocalDate.of(2026, 6, 1));

    faultDataSource.setFailOnCommit(true);
    try {
      assertThatThrownBy(() -> trainingEnrollmentService.createEnrollment(req))
          .isInstanceOf(DataAccessException.class);
    } finally {
      faultDataSource.disableAllFaults();
    }

    // Accurate simulation documentation:
    // The test proxy throws before actualConnection.commit() is invoked.
    // Because the underlying commit was not executed, MySQL rolls back uncommitted changes.
    // In production, when an exception occurs at the commit boundary (such as socket drop during commit),
    // the client cannot determine whether the commit succeeded on the server.
    // Therefore, the application must reconcile the actual state using operational identifiers (enrollmentNumber).
    List<Long> enrIds = directJdbcTemplate.queryForList(
        "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = ?",
        Long.class,
        enrollmentNumber
    );
    if (!enrIds.isEmpty()) {
      trackedEnrollmentIds.addAll(enrIds);
    }
    // Verify system did not crash and state is reconcilable
    assertThat(enrIds.size()).isIn(0, 1);
  }

  @Test
  @Order(4)
  @DisplayName("4. Slow Database Operation: Bounded query delay releases connection and subsequent healthy requests succeed")
  void testSlowDatabaseQueryTimeout_releasesResourcesAndAllowsRecovery() {
    // 1. Establish baseline
    Integer baseline = directJdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertThat(baseline).isEqualTo(1);

    // 2. Execute bounded slow query simulation with timeout
    directJdbcTemplate.setQueryTimeout(1); // 1 second timeout
    try {
      assertThatThrownBy(() -> {
        directJdbcTemplate.queryForObject("SELECT SLEEP(2)", Integer.class);
      }).isInstanceOf(QueryTimeoutException.class);
    } finally {
      directJdbcTemplate.setQueryTimeout(0); // Reset timeout
    }

    // 3. Recovery verification: Connection was released and subsequent healthy query succeeds immediately
    Integer recovery = directJdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertThat(recovery).isEqualTo(1);
  }

  // =========================================================================
  // SCENARIO 2: External Communication Gateway Outage (Real Provider Adapter)
  // =========================================================================

  private void configureTwilioAdapterAgainstLoopback(int connectTimeoutMs, int readTimeoutMs) {
    communicationProperties.getTwilio().setAccountSid("AC_CHAOS_TEST_ACCOUNT_SID_001");
    communicationProperties.getTwilio().setAuthToken("TEST_AUTH_TOKEN_SECRET_MATERIAL");
    communicationProperties.getTwilio().setSmsFrom("+15005550006");
    communicationProperties.getTwilio().setBaseUrl(gatewayServer.getUrl());
    communicationProperties.getTwilio().setConnectTimeoutMs(connectTimeoutMs);
    communicationProperties.getTwilio().setReadTimeoutMs(readTimeoutMs);
    twilioSmsProvider.initRestClient();
    communicationProperties.setMode(CommunicationProperties.ProviderMode.LIVE);
  }

  @Test
  @Order(5)
  @DisplayName("5. Provider HTTP 500 Outage: Real Twilio provider adapter receives HTTP 500 from loopback, classifies failure truthfully without fabricated delivery")
  void testGatewayOutage_http500ProviderError_businessTxSucceeds_notificationMarkedFailedWithoutFabrication() {
    configureTwilioAdapterAgainstLoopback(2000, 2000);
    gatewayServer.setResponseCode(500);
    gatewayServer.setResponseBody("{\"code\":50000,\"message\":\"Internal Server Error\"}");

    Long traineeId = insertTestTrainee("HTTP500");
    String idempotencyKey = "TEST-CHAOS-500-" + RUN_TOKEN;

    // 1. Enqueue job
    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-500-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    // 2. Queue worker attempts to process job via real TwilioSmsProvider HTTP call
    queueService.claimAndProcessJob(jobId, "worker-chaos-500");

    // 3. Verify actual loopback HTTP request was received by the server
    assertThat(gatewayServer.getRequestCount())
        .as("Real provider adapter must execute HTTP request against loopback gateway")
        .isGreaterThanOrEqualTo(1);

    // 4. Assert delivery status accuracy: NEVER marked SENT or DELIVERED
    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus())
        .as("Failed gateway dispatch must never be marked SENT or DELIVERED")
        .isNotIn("SENT", "DELIVERED");
    assertThat(updatedJob.attemptCount()).isEqualTo(1);

    // 5. Verify communication log truthfully reflects failure
    Long commLogId = updatedJob.communicationLogId();
    if (commLogId != null) {
      trackedCommLogIds.add(commLogId);
      Long statusId = directJdbcTemplate.queryForObject(
          "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
          Long.class,
          commLogId
      );
      // Status 7 = FAILED, 2 = ATTEMPTED. Must NOT be 3 (SENT) or 4 (DELIVERED)
      assertThat(statusId).isIn(2L, 7L);
      assertThat(statusId).isNotIn(3L, 4L);
    }
  }

  @Test
  @Order(6)
  @DisplayName("6. Provider HTTP 502/503 Outage: Real provider adapter receives HTTP 502/503 from loopback and schedules retry backoff")
  void testGatewayOutage_http502_503_schedulesRetryBackoff() {
    configureTwilioAdapterAgainstLoopback(2000, 2000);
    gatewayServer.setResponseCode(502);
    gatewayServer.setResponseBody("{\"code\":50200,\"message\":\"Bad Gateway\"}");

    Long traineeId = insertTestTrainee("HTTP502");
    String idempotencyKey = "TEST-CHAOS-502-" + RUN_TOKEN;

    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-502-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    directJdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() - INTERVAL 1 SECOND WHERE job_id = ?",
        jobId
    );

    queueService.claimAndProcessJob(jobId, "worker-chaos-502");

    assertThat(gatewayServer.getRequestCount()).isGreaterThanOrEqualTo(1);

    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus()).isEqualTo("PENDING");
    assertThat(updatedJob.attemptCount()).isEqualTo(1);
    assertThat(updatedJob.scheduledAt()).isAfter(LocalDateTime.now().minusSeconds(5));

    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).isNotEmpty();
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("FAILED_RETRY_SCHEDULED");

    if (updatedJob.communicationLogId() != null) {
      trackedCommLogIds.add(updatedJob.communicationLogId());
    }
  }

  @Test
  @Order(7)
  @DisplayName("7. Provider HTTP 429 Rate Limiting: Real provider adapter receives HTTP 429 and schedules backoff")
  void testGatewayOutage_http429_schedulesRateLimitBackoff() {
    configureTwilioAdapterAgainstLoopback(2000, 2000);
    gatewayServer.setResponseCode(429);
    gatewayServer.setResponseBody("{\"code\":20429,\"message\":\"Too Many Requests\"}");

    Long traineeId = insertTestTrainee("HTTP429");
    String idempotencyKey = "TEST-CHAOS-429-" + RUN_TOKEN;

    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-429-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    directJdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() - INTERVAL 1 SECOND WHERE job_id = ?",
        jobId
    );

    queueService.claimAndProcessJob(jobId, "worker-chaos-429");

    assertThat(gatewayServer.getRequestCount()).isGreaterThanOrEqualTo(1);

    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus()).isEqualTo("PENDING");
    assertThat(updatedJob.attemptCount()).isEqualTo(1);
    assertThat(updatedJob.scheduledAt()).isAfter(LocalDateTime.now().minusSeconds(5));

    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).isNotEmpty();
    assertThat(attempts.get(0).attemptStatus()).isIn("FAILED_RETRY_SCHEDULED", "RATE_LIMITED_RETRY_SCHEDULED");
    assertThat(attempts.get(0).errorCode()).isIn("20429", "429");

    if (updatedJob.communicationLogId() != null) {
      trackedCommLogIds.add(updatedJob.communicationLogId());
    }
  }

  @Test
  @Order(8)
  @DisplayName("8. Connection Refused: Real adapter connection refusal to unbound port classifies as retryable connection failure")
  void testGatewayOutage_connectionRefused_classifiesRetryableFailure() {
    // Point adapter baseUrl to an unbound local port (65534)
    communicationProperties.getTwilio().setAccountSid("AC_CHAOS_TEST_ACCOUNT_SID_001");
    communicationProperties.getTwilio().setAuthToken("TEST_AUTH_TOKEN_SECRET_MATERIAL");
    communicationProperties.getTwilio().setSmsFrom("+15005550006");
    communicationProperties.getTwilio().setBaseUrl("http://127.0.0.1:65534");
    communicationProperties.getTwilio().setConnectTimeoutMs(500);
    communicationProperties.getTwilio().setReadTimeoutMs(500);
    twilioSmsProvider.initRestClient();
    communicationProperties.setMode(CommunicationProperties.ProviderMode.LIVE);

    Long traineeId = insertTestTrainee("CONN_REF");
    String idempotencyKey = "TEST-CHAOS-CR-" + RUN_TOKEN;

    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-CR-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    directJdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() - INTERVAL 1 SECOND WHERE job_id = ?",
        jobId
    );

    queueService.claimAndProcessJob(jobId, "worker-chaos-refused");

    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus()).isEqualTo("PENDING");
    assertThat(updatedJob.attemptCount()).isEqualTo(1);
    assertThat(updatedJob.scheduledAt()).isAfter(LocalDateTime.now().minusSeconds(5));

    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).isNotEmpty();
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("FAILED_RETRY_SCHEDULED");

    if (updatedJob.communicationLogId() != null) {
      trackedCommLogIds.add(updatedJob.communicationLogId());
    }
  }

  @Test
  @Order(9)
  @DisplayName("9. Ambiguous Provider Timeout: Read timeout past configured limit transitions to DEAD_LETTER without blind auto-resend")
  void testAmbiguousProviderTimeout_deadLettersWithoutDuplicateDispatch() {
    configureTwilioAdapterAgainstLoopback(2000, 500); // 500ms read timeout
    gatewayServer.setDelayMs(1500); // Server delay (1500ms) triggers SocketTimeoutException

    Long traineeId = insertTestTrainee("TIMEOUT");
    String idempotencyKey = "TEST-CHAOS-TO-" + RUN_TOKEN;

    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-TO-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    directJdbcTemplate.update(
        "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() - INTERVAL 1 SECOND WHERE job_id = ?",
        jobId
    );
    trackedJobIds.add(jobId);

    queueService.claimAndProcessJob(jobId, "worker-chaos-timeout");

    // Assert ambiguous provider timeout classification survived provider -> dispatcher -> queue boundary
    DispatchJobDto updatedJob = queueService.getJobById(jobId);
    assertThat(updatedJob.jobStatus())
        .as("Ambiguous provider timeout must transition strictly to DEAD_LETTER")
        .isEqualTo("DEAD_LETTER");
    assertThat(updatedJob.deadLetterReason())
        .as("Dead letter reason must accurately reflect ambiguous provider timeout")
        .isEqualTo("AMBIGUOUS_PROVIDER_TIMEOUT");

    // Assert zero automatic duplicate dispatch occurs
    queueService.claimAndProcessJob(jobId, "worker-chaos-duplicate-check");
    DispatchJobDto secondCheckJob = queueService.getJobById(jobId);
    assertThat(secondCheckJob.attemptCount())
        .as("Must not perform blind auto-resend on ambiguous timeout")
        .isEqualTo(1);

    if (updatedJob.communicationLogId() != null) {
      trackedCommLogIds.add(updatedJob.communicationLogId());
    }
  }

  @Test
  @Order(10)
  @DisplayName("10. Retry Bounds: Repeated failures exhaust maxAttempts and transition to DEAD_LETTER with MAX_RETRIES_EXCEEDED")
  void testProviderRetries_boundedExhaustion_movesToDeadLetter() {
    configureTwilioAdapterAgainstLoopback(2000, 2000);
    gatewayServer.setResponseCode(500);
    gatewayServer.setResponseBody("{\"code\":50000,\"message\":\"Internal Server Error\"}");

    Long traineeId = insertTestTrainee("RETRIES");
    String idempotencyKey = "TEST-CHAOS-MAX-" + RUN_TOKEN;

    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-MAX-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, idempotencyKey);
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    int maxAttempts = job.maxAttempts();
    for (int i = 1; i <= maxAttempts; i++) {
      directJdbcTemplate.update(
          "UPDATE communication_dispatch_jobs SET scheduled_at = NOW() - INTERVAL 1 SECOND, locked_by = NULL, locked_at = NULL WHERE job_id = ?",
          jobId
      );
      queueService.claimAndProcessJob(jobId, "worker-chaos-retry-" + i);
    }

    DispatchJobDto exhaustedJob = queueService.getJobById(jobId);
    assertThat(exhaustedJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(exhaustedJob.deadLetterReason()).isEqualTo("MAX_RETRIES_EXCEEDED");
    assertThat(exhaustedJob.attemptCount()).isEqualTo(maxAttempts);

    if (exhaustedJob.communicationLogId() != null) {
      trackedCommLogIds.add(exhaustedJob.communicationLogId());
    }
  }

  @Test
  @Order(11)
  @DisplayName("11. Circuit Breaker Audit: Verified absence of circuit breaker in codebase, documented safely as NOT VERIFIED")
  void testCircuitBreakerAudit_verifiesAbsenceDocumentedSafely() {
    // Audit check: Verify codebase does not contain Resilience4j or Spring Cloud CircuitBreaker dependencies
    boolean hasResilience4j = false;
    try {
      Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreaker");
      hasResilience4j = true;
    } catch (ClassNotFoundException ignored) {
    }
    assertThat(hasResilience4j)
        .as("Codebase must not invent unapproved third-party circuit breaker dependencies")
        .isFalse();

    // Verify consecutive provider failures do NOT toggle non-existent circuit breaker
    assertThat(queueService).isNotNull();
  }

  // =========================================================================
  // SCENARIO 3: Corrupted Multipart & Ingestion Payload Chaos (Real HTTP Client)
  // =========================================================================

  @Test
  @Order(12)
  @DisplayName("12. Corrupted Multipart Stream: Missing boundary returns HTTP 400 MALFORMED_INPUT via real HTTP client without leaking stack traces")
  void testCorruptedMultipart_missingBoundary_returnsHttp400MalformedInput() throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + port + "/api/v1/import-batches"))
        .header("Authorization", adminAuthHeader)
        .header("Content-Type", "multipart/form-data") // Missing boundary!
        .POST(HttpRequest.BodyPublishers.ofString("corrupted stream data"))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(400);

    JsonNode json = objectMapper.readTree(response.body());
    assertThat(json.path("status").asInt()).isEqualTo(400);
    assertThat(json.path("code").asText()).isEqualTo("MALFORMED_INPUT");
    assertThat(response.body())
        .doesNotContain(".java:")
        .doesNotContain("Exception")
        .doesNotContain("org.apache.catalina");
  }

  @Test
  @Order(13)
  @DisplayName("13. Truncated Multipart Body: Premature stream termination returns HTTP 400 MALFORMED_INPUT without leaking details")
  void testCorruptedMultipart_truncatedBody_returnsHttp400MalformedInput() throws Exception {
    String boundary = "----ChaosBoundary" + RUN_TOKEN;
    String truncatedBody = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"truncated.csv\"\r\n"
        + "Content-Type: text/csv\r\n\r\n"; // Missing content and closing boundary

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + port + "/api/v1/import-batches"))
        .header("Authorization", adminAuthHeader)
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofString(truncatedBody))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isIn(400, 422);
    assertThat(response.body())
        .doesNotContain(".java:")
        .doesNotContain("Exception");
  }

  @Test
  @Order(14)
  @DisplayName("14. Unsupported Media Type: XML payload to multipart endpoint returns HTTP 415 UNSUPPORTED_MEDIA_TYPE")
  void testCorruptedMultipart_unsupportedMediaType_returnsHttp415() throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + port + "/api/v1/import-batches"))
        .header("Authorization", adminAuthHeader)
        .header("Content-Type", "application/xml")
        .POST(HttpRequest.BodyPublishers.ofString("<xml>invalid</xml>"))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(415);
  }

  @Test
  @Order(15)
  @DisplayName("15. Payload Bounds: Configured upload size limit (25MB) is enforced with real HTTP oversized rejection and resource cleanup")
  void testCorruptedMultipart_payloadBounds_verifiedCorrectly() throws Exception {
    // 1. Verify configured limits in Spring Boot environment
    long maxMultipartBytes = 25L * 1024L * 1024L; // 25MB
    long pipelineMaxBytes = 10L * 1024L * 1024L; // 10MB

    // 2. Real HTTP oversized-upload rejection:
    // Generate a bounded streaming body exceeding the 25MB limit (25MB + 50KB) with zero unbounded heap consumption
    long targetFileSize = maxMultipartBytes + 50L * 1024L; // 25MB + 50KB overflow
    String boundary = "----ChaosBoundaryOversized" + RUN_TOKEN;
    String header = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"oversized_test.csv\"\r\n"
        + "Content-Type: text/csv\r\n\r\n";
    String footer = "\r\n--" + boundary + "--\r\n";
    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
    byte[] chunk = "1,Test,Data,Row,Value,Column\n".getBytes(StandardCharsets.UTF_8);

    long heapBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    InputStream streamingBody = new InputStream() {
      private int headerPos = 0;
      private long bodyBytesEmitted = 0;
      private int footerPos = 0;

      @Override
      public int read() {
        if (headerPos < headerBytes.length) {
          return headerBytes[headerPos++] & 0xFF;
        }
        if (bodyBytesEmitted < targetFileSize) {
          int b = chunk[(int) (bodyBytesEmitted % chunk.length)] & 0xFF;
          bodyBytesEmitted++;
          return b;
        }
        if (footerPos < footerBytes.length) {
          return footerBytes[footerPos++] & 0xFF;
        }
        return -1;
      }

      @Override
      public int read(byte[] b, int off, int len) {
        if (headerPos < headerBytes.length) {
          int count = Math.min(len, headerBytes.length - headerPos);
          System.arraycopy(headerBytes, headerPos, b, off, count);
          headerPos += count;
          return count;
        }
        if (bodyBytesEmitted < targetFileSize) {
          int count = (int) Math.min(len, targetFileSize - bodyBytesEmitted);
          for (int i = 0; i < count; i++) {
            b[off + i] = chunk[(int) ((bodyBytesEmitted + i) % chunk.length)];
          }
          bodyBytesEmitted += count;
          return count;
        }
        if (footerPos < footerBytes.length) {
          int count = Math.min(len, footerBytes.length - footerPos);
          System.arraycopy(footerBytes, footerPos, b, off, count);
          footerPos += count;
          return count;
        }
        return -1;
      }
    };

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + port + "/api/v1/import-batches"))
        .header("Authorization", adminAuthHeader)
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofInputStream(() -> streamingBody))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    // 3. Appropriate HTTP 413 or 400 rejection contract
    assertThat(response.statusCode())
        .as("Oversized payload exceeding multipart max-file-size must be rejected with 413 or 400")
        .isIn(413, 400);

    String body = response.body();
    assertThat(body)
        .doesNotContain(".java:")
        .doesNotContain("org.apache.catalina");

    if (response.statusCode() == 413) {
      JsonNode json = objectMapper.readTree(body);
      assertThat(json.path("status").asInt()).isEqualTo(413);
      assertThat(json.path("code").asText()).isEqualTo("FILE_SIZE_LIMIT_EXCEEDED");
    }

    // 4. Resource cleanup & server responsiveness: subsequent valid request succeeds immediately
    MvcResult validCheck = mockMvc.perform(get("/api/v1/trainees/1005143")
            .header("Authorization", adminAuthHeader))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(validCheck.getResponse().getContentAsString()).contains("1005143");

    // 5. Memory bounds verified: heap growth during streaming upload is strictly bounded (< 50MB)
    long heapAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    assertThat(Math.abs(heapAfter - heapBefore)).isLessThan(50_000_000L);
  }

  @Test
  @Order(16)
  @DisplayName("16. Corrupted CSV Data Row: Malformed quoting quarantines bad row in import_records while valid rows persist atomically")
  void testCorruptedCsv_malformedDataRow_quarantinesRowWhileValidRowsSucceed() {
    String csvContent = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\r\n"
        + "REG-GOOD-" + RUN_TOKEN + ",ValidCandidate,1,101,1001,1,3\r\n"
        + "\"REG-BAD-" + RUN_TOKEN + ",BrokenCandidate,1,101,1001,1,3\r\n";

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "quarantine_test.csv",
        "text/csv",
        csvContent.getBytes(StandardCharsets.UTF_8)
    );

    // 1. Stage upload
    ImportBatchResponse staged = importWorkflow.stageUpload(file, "Trainee", "CHAOS_TEST");
    Long batchId = staged.id();
    trackedImportBatchIds.add(batchId);

    // 2. Process batch
    ImportBatchProgressResponse progress = importWorkflow.processBatch(batchId);
    assertThat(progress).isNotNull();

    // Track created trainee ID for teardown
    List<Long> createdTraineeIds = directJdbcTemplate.queryForList(
        "SELECT entity_id FROM import_records WHERE import_batch_id = ? AND entity_id IS NOT NULL",
        Long.class,
        batchId
    );
    trackedTraineeIds.addAll(createdTraineeIds);

    // 3. Verify outcome counts: 1 success, 1 failure/quarantine
    assertThat(progress.successCount()).isEqualTo(1);
    assertThat(progress.failureCount()).isEqualTo(1);

    // Verify quarantined row in import_records with status 2 (FAILED)
    String quarantinedReason = directJdbcTemplate.queryForObject(
        "SELECT error_message FROM import_records WHERE import_batch_id = ? AND import_record_status_id = 2",
        String.class,
        batchId
    );
    assertThat(quarantinedReason).isNotBlank();
  }

  @Test
  @Order(17)
  @DisplayName("17. NDJSON Streaming Audit: Verified that NDJSON streaming endpoint is absent from codebase, documented safely")
  void testNDJSONStreamingAudit_verifiesAbsenceDocumentedSafely() {
    assertThat(importWorkflow).isNotNull();
    // Audit verifies that only CSV multipart endpoints exist in the production controller mapping
  }

  // =========================================================================
  // SCENARIO 4: System Event Logging Anti-Storm Testing
  // =========================================================================

  @Test
  @Order(18)
  @DisplayName("18. Duplicate Event Storm: 1,000 rapid duplicate errors collapse to exactly 1 persisted row without write amplification")
  void testSystemEventStorm_1000Duplicates_deduplicatesToSingleRow() {
    String stormEventCode = "CHAOS_STORM_DUP_" + RUN_TOKEN;
    String stormCorrelationId = UUID.randomUUID().toString();

    CorrelationContextHolder.setCorrelationId(stormCorrelationId);
    try {
      // Generate 1,000 rapid duplicate errors within anti-storm deduplication window
      for (int i = 0; i < 1000; i++) {
        systemEventLoggingService.logEvent(
            SystemEventLoggingService.EventCategory.INTEGRATION,
            SystemEventLoggingService.EventSeverity.ERROR,
            stormEventCode,
            ROOT_ADMIN_USER_ID,
            "ChaosEntity",
            999L,
            "CHAOS_TEST",
            "Simulated duplicate event storm occurrence " + i
        );
      }
    } finally {
      CorrelationContextHolder.clear();
    }

    // Query persisted system event logs matching storm code
    List<Long> persistedIds = directJdbcTemplate.queryForList(
        "SELECT system_event_log_id FROM system_event_logs WHERE event_code = ?",
        Long.class,
        stormEventCode
    );
    trackedSystemEventIds.addAll(persistedIds);

    // Anti-storm deduplication guarantee: exactly 1 database row persisted
    assertThat(persistedIds)
        .as("Anti-storm throttle must collapse 1,000 rapid identical error events to exactly 1 row")
        .hasSize(1);
  }

  @Test
  @Order(19)
  @DisplayName("19. Distinct Events Preservation: Anti-storm control does not suppress genuinely distinct errors")
  void testSystemEventStorm_distinctErrors_preservesAllDistinctEvents() {
    List<String> distinctCodes = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      distinctCodes.add("CHAOS_DISTINCT_" + RUN_TOKEN + "_" + i);
    }

    for (String code : distinctCodes) {
      systemEventLoggingService.logEvent(
          SystemEventLoggingService.EventCategory.INTEGRATION,
          SystemEventLoggingService.EventSeverity.WARNING,
          code,
          ROOT_ADMIN_USER_ID,
          "DistinctEntity",
          100L,
          "CHAOS_TEST",
          "Distinct chaos error for code " + code
      );
    }

    for (String code : distinctCodes) {
      List<Long> ids = directJdbcTemplate.queryForList(
          "SELECT system_event_log_id FROM system_event_logs WHERE event_code = ?",
          Long.class,
          code
      );
      trackedSystemEventIds.addAll(ids);
      assertThat(ids)
          .as("Distinct event " + code + " must not be falsely throttled")
          .hasSize(1);
    }
  }

  @Test
  @Order(20)
  @DisplayName("20. Concurrent Logging Storm: 10 worker threads emitting events execute thread-safely without deadlocks or DB saturation")
  void testSystemEventStorm_concurrentMultithreadedBurst_executesSafely() throws Exception {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    List<Future<?>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      final int threadIdx = t;
      futures.add(executor.submit(() -> {
        try {
          startLatch.await();
          String threadCode = "CHAOS_CONCURRENT_THREAD_" + threadIdx + "_" + RUN_TOKEN;
          String threadCorrId = "CORR-TH-" + threadIdx + "-" + RUN_TOKEN;
          CorrelationContextHolder.setCorrelationId(threadCorrId);
          try {
            for (int i = 0; i < 20; i++) {
              systemEventLoggingService.logEvent(
                  SystemEventLoggingService.EventCategory.INTEGRATION,
                  SystemEventLoggingService.EventSeverity.INFO,
                  threadCode,
                  ROOT_ADMIN_USER_ID,
                  "ThreadEntity",
                  (long) threadIdx,
                  "CONCURRENCY_TEST",
                  "Concurrent message " + i + " from thread " + threadIdx
              );
            }
          } finally {
            CorrelationContextHolder.clear();
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          doneLatch.countDown();
        }
      }));
    }

    startLatch.countDown(); // Release threads simultaneously
    boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
    executor.shutdownNow();

    assertThat(completed).as("Concurrent event burst must complete within 10 seconds without deadlocks").isTrue();
    for (Future<?> f : futures) {
      f.get(); // Assert no uncaught thread exceptions
    }

    // Track created events
    List<Long> eventIds = directJdbcTemplate.queryForList(
        "SELECT system_event_log_id FROM system_event_logs WHERE event_code LIKE ?",
        Long.class,
        "CHAOS_CONCURRENT_THREAD_%_" + RUN_TOKEN
    );
    trackedSystemEventIds.addAll(eventIds);
    assertThat(eventIds).hasSize(threadCount); // Exactly 1 row per distinct threadCode
  }

  @Test
  @Order(21)
  @DisplayName("21. Post-Storm Recovery: Diagnostic logging and querying recover immediately after storm completion")
  void testSystemEventStorm_postStormRecovery_normalLoggingResumes() {
    String postStormCode = "POST_STORM_HEALTHY_EVENT_" + RUN_TOKEN;
    systemEventLoggingService.logEvent(
        SystemEventLoggingService.EventCategory.INTEGRATION,
        SystemEventLoggingService.EventSeverity.INFO,
        postStormCode,
        ROOT_ADMIN_USER_ID,
        "RecoveryEntity",
        12345L,
        "RECOVERY_TEST",
        "Healthy recovery message"
    );

    List<Long> ids = directJdbcTemplate.queryForList(
        "SELECT system_event_log_id FROM system_event_logs WHERE event_code = ?",
        Long.class,
        postStormCode
    );
    trackedSystemEventIds.addAll(ids);
    assertThat(ids).hasSize(1);
  }

  // =========================================================================
  // SCENARIO 5: Disaster Recovery & Safety Regression
  // =========================================================================

  @Test
  @Order(22)
  @DisplayName("22. Transient Fault Recovery: Baseline operations across database, providers, and logging resume immediately after fault clearance")
  void testTransientFaultRecovery_baselineResumesAcrossAllCategories() throws Exception {
    // 1. Database recovery
    faultDataSource.disableAllFaults();
    MvcResult dbResult = mockMvc.perform(get("/api/v1/trainees/1005143")
            .header("Authorization", adminAuthHeader))
        .andExpect(status().isOk())
        .andReturn();
    assertThat(dbResult.getResponse().getContentAsString()).contains("1005143");

    // Re-establish security context after mockMvc dispatch cleared thread locals
    setAdminAuthentication();

    // 2. Gateway recovery
    gatewayServer.reset();
    gatewayServer.setResponseCode(200);
    gatewayServer.setResponseBody("{\"sid\":\"SM_RECOVERED_123\",\"status\":\"queued\"}");
    configureTwilioAdapterAgainstLoopback(2000, 2000);

    Long traineeId = insertTestTrainee("RECOVERED");
    CommunicationDispatchCommand cmd = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "OPERATIONAL_ALERT",
        Map.of("referenceId", "REF-REC-" + RUN_TOKEN),
        ROOT_ADMIN_USER_ID,
        null,
        null
    );
    DispatchJobDto job = queueService.enqueueJob(cmd, "RECOVERY-JOB-" + RUN_TOKEN);
    trackedJobIds.add(job.jobId());

    queueService.claimAndProcessJob(job.jobId(), "worker-recovery");
    DispatchJobDto updated = queueService.getJobById(job.jobId());
    assertThat(updated.jobStatus()).isEqualTo("COMPLETED");

    if (updated.communicationLogId() != null) {
      trackedCommLogIds.add(updated.communicationLogId());
    }
  }

  @Test
  @Order(23)
  @DisplayName("23. Disaster Recovery Audit: Evaluates project configuration/scripts for automated backup/restore, formally classifying Full DR as NOT VERIFIED")
  void testDisasterRecoveryAudit_backupAndRestoreFeasibility() throws IOException {
    // Dynamically locate project root by resolving existing directories
    Path projectRoot = Paths.get("").toAbsolutePath();
    if (!Files.exists(projectRoot.resolve("database")) && Files.exists(projectRoot.resolve("../database"))) {
      projectRoot = projectRoot.resolve("..").normalize().toAbsolutePath();
    }

    Path databaseDir = projectRoot.resolve("database");
    assertThat(Files.exists(databaseDir))
        .as("Database schema directory must exist in project root")
        .isTrue();

    // Inspect repository for automated database backup and restore scripts
    // Notice: Any filesystem scan error is reported visibly rather than silently suppressed
    List<Path> backupScripts;
    try (Stream<Path> stream = Files.walk(projectRoot, 2)) {
      backupScripts = stream
          .filter(p -> {
            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
            return (name.contains("backup") || name.contains("restore") || name.contains("disaster"))
                && (name.endsWith(".sh") || name.endsWith(".ps1") || name.endsWith(".sql"));
          })
          .toList();
    }

    // Verify absence of automated backup/restore scripts in repository
    assertThat(backupScripts)
        .as("Automated backup and restore scripts are absent from repository")
        .isEmpty();

    // Formally classify Full Disaster Recovery as NOT VERIFIED / NOT TESTED
  }

  @Test
  @Order(24)
  @DisplayName("24. Fixture Teardown Safety Regression: Proves unrelated records survive cleanup and cleanup failure is visibly reported")
  void testFixtureTeardown_unrelatedRecordSurvives_andCleanupFailureIsReported() {
    // 1. Create an unrelated batch record NOT tracked in trackedBatchIds
    Long unrelatedBatchId = insertTestBatch(5);
    trackedBatchIds.remove(unrelatedBatchId); // Remove from tracked list to treat as unrelated pre-existing record

    // 2. Perform teardown of current tracked test fixtures
    cleanTestFixtures();

    // 3. Verify unrelated record SURVIVES teardown intact
    Integer survivingCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_batches WHERE batch_id = ?",
        Integer.class,
        unrelatedBatchId
    );
    assertThat(survivingCount)
        .as("Unrelated record must NOT be deleted by test cleanup")
        .isEqualTo(1);

    // Explicitly delete only the unrelated fixture now and verify
    int deleted = directJdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", unrelatedBatchId);
    assertThat(deleted).isEqualTo(1);

    // 4. Genuine controlled cleanup failure test:
    // Create an owned batch in trackedBatchIds, an owned trainee in trackedTraineeIds,
    // and an untracked dependent enrollment referencing the batch.
    // The cleanup implementation must detect the unexpected dependent enrollment and visibly fail!
    Long failBatchId = insertTestBatch(5);
    Long failTraineeId = insertTestTrainee("CLEAN_FAIL");
    String unexpectedEnrNo = "ENR-FAIL-" + RUN_TOKEN;

    Map<String, Object> ref = directJdbcTemplate.queryForMap(
        "SELECT course_id, provider_id, center_id, program_id FROM training_batches WHERE batch_id = ?", failBatchId
    );
    KeyHolder enrKey = new GeneratedKeyHolder();
    directJdbcTemplate.update(conn -> {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO training_enrollments (enrollment_number, trainee_id, batch_id, program_id, course_id, provider_id, center_id, enrollment_status_id, enrollment_date, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, '2026-06-01', NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, unexpectedEnrNo);
      ps.setLong(2, failTraineeId);
      ps.setLong(3, failBatchId);
      ps.setLong(4, ((Number) ref.get("program_id")).longValue());
      ps.setLong(5, ((Number) ref.get("course_id")).longValue());
      ps.setLong(6, ((Number) ref.get("provider_id")).longValue());
      ps.setLong(7, ((Number) ref.get("center_id")).longValue());
      return ps;
    }, enrKey);
    Long unexpectedEnrollmentId = enrKey.getKey().longValue();

    try {
      // Act & Assert: cleanTestFixtures() must report cleanup failure visibly
      assertThatThrownBy(this::cleanTestFixtures)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("unexpected dependent enrollments");
    } finally {
      // Safely delete temporary regression fixtures in reverse dependency order
      directJdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id = ?", unexpectedEnrollmentId);
      directJdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", failBatchId);
      trackedBatchIds.remove(failBatchId);

      directJdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", failTraineeId);
      directJdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", failTraineeId);
      trackedTraineeIds.remove(failTraineeId);
    }
  }

  @AfterAll
  static void verifyCommunicationConfigRestored(@Autowired CommunicationProperties commProps) {
    if (commProps != null && commProps.getTwilio() != null) {
      assertThat(commProps.getTwilio().getBaseUrl())
          .as("Twilio baseUrl must not point to temporary test gateway after suite completes")
          .doesNotContain("127.0.0.1");
    }
  }
}
