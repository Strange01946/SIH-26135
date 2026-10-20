package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
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
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.mapper.SkillGapRecommendationMapper;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRecommendationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TraineeUnemploymentEventRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.CommunicationLogService;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.EmploymentLifecycleWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.FollowupEngagementWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementEmploymentWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.SkillGapRemediationWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingOutcomeWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.VerificationWorkflowServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Phase 25.11 Regression Suite: Mandatory WorkflowSecurityScopeValidator Wiring.
 *
 * <p>Verifies fail-closed behavior across all six workflow services when constructed
 * without a WorkflowSecurityScopeValidator or when validator wiring is missing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Workflow Service Mandatory Validator Wiring Regression Tests")
class WorkflowServiceMandatoryWiringTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // =========================================================================
  // 1. PlacementEmploymentWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("1. PlacementEmploymentWorkflowService Wiring")
  class PlacementEmploymentWiringTests {

    @Mock private PlacementRecordService placementRecordService;
    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private EmploymentRecordRepository employmentRecordRepository;
    @Mock private RefPlacementStatusRepository refPlacementStatusRepository;
    @Mock private RefJoiningStatusRepository refJoiningStatusRepository;

    @Test
    @DisplayName("transitionPlacementToEmployment fails closed when validator is not wired")
    void shouldFailClosedWithoutValidatorOnTransition() {
      PlacementEmploymentWorkflowServiceImpl service = new PlacementEmploymentWorkflowServiceImpl(
          placementRecordService, employmentRecordService, salaryHistoryService,
          employmentRecordRepository, refPlacementStatusRepository, refJoiningStatusRepository,
          null // no validator
      );

      PlacementRecordResponse placement = Mockito.mock(PlacementRecordResponse.class);
      Mockito.when(placementRecordService.getPlacementRecordById(1L)).thenReturn(placement);

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(1L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> service.transitionPlacementToEmployment(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getTraineePlacementEmploymentSummary fails closed when validator is not wired")
    void shouldFailClosedWithoutValidatorOnSummary() {
      PlacementEmploymentWorkflowServiceImpl service = new PlacementEmploymentWorkflowServiceImpl(
          placementRecordService, employmentRecordService, salaryHistoryService,
          employmentRecordRepository, refPlacementStatusRepository, refJoiningStatusRepository,
          null
      );

      assertThatThrownBy(() -> service.getTraineePlacementEmploymentSummary(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  // =========================================================================
  // 2. TrainingOutcomeWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("2. TrainingOutcomeWorkflowService Wiring")
  class TrainingOutcomeWiringTests {

    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private TraineeAssessmentService traineeAssessmentService;
    @Mock private AssessmentResultService assessmentResultService;
    @Mock private CertificationService certificationService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;

    @Test
    @DisplayName("recordAssessmentResultAndIssueCertificate fails closed when validator is not wired")
    void shouldFailClosedWithoutValidatorOnOutcome() {
      TrainingOutcomeWorkflowServiceImpl service = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService, traineeAssessmentService, assessmentResultService,
          certificationService, courseSkillRepository, skillGapRepository,
          refSkillGapStatusRepository,
          null // no validator
      );

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setAssessmentResultRequest(new CreateAssessmentResultRequest());

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getTraineeTrainingOutcomeSummary fails closed when validator is not wired")
    void shouldFailClosedWithoutValidatorOnOutcomeSummary() {
      TrainingOutcomeWorkflowServiceImpl service = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService, traineeAssessmentService, assessmentResultService,
          certificationService, courseSkillRepository, skillGapRepository,
          refSkillGapStatusRepository,
          null
      );

      assertThatThrownBy(() -> service.getTraineeTrainingOutcomeSummary(10L, 20L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  // =========================================================================
  // 3. SkillGapRemediationWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("3. SkillGapRemediationWorkflowService Wiring")
  class SkillGapRemediationWiringTests {

    @Mock private SkillGapAssessmentService skillGapAssessmentService;
    @Mock private SkillGapService skillGapService;
    @Mock private SkillGapRecommendationService skillGapRecommendationService;
    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private SkillGapRecommendationRepository skillGapRecommendationRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;
    @Mock private TraineeRepository traineeRepository;

    @Test
    @DisplayName("recordAssessmentWithGapsAndRecommendations fails closed without validator")
    void shouldFailClosedWithoutValidatorOnRecordAssessment() {
      SkillGapRemediationWorkflowServiceImpl service = new SkillGapRemediationWorkflowServiceImpl(
          skillGapAssessmentService, skillGapService, skillGapRecommendationService,
          trainingEnrollmentService, courseSkillRepository, skillGapRepository,
          skillGapRecommendationRepository, refSkillGapStatusRepository,
          traineeRepository, new SkillGapRecommendationMapper(),
          null // no validator
      );

      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());
      request.setGapRequests(List.of(new CreateSkillGapRequest()));

      assertThatThrownBy(() -> service.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("acceptRecommendation fails closed without validator")
    void shouldFailClosedWithoutValidatorOnAccept() {
      SkillGapRemediationWorkflowServiceImpl service = new SkillGapRemediationWorkflowServiceImpl(
          skillGapAssessmentService, skillGapService, skillGapRecommendationService,
          trainingEnrollmentService, courseSkillRepository, skillGapRepository,
          skillGapRecommendationRepository, refSkillGapStatusRepository,
          traineeRepository, new SkillGapRecommendationMapper(),
          null
      );

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> service.acceptRecommendation(1L, request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("rejectRecommendation fails closed without validator")
    void shouldFailClosedWithoutValidatorOnReject() {
      SkillGapRemediationWorkflowServiceImpl service = new SkillGapRemediationWorkflowServiceImpl(
          skillGapAssessmentService, skillGapService, skillGapRecommendationService,
          trainingEnrollmentService, courseSkillRepository, skillGapRepository,
          skillGapRecommendationRepository, refSkillGapStatusRepository,
          traineeRepository, new SkillGapRecommendationMapper(),
          null
      );

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(10L, "Remarks", null);
      assertThatThrownBy(() -> service.rejectRecommendation(1L, request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getTraineeSkillGapProfile fails closed without validator")
    void shouldFailClosedWithoutValidatorOnProfile() {
      SkillGapRemediationWorkflowServiceImpl service = new SkillGapRemediationWorkflowServiceImpl(
          skillGapAssessmentService, skillGapService, skillGapRecommendationService,
          trainingEnrollmentService, courseSkillRepository, skillGapRepository,
          skillGapRecommendationRepository, refSkillGapStatusRepository,
          traineeRepository, new SkillGapRecommendationMapper(),
          null
      );

      assertThatThrownBy(() -> service.getTraineeSkillGapProfile(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  // =========================================================================
  // 4. EmploymentLifecycleWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("4. EmploymentLifecycleWorkflowService Wiring")
  class EmploymentLifecycleWiringTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private EmploymentExitEventService employmentExitEventService;
    @Mock private TraineeUnemploymentEventService traineeUnemploymentEventService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;

    @Test
    @DisplayName("recordExitAndInitiateUnemployment fails closed without validator")
    void shouldFailClosedWithoutValidatorOnExit() {
      EmploymentLifecycleWorkflowServiceImpl service = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService, employmentExitEventService,
          traineeUnemploymentEventService, salaryHistoryService,
          traineeUnemploymentEventRepository,
          null
      );

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      request.setExitEventRequest(new CreateEmploymentExitEventRequest());

      assertThatThrownBy(() -> service.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("transitionUnemploymentToEmployment fails closed without validator")
    void shouldFailClosedWithoutValidatorOnTransition() {
      EmploymentLifecycleWorkflowServiceImpl service = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService, employmentExitEventService,
          traineeUnemploymentEventService, salaryHistoryService,
          traineeUnemploymentEventRepository,
          null
      );

      UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
      request.setUnemploymentEventId(1L);
      request.setNewEmploymentRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> service.transitionUnemploymentToEmployment(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getTraineeCareerTimeline fails closed without validator")
    void shouldFailClosedWithoutValidatorOnTimeline() {
      EmploymentLifecycleWorkflowServiceImpl service = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService, employmentExitEventService,
          traineeUnemploymentEventService, salaryHistoryService,
          traineeUnemploymentEventRepository,
          null
      );

      assertThatThrownBy(() -> service.getTraineeCareerTimeline(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  // =========================================================================
  // 5. VerificationWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("5. VerificationWorkflowService Wiring")
  class VerificationWiringTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
    @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
    @Mock private EmploymentVerificationService employmentVerificationService;
    @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
    @Mock private UserRepository userRepository;
    @Mock private RefUserStatusRepository refUserStatusRepository;

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed without validator")
    void shouldFailClosedWithoutValidatorOnInitiate() {
      VerificationWorkflowServiceImpl service = new VerificationWorkflowServiceImpl(
          employmentRecordService, followupTaskService, surveyResponseService,
          employmentVerificationRequestService, employmentVerificationAttemptService,
          employmentVerificationService, employmentVerificationEvidenceService,
          userRepository, refUserStatusRepository
      );
      // Not calling setScopeValidator

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(1L);
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest details =
          new in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(100L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> service.initiateVerificationFromSurvey(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed without validator")
    void shouldFailClosedWithoutValidatorOnVerdict() {
      VerificationWorkflowServiceImpl service = new VerificationWorkflowServiceImpl(
          employmentRecordService, followupTaskService, surveyResponseService,
          employmentVerificationRequestService, employmentVerificationAttemptService,
          employmentVerificationService, employmentVerificationEvidenceService,
          userRepository, refUserStatusRepository
      );

      UserPrincipal principal = new UserPrincipal(42L, "verifier");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));

      User user = new User();
      user.setId(42L);
      user.setUserStatusId(1L);
      Mockito.when(userRepository.findById(42L)).thenReturn(Optional.of(user));

      RefUserStatus status = new RefUserStatus();
      status.setId(1L);
      status.setStatusCode("ACTIVE");
      Mockito.when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(status));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      request.setVerificationRequest(new CreateEmploymentVerificationRequest());

      assertThatThrownBy(() -> service.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getEmploymentVerificationSummary fails closed without validator")
    void shouldFailClosedWithoutValidatorOnSummary() {
      VerificationWorkflowServiceImpl service = new VerificationWorkflowServiceImpl(
          employmentRecordService, followupTaskService, surveyResponseService,
          employmentVerificationRequestService, employmentVerificationAttemptService,
          employmentVerificationService, employmentVerificationEvidenceService,
          userRepository, refUserStatusRepository
      );

      assertThatThrownBy(() -> service.getEmploymentVerificationSummary(100L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  // =========================================================================
  // 6. FollowupEngagementWorkflowService Wiring Tests
  // =========================================================================
  @Nested
  @DisplayName("6. FollowupEngagementWorkflowService Wiring")
  class FollowupEngagementWiringTests {

    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private CommunicationLogService communicationLogService;
    @Mock private TraineeConsentService traineeConsentService;

    @Test
    @DisplayName("recordFollowupSurveyResponse fails closed without validator")
    void shouldFailClosedWithoutValidatorOnSurvey() {
      FollowupEngagementWorkflowServiceImpl service = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService, surveyResponseService,
          communicationLogService, traineeConsentService,
          null
      );

      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(1L);
      request.setSurveyResponseRequest(new CreateSurveyResponseRequest());

      assertThatThrownBy(() -> service.recordFollowupSurveyResponse(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("recordEngagementAttempt fails closed without validator")
    void shouldFailClosedWithoutValidatorOnEngagement() {
      FollowupEngagementWorkflowServiceImpl service = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService, surveyResponseService,
          communicationLogService, traineeConsentService,
          null
      );

      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(1L);
      request.setCommunicationLogRequest(new in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest());

      assertThatThrownBy(() -> service.recordEngagementAttempt(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }

    @Test
    @DisplayName("getTraineeEngagementHistory fails closed without validator")
    void shouldFailClosedWithoutValidatorOnHistory() {
      FollowupEngagementWorkflowServiceImpl service = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService, surveyResponseService,
          communicationLogService, traineeConsentService,
          null
      );

      assertThatThrownBy(() -> service.getTraineeEngagementHistory(10L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }
}
