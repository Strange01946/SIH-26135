package in.gov.sih.sih26135.aspect;

import in.gov.sih.sih26135.annotation.AuditTrail;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.event.AuditTrailEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("AuditTrailAspect Unit & Attribution Contract Tests")
class AuditTrailAspectTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  private AuditTrailAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new AuditTrailAspect(eventPublisher, scopeValidator);
    CorrelationContextHolder.setCorrelationId("CORR-TEST-1234");
  }

  @AfterEach
  void tearDown() {
    CorrelationContextHolder.clear();
  }

  private AuditTrail createAuditTrail(String action, String entityType, String summary, String entityIdArg) {
    return new AuditTrail() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return AuditTrail.class;
      }

      @Override
      public String action() {
        return action;
      }

      @Override
      public String entityType() {
        return entityType;
      }

      @Override
      public String summary() {
        return summary;
      }

      @Override
      public String entityIdArg() {
        return entityIdArg;
      }
    };
  }

  public record DummyResponse(Long id, String status) {}
  public record DummyPlacementRequest(Long placementId) {
    public Long getPlacementId() {
      return placementId;
    }
  }

  @Test
  @DisplayName("Successful workflow execution publishes AuditTrailEvent with actor, correlation, and entity attribution")
  void testSuccessfulInterceptionPublishesEvent() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "Placement", "Placement transitioned to employment", "");
    DummyResponse expectedResponse = new DummyResponse(888L, "ACTIVE");

    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(101L);
    when(joinPoint.proceed()).thenReturn(expectedResponse);
    when(joinPoint.getArgs()).thenReturn(new Object[]{new DummyPlacementRequest(777L)});

    Object result = aspect.interceptWorkflowMethod(joinPoint, annotation);

    assertThat(result).isSameAs(expectedResponse);
    verify(joinPoint, times(1)).proceed();

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher, times(1)).publishEvent(captor.capture());

    AuditTrailEvent event = captor.getValue();
    assertThat(event.actionCode()).isEqualTo("STATUS_CHANGE");
    assertThat(event.entityType()).isEqualTo("Placement");
    assertThat(event.entityId()).isEqualTo(777L);
    assertThat(event.actorUserId()).isEqualTo(101L);
    assertThat(event.correlationId()).isEqualTo("CORR-TEST-1234");
    assertThat(event.changeSummary()).isEqualTo("Placement transitioned to employment");
    assertThat(event.occurredAt()).isNotNull();
  }

  @Test
  @DisplayName("Failed workflow execution propagates exception and does NOT publish audit event")
  void testFailedWorkflowExecutionDoesNotPublishEvent() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "TrainingEnrollment", "Certificate issued", "");

    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(102L);
    when(joinPoint.proceed()).thenThrow(new BadRequestException("Invalid enrollment transition", "INVALID_STATE"));

    assertThatThrownBy(() -> aspect.interceptWorkflowMethod(joinPoint, annotation))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid enrollment transition");

    verify(joinPoint, times(1)).proceed();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Event publication failure does NOT propagate exception and does NOT fail primary business method")
  void testEventPublicationFailureDoesNotFailWorkflowOutcome() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "Placement", "Placement transitioned", "");
    DummyResponse expectedResponse = new DummyResponse(999L, "SUCCESS");

    when(scopeValidator.resolveAuthenticatedUserId()).thenReturn(105L);
    when(joinPoint.proceed()).thenReturn(expectedResponse);
    when(joinPoint.getArgs()).thenReturn(new Object[]{new DummyPlacementRequest(999L)});
    doThrow(new RuntimeException("Simulated event publisher failure")).when(eventPublisher).publishEvent(any());

    // Method execution must succeed and return business result despite audit event failure
    Object result = aspect.interceptWorkflowMethod(joinPoint, annotation);
    assertThat(result).isSameAs(expectedResponse);
  }

  // =========================================================================
  // Workflow Attribution Contract Tests for all Six Phase 25 Workflows
  // =========================================================================

  @Test
  @DisplayName("Workflow 1 Attribution: transitionPlacementToEmployment correctly resolves Placement entity ID")
  void testWorkflow1PlacementAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "Placement", "Placement transitioned to employment", "");
    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(701L);

    PlacementRecordResponse placementRecord = mock(PlacementRecordResponse.class);
    when(placementRecord.id()).thenReturn(701L);
    PlacementToEmploymentTransitionResponse response = mock(PlacementToEmploymentTransitionResponse.class);
    when(response.placement()).thenReturn(placementRecord);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("Placement");
    assertThat(captor.getValue().entityId()).isEqualTo(701L);
  }

  @Test
  @DisplayName("Workflow 2 Attribution: recordAssessmentResultAndIssueCertificate correctly resolves TrainingEnrollment entity ID")
  void testWorkflow2TrainingEnrollmentAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "TrainingEnrollment", "Assessment recorded and certificate issued", "");
    RecordAssessmentAndIssueCertificateRequest request = new RecordAssessmentAndIssueCertificateRequest();
    request.setEnrollmentId(702L);

    TrainingEnrollmentResponse updatedEnrollment = mock(TrainingEnrollmentResponse.class);
    when(updatedEnrollment.getId()).thenReturn(702L);
    RecordAssessmentAndIssueCertificateResponse response = mock(RecordAssessmentAndIssueCertificateResponse.class);
    when(response.updatedEnrollment()).thenReturn(updatedEnrollment);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("TrainingEnrollment");
    assertThat(captor.getValue().entityId()).isEqualTo(702L);
  }

  @Test
  @DisplayName("Workflow 3 Attribution: recordVerificationVerdictAndSynchronize correctly resolves EmploymentVerification entity ID, NOT verificationRequest ID")
  void testWorkflow3EmploymentVerificationAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "EmploymentVerification", "Employment verification verdict recorded", "");
    RecordVerificationVerdictRequest request = new RecordVerificationVerdictRequest();
    CreateEmploymentVerificationRequest verReq = new CreateEmploymentVerificationRequest();
    verReq.setEmploymentVerificationRequestId(9999L); // Nested request ID must NOT be selected!
    request.setVerificationRequest(verReq);

    EmploymentVerificationResponse verification = mock(EmploymentVerificationResponse.class);
    when(verification.id()).thenReturn(703L); // True created verification ID
    RecordVerificationVerdictResponse response = mock(RecordVerificationVerdictResponse.class);
    when(response.verification()).thenReturn(verification);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("EmploymentVerification");
    assertThat(captor.getValue().entityId()).isEqualTo(703L); // Must be 703L, never 9999L!
  }

  @Test
  @DisplayName("Workflow 4 Attribution: recordFollowupSurveyResponse correctly resolves FollowupTask entity ID")
  void testWorkflow4FollowupTaskAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "FollowupTask", "Follow-up survey response recorded", "");
    RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
    request.setFollowupTaskId(704L);

    FollowupTaskResponse updatedTask = mock(FollowupTaskResponse.class);
    when(updatedTask.getId()).thenReturn(704L);
    RecordFollowupSurveyResponseResponse response = mock(RecordFollowupSurveyResponseResponse.class);
    when(response.updatedFollowupTask()).thenReturn(updatedTask);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("FollowupTask");
    assertThat(captor.getValue().entityId()).isEqualTo(704L);
  }

  @Test
  @DisplayName("Workflow 5 Attribution: enrollTraineeInRemediationCourse correctly resolves SkillGapRecommendation entity ID")
  void testWorkflow5SkillGapRecommendationAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "SkillGapRecommendation", "Trainee enrolled in remediation course", "");
    SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
    request.setRecommendationId(705L);

    SkillGapRecommendationResponse updatedRec = mock(SkillGapRecommendationResponse.class);
    when(updatedRec.id()).thenReturn(705L);
    SkillGapRemediationEnrollmentResponse response = mock(SkillGapRemediationEnrollmentResponse.class);
    when(response.recommendation()).thenReturn(updatedRec);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("SkillGapRecommendation");
    assertThat(captor.getValue().entityId()).isEqualTo(705L);
  }

  @Test
  @DisplayName("Workflow 6 Attribution: recordExitAndInitiateUnemployment correctly resolves EmploymentRecord entity ID, NOT exitEvent ID or enrollment ID")
  void testWorkflow6EmploymentRecordAttribution() throws Throwable {
    AuditTrail annotation = createAuditTrail("STATUS_CHANGE", "EmploymentRecord", "Employment exit recorded and unemployment initiated", "");
    EmploymentExitAndUnemploymentRequest request = new EmploymentExitAndUnemploymentRequest();
    CreateEmploymentExitEventRequest exitReq = new CreateEmploymentExitEventRequest();
    exitReq.setEmploymentId(706L);
    exitReq.setEnrollmentId(9876L); // Must NOT select enrollment ID
    request.setExitEventRequest(exitReq);

    EmploymentExitEventResponse exitEvent = mock(EmploymentExitEventResponse.class);
    when(exitEvent.employmentId()).thenReturn(706L); // Target employment record ID
    EmploymentExitAndUnemploymentResponse response = mock(EmploymentExitAndUnemploymentResponse.class);
    when(response.exitEvent()).thenReturn(exitEvent);

    when(joinPoint.proceed()).thenReturn(response);
    when(joinPoint.getArgs()).thenReturn(new Object[]{request});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityType()).isEqualTo("EmploymentRecord");
    assertThat(captor.getValue().entityId()).isEqualTo(706L); // Must be 706L
  }
}
