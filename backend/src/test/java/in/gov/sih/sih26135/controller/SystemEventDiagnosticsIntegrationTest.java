package in.gov.sih.sih26135.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.security.config.SecurityLockoutProperties;
import in.gov.sih.sih26135.config.SystemEventLoggingProperties;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.dto.response.ImportBatchProgressResponse;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.DataQualityEngineService;
import in.gov.sih.sih26135.service.ImportBatchProcessingWorkflow;
import in.gov.sih.sih26135.service.SystemEventLoggingService;
import in.gov.sih.sih26135.service.SystemEventLoggingService.EventCategory;
import in.gov.sih.sih26135.service.SystemEventLoggingService.EventSeverity;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.export.ExportRateLimiter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.6 - System Event Diagnostics, Operational Alerting & Correlation Tracing Integration Tests")
class SystemEventDiagnosticsIntegrationTest {

  private static final long STATE_A_ID = 9921L;
  private static final long STATE_B_ID = 9922L;
  private static final long DISTRICT_A1_ID = 9921L;
  private static final long DISTRICT_B1_ID = 9922L;
  private static final long DISTRICT_A2_ID = 9923L;

  private static final long SECTOR_ID = 9921L;
  private static final long COURSE_ID = 9921L;
  private static final long ORG_ID = 9921L;
  private static final long DEPT_ID = 9921L;
  private static final long SCHEME_ID = 9921L;
  private static final long PROGRAM_ID = 9921L;

  private static final long PROVIDER_A_ID = 9921L;
  private static final long PROVIDER_B_ID = 9922L;
  private static final long EMPLOYER_A_ID = 9921L;
  private static final long CENTER_A_ID = 9921L;
  private static final long BATCH_A_ID = 9921L;

  private static final long TRAINEE_1_ID = 9921L;
  private static final long TRAINEE_2_ID = 9922L;
  private static final long TRAINEE_3_ID = 9923L;
  private static final long ENROLLMENT_1_ID = 9921L;
  private static final long ENROLLMENT_2_ID = 9922L;
  private static final long ENROLLMENT_3_ID = 9923L;

  private static final long USER_SUPER_ADMIN_ID = 9920L;
  private static final long USER_STATE_A_ID = 9921L;
  private static final long USER_DIST_A1_ID = 9922L;
  private static final long USER_STATE_B_ID = 9923L;
  private static final long USER_PROVIDER_A_ID = 9924L;
  private static final long USER_PROVIDER_B_ID = 9925L;
  private static final long USER_EMPLOYER_A_ID = 9926L;
  private static final long USER_TRAINEE_ID = 9927L;
  private static final long USER_LOGIN_VICTIM_ID = 9928L;
  private static final long USER_UNSCOPED_STATE_ID = 9929L;

  private static final String VICTIM_USERNAME = "diag286_victim";
  private static final String VICTIM_PASSWORD = "CorrectPassword#286";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private SystemEventLoggingService systemEventLoggingService;

  @Autowired
  private SystemEventLoggingProperties eventLoggingProperties;

  @Autowired
  private DataQualityEngineProperties dataQualityProperties;

  @Autowired
  private SecurityLockoutProperties securityLockoutProperties;

  @Autowired
  private ExportRateLimiter exportRateLimiter;

  @Autowired
  private ImportBatchProcessingWorkflow importBatchWorkflow;

  @Autowired
  private DataQualityEngineService dataQualityEngineService;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  @Qualifier("auditTaskExecutor")
  private Executor auditTaskExecutor;

  @Autowired
  private CorrelationIdFilter correlationIdFilter;

  private String tokenSuperAdmin;
  private String tokenReadOnlyAdmin;
  private String tokenStateA;
  private String tokenDistrictA1;
  private String tokenStateB;
  private String tokenProviderA;
  private String tokenProviderB;
  private String tokenEmployerA;
  private String tokenTrainee;
  private String tokenUnscopedState;

  private int origSpikeNewIssuesThreshold;
  private int origSpikeHighSeverityThreshold;
  private int origSpikeCriticalSeverityThreshold;
  private int origSpikeAlertCooldownSeconds;
  private int origRateLimitCooldownSeconds;
  private int origDedupWindowSeconds;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    origSpikeNewIssuesThreshold = dataQualityProperties.getSpikeNewIssuesThreshold();
    origSpikeHighSeverityThreshold = dataQualityProperties.getSpikeHighSeverityThreshold();
    origSpikeCriticalSeverityThreshold = dataQualityProperties.getSpikeCriticalSeverityThreshold();
    origSpikeAlertCooldownSeconds = dataQualityProperties.getSpikeAlertCooldownSeconds();
    origRateLimitCooldownSeconds = eventLoggingProperties.getRateLimitAlertCooldownSeconds();
    origDedupWindowSeconds = eventLoggingProperties.getDedupWindowSeconds();

    SecurityContextHolder.clearContext();
    CorrelationContextHolder.clear();
    MDC.clear();
    exportRateLimiter.reset();
    systemEventLoggingService.resetThrottleState();

