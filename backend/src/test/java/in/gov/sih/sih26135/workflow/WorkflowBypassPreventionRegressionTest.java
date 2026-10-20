package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
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
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 25.11 Bypass Prevention Regression Tests")
class WorkflowBypassPreventionRegressionTest {

  @Mock private WorkflowSecurityScopeValidator scopeValidator;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(Long userId, String username, List<String> roles) {
    UserPrincipal principal = new UserPrincipal(userId, username);
    List<SimpleGrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, authorities));
  }

  // =========================================================================
  // 1. SkillGapRemediationWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("1. Skill-Gap Remediation Bypass Prevention")
  class SkillGapRemediationBypassTests {

    @Mock private SkillGapAssessmentService skillGapAssessmentService;
    @Mock private SkillGapService skillGapService;
    @Mock private SkillGapRecommendationService skillGapRecommendationService;
    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private SkillGapRecommendationRepository skillGapRecommendationRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;
    @Mock private TraineeRepository traineeRepository;
    @Mock private SkillGapRecommendationMapper skillGapRecommendationMapper;

    private SkillGapRemediationWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new SkillGapRemediationWorkflowServiceImpl(
          skillGapAssessmentService, skillGapService, skillGapRecommendationService,
          trainingEnrollmentService, courseSkillRepository, skillGapRepository,
          skillGapRecommendationRepository, refSkillGapStatusRepository,
          traineeRepository, skillGapRecommendationMapper, scopeValidator
      );
    }

    @Test
    @DisplayName("recordAssessmentWithGaps rejects when assessment traineeId is null with zero mutation")
    void recordAssessmentWithGaps_nullTraineeId_failsClosed() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      CreateSkillGapAssessmentRequest assessReq = new CreateSkillGapAssessmentRequest();
      assessReq.setTraineeId(null); // NULL trainee
      request.setAssessmentRequest(assessReq);
      request.setGapRequests(List.of(new CreateSkillGapRequest()));

      assertThatThrownBy(() -> service.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ID_REQUIRED");

      verify(skillGapAssessmentService, never()).createAssessment(any());
      verify(skillGapService, never()).createSkillGap(any());
    }

    @Test
    @DisplayName("enrollTraineeInRemediationCourse resolves trainee ownership from parent gap and rejects cross-trainee enrollment with zero mutation")
    void enrollTrainee_crossTraineeScope_failsClosed() {
      setAuthenticatedUser(10L, "trainee_user", List.of("ROLE_trainee"));

      SkillGapRecommendationResponse rec = new SkillGapRecommendationResponse(
          100L, 200L, "GAP-001", 1, 1L, "COURSE", "Course",
          300L, "CRS-001", "Java Course", 1L, "SKL-001", "Java",
          true, LocalDate.now(), "Remarks", 1L,
          LocalDateTime.now(), LocalDateTime.now()
      );
      when(skillGapRecommendationService.getRecommendationById(100L)).thenReturn(rec);

      SkillGap gap = new SkillGap();
      gap.setId(200L);
      gap.setIsCurrent(true);
      Trainee targetTrainee = new Trainee();
      targetTrainee.setId(999L);
      gap.setTrainee(targetTrainee);
      when(skillGapRepository.findById(200L)).thenReturn(Optional.of(gap));

      Trainee authTrainee = new Trainee();
      authTrainee.setId(50L);
      when(traineeRepository.findByUserId(10L)).thenReturn(Optional.of(authTrainee));

      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(100L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setTraineeId(null); // Caller omits traineeId to attempt bypass
      request.setEnrollmentRequest(enrolReq);

      assertThatThrownBy(() -> service.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("acceptRecommendation rejects operator with operational permission shortcut assessment.manage who is not trainee owner")
    void acceptRecommendation_unauthorizedPermissionShortcut_failsClosed() {
      setAuthenticatedUser(11L, "operator_user", List.of("assessment.manage"));

      SkillGapRecommendation recEntity = new SkillGapRecommendation();
      recEntity.setId(100L);
      SkillGap gap = new SkillGap();
      gap.setId(200L);
      Trainee gapTrainee = new Trainee();
      gapTrainee.setId(999L);
      gap.setTrainee(gapTrainee);
      recEntity.setSkillGap(gap);
      when(skillGapRecommendationRepository.findById(100L)).thenReturn(Optional.of(recEntity));

      when(traineeRepository.findByUserId(11L)).thenReturn(Optional.empty());
      when(scopeValidator.isAdministrativeCaller()).thenReturn(false);

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      request.setTraineeId(999L);

      assertThatThrownBy(() -> service.acceptRecommendation(100L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(skillGapRecommendationService, never()).updateRecommendation(any(), any());
    }

    @Test
    @DisplayName("rejectRecommendation rejects operator with unauthorized role ROLE_ADMIN who is not trainee owner")
    void rejectRecommendation_unauthorizedRoleAdmin_failsClosed() {
      setAuthenticatedUser(12L, "legacy_admin", List.of("ROLE_ADMIN"));

      SkillGapRecommendation recEntity = new SkillGapRecommendation();
      recEntity.setId(100L);
      SkillGap gap = new SkillGap();
      gap.setId(200L);
      Trainee gapTrainee = new Trainee();
      gapTrainee.setId(999L);
      gap.setTrainee(gapTrainee);
      recEntity.setSkillGap(gap);
      when(skillGapRecommendationRepository.findById(100L)).thenReturn(Optional.of(recEntity));

      when(traineeRepository.findByUserId(12L)).thenReturn(Optional.empty());
      when(scopeValidator.isAdministrativeCaller()).thenReturn(false);

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      request.setRemarks("Cannot proceed");

      assertThatThrownBy(() -> service.rejectRecommendation(100L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(skillGapRecommendationService, never()).updateRecommendation(any(), any());
    }
  }

  // =========================================================================
  // 2. FollowupEngagementWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("2. Followup-Engagement Bypass Prevention")
  class FollowupEngagementBypassTests {

    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private CommunicationLogService communicationLogService;
    @Mock private TraineeConsentService traineeConsentService;

    private FollowupEngagementWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService, surveyResponseService,
          communicationLogService, traineeConsentService,
          scopeValidator
      );
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse rejects missing parent task with zero survey mutation")
    void recordSurveyResponse_missingParentTask_failsClosed() {
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(999L);
      request.setSurveyResponseRequest(new CreateSurveyResponseRequest());

      when(followupTaskService.getFollowupTaskById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.recordFollowupSurveyResponse(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("recordEngagementAttempt rejects missing parent task with zero log mutation")
    void recordEngagementAttempt_missingParentTask_failsClosed() {
      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(999L);
      request.setCommunicationLogRequest(new CreateCommunicationLogRequest());

      when(followupTaskService.getFollowupTaskById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.recordEngagementAttempt(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(communicationLogService, never()).logCommunication(any());
    }
  }

  // =========================================================================
  // 3. EmploymentLifecycleWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("3. Employment-Lifecycle Bypass Prevention")
  class EmploymentLifecycleBypassTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private EmploymentExitEventService employmentExitEventService;
    @Mock private TraineeUnemploymentEventService traineeUnemploymentEventService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;

    private EmploymentLifecycleWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService, employmentExitEventService,
          traineeUnemploymentEventService, salaryHistoryService,
          traineeUnemploymentEventRepository, scopeValidator
      );
    }

    @Test
    @DisplayName("recordExit rejects null employmentId with zero exit event mutation")
    void recordExit_nullEmploymentId_failsClosed() {
      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(null); // NULL employment
      request.setExitEventRequest(exitReq);

      assertThatThrownBy(() -> service.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_ID_REQUIRED");

      verify(employmentExitEventService, never()).createExitEvent(any());
    }

    @Test
    @DisplayName("recordExit rejects missing parent employment record with zero exit event mutation")
    void recordExit_missingEmploymentRecord_failsClosed() {
      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(999L);
      request.setExitEventRequest(exitReq);

      when(employmentRecordService.getEmploymentRecordById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(employmentExitEventService, never()).createExitEvent(any());
    }

    @Test
    @DisplayName("transitionUnemploymentToEmployment rejects null employerId with zero employment creation")
    void transitionUnemployment_nullEmployerId_failsClosed() {
      UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
      request.setUnemploymentEventId(10L);
      CreateEmploymentRecordRequest newEmp = new CreateEmploymentRecordRequest();
      newEmp.setEmployerId(null); // NULL employer
      request.setNewEmploymentRequest(newEmp);

      TraineeUnemploymentEvent unempEvent = new TraineeUnemploymentEvent();
      unempEvent.setId(10L);
      Trainee trainee = new Trainee();
      trainee.setId(5L);
      unempEvent.setTrainee(trainee);
      when(traineeUnemploymentEventRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(unempEvent));

      assertThatThrownBy(() -> service.transitionUnemploymentToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ID_REQUIRED");

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }
  }

  // =========================================================================
  // 4. VerificationWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("4. Verification Bypass Prevention")
  class VerificationBypassTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
    @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
    @Mock private EmploymentVerificationService employmentVerificationService;
    @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
    @Mock private UserRepository userRepository;
    @Mock private RefUserStatusRepository refUserStatusRepository;

    private VerificationWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new VerificationWorkflowServiceImpl(
          employmentRecordService, followupTaskService, surveyResponseService,
          employmentVerificationRequestService, employmentVerificationAttemptService,
          employmentVerificationService, employmentVerificationEvidenceService,
          userRepository, refUserStatusRepository, scopeValidator
      );
    }

    @Test
    @DisplayName("initiateVerification rejects missing survey response with zero request creation")
    void initiateVerification_missingSurveyResponse_failsClosed() {
      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(999L);
      request.setVerificationRequestDetails(new in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest());
      when(surveyResponseService.getSurveyResponseById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.initiateVerificationFromSurvey(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("recordVerdict rejects null employmentRecordId with zero verification creation")
    void recordVerdict_nullEmploymentRecordId_failsClosed() {
      setAuthenticatedUser(1L, "verifier_user", List.of("ROLE_verifier"));

      User verifier = new User();
      verifier.setId(1L);
      verifier.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(verifier));

      RefUserStatus status = new RefUserStatus();
      status.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(status));

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(null); // NULL employment
      verReq.setEmploymentVerificationRequestId(null);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> service.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_ID_REQUIRED");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }
  }

  // =========================================================================
  // 5. TrainingOutcomeWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("5. Training-Outcome Bypass Prevention")
  class TrainingOutcomeBypassTests {

    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private AssessmentResultService assessmentResultService;
    @Mock private CertificationService certificationService;
    @Mock private TraineeAssessmentService traineeAssessmentService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;

    private TrainingOutcomeWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService, traineeAssessmentService,
          assessmentResultService, certificationService,
          courseSkillRepository, skillGapRepository,
          refSkillGapStatusRepository, scopeValidator
      );
    }

    @Test
    @DisplayName("recordAssessmentResult rejects unresolvable enrollment with zero mutation")
    void recordAssessmentResult_unresolvableEnrollment_failsClosed() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest arReq = new CreateAssessmentResultRequest();
      arReq.setTraineeAssessmentId(null);
      request.setAssessmentResultRequest(arReq);
      request.setEnrollmentId(null); // Unresolvable enrollment

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_ID_REQUIRED");

      verify(assessmentResultService, never()).createAssessmentResult(any());
    }

    @Test
    @DisplayName("recordAssessmentResult rejects missing parent enrollment with zero mutation")
    void recordAssessmentResult_missingEnrollment_failsClosed() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest arReq = new CreateAssessmentResultRequest();
      request.setAssessmentResultRequest(arReq);
      request.setEnrollmentId(999L);

      when(trainingEnrollmentService.getById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(assessmentResultService, never()).createAssessmentResult(any());
    }
  }

  // =========================================================================
  // 6. PlacementEmploymentWorkflowService Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("6. Placement-Employment Bypass Prevention")
  class PlacementEmploymentBypassTests {

    @Mock private PlacementRecordService placementRecordService;
    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private EmploymentRecordRepository employmentRecordRepository;
    @Mock private RefPlacementStatusRepository refPlacementStatusRepository;
    @Mock private RefJoiningStatusRepository refJoiningStatusRepository;

    private PlacementEmploymentWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
      service = new PlacementEmploymentWorkflowServiceImpl(
          placementRecordService, employmentRecordService,
          salaryHistoryService, employmentRecordRepository,
          refPlacementStatusRepository, refJoiningStatusRepository,
          scopeValidator
      );
    }

    @Test
    @DisplayName("transitionPlacement rejects missing placement with zero employment record creation")
    void transitionPlacement_missingPlacement_failsClosed() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(999L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      when(placementRecordService.getPlacementRecordById(999L)).thenReturn(null);

      assertThatThrownBy(() -> service.transitionPlacementToEmployment(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }
  }

  // =========================================================================
  // 7. Administrative Target & Affiliation Bypass Prevention
  // =========================================================================
  @Nested
  @DisplayName("7. Administrative Target & Affiliation Bypass Prevention")
  class AdminTargetAndAffiliationBypassTests {

    @Mock private EmployerRepository realEmployerRepo;
    @Mock private TrainingProviderRepository realProviderRepo;
    @Mock private TraineeRepository realTraineeRepo;
    @Mock private UserRepository realUserRepo;
    @Mock private OrganizationRepository realOrganizationRepo;

    @Mock private PlacementRecordService placementRecordService;
    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private EmploymentRecordRepository employmentRecordRepository;
    @Mock private RefPlacementStatusRepository refPlacementStatusRepository;
    @Mock private RefJoiningStatusRepository refJoiningStatusRepository;

    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private TraineeAssessmentService traineeAssessmentService;
    @Mock private AssessmentResultService assessmentResultService;
    @Mock private CertificationService certificationService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;

    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private CommunicationLogService communicationLogService;
    @Mock private TraineeConsentService traineeConsentService;

    @Mock private EmploymentExitEventService employmentExitEventService;
    @Mock private TraineeUnemploymentEventService traineeUnemploymentEventService;
    @Mock private TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;

    @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
    @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
    @Mock private EmploymentVerificationService employmentVerificationService;
    @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
    @Mock private RefUserStatusRepository refUserStatusRepository;

    private WorkflowSecurityScopeValidator realValidator;
    private PlacementEmploymentWorkflowServiceImpl placementService;
    private TrainingOutcomeWorkflowServiceImpl outcomeService;
    private FollowupEngagementWorkflowServiceImpl followupService;
    private EmploymentLifecycleWorkflowServiceImpl lifecycleService;
    private VerificationWorkflowServiceImpl verificationService;

    @BeforeEach
    void setUp() {
      realValidator = new WorkflowSecurityScopeValidator(
          realUserRepo, realTraineeRepo, realEmployerRepo, realProviderRepo, realOrganizationRepo);

      lenient().when(realOrganizationRepo.existsById(anyLong())).thenReturn(true);

      placementService = new PlacementEmploymentWorkflowServiceImpl(
          placementRecordService, employmentRecordService,
          salaryHistoryService, employmentRecordRepository,
          refPlacementStatusRepository, refJoiningStatusRepository,
          realValidator
      );

      outcomeService = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService, traineeAssessmentService,
          assessmentResultService, certificationService,
          courseSkillRepository, skillGapRepository,
          refSkillGapStatusRepository, realValidator
      );

      followupService = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService, surveyResponseService,
          communicationLogService, traineeConsentService,
          realValidator
      );

      lifecycleService = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService, employmentExitEventService,
          traineeUnemploymentEventService, salaryHistoryService,
          traineeUnemploymentEventRepository, realValidator
      );

      verificationService = new VerificationWorkflowServiceImpl(
          employmentRecordService, followupTaskService,
          surveyResponseService, employmentVerificationRequestService,
          employmentVerificationAttemptService, employmentVerificationService,
          employmentVerificationEvidenceService, realUserRepo,
          refUserStatusRepository, realValidator
      );

      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              new UserPrincipal(1L, "super_admin"),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
          )
      );
    }

    @Test
    @DisplayName("Admin transitionPlacement: missing target employer fails closed with zero mutation")
    void adminTransitionPlacement_targetEmployerNotFound_failsClosed() {
      PlacementRecordResponse placement = new PlacementRecordResponse(
          10L, "PLC-010", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 999L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
      when(realEmployerRepo.findById(999L)).thenReturn(Optional.empty());

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> placementService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_NOT_FOUND");

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Admin transitionPlacement: employer nonexistent organization affiliation fails closed with zero mutation")
    void adminTransitionPlacement_employerNonexistentOrganization_failsClosed() {
      PlacementRecordResponse placement = new PlacementRecordResponse(
          10L, "PLC-010", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 999L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

      Employer employerWithInvalidOrg = new Employer();
      employerWithInvalidOrg.setId(999L);
      employerWithInvalidOrg.setOrganizationId(7777L);
      when(realEmployerRepo.findById(999L)).thenReturn(Optional.of(employerWithInvalidOrg));
      when(realOrganizationRepo.existsById(7777L)).thenReturn(false);

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> placementService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Admin transitionPlacement: employer missing organization affiliation fails closed with zero mutation")
    void adminTransitionPlacement_employerMissingOrganization_failsClosed() {
      PlacementRecordResponse placement = new PlacementRecordResponse(
          10L, "PLC-010", 100L, "TRN-001", "John", "Doe",
          1L, "ENR-001", 1L, "Course", 1L, "Program",
          1L, "Provider", 999L, "Employer", 1L, "Branch",
          1L, "JOB-001", 1L, 1L, "Role", 1L, "FULL_TIME",
          1L, "DIRECT", 1L, "OFFERED", 1L, "JOINED",
          BigDecimal.valueOf(50000), BigDecimal.valueOf(50000), 1L, "MONTHLY", "INR",
          LocalDate.now(), LocalDate.now(), LocalDate.now(), null, null, "Remarks",
          1L, 1L, 1L, "VERIFIED", LocalDateTime.now(), 1L,
          LocalDateTime.now(), LocalDateTime.now(), null
      );
      when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

      Employer employerWithoutOrg = new Employer();
      employerWithoutOrg.setId(999L);
      employerWithoutOrg.setOrganizationId(null);
      when(realEmployerRepo.findById(999L)).thenReturn(Optional.of(employerWithoutOrg));

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);
      request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> placementService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Admin recordAssessmentResult: provider nonexistent organization affiliation fails closed with zero mutation")
    void adminRecordAssessmentResult_providerNonexistentOrganization_failsClosed() {
      TrainingEnrollmentResponse enrollment = new TrainingEnrollmentResponse();
      enrollment.setId(20L);
      enrollment.setTraineeId(100L);
      enrollment.setProviderId(888L);
      when(trainingEnrollmentService.getById(20L)).thenReturn(enrollment);

      TrainingProvider providerWithInvalidOrg = new TrainingProvider();
      providerWithInvalidOrg.setId(888L);
      providerWithInvalidOrg.setOrganizationId(7777L);
      when(realProviderRepo.findById(888L)).thenReturn(Optional.of(providerWithInvalidOrg));
      when(realOrganizationRepo.existsById(7777L)).thenReturn(false);

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(20L);
      request.setAssessmentResultRequest(new CreateAssessmentResultRequest());

      assertThatThrownBy(() -> outcomeService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Admin recordAssessmentResult: missing target provider fails closed with zero mutation")
    void adminRecordAssessmentResult_targetProviderNotFound_failsClosed() {
      TrainingEnrollmentResponse enrollment = new TrainingEnrollmentResponse();
      enrollment.setId(20L);
      enrollment.setTraineeId(100L);
      enrollment.setProviderId(888L);
      when(trainingEnrollmentService.getById(20L)).thenReturn(enrollment);
      when(realProviderRepo.findById(888L)).thenReturn(Optional.empty());

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(20L);
      request.setAssessmentResultRequest(new CreateAssessmentResultRequest());

      assertThatThrownBy(() -> outcomeService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_NOT_FOUND");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Admin recordAssessmentResult: provider missing organization affiliation fails closed with zero mutation")
    void adminRecordAssessmentResult_providerMissingOrganization_failsClosed() {
      TrainingEnrollmentResponse enrollment = new TrainingEnrollmentResponse();
      enrollment.setId(20L);
      enrollment.setTraineeId(100L);
      enrollment.setProviderId(888L);
      when(trainingEnrollmentService.getById(20L)).thenReturn(enrollment);

      TrainingProvider providerWithoutOrg = new TrainingProvider();
      providerWithoutOrg.setId(888L);
      providerWithoutOrg.setOrganizationId(null);
      when(realProviderRepo.findById(888L)).thenReturn(Optional.of(providerWithoutOrg));

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(20L);
      request.setAssessmentResultRequest(new CreateAssessmentResultRequest());

      assertThatThrownBy(() -> outcomeService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Admin recordExitAndInitiateUnemployment: missing target employer fails closed with zero mutation")
    void adminRecordExit_targetEmployerNotFound_failsClosed() {
      EmploymentRecordResponse emp = mock(EmploymentRecordResponse.class);
      when(emp.employerId()).thenReturn(777L);
      when(employmentRecordService.getEmploymentRecordById(30L)).thenReturn(emp);
      when(realEmployerRepo.findById(777L)).thenReturn(Optional.empty());

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(30L);
      request.setExitEventRequest(exitReq);

      assertThatThrownBy(() -> lifecycleService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_NOT_FOUND");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Admin initiateVerificationFromSurvey: missing target employer fails closed with zero mutation")
    void adminInitiateVerification_targetEmployerNotFound_failsClosed() {
      SurveyResponseResponse survey = new SurveyResponseResponse();
      survey.setId(60L);
      survey.setTraineeId(100L);
      when(surveyResponseService.getSurveyResponseById(60L)).thenReturn(survey);
      when(realTraineeRepo.existsById(100L)).thenReturn(true);

      EmploymentRecordResponse emp = mock(EmploymentRecordResponse.class);
      when(emp.employerId()).thenReturn(666L);
      when(employmentRecordService.getEmploymentRecordById(40L)).thenReturn(emp);
      when(realEmployerRepo.findById(666L)).thenReturn(Optional.empty());

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(60L);
      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(40L);
      details.setTraineeId(100L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> verificationService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_NOT_FOUND");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("Admin recordFollowupSurveyResponse: missing target trainee fails closed with zero mutation")
    void adminFollowupSurvey_targetTraineeNotFound_failsClosed() {
      FollowupTaskResponse task = new FollowupTaskResponse();
      task.setId(50L);
      task.setTraineeId(555L);
      when(followupTaskService.getFollowupTaskById(50L)).thenReturn(task);
      when(realTraineeRepo.existsById(555L)).thenReturn(false);

      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(50L);
      request.setSurveyResponseRequest(new CreateSurveyResponseRequest());

      assertThatThrownBy(() -> followupService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_NOT_FOUND");

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("Admin recordEngagementAttempt: missing target trainee fails closed with zero mutation")
    void adminEngagementAttempt_targetTraineeNotFound_failsClosed() {
      FollowupTaskResponse task = new FollowupTaskResponse();
      task.setId(50L);
      task.setTraineeId(555L);
      when(followupTaskService.getFollowupTaskById(50L)).thenReturn(task);
      when(realTraineeRepo.existsById(555L)).thenReturn(false);

      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(50L);
      request.setCommunicationLogRequest(new CreateCommunicationLogRequest());

      assertThatThrownBy(() -> followupService.recordEngagementAttempt(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_NOT_FOUND");

      verify(communicationLogService, never()).logCommunication(any());
    }
  }
}
