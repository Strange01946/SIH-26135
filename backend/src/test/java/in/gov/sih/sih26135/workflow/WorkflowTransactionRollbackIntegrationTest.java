package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import in.gov.sih.sih26135.service.FollowupEngagementWorkflowService;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stage H Database-backed transaction integrity, rollback, and proxy contract verification.
 *
 * <p>Requirements and Verification Principles:
 * 1. Dedicated test catalog guard: Confirms active JDBC connection strictly targets SIH26135_TEST.
 *    Does not impose an arbitrary permanent zero-row requirement on domain tables so legitimate test fixtures
 *    are supported.
 * 2. Multi-step atomicity & rollback: Proves that an intermediate unhandled failure rolls back all preceding
 *    modifications within a Spring-managed @Transactional boundary, rolling back the test transaction's inserted
 *    rows and keeping table count unchanged (delta count == 0).
 * 3. Propagation.REQUIRED: Proves inner participating service methods join the outer physical transaction and that
 *    inner failures roll back the entire transaction.
 * 4. Transaction Engine Rollback Mechanics: Proves that throwing an unhandled BadRequestException (e.g.
 *    ENROLLMENT_TRAINEE_MISMATCH) within a transactional method triggers Spring TransactionSynchronization
 *    STATUS_ROLLED_BACK and causes MySQL to discard the uncommitted row with zero delta in table count.
 * 5. Exact contract-based signature audit: Audits 100% of exact method signatures (all 18 public methods:
 *    12 mutating, 6 read-only) across all 6 workflow service interfaces. Guarantees no missing, extra, duplicate,
 *    or unaccounted method signatures, and verifies @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
 *    on mutating methods and readOnly = true on queries.
 * 6. Genuine production workflow bean execution: Directly executes the six production workflow beans against SIH26135_TEST,
 *    asserting fail-fast validation and fail-closed lookups, and verifies by querying all 17 participating domain tables
 *    that row counts remain strictly unchanged across the tested invocations (without claiming full state integrity or
 *    absence of in-place modifications/balanced operations from count equality alone).
 *
 * <p>Environmental & Evidence Boundary Limitations (Explicit Disclosure):
 * - In SIH26135_TEST, reference tables (ref_*) are populated, whereas business domain tables (trainees, courses, batches,
 *   training_enrollments, placement_records, employment_records, skill_gaps, etc.) currently have 0 rows in baseline.
 * - Under strict project constraints forbidding synthetic data fabrication across 20+ cascading foreign keys,
 *   end-to-end multi-step rollback of production workflows across business domain tables remains unverified via database fixtures.
 * - Production transaction safety across those multi-write domain workflows is substantiated through source-level
 *   transaction-boundary and propagation analysis, unit tests verifying step-level short-circuiting, and database-level
 *   proof of Spring and MySQL rollback mechanics on populated tables.
 */
