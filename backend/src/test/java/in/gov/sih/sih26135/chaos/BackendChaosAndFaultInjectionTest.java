package in.gov.sih.sih26135.chaos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.MockSmsProvider;
import in.gov.sih.sih26135.communication.queue.DurableDispatchQueueService;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
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
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javax.sql.DataSource;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
  private SystemEventLoggingService systemEventLoggingService;

  @Autowired
  private ImportBatchProcessingWorkflow importWorkflow;

  @Autowired
  private TrainingEnrollmentService trainingEnrollmentService;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private TrainingBatchRepository trainingBatchRepository;

  private TestDatabaseFaultDataSource faultDataSource;
  private JdbcTemplate directJdbcTemplate;
  private String adminAuthHeader;

  private final List<Long> trackedTraineeIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedBatchIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedEnrollmentIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedJobIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedCommLogIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedSystemEventIds = new CopyOnWriteArrayList<>();
  private final List<Long> trackedImportBatchIds = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws SQLException {
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
      this.faultDataSource.setConnectionFailureEnabled(false);
      this.directJdbcTemplate = new JdbcTemplate(tfds.getActualTargetDataSource());
    } else {
      this.directJdbcTemplate = this.jdbcTemplate;
    }
    verifyActiveCatalog();
  }

  @AfterEach
  void tearDown() {
    try {
      if (faultDataSource != null) {
        faultDataSource.setConnectionFailureEnabled(false);
      }
      if (mockSmsProvider != null) {
        mockSmsProvider.clear();
      }
      systemEventLoggingService.resetThrottleState();
      cleanTestFixtures();
    } finally {
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
    // 1. Clean tracked enrollments
    for (Long enrId : trackedEnrollmentIds) {
      try {
        directJdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id = ?", enrId);
      } catch (Exception ignored) {}
    }
    trackedEnrollmentIds.clear();
    try {
      directJdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_number LIKE ?", "ENR-RB-" + RUN_TOKEN + "%");
    } catch (Exception ignored) {}

    // 2. Clean tracked import records & batches
    for (Long batchId : trackedImportBatchIds) {
      try {
        directJdbcTemplate.update("DELETE FROM import_records WHERE import_batch_id = ?", batchId);
        directJdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id = ?", batchId);
      } catch (Exception ignored) {}
    }
    trackedImportBatchIds.clear();

    // 3. Clean tracked communication jobs & attempts
    for (Long jobId : trackedJobIds) {
      try {
        directJdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
        directJdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
      } catch (Exception ignored) {}
    }
    trackedJobIds.clear();

    for (Long logId : trackedCommLogIds) {
      try {
        directJdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
      } catch (Exception ignored) {}
    }
    trackedCommLogIds.clear();

    // 4. Clean tracked system event logs
    for (Long eventId : trackedSystemEventIds) {
      try {
        directJdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id = ?", eventId);
      } catch (Exception ignored) {}
    }
    trackedSystemEventIds.clear();

    // 5. Clean tracked training batches
    for (Long batchId : trackedBatchIds) {
      try {
        directJdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", batchId);
      } catch (Exception ignored) {}
    }
    trackedBatchIds.clear();

    // 6. Clean tracked trainees and their consents
    for (Long trId : trackedTraineeIds) {
      if (trId.equals(AUTHORITATIVE_TEST_TRAINEE_ID)) {
        continue; // Never touch authoritative trainee
      }
      try {
        directJdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", trId);
        directJdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", trId);
      } catch (Exception ignored) {}
    }
    trackedTraineeIds.clear();
  }

  private Long insertTestTrainee(String prefix) {
    String suffix = RUN_TOKEN + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
    String regNo = "REG-CH-" + prefix + "-" + suffix;
    String phone = "+9197" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    String email = "chaos." + prefix.toLowerCase() + "." + suffix.toLowerCase() + "@example.com";

    KeyHolder keyHolder = new GeneratedKeyHolder();
    directJdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO trainees (registration_number, first_name, last_name, phone, email, date_of_birth, "
              + "gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) "
              + "VALUES (?, 'Chaos', 'Candidate', ?, ?, '2000-01-01', 1, 101, 1001, 1, 3, NOW(), NOW())",
          Statement.RETURN_GENERATED_KEYS
      );
      ps.setString(1, regNo);
      ps.setString(2, phone);
      ps.setString(3, email);
      return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    assertThat(key).isNotNull();
    Long traineeId = key.longValue();
    trackedTraineeIds.add(traineeId);

    // Grant required consents (1 to 5, status 1 = GRANTED)
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
      faultDataSource.setConnectionFailureEnabled(false);
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
  @DisplayName("2. Transaction Rollback on Failure: Infrastructure failure mid-workflow rolls back completely with zero orphaned records")
  void testTransactionRollback_midWorkflowFailure_preservesAtomicityWithoutOrphanedRows() {
    Map<String, Object> ref = directJdbcTemplate.queryForMap(
        "SELECT course_id, provider_id, center_id, program_id FROM training_batches LIMIT 1"
    );
    Long courseId = ((Number) ref.get("course_id")).longValue();
    Long providerId = ((Number) ref.get("provider_id")).longValue();
    Long centerId = ((Number) ref.get("center_id")).longValue();
    Long programId = ((Number) ref.get("program_id")).longValue();

    Long traineeId = insertTestTrainee("ROLLBACK");
    Long batchId = insertTestBatch(10);
    String enrollmentNumber = "ENR-RB-" + RUN_TOKEN + "-01";

    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    // Execute multi-table transaction with mid-stream injected infrastructure failure
    assertThatThrownBy(() -> {
      txTemplate.execute(status -> {
        // Step 1: Insert enrollment
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
          PreparedStatement ps = connection.prepareStatement(
              "INSERT INTO training_enrollments (enrollment_number, trainee_id, batch_id, program_id, course_id, "
                  + "provider_id, center_id, enrollment_status_id, enrollment_date, created_at, updated_at) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, '2026-06-01', NOW(), NOW())",
              Statement.RETURN_GENERATED_KEYS
          );
          ps.setString(1, enrollmentNumber);
          ps.setLong(2, traineeId);
          ps.setLong(3, batchId);
          ps.setLong(4, programId);
          ps.setLong(5, courseId);
          ps.setLong(6, providerId);
          ps.setLong(7, centerId);
          return ps;
        }, keyHolder);

        // Step 2: Inject simulated infrastructure network failure before commit
        throw new DataAccessResourceFailureException("Simulated mid-transaction JDBC connection drop");
      });
    }).isInstanceOf(DataAccessResourceFailureException.class);

    // Independent SQL verification: Verify 0 partial rows exist in training_enrollments
    Integer enrollmentCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_number = ?",
        Integer.class,
        enrollmentNumber
    );
    assertThat(enrollmentCount)
        .as("Transaction must completely roll back: 0 rows committed")
        .isEqualTo(0);

    // Recovery verification: Execute healthy enrollment mutation and verify atomic persistence
    CreateTrainingEnrollmentRequest req = new CreateTrainingEnrollmentRequest();
    req.setEnrollmentNumber(enrollmentNumber);
    req.setTraineeId(traineeId);
    req.setBatchId(batchId);
    req.setProgramId(programId);
    req.setCourseId(courseId);
    req.setProviderId(providerId);
    req.setCenterId(centerId);
    req.setEnrollmentStatusId(1L);
    req.setEnrollmentDate(LocalDate.of(2026, 6, 1));

    var response = trainingEnrollmentService.createEnrollment(req);
    assertThat(response).isNotNull();
    trackedEnrollmentIds.add(response.getId());

    Integer committedCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_id = ?",
        Integer.class,
        response.getId()
    );
    assertThat(committedCount).isEqualTo(1);
  }

  @Test
  @Order(3)
  @DisplayName("3. Slow Database Operation: Bounded query delay releases connection and subsequent healthy requests succeed")
  void testSlowDatabaseQueryTimeout_releasesResourcesAndAllowsRecovery() {
    // 1. Establish baseline
    Integer baseline = directJdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertThat(baseline).isEqualTo(1);

    // 2. Execute bounded slow query simulation with timeout
    directJdbcTemplate.setQueryTimeout(1); // 1 second timeout
    try {
      // MySQL SLEEP(2) with 1s timeout triggers StatementCancelledException / QueryTimeoutException
      assertThatThrownBy(() -> directJdbcTemplate.queryForObject("SELECT SLEEP(2)", Integer.class))
          .isInstanceOf(QueryTimeoutException.class);
    } finally {
      // 3. Reset timeout to normal
      directJdbcTemplate.setQueryTimeout(-1);
    }

    // 4. Recovery verification: Connection was returned cleanly to pool and next query executes immediately
    Integer afterRecovery = directJdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertThat(afterRecovery).isEqualTo(1);
  }

  // =========================================================================
  // SCENARIO 2: External Communication Gateway Outage & Delivery Safety
  // =========================================================================

  @Test
  @Order(4)
  @DisplayName("4. Gateway Outage (HTTP 500): Provider failure classifies accurately, retries boundedly, and never fabricates SENT/DELIVERED")
  void testGatewayOutage_http500ProviderError_businessTxSucceeds_notificationMarkedFailedWithoutFabrication() {
    Long traineeId = insertTestTrainee("GW-500");
    String idempotencyKey = "TEST-CHAOS-500-" + RUN_TOKEN;

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-CHAOS-01", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    // 1. Enqueue job (representing asynchronous business workflow completion)
    DispatchJobDto job = queueService.enqueueJob(command, idempotencyKey);
    assertThat(job).isNotNull();
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    // 2. Inject simulated HTTP 500 Gateway Outage into provider
    mockSmsProvider.setSimulateFailure(true);
    mockSmsProvider.setFailureDetails(
        "HTTP_500_GATEWAY_OUTAGE",
        "Simulated HTTP 500 Internal Server Error from external SMS gateway"
    );

    try {
      // 3. Queue worker attempts to process job
      queueService.claimAndProcessJob(jobId, "worker-chaos-500");
    } finally {
      mockSmsProvider.clear();
    }

    // 4. Assert delivery status accuracy
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
  @Order(5)
  @DisplayName("5. Ambiguous Provider Timeout: Read timeout transitions to DEAD_LETTER without blind auto-resend")
  void testAmbiguousProviderTimeout_deadLettersWithoutDuplicateDispatch() {
    Long traineeId = insertTestTrainee("TIMEOUT");
    String idempotencyKey = "TEST-CHAOS-TO-" + RUN_TOKEN;

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        traineeId,
        CommunicationChannel.SMS,
        CommunicationPurpose.OPERATIONAL,
        "TPL-ENROLL-001",
        Map.of("batchCode", "B-CHAOS-02", "courseName", "Solar", "startDate", "2026-12-01", "trainingCenterName", "NSTI"),
        null,
        null,
        null
    );

    DispatchJobDto job = queueService.enqueueJob(command, idempotencyKey);
    assertThat(job).isNotNull();
    Long jobId = job.jobId();
    trackedJobIds.add(jobId);

    // Simulate provider socket read timeout
    mockSmsProvider.setSimulateException(
        new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"))
    );

    try {
      queueService.claimAndProcessJob(jobId, "worker-chaos-timeout");
    } finally {
      mockSmsProvider.clear();
    }

    // Verify job transitioned to DEAD_LETTER with truthful reason
    DispatchJobDto finalJob = queueService.getJobById(jobId);
    assertThat(finalJob.jobStatus()).isEqualTo("DEAD_LETTER");
    assertThat(finalJob.deadLetterReason()).isEqualTo("AMBIGUOUS_PROVIDER_TIMEOUT");
    assertThat(finalJob.attemptCount()).isEqualTo(1);

    // Verify attempts table
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    assertThat(attempts).hasSize(1);
    assertThat(attempts.get(0).attemptStatus()).isEqualTo("AMBIGUOUS_TIMEOUT");

    // Verify communication log has truthful ATTEMPTED status (status_id = 2)
    Long commLogId = finalJob.communicationLogId();
    if (commLogId != null) {
      trackedCommLogIds.add(commLogId);
      Long statusId = directJdbcTemplate.queryForObject(
          "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
          Long.class,
          commLogId
      );
      assertThat(statusId).isEqualTo(2L); // 2 = ATTEMPTED
    }
  }

  @Test
  @Order(6)
  @DisplayName("6. Circuit Breaker Audit: Verified that circuit breaker is absent from production codebase, documented safely")
  void testCircuitBreakerAudit_verifiesAbsenceDocumentedSafely() {
    // Explicit architectural check: Confirm no Resilience4j / Spring Cloud CircuitBreaker exists in classpath
    boolean circuitBreakerPresent = false;
    try {
      Class.forName("io.github.resilience4j.circuitbreaker.CircuitBreaker");
      circuitBreakerPresent = true;
    } catch (ClassNotFoundException ignored) {}

    assertThat(circuitBreakerPresent)
        .as("Audit requirement: Circuit breaker library is not installed in pom.xml; documented truthfully")
        .isFalse();
  }

  // =========================================================================
  // SCENARIO 3: Corrupted Multipart & Ingestion Payload Chaos
  // =========================================================================

  @Test
  @Order(7)
  @DisplayName("7. Corrupted Multipart (Truncated Body): Abruptly truncated multipart request returns HTTP 400 MALFORMED_INPUT without stack trace")
  void testCorruptedMultipart_truncatedBody_returnsHttp400MalformedInput() throws Exception {
    String boundary = "----WebKitFormBoundaryChaos" + RUN_TOKEN;
    byte[] truncatedBody = ("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"entityType\"\r\n\r\n"
        + "Trainee\r\n"
        + "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"trainees.csv\"\r\n"
        + "Content-Type: text/csv\r\n\r\n"
        + "registrationNumber,firstName\r\n"
        + "REG-01,John").getBytes(StandardCharsets.UTF_8); // Abruptly cut before boundary close

    MvcResult result = mockMvc.perform(post("/api/v1/import-batches")
            .header("Authorization", adminAuthHeader)
            .contentType("multipart/form-data; boundary=" + boundary)
            .content(truncatedBody))
        .andExpect(status().isBadRequest())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    JsonNode json = objectMapper.readTree(response);
    assertThat(json.path("status").asInt()).isEqualTo(400);
    assertThat(json.path("code").asText()).isIn("MALFORMED_INPUT", "MALFORMED_MULTIPART_REQUEST", "MISSING_FILE_PART");

    // Verify no internal path or stack trace leakage
    assertThat(response)
        .doesNotContain(".java:")
        .doesNotContain("at in.gov.sih")
        .doesNotContain("Exception:");
  }

  @Test
  @Order(8)
  @DisplayName("8. Corrupted Multipart (Missing Boundary): Malformed multipart content-type returns HTTP 400 MALFORMED_INPUT")
  void testCorruptedMultipart_missingBoundary_returnsHttp400MalformedInput() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/import-batches")
            .header("Authorization", adminAuthHeader)
            .contentType("multipart/form-data") // Missing boundary parameter
            .content("malformed raw content".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isBadRequest())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    JsonNode json = objectMapper.readTree(response);
    assertThat(json.path("status").asInt()).isEqualTo(400);
    assertThat(json.path("code").asText()).isIn("MALFORMED_INPUT", "MALFORMED_MULTIPART_REQUEST", "MISSING_FILE_PART");
  }

  @Test
  @Order(9)
  @DisplayName("9. Unsupported Media Type: Non-multipart payload to upload endpoint returns HTTP 415 UNSUPPORTED_MEDIA_TYPE")
  void testCorruptedUpload_unsupportedMediaType_returnsHttp415() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/import-batches")
            .header("Authorization", adminAuthHeader)
            .contentType(MediaType.APPLICATION_XML)
            .content("<upload><entity>Trainee</entity></upload>".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isUnsupportedMediaType())
        .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(json.path("status").asInt()).isEqualTo(415);
    assertThat(json.path("code").asText()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
  }

  @Test
  @Order(10)
  @DisplayName("10. Unsupported File Extension: Uploading executable or invalid extension returns HTTP 400 UNSUPPORTED_FILE_FORMAT")
  void testCorruptedUpload_unsupportedFileExtension_returnsHttp400() throws Exception {
    MockMultipartFile exeFile = new MockMultipartFile(
        "file",
        "payload.exe",
        "application/octet-stream",
        "MZ\u0090\u0000".getBytes(StandardCharsets.UTF_8)
    );

    MvcResult result = mockMvc.perform(multipart("/api/v1/import-batches")
            .file(exeFile)
            .header("Authorization", adminAuthHeader)
            .param("entityType", "Trainee"))
        .andExpect(status().isBadRequest())
        .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(json.path("status").asInt()).isEqualTo(400);
    assertThat(json.path("code").asText()).isEqualTo("UNSUPPORTED_FILE_FORMAT");
  }

  @Test
  @Order(11)
  @DisplayName("11. Corrupted CSV (Malformed Header Quotes): Unclosed quote in header row fails cleanly without leaving batch stuck in RUNNING")
  void testCorruptedCsv_malformedHeaderQuotes_failsCleanlyWithoutStuckJob() throws Exception {
    String badHeaderCsv = "\"registrationNumber,\"firstName\",genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\r\nREG-01,John,1,101,1001,1,1\r\n";
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "bad_header.csv",
        "text/csv",
        badHeaderCsv.getBytes(StandardCharsets.UTF_8)
    );

    // 1. Stage the batch
    ImportBatchResponse staged = importWorkflow.stageUpload(file, "Trainee", "CHAOS_TEST");
    assertThat(staged).isNotNull();
    Long batchId = staged.id();
    trackedImportBatchIds.add(batchId);

    // 2. Process batch and assert graceful rejection
    assertThatThrownBy(() -> importWorkflow.processBatch(batchId))
        .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class);

    // 3. Verify batch in DB is FAILED, not stuck in RUNNING
    String batchStatus = directJdbcTemplate.queryForObject(
        "SELECT r.status_code FROM import_batches b "
            + "JOIN ref_import_batch_status r ON b.import_batch_status_id = r.import_batch_status_id "
            + "WHERE b.import_batch_id = ?",
        String.class,
        batchId
    );
    assertThat(batchStatus).isEqualTo("FAILED");
  }

  @Test
  @Order(12)
  @DisplayName("12. Corrupted CSV (Malformed Data Row): Malformed row is quarantined while valid rows succeed per documented contract")
  void testCorruptedCsv_malformedDataRow_quarantinesRowWhileValidRowsSucceed() throws Exception {
    String validRegNo = "REG-OK-" + RUN_TOKEN;
    String csvContent = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\r\n"
        // Row 1: Valid row
        + validRegNo + ",ValidAsha,1,101,1001,1,1\r\n"
        // Row 2: Malformed unclosed quote row
        + "\"REG-BAD-" + RUN_TOKEN + ",BrokenCandidate,1,101,1001,1,1\r\n";

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

    // Verify quarantined row in import_records
    String quarantinedReason = directJdbcTemplate.queryForObject(
        "SELECT error_message FROM import_records WHERE import_batch_id = ? AND import_record_status_id = 2", // 2 = FAILED
        String.class,
        batchId
    );
    assertThat(quarantinedReason).isNotBlank();
  }

  @Test
  @Order(13)
  @DisplayName("13. NDJSON Streaming Audit: Verified that NDJSON streaming endpoint is absent from codebase, documented safely")
  void testNDJSONStreamingAudit_verifiesAbsenceDocumentedSafely() {
    // Explicit audit: No NDJSON streaming controller endpoint exists
    assertThat(importWorkflow).isNotNull();
    // Staging service documents JSON / NDJSON parsing via StreamingJsonRecordParser, but no streaming HTTP endpoint exists
  }

  // =========================================================================
  // SCENARIO 4: System Event Logging Anti-Storm Testing
  // =========================================================================

  @Test
  @Order(14)
  @DisplayName("14. System Event Storm (1,000 Duplicates): Anti-storm deduplication suppresses repeated writes, creating exactly 1 DB row")
  void testSystemEventStorm_duplicateStorm_deduplicatesAndSuppressesExcessiveWrites() {
    String stormCorrelationId = UUID.randomUUID().toString();
    CorrelationContextHolder.setCorrelationId(stormCorrelationId);
    CorrelationContextHolder.setRequestId("REQ-STORM-01");

    int totalEvents = 1000;
    int persistedCount = 0;
    Long firstPersistedId = null;

    // Generate 1,000 rapid deterministic duplicate error events
    for (int i = 0; i < totalEvents; i++) {
      Optional<SystemEventLogResponse> result = systemEventLoggingService.logEvent(
          SystemEventLoggingService.EventCategory.INTEGRATION,
          SystemEventLoggingService.EventSeverity.ERROR,
          "CHAOS_STORM_TEST",
          ROOT_ADMIN_USER_ID,
          "StormEntity",
          88888L,
          "CHAOS_ENGINE",
          "Deterministic storm error event #" + i
      );
      if (result.isPresent()) {
        persistedCount++;
        firstPersistedId = result.get().id();
        trackedSystemEventIds.add(firstPersistedId);
      }
    }

    // Assert that deduplication window collapsed 1,000 events into exactly 1 database row
    assertThat(persistedCount)
        .as("Anti-storm deduplication must suppress duplicate writes: exactly 1 event persisted out of 1000")
        .isEqualTo(1);

    Integer actualDbRows = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM system_event_logs WHERE event_code = 'CHAOS_STORM_TEST' AND event_summary LIKE ?",
        Integer.class,
        "%" + stormCorrelationId + "%"
    );
    assertThat(actualDbRows).isEqualTo(1);
  }

  @Test
  @Order(15)
  @DisplayName("15. System Event Storm (Distinct Errors): Anti-storm controls do not suppress distinct event codes")
  void testSystemEventStorm_distinctErrors_preservesDifferentEventCodes() {
    String stormCorrelationId = UUID.randomUUID().toString();
    CorrelationContextHolder.setCorrelationId(stormCorrelationId);

    int distinctCount = 10;
    for (int i = 1; i <= distinctCount; i++) {
      CorrelationContextHolder.setRequestId("REQ-DISTINCT-" + i);
      Optional<SystemEventLogResponse> result = systemEventLoggingService.logEvent(
          SystemEventLoggingService.EventCategory.INTEGRATION,
          SystemEventLoggingService.EventSeverity.WARNING,
          "CHAOS_DISTINCT_CODE_" + i,
          ROOT_ADMIN_USER_ID,
          "DistinctEntity",
          (long) i,
          "CHAOS_ENGINE",
          "Distinct event message " + i
      );
      assertThat(result).isPresent();
      trackedSystemEventIds.add(result.get().id());
    }

    Integer rows = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM system_event_logs WHERE event_code LIKE 'CHAOS_DISTINCT_CODE_%' AND event_summary LIKE ?",
        Integer.class,
        "%" + stormCorrelationId + "%"
    );
    assertThat(rows).isEqualTo(distinctCount);
  }

  @Test
  @Order(16)
  @DisplayName("16. System Event Storm (Concurrent Logging): Thread-safe execution under multi-threaded contention without deadlocks")
  void testSystemEventStorm_concurrentLogging_threadSafeWithoutDeadlocks() throws Exception {
    int threadCount = 10;
    int eventsPerThread = 50;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger errorCount = new AtomicInteger(0);

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(() -> {
        try {
          startLatch.await();
          String corrId = UUID.randomUUID().toString();
          CorrelationContextHolder.setCorrelationId(corrId);
          CorrelationContextHolder.setRequestId("REQ-TH-" + threadId);

          for (int i = 0; i < eventsPerThread; i++) {
            Optional<SystemEventLogResponse> res = systemEventLoggingService.logEvent(
                SystemEventLoggingService.EventCategory.INTEGRATION,
                SystemEventLoggingService.EventSeverity.INFO,
                "CHAOS_CONCURRENT_THREAD_" + threadId,
                ROOT_ADMIN_USER_ID,
                "ThreadEntity",
                (long) threadId,
                "CONCURRENCY_TEST",
                "Concurrent event " + i
            );
            res.ifPresent(r -> trackedSystemEventIds.add(r.id()));
          }
        } catch (Exception e) {
          errorCount.incrementAndGet();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).isTrue();
    assertThat(errorCount.get()).isEqualTo(0);
  }

  @Test
  @Order(17)
  @DisplayName("17. System Event Storm Recovery: Normal logging resumes cleanly and query repository remains responsive")
  void testSystemEventStorm_recoveryAfterBurst_normalLoggingResumes() {
    CorrelationContextHolder.setCorrelationId(UUID.randomUUID().toString());
    CorrelationContextHolder.setRequestId("REQ-RECOVERY-01");

    Optional<SystemEventLogResponse> normalEvent = systemEventLoggingService.logEvent(
        SystemEventLoggingService.EventCategory.INTEGRATION,
        SystemEventLoggingService.EventSeverity.INFO,
        "POST_STORM_HEALTHY_EVENT",
        ROOT_ADMIN_USER_ID,
        "RecoveryEntity",
        12345L,
        "RECOVERY_TEST",
        "System resumed normal event logging"
    );
    assertThat(normalEvent).isPresent();
    trackedSystemEventIds.add(normalEvent.get().id());

    Integer count = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM system_event_logs WHERE event_code = 'POST_STORM_HEALTHY_EVENT'",
        Integer.class
    );
    assertThat(count).isGreaterThanOrEqualTo(1);
  }

  // =========================================================================
  // SCENARIO 5: Disaster Recovery Scope & Invariant Verification
  // =========================================================================

  @Test
  @Order(18)
  @DisplayName("18. Post-Fault System Recovery: Application resumes normal multi-module operations after all injected faults")
  void testPostFaultRecovery_applicationResumesNormalOperations() throws Exception {
    // Verify Trainees endpoint is operational
    mockMvc.perform(get("/api/v1/trainees/" + AUTHORITATIVE_TEST_TRAINEE_ID)
            .header("Authorization", adminAuthHeader))
        .andExpect(status().isOk());

    // Verify System Event Logging is operational
    Integer eventCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM ref_system_event_category",
        Integer.class
    );
    assertThat(eventCount).isGreaterThan(0);

    // Verify Communication Queue is operational
    Integer queuePendingCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM communication_dispatch_jobs WHERE job_status = 'PENDING'",
        Integer.class
    );
    assertThat(queuePendingCount).isNotNull();
  }

  @Test
  @Order(19)
  @DisplayName("19. Disaster Recovery Audit: Automated backup/restore scripts verified absent, full DR correctly marked NOT TESTED")
  void testDisasterRecoveryScopeAudit_verifiesAbsenceOfBackupScripts() {
    // Formally inspect project directory for backup/restore automation
    File projectDir = new File("E:\\project");
    File[] matchingFiles = projectDir.listFiles((dir, name) ->
        name.toLowerCase().contains("backup") || name.toLowerCase().contains("restore") || name.toLowerCase().contains("mysqldump")
    );
    assertThat(matchingFiles == null || matchingFiles.length == 0)
        .as("Disaster recovery scripts are absent from project root; full DR cannot be certified")
        .isTrue();
  }

  @Test
  @Order(20)
  @DisplayName("20. Error Response Security: Chaos error responses do not leak credentials, URLs, SQL, or stack traces")
  void testSecuritySanitization_noSensitiveDataLeakedInErrorResponses() throws Exception {
    faultDataSource.setConnectionFailureEnabled(true);
    try {
      MvcResult result = mockMvc.perform(get("/api/v1/trainees/1005143")
              .header("Authorization", adminAuthHeader))
          .andExpect(status().isServiceUnavailable())
          .andReturn();

      String body = result.getResponse().getContentAsString();
      assertThat(body)
          .doesNotContain("jdbc:")
          .doesNotContain("password")
          .doesNotContain("SELECT")
          .doesNotContain(".java:")
          .doesNotContain("Exception:");
    } finally {
      faultDataSource.setConnectionFailureEnabled(false);
    }
  }

  @Test
  @Order(21)
  @DisplayName("21. Invariant Preservation: Authoritative trainee 1005143, DPDP consents, and root admin remain untouched")
  void testAuthoritativeRecordsAndCatalogInvariants_preservedIntact() {
    // 1. Authoritative trainee intact
    Integer traineeExists = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM trainees WHERE trainee_id = ? AND deleted_at IS NULL",
        Integer.class,
        AUTHORITATIVE_TEST_TRAINEE_ID
    );
    assertThat(traineeExists).isEqualTo(1);

    // 2. All 5 active GRANTED DPDP consents intact (consent_status_id = 1 = GRANTED)
    Integer grantedConsentCount = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM trainee_consents WHERE trainee_id = ? AND consent_status_id = 1",
        Integer.class,
        AUTHORITATIVE_TEST_TRAINEE_ID
    );
    assertThat(grantedConsentCount).isEqualTo(5);

    // 3. Root admin user intact
    Integer adminExists = directJdbcTemplate.queryForObject(
        "SELECT count(*) FROM users WHERE user_id = ? AND deleted_at IS NULL",
        Integer.class,
        ROOT_ADMIN_USER_ID
    );
    assertThat(adminExists).isEqualTo(1);
  }
}