    cleanUpFixtures();
    seedFixtures();
    catalogService.ensureDefaultRulesProvisioned();

    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "diag286_super",
        List.of("ROLE_super_admin", "system.manage", "audit.read", "analytics.read"));
    tokenReadOnlyAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ID, "diag286_state_a",
        List.of("ROLE_state_admin", "audit.read"));
    tokenStateA = tokenReadOnlyAdmin;
    tokenDistrictA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_DIST_A1_ID, "diag286_dist_a1",
        List.of("ROLE_district_officer", "audit.read"));
    tokenStateB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ID, "diag286_state_b",
        List.of("ROLE_state_admin", "audit.read"));
    tokenProviderA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A_ID, "diag286_prov_a",
        List.of("ROLE_training_provider", "audit.read", "trainee.write"));
    tokenProviderB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_B_ID, "diag286_prov_b",
        List.of("ROLE_training_provider", "audit.read"));
    tokenEmployerA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_A_ID, "diag286_emp_a",
        List.of("ROLE_employer", "audit.read"));
    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_ID, "diag286_trainee",
        List.of("ROLE_trainee", "audit.read"));
    tokenUnscopedState = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_UNSCOPED_STATE_ID, "diag286_unscoped",
        List.of("ROLE_state_admin", "audit.read"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    CorrelationContextHolder.clear();
    MDC.clear();
    exportRateLimiter.reset();
    systemEventLoggingService.resetThrottleState();

    dataQualityProperties.setSpikeNewIssuesThreshold(origSpikeNewIssuesThreshold);
    dataQualityProperties.setSpikeHighSeverityThreshold(origSpikeHighSeverityThreshold);
    dataQualityProperties.setSpikeCriticalSeverityThreshold(origSpikeCriticalSeverityThreshold);
    dataQualityProperties.setSpikeAlertCooldownSeconds(origSpikeAlertCooldownSeconds);
    eventLoggingProperties.setRateLimitAlertCooldownSeconds(origRateLimitCooldownSeconds);
    eventLoggingProperties.setDedupWindowSeconds(origDedupWindowSeconds);

    cleanUpFixtures();
  }

  private void seedFixtures() {
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'D1', 'Diag State A', 'LG9921', 1, NOW(), NOW())",
        STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'D2', 'Diag State B', 'LG9922', 1, NOW(), NOW())",
        STATE_B_ID);

    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DG1', 'Diag District A1', 'LGD9921', 1, NOW(), NOW())",
        DISTRICT_A1_ID, STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DG2', 'Diag District B1', 'LGD9922', 1, NOW(), NOW())",
        DISTRICT_B1_ID, STATE_B_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DG3', 'Diag District A2', 'LGD9923', 1, NOW(), NOW())",
        DISTRICT_A2_ID, STATE_A_ID);

    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9921', 'Diag Provider A', 'REG-PRV-9921', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-9922', 'Diag Provider B', 'REG-PRV-9922', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_B_ID, STATE_B_ID, DISTRICT_B1_ID);
    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-9921', 'Diag Employer A', 'REG-EMP-9921', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-9921', 'Diag Sector', 1, NOW(), NOW())",
        SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-9921', 'Diag Course', ?, 1, 1, NOW(), NOW(), NULL)",
        COURSE_ID, SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-9921', 'Diag Org', 1, 1, NOW(), NOW())",
        ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-9921', 'Diag Dept', 1, NOW(), NOW())",
        DEPT_ID, ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-9921', 'Diag Scheme', ?, 'INR', 1, NOW(), NOW())",
        SCHEME_ID, DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-9921', 'Diag Program', ?, ?, 1, NOW(), NOW())",
        PROGRAM_ID, DEPT_ID, SCHEME_ID);
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-9921', 'Diag Center A', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A_ID, PROVIDER_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-9921', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A_ID, COURSE_ID, PROVIDER_A_ID, CENTER_A_ID, PROGRAM_ID);

    insertUser(USER_SUPER_ADMIN_ID, "diag286_super", "diag9920@test.gov.in", 1L, null, null, null, null, "super_admin");
    insertUser(USER_STATE_A_ID, "diag286_state_a", "diag9921@test.gov.in", 1L, STATE_A_ID, null, null, null, "state_admin");
    insertUser(USER_DIST_A1_ID, "diag286_dist_a1", "diag9922@test.gov.in", 1L, STATE_A_ID, DISTRICT_A1_ID, null, null, "district_officer");
    insertUser(USER_STATE_B_ID, "diag286_state_b", "diag9923@test.gov.in", 1L, STATE_B_ID, null, null, null, "state_admin");
    insertUser(USER_PROVIDER_A_ID, "diag286_prov_a", "diag9924@test.gov.in", 1L, null, null, PROVIDER_A_ID, null, "training_provider");
    insertUser(USER_PROVIDER_B_ID, "diag286_prov_b", "diag9925@test.gov.in", 1L, null, null, PROVIDER_B_ID, null, "training_provider");
    insertUser(USER_EMPLOYER_A_ID, "diag286_emp_a", "diag9926@test.gov.in", 1L, null, null, null, EMPLOYER_A_ID, "employer");
    insertUser(USER_TRAINEE_ID, "diag286_trainee", "diag9927@test.gov.in", 2L, STATE_A_ID, DISTRICT_A1_ID, null, null, "trainee");

    String victimHash = passwordEncoder.encode(VICTIM_PASSWORD);
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, employer_id, failed_login_count, created_at, updated_at) "
            + "VALUES (?, ?, 'diag9928@test.gov.in', ?, 'bcrypt', 2, ?, ?, ?, 0, NOW(), NOW())",
        USER_LOGIN_VICTIM_ID, VICTIM_USERNAME, victimHash, STATE_A_ID, DISTRICT_A1_ID, EMPLOYER_A_ID);
    jdbcTemplate.update(
        "INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'employer' LIMIT 1",
        USER_LOGIN_VICTIM_ID);

    insertUser(USER_UNSCOPED_STATE_ID, "diag286_unscoped", "diag9929@test.gov.in", 1L, null, null, null, null, "state_admin");
  }

  private void insertUser(
      Long userId,
      String username,
      String email,
      Long statusId,
      Long stateId,
      Long districtId,
      Long providerId,
      Long employerId,
      String roleCode) {
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, employer_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'hash', 'argon2id', ?, ?, ?, ?, ?, NOW(), NOW())",
        userId, username, email, statusId, stateId, districtId, providerId, employerId);
    if (roleCode != null) {
      jdbcTemplate.update(
          "INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT ?, role_id, NOW() FROM roles WHERE role_code = ? LIMIT 1",
          userId, roleCode);
    }
  }

  private void cleanUpFixtures() {
    jdbcTemplate.update(
        "DELETE FROM audit_logs WHERE actor_user_id BETWEEN 9920 AND 9929 "
            + "OR (entity_type = 'User' AND entity_id BETWEEN 9920 AND 9929)");
    jdbcTemplate.update(
        "DELETE FROM system_event_logs WHERE actor_user_id BETWEEN 9920 AND 9929 "
            + "OR entity_id BETWEEN 9920 AND 9929 "
            + "OR event_summary LIKE '%DIAG286%' "
            + "OR (entity_type = 'ImportBatch' AND entity_id IN (SELECT import_batch_id FROM import_batches WHERE initiated_by_user_id BETWEEN 9920 AND 9929))");

    jdbcTemplate.update(
        "DELETE FROM data_quality_issue_events WHERE data_quality_issue_id IN "
            + "(SELECT data_quality_issue_id FROM data_quality_issues WHERE entity_type = 'Trainee' AND entity_id BETWEEN 9921 AND 9929)");
    jdbcTemplate.update(
        "DELETE FROM data_quality_issues WHERE entity_type = 'Trainee' AND entity_id BETWEEN 9921 AND 9929");

    jdbcTemplate.update(
        "DELETE FROM import_records WHERE import_batch_id IN (SELECT import_batch_id FROM import_batches WHERE initiated_by_user_id BETWEEN 9920 AND 9929)");
    jdbcTemplate.update("DELETE FROM import_batches WHERE initiated_by_user_id BETWEEN 9920 AND 9929");

    jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id BETWEEN 9921 AND 9929 OR trainee_id BETWEEN 9921 AND 9929");
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id BETWEEN 9921 AND 9929 OR registration_number LIKE 'DIAG286-%'");
    jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", BATCH_A_ID);
    jdbcTemplate.update("DELETE FROM training_centers WHERE center_id = ?", CENTER_A_ID);
    jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", COURSE_ID);
    jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", PROGRAM_ID);
    jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", SCHEME_ID);
    jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", DEPT_ID);
    jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", ORG_ID);
    jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SECTOR_ID);

    jdbcTemplate.update("DELETE FROM user_roles WHERE user_id BETWEEN 9920 AND 9929");
    jdbcTemplate.update("DELETE FROM users WHERE user_id BETWEEN 9920 AND 9929");
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)", PROVIDER_A_ID, PROVIDER_B_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", EMPLOYER_A_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)", DISTRICT_A1_ID, DISTRICT_B1_ID, DISTRICT_A2_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)", STATE_A_ID, STATE_B_ID);
  }

  private void setSecurityContext(Long userId, String username, List<String> authorities) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, username),
            null,
            authorities.stream().map(SimpleGrantedAuthority::new).toList()));
  }

  @Nested
  @DisplayName("1. Correlation & Request ID HTTP and Async Propagation")
  class CorrelationPropagationTests {

    @Test
    @DisplayName("HTTP preserves valid X-Correlation-Id, replaces malformed headers, and cleans MDC afterward")
    void httpCorrelationValidationAndCleanup() throws Exception {
      String validCorrId = "diag-286-corr-trace-001";
      mockMvc.perform(get("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, validCorrId)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, validCorrId))
          .andExpect(header().string(CorrelationIdFilter.REQUEST_ID_HEADER, validCorrId));

      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(CorrelationContextHolder.getExplicitRequestId()).isNull();

      String malformed = "bad\ncrlf!@#value";
      MvcResult malformedResult = mockMvc.perform(get("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, malformed)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();

      String generatedCorr = malformedResult.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
      assertThat(generatedCorr).isNotBlank();
      assertThat(generatedCorr).isNotEqualTo(malformed);
      assertThat(CorrelationIdFilter.isValidCorrelationId(generatedCorr)).isTrue();
    }

    @Test
    @DisplayName("Async auditTaskExecutor propagates parent correlationId, mints distinct requestId, and leaves worker MDC clean")
    void asyncTaskDecoratorPropagatesAndCleansUp() throws Exception {
      String parentCorr = "diag-286-parent-corr";
      String parentReq = "diag-286-parent-req";
      CorrelationContextHolder.setCorrelationId(parentCorr);
      CorrelationContextHolder.setRequestId(parentReq);
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, parentCorr);
      MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, parentReq);

      AtomicReference<String> childCorr = new AtomicReference<>();
      AtomicReference<String> childReq = new AtomicReference<>();
      AtomicReference<String> childMdcCorr = new AtomicReference<>();
      AtomicReference<String> childMdcReq = new AtomicReference<>();

      CompletableFuture.runAsync(() -> {
        childCorr.set(CorrelationContextHolder.getCorrelationId());
        childReq.set(CorrelationContextHolder.getExplicitRequestId());
        childMdcCorr.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
        childMdcReq.set(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY));
      }, auditTaskExecutor).get(5, TimeUnit.SECONDS);

      assertThat(childCorr.get()).isEqualTo(parentCorr);
      assertThat(childMdcCorr.get()).isEqualTo(parentCorr);
      assertThat(childReq.get()).isNotBlank().isNotEqualTo(parentReq);
      assertThat(childMdcReq.get()).isEqualTo(childReq.get());

      // Next unrelated task without parent correlation context receives fresh server correlationId and sees zero leftover state
      CorrelationContextHolder.clear();
      MDC.clear();

      AtomicReference<String> unlinkedCorr = new AtomicReference<>();
      AtomicReference<String> unlinkedReq = new AtomicReference<>();
      CompletableFuture.runAsync(() -> {
        unlinkedCorr.set(CorrelationContextHolder.getCorrelationId());
        unlinkedReq.set(CorrelationContextHolder.getExplicitRequestId());
      }, auditTaskExecutor).get(5, TimeUnit.SECONDS);

      assertThat(unlinkedCorr.get()).isNotBlank().isNotEqualTo(parentCorr);
      assertThat(unlinkedReq.get()).isNotBlank().isNotEqualTo(parentReq);
      assertThat(CorrelationIdFilter.isValidCorrelationId(unlinkedCorr.get())).isTrue();
      assertThat(CorrelationIdFilter.isValidCorrelationId(unlinkedReq.get())).isTrue();
    }

    @Test
    @DisplayName("Preserves distinct valid X-Correlation-Id (trace-123) and X-Request-Id (request-456) across headers, context, MDC, persisted events, and replaces invalid/oversized IDs")
    void preservesDistinctHttpIdsAndHandlesMissingInvalidOversized() throws Exception {
      // 1. Live filter thread-context & MDC verification with distinct valid incoming IDs
      MockHttpServletRequest filterReq = new MockHttpServletRequest("GET", "/api/v1/system-event-logs");
      filterReq.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123");
      filterReq.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "request-456");
      MockHttpServletResponse filterResp = new MockHttpServletResponse();

      AtomicReference<String> inChainHolderCorr = new AtomicReference<>();
      AtomicReference<String> inChainHolderReq = new AtomicReference<>();
      AtomicReference<String> inChainMdcCorr = new AtomicReference<>();
      AtomicReference<String> inChainMdcReq = new AtomicReference<>();

      MockFilterChain chain = new MockFilterChain() {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
          inChainHolderCorr.set(CorrelationContextHolder.getCorrelationId());
          inChainHolderReq.set(CorrelationContextHolder.getExplicitRequestId());
          inChainMdcCorr.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
          inChainMdcReq.set(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY));
        }
      };

      correlationIdFilter.doFilter(filterReq, filterResp, chain);

      assertThat(inChainHolderCorr.get()).isEqualTo("trace-123");
      assertThat(inChainHolderReq.get()).isEqualTo("request-456");
      assertThat(inChainMdcCorr.get()).isEqualTo("trace-123");
      assertThat(inChainMdcReq.get()).isEqualTo("request-456");
      assertThat(filterResp.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("trace-123");
      assertThat(filterResp.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo("request-456");

      // Context cleanup verified in finally
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(CorrelationContextHolder.getExplicitRequestId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();

      // 2. End-to-end HTTP persistence + diagnostic search with distinct X-Correlation-Id & X-Request-Id
      CreateSystemEventLogRequest postReq = new CreateSystemEventLogRequest(
          4L, 1L, "DIAG286_DISTINCT_IDS", "DIAG286 distinct correlation and request trace", null);
      MvcResult createRes = mockMvc.perform(post("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123")
              .header(CorrelationIdFilter.REQUEST_ID_HEADER, "request-456")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(postReq)))
          .andExpect(status().isCreated())
          .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123"))
          .andExpect(header().string(CorrelationIdFilter.REQUEST_ID_HEADER, "request-456"))
          .andExpect(jsonPath("$.correlationId").value("trace-123"))
          .andReturn();

      JsonNode createdNode = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data");
      assertThat(createdNode.path("eventSummary").asText()).startsWith("[cid=trace-123|rid=request-456] ");

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", "trace-123")
              .param("requestId", "request-456")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].eventCode").value("DIAG286_DISTINCT_IDS"));

      jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id = ?", createdNode.path("id").asLong());

      // 3. Valid X-Correlation-Id + invalid/oversized X-Request-Id -> preserves trace-123, replaces bad requestId
      MvcResult badReqIdRes = mockMvc.perform(get("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123")
              .header(CorrelationIdFilter.REQUEST_ID_HEADER, "bad\r\nreq!")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace-123"))
          .andReturn();
      String replacedReq = badReqIdRes.getResponse().getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);
      assertThat(replacedReq).isNotBlank().isNotEqualTo("trace-123").doesNotContain("\r", "\n", "!");
      assertThat(UUID.fromString(replacedReq)).isNotNull();

      // 4. Oversized X-Correlation-Id (> 64 chars) + missing X-Request-Id -> replaces both with safe UUID
      String oversized = "x".repeat(65);
      MvcResult oversizedRes = mockMvc.perform(get("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, oversized)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andReturn();
      String genCorr = oversizedRes.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
      String genReq = oversizedRes.getResponse().getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);
      assertThat(genCorr).isNotEqualTo(oversized);
      assertThat(genReq).isEqualTo(genCorr);
      assertThat(UUID.fromString(genCorr)).isNotNull();
    }
  }

  @Nested
  @DisplayName("2. Operational Trigger: Account Lockout")
  class AccountLockoutEventTests {

    @Test
    @DisplayName("Login failures up to threshold emit ACCOUNT_LOCKED once with correlationId and zero credential leakage")
    void accountLockoutEmitsStructuredCorrelatedEventOnce() throws Exception {
      int maxAttempts = securityLockoutProperties.getMaxFailedAttempts();
      assertThat(maxAttempts).isGreaterThanOrEqualTo(2);

      String corrId = "diag-286-lockout-corr";
      String wrongSecret = "SuperSecretWrongPass#999";

      for (int attempt = 1; attempt <= maxAttempts + 1; attempt++) {
        String body = objectMapper.writeValueAsString(Map.of(
            "usernameOrEmail", VICTIM_USERNAME,
            "password", wrongSecret
        ));
        mockMvc.perform(post("/api/v1/auth/login")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId));
      }

      Integer blockedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE event_code = 'LOGIN_BLOCKED_LOCKED_ACCOUNT' AND actor_user_id = ?",
          Integer.class, USER_LOGIN_VICTIM_ID);
      assertThat(blockedCount).isEqualTo(1);

      Integer lockedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE event_code = 'ACCOUNT_LOCKED' AND actor_user_id = ?",
          Integer.class, USER_LOGIN_VICTIM_ID);
      assertThat(lockedCount).isEqualTo(1);

      Map<String, Object> lockedRow = jdbcTemplate.queryForMap(
          "SELECT s.system_event_category_id, s.system_event_severity_id, s.entity_type, s.entity_id, s.source_component, s.event_summary "
              + "FROM system_event_logs s WHERE s.event_code = 'ACCOUNT_LOCKED' AND s.actor_user_id = ?",
          USER_LOGIN_VICTIM_ID);
      assertThat(((Number) lockedRow.get("system_event_category_id")).longValue()).isEqualTo(1L); // SECURITY
      assertThat(((Number) lockedRow.get("system_event_severity_id")).longValue()).isEqualTo(3L); // ERROR
      assertThat(lockedRow.get("entity_type")).isEqualTo("USER");
      assertThat(((Number) lockedRow.get("entity_id")).longValue()).isEqualTo(USER_LOGIN_VICTIM_ID);
      String summary = String.valueOf(lockedRow.get("event_summary"));
      assertThat(summary).contains("[cid=" + corrId + "|rid=");
      assertThat(summary).doesNotContain(wrongSecret);
      assertThat(summary).doesNotContain("$2a$");
    }
  }

  @Nested
  @DisplayName("3. Operational Trigger: Rate-Limit Violation")
  class RateLimitEventTests {

    @Test
    @DisplayName("HTTP 429 rate-limit rejection preserves contract and logs throttled ACCESS/WARNING event")
    void exportRateLimitLogsThrottledAccessWarningEvent() throws Exception {
      for (int i = 0; i < 5; i++) {
        exportRateLimiter.checkAndAcquire(USER_SUPER_ADMIN_ID);
        exportRateLimiter.release(USER_SUPER_ADMIN_ID);
      }

      String corrId = "diag-286-ratelimit-corr";
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", String.valueOf(DISTRICT_A1_ID))
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isTooManyRequests())
          .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
          .andExpect(jsonPath("$.code").value("EXPORT_RATE_LIMIT_EXCEEDED"));

      // Burst of subsequent rate-limited calls still gets 429 but cooldown prevents log amplification
      mockMvc.perform(get("/api/v1/analytics/export/dsdp/json")
              .param("districtId", String.valueOf(DISTRICT_A1_ID))
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isTooManyRequests());

      Integer rlCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE event_code = 'EXPORT_RATE_LIMIT_EXCEEDED' AND actor_user_id = ?",
          Integer.class, USER_SUPER_ADMIN_ID);
      assertThat(rlCount).isEqualTo(1);

      Map<String, Object> rlRow = jdbcTemplate.queryForMap(
          "SELECT system_event_category_id, system_event_severity_id, event_summary "
              + "FROM system_event_logs WHERE event_code = 'EXPORT_RATE_LIMIT_EXCEEDED' AND actor_user_id = ?",
          USER_SUPER_ADMIN_ID);
      assertThat(((Number) rlRow.get("system_event_category_id")).longValue()).isEqualTo(5L); // ACCESS
      assertThat(((Number) rlRow.get("system_event_severity_id")).longValue()).isEqualTo(2L); // WARNING
      assertThat(String.valueOf(rlRow.get("event_summary")))
          .contains("[cid=" + corrId + "|rid=")
          .contains("/api/v1/analytics/export/dsdp/json")
          .doesNotContain("districtId=");
    }
  }

  @Nested
  @DisplayName("4. Operational Trigger: Phase 28.5 Failed Batch Job & Background Correlation")
  class BatchFailureEventTests {

    @Test
    @DisplayName("Completed batch with quarantined rows emits zero failure events; FAILED batch persists BATCH_JOB/ERROR independently with server correlation")
    void batchFailureEmitsBatchJobErrorWhilePartialQuarantineDoesNot() {
      setSecurityContext(USER_PROVIDER_A_ID, "diag286_prov_a",
          List.of("ROLE_training_provider", "trainee.write"));

      // 1. Valid header + 1 valid trainee + 1 invalid row -> COMPLETED with row quarantine
      String validAndQuarantinedCsv =
          "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
              + "DIAG286-OK-1,Ananya,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n"
              + "DIAG286-BAD-2,,999," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n";
      ImportBatchResponse okBatch = importBatchWorkflow.stageUpload(
          new MockMultipartFile("file", "diag286_partial.csv", "text/csv",
              validAndQuarantinedCsv.getBytes(StandardCharsets.UTF_8)),
          "Trainee", "DIAG286_SRC");

      ImportBatchProgressResponse okProgress = importBatchWorkflow.processBatch(okBatch.id());
      assertThat(okProgress.status()).isEqualTo("COMPLETED");
      assertThat(okProgress.failureCount()).isEqualTo(1);

      Integer partialFailEventCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE entity_type = 'ImportBatch' AND entity_id = ?",
          Integer.class, okBatch.id());
      assertThat(partialFailEventCount).isZero();

      // 2. Malformed JSON staged -> executed without active HTTP correlation context -> server generates & clears correlationId
      ImportBatchResponse badBatch = importBatchWorkflow.stageUpload(
          new MockMultipartFile("file", "diag286_corrupt.json", "application/json",
              "[{malformed_json".getBytes(StandardCharsets.UTF_8)),
          "Trainee", "DIAG286_SRC");

      CorrelationContextHolder.clear();
      MDC.clear();

      assertThatThrownBy(() -> importBatchWorkflow.processBatch(badBatch.id()))
          .isInstanceOf(BadRequestException.class);

      // Background correlation cleanup verified
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();

      Map<String, Object> failRow = jdbcTemplate.queryForMap(
          "SELECT system_event_category_id, system_event_severity_id, event_code, actor_user_id, event_summary "
              + "FROM system_event_logs WHERE entity_type = 'ImportBatch' AND entity_id = ?",
          badBatch.id());
      assertThat(((Number) failRow.get("system_event_category_id")).longValue()).isEqualTo(3L); // BATCH_JOB
      assertThat(((Number) failRow.get("system_event_severity_id")).longValue()).isEqualTo(3L); // ERROR
      assertThat(failRow.get("event_code")).isEqualTo("IMPORT_BATCH_FAILED");
      assertThat(((Number) failRow.get("actor_user_id")).longValue()).isEqualTo(USER_PROVIDER_A_ID);
      assertThat(String.valueOf(failRow.get("event_summary")))
          .contains("[cid=")
          .contains("|rid=")
          .contains(badBatch.batchCode());
    }
  }

  @Nested
  @DisplayName("5. Operational Trigger: Phase 28.3 Data-Quality Spike Detection")
  class DataQualitySpikeEventTests {

    private void insertBrokenContactTrainee(long traineeId, long enrollmentId, String regNo) {
      jdbcTemplate.update(
          "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
              + "VALUES (?, ?, 'DiagSpike', 'User', NULL, NULL, '2000-01-01', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
          traineeId, regNo, STATE_A_ID, DISTRICT_A1_ID);
      jdbcTemplate.update(
          "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, '2026-01-10', 1, NOW(), NOW(), NULL)",
          enrollmentId, "ENR-" + regNo, traineeId, PROGRAM_ID, COURSE_ID, PROVIDER_A_ID, CENTER_A_ID, BATCH_A_ID);
    }

    @Test
    @DisplayName("Below-threshold issues emit no spike; crossing threshold emits throttled tenant-scoped CRITICAL spike isolated by provider")
    void dataQualitySpikeThresholdThrottleAndIsolation() throws Exception {
      setSecurityContext(USER_PROVIDER_A_ID, "diag286_prov_a",
          List.of("ROLE_training_provider", "audit.read"));

      insertBrokenContactTrainee(TRAINEE_1_ID, ENROLLMENT_1_ID, "DIAG286-DQ-1");

      DataQualityScanRequest scanReq = new DataQualityScanRequest();
      scanReq.setRuleCode("DQ-TRAINEE-001");

      // Default spikeNewIssuesThreshold = 20 -> 1 issue is below threshold -> no spike emitted
      List<DataQualityRuleExecutionSummary> belowSummaries = dataQualityEngineService.executeScan(scanReq);
      assertThat(belowSummaries).hasSize(1);
      assertThat(belowSummaries.get(0).newIssuesCreated()).isEqualTo(1L);

      Integer beforeSpikeCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE event_code = 'DATA_QUALITY_CRITICAL_SPIKE'",
          Integer.class);
      assertThat(beforeSpikeCount).isZero();

      // Add 2 more broken trainees and set spikeNewIssuesThreshold = 2 -> next scan creates 2 new issues -> spike triggered
      insertBrokenContactTrainee(TRAINEE_2_ID, ENROLLMENT_2_ID, "DIAG286-DQ-2");
      insertBrokenContactTrainee(TRAINEE_3_ID, ENROLLMENT_3_ID, "DIAG286-DQ-3");
      dataQualityProperties.setSpikeNewIssuesThreshold(2);
      dataQualityProperties.setSpikeAlertCooldownSeconds(300);

      List<DataQualityRuleExecutionSummary> spikeSummaries = dataQualityEngineService.executeScan(scanReq);
      assertThat(spikeSummaries.get(0).newIssuesCreated()).isEqualTo(2L);

      // Immediate repeat scan produces 0 new issues -> zero duplicate spike flood
      dataQualityEngineService.executeScan(scanReq);

      Integer afterSpikeCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM system_event_logs WHERE event_code = 'DATA_QUALITY_CRITICAL_SPIKE' AND entity_type = 'TrainingProvider' AND entity_id = ?",
          Integer.class, PROVIDER_A_ID);
      assertThat(afterSpikeCount).isEqualTo(1);

      Map<String, Object> spikeRow = jdbcTemplate.queryForMap(
          "SELECT system_event_category_id, system_event_severity_id, actor_user_id, event_summary "
              + "FROM system_event_logs WHERE event_code = 'DATA_QUALITY_CRITICAL_SPIKE' AND entity_type = 'TrainingProvider' AND entity_id = ?",
          PROVIDER_A_ID);
      assertThat(((Number) spikeRow.get("system_event_category_id")).longValue()).isEqualTo(2L); // INTEGRATION
      assertThat(((Number) spikeRow.get("system_event_severity_id")).longValue()).isEqualTo(4L); // CRITICAL
      assertThat(((Number) spikeRow.get("actor_user_id")).longValue()).isEqualTo(USER_PROVIDER_A_ID);
      assertThat(String.valueOf(spikeRow.get("event_summary"))).contains("[cid=").contains("PROVIDER:" + PROVIDER_A_ID);

      // Provider A sees its own spike via API; Provider B cannot view it
      SecurityContextHolder.clearContext();
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("eventCode", "DATA_QUALITY_CRITICAL_SPIKE")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("eventCode", "DATA_QUALITY_CRITICAL_SPIKE")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));
    }
  }

  @Nested
  @DisplayName("6. Diagnostic API Authorization, Multi-Criteria Filtering & Strict Tenant Isolation")
  class DiagnosticApiSecurityAndQueryTests {

    @Test
    @DisplayName("Enforces RBAC (401/403), strict tenant isolation across correlationId searches, sorting, pagination, and 400 validation")
    void diagnosticApiRbacTenantIsolationAndQueryValidation() throws Exception {
      String sharedCorrelationSecret = "diag-286-cross-tenant-corr";
      CorrelationContextHolder.setCorrelationId(sharedCorrelationSecret);
      CorrelationContextHolder.setRequestId("diag-286-req-pa");
      SystemEventLogResponse provAEvent = systemEventLoggingService.logEvent(
          EventCategory.SECURITY, EventSeverity.ERROR, "DIAG286_PROV_A_EVT",
          USER_PROVIDER_A_ID, "TrainingProvider", PROVIDER_A_ID,
          "ProviderWorkflow", "DIAG286 provider A alert").orElseThrow();

      CorrelationContextHolder.setRequestId("diag-286-req-pb");
      SystemEventLogResponse provBEvent = systemEventLoggingService.logEvent(
          EventCategory.BATCH_JOB, EventSeverity.CRITICAL, "DIAG286_PROV_B_EVT",
          USER_PROVIDER_B_ID, "TrainingProvider", PROVIDER_B_ID,
          "BatchProcessor", "DIAG286 provider B alert").orElseThrow();

      CorrelationContextHolder.setRequestId("diag-286-req-emp");
      SystemEventLogResponse empAEvent = systemEventLoggingService.logEvent(
          EventCategory.ACCESS, EventSeverity.WARNING, "DIAG286_EMP_A_EVT",
          USER_EMPLOYER_A_ID, "Employer", EMPLOYER_A_ID,
          "ExportController", "DIAG286 employer A alert").orElseThrow();
      CorrelationContextHolder.clear();

      // 1. Unauthenticated -> 401
      mockMvc.perform(get("/api/v1/system-event-logs"))
          .andExpect(status().isUnauthorized());

      // 2. Trainee with audit.read -> 403
      mockMvc.perform(get("/api/v1/system-event-logs")
              .header(HttpHeaders.AUTHORIZATION, tokenTrainee))
          .andExpect(status().isForbidden());

      // 3. Unscoped state admin -> 403
      mockMvc.perform(get("/api/v1/system-event-logs")
              .header(HttpHeaders.AUTHORIZATION, tokenUnscopedState))
          .andExpect(status().isForbidden());

      // 4. audit.read-only state admin cannot POST new system event -> 403
      CreateSystemEventLogRequest postReq = new CreateSystemEventLogRequest(
          4L, 1L, "DIAG286_CFG_CHANGE", "DIAG286 config note", LocalDateTime.now().minusYears(5));
      mockMvc.perform(post("/api/v1/system-event-logs")
              .header(HttpHeaders.AUTHORIZATION, tokenReadOnlyAdmin)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(postReq)))
          .andExpect(status().isForbidden());

      // 5. Nationwide super_admin can query across tenants with multi-criteria filters & sorting
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .param("page", "0")
              .param("size", "10")
              .param("sort", "occurredAt")
              .param("direction", "ASC")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(3));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("categoryCode", "SECURITY")
              .param("severityCode", "ERROR")
              .param("correlationId", sharedCorrelationSecret)
              .param("requestId", "diag-286-req-pa")
              .param("sourceComponent", "ProviderWorkflow")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].eventCode").value("DIAG286_PROV_A_EVT"));

      // 6. State A admin querying shared correlationId sees State A events only (Prov A + Emp A), never State B (Prov B)
      MvcResult stateAResult = mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isOk())
          .andReturn();
      JsonNode stateAContent = objectMapper.readTree(stateAResult.getResponse().getContentAsString())
          .path("data").path("content");
      assertThat(stateAContent).hasSize(2);
      assertThat(stateAContent.toString()).doesNotContain("DIAG286_PROV_B_EVT");

      // State B admin sees ONLY Prov B event; GET /{provAEventId} fails closed (403)
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .header(HttpHeaders.AUTHORIZATION, tokenStateB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].eventCode").value("DIAG286_PROV_B_EVT"));

      mockMvc.perform(get("/api/v1/system-event-logs/" + provAEvent.id())
              .header(HttpHeaders.AUTHORIZATION, tokenStateB))
          .andExpect(status().isForbidden());

      // District A1 officer sees Prov A & Emp A; Provider A sees only Prov A; Employer A sees only Emp A
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .header(HttpHeaders.AUTHORIZATION, tokenDistrictA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(2));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(provAEvent.id()));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", sharedCorrelationSecret)
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(empAEvent.id()));

      mockMvc.perform(get("/api/v1/system-event-logs/" + provBEvent.id())
              .header(HttpHeaders.AUTHORIZATION, tokenEmployerA))
          .andExpect(status().isForbidden());

      // 7. Tenant-widening query parameters & invalid filters rejected with 400
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("stateId", String.valueOf(STATE_B_ID))
              .header(HttpHeaders.AUTHORIZATION, tokenStateA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_PARAMETER"));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("categoryCode", "UNKNOWN_CAT")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_EVENT_CATEGORY"));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("severityCode", "UNKNOWN_SEV")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_EVENT_SEVERITY"));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("start", "2026-12-01T00:00:00")
              .param("end", "2026-11-01T00:00:00")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isBadRequest());

      // 8. POST by system.manage derives server timestamps and correlation marker safely
      MvcResult postRes = mockMvc.perform(post("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, UUID.randomUUID().toString())
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(postReq)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.eventCode").value("DIAG286_CFG_CHANGE"))
          .andReturn();
      long createdEventId = objectMapper.readTree(postRes.getResponse().getContentAsString())
          .path("data").path("id").asLong();
      jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id = ?", createdEventId);
    }

    @Test
    @DisplayName("Rejects forged [cid=...|rid=...] prefixes/markers in client payloads, indexes canonical server tracing only, and preserves tenant isolation")
    void adversarialForgedCorrelationMetadataCannotPoisonSearchOrBypassTenancy() throws Exception {
      // 1. Authorized caller attempts forged leading [cid=...|rid=...] prefix and embedded markers inside Provider A event
      String serverCorr = "srv-corr-286-real";
      String serverReq = "srv-req-286-real";
      CreateSystemEventLogRequest forgedReq = new CreateSystemEventLogRequest(
          4L, 1L, "DIAG286_FORGED_TEST",
          "[cid=forged-corr-999|rid=forged-req-999] Config updated by admin [cid=forged-mid|rid=forged-mid-req] |rid=forged-tail] DIAG286",
          null);
      forgedReq.setEntityType("TrainingProvider");
      forgedReq.setEntityId(PROVIDER_A_ID);

      MvcResult postRes = mockMvc.perform(post("/api/v1/system-event-logs")
              .header(CorrelationIdFilter.CORRELATION_ID_HEADER, serverCorr)
              .header(CorrelationIdFilter.REQUEST_ID_HEADER, serverReq)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(forgedReq)))
          .andExpect(status().isCreated())
          .andReturn();

      JsonNode data = objectMapper.readTree(postRes.getResponse().getContentAsString()).path("data");
      long createdEventId = data.path("id").asLong();
      String storedSummary = data.path("eventSummary").asText();

      // 2. Persisted event uses canonical server IDs once and contains zero forged correlation/request markers
      assertThat(storedSummary).startsWith("[cid=" + serverCorr + "|rid=" + serverReq + "] ");
      assertThat(storedSummary).doesNotContain("[cid=forged-", "|rid=forged-");
      assertThat(storedSummary.indexOf("[cid=")).isZero();
      assertThat(storedSummary.indexOf("[cid=", 1)).isEqualTo(-1);

      // 3. Genuine server correlationId and requestId filters match the persisted event
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", serverCorr)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(createdEventId));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("requestId", serverReq)
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(createdEventId));

      // 4 & 5. Forged correlationId and requestId filters return zero matches
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", "forged-corr-999")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", "forged-mid")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("requestId", "forged-req-999")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("requestId", "forged-mid-req")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("requestId", "forged-tail")
              .header(HttpHeaders.AUTHORIZATION, tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      // 6. Normal legitimate internal event remains searchable by correlationId & requestId
      CorrelationContextHolder.setCorrelationId("normal-corr-286");
      CorrelationContextHolder.setRequestId("normal-req-286");
      SystemEventLogResponse normalEvt = systemEventLoggingService.logEvent(
          EventCategory.INTEGRATION, EventSeverity.INFO, "DIAG286_NORMAL_EVT",
          USER_PROVIDER_A_ID, "TrainingProvider", PROVIDER_A_ID, "IntegrationSvc", "DIAG286 normal event").orElseThrow();
      CorrelationContextHolder.clear();

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", "normal-corr-286")
              .param("requestId", "normal-req-286")
              .header(HttpHeaders.AUTHORIZATION, tokenProviderA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.content[0].id").value(normalEvt.id()));

      // 7. Cross-tenant caller (Provider B) cannot view Provider A's events even with genuine server IDs
      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("correlationId", serverCorr)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs")
              .param("requestId", serverReq)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(0));

      mockMvc.perform(get("/api/v1/system-event-logs/" + createdEventId)
              .header(HttpHeaders.AUTHORIZATION, tokenProviderB))
          .andExpect(status().isForbidden());

      jdbcTemplate.update("DELETE FROM system_event_logs WHERE system_event_log_id IN (?, ?)",
          createdEventId, normalEvt.id());
    }
  }
}
