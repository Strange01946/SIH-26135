package in.gov.sih.sih26135.aspect;

import in.gov.sih.sih26135.annotation.AuditTrail;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.event.AuditTrailEvent;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;

/**
 * Aspect that intercepts workflow service methods annotated with {@link AuditTrail}.
 *
 * <p>Captures security context and correlation identifiers on the calling thread,
 * proceeds with target method execution, and upon successful completion publishes
 * an {@link AuditTrailEvent} for asynchronous, after-commit persistence.
 *
 * <p>Preserves original business exceptions, return values, and transactional rollback semantics.
 */
@Aspect
@Component
public class AuditTrailAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditTrailAspect.class);

  private final ApplicationEventPublisher eventPublisher;
  private final WorkflowSecurityScopeValidator scopeValidator;

  @Autowired
  public AuditTrailAspect(
      ApplicationEventPublisher eventPublisher,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.eventPublisher = eventPublisher;
    this.scopeValidator = scopeValidator;
  }

  @Around("@annotation(auditTrail)")
  public Object interceptWorkflowMethod(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {
    // 1. Capture calling context before crossing transactional / asynchronous boundaries
    Long actorUserId = null;
    if (scopeValidator != null) {
      try {
        actorUserId = scopeValidator.resolveAuthenticatedUserId();
      } catch (Exception e) {
        log.debug("Could not resolve authenticated actor user ID: {}", e.getMessage());
      }
    }

    String correlationId = CorrelationContextHolder.getCorrelationId();
    LocalDateTime occurredAt = LocalDateTime.now();

    // 2. Execute original business method - if an exception occurs, it propagates and NO event is published
    Object result = joinPoint.proceed();

    // 3. Resolve affected entity identifier safely
    Long entityId = resolveEntityId(joinPoint, result, auditTrail);

    // 4. Construct sanitized summary
    String summary = auditTrail.summary();
    if (summary == null || summary.isBlank()) {
      summary = auditTrail.entityType() + " " + auditTrail.action();
    }

    // 5. Publish typed event for after-commit asynchronous persistence
    AuditTrailEvent event = new AuditTrailEvent(
        auditTrail.action(),
        auditTrail.entityType(),
        entityId,
        actorUserId,
        summary,
        occurredAt,
        correlationId
    );

    log.debug("Publishing AuditTrailEvent: entityType={}, entityId={}, action={}, actor={}",
        event.entityType(), event.entityId(), event.actionCode(), event.actorUserId());
    eventPublisher.publishEvent(event);

    return result;
  }

  /**
   * Resolves the primary entity ID from method arguments or return value using reflection.
   */
  Long resolveEntityId(ProceedingJoinPoint joinPoint, Object result, AuditTrail annotation) {
    // Check explicit parameter name if specified
    if (!annotation.entityIdArg().isBlank()) {
      Long id = extractIdByParamName(joinPoint, annotation.entityIdArg());
      if (id != null) {
        return id;
      }
    }

    // Inspect method arguments for ID properties
    Object[] args = joinPoint.getArgs();
    if (args != null) {
      for (Object arg : args) {
        if (arg == null) continue;
        if (arg instanceof Long l) {
          return l;
        }
        Long id = extractIdFromObject(arg, annotation.entityType());
        if (id != null) {
          return id;
        }
      }
    }

    // Inspect return value
    if (result != null) {
      if (result instanceof Long l) {
        return l;
      }
      Long id = extractIdFromObject(result, annotation.entityType());
      if (id != null) {
        return id;
      }
    }

    return null;
  }

  private Long extractIdByParamName(ProceedingJoinPoint joinPoint, String paramName) {
    if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
      String[] paramNames = methodSignature.getParameterNames();
      Object[] args = joinPoint.getArgs();
      if (paramNames != null && args != null) {
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
          if (paramName.equalsIgnoreCase(paramNames[i])) {
            Object val = args[i];
            if (val instanceof Long l) return l;
            if (val instanceof Number n) return n.longValue();
          }
        }
      }
    }
    return null;
  }

  private Long extractIdFromObject(Object target, String entityType) {
    if (target == null) return null;

    Class<?> clazz = target.getClass();

    // 1. Check direct getters: getId(), id()
    Long directId = invokeIdGetter(target, "getId");
    if (directId != null) return directId;

    directId = invokeIdGetter(target, "id");
    if (directId != null) return directId;

    // 2. Check entity-specific getters (e.g. getPlacementId, getEnrollmentId, getFollowupTaskId, getRecommendationId)
    String entitySpecificGetter = "get" + entityType + "Id";
    Long specificId = invokeIdGetter(target, entitySpecificGetter);
    if (specificId != null) return specificId;

    // Common workflow getters
    for (String getter : new String[]{
        "getPlacementId", "getEnrollmentId", "getFollowupTaskId",
        "getRecommendationId", "getEmploymentId", "getEmploymentVerificationRequestId",
        "getEntityId", "getTraineeAssessmentId"
    }) {
      Long id = invokeIdGetter(target, getter);
      if (id != null) return id;
    }

    // 3. If target is a Record, inspect nested record components
    if (clazz.isRecord()) {
      RecordComponent[] components = clazz.getRecordComponents();
      if (components != null) {
        for (RecordComponent rc : components) {
          try {
            Method accessor = rc.getAccessor();
            Object child = accessor.invoke(target);
            if (child != null) {
              Long childId = extractIdFromObject(child, entityType);
              if (childId != null) return childId;
            }
          } catch (Exception ignored) {
          }
        }
      }
    }

    // 4. Nested DTO getters (e.g. getVerificationRequest, getExitEventRequest, getSurveyResponseRequest)
    for (String nestedGetter : new String[]{
        "getVerificationRequest", "getExitEventRequest", "getSurveyResponseRequest",
        "getEnrollmentRequest", "getAssessmentResultRequest"
    }) {
      try {
        Method m = clazz.getMethod(nestedGetter);
        Object nested = m.invoke(target);
        if (nested != null) {
          Long nestedId = extractIdFromObject(nested, entityType);
          if (nestedId != null) return nestedId;
        }
      } catch (Exception ignored) {
      }
    }

    return null;
  }

  private Long invokeIdGetter(Object target, String methodName) {
    try {
      Method method = target.getClass().getMethod(methodName);
      Object val = method.invoke(target);
      if (val instanceof Long l) return l;
      if (val instanceof Number n) return n.longValue();
    } catch (Exception ignored) {
    }
    return null;
  }
}
