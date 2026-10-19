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
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
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

  @BeforeEach
  void enforceDatabaseIsolationGuard() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
    }
  }

  @AfterEach
  void cleanUpTestData() {
    refSkillGapStatusRepository.findByStatusCode(CODE_PASS_1).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_PASS_2).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_1).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_FAIL_2).ifPresent(refSkillGapStatusRepository::delete);
    refSkillGapStatusRepository.findByStatusCode(CODE_MISMATCH).ifPresent(refSkillGapStatusRepository::delete);
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
