package in.gov.sih.sih26135.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheConfig;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.controller.TrainingEnrollmentController;
import in.gov.sih.sih26135.controller.workflow.EmploymentLifecycleWorkflowController;
import in.gov.sih.sih26135.controller.workflow.PlacementEmploymentWorkflowController;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.controller.workflow.TrainingOutcomeWorkflowController;
import in.gov.sih.sih26135.controller.workflow.VerificationWorkflowController;
import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.RemediationTriggerRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.TraineeEnrollmentOrchestrationRequest;
import in.gov.sih.sih26135.dto.response.RemediationTriggerResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.event.TraineeLifecycleEvent;
import in.gov.sih.sih26135.event.listener.CandidateLifecycleEventListener;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CandidateLifecycleOrchestratorService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Spring Boot database-backed integration test for Phase 29.2 Candidate Lifecycle Orchestration.
 *
 * <p>Requirements verified:
 * <ul>
 *   <li>Dedicated test catalog guard: Connection strictly targets SIH26135_TEST.</li>
 *   <li>Production entry-point wiring: Controllers connect through the orchestrator bean and fail closed.</li>
 *   <li>Transactional proxy & boundaries: Orchestrator service is a genuine Spring transactional proxy.</li>
 *   <li>Mandatory consent enforcement: DATA_PROCESSING required; cannot be bypassed via existing REST endpoint.</li>
 *   <li>Real transaction rollback: Multi-service operation rolls back partial writes automatically on failure.</li>
 *   <li>Committed event processing vs. rollback: AFTER_COMMIT listener executed on commit, suppressed on rollback.</li>
 *   <li>Cache invalidation: evictOnEnrollment clears target cache regions on successful commit.</li>
 *   <li>Tenant isolation: Non-nationwide caller without matching tenant authority fails closed on existing entity.</li>
 *   <li>Post-commit remediation: Independent transaction commit on assessment and honest non-durable advisory reporting.</li>
 * </ul>
 */
@SpringBootTest
@DisplayName("Phase 29.2 — Candidate Lifecycle Spring Integration Tests (SIH26135_TEST)")
class CandidateLifecycleSpringIntegrationTest {

  private static final Long FIXTURE_STATE_ID = 9981L;
  private static final Long FIXTURE_DISTRICT_ID = 9981L;
  private static final Long FIXTURE_SECTOR_ID = 9981L;
  private static final Long FIXTURE_COURSE_ID = 9981L;
  private static final Long FIXTURE_ORG_ID = 9981L;
  private static final Long FIXTURE_DEPT_ID = 9981L;
  private static final Long FIXTURE_SCHEME_ID = 9981L;
  private static final Long FIXTURE_PROGRAM_ID = 9981L;
  private static final Long FIXTURE_PROVIDER_ID = 9981L;
  private static final Long FIXTURE_CENTER_ID = 9981L;
  private static final Long FIXTURE_BATCH_ID = 9981L;
  private static final Long FIXTURE_TRAINEE_ID = 9981L;
  private static final Long FIXTURE_SKILL_CAT_ID = 9981L;
  private static final Long FIXTURE_SKILL_ID = 9981L;
  private static final Long FIXTURE_SGA_ID = 9981L;
  private static final Long FIXTURE_GAP_ID = 9981L;
  private static final Long FIXTURE_REC_ID = 9981L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private CandidateLifecycleOrchestratorService orchestratorService;

  @Autowired
  private CandidateLifecycleEventListener eventListener;

  @Autowired
  private TrainingOutcomeWorkflowController trainingOutcomeWorkflowController;

  @Autowired
  private PlacementEmploymentWorkflowController placementEmploymentWorkflowController;

  @Autowired
  private VerificationWorkflowController verificationWorkflowController;

  @Autowired
  private EmploymentLifecycleWorkflowController employmentLifecycleWorkflowController;

  @Autowired
  private TrainingEnrollmentController trainingEnrollmentController;

  @Autowired
  private SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;

  @Autowired
  private TrainingEnrollmentService trainingEnrollmentService;

  @Autowired
  private AnalyticsCacheEvictionService evictionService;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() throws Exception {
    jdbcTemplate = new JdbcTemplate(dataSource);
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
    }