@SpringBootTest
@Import(WorkflowTransactionRollbackIntegrationTest.TestWorkflowConfig.class)
class WorkflowTransactionRollbackIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private RefSkillGapStatusRepository refSkillGapStatusRepository;

  @Autowired
  private TestWorkflowOrchestrator orchestrator;

  // Autowire the 6 production workflow services to verify their runtime transactional proxies and execution
  @Autowired
  private TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;

  @Autowired
  private PlacementEmploymentWorkflowService placementEmploymentWorkflowService;

  @Autowired
  private EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService;

  @Autowired
  private VerificationWorkflowService verificationWorkflowService;

  @Autowired
  private SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;

  @Autowired
  private FollowupEngagementWorkflowService followupEngagementWorkflowService;

  private static final String CODE_PASS_1 = "TX_INT_OK_1";
  private static final String CODE_PASS_2 = "TX_INT_OK_2";
  private static final String CODE_FAIL_1 = "TX_INT_ERR_1";
  private static final String CODE_FAIL_2 = "TX_INT_ERR_2";
  private static final String CODE_MISMATCH = "TX_ENROLL_MISMATCH";

  // Exact 17 domain tables participating in the six cross-domain workflows
  private static final List<String> WORKFLOW_DOMAIN_TABLES = List.of(
      "assessment_results",
      "certifications",
      "training_enrollments",
      "placement_records",
      "employment_records",
      "salary_history",
      "employment_exit_events",
      "trainee_unemployment_events",
      "employment_verification_requests",
      "employment_verifications",
      "employment_verification_evidence",
      "skill_gap_assessments",
      "skill_gaps",
      "skill_gap_recommendations",
      "followup_tasks",
      "survey_responses",
      "communication_logs"
  );

  // Exact contract mapping of all 18 public method signatures across the 6 workflow interfaces
  private static final Map<Class<?>, WorkflowContract> WORKFLOW_CONTRACTS = Map.of(
      TrainingOutcomeWorkflowService.class, new WorkflowContract(
          Set.of("recordAssessmentResultAndIssueCertificate(RecordAssessmentAndIssueCertificateRequest)"),
          Set.of("getTraineeTrainingOutcomeSummary(Long,Long)")
      ),
      PlacementEmploymentWorkflowService.class, new WorkflowContract(
          Set.of("transitionPlacementToEmployment(PlacementToEmploymentTransitionRequest)"),
          Set.of("getTraineePlacementEmploymentSummary(Long)")
      ),
      EmploymentLifecycleWorkflowService.class, new WorkflowContract(
          Set.of(
              "recordExitAndInitiateUnemployment(EmploymentExitAndUnemploymentRequest)",
              "transitionUnemploymentToEmployment(UnemploymentToEmploymentTransitionRequest)"
          ),
          Set.of("getTraineeCareerTimeline(Long)")
      ),
      VerificationWorkflowService.class, new WorkflowContract(
          Set.of(
              "initiateVerificationFromSurvey(InitiateVerificationFromSurveyRequest)",
              "recordVerificationVerdictAndSynchronize(RecordVerificationVerdictRequest)"
          ),
          Set.of("getEmploymentVerificationSummary(Long)")
      ),
      SkillGapRemediationWorkflowService.class, new WorkflowContract(
          Set.of(
              "recordAssessmentWithGapsAndRecommendations(RecordSkillGapAssessmentWithGapsRequest)",
              "acceptRecommendation(Long,TraineeRecommendationDecisionRequest)",
              "rejectRecommendation(Long,TraineeRecommendationDecisionRequest)",
              "enrollTraineeInRemediationCourse(SkillGapRemediationEnrollmentRequest)"
          ),
          Set.of("getTraineeSkillGapProfile(Long)")
      ),
      FollowupEngagementWorkflowService.class, new WorkflowContract(
          Set.of(
              "recordFollowupSurveyResponse(RecordFollowupSurveyResponseRequest)",
              "recordEngagementAttempt(RecordEngagementAttemptRequest)"
          ),
          Set.of("getTraineeEngagementHistory(Long)")
      )
  );

  private record WorkflowContract(Set<String> mutatingSignatures, Set<String> readOnlySignatures) {}

  private static String formatMethodSignature(Method m) {
    return m.getName() + "(" + Arrays.stream(m.getParameterTypes())
        .map(Class::getSimpleName)
        .collect(Collectors.joining(",")) + ")";
  }

  private static final Long FIXTURE_STATE_ID = 9991L;
  private static final String FIXTURE_STATE_CODE = "TS";
  private static final String FIXTURE_STATE_NAME = "Test State 9991";

  private static final Long FIXTURE_DISTRICT_ID = 9991L;
  private static final String FIXTURE_DISTRICT_CODE = "TD91";
  private static final String FIXTURE_DISTRICT_NAME = "Test District 9991";

  private static final Long FIXTURE_TRAINEE_ID = 9991L;
  private static final String FIXTURE_TRAINEE_REG = "REG-TX-9991";

  private static final Long FIXTURE_SKILL_CATEGORY_ID = 9991L;
  private static final String FIXTURE_SKILL_CAT_CODE = "CAT-TX-9991";
  private static final String FIXTURE_SKILL_CAT_NAME = "Category Tx 9991";

  private static final Long FIXTURE_SKILL_ID = 9991L;
  private static final String FIXTURE_SKILL_CODE = "SKL-TX-9991";
  private static final String FIXTURE_SKILL_NAME = "Skill Tx 9991";

  // Tracking flags to ensure cleanup targets ONLY records demonstrably created by this test
  private boolean stateCreatedByTest = false;
  private boolean districtCreatedByTest = false;
  private boolean traineeCreatedByTest = false;
  private boolean skillCategoryCreatedByTest = false;
  private boolean skillCreatedByTest = false;
  private final Set<String> createdAssessmentNumbers = new HashSet<>();
  private final Set<String> createdSkillGapNumbers = new HashSet<>();

  @BeforeEach
  void enforceDatabaseIsolationGuard() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
    }
    stateCreatedByTest = false;
    districtCreatedByTest = false;
    traineeCreatedByTest = false;
    skillCategoryCreatedByTest = false;
    skillCreatedByTest = false;
    createdAssessmentNumbers.clear();
    createdSkillGapNumbers.clear();
  }

  @AfterEach
  void cleanUpTestData() {
    refSkillGapStatusRepository.findByStatusCode(CODE_PASS_1).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_PASS_2).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_1).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_2).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_MISMATCH).ifPresent(refSkillGapStatusRepository::delete);
    cleanUpDomainFixture();
    SecurityContextHolder.clearContext();
  }

  private void checkNoCollisions(JdbcTemplate jdbc) {
    Integer stateCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM states WHERE state_id = ? OR state_code = ? OR state_name = ?",
        Integer.class, FIXTURE_STATE_ID, FIXTURE_STATE_CODE, FIXTURE_STATE_NAME);
    if (stateCollisions != null && stateCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: state record already exists with ID "
          + FIXTURE_STATE_ID + " or code '" + FIXTURE_STATE_CODE + "' or name '" + FIXTURE_STATE_NAME + "'. Aborting safely without deleting.");
    }

    Integer districtCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM districts WHERE district_id = ? OR district_code = ? OR district_name = ?",
        Integer.class, FIXTURE_DISTRICT_ID, FIXTURE_DISTRICT_CODE, FIXTURE_DISTRICT_NAME);
    if (districtCollisions != null && districtCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: district record already exists with ID "
          + FIXTURE_DISTRICT_ID + " or code '" + FIXTURE_DISTRICT_CODE + "' or name '" + FIXTURE_DISTRICT_NAME + "'. Aborting safely without deleting.");
    }

    Integer traineeCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM trainees WHERE trainee_id = ? OR registration_number = ?",
        Integer.class, FIXTURE_TRAINEE_ID, FIXTURE_TRAINEE_REG);
    if (traineeCollisions != null && traineeCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: trainee record already exists with ID "
          + FIXTURE_TRAINEE_ID + " or registration '" + FIXTURE_TRAINEE_REG + "'. Aborting safely without deleting.");
    }

    Integer catCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM skill_categories WHERE skill_category_id = ? OR category_code = ? OR category_name = ?",
        Integer.class, FIXTURE_SKILL_CATEGORY_ID, FIXTURE_SKILL_CAT_CODE, FIXTURE_SKILL_CAT_NAME);
    if (catCollisions != null && catCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: skill category record already exists with ID "
          + FIXTURE_SKILL_CATEGORY_ID + " or code '" + FIXTURE_SKILL_CAT_CODE + "' or name '" + FIXTURE_SKILL_CAT_NAME + "'. Aborting safely without deleting.");
    }

    Integer skillCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM skills WHERE skill_id = ? OR skill_code = ? OR skill_name = ?",
        Integer.class, FIXTURE_SKILL_ID, FIXTURE_SKILL_CODE, FIXTURE_SKILL_NAME);
    if (skillCollisions != null && skillCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: skill record already exists with ID "
          + FIXTURE_SKILL_ID + " or code '" + FIXTURE_SKILL_CODE + "' or name '" + FIXTURE_SKILL_NAME + "'. Aborting safely without deleting.");
    }

    Integer assessCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE assessment_number IN ('TX-SGA-ROLLBACK-001', 'TX-SGA-COMMIT-001')",
        Integer.class);
    if (assessCollisions != null && assessCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: assessment record already exists with test assessment numbers. Aborting safely without deleting.");
    }

    Integer gapCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM skill_gaps WHERE skill_gap_number IN ('TX-GAP-ROLLBACK-001', 'TX-GAP-ROLLBACK-002', 'TX-GAP-COMMIT-001')",
        Integer.class);
    if (gapCollisions != null && gapCollisions > 0) {
      throw new IllegalStateException("Test fixture setup collision: skill gap record already exists with test skill gap numbers. Aborting safely without deleting.");
    }
  }

  private void cleanUpDomainFixture() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    // 1. Clean up created skill gaps by their exact recorded numbers
    for (String gapNum : new HashSet<>(createdSkillGapNumbers)) {
      try {
        jdbc.update("DELETE FROM skill_gaps WHERE skill_gap_number = ?", gapNum);
      } catch (Exception ignored) {
      }
    }
    createdSkillGapNumbers.clear();

    // 2. Clean up created assessments by their exact recorded numbers
    for (String assessNum : new HashSet<>(createdAssessmentNumbers)) {
      try {
        jdbc.update("DELETE FROM skill_gap_assessments WHERE assessment_number = ?", assessNum);
      } catch (Exception ignored) {
      }
    }
    createdAssessmentNumbers.clear();

    // 3. Clean up skills only if demonstrably created by this test
    if (skillCreatedByTest) {
      try {
        jdbc.update("DELETE FROM skills WHERE skill_id = ? AND skill_code = ?", FIXTURE_SKILL_ID, FIXTURE_SKILL_CODE);
      } finally {
        skillCreatedByTest = false;
      }
    }

    // 4. Clean up skill categories only if demonstrably created by this test
    if (skillCategoryCreatedByTest) {
      try {
        jdbc.update("DELETE FROM skill_categories WHERE skill_category_id = ? AND category_code = ?", FIXTURE_SKILL_CATEGORY_ID, FIXTURE_SKILL_CAT_CODE);
      } finally {
        skillCategoryCreatedByTest = false;
      }
    }

    // 5. Clean up trainees only if demonstrably created by this test
    if (traineeCreatedByTest) {
      try {
        jdbc.update("DELETE FROM trainees WHERE trainee_id = ? AND registration_number = ?", FIXTURE_TRAINEE_ID, FIXTURE_TRAINEE_REG);
      } finally {
        traineeCreatedByTest = false;
      }
    }

    // 6. Clean up districts only if demonstrably created by this test
    if (districtCreatedByTest) {
      try {
        jdbc.update("DELETE FROM districts WHERE district_id = ? AND district_code = ?", FIXTURE_DISTRICT_ID, FIXTURE_DISTRICT_CODE);
      } finally {
        districtCreatedByTest = false;
      }
    }

    // 7. Clean up states only if demonstrably created by this test
    if (stateCreatedByTest) {
      try {
        jdbc.update("DELETE FROM states WHERE state_id = ? AND state_code = ?", FIXTURE_STATE_ID, FIXTURE_STATE_CODE);
      } finally {
        stateCreatedByTest = false;
      }
    }
  }

  private void insertMinimalDomainFixture() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    checkNoCollisions(jdbc);

    try {
      jdbc.update(
          "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, 1, NOW(), NOW())",
          FIXTURE_STATE_ID, FIXTURE_STATE_CODE, FIXTURE_STATE_NAME
      );
      stateCreatedByTest = true;

      jdbc.update(
          "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, 1, NOW(), NOW())",
          FIXTURE_DISTRICT_ID, FIXTURE_STATE_ID, FIXTURE_DISTRICT_CODE, FIXTURE_DISTRICT_NAME
      );
      districtCreatedByTest = true;

      jdbc.update(
          "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, "
              + "current_employment_status_id, profile_status_id, created_at, updated_at) "
              + "VALUES (?, ?, 'Rollback', 'Tester', 1, ?, ?, 1, 3, NOW(), NOW())",
          FIXTURE_TRAINEE_ID, FIXTURE_TRAINEE_REG, FIXTURE_STATE_ID, FIXTURE_DISTRICT_ID
      );
      traineeCreatedByTest = true;

      jdbc.update(
          "INSERT INTO skill_categories (skill_category_id, category_code, category_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, 1, NOW(), NOW())",
          FIXTURE_SKILL_CATEGORY_ID, FIXTURE_SKILL_CAT_CODE, FIXTURE_SKILL_CAT_NAME
      );
      skillCategoryCreatedByTest = true;

      jdbc.update(
          "INSERT INTO skills (skill_id, skill_code, skill_name, skill_category_id, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, 1, NOW(), NOW())",
          FIXTURE_SKILL_ID, FIXTURE_SKILL_CODE, FIXTURE_SKILL_NAME, FIXTURE_SKILL_CATEGORY_ID
      );
      skillCreatedByTest = true;
    } catch (Exception e) {
      cleanUpDomainFixture();
      throw e;
    }
  }

  @Test
  @DisplayName("1. Catalog Isolation Guard confirms connection strictly targets SIH26135_TEST")
  void testCatalogIsolationGuard() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      String catalog = connection.getCatalog();
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      assertThat(catalog).isNotEqualToIgnoringCase(MysqlConnectionTest.FORBIDDEN_PRODUCTION_CATALOG);
    }
  }

  @Test
  @DisplayName("2. Multi-step workflow persists atomically when all steps succeed")
  void testMultiStepWorkflow_allStepsSucceed_commitsBothRecords() {
    long countBefore = refSkillGapStatusRepository.count();

    // Act
    orchestrator.executeMultiStepWorkflow(CODE_PASS_1, CODE_PASS_2, false);

    // Assert: both records were committed to the database
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_PASS_1)).isPresent();
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_PASS_2)).isPresent();

    long countAfter = refSkillGapStatusRepository.count();
    assertThat(countAfter).isEqualTo(countBefore + 2);
  }

  @Test
  @DisplayName("3. Intermediate failure in step 2 triggers full transaction rollback, rolling back inserted rows with unchanged table counts")
  void testMultiStepWorkflow_step2Fails_rollsBackPrecedingModifications() {
    long countBefore = refSkillGapStatusRepository.count();

    // Initial state: neither record exists
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_1)).isEmpty();
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_2)).isEmpty();

    // Act: step 1 writes CODE_FAIL_1, then step 2 writes CODE_FAIL_2 and throws an exception
    assertThatThrownBy(() -> orchestrator.executeMultiStepWorkflow(CODE_FAIL_1, CODE_FAIL_2, true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Simulated step 2 unhandled failure triggering rollback");

    // Assert: Step 1 write was rolled back by MySQL engine, discarding inserted rows and keeping table count unchanged
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_1))
        .as("Step 1 record must be rolled back by database transaction")
        .isEmpty();
    assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_2))
        .as("Step 2 record must be rolled back by database transaction")
        .isEmpty();

    long countAfter = refSkillGapStatusRepository.count();
    assertThat(countAfter)
        .as("Repository count must be strictly unchanged after rolled back transaction")
        .isEqualTo(countBefore);
  }

  @Test
  @DisplayName("4. Propagation.REQUIRED joins parent transaction and rolls back entire workflow on inner failure")
  void testTransactionPropagationRequired_innerFailureRollsBackOuterTransaction() {
    String outerCode = "TX_PROP_OUTER";
    String innerCode = "TX_PROP_INNER";
    long countBefore = refSkillGapStatusRepository.count();

    try {
      assertThatThrownBy(() -> orchestrator.executeMultiStepWorkflow(outerCode, innerCode, true))
          .isInstanceOf(IllegalStateException.class);

      // Verify that inner failure rolled back the outer transaction
      assertThat(refSkillGapStatusRepository.findByStatusCode(outerCode)).isEmpty();
      assertThat(refSkillGapStatusRepository.findByStatusCode(innerCode)).isEmpty();

      long countAfter = refSkillGapStatusRepository.count();
      assertThat(countAfter)
          .as("Repository count must remain unchanged when inner failure triggers full rollback")
          .isEqualTo(countBefore);
    } finally {
      refSkillGapStatusRepository.findByStatusCode(outerCode).ifPresent(refSkillGapStatusRepository::delete);
      refSkillGapStatusRepository.findByStatusCode(innerCode).ifPresent(refSkillGapStatusRepository::delete);
    }
  }

  @Test
  @DisplayName("5. Transaction Engine Proof: unhandled BadRequestException triggers full MySQL rollback and STATUS_ROLLED_BACK")
  void testTransactionEngineRollback_onBadRequestException_demonstratesSpringAndDatabaseRollbackSemantics() {
    // Note: This test proves that Spring's TransactionInterceptor and the underlying MySQL
    // database engine correctly roll back uncommitted database modifications when a BadRequestException
    // (such as ENROLLMENT_TRAINEE_MISMATCH) is thrown. This demonstrates transaction rollback mechanics
    // for the exception type used across workflows. End-to-end multi-table production workflow rollback
    // against domain entities remains unverified via database fixtures due to the empty baseline in
    // SIH26135_TEST, and is verified via source-level transaction boundary analysis.
    long countBefore = refSkillGapStatusRepository.count();
    AtomicInteger completionStatus = new AtomicInteger(-1);

    try {
      assertThatThrownBy(() -> orchestrator.executeWithBadRequestExceptionAndSynchronization(
          CODE_MISMATCH,
          "Enrollment trainee does not match assessment result trainee",
          "ENROLLMENT_TRAINEE_MISMATCH",
          completionStatus))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Enrollment trainee does not match assessment result trainee")
          .hasFieldOrPropertyWithValue("errorCode", "ENROLLMENT_TRAINEE_MISMATCH");

      // Verify Spring TransactionSynchronization observed STATUS_ROLLED_BACK
      assertThat(completionStatus.get())
          .as("Spring transaction synchronization must receive STATUS_ROLLED_BACK")
          .isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);

      // Verify MySQL database state: preceding write was completely rolled back, discarding the test transaction's inserted row with unchanged table count
      assertThat(refSkillGapStatusRepository.findByStatusCode(CODE_MISMATCH))
          .as("Record inserted prior to BadRequestException must be rolled back by MySQL")
          .isEmpty();

      long countAfter = refSkillGapStatusRepository.count();
      assertThat(countAfter)
          .as("Table row count must remain unchanged after exception rollback")
          .isEqualTo(countBefore);
    } finally {
      refSkillGapStatusRepository.findByStatusCode(CODE_MISMATCH).ifPresent(refSkillGapStatusRepository::delete);
    }
  }

  @Test
  @DisplayName("6. Strict contract audit verifies all 18 exact method signatures across all 6 workflow services declare correct transaction attributes")
  void testAllSixWorkflowServicesAreTransactionalProxiesWithStrictContractAudit() throws Exception {
    for (Map.Entry<Class<?>, WorkflowContract> entry : WORKFLOW_CONTRACTS.entrySet()) {
      Class<?> iface = entry.getKey();
      WorkflowContract contract = entry.getValue();

      // Verify mutating and read-only contract sets are completely disjoint (no duplicate signatures)
      Set<String> intersection = new HashSet<>(contract.mutatingSignatures());
      intersection.retainAll(contract.readOnlySignatures());
      assertThat(intersection)
          .as("Workflow contract for %s must not contain duplicate signatures across mutating and read-only sets", iface.getSimpleName())
          .isEmpty();

      Set<String> expectedSignatures = new TreeSet<>();
      expectedSignatures.addAll(contract.mutatingSignatures());
      expectedSignatures.addAll(contract.readOnlySignatures());

      // Extract exact actual public method signatures declared on the interface
      Method[] ifaceMethods = iface.getMethods();
      Set<String> actualSignatures = Arrays.stream(ifaceMethods)
          .map(WorkflowTransactionRollbackIntegrationTest::formatMethodSignature)
          .collect(Collectors.toCollection(TreeSet::new));

      // Exact set equality: ensures NO missing, NO extra, NO duplicate, and NO unaccounted signatures
      assertThat(actualSignatures)
          .as("Actual public interface method signatures for %s must exactly match the audit contract", iface.getSimpleName())
          .isEqualTo(expectedSignatures);

      Object serviceProxy = getServiceProxyForInterface(iface);
      assertThat(serviceProxy)
          .as("Service bean for %s must be present in Spring context", iface.getSimpleName())
          .isNotNull();

      Class<?> implClass = resolveImplementationClass(serviceProxy);
      Transactional classTx = implClass.getAnnotation(Transactional.class);
      assertThat(classTx)
          .as("Service implementation %s must declare class-level @Transactional", implClass.getSimpleName())
          .isNotNull();
      assertThat(classTx.readOnly())
          .as("Service implementation %s must declare readOnly = true by default", implClass.getSimpleName())
          .isTrue();

      for (Method ifaceMethod : ifaceMethods) {
        String sig = formatMethodSignature(ifaceMethod);
        Method implMethod = implClass.getMethod(ifaceMethod.getName(), ifaceMethod.getParameterTypes());

        if (contract.mutatingSignatures().contains(sig)) {
          Transactional methodTx = implMethod.getAnnotation(Transactional.class);
          assertThat(methodTx)
              .as("Mutating method %s on %s must declare @Transactional", sig, implClass.getSimpleName())
              .isNotNull();
          assertThat(methodTx.readOnly())
              .as("Mutating method %s on %s must have readOnly = false", sig, implClass.getSimpleName())
              .isFalse();
          assertThat(methodTx.propagation())
              .as("Mutating method %s on %s must use Propagation.REQUIRED", sig, implClass.getSimpleName())
              .isEqualTo(Propagation.REQUIRED);
        } else if (contract.readOnlySignatures().contains(sig)) {
          Transactional methodTx = implMethod.getAnnotation(Transactional.class);
          if (methodTx != null) {
            assertThat(methodTx.readOnly())
                .as("Read-only method %s on %s must have readOnly = true", sig, implClass.getSimpleName())
                .isTrue();
          }
        }
      }
    }
  }

  @Test
  @DisplayName("7. Production workflow service entry points fail closed on invalid/missing targets with unchanged row counts across all 17 domain tables")
  void testProductionWorkflowServicesFailClosedWithoutChangingDomainRowCounts() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    // Query baseline row count for each of the 17 domain tables relevant to the six workflows
    Map<String, Integer> countsBefore = new HashMap<>();
    for (String table : WORKFLOW_DOMAIN_TABLES) {
      countsBefore.put(table, jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class));
    }

    // 1. TrainingOutcomeWorkflowService: fail closed on non-existent enrollment
    RecordAssessmentAndIssueCertificateRequest req1 = new RecordAssessmentAndIssueCertificateRequest();
    req1.setEnrollmentId(999999L);
    req1.setCompletionEnrollmentStatusId(2L);
    CreateAssessmentResultRequest assessReq = new CreateAssessmentResultRequest();
    assessReq.setTraineeId(100L);
    assessReq.setMaximumScore(BigDecimal.valueOf(100));
    assessReq.setAssessmentOutcomeId(1L);
    req1.setAssessmentResultRequest(assessReq);
    assertThatThrownBy(() -> trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(req1))
        .isInstanceOf(ResourceNotFoundException.class);

    // 2. PlacementEmploymentWorkflowService: fail closed on non-existent placement
    PlacementToEmploymentTransitionRequest req2 = new PlacementToEmploymentTransitionRequest();
    req2.setPlacementId(999999L);
    req2.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());
    assertThatThrownBy(() -> placementEmploymentWorkflowService.transitionPlacementToEmployment(req2))
        .isInstanceOf(ResourceNotFoundException.class);

    // 3. EmploymentLifecycleWorkflowService: fail closed on non-existent employment & unemployment records
    EmploymentExitAndUnemploymentRequest req3a = new EmploymentExitAndUnemploymentRequest();
    CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
    exitReq.setEmploymentId(999999L);
    req3a.setExitEventRequest(exitReq);
    assertThatThrownBy(() -> employmentLifecycleWorkflowService.recordExitAndInitiateUnemployment(req3a))
        .isInstanceOf(ResourceNotFoundException.class);

    UnemploymentToEmploymentTransitionRequest req3b = new UnemploymentToEmploymentTransitionRequest();
    req3b.setUnemploymentEventId(999999L);
    req3b.setNewEmploymentRequest(new CreateEmploymentRecordRequest());
    assertThatThrownBy(() -> employmentLifecycleWorkflowService.transitionUnemploymentToEmployment(req3b))
        .isInstanceOf(ResourceNotFoundException.class);

    // 4. VerificationWorkflowService: fail closed on non-existent survey response
    InitiateVerificationFromSurveyRequest req4 = new InitiateVerificationFromSurveyRequest();
    req4.setSurveyResponseId(999999L);
    req4.setVerificationRequestDetails(new CreateEmploymentVerificationRequestRequest());
    assertThatThrownBy(() -> verificationWorkflowService.initiateVerificationFromSurvey(req4))
        .isInstanceOf(ResourceNotFoundException.class);

    // 5. SkillGapRemediationWorkflowService: fail closed on unauthenticated access
    TraineeRecommendationDecisionRequest req5a = new TraineeRecommendationDecisionRequest();
    assertThatThrownBy(() -> skillGapRemediationWorkflowService.acceptRecommendation(999999L, req5a))
        .isInstanceOf(InvalidCredentialsException.class);

    SkillGapRemediationEnrollmentRequest req5b = new SkillGapRemediationEnrollmentRequest();
    req5b.setRecommendationId(999999L);
    req5b.setEnrollmentRequest(new CreateTrainingEnrollmentRequest());
    assertThatThrownBy(() -> skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(req5b))
        .isInstanceOf(InvalidCredentialsException.class);

    // 6. FollowupEngagementWorkflowService: fail closed on non-existent task
    RecordFollowupSurveyResponseRequest req6a = new RecordFollowupSurveyResponseRequest();
    req6a.setFollowupTaskId(999999L);
    req6a.setSurveyResponseRequest(new CreateSurveyResponseRequest());
    assertThatThrownBy(() -> followupEngagementWorkflowService.recordFollowupSurveyResponse(req6a))
        .isInstanceOf(ResourceNotFoundException.class);

    RecordEngagementAttemptRequest req6b = new RecordEngagementAttemptRequest();
    req6b.setFollowupTaskId(999999L);
    req6b.setCommunicationLogRequest(new CreateCommunicationLogRequest());
    assertThatThrownBy(() -> followupEngagementWorkflowService.recordEngagementAttempt(req6b))
        .isInstanceOf(ResourceNotFoundException.class);

    // Query row count for each of the 17 domain tables after fail-closed executions
    Map<String, Integer> countsAfter = new HashMap<>();
    for (String table : WORKFLOW_DOMAIN_TABLES) {
      countsAfter.put(table, jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class));
    }

    // Verify row counts across all 17 participating domain tables remained unchanged during the tested fail-closed calls.
    // Note: Equal row counts confirm net table population was unaffected, but do not prove that existing rows
    // were not updated or that a deletion was not balanced by an insertion. We do not claim full state integrity
    // or orphan absence from count equality alone.
    assertThat(countsAfter)
        .as("Row counts across all 17 participating domain tables must remain strictly identical before and after fail-closed workflow executions")
        .isEqualTo(countsBefore);
  }

  @Test
  @DisplayName("8. Real production workflow bean rolls back multi-step business-domain modifications when subsequent step fails")
  void testProductionWorkflowMultiStepRollback_realDomainEntities_rollsBackPrecedingWritesOnFailure() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    insertMinimalDomainFixture();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("super_admin_user", "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_super_admin")))
    );

    String assessmentNum = "TX-SGA-ROLLBACK-001";
    String gapNum1 = "TX-GAP-ROLLBACK-001";
    String gapNum2 = "TX-GAP-ROLLBACK-002";

    createdAssessmentNumbers.add(assessmentNum);
    createdSkillGapNumbers.add(gapNum1);
    createdSkillGapNumbers.add(gapNum2);

    int sgaCountBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gap_assessments", Integer.class);
    int sgCountBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gaps", Integer.class);

    RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();

    CreateSkillGapAssessmentRequest assessReq = new CreateSkillGapAssessmentRequest();
    assessReq.setAssessmentNumber(assessmentNum);
    assessReq.setTraineeId(FIXTURE_TRAINEE_ID);
    assessReq.setSkillGapSourceId(1L); // ASSESSMENT
    assessReq.setSkillGapAssessmentStatusId(1L); // DRAFT
    assessReq.setAssessedOn(LocalDate.now());
    request.setAssessmentRequest(assessReq);

    // Step 2a: Valid skill gap that is written to database during transaction
    CreateSkillGapRequest gap1 = new CreateSkillGapRequest();
    gap1.setSkillGapNumber(gapNum1);
    gap1.setTraineeId(FIXTURE_TRAINEE_ID);
    gap1.setSkillId(FIXTURE_SKILL_ID);
    gap1.setRequiredSkillLevelId(3L); // INTERMEDIATE
    gap1.setObservedSkillLevelId(1L); // AWARENESS
    gap1.setSkillGapSeverityId(2L); // LOW
    gap1.setSkillGapStatusId(1L); // OPEN
    gap1.setSkillGapSourceId(1L); // ASSESSMENT
    gap1.setIdentifiedOn(LocalDate.now());

    // Step 2b: Second skill gap with mismatched trainee ID to trigger controlled failure
    CreateSkillGapRequest gap2 = new CreateSkillGapRequest();
    gap2.setSkillGapNumber(gapNum2);
    gap2.setTraineeId(888888L); // Mismatched trainee ID triggers SKILL_GAP_TRAINEE_MISMATCH after gap1 write
    gap2.setSkillId(FIXTURE_SKILL_ID);
    gap2.setRequiredSkillLevelId(3L);
    gap2.setSkillGapSeverityId(2L);
    gap2.setSkillGapStatusId(1L);
    gap2.setSkillGapSourceId(1L);
    gap2.setIdentifiedOn(LocalDate.now());

    request.setGapRequests(List.of(gap1, gap2));

    // Act: Invoke the genuine Spring transaction proxy of SkillGapRemediationWorkflowService
    assertThatThrownBy(() -> skillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Skill gap trainee does not match assessment trainee")
        .hasFieldOrPropertyWithValue("errorCode", "SKILL_GAP_TRAINEE_MISMATCH");

    // Assert: MySQL InnoDB transaction rolled back preceding domain writes
    int sgaCountAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gap_assessments", Integer.class);
    int sgCountAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gaps", Integer.class);

    assertThat(sgaCountAfter)
        .as("skill_gap_assessments table row count must remain unchanged after transaction rollback")
        .isEqualTo(sgaCountBefore);
    assertThat(sgCountAfter)
        .as("skill_gaps table row count must remain unchanged after transaction rollback")
        .isEqualTo(sgCountBefore);

    // Assert no partial state remains in MySQL
    int sgaMatch = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE assessment_number = ?", Integer.class, assessmentNum);
    int sgMatch = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gaps WHERE skill_gap_number = ?", Integer.class, gapNum1);

    assertThat(sgaMatch).as("Assessment record written in step 1 must be discarded by MySQL rollback").isZero();
    assertThat(sgMatch).as("Skill gap record written in step 2 must be discarded by MySQL rollback").isZero();
  }

  @Test
  @DisplayName("9. Real production workflow bean commits multi-step business-domain modifications when all steps succeed")
  void testProductionWorkflowMultiStepCommit_realDomainEntities_commitsAtomically() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    insertMinimalDomainFixture();

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("super_admin_user", "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_super_admin")))
    );

    String assessmentNum = "TX-SGA-COMMIT-001";
    String gapNum = "TX-GAP-COMMIT-001";

    createdAssessmentNumbers.add(assessmentNum);
    createdSkillGapNumbers.add(gapNum);

    int sgaCountBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gap_assessments", Integer.class);
    int sgCountBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gaps", Integer.class);

    RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();

    CreateSkillGapAssessmentRequest assessReq = new CreateSkillGapAssessmentRequest();
    assessReq.setAssessmentNumber(assessmentNum);
    assessReq.setTraineeId(FIXTURE_TRAINEE_ID);
    assessReq.setSkillGapSourceId(1L); // ASSESSMENT
    assessReq.setSkillGapAssessmentStatusId(1L); // DRAFT
    assessReq.setAssessedOn(LocalDate.now());
    request.setAssessmentRequest(assessReq);

    CreateSkillGapRequest gap = new CreateSkillGapRequest();
    gap.setSkillGapNumber(gapNum);
    gap.setTraineeId(FIXTURE_TRAINEE_ID);
    gap.setSkillId(FIXTURE_SKILL_ID);
    gap.setRequiredSkillLevelId(3L); // INTERMEDIATE
    gap.setObservedSkillLevelId(1L); // AWARENESS
    gap.setSkillGapSeverityId(2L); // LOW
    gap.setSkillGapStatusId(1L); // OPEN
    gap.setSkillGapSourceId(1L); // ASSESSMENT
    gap.setIdentifiedOn(LocalDate.now());

    request.setGapRequests(List.of(gap));

    // Act: Invoke the genuine Spring transaction proxy of SkillGapRemediationWorkflowService
    var response = skillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations(request);

    // Assert: Both domain writes were committed to MySQL
    assertThat(response).isNotNull();
    assertThat(response.assessment()).isNotNull();
    assertThat(response.assessment().assessmentNumber()).isEqualTo(assessmentNum);
    assertThat(response.skillGaps()).hasSize(1);
    assertThat(response.skillGaps().get(0).skillGapNumber()).isEqualTo(gapNum);

    int sgaCountAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gap_assessments", Integer.class);
    int sgCountAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM skill_gaps", Integer.class);

    assertThat(sgaCountAfter)
        .as("skill_gap_assessments row count must increase by 1 upon successful commit")
        .isEqualTo(sgaCountBefore + 1);
    assertThat(sgCountAfter)
        .as("skill_gaps row count must increase by 1 upon successful commit")
        .isEqualTo(sgCountBefore + 1);

    int sgaMatch = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gap_assessments WHERE assessment_number = ?", Integer.class, assessmentNum);
    int sgMatch = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM skill_gaps WHERE skill_gap_number = ?", Integer.class, gapNum);

    assertThat(sgaMatch).as("Assessment record must exist in MySQL").isEqualTo(1);
    assertThat(sgMatch).as("Skill gap record must exist in MySQL").isEqualTo(1);
  }

  @Test
  @DisplayName("10. Domain fixture setup detects pre-existing records across fixture tables and unique keys, fails safely, and preserves pre-existing records")
  void testDomainFixtureSetup_onCollision_failsSafelyWithoutDeletingPreExistingRecords() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    // Scenario 1: Pre-existing state colliding on state_id
    verifyStateCollision(jdbc, FIXTURE_STATE_ID, "PX", "Pre-existing State 9991", "state record already exists with ID " + FIXTURE_STATE_ID);

    // Scenario 2: Pre-existing state colliding on state_code
    verifyStateCollision(jdbc, 9993L, FIXTURE_STATE_CODE, "Pre-existing State TS", "or code '" + FIXTURE_STATE_CODE + "'");

    // Scenario 3: Pre-existing state colliding on state_name
    verifyStateCollision(jdbc, 9993L, "PZ", FIXTURE_STATE_NAME, "or name '" + FIXTURE_STATE_NAME + "'");

    // Scenario 4: Pre-existing skill category colliding on skill_category_id
    verifySkillCategoryCollision(jdbc, FIXTURE_SKILL_CATEGORY_ID, "CAT-PX", "Pre-existing Cat 9991", "skill category record already exists with ID " + FIXTURE_SKILL_CATEGORY_ID);

    // Scenario 5: Pre-existing skill category colliding on category_code
    verifySkillCategoryCollision(jdbc, 9993L, FIXTURE_SKILL_CAT_CODE, "Pre-existing Cat Code", "or code '" + FIXTURE_SKILL_CAT_CODE + "'");

    // Scenario 6: Pre-existing skill category colliding on category_name
    verifySkillCategoryCollision(jdbc, 9993L, "CAT-PZ", FIXTURE_SKILL_CAT_NAME, "or name '" + FIXTURE_SKILL_CAT_NAME + "'");

    // Scenario 7: Pre-existing district colliding on district_id
    verifyDistrictCollision(jdbc, FIXTURE_DISTRICT_ID, "DPX", "Pre-existing District 9991", "district record already exists with ID " + FIXTURE_DISTRICT_ID);

    // Scenario 8: Pre-existing district colliding on district_code
    verifyDistrictCollision(jdbc, 9993L, FIXTURE_DISTRICT_CODE, "Pre-existing District Code", "or code '" + FIXTURE_DISTRICT_CODE + "'");

    // Scenario 9: Pre-existing district colliding on district_name
    verifyDistrictCollision(jdbc, 9993L, "DPZ", FIXTURE_DISTRICT_NAME, "or name '" + FIXTURE_DISTRICT_NAME + "'");

    // Scenario 10: Pre-existing skill colliding on skill_id
    verifySkillCollision(jdbc, FIXTURE_SKILL_ID, "SPX", "Pre-existing Skill 9991", "skill record already exists with ID " + FIXTURE_SKILL_ID);

    // Scenario 11: Pre-existing skill colliding on skill_code
    verifySkillCollision(jdbc, 9993L, FIXTURE_SKILL_CODE, "Pre-existing Skill Code", "or code '" + FIXTURE_SKILL_CODE + "'");

    // Scenario 12: Pre-existing skill colliding on skill_name
    verifySkillCollision(jdbc, 9993L, "SPZ", FIXTURE_SKILL_NAME, "or name '" + FIXTURE_SKILL_NAME + "'");

    // Scenario 13: Pre-existing trainee colliding on trainee_id
    verifyTraineeCollision(jdbc, FIXTURE_TRAINEE_ID, "REG-PX", "trainee record already exists with ID " + FIXTURE_TRAINEE_ID);

    // Scenario 14: Pre-existing trainee colliding on registration_number
    verifyTraineeCollision(jdbc, 9993L, FIXTURE_TRAINEE_REG, "or registration '" + FIXTURE_TRAINEE_REG + "'");

    // Scenario 15: Pre-existing assessment colliding on assessment_number 'TX-SGA-ROLLBACK-001'
    verifyAssessmentCollision(jdbc, "TX-SGA-ROLLBACK-001");

    // Scenario 16: Pre-existing assessment colliding on assessment_number 'TX-SGA-COMMIT-001'
    verifyAssessmentCollision(jdbc, "TX-SGA-COMMIT-001");

    // Scenario 17: Pre-existing skill gap colliding on skill_gap_number 'TX-GAP-ROLLBACK-001'
    verifySkillGapCollision(jdbc, "TX-GAP-ROLLBACK-001");

    // Scenario 18: Pre-existing skill gap colliding on skill_gap_number 'TX-GAP-ROLLBACK-002'
    verifySkillGapCollision(jdbc, "TX-GAP-ROLLBACK-002");

    // Scenario 19: Pre-existing skill gap colliding on skill_gap_number 'TX-GAP-COMMIT-001'
    verifySkillGapCollision(jdbc, "TX-GAP-COMMIT-001");
  }

  private void verifyStateCollision(JdbcTemplate jdbc, Long stateId, String stateCode, String stateName, String expectedMessage) {
    jdbc.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 1, NOW(), NOW())",
        stateId, stateCode, stateName
    );
    try {
      assertThatThrownBy(this::insertMinimalDomainFixture)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(expectedMessage);

      cleanUpDomainFixture();

      var rows = jdbc.queryForList("SELECT state_id, state_code, state_name FROM states WHERE state_id = ?", stateId);
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("state_code")).isEqualTo(stateCode);
      assertThat(rows.get(0).get("state_name")).isEqualTo(stateName);
    } finally {
      jdbc.update("DELETE FROM states WHERE state_id = ?", stateId);
    }
  }

  private void verifySkillCategoryCollision(JdbcTemplate jdbc, Long catId, String catCode, String catName, String expectedMessage) {
    jdbc.update(
        "INSERT INTO skill_categories (skill_category_id, category_code, category_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 1, NOW(), NOW())",
        catId, catCode, catName
    );
    try {
      assertThatThrownBy(this::insertMinimalDomainFixture)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(expectedMessage);

      cleanUpDomainFixture();

      var rows = jdbc.queryForList("SELECT skill_category_id, category_code, category_name FROM skill_categories WHERE skill_category_id = ?", catId);
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).get("category_code")).isEqualTo(catCode);
      assertThat(rows.get(0).get("category_name")).isEqualTo(catName);
    } finally {
      jdbc.update("DELETE FROM skill_categories WHERE skill_category_id = ?", catId);
    }
  }

  private void verifyDistrictCollision(JdbcTemplate jdbc, Long districtId, String districtCode, String districtName, String expectedMessage) {
    Long parentStateId = 9998L;
    jdbc.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P8', 'Pre Parent State 9998', 1, NOW(), NOW())",
        parentStateId
    );
    try {
      jdbc.update(
          "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, 1, NOW(), NOW())",
          districtId, parentStateId, districtCode, districtName
      );
      try {
        assertThatThrownBy(this::insertMinimalDomainFixture)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(expectedMessage);

        cleanUpDomainFixture();

        var rows = jdbc.queryForList("SELECT district_id, district_code, district_name FROM districts WHERE district_id = ?", districtId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("district_code")).isEqualTo(districtCode);
        assertThat(rows.get(0).get("district_name")).isEqualTo(districtName);
      } finally {
        jdbc.update("DELETE FROM districts WHERE district_id = ?", districtId);
      }
    } finally {
      jdbc.update("DELETE FROM states WHERE state_id = ?", parentStateId);
    }
  }

  private void verifySkillCollision(JdbcTemplate jdbc, Long skillId, String skillCode, String skillName, String expectedMessage) {
    Long parentCatId = 9998L;
    jdbc.update(
        "INSERT INTO skill_categories (skill_category_id, category_code, category_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'CAT-P8', 'Pre Parent Cat 9998', 1, NOW(), NOW())",
        parentCatId
    );
    try {
      jdbc.update(
          "INSERT INTO skills (skill_id, skill_code, skill_name, skill_category_id, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, ?, ?, 1, NOW(), NOW())",
          skillId, skillCode, skillName, parentCatId
      );
      try {
        assertThatThrownBy(this::insertMinimalDomainFixture)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(expectedMessage);

        cleanUpDomainFixture();

        var rows = jdbc.queryForList("SELECT skill_id, skill_code, skill_name FROM skills WHERE skill_id = ?", skillId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("skill_code")).isEqualTo(skillCode);
        assertThat(rows.get(0).get("skill_name")).isEqualTo(skillName);
      } finally {
        jdbc.update("DELETE FROM skills WHERE skill_id = ?", skillId);
      }
    } finally {
      jdbc.update("DELETE FROM skill_categories WHERE skill_category_id = ?", parentCatId);
    }
  }

  private void verifyTraineeCollision(JdbcTemplate jdbc, Long traineeId, String regNumber, String expectedMessage) {
    Long parentStateId = 9998L;
    Long parentDistrictId = 9998L;
    jdbc.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P8', 'Pre Parent State 9998', 1, NOW(), NOW())",
        parentStateId
    );
    try {
      jdbc.update(
          "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, 'DP8', 'Pre Parent Dist 9998', 1, NOW(), NOW())",
          parentDistrictId, parentStateId
      );
      try {
        jdbc.update(
            "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, "
                + "current_employment_status_id, profile_status_id, created_at, updated_at) "
                + "VALUES (?, ?, 'Pre', 'Trainee', 1, ?, ?, 1, 3, NOW(), NOW())",
            traineeId, regNumber, parentStateId, parentDistrictId
        );
        try {
          assertThatThrownBy(this::insertMinimalDomainFixture)
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining(expectedMessage);

          cleanUpDomainFixture();

          var rows = jdbc.queryForList("SELECT trainee_id, registration_number FROM trainees WHERE trainee_id = ?", traineeId);
          assertThat(rows).hasSize(1);
          assertThat(rows.get(0).get("registration_number")).isEqualTo(regNumber);
        } finally {
          jdbc.update("DELETE FROM trainees WHERE trainee_id = ?", traineeId);
        }
      } finally {
        jdbc.update("DELETE FROM districts WHERE district_id = ?", parentDistrictId);
      }
    } finally {
      jdbc.update("DELETE FROM states WHERE state_id = ?", parentStateId);
    }
  }

  private void verifyAssessmentCollision(JdbcTemplate jdbc, String assessmentNumber) {
    Long parentStateId = 9998L;
    Long parentDistrictId = 9998L;
    Long parentTraineeId = 9998L;

    jdbc.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P8', 'Pre Parent State 9998', 1, NOW(), NOW())",
        parentStateId
    );
    try {
      jdbc.update(
          "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, 'DP8', 'Pre Parent Dist 9998', 1, NOW(), NOW())",
          parentDistrictId, parentStateId
      );
      try {
        jdbc.update(
            "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, "
                + "current_employment_status_id, profile_status_id, created_at, updated_at) "
                + "VALUES (?, 'REG-P8', 'Pre', 'Trainee', 1, ?, ?, 1, 3, NOW(), NOW())",
            parentTraineeId, parentStateId, parentDistrictId
        );
        try {
          jdbc.update(
              "INSERT INTO skill_gap_assessments (assessment_number, trainee_id, skill_gap_source_id, skill_gap_assessment_status_id, assessed_on, created_at, updated_at) "
                  + "VALUES (?, ?, 1, 1, CURDATE(), NOW(), NOW())",
              assessmentNumber, parentTraineeId
          );
          try {
            assertThatThrownBy(this::insertMinimalDomainFixture)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assessment record already exists with test assessment numbers");

            cleanUpDomainFixture();

            var rows = jdbc.queryForList(
                "SELECT assessment_number, trainee_id FROM skill_gap_assessments WHERE assessment_number = ?", assessmentNumber);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("assessment_number")).isEqualTo(assessmentNumber);
          } finally {
            jdbc.update("DELETE FROM skill_gap_assessments WHERE assessment_number = ?", assessmentNumber);
          }
        } finally {
          jdbc.update("DELETE FROM trainees WHERE trainee_id = ?", parentTraineeId);
        }
      } finally {
        jdbc.update("DELETE FROM districts WHERE district_id = ?", parentDistrictId);
      }
    } finally {
      jdbc.update("DELETE FROM states WHERE state_id = ?", parentStateId);
    }
  }

  private void verifySkillGapCollision(JdbcTemplate jdbc, String skillGapNumber) {
    Long parentStateId = 9998L;
    Long parentDistrictId = 9998L;
    Long parentTraineeId = 9998L;
    Long parentCatId = 9998L;
    Long parentSkillId = 9998L;
    String parentAssessNumber = "TX-SGA-PRE-9998";

    jdbc.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'P8', 'Pre Parent State 9998', 1, NOW(), NOW())",
        parentStateId
    );
    try {
      jdbc.update(
          "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
              + "VALUES (?, ?, 'DP8', 'Pre Parent Dist 9998', 1, NOW(), NOW())",
          parentDistrictId, parentStateId
      );
      try {
        jdbc.update(
            "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, "
                + "current_employment_status_id, profile_status_id, created_at, updated_at) "
                + "VALUES (?, 'REG-P8', 'Pre', 'Trainee', 1, ?, ?, 1, 3, NOW(), NOW())",
            parentTraineeId, parentStateId, parentDistrictId
        );
        try {
          jdbc.update(
              "INSERT INTO skill_categories (skill_category_id, category_code, category_name, lifecycle_status_id, created_at, updated_at) "
                  + "VALUES (?, 'CAT-P8', 'Pre Parent Cat 9998', 1, NOW(), NOW())",
              parentCatId
          );
          try {
            jdbc.update(
                "INSERT INTO skills (skill_id, skill_code, skill_name, skill_category_id, lifecycle_status_id, created_at, updated_at) "
                    + "VALUES (?, 'SKL-P8', 'Pre Skill P8', ?, 1, NOW(), NOW())",
                parentSkillId, parentCatId
            );
            try {
              jdbc.update(
                  "INSERT INTO skill_gap_assessments (assessment_number, trainee_id, skill_gap_source_id, skill_gap_assessment_status_id, assessed_on, created_at, updated_at) "
                      + "VALUES (?, ?, 1, 1, CURDATE(), NOW(), NOW())",
                  parentAssessNumber, parentTraineeId
              );
              Long sgaId = jdbc.queryForObject(
                  "SELECT skill_gap_assessment_id FROM skill_gap_assessments WHERE assessment_number = ?", Long.class, parentAssessNumber);
              try {
                jdbc.update(
                    "INSERT INTO skill_gaps (skill_gap_number, skill_gap_assessment_id, trainee_id, skill_id, required_skill_level_id, observed_skill_level_id, skill_gap_severity_id, skill_gap_status_id, skill_gap_source_id, is_current, identified_on, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, 3, 1, 2, 1, 1, 1, CURDATE(), NOW(), NOW())",
                    skillGapNumber, sgaId, parentTraineeId, parentSkillId
                );
                try {
                  assertThatThrownBy(this::insertMinimalDomainFixture)
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("skill gap record already exists with test skill gap numbers");

                  cleanUpDomainFixture();

                  var rows = jdbc.queryForList(
                      "SELECT skill_gap_number, trainee_id, skill_id FROM skill_gaps WHERE skill_gap_number = ?", skillGapNumber);
                  assertThat(rows).hasSize(1);
                  assertThat(rows.get(0).get("skill_gap_number")).isEqualTo(skillGapNumber);
                } finally {
                  jdbc.update("DELETE FROM skill_gaps WHERE skill_gap_number = ?", skillGapNumber);
                }
              } finally {
                jdbc.update("DELETE FROM skill_gap_assessments WHERE assessment_number = ?", parentAssessNumber);
              }
            } finally {
              jdbc.update("DELETE FROM skills WHERE skill_id = ?", parentSkillId);
            }
          } finally {
            jdbc.update("DELETE FROM skill_categories WHERE skill_category_id = ?", parentCatId);
          }
        } finally {
          jdbc.update("DELETE FROM trainees WHERE trainee_id = ?", parentTraineeId);
        }
      } finally {
        jdbc.update("DELETE FROM districts WHERE district_id = ?", parentDistrictId);
      }
    } finally {
      jdbc.update("DELETE FROM states WHERE state_id = ?", parentStateId);
    }
  }

  private Object getServiceProxyForInterface(Class<?> iface) {
    if (iface == TrainingOutcomeWorkflowService.class) return trainingOutcomeWorkflowService;
    if (iface == PlacementEmploymentWorkflowService.class) return placementEmploymentWorkflowService;
    if (iface == EmploymentLifecycleWorkflowService.class) return employmentLifecycleWorkflowService;
    if (iface == VerificationWorkflowService.class) return verificationWorkflowService;
    if (iface == SkillGapRemediationWorkflowService.class) return skillGapRemediationWorkflowService;
    if (iface == FollowupEngagementWorkflowService.class) return followupEngagementWorkflowService;
    throw new IllegalArgumentException("Unknown interface: " + iface.getName());
  }

  private Class<?> resolveImplementationClass(Object proxy) {
    if (org.springframework.aop.support.AopUtils.isAopProxy(proxy)) {
      return org.springframework.aop.support.AopUtils.getTargetClass(proxy);
    }
    return proxy.getClass();
  }

  // ── Test-specific Spring Beans demonstrating transaction rollback ──────────

  @TestConfiguration
  static class TestWorkflowConfig {
    @Bean
    public TestWorkflowOrchestrator testWorkflowOrchestrator(
        RefSkillGapStatusRepository repository,
        TestWorkflowStep2Service step2Service) {
      return new TestWorkflowOrchestrator(repository, step2Service);
    }

    @Bean
    public TestWorkflowStep2Service testWorkflowStep2Service(RefSkillGapStatusRepository repository) {
      return new TestWorkflowStep2Service(repository);
    }
  }

  @Service
  static class TestWorkflowOrchestrator {
    private final RefSkillGapStatusRepository repository;
    private final TestWorkflowStep2Service step2Service;

    public TestWorkflowOrchestrator(RefSkillGapStatusRepository repository, TestWorkflowStep2Service step2Service) {
      this.repository = repository;
      this.step2Service = step2Service;
    }

    @Transactional
    public void executeMultiStepWorkflow(String code1, String code2, boolean failInStep2) {
      RefSkillGapStatus s1 = new RefSkillGapStatus(code1, "Status " + code1, 991);
      s1.setCreatedAt(LocalDateTime.now());
      s1.setUpdatedAt(LocalDateTime.now());
      repository.save(s1);

      step2Service.executeStep2(code2, failInStep2);
    }

    @Transactional
    public void executeWithBadRequestExceptionAndSynchronization(
        String code,
        String errorMessage,
        String errorCode,
        AtomicInteger completionStatus) {
      RefSkillGapStatus s = new RefSkillGapStatus(code, "Status " + code, 995);
      s.setCreatedAt(LocalDateTime.now());
      s.setUpdatedAt(LocalDateTime.now());
      repository.save(s);

      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                completionStatus.set(status);
              }
            });
      }

      throw new BadRequestException(errorMessage, errorCode);
    }
  }

  @Service
  static class TestWorkflowStep2Service {
    private final RefSkillGapStatusRepository repository;

    public TestWorkflowStep2Service(RefSkillGapStatusRepository repository) {
      this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void executeStep2(String code2, boolean fail) {
      RefSkillGapStatus s2 = new RefSkillGapStatus(code2, "Status " + code2, 992);
      s2.setCreatedAt(LocalDateTime.now());
      s2.setUpdatedAt(LocalDateTime.now());
      repository.save(s2);

      if (fail) {
        throw new IllegalStateException("Simulated step 2 unhandled failure triggering rollback");
      }
    }
  }
}
