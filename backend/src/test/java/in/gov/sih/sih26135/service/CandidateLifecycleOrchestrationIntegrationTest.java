package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.RemediationTriggerRequest;
import in.gov.sih.sih26135.dto.request.TraineeEnrollmentOrchestrationRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.CandidateLifecycleSummaryResponse;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.RemediationTriggerResponse;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.dto.response.TraineeEnrollmentOrchestrationResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.event.DataQualityAlertEvent;
import in.gov.sih.sih26135.event.EmploymentVerificationEvent;
import in.gov.sih.sih26135.event.PlacementLifecycleEvent;
import in.gov.sih.sih26135.event.TraineeLifecycleEvent;
import in.gov.sih.sih26135.event.listener.CandidateLifecycleEventListener;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.CandidateLifecycleOrchestratorServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 29.2 — Candidate Lifecycle Cross-Workflow Orchestration & Transaction Choreography Tests")
class CandidateLifecycleOrchestrationIntegrationTest {

  @Mock private TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;
  @Mock private PlacementEmploymentWorkflowService placementEmploymentWorkflowService;
  @Mock private VerificationWorkflowService verificationWorkflowService;
  @Mock private EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService;
  @Mock private SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;
  @Mock private TraineeConsentService traineeConsentService;
  @Mock private TrainingEnrollmentService trainingEnrollmentService;
  @Mock private TrainingBatchRepository trainingBatchRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private WorkflowSecurityScopeValidator scopeValidator;

  private CandidateLifecycleOrchestratorServiceImpl orchestratorService;
  private CandidateLifecycleEventListener eventListener;

  private static final Long TRAINEE_ID = 101L;
  private static final Long BATCH_ID = 202L;
  private static final Long PROVIDER_ID = 303L;
  private static final Long COURSE_ID = 404L;
  private static final Long PROGRAM_ID = 505L;
  private static final Long CENTER_ID = 606L;
  private static final Long ENROLLMENT_ID = 707L;
  private static final Long PLACEMENT_ID = 808L;
  private static final Long EMPLOYER_ID = 909L;
  private static final Long EMPLOYMENT_ID = 1010L;
  private static final Long VERIFICATION_ID = 1111L;
  private static final Long EXIT_EVENT_ID = 1212L;
  private static final Long ASSESSMENT_ID = 1313L;

  private Trainee testTrainee;
  private TrainingBatch testBatch;
  private TrainingProvider testProvider;
  private Course testCourse;
  private Program testProgram;
  private TrainingCenter testCenter;
  private RefEnrollmentStatus enrolledStatus;

  @BeforeEach
  void setUp() {
    CorrelationContextHolder.setCorrelationId("corr-test-29-2");

    orchestratorService = new CandidateLifecycleOrchestratorServiceImpl(
        trainingOutcomeWorkflowService,
        placementEmploymentWorkflowService,
        verificationWorkflowService,
        employmentLifecycleWorkflowService,
        skillGapRemediationWorkflowService,
        traineeConsentService,
        trainingEnrollmentService,
        trainingBatchRepository,
        traineeRepository,
        refEnrollmentStatusRepository,
        eventPublisher,
        scopeValidator
    );

    eventListener = new CandidateLifecycleEventListener();

    testTrainee = new Trainee();
    testTrainee.setId(TRAINEE_ID);
    testTrainee.setFirstName("Rahul");
    testTrainee.setLastName("Sharma");

    testProvider = new TrainingProvider();
    testProvider.setId(PROVIDER_ID);

    testCourse = new Course();
    testCourse.setId(COURSE_ID);

    testProgram = new Program();
    testProgram.setId(PROGRAM_ID);

    testCenter = new TrainingCenter();
    testCenter.setId(CENTER_ID);
    testCenter.setStateId(27L);
    testCenter.setDistrictId(518L);

    testBatch = new TrainingBatch();
    testBatch.setId(BATCH_ID);
    testBatch.setTrainingProvider(testProvider);
    testBatch.setCourse(testCourse);
    testBatch.setProgram(testProgram);
    testBatch.setTrainingCenter(testCenter);
    testBatch.setStartDate(LocalDate.of(2026, 1, 10));
    testBatch.setEndDate(LocalDate.of(2026, 4, 10));

    enrolledStatus = new RefEnrollmentStatus("ENROLLED", "Enrolled", false, false, 1);
    enrolledStatus.setId(1L);
  }

