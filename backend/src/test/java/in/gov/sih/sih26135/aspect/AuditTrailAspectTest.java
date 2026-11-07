package in.gov.sih.sih26135.aspect;

import in.gov.sih.sih26135.annotation.AuditTrail;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditTrailAspect Unit & Interception Tests")
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
  @DisplayName("Entity ID resolved from return value when not available in method arguments")
  void testEntityIdResolvedFromReturnValue() throws Throwable {
    AuditTrail annotation = createAuditTrail("CREATE", "EmploymentVerification", "Verification verdict created", "");
    DummyResponse responseWithId = new DummyResponse(555L, "VERIFIED");

    when(joinPoint.proceed()).thenReturn(responseWithId);
    when(joinPoint.getArgs()).thenReturn(new Object[]{"string-arg-without-id"});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().entityId()).isEqualTo(555L);
  }

  @Test
  @DisplayName("Default summary generated when annotation summary is blank")
  void testDefaultSummaryWhenBlank() throws Throwable {
    AuditTrail annotation = createAuditTrail("UPDATE", "FollowupTask", "", "");
    DummyResponse responseWithId = new DummyResponse(123L, "COMPLETED");

    when(joinPoint.proceed()).thenReturn(responseWithId);
    when(joinPoint.getArgs()).thenReturn(new Object[]{});

    aspect.interceptWorkflowMethod(joinPoint, annotation);

    ArgumentCaptor<AuditTrailEvent> captor = ArgumentCaptor.forClass(AuditTrailEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().changeSummary()).isEqualTo("FollowupTask UPDATE");
  }
}
