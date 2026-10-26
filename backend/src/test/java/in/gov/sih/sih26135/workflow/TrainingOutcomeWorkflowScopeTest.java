package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.TrainingOutcomeWorkflowServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingOutcomeWorkflow Scope & Boundary Tests")
class TrainingOutcomeWorkflowScopeTest {

  @Mock private TrainingEnrollmentService trainingEnrollmentService;
  @Mock private TraineeAssessmentService traineeAssessmentService;
  @Mock private AssessmentResultService assessmentResultService;
  @Mock private CertificationService certificationService;
  @Mock private CourseSkillRepository courseSkillRepository;
  @Mock private SkillGapRepository skillGapRepository;
  @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;
  @Mock private WorkflowSecurityScopeValidator scopeValidator;

  private TrainingOutcomeWorkflowServiceImpl workflowService;

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
  }

  private TrainingEnrollmentResponse buildEnrollment(Long enrollmentId, Long traineeId, Long providerId) {
    TrainingEnrollmentResponse enrol = new TrainingEnrollmentResponse();
    enrol.setId(enrollmentId);
    enrol.setTraineeId(traineeId);
    enrol.setProviderId(providerId);
    enrol.setCourseId(101L);
    enrol.setStartDate(LocalDate.of(2026, 1, 1));
    enrol.setExpectedCompletionDate(LocalDate.of(2026, 6, 1));
    return enrol;
  }

  @Nested
  @DisplayName("1. Provider Scope Enforcement on Outcome Recording")
  class ProviderScopeOutcomeRecordingTests {

    @Test
    @DisplayName("Permits recording outcome when provider scope check passes")
    void testRecordOutcome_inScopeProviderPermitted() {
      TrainingEnrollmentResponse enrol = buildEnrollment(1L, 100L, 10L);
      when(trainingEnrollmentService.getById(1L)).thenReturn(enrol);

      TraineeAssessmentResponse ta = new TraineeAssessmentResponse();
      ta.setId(5L);
      ta.setEnrollmentId(1L);
      ta.setTraineeId(100L);
      when(traineeAssessmentService.getById(5L)).thenReturn(ta);

      CreateAssessmentResultRequest arReq = new CreateAssessmentResultRequest();
      arReq.setTraineeId(100L);
      arReq.setTraineeAssessmentId(5L);
      arReq.setMaximumScore(new BigDecimal("100.00"));
      arReq.setObtainedScore(new BigDecimal("85.00"));
      arReq.setAssessmentOutcomeId(1L);

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(1L);
      request.setCompletionEnrollmentStatusId(2L);
      request.setAssessmentResultRequest(arReq);

      AssessmentResultResponse arResp = new AssessmentResultResponse();
      arResp.setId(50L);
      arResp.setTraineeId(100L);
      arResp.setIsPassFlag(true);
      when(assessmentResultService.createAssessmentResult(arReq)).thenReturn(arResp);

      TrainingEnrollmentResponse updatedEnrol = buildEnrollment(1L, 100L, 10L);
      updatedEnrol.setIsCompletedFlag(false);
      when(trainingEnrollmentService.updateEnrollment(anyLong(), any())).thenReturn(updatedEnrol);

      assertThatCode(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .doesNotThrowAnyException();

      verify(scopeValidator).validateProviderScope(10L);
      verify(assessmentResultService).createAssessmentResult(arReq);
    }

    @Test
    @DisplayName("Rejects recording outcome when caller is out of provider scope")
    void testRecordOutcome_outOfScopeProviderRejected() {
      TrainingEnrollmentResponse enrol = buildEnrollment(1L, 100L, 20L);
      when(trainingEnrollmentService.getById(1L)).thenReturn(enrol);

      org.mockito.Mockito.doThrow(new BadRequestException(
          "Authenticated training provider is not authorized to act on records of another training provider",
          "PROVIDER_MISMATCH"))
          .when(scopeValidator).validateProviderScope(20L);

      CreateAssessmentResultRequest arReq = new CreateAssessmentResultRequest();
      arReq.setTraineeId(100L);
      arReq.setTraineeAssessmentId(5L);

      RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
      request.setEnrollmentId(1L);
      request.setCompletionEnrollmentStatusId(2L);
      request.setAssessmentResultRequest(arReq);

      assertThatThrownBy(() -> workflowService.recordAssessmentResultAndIssueCertificate(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("2. Provider and Trainee Scope Enforcement on Outcome Summary")
  class OutcomeSummaryScopeTests {

    @Test
    @DisplayName("Permits summary read when in scope")
    void testGetSummary_inScopePermitted() {
      TrainingEnrollmentResponse enrol = buildEnrollment(1L, 100L, 10L);
      when(trainingEnrollmentService.getById(1L)).thenReturn(enrol);
      when(traineeAssessmentService.getByEnrollmentId(1L)).thenReturn(Collections.emptyList());
      when(assessmentResultService.getByTraineeId(100L)).thenReturn(Collections.emptyList());
      when(certificationService.getByEnrollmentId(1L)).thenReturn(Collections.emptyList());

      TraineeTrainingOutcomeSummaryResponse resp =
          workflowService.getTraineeTrainingOutcomeSummary(100L, 1L);

      assertThat(resp).isNotNull();
      verify(scopeValidator).validateTraineeScope(100L);
    }

    @Test
    @DisplayName("Rejects summary read when out of trainee scope")
    void testGetSummary_outOfScopeTraineeRejected() {
      org.mockito.Mockito.doThrow(new BadRequestException(
          "Authenticated trainee is not authorized to act on records of another trainee",
          "TRAINEE_MISMATCH"))
          .when(scopeValidator).validateTraineeScope(100L);

      assertThatThrownBy(() -> workflowService.getTraineeTrainingOutcomeSummary(100L, 1L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(traineeAssessmentService, never()).getByEnrollmentId(anyLong());
    }
  }
}