  @AfterEach
  void tearDown() {
    CorrelationContextHolder.clear();
  }

  // =========================================================================
  // Stage A: Registration and Enrollment Orchestration
  // =========================================================================

  @Nested
  @DisplayName("Stage A: Registration & Enrollment Orchestration")
  class StageAEnrollmentTests {

    @Test
    @DisplayName("Successfully enrolls candidate with validated consent and publishes TraineeLifecycleEvent")
    void testEnrollCandidateSuccessWithValidatedConsent() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );
      request.setConsentCode("DATA_PROCESSING");

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
      when(refEnrollmentStatusRepository.findByStatusCode("ENROLLED")).thenReturn(Optional.of(enrolledStatus));
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "DATA_PROCESSING");

      TrainingEnrollmentResponse mockResponse = new TrainingEnrollmentResponse();
      mockResponse.setId(ENROLLMENT_ID);
      mockResponse.setTraineeId(TRAINEE_ID);
      mockResponse.setEnrollmentNumber("ENR-202-101-12345");
      when(trainingEnrollmentService.createEnrollment(any(CreateTrainingEnrollmentRequest.class)))
          .thenReturn(mockResponse);

      TraineeEnrollmentOrchestrationResponse response =
          orchestratorService.enrollCandidateWithConsentValidation(request);

      assertThat(response).isNotNull();
      assertThat(response.enrollment().getId()).isEqualTo(ENROLLMENT_ID);
      assertThat(response.consentStatus()).isEqualTo("VERIFIED");
      assertThat(response.stageStatus()).isEqualTo("COMPLETED");

      verify(scopeValidator).validateProviderScope(PROVIDER_ID);
      verify(scopeValidator).validateTraineeScope(TRAINEE_ID);
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "DATA_PROCESSING");
      verify(trainingEnrollmentService).createEnrollment(any(CreateTrainingEnrollmentRequest.class));

      ArgumentCaptor<TraineeLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(TraineeLifecycleEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TraineeLifecycleEvent publishedEvent = eventCaptor.getValue();

      assertThat(publishedEvent.getEventType()).isEqualTo("ENROLLMENT_CONFIRMED");
      assertThat(publishedEvent.getEntityId()).isEqualTo(TRAINEE_ID);
      assertThat(publishedEvent.batchId()).isEqualTo(BATCH_ID);
      assertThat(publishedEvent.newStatus()).isEqualTo("ENROLLED");
      assertThat(publishedEvent.getCorrelationId()).isEqualTo("corr-test-29-2");

      eventListener.onTraineeLifecycleEvent(publishedEvent);
    }

    @Test
    @DisplayName("Throws BadRequestException and halts enrollment when consent is missing")
    void testEnrollCandidateThrowsWhenConsentMissing() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
      doThrow(new BadRequestException("Trainee has not granted consent for DATA_PROCESSING", "CONSENT_REQUIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "DATA_PROCESSING");

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Trainee has not granted consent for DATA_PROCESSING");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws AccessDeniedException and aborts enrollment when provider scope is unauthorized")
    void testEnrollCandidateThrowsWhenUnauthorizedScope() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));
      doThrow(new AccessDeniedException("Access denied"))
          .when(scopeValidator).validateProviderScope(PROVIDER_ID);

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Access denied");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when trainee is soft-deleted")
    void testEnrollCandidateThrowsWhenTraineeSoftDeleted() {
      testTrainee.setDeletedAt(LocalDateTime.now());
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Cannot create training enrollment for soft-deleted trainee");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when batch is deleted")
    void testEnrollCandidateThrowsWhenBatchDeleted() {
      testBatch.setDeletedAt(LocalDateTime.now());
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Cannot create training enrollment for soft-deleted training batch");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when provider in request mismatches batch provider")
    void testEnrollCandidateThrowsWhenProviderMismatch() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );
      request.setTrainingProviderId(9999L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Training provider does not match training batch provider");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when course in request mismatches batch course")
    void testEnrollCandidateThrowsWhenCourseMismatch() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );
      request.setCourseId(9999L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Course does not match training batch course");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when program in request mismatches batch program")
    void testEnrollCandidateThrowsWhenProgramMismatch() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );
      request.setProgramId(9999L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Program does not match training batch program");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Throws BadRequestException when center in request mismatches batch center")
    void testEnrollCandidateThrowsWhenCenterMismatch() {
      TraineeEnrollmentOrchestrationRequest request = new TraineeEnrollmentOrchestrationRequest(
          TRAINEE_ID, BATCH_ID, LocalDate.of(2026, 1, 10)
      );
      request.setCenterId(9999L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(testTrainee));
      when(trainingBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(testBatch));

      assertThatThrownBy(() -> orchestratorService.enrollCandidateWithConsentValidation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Center does not match training batch center");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  // =========================================================================
  // Stage B: Training & Assessment with Eligible Certification
  // =========================================================================

  @Nested
  @DisplayName("Stage B: Training Outcome & Certification Orchestration")
  class StageBTrainingAssessmentTests {

    @Test
    @DisplayName("Delegates to TrainingOutcomeWorkflowService and publishes TraineeLifecycleEvent on certification")
    void testRecordAssessmentAndEligibleCertification() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();

      AssessmentResultResponse assessmentResp = new AssessmentResultResponse();
      assessmentResp.setId(55L);
      assessmentResp.setTraineeId(TRAINEE_ID);

      CertificationResponse certResp = new CertificationResponse();
      certResp.setId(77L);
      certResp.setTraineeId(TRAINEE_ID);
      certResp.setCourseId(COURSE_ID);
      certResp.setCertificateNumber("CERT-2026-999");

      RecordAssessmentAndIssueCertificateResponse mockResponse =
          new RecordAssessmentAndIssueCertificateResponse(assessmentResp, certResp, new TrainingEnrollmentResponse());

      when(trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(request))
          .thenReturn(mockResponse);

      RecordAssessmentAndIssueCertificateResponse response =
          orchestratorService.recordAssessmentAndCertification(request);

      assertThat(response).isNotNull();
      assertThat(response.certification()).isNotNull();
      assertThat(response.certification().getCertificateNumber()).isEqualTo("CERT-2026-999");

      ArgumentCaptor<TraineeLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(TraineeLifecycleEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TraineeLifecycleEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("CERTIFICATION_ISSUED");
      assertThat(published.getEntityId()).isEqualTo(TRAINEE_ID);
      assertThat(published.newStatus()).isEqualTo("CERTIFIED");

      eventListener.onTraineeLifecycleEvent(published);
    }

    @Test
    @DisplayName("Does not publish certification event when certification is not eligible/issued")
    void testRecordAssessmentWithoutCertificationDoesNotPublishCertEvent() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();

      AssessmentResultResponse assessmentResp = new AssessmentResultResponse();
      assessmentResp.setId(55L);
      assessmentResp.setTraineeId(TRAINEE_ID);

      RecordAssessmentAndIssueCertificateResponse mockResponse =
          new RecordAssessmentAndIssueCertificateResponse(assessmentResp, null, new TrainingEnrollmentResponse());

      when(trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(request))
          .thenReturn(mockResponse);

      RecordAssessmentAndIssueCertificateResponse response =
          orchestratorService.recordAssessmentAndCertification(request);

      assertThat(response).isNotNull();
      assertThat(response.certification()).isNull();

      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  // =========================================================================
  // Stage C: Placement to Employment Transition
  // =========================================================================

  @Nested
  @DisplayName("Stage C: Placement to Employment Transition Orchestration")
  class StageCPlacementEmploymentTests {

    @Test
    @DisplayName("Transitions placement to employment and publishes PlacementLifecycleEvent")
    void testOrchestratePlacementToEmploymentSuccess() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(PLACEMENT_ID);

      PlacementRecordResponse placementResp = mock(PlacementRecordResponse.class);
      when(placementResp.id()).thenReturn(PLACEMENT_ID);
      when(placementResp.traineeId()).thenReturn(TRAINEE_ID);
      when(placementResp.placementStatusCode()).thenReturn("JOINED");
      when(placementResp.joiningStatusCode()).thenReturn("JOINED");
      when(placementResp.offeredSalary()).thenReturn(new BigDecimal("45000.00"));

      EmploymentRecordResponse employmentResp = mock(EmploymentRecordResponse.class);
      when(employmentResp.id()).thenReturn(EMPLOYMENT_ID);
      when(employmentResp.employerId()).thenReturn(EMPLOYER_ID);
      when(employmentResp.workStateId()).thenReturn(27L);

      PlacementToEmploymentTransitionResponse mockResponse =
          new PlacementToEmploymentTransitionResponse(placementResp, employmentResp, null);

      when(placementEmploymentWorkflowService.transitionPlacementToEmployment(request))
          .thenReturn(mockResponse);

      PlacementToEmploymentTransitionResponse response =
          orchestratorService.orchestratePlacementToEmployment(request);

      assertThat(response).isNotNull();
      assertThat(response.placement().id()).isEqualTo(PLACEMENT_ID);
      assertThat(response.employmentRecord().id()).isEqualTo(EMPLOYMENT_ID);

      ArgumentCaptor<PlacementLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(PlacementLifecycleEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      PlacementLifecycleEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("EMPLOYMENT_TRANSITION");
      assertThat(published.getEntityId()).isEqualTo(PLACEMENT_ID);
      assertThat(published.traineeId()).isEqualTo(TRAINEE_ID);
      assertThat(published.employerId()).isEqualTo(EMPLOYER_ID);
      assertThat(published.placementStatusCode()).isEqualTo("JOINED");

      eventListener.onPlacementLifecycleEvent(published);
    }
  }

  // =========================================================================
  // Stage D: Employment Verification Verdict Attestation
  // =========================================================================

  @Nested
  @DisplayName("Stage D: Employment Verification Verdict Orchestration")
  class StageDVerificationTests {

    @Test
    @DisplayName("Publishes EmploymentVerificationEvent on verified verdict")
    void testOrchestrateEmploymentVerificationVerdictVerified() {
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();

      EmploymentVerificationResponse verResp = mock(EmploymentVerificationResponse.class);
      when(verResp.id()).thenReturn(VERIFICATION_ID);
      when(verResp.employmentRecordId()).thenReturn(EMPLOYMENT_ID);
      when(verResp.traineeId()).thenReturn(TRAINEE_ID);
      when(verResp.employerId()).thenReturn(EMPLOYER_ID);
      when(verResp.verificationNumber()).thenReturn("VRF-001");
      when(verResp.methodCode()).thenReturn("PAYSLIP");
      when(verResp.recordVerificationStatusCode()).thenReturn("VERIFIED");

      RecordVerificationVerdictResponse mockResponse =
          new RecordVerificationVerdictResponse(verResp, Collections.emptyList(), null, null);

      when(verificationWorkflowService.recordVerificationVerdictAndSynchronize(request))
          .thenReturn(mockResponse);

      RecordVerificationVerdictResponse response =
          orchestratorService.orchestrateEmploymentVerificationVerdict(request);

      assertThat(response).isNotNull();
      assertThat(response.verification().id()).isEqualTo(VERIFICATION_ID);

      ArgumentCaptor<EmploymentVerificationEvent> eventCaptor = ArgumentCaptor.forClass(EmploymentVerificationEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      EmploymentVerificationEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("VERIFICATION_VERDICT");
      assertThat(published.getEntityId()).isEqualTo(VERIFICATION_ID);
      assertThat(published.employmentRecordId()).isEqualTo(EMPLOYMENT_ID);
      assertThat(published.verificationStatus()).isEqualTo("VERIFIED");

      eventListener.onEmploymentVerificationEvent(published);
    }

    @Test
    @DisplayName("Publishes both EmploymentVerificationEvent and DataQualityAlertEvent on rejected verdict")
    void testOrchestrateEmploymentVerificationVerdictRejectedTriggersDataQualityAlert() {
      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();

      EmploymentVerificationResponse verResp = mock(EmploymentVerificationResponse.class);
      when(verResp.id()).thenReturn(VERIFICATION_ID);
      when(verResp.employmentRecordId()).thenReturn(EMPLOYMENT_ID);
      when(verResp.traineeId()).thenReturn(TRAINEE_ID);
      when(verResp.employerId()).thenReturn(EMPLOYER_ID);
      when(verResp.verificationNumber()).thenReturn("VRF-002");
      when(verResp.methodCode()).thenReturn("PAYSLIP");
      when(verResp.recordVerificationStatusCode()).thenReturn("REJECTED");

      RecordVerificationVerdictResponse mockResponse =
          new RecordVerificationVerdictResponse(verResp, Collections.emptyList(), null, null);

      when(verificationWorkflowService.recordVerificationVerdictAndSynchronize(request))
          .thenReturn(mockResponse);

      RecordVerificationVerdictResponse response =
          orchestratorService.orchestrateEmploymentVerificationVerdict(request);

      assertThat(response).isNotNull();

      ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
      verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
      List<Object> publishedEvents = eventCaptor.getAllValues();

      assertThat(publishedEvents.get(0)).isInstanceOf(EmploymentVerificationEvent.class);
      assertThat(publishedEvents.get(1)).isInstanceOf(DataQualityAlertEvent.class);

      DataQualityAlertEvent alertEvent = (DataQualityAlertEvent) publishedEvents.get(1);
      assertThat(alertEvent.ruleCode()).isEqualTo("DQ-EMPLOYMENT-VERIFICATION-FAILED");
      assertThat(alertEvent.severity()).isEqualTo("HIGH");
      assertThat(alertEvent.issueStatus()).isEqualTo("OPEN");

      eventListener.onDataQualityAlertEvent(alertEvent);
    }
  }

  // =========================================================================
  // Stage E: Retention, Exit and Unemployment Orchestration
  // =========================================================================

  @Nested
  @DisplayName("Stage E: Employment Exit & Unemployment Orchestration")
  class StageEExitUnemploymentTests {

    @Test
    @DisplayName("Records exit and unemployment atomically, publishing TraineeLifecycleEvent")
    void testOrchestrateEmploymentExitAndUnemploymentSuccess() {
      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();

      EmploymentExitEventResponse exitResp = mock(EmploymentExitEventResponse.class);
      when(exitResp.id()).thenReturn(EXIT_EVENT_ID);
      when(exitResp.traineeId()).thenReturn(TRAINEE_ID);

      TraineeUnemploymentEventResponse unempResp = mock(TraineeUnemploymentEventResponse.class);
      when(unempResp.id()).thenReturn(333L);

      EmploymentExitAndUnemploymentResponse mockResponse =
          new EmploymentExitAndUnemploymentResponse(exitResp, unempResp);

      when(employmentLifecycleWorkflowService.recordExitAndInitiateUnemployment(request))
          .thenReturn(mockResponse);

      EmploymentExitAndUnemploymentResponse response =
          orchestratorService.orchestrateEmploymentExitAndUnemployment(request);

      assertThat(response).isNotNull();
      assertThat(response.exitEvent().id()).isEqualTo(EXIT_EVENT_ID);
      assertThat(response.unemploymentEvent().id()).isEqualTo(333L);

      ArgumentCaptor<TraineeLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(TraineeLifecycleEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TraineeLifecycleEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("EMPLOYMENT_EXIT");
      assertThat(published.getEntityId()).isEqualTo(TRAINEE_ID);
      assertThat(published.previousStatus()).isEqualTo("EMPLOYED");
      assertThat(published.newStatus()).isEqualTo("UNEMPLOYED");

      eventListener.onTraineeLifecycleEvent(published);
    }

    @Test
    @DisplayName("Propagates failure and halts event dispatching when exit/unemployment workflow fails")
    void testOrchestrateEmploymentExitAndUnemploymentRollbackOnWorkflowFailure() {
      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();

      doThrow(new ConflictException("Active unemployment already exists", "DUPLICATE_UNEMPLOYMENT"))
          .when(employmentLifecycleWorkflowService).recordExitAndInitiateUnemployment(request);

      assertThatThrownBy(() -> orchestratorService.orchestrateEmploymentExitAndUnemployment(request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("Active unemployment already exists");

      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  // =========================================================================
  // Stage F: Skill-Gap Remediation and Actionable Handoff
  // =========================================================================

  @Nested
  @DisplayName("Stage F: Skill-Gap Remediation Orchestration")
  class StageFRemediationTests {

    @Test
    @DisplayName("Executes assessment with gaps and publishes TraineeLifecycleEvent when assessment data is present")
    void testTriggerSkillGapRemediationWithAssessmentData() {
      RecordSkillGapAssessmentWithGapsRequest assessmentReq = new RecordSkillGapAssessmentWithGapsRequest();
      CreateSkillGapAssessmentRequest inner = new CreateSkillGapAssessmentRequest();
      inner.setTraineeId(TRAINEE_ID);
      assessmentReq.setAssessmentRequest(inner);

      RemediationTriggerRequest request = new RemediationTriggerRequest(
          TRAINEE_ID, "EMPLOYMENT_EXIT", EXIT_EVENT_ID, assessmentReq, "Exit remediation"
      );

      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);

      SkillGapAssessmentResponse assessmentResp = mock(SkillGapAssessmentResponse.class);
      when(assessmentResp.id()).thenReturn(ASSESSMENT_ID);

      SkillGapResponse gapResp = mock(SkillGapResponse.class);

      RecordSkillGapAssessmentWithGapsResponse mockResponse =
          new RecordSkillGapAssessmentWithGapsResponse(assessmentResp, List.of(gapResp), Collections.emptyList());

      when(skillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations(assessmentReq))
          .thenReturn(mockResponse);

      RemediationTriggerResponse response = orchestratorService.triggerSkillGapRemediation(request);

      assertThat(response).isNotNull();
      assertThat(response.remediationStatus()).isEqualTo("EVALUATED");
      assertThat(response.assessmentResponse()).isNotNull();

      ArgumentCaptor<TraineeLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(TraineeLifecycleEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      TraineeLifecycleEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("REMEDIATION_EVALUATED");
      assertThat(published.getEntityId()).isEqualTo(TRAINEE_ID);
      assertThat(published.newStatus()).isEqualTo("IN_REMEDIATION");

      eventListener.onTraineeLifecycleEvent(published);
    }

    @Test
    @DisplayName("Throws BadRequestException when inner assessment trainee mismatches target trainee")
    void testTriggerSkillGapRemediationThrowsWhenTraineeMismatch() {
      RecordSkillGapAssessmentWithGapsRequest assessmentReq = new RecordSkillGapAssessmentWithGapsRequest();
      CreateSkillGapAssessmentRequest inner = new CreateSkillGapAssessmentRequest();
      inner.setTraineeId(9999L);
      assessmentReq.setAssessmentRequest(inner);

      RemediationTriggerRequest request = new RemediationTriggerRequest(
          TRAINEE_ID, "EMPLOYMENT_EXIT", EXIT_EVENT_ID, assessmentReq, "Exit remediation"
      );

      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);

      assertThatThrownBy(() -> orchestratorService.triggerSkillGapRemediation(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Assessment request trainee ID does not match remediation target trainee");

      verify(skillGapRemediationWorkflowService, never()).recordAssessmentWithGapsAndRecommendations(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Performs safe pending handoff and publishes CommunicationTriggerEvent when assessment data is absent")
    void testTriggerSkillGapRemediationWithoutAssessmentDataPendingHandoff() {
      RemediationTriggerRequest request = new RemediationTriggerRequest(
          TRAINEE_ID, "VERIFICATION_FAILURE"
      );

      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);

      RemediationTriggerResponse response = orchestratorService.triggerSkillGapRemediation(request);

      assertThat(response).isNotNull();
      assertThat(response.remediationStatus()).isEqualTo("PENDING_ASSESSMENT_DATA");
      assertThat(response.assessmentResponse()).isNull();
      assertThat(response.remarks()).contains("VERIFICATION_FAILURE");

      ArgumentCaptor<CommunicationTriggerEvent> eventCaptor = ArgumentCaptor.forClass(CommunicationTriggerEvent.class);
      verify(eventPublisher).publishEvent(eventCaptor.capture());
      CommunicationTriggerEvent published = eventCaptor.getValue();

      assertThat(published.getEventType()).isEqualTo("REMEDIATION_ACTION_REQUIRED");
      assertThat(published.templateCode()).isEqualTo("SKILL_GAP_ASSESSMENT_PENDING");

      eventListener.onCommunicationTriggerEvent(published);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when remediation target trainee does not exist")
    void testTriggerSkillGapRemediationThrowsWhenTraineeNotFound() {
      RemediationTriggerRequest request = new RemediationTriggerRequest(
          9999L, "EMPLOYMENT_EXIT"
      );

      when(traineeRepository.existsById(9999L)).thenReturn(false);

      assertThatThrownBy(() -> orchestratorService.triggerSkillGapRemediation(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Trainee");

      verify(eventPublisher, never()).publishEvent(any());
    }
  }

  // =========================================================================
  // Unified Read-Only Candidate Lifecycle Journey Summary
  // =========================================================================

  @Nested
  @DisplayName("Unified Candidate Lifecycle Journey Summary")
  class LifecycleSummaryTests {

    @Test
    @DisplayName("Aggregates all lifecycle stages into a unified read-only journey response")
    void testGetCandidateLifecycleSummaryAggregatesAllStages() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);

      TraineeConsentResponse consent = new TraineeConsentResponse();
      consent.setId(10L);
      consent.setTraineeId(TRAINEE_ID);
      when(traineeConsentService.getConsentsByTraineeId(TRAINEE_ID)).thenReturn(List.of(consent));

      TrainingEnrollmentResponse enrollment = new TrainingEnrollmentResponse();
      enrollment.setId(ENROLLMENT_ID);
      enrollment.setTraineeId(TRAINEE_ID);
      when(trainingEnrollmentService.getByTraineeId(TRAINEE_ID)).thenReturn(List.of(enrollment));

      TraineePlacementEmploymentSummaryResponse placementSummary =
          new TraineePlacementEmploymentSummaryResponse(TRAINEE_ID, Collections.emptyList(), Collections.emptyList());
      when(placementEmploymentWorkflowService.getTraineePlacementEmploymentSummary(TRAINEE_ID))
          .thenReturn(placementSummary);

      TraineeCareerTimelineResponse careerTimeline =
          new TraineeCareerTimelineResponse(TRAINEE_ID, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
      when(employmentLifecycleWorkflowService.getTraineeCareerTimeline(TRAINEE_ID))
          .thenReturn(careerTimeline);

      TraineeSkillGapProfileResponse skillGapProfile =
          new TraineeSkillGapProfileResponse(TRAINEE_ID, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
      when(skillGapRemediationWorkflowService.getTraineeSkillGapProfile(TRAINEE_ID))
          .thenReturn(skillGapProfile);

      CandidateLifecycleSummaryResponse summary = orchestratorService.getCandidateLifecycleSummary(TRAINEE_ID);

      assertThat(summary).isNotNull();
      assertThat(summary.traineeId()).isEqualTo(TRAINEE_ID);
      assertThat(summary.consents()).hasSize(1);
      assertThat(summary.enrollments()).hasSize(1);
      assertThat(summary.placementEmploymentSummary()).isNotNull();
      assertThat(summary.careerTimeline()).isNotNull();
      assertThat(summary.skillGapProfile()).isNotNull();

      verify(scopeValidator).validateTraineeScope(TRAINEE_ID);
    }
  }

  // =========================================================================
  // Event Listener Null Safety & Robustness Tests
  // =========================================================================

  @Nested
  @DisplayName("CandidateLifecycleEventListener Safety Tests")
  class EventListenerSafetyTests {

    @Test
    @DisplayName("Handles null events gracefully without throwing exceptions")
    void testCandidateLifecycleEventListenerHandlesNullEventsGracefully() {
      eventListener.onTraineeLifecycleEvent(null);
      eventListener.onPlacementLifecycleEvent(null);
      eventListener.onEmploymentVerificationEvent(null);
      eventListener.onDataQualityAlertEvent(null);
      eventListener.onCommunicationTriggerEvent(null);
    }
  }
}
