package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
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
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.EmploymentLifecycleWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.FollowupEngagementWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementEmploymentWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingOutcomeWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.VerificationWorkflowServiceImpl;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Phase 25.12 Comprehensive Regression Test Suite:
 * Evidence-based cross-domain data consistency, pre-mutation validation,
 * absent association rejection, historical data boundaries, and analytics read-only immutability.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrossDomainDataConsistencyRegressionTest {

  @Mock private WorkflowSecurityScopeValidator scopeValidator;

  // =========================================================================
  // Domain 1: Placement to Employment Transition
  // =========================================================================
  @Nested
  @DisplayName("1. Placement to Employment Cross-Domain Consistency")
  class PlacementToEmploymentTests {

    @Mock private PlacementRecordService placementRecordService;
    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private SalaryHistoryService salaryHistoryService;
    @Mock private EmploymentRecordRepository employmentRecordRepository;
    @Mock private RefPlacementStatusRepository refPlacementStatusRepository;
    @Mock private RefJoiningStatusRepository refJoiningStatusRepository;

    private PlacementEmploymentWorkflowServiceImpl workflowService;
    private PlacementRecordResponse samplePlacement;

    @BeforeEach
    void setUp() {
      workflowService = new PlacementEmploymentWorkflowServiceImpl(
          placementRecordService,
          employmentRecordService,
          salaryHistoryService,
          employmentRecordRepository,
          refPlacementStatusRepository,
          refJoiningStatusRepository,
          scopeValidator
      );

      samplePlacement = mock(PlacementRecordResponse.class);
      when(samplePlacement.id()).thenReturn(100L);
      when(samplePlacement.traineeId()).thenReturn(50L);
      when(samplePlacement.employerId()).thenReturn(10L);
      when(samplePlacement.enrollmentId()).thenReturn(200L);
      when(samplePlacement.placementStatusCode()).thenReturn("OFFERED");
      when(samplePlacement.joiningStatusCode()).thenReturn("JOINED");
      when(samplePlacement.deletedAt()).thenReturn(null);

      when(placementRecordService.getPlacementRecordById(100L)).thenReturn(samplePlacement);
      when(employmentRecordRepository.findByPlacementRecordId(100L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Fails closed upfront when employment record employer differs from placement employer")
    void testEmployerPlacementMismatch_rejectedUpfront() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(100L);
      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      empReq.setEmployerId(999L); // Mismatched employer (placement has 10L)
      request.setEmploymentRecordRequest(empReq);

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_PLACEMENT_MISMATCH");

      verify(placementRecordService, never()).updatePlacementRecord(anyLong(), any());
      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Fails closed upfront when employment record placementId differs from request placementId")
    void testPlacementRecordMismatch_rejectedUpfront() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(100L);
      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      empReq.setPlacementId(999L); // Mismatched placement ID
      request.setEmploymentRecordRequest(empReq);

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_RECORD_MISMATCH");

      verify(placementRecordService, never()).updatePlacementRecord(anyLong(), any());
      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Fails closed upfront when employment record enrollmentId differs from placement enrollmentId")
    void testEnrollmentPlacementMismatch_rejectedUpfront() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(100L);
      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      empReq.setEnrollmentId(888L); // Mismatched enrollment (placement has 200L)
      request.setEmploymentRecordRequest(empReq);

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_PLACEMENT_MISMATCH");

      verify(placementRecordService, never()).updatePlacementRecord(anyLong(), any());
      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Fails closed upfront when caller supplies enrollmentId but placement has absent enrollment (null)")
    void testEnrollmentSuppliedWhenPlacementHasNoEnrollment_rejectedUpfront() {
      when(samplePlacement.enrollmentId()).thenReturn(null); // Placement has no enrollment

      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(100L);
      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      empReq.setEnrollmentId(888L); // Caller supplies enrollment
      request.setEmploymentRecordRequest(empReq);

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_PLACEMENT_MISMATCH");

      verify(placementRecordService, never()).updatePlacementRecord(anyLong(), any());
      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }

    @Test
    @DisplayName("Fails closed upfront when initial salary request has pre-set employmentId")
    void testSalaryEmploymentIdPreset_rejectedUpfront() {
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(100L);
      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      request.setEmploymentRecordRequest(empReq);

      CreateSalaryHistoryRequest salaryReq = new CreateSalaryHistoryRequest();
      salaryReq.setEmploymentId(777L); // Pre-set employmentId
      request.setInitialSalaryRequest(salaryReq);

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_ID_PRESET");

      verify(placementRecordService, never()).updatePlacementRecord(anyLong(), any());
      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }
  }

  // =========================================================================
  // Domain 2: Training Outcome Workflow
  // =========================================================================
  @Nested
  @DisplayName("2. Training Outcome Cross-Domain Consistency")
  class TrainingOutcomeTests {

    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private TraineeAssessmentService traineeAssessmentService;
    @Mock private AssessmentResultService assessmentResultService;
    @Mock private CertificationService certificationService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;

    private TrainingOutcomeWorkflowServiceImpl workflowService;
    private TrainingEnrollmentResponse sampleEnrollment;

    @BeforeEach
    void setUp() {
      workflowService = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService,
          traineeAssessmentService,
          assessmentResultService,
          certificationService,
          courseSkillRepository,
          skillGapRepository,
          refSkillGapStatusRepository,
          scopeValidator
      );

      sampleEnrollment = new TrainingEnrollmentResponse();
      sampleEnrollment.setId(200L);
      sampleEnrollment.setTraineeId(50L);
      sampleEnrollment.setProviderId(10L);
      sampleEnrollment.setCourseId(30L);
      when(trainingEnrollmentService.getById(200L)).thenReturn(sampleEnrollment);
    }

    @Test
    @DisplayName("Fails closed upfront before mutation when supplied traineeAssessmentId cannot be resolved")
    void testUnresolvableAssessment_failsClosed_noMutation() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(200L);

      CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest();
      resultReq.setTraineeAssessmentId(999L);
      request.setAssessmentResultRequest(resultReq);

      // Trainee assessment cannot be resolved (returns null)
      when(traineeAssessmentService.getById(999L)).thenReturn(null);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
    }

    @Test
    @DisplayName("Fails closed upfront before mutation when trainee assessment has null enrollment")
    void testAssessmentEnrollmentNull_failsClosed_noMutation() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(200L);

      CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest();
      resultReq.setTraineeAssessmentId(900L);
      request.setAssessmentResultRequest(resultReq);

      TraineeAssessmentResponse nullEnrolTa = new TraineeAssessmentResponse();
      nullEnrolTa.setId(900L);
      nullEnrolTa.setEnrollmentId(null); // Null enrollment!
      nullEnrolTa.setTraineeId(50L);
      when(traineeAssessmentService.getById(900L)).thenReturn(nullEnrolTa);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_ASSESSMENT_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Fails closed upfront when trainee assessment belongs to a different enrollment")
    void testEnrollmentAssessmentMismatch_rejectedUpfront() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(200L);

      CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest();
      resultReq.setTraineeAssessmentId(900L);
      request.setAssessmentResultRequest(resultReq);

      TraineeAssessmentResponse mismatchTa = new TraineeAssessmentResponse();
      mismatchTa.setId(900L);
      mismatchTa.setEnrollmentId(999L); // Different enrollment!
      mismatchTa.setTraineeId(50L);
      when(traineeAssessmentService.getById(900L)).thenReturn(mismatchTa);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_ASSESSMENT_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
    }

    @Test
    @DisplayName("Fails closed upfront when trainee assessment belongs to a different trainee")
    void testAssessmentTraineeMismatch_rejectedUpfront() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(200L);

      CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest();
      resultReq.setTraineeAssessmentId(900L);
      request.setAssessmentResultRequest(resultReq);

      TraineeAssessmentResponse mismatchTa = new TraineeAssessmentResponse();
      mismatchTa.setId(900L);
      mismatchTa.setEnrollmentId(200L);
      mismatchTa.setTraineeId(999L); // Different trainee!
      when(traineeAssessmentService.getById(900L)).thenReturn(mismatchTa);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TRAINEE_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
    }

    @Test
    @DisplayName("Fails closed upfront when certification request specifies mismatched enrollment")
    void testCertificationEnrollmentMismatch_rejectedUpfront() {
      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(200L);

      CreateAssessmentResultRequest resultReq = new CreateAssessmentResultRequest();
      resultReq.setTraineeId(50L);
      request.setAssessmentResultRequest(resultReq);

      CreateCertificationRequest certReq = new CreateCertificationRequest();
      certReq.setEnrollmentId(999L); // Different enrollment!
      certReq.setTraineeId(50L);
      request.setCertificationRequest(certReq);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_CERTIFICATION_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
    }
  }

  // =========================================================================
  // Domain 3: Employment Lifecycle Workflow
  // =========================================================================
  @Nested
  @DisplayName("3. Employment Lifecycle Cross-Domain Consistency")
  class EmploymentLifecycleTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private EmploymentExitEventService employmentExitEventService;
    @Mock private TraineeUnemploymentEventService traineeUnemploymentEventService;
    @Mock private TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;
    @Mock private SalaryHistoryService salaryHistoryService;

    private EmploymentLifecycleWorkflowServiceImpl workflowService;
    private EmploymentRecordResponse sampleEmp;

    @BeforeEach
    void setUp() {
      workflowService = new EmploymentLifecycleWorkflowServiceImpl(
          employmentRecordService,
          employmentExitEventService,
          traineeUnemploymentEventService,
          salaryHistoryService,
          traineeUnemploymentEventRepository,
          scopeValidator
      );

      sampleEmp = mock(EmploymentRecordResponse.class);
      when(sampleEmp.id()).thenReturn(10L);
      when(sampleEmp.traineeId()).thenReturn(50L);
      when(sampleEmp.employerId()).thenReturn(100L);
      when(sampleEmp.enrollmentId()).thenReturn(200L);
      when(sampleEmp.placementId()).thenReturn(300L);
    }

    @Test
    @DisplayName("Fails closed upfront when unemployment event preceding employment differs from exit employment")
    void testEmploymentUnemploymentMismatch_rejectedUpfront() {
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(10L);
      exitReq.setTraineeId(50L);
      exitReq.setSeparationDate(LocalDate.of(2026, 3, 1));
      request.setExitEventRequest(exitReq);

      CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest();
      unempReq.setTraineeId(50L);
      unempReq.setPrecedingEmploymentId(999L); // Different preceding employment!
      request.setUnemploymentEventRequest(unempReq);

      assertThatThrownBy(() -> workflowService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_UNEMPLOYMENT_MISMATCH");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Fails closed upfront when unemployment event enrollment differs from exit employment enrollment")
    void testEnrollmentEmploymentMismatch_rejectedUpfront() {
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(10L);
      exitReq.setTraineeId(50L);
      exitReq.setSeparationDate(LocalDate.of(2026, 3, 1));
      request.setExitEventRequest(exitReq);

      CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest();
      unempReq.setTraineeId(50L);
      unempReq.setEnrollmentId(999L); // Different enrollment!
      request.setUnemploymentEventRequest(unempReq);

      assertThatThrownBy(() -> workflowService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_EMPLOYMENT_MISMATCH");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Fails closed upfront when caller supplies enrollmentId but employment record has no enrollment (null)")
    void testUnemploymentEnrollmentSuppliedWhenEmploymentHasNoEnrollment_rejectedUpfront() {
      when(sampleEmp.enrollmentId()).thenReturn(null); // Direct employment with no enrollment
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(10L);
      exitReq.setTraineeId(50L);
      exitReq.setSeparationDate(LocalDate.of(2026, 3, 1));
      request.setExitEventRequest(exitReq);

      CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest();
      unempReq.setTraineeId(50L);
      unempReq.setEnrollmentId(888L); // Caller supplies enrollment
      request.setUnemploymentEventRequest(unempReq);

      assertThatThrownBy(() -> workflowService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_EMPLOYMENT_MISMATCH");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Fails closed upfront when caller supplies placementId but employment record has no placement (null)")
    void testUnemploymentPlacementSuppliedWhenEmploymentHasNoPlacement_rejectedUpfront() {
      when(sampleEmp.placementId()).thenReturn(null); // Direct employment with no placement
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(10L);
      exitReq.setTraineeId(50L);
      exitReq.setSeparationDate(LocalDate.of(2026, 3, 1));
      request.setExitEventRequest(exitReq);

      CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest();
      unempReq.setTraineeId(50L);
      unempReq.setPlacementId(777L); // Caller supplies placement
      request.setUnemploymentEventRequest(unempReq);

      assertThatThrownBy(() -> workflowService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Fails closed upfront when unemployment event has pre-set exitEventId")
    void testExitEventIdPreset_rejectedUpfront() {
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
      exitReq.setEmploymentId(10L);
      exitReq.setTraineeId(50L);
      exitReq.setSeparationDate(LocalDate.of(2026, 3, 1));
      request.setExitEventRequest(exitReq);

      CreateTraineeUnemploymentEventRequest unempReq = new CreateTraineeUnemploymentEventRequest();
      unempReq.setTraineeId(50L);
      unempReq.setEmploymentExitEventId(888L); // Pre-set exitEventId
      request.setUnemploymentEventRequest(unempReq);

      assertThatThrownBy(() -> workflowService.recordExitAndInitiateUnemployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EXIT_EVENT_ID_PRESET");

      verify(employmentExitEventService, never()).createExitEvent(any());
      verify(traineeUnemploymentEventService, never()).createUnemploymentEvent(any());
    }

    @Test
    @DisplayName("Fails closed upfront in unemployment-to-employment transition when initial salary has pre-set employmentId")
    void testTransitionUnemploymentToEmployment_salaryEmploymentIdPreset_rejectedUpfront() {
      TraineeUnemploymentEvent unempEvent = mock(TraineeUnemploymentEvent.class);
      Trainee trainee = mock(Trainee.class);
      when(trainee.getId()).thenReturn(50L);
      when(unempEvent.getTrainee()).thenReturn(trainee);
      when(unempEvent.getIsCurrent()).thenReturn(true);
      when(unempEvent.getEndDate()).thenReturn(null);
      when(unempEvent.getSucceedingEmployment()).thenReturn(null);
      when(unempEvent.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
      when(traineeUnemploymentEventRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(unempEvent));

      UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
      request.setUnemploymentEventId(500L);

      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setTraineeId(50L);
      empReq.setEmployerId(10L);
      empReq.setStartDate(LocalDate.of(2026, 2, 1));
      request.setNewEmploymentRequest(empReq);

      CreateSalaryHistoryRequest salaryReq = new CreateSalaryHistoryRequest();
      salaryReq.setEmploymentId(777L); // Pre-set employmentId
      request.setInitialSalaryRequest(salaryReq);

      assertThatThrownBy(() -> workflowService.transitionUnemploymentToEmployment(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_ID_PRESET");

      verify(employmentRecordService, never()).createEmploymentRecord(any());
    }
  }

  // =========================================================================
  // Domain 4: Verification Workflow
  // =========================================================================
  @Nested
  @DisplayName("4. Verification Cross-Domain Consistency")
  class VerificationWorkflowTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
    @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
    @Mock private EmploymentVerificationService employmentVerificationService;
    @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
    @Mock private UserRepository userRepository;
    @Mock private RefUserStatusRepository refUserStatusRepository;

    private VerificationWorkflowServiceImpl workflowService;
    private SurveyResponseResponse sampleSurvey;
    private EmploymentRecordResponse sampleEmp;

    @BeforeEach
    void setUp() {
      workflowService = new VerificationWorkflowServiceImpl(
          employmentRecordService,
          followupTaskService,
          surveyResponseService,
          employmentVerificationRequestService,
          employmentVerificationAttemptService,
          employmentVerificationService,
          employmentVerificationEvidenceService,
          userRepository,
          refUserStatusRepository,
          scopeValidator
      );

      sampleSurvey = new SurveyResponseResponse();
      sampleSurvey.setId(300L);
      sampleSurvey.setTraineeId(50L);
      sampleSurvey.setFollowupTaskId(600L);

      sampleEmp = mock(EmploymentRecordResponse.class);
      when(sampleEmp.id()).thenReturn(10L);
      when(sampleEmp.traineeId()).thenReturn(50L);
      when(sampleEmp.employerId()).thenReturn(100L);
      when(sampleEmp.placementId()).thenReturn(300L);
    }

    @AfterEach
    void tearDown() {
      SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when employerId contradicts employment")
    void testSurveyEmployerMismatch_rejectedUpfront() {
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      details.setEmployerId(999L); // Contradicts sampleEmp (100L)
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when caller supplies followupTaskId but survey has none (null)")
    void testInitiateVerification_taskSuppliedWhenSurveyHasNoTask_rejectedUpfront() {
      sampleSurvey.setFollowupTaskId(null); // Survey not linked to followup task
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      details.setFollowupTaskId(888L); // Caller supplies followupTaskId
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("FOLLOWUP_TASK_MISMATCH");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when caller supplies placementRecordId but employment has no placement (null)")
    void testInitiateVerification_placementSuppliedWhenEmploymentHasNoPlacement_rejectedUpfront() {
      when(sampleEmp.placementId()).thenReturn(null); // Direct employment
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      details.setPlacementRecordId(777L); // Caller supplies placement
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when linked followup task cannot be resolved")
    void testInitiateVerification_unresolvedFollowupTaskParent_rejectedUpfront() {
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);
      when(followupTaskService.getFollowupTaskById(600L)).thenReturn(null); // Unresolved parent task!

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when followup task placement mismatch and employment has no placement (null)")
    void testInitiateVerification_taskPlacementMismatchWithEmploymentPlacementNull_rejectedUpfront() {
      when(sampleEmp.placementId()).thenReturn(null); // Direct employment has no placement
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      FollowupTaskResponse task = new FollowupTaskResponse();
      task.setId(600L);
      task.setTraineeId(50L);
      task.setEmploymentId(10L);
      task.setPlacementId(555L); // Task has placement, but employment has null!
      when(followupTaskService.getFollowupTaskById(600L)).thenReturn(task);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_TASK_MISMATCH");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when followup task employment contradicts target employment")
    void testFollowupTaskEmploymentMismatch_rejectedUpfront() {
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      FollowupTaskResponse task = new FollowupTaskResponse();
      task.setId(600L);
      task.setTraineeId(50L);
      task.setEmploymentId(999L); // Points to a different employment record!
      when(followupTaskService.getFollowupTaskById(600L)).thenReturn(task);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_TASK_MISMATCH");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request belongs to different employment")
    void testVerdictVerificationRequestEmploymentMismatch_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(999L); // Different employment record!
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L); // Mismatch with vReq (999L)
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYMENT_VERIFICATION_REQUEST_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when caller supplies placementRecordId but employment has no placement (null)")
    void testRecordVerificationVerdict_placementSuppliedWhenEmploymentHasNoPlacement_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      when(sampleEmp.placementId()).thenReturn(null); // Direct employment
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setPlacementRecordId(777L); // Caller supplies placement
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verdict status contradicts employment record status")
    void testVerificationStatusConflict_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      verReq.setRecordVerificationStatusId(1L); // VERIFIED status
      request.setVerificationRequest(verReq);
      request.setEmploymentRecordStatusId(2L); // Contradictory UNVERIFIED status

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("VERIFICATION_STATUS_CONFLICT");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when linked followup task has null traineeId")
    void testInitiateVerification_linkedFollowupTaskNullTrainee_rejectedUpfront() {
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      FollowupTaskResponse taskWithoutTrainee = new FollowupTaskResponse();
      taskWithoutTrainee.setId(600L);
      taskWithoutTrainee.setTraineeId(null); // Missing required trainee association!
      when(followupTaskService.getFollowupTaskById(600L)).thenReturn(taskWithoutTrainee);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_REQUIRED");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("initiateVerificationFromSurvey fails closed upfront when employment record has null traineeId")
    void testInitiateVerification_employmentNullTrainee_rejectedUpfront() {
      when(sampleEmp.traineeId()).thenReturn(null); // Missing required trainee on employment!
      when(surveyResponseService.getSurveyResponseById(300L)).thenReturn(sampleSurvey);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest();
      request.setSurveyResponseId(300L);

      CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
      details.setEmploymentRecordId(10L);
      request.setVerificationRequestDetails(details);

      assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_REQUIRED");

      verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when employment record has null traineeId")
    void testRecordVerificationVerdict_employmentNullTrainee_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      when(sampleEmp.traineeId()).thenReturn(null); // Missing required trainee on employment!
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_REQUIRED");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request has null traineeId")
    void testRecordVerificationVerdict_verificationRequestNullTrainee_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(null); // Missing required trainee association!
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_REQUIRED");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request employer contradicts target employment employer")
    void testRecordVerificationVerdict_vReqEmployerMismatch_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(888L); // Different employer!
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request has null employerId")
    void testRecordVerificationVerdict_vReqNullEmployer_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(null); // Missing required employer on vReq!
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_REQUIRED");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when employment has placement but verification request placement is null")
    void testRecordVerificationVerdict_vReqNullPlacementWhenEmploymentHasPlacement_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(100L);
      when(vReq.placementRecordId()).thenReturn(null); // Missing required placement when employment has placement 300L!
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request placement contradicts employment placement")
    void testRecordVerificationVerdict_vReqPlacementMismatchWhenEmploymentHasPlacement_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(100L);
      when(vReq.placementRecordId()).thenReturn(999L); // Contradicts sampleEmp.placementId (300L)
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize fails closed when verification request supplies placement but employment has no placement (null)")
    void testRecordVerificationVerdict_vReqSuppliesPlacementWhenEmploymentHasNoPlacement_rejectedUpfront() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      when(sampleEmp.placementId()).thenReturn(null); // Direct employment
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(100L);
      when(vReq.placementRecordId()).thenReturn(777L); // Supplies placement when employment has none
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      assertThatThrownBy(() -> workflowService.recordVerificationVerdictAndSynchronize(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PLACEMENT_EMPLOYMENT_MISMATCH");

      verify(employmentVerificationService, never()).createEmploymentVerification(any());
      verify(employmentVerificationEvidenceService, never()).createVerificationEvidence(any());
      verify(employmentVerificationRequestService, never()).updateVerificationRequest(any(), any());
      verify(employmentRecordService, never()).updateEmploymentRecord(any(), any());
    }

    @Test
    @DisplayName("recordVerificationVerdictAndSynchronize preserves genuinely optional placement when both employment and verification request have null placement")
    void testRecordVerificationVerdict_preservesGenuinelyOptionalPlacementWhenBothAreNull() {
      UserPrincipal principal = new UserPrincipal(1L, "admin");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

      User activeUser = new User();
      activeUser.setId(1L);
      activeUser.setUserStatusId(1L);
      when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

      RefUserStatus activeStatus = new RefUserStatus();
      activeStatus.setStatusCode("ACTIVE");
      when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(activeStatus));

      when(sampleEmp.placementId()).thenReturn(null); // Genuinely optional direct employment
      when(employmentRecordService.getEmploymentRecordById(10L)).thenReturn(sampleEmp);

      EmploymentVerificationRequestResponse vReq = mock(EmploymentVerificationRequestResponse.class);
      when(vReq.employmentRecordId()).thenReturn(10L);
      when(vReq.traineeId()).thenReturn(50L);
      when(vReq.employerId()).thenReturn(100L);
      when(vReq.placementRecordId()).thenReturn(null); // Genuinely optional null placement
      when(employmentVerificationRequestService.getVerificationRequestById(700L)).thenReturn(vReq);

      EmploymentVerificationResponse mockVerification = mock(EmploymentVerificationResponse.class);
      when(mockVerification.id()).thenReturn(999L);
      when(employmentVerificationService.createEmploymentVerification(any())).thenReturn(mockVerification);

      RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
      CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
      verReq.setEmploymentVerificationRequestId(700L);
      verReq.setEmploymentRecordId(10L);
      request.setVerificationRequest(verReq);

      RecordVerificationVerdictResponse result = workflowService.recordVerificationVerdictAndSynchronize(request);

      assertThat(result).isNotNull();
      assertThat(result.verification().id()).isEqualTo(999L);
      verify(employmentVerificationService).createEmploymentVerification(verReq);
      assertThat(verReq.getPlacementRecordId()).isNull(); // Genuinely optional null preserved
    }
  }

  // =========================================================================
  // Domain 5: Followup Engagement Workflow
  // =========================================================================
  @Nested
  @DisplayName("5. Followup Engagement Cross-Domain Consistency")
  class FollowupEngagementTests {

    @Mock private FollowupTaskService followupTaskService;
    @Mock private SurveyResponseService surveyResponseService;
    @Mock private CommunicationLogService communicationLogService;
    @Mock private TraineeConsentService traineeConsentService;

    private FollowupEngagementWorkflowServiceImpl workflowService;
    private FollowupTaskResponse sampleTask;

    @BeforeEach
    void setUp() {
      workflowService = new FollowupEngagementWorkflowServiceImpl(
          followupTaskService,
          surveyResponseService,
          communicationLogService,
          traineeConsentService,
          scopeValidator
      );

      sampleTask = new FollowupTaskResponse();
      sampleTask.setId(600L);
      sampleTask.setTraineeId(50L);
      sampleTask.setEnrollmentId(200L);
      sampleTask.setSurveyId(40L);
      when(followupTaskService.getFollowupTaskById(600L)).thenReturn(sampleTask);
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse fails closed upfront when survey request task differs from target task")
    void testFollowupTaskMismatch_rejectedUpfront() {
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(600L);

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setFollowupTaskId(999L); // Different task!
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("FOLLOWUP_TASK_MISMATCH");

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse fails closed upfront when survey response enrollment differs from task enrollment")
    void testEnrollmentTaskMismatch_rejectedUpfront() {
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(600L);

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setEnrollmentId(999L); // Different enrollment!
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TASK_MISMATCH");

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse fails closed upfront when caller supplies enrollmentId but task has no enrollment (null)")
    void testSurveyEnrollmentSuppliedWhenTaskHasNoEnrollment_rejectedUpfront() {
      sampleTask.setEnrollmentId(null); // Task has no enrollment

      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(600L);

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setEnrollmentId(888L); // Caller supplies enrollment
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TASK_MISMATCH");

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse fails closed upfront when survey response surveyId differs from task surveyId")
    void testSurveyTaskMismatch_rejectedUpfront() {
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(600L);

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setSurveyId(999L); // Different survey!
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SURVEY_TASK_MISMATCH");

      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("recordFollowupSurveyResponse preserves legitimate optional relationship when task has no pre-associated survey")
    void testSurveyIdPreservedWhenTaskSurveyOptional() {
      sampleTask.setSurveyId(null); // Task has no pre-associated surveyId (legitimate optional relationship)

      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(600L);

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setSurveyId(777L); // Caller legitimately supplies surveyId
      request.setSurveyResponseRequest(surveyReq);

      SurveyResponseResponse mockResponse = new SurveyResponseResponse();
      mockResponse.setId(10L);
      mockResponse.setSurveyId(777L);
      when(surveyResponseService.createSurveyResponse(any())).thenReturn(mockResponse);

      RecordFollowupSurveyResponseResponse result = workflowService.recordFollowupSurveyResponse(request);

      assertThat(result).isNotNull();
      assertThat(result.surveyResponse().getId()).isEqualTo(10L);
      verify(surveyResponseService).createSurveyResponse(surveyReq);
    }

    @Test
    @DisplayName("recordEngagementAttempt fails closed upfront when communication log task differs from target task")
    void testCommunicationLogTaskMismatch_rejectedUpfront() {
      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(600L);

      CreateCommunicationLogRequest commReq = new CreateCommunicationLogRequest();
      commReq.setFollowupTaskId(999L); // Different task!
      request.setCommunicationLogRequest(commReq);

      assertThatThrownBy(() -> workflowService.recordEngagementAttempt(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("FOLLOWUP_TASK_MISMATCH");

      verify(communicationLogService, never()).logCommunication(any());
    }
  }

  // =========================================================================
  // Domain 6: Analytics Views Immutability & Read-Only Invariants
  // =========================================================================
  @Nested
  @DisplayName("6. Analytics Views Immutability and Read-Only Boundaries")
  class AnalyticsViewImmutabilityTests {

    private final List<String> analyticsViewEntityClassNames = List.of(
        "in.gov.sih.sih26135.entity.analytics.AttritionReasonSummary",
        "in.gov.sih.sih26135.entity.analytics.CourseOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.DistrictOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.EmployerHiringSummary",
        "in.gov.sih.sih26135.entity.analytics.EmploymentExitFact",
        "in.gov.sih.sih26135.entity.analytics.EmploymentFact",
        "in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact",
        "in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact",
        "in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact",
        "in.gov.sih.sih26135.entity.analytics.FollowupFact",
        "in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.PlacementFact",
        "in.gov.sih.sih26135.entity.analytics.ProgramOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.ProviderOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact",
        "in.gov.sih.sih26135.entity.analytics.SkillGapFact",
        "in.gov.sih.sih26135.entity.analytics.SkillGapSummary",
        "in.gov.sih.sih26135.entity.analytics.SurveyResponseFact",
        "in.gov.sih.sih26135.entity.analytics.TraineeOutcomeSummary",
        "in.gov.sih.sih26135.entity.analytics.UnemploymentFact",
        "in.gov.sih.sih26135.entity.analytics.UnemploymentReasonSummary"
    );

    @Test
    @DisplayName("All 21 analytical view entities are annotated with Hibernate @Immutable")
    void testAllAnalyticsViewEntitiesAreImmutable() {
      assertThat(analyticsViewEntityClassNames).hasSize(21);

      for (String className : analyticsViewEntityClassNames) {
        try {
          Class<?> clazz = Class.forName(className);
          assertThat(clazz.isAnnotationPresent(Immutable.class))
              .withFailMessage("Analytics view entity class %s is NOT annotated with @Immutable", className)
              .isTrue();
        } catch (ClassNotFoundException e) {
          throw new AssertionError("Analytics view entity class not found: " + className, e);
        }
      }
    }
  }
}
