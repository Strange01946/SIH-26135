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
 * Event publication failures are isolated defensively to prevent disrupting successful business outcomes.
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

    // 3. Resolve affected entity identifier safely using explicit extraction
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

    try {
      log.debug("Publishing AuditTrailEvent: entityType={}, entityId={}, action={}, actor={}",
          event.entityType(), event.entityId(), event.actionCode(), event.actorUserId());
      eventPublisher.publishEvent(event);
    } catch (Exception ex) {
      log.error("Failed to publish AuditTrailEvent for entityType={}, entityId={}: {}",
          event.entityType(), event.entityId(), ex.getMessage(), ex);
    }

    return result;
  }

  /**
   * Resolves the primary entity ID from method arguments or return value using explicit
   * domain-type extractors to avoid selecting ambiguous nested DTO identifiers.
   */
  Long resolveEntityId(ProceedingJoinPoint joinPoint, Object result, AuditTrail annotation) {
    String entityType = annotation.entityType();

    // 1. Explicit domain entity extraction based on entityType
    Long explicitId = extractExplicitDomainEntityId(entityType, joinPoint.getArgs(), result);
    if (explicitId != null) {
      return explicitId;
    }

    // 2. Check explicit parameter name if specified on annotation
    if (!annotation.entityIdArg().isBlank()) {
      Long id = extractIdByParamName(joinPoint, annotation.entityIdArg());
      if (id != null) {
        return id;
      }
    }

    // 3. Inspect method arguments for ID properties
    Object[] args = joinPoint.getArgs();
    if (args != null) {
      for (Object arg : args) {
        if (arg == null) continue;
        if (arg instanceof Long l) {
          return l;
        }
        Long id = extractIdFromObject(arg, entityType);
        if (id != null) {
          return id;
        }
      }
    }

    // 4. Inspect return value
    if (result != null) {
      if (result instanceof Long l) {
        return l;
      }
      Long id = extractIdFromObject(result, entityType);
      if (id != null) {
        return id;
      }
    }

    return null;
  }

  /**
   * Explicit, deterministic extraction for core domain entities ensuring exact attribution.
   */
  private Long extractExplicitDomainEntityId(String entityType, Object[] args, Object result) {
    if (entityType == null) return null;
    String norm = entityType.trim().toUpperCase().replace("_", "");

    switch (norm) {
      case "PLACEMENT", "PLACEMENTRECORD" -> {
        if (args != null) {
          for (Object arg : args) {
            Long id = invokeIdGetter(arg, "getPlacementId");
            if (id != null) return id;
          }
        }
        if (result != null) {
          Long id = invokeIdGetter(result, "getPlacementId");
          if (id != null) return id;
          Object placement = invokeMethod(result, "placement");
          if (placement == null) {
            placement = invokeMethod(result, "placementRecord");
          }
          if (placement != null) {
            Long pid = invokeIdGetter(placement, "id");
            if (pid != null) return pid;
          }
        }
      }
      case "TRAININGENROLLMENT", "TRAINING" -> {
        if (result != null) {
          Object enrollment = invokeMethod(result, "updatedEnrollment");
          if (enrollment != null) {
            Long eid = invokeIdGetter(enrollment, "id");
            if (eid == null) eid = invokeIdGetter(enrollment, "getId");
            if (eid != null) return eid;
          }
        }
        if (args != null) {
          for (Object arg : args) {
            Long id = invokeIdGetter(arg, "getEnrollmentId");
            if (id != null) return id;
            Object certReq = invokeMethod(arg, "getCertificationRequest");
            if (certReq != null) {
              Long eid = invokeIdGetter(certReq, "getEnrollmentId");
              if (eid != null) return eid;
            }
          }
        }
      }
      case "EMPLOYMENTVERIFICATION", "VERIFICATION" -> {
        // Authoritative verification is created during the method, so result.verification().id() is authoritative
        if (result != null) {
          Object verification = invokeMethod(result, "verification");
          if (verification != null) {
            Long vid = invokeIdGetter(verification, "id");
            if (vid == null) vid = invokeIdGetter(verification, "getId");
            if (vid != null) return vid;
          }
        }
      }
      case "FOLLOWUPTASK", "FOLLOWUP" -> {
        if (args != null) {
          for (Object arg : args) {
            Long id = invokeIdGetter(arg, "getFollowupTaskId");
            if (id != null) return id;
          }
        }
        if (result != null) {
          Object task = invokeMethod(result, "updatedFollowupTask");
          if (task == null) {
            task = invokeMethod(result, "updatedTask");
          }
          if (task != null) {
            Long tid = invokeIdGetter(task, "getId");
            if (tid == null) tid = invokeIdGetter(task, "id");
            if (tid != null) return tid;
          }
        }
      }
      case "SKILLGAPRECOMMENDATION", "SKILLGAP" -> {
        if (args != null) {
          for (Object arg : args) {
            Long id = invokeIdGetter(arg, "getRecommendationId");
            if (id != null) return id;
          }
        }
        if (result != null) {
          Object rec = invokeMethod(result, "recommendation");
          if (rec == null) {
            rec = invokeMethod(result, "updatedRecommendation");
          }
          if (rec != null) {
            Long rid = invokeIdGetter(rec, "id");
            if (rid == null) rid = invokeIdGetter(rec, "getId");
            if (rid != null) return rid;
          }
        }
      }
      case "EMPLOYMENTRECORD", "EMPLOYMENT" -> {
        // Authoritative target is the EmploymentRecord being transitioned, NOT exitEvent.id()!
        if (args != null) {
          for (Object arg : args) {
            Object exitReq = invokeMethod(arg, "getExitEventRequest");
            if (exitReq != null) {
              Long empId = invokeIdGetter(exitReq, "getEmploymentId");
              if (empId != null) return empId;
            }
            Long empId = invokeIdGetter(arg, "getEmploymentId");
            if (empId != null) return empId;
          }
        }
        if (result != null) {
          Object exitEvent = invokeMethod(result, "exitEvent");
          if (exitEvent != null) {
            Long empId = invokeIdGetter(exitEvent, "employmentId");
            if (empId == null) empId = invokeIdGetter(exitEvent, "getEmploymentId");
            if (empId != null) return empId;
          }
        }
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

    // 1. Check entity-specific getters first to avoid returning unrelated ID
    String entitySpecificGetter = "get" + entityType + "Id";
    Long specificId = invokeIdGetter(target, entitySpecificGetter);
    if (specificId != null) return specificId;

    // 2. Check direct getters: getId(), id()
    Long directId = invokeIdGetter(target, "getId");
    if (directId != null) return directId;

    directId = invokeIdGetter(target, "id");
    if (directId != null) return directId;

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

    return null;
  }

  private Long invokeIdGetter(Object target, String methodName) {
    if (target == null) return null;
    try {
      Method method = target.getClass().getMethod(methodName);
      Object val = method.invoke(target);
      if (val instanceof Long l) return l;
      if (val instanceof Number n) return n.longValue();
    } catch (Exception ignored) {
    }
    return null;
  }

  private Object invokeMethod(Object target, String methodName) {
    if (target == null) return null;
    try {
      Method method = target.getClass().getMethod(methodName);
      return method.invoke(target);
    } catch (Exception ignored) {
      return null;
    }
  }
}