    // Clean up any stale fixtures before test execution
    cleanUpDomainFixtures();

    // Insert clean, isolated test fixtures in SIH26135_TEST
    insertDomainFixtures();

    // Default nationwide admin authentication
    setAdminAuthentication();

    if (eventListener != null) {
      eventListener.clearProcessedRemediationEvents();
    }
  }

  @AfterEach
  void tearDown() {
    try {
      cleanUpDomainFixtures();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void setAdminAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new in.gov.sih.sih26135.security.UserPrincipal(1L, "admin"),
            "N/A",
            List.of(
                new SimpleGrantedAuthority("admin.nationwide"),
                new SimpleGrantedAuthority("ROLE_super_admin"),
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("enrollment.manage"),
                new SimpleGrantedAuthority("assessment.manage")
            )
        )
    );
  }

  private void insertDomainFixtures() {
    // 1. Geography
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'T8', 'Test State 9981', 1, NOW(), NOW())",
        FIXTURE_STATE_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'TD81', 'Test District 9981', 1, NOW(), NOW())",
        FIXTURE_DISTRICT_ID, FIXTURE_STATE_ID);

    // 2. Organization, Department, Scheme, Program
    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-81', 'Test Sector 81', 1, NOW(), NOW())",
        FIXTURE_SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-81', 'Test Course 81', ?, 1, 1, NOW(), NOW(), NULL)",
        FIXTURE_COURSE_ID, FIXTURE_SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-81', 'Test Org 81', 1, 1, NOW(), NOW())",
        FIXTURE_ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-81', 'Test Dept 81', 1, NOW(), NOW())",
        FIXTURE_DEPT_ID, FIXTURE_ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-81', 'Test Scheme 81', ?, 'INR', 1, NOW(), NOW())",
        FIXTURE_SCHEME_ID, FIXTURE_DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-81', 'Test Program 81', ?, ?, 1, NOW(), NOW())",
        FIXTURE_PROGRAM_ID, FIXTURE_DEPT_ID, FIXTURE_SCHEME_ID);

    // 3. Provider & Center
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-81', 'Provider 81', 'REG-PRV-81', 1, ?, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        FIXTURE_PROVIDER_ID, FIXTURE_ORG_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-81', 'Center 81', ?, ?, 1, NOW(), NOW(), NULL)",
        FIXTURE_CENTER_ID, FIXTURE_PROVIDER_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    // 4. Batch
    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-81', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        FIXTURE_BATCH_ID, FIXTURE_COURSE_ID, FIXTURE_PROVIDER_ID, FIXTURE_CENTER_ID, FIXTURE_PROGRAM_ID);

    // 5. Trainee
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'REG-TR-9981', 'Valid', 'Trainee', '9800009981', 'tr9981@test.gov.in', '2000-01-01', 1, ?, ?, 1, 3, NOW(), NOW(), NULL)",
        FIXTURE_TRAINEE_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID);

    // 6. Skill Category & Skill (for rollback / remediation testing)
    jdbcTemplate.update(
        "INSERT INTO skill_categories (skill_category_id, category_code, category_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'CAT-81', 'Category 81', 1, NOW(), NOW())",
        FIXTURE_SKILL_CAT_ID);
    jdbcTemplate.update(
        "INSERT INTO skills (skill_id, skill_code, skill_name, skill_category_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SKL-81', 'Skill 81', ?, 1, NOW(), NOW())",
        FIXTURE_SKILL_ID, FIXTURE_SKILL_CAT_ID);

    // 7. Course Skill mapping
    jdbcTemplate.update(
        "INSERT INTO course_skills (course_id, skill_id, taught_skill_level_id, skill_importance_id, is_core_skill, created_at, updated_at) "
            + "VALUES (?, ?, 1, 1, 1, NOW(), NOW())",
        FIXTURE_COURSE_ID, FIXTURE_SKILL_ID);
  }

  private void insertRemediationFixtures() {
    jdbcTemplate.update(
        "INSERT INTO skill_gap_assessments (skill_gap_assessment_id, assessment_number, trainee_id, assessed_on, "
            + "skill_gap_assessment_status_id, skill_gap_source_id, created_at, updated_at) "
            + "VALUES (?, 'SGA-REMED-9981', ?, NOW(), 1, 1, NOW(), NOW())",
        FIXTURE_SGA_ID, FIXTURE_TRAINEE_ID);

    jdbcTemplate.update(
        "INSERT INTO skill_gaps (skill_gap_id, skill_gap_number, skill_gap_assessment_id, trainee_id, skill_id, "
            + "required_skill_level_id, observed_skill_level_id, skill_gap_severity_id, skill_gap_status_id, "
            + "skill_gap_source_id, is_current, identified_on, created_at, updated_at) "
            + "VALUES (?, 'SG-REMED-9981', ?, ?, ?, 3, 1, 2, 1, 1, 1, NOW(), NOW(), NOW())",
        FIXTURE_GAP_ID, FIXTURE_SGA_ID, FIXTURE_TRAINEE_ID, FIXTURE_SKILL_ID);

    jdbcTemplate.update(
        "INSERT INTO skill_gap_recommendations (skill_gap_recommendation_id, skill_gap_id, recommendation_number, "
            + "skill_gap_action_type_id, recommended_course_id, is_accepted_flag, accepted_on, created_at, updated_at) "
            + "VALUES (?, ?, 1, 1, ?, 1, NOW(), NOW(), NOW())",
        FIXTURE_REC_ID, FIXTURE_GAP_ID, FIXTURE_COURSE_ID);
  }

  private void cleanUpDomainFixtures() {
    if (jdbcTemplate == null) {
      return;
    }
    // Delete in reverse dependency order
    try {
      jdbcTemplate.update("DELETE FROM skill_gap_recommendations WHERE skill_gap_id IN (SELECT skill_gap_id FROM skill_gaps WHERE trainee_id = ?) OR skill_gap_recommendation_id = ?", FIXTURE_TRAINEE_ID, FIXTURE_REC_ID);
      jdbcTemplate.update("DELETE FROM skill_gaps WHERE trainee_id = ? OR skill_gap_id = ?", FIXTURE_TRAINEE_ID, FIXTURE_GAP_ID);
      jdbcTemplate.update("DELETE FROM skill_gap_assessments WHERE trainee_id = ? OR skill_gap_assessment_id = ?", FIXTURE_TRAINEE_ID, FIXTURE_SGA_ID);
      jdbcTemplate.update("DELETE FROM course_skills WHERE course_id = ? AND skill_id = ?", FIXTURE_COURSE_ID, FIXTURE_SKILL_ID);
      jdbcTemplate.update("DELETE FROM training_enrollments WHERE trainee_id = ? OR batch_id = ?", FIXTURE_TRAINEE_ID, FIXTURE_BATCH_ID);
      jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM skills WHERE skill_id = ?", FIXTURE_SKILL_ID);
      jdbcTemplate.update("DELETE FROM skill_categories WHERE skill_category_id = ?", FIXTURE_SKILL_CAT_ID);
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
      jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", FIXTURE_BATCH_ID);
      jdbcTemplate.update("DELETE FROM training_centers WHERE center_id = ?", FIXTURE_CENTER_ID);
      jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", FIXTURE_PROVIDER_ID);
      jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", FIXTURE_PROGRAM_ID);
      jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", FIXTURE_SCHEME_ID);
      jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", FIXTURE_DEPT_ID);
      jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", FIXTURE_ORG_ID);
      jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", FIXTURE_COURSE_ID);
      jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", FIXTURE_SECTOR_ID);
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", FIXTURE_DISTRICT_ID);
      jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", FIXTURE_STATE_ID);
    } catch (Exception ignored) {
    }
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
  @DisplayName("2. Orchestrator service is wired as a genuine Spring transactional proxy")
  void testOrchestratorIsTransactionalProxy() {
    assertThat(orchestratorService).isNotNull();
    assertThat(AopUtils.isAopProxy(orchestratorService))
        .as("CandidateLifecycleOrchestratorService must be wrapped in a Spring AOP transactional proxy")
        .isTrue();
  }

  @Test
  @DisplayName("3. Production workflow controllers and training enrollment controller are wired to orchestrator")
  void testControllersAreConnectedToOrchestrator() throws Exception {
    assertThat(getControllerOrchestrator(trainingOutcomeWorkflowController))
        .as("TrainingOutcomeWorkflowController must have orchestrator injected")
        .isSameAs(orchestratorService);

    assertThat(getControllerOrchestrator(placementEmploymentWorkflowController))
        .as("PlacementEmploymentWorkflowController must have orchestrator injected")
        .isSameAs(orchestratorService);

    assertThat(getControllerOrchestrator(verificationWorkflowController))
        .as("VerificationWorkflowController must have orchestrator injected")
        .isSameAs(orchestratorService);

    assertThat(getControllerOrchestrator(employmentLifecycleWorkflowController))
        .as("EmploymentLifecycleWorkflowController must have orchestrator injected")
        .isSameAs(orchestratorService);

    assertThat(getControllerOrchestrator(trainingEnrollmentController))
        .as("TrainingEnrollmentController must have orchestrator injected")
        .isSameAs(orchestratorService);

    // Verify fail-closed behavior when controller is constructed without orchestrator
    TrainingEnrollmentController disconnectedController = new TrainingEnrollmentController(
        (in.gov.sih.sih26135.service.TrainingEnrollmentService) org.mockito.Mockito.mock(in.gov.sih.sih26135.service.TrainingEnrollmentService.class)
    );
    CreateTrainingEnrollmentRequest req = new CreateTrainingEnrollmentRequest();
    req.setTraineeId(FIXTURE_TRAINEE_ID);
    req.setBatchId(FIXTURE_BATCH_ID);
    req.setEnrollmentDate(LocalDate.now());
    assertThatThrownBy(() -> disconnectedController.createEnrollment(req, new MockHttpServletRequest("POST", "/api/v1/training-enrollments")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("orchestration");
  }

  @Test
  @DisplayName("4. Stage A: Mandatory DATA_PROCESSING consent is enforced across all entry points without bypass")
  void testMandatoryConsentEnforcementAcrossAllEntryPoints() {
    TraineeEnrollmentOrchestrationRequest req = new TraineeEnrollmentOrchestrationRequest();
    req.setTraineeId(FIXTURE_TRAINEE_ID);
    req.setBatchId(FIXTURE_BATCH_ID);
    req.setEnrollmentDate(LocalDate.of(2026, 1, 15));

    // Case 4a: Existing trainee with NO consent records fails with CONSENT_REQUIRED
    assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REQUIRED");
          assertThat(bre.getMessage()).contains("DATA_PROCESSING");
        });

    // Case 4b: Caller attempts bypass using secondary consent (consent_type_id = 2, EMPLOYMENT_FOLLOW_UP)
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, created_at, updated_at) "
            + "VALUES (?, 2, 'Follow up', 1, 'v1.0', NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    req.setConsentTypeId(2L);
    assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REQUIRED");
          assertThat(bre.getMessage()).contains("DATA_PROCESSING");
        });

    // Case 4c: Trainee has DATA_PROCESSING consent, but status is REVOKED (consent_status_id = 2)
    jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at, created_at, updated_at) "
            + "VALUES (?, 1, 'Data processing', 2, 'v1.0', NOW(), NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REVOKED");
        });

    // Case 4d: Production REST endpoint POST /api/v1/training-enrollments cannot bypass consent
    CreateTrainingEnrollmentRequest restReq = new CreateTrainingEnrollmentRequest();
    restReq.setTraineeId(FIXTURE_TRAINEE_ID);
    restReq.setBatchId(FIXTURE_BATCH_ID);
    restReq.setEnrollmentDate(LocalDate.of(2026, 1, 1));
    restReq.setStartDate(LocalDate.of(2026, 1, 1));
    restReq.setExpectedCompletionDate(LocalDate.of(2026, 6, 30));
    restReq.setEnrollmentNumber("ENR-TEST-9981-REV");
    restReq.setEnrollmentStatusId(1L);

    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest("POST", "/api/v1/training-enrollments");
    assertThatThrownBy(() -> trainingEnrollmentController.createEnrollment(restReq, mockHttpRequest))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REVOKED");
        });

    // Case 4e: Valid DATA_PROCESSING consent permits enrollment creation via production endpoint
    jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, created_at, updated_at) "
            + "VALUES (?, 1, 'Data processing', 1, 'v1.0', NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    restReq.setEnrollmentNumber("ENR-TEST-9981-OK");
    ResponseEntity<ApiResponse<TrainingEnrollmentResponse>> response =
        trainingEnrollmentController.createEnrollment(restReq, mockHttpRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData()).isNotNull();
    assertThat(response.getBody().getData().getEnrollmentNumber()).isEqualTo("ENR-TEST-9981-OK");

    // Verify row committed in training_enrollments
    Integer enrollmentCount = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_number = 'ENR-TEST-9981-OK'",
        Integer.class
    );
    assertThat(enrollmentCount).isEqualTo(1);
  }

  @Test
  @DisplayName("5. Real transaction rollback: Multi-service orchestrated operation rolls back partial writes on failure")
  void testRealTransactionRollbackOnFailure() {
    String assessmentNumber = "TX-SGA-ROLLBACK-9981";
    String gapNum1 = "TX-GAP-ROLLBACK-9981";

    RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
    CreateSkillGapAssessmentRequest assessReq = new CreateSkillGapAssessmentRequest();
    assessReq.setAssessmentNumber(assessmentNumber);
    assessReq.setTraineeId(FIXTURE_TRAINEE_ID);
    assessReq.setSkillGapSourceId(1L);
    assessReq.setSkillGapAssessmentStatusId(1L);
    assessReq.setAssessedOn(LocalDate.now());
    request.setAssessmentRequest(assessReq);

    // Step 2a: Valid skill gap
    CreateSkillGapRequest gap1 = new CreateSkillGapRequest();
    gap1.setSkillGapNumber(gapNum1);
    gap1.setTraineeId(FIXTURE_TRAINEE_ID);
    gap1.setSkillId(FIXTURE_SKILL_ID);
    gap1.setSkillGapSourceId(1L);
    gap1.setSkillGapStatusId(1L);
    gap1.setIdentifiedOn(LocalDate.now());
    gap1.setRequiredSkillLevelId(3L);
    gap1.setObservedSkillLevelId(1L);
    gap1.setSkillGapSeverityId(2L);

    // Step 2b: Invalid skill gap referencing non-existent skill ID 999999L
    CreateSkillGapRequest gap2 = new CreateSkillGapRequest();
    gap2.setSkillGapNumber("TX-GAP-ERR-9981");
    gap2.setTraineeId(FIXTURE_TRAINEE_ID);
    gap2.setSkillId(999999L);
    gap2.setSkillGapSourceId(1L);
    gap2.setSkillGapStatusId(1L);
    gap2.setIdentifiedOn(LocalDate.now());
    gap2.setRequiredSkillLevelId(3L);
    gap2.setObservedSkillLevelId(1L);
    gap2.setSkillGapSeverityId(2L);

    request.setGapRequests(List.of(gap1, gap2));

    RemediationTriggerRequest remReq = new RemediationTriggerRequest(
        FIXTURE_TRAINEE_ID,
        "EMPLOYMENT_EXIT",
        1L,
        request,
        "Rollback multi-step verification"
    );

    // Multi-service execution fails on step 2b with ResourceNotFoundException
    assertThatThrownBy(() -> orchestratorService.triggerSkillGapRemediation(remReq))
        .isInstanceOf(ResourceNotFoundException.class);

    // Assert that preceding step 1 and step 2a writes were automatically rolled back
    Integer sgaCount = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE assessment_number = ?",
        Integer.class,
        assessmentNumber
    );
    assertThat(sgaCount).as("Preceding assessment write must be rolled back on multi-step failure").isEqualTo(0);

    Integer gapCount = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gaps WHERE skill_gap_number = ?",
        Integer.class,
        gapNum1
    );
    assertThat(gapCount).as("Preceding skill gap write must be rolled back on multi-step failure").isEqualTo(0);
  }

  @Test
  @DisplayName("6. AFTER_COMMIT event listener is triggered on commit and completely suppressed on rollback")
  void testAfterCommitListenerExecutionAndSuppression() {
    String commitEventId = "evt-commit-9981";
    String rollbackEventId = "evt-rollback-9981";

    // 1. Transaction that COMMITS: AFTER_COMMIT listener MUST execute
    DefaultTransactionDefinition defCommit = new DefaultTransactionDefinition();
    defCommit.setName("Tx-Commit-Listener-Test");
    TransactionStatus statusCommit = transactionManager.getTransaction(defCommit);

    eventPublisher.publishEvent(new TraineeLifecycleEvent(
        commitEventId,
        "EMPLOYMENT_EXIT",
        FIXTURE_TRAINEE_ID,
        1L,
        "corr-commit-test",
        LocalDateTime.now(),
        null, null, null, null, null, null,
        "EMPLOYED",
        "UNEMPLOYED",
        "{}"
    ));

    transactionManager.commit(statusCommit);

    // Proves actual AFTER_COMMIT execution: event ID recorded in processed set
    assertThat(eventListener.isRemediationEventProcessed(commitEventId))
        .as("AFTER_COMMIT event listener must execute when transaction successfully commits")
        .isTrue();

    // 2. Transaction that ROLLS BACK: AFTER_COMMIT listener MUST be suppressed
    DefaultTransactionDefinition defRollback = new DefaultTransactionDefinition();
    defRollback.setName("Tx-Rollback-Listener-Test");
    TransactionStatus statusRollback = transactionManager.getTransaction(defRollback);

    eventPublisher.publishEvent(new TraineeLifecycleEvent(
        rollbackEventId,
        "EMPLOYMENT_EXIT",
        FIXTURE_TRAINEE_ID,
        1L,
        "corr-rollback-test",
        LocalDateTime.now(),
        null, null, null, null, null, null,
        "EMPLOYED",
        "UNEMPLOYED",
        "{}"
    ));

    transactionManager.rollback(statusRollback);

    // Proves actual AFTER_COMMIT suppression: event ID NOT recorded in processed set
    assertThat(eventListener.isRemediationEventProcessed(rollbackEventId))
        .as("AFTER_COMMIT event listener must NOT execute when transaction is rolled back")
        .isFalse();
  }

  @Test
  @DisplayName("7. Analytics cache eviction occurs on successful commit and clears target cache regions")
  void testCacheEvictionOnCommit() {
    Cache nationalCache = cacheManager.getCache(AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW);
    if (nationalCache != null) {
      nationalCache.put("test-key-29-2", "test-value-cached");
      assertThat(nationalCache.get("test-key-29-2")).isNotNull();

      DefaultTransactionDefinition def = new DefaultTransactionDefinition();
      def.setName("Test-Cache-Evict-Tx");
      TransactionStatus status = transactionManager.getTransaction(def);

      evictionService.evictOnEnrollment(this);

      transactionManager.commit(status);

      // National overview cache must be cleared following committed eviction event
      assertThat(nationalCache.get("test-key-29-2")).isNull();
    }
  }

  @Test
  @DisplayName("8. Tenant isolation: Unprivileged non-administrative caller fails closed on existing trainee")
  void testTenantIsolationFailsClosedOnExistingTrainee() {
    // Authenticate as non-administrative user who has NO trainee or provider assignment
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "unassigned_user",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        )
    );

    TraineeEnrollmentOrchestrationRequest req = new TraineeEnrollmentOrchestrationRequest();
    req.setTraineeId(FIXTURE_TRAINEE_ID);
    req.setBatchId(FIXTURE_BATCH_ID);
    req.setEnrollmentDate(LocalDate.now());

    // Because trainee 9981 exists, scopeValidator enforces caller assignment check
    // and throws BadRequestException (not 404 ResourceNotFoundException)
    assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(req))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isIn(
              "AUTHENTICATED_USER_UNRESOLVED",
              "TRAINEE_ASSIGNMENT_REQUIRED",
              "TRAINEE_MISMATCH"
          );
        });
  }

  @Test
  @DisplayName("9. Post-commit remediation: Independent transaction commit on assessment and honest advisory reporting")
  void testPostCommitRemediationTransactionSafetyAndHonestAdvisory() {
    // Case 9a: When assessment data is absent, return PENDING_ASSESSMENT_DATA advisory without fabricating records
    RemediationTriggerRequest advisoryReq = new RemediationTriggerRequest(
        FIXTURE_TRAINEE_ID,
        "EMPLOYMENT_EXIT",
        1L,
        null,
        "Advisory trigger without assessment data"
    );

    RemediationTriggerResponse advisoryResp = orchestratorService.triggerSkillGapRemediation(advisoryReq);
    assertThat(advisoryResp).isNotNull();
    assertThat(advisoryResp.remediationStatus()).isEqualTo("PENDING_ASSESSMENT_DATA");
    assertThat(advisoryResp.assessmentResponse()).isNull();

    // Verify no fabricated skill gap assessment was created
    Integer count = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE trainee_id = ?",
        Integer.class,
        FIXTURE_TRAINEE_ID
    );
    assertThat(count).isEqualTo(0);

    // Case 9b: When assessment data is provided, execute within REQUIRES_NEW and commit to database
    String assessmentNumber = "TX-SGA-COMMIT-9981";
    RecordSkillGapAssessmentWithGapsRequest assessReq = new RecordSkillGapAssessmentWithGapsRequest();
    CreateSkillGapAssessmentRequest inner = new CreateSkillGapAssessmentRequest();
    inner.setAssessmentNumber(assessmentNumber);
    inner.setTraineeId(FIXTURE_TRAINEE_ID);
    inner.setSkillGapSourceId(1L);
    inner.setSkillGapAssessmentStatusId(1L);
    inner.setAssessedOn(LocalDate.now());
    assessReq.setAssessmentRequest(inner);

    CreateSkillGapRequest validGap = new CreateSkillGapRequest();
    validGap.setSkillGapNumber("TX-GAP-COMMIT-9981");
    validGap.setTraineeId(FIXTURE_TRAINEE_ID);
    validGap.setSkillId(FIXTURE_SKILL_ID);
    validGap.setSkillGapSourceId(1L);
    validGap.setSkillGapStatusId(1L);
    validGap.setIdentifiedOn(LocalDate.now());
    validGap.setRequiredSkillLevelId(3L);
    validGap.setObservedSkillLevelId(1L);
    validGap.setSkillGapSeverityId(2L);
    assessReq.setGapRequests(List.of(validGap));

    RemediationTriggerRequest fullReq = new RemediationTriggerRequest(
        FIXTURE_TRAINEE_ID,
        "EMPLOYMENT_EXIT",
        1L,
        assessReq,
        "Full assessment evaluation"
    );

    RemediationTriggerResponse fullResp = orchestratorService.triggerSkillGapRemediation(fullReq);
    assertThat(fullResp).isNotNull();
    assertThat(fullResp.remediationStatus()).isEqualTo("EVALUATED");
    assertThat(fullResp.assessmentResponse()).isNotNull();

    // Verify row committed in skill_gap_assessments
    Integer committedAssessments = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE assessment_number = ?",
        Integer.class,
        assessmentNumber
    );
    assertThat(committedAssessments).isEqualTo(1);
  }

  @Test
  @DisplayName("10. Authoritative boundary: Remediation-course enrollment enforces mandatory DATA_PROCESSING consent without bypass")
  void testRemediationEnrollmentMandatoryConsentEnforcement() {
    insertRemediationFixtures();

    CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
    enrolReq.setTraineeId(FIXTURE_TRAINEE_ID);
    enrolReq.setBatchId(FIXTURE_BATCH_ID);
    enrolReq.setCourseId(FIXTURE_COURSE_ID);
    enrolReq.setEnrollmentDate(LocalDate.of(2026, 1, 1));
    enrolReq.setStartDate(LocalDate.of(2026, 1, 1));
    enrolReq.setExpectedCompletionDate(LocalDate.of(2026, 6, 30));
    enrolReq.setEnrollmentStatusId(1L);
    enrolReq.setEnrollmentNumber("ENR-REMED-9981");

    SkillGapRemediationEnrollmentRequest remedReq = new SkillGapRemediationEnrollmentRequest();
    remedReq.setRecommendationId(FIXTURE_REC_ID);
    remedReq.setEnrollmentRequest(enrolReq);

    // 1. Normal enrollment directly via TrainingEnrollmentService without DATA_PROCESSING consent is rejected
    assertThatThrownBy(() -> trainingEnrollmentService.createEnrollment(enrolReq))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REQUIRED");
          assertThat(bre.getMessage()).contains("DATA_PROCESSING");
        });

    // 2. Remediation-course enrollment without DATA_PROCESSING consent is rejected (CONSENT_REQUIRED)
    assertThatThrownBy(() -> skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(remedReq))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REQUIRED");
          assertThat(bre.getMessage()).contains("DATA_PROCESSING");
        });

    // Verify rejected attempt creates NO partial enrollment or status updates
    Integer countNoConsent = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_number = 'ENR-REMED-9981'",
        Integer.class
    );
    assertThat(countNoConsent).isEqualTo(0);
    String statusNoConsent = jdbcTemplate.queryForObject(
        "SELECT s.status_code FROM skill_gaps g JOIN ref_skill_gap_status s ON g.skill_gap_status_id = s.skill_gap_status_id WHERE g.skill_gap_id = ?",
        String.class,
        FIXTURE_GAP_ID
    );
    assertThat(statusNoConsent).isEqualTo("OPEN");

    // 3. Secondary consent (consent_type_id = 2, EMPLOYMENT_FOLLOW_UP) cannot substitute for DATA_PROCESSING
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, created_at, updated_at) "
            + "VALUES (?, 2, 'Follow up only', 1, 'v1.0', NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    assertThatThrownBy(() -> skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(remedReq))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REQUIRED");
          assertThat(bre.getMessage()).contains("DATA_PROCESSING");
        });

    // 4. Revoked DATA_PROCESSING consent prevents remediation enrollment (CONSENT_REVOKED)
    jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at, created_at, updated_at) "
            + "VALUES (?, 1, 'Data processing revoked', 2, 'v1.0', NOW(), NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    assertThatThrownBy(() -> skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(remedReq))
        .isInstanceOf(BadRequestException.class)
        .satisfies(ex -> {
          BadRequestException bre = (BadRequestException) ex;
          assertThat(bre.getErrorCode()).isEqualTo("CONSENT_REVOKED");
        });

    // Verify revoked consent attempt creates NO partial enrollment or status updates
    Integer countRevoked = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_number = 'ENR-REMED-9981'",
        Integer.class
    );
    assertThat(countRevoked).isEqualTo(0);
    String statusRevoked = jdbcTemplate.queryForObject(
        "SELECT s.status_code FROM skill_gaps g JOIN ref_skill_gap_status s ON g.skill_gap_status_id = s.skill_gap_status_id WHERE g.skill_gap_id = ?",
        String.class,
        FIXTURE_GAP_ID
    );
    assertThat(statusRevoked).isEqualTo("OPEN");

    // 5. Valid DATA_PROCESSING consent permits remediation enrollment and transitions skill gap to IN_REMEDIATION
    jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", FIXTURE_TRAINEE_ID);
    jdbcTemplate.update(
        "INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, created_at, updated_at) "
            + "VALUES (?, 1, 'Data processing granted', 1, 'v1.0', NOW(), NOW(), NOW())",
        FIXTURE_TRAINEE_ID);

    SkillGapRemediationEnrollmentResponse response =
        skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(remedReq);

    assertThat(response.createdEnrollment()).isNotNull();
    assertThat(response.createdEnrollment().getEnrollmentNumber()).isEqualTo("ENR-REMED-9981");

    // Verify enrollment committed in database
    Integer countCommitted = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM training_enrollments WHERE enrollment_number = 'ENR-REMED-9981'",
        Integer.class
    );
    assertThat(countCommitted).isEqualTo(1);

    // Verify linked skill gap transitioned to IN_REMEDIATION
    String finalStatus = jdbcTemplate.queryForObject(
        "SELECT s.status_code FROM skill_gaps g JOIN ref_skill_gap_status s ON g.skill_gap_status_id = s.skill_gap_status_id WHERE g.skill_gap_id = ?",
        String.class,
        FIXTURE_GAP_ID
    );
    assertThat(finalStatus).isEqualTo("IN_REMEDIATION");
  }

  private Object getControllerOrchestrator(Object controller) throws Exception {
    Object target = org.springframework.test.util.AopTestUtils.getTargetObject(controller);
    Field field = target.getClass().getDeclaredField("candidateLifecycleOrchestratorService");
    field.setAccessible(true);
    return field.get(target);
  }
}
