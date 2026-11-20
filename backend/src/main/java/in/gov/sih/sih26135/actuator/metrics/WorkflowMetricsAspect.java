package in.gov.sih.sih26135.actuator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect intercepting Phase 25 workflow services to record execution duration and outcome metrics.
 *
 * <p>Principles:
 * <ul>
 *   <li>Metric name: {@code workflow.transition.duration}.</li>
 *   <li>Bounded, low-cardinality tags: {@code workflow.type}, {@code transition}, {@code outcome}.</li>
 *   <li>Strictly zero PII: no user IDs, trainee IDs, batch IDs, or arbitrary exception messages.</li>
 *   <li>Outcome is categorized as {@code SUCCESS} or {@code FAILURE}.</li>
 *   <li>Exceptions are recorded and rethrown unchanged, preserving business transaction semantics.</li>
 *   <li>Read-only query methods (get*, find*, query*) and configuration setters (set*) are strictly excluded.</li>
 * </ul>
 */
@Aspect
@Component
public class WorkflowMetricsAspect {

  private final MeterRegistry meterRegistry;

  @Autowired
  public WorkflowMetricsAspect(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry is required");
  }

  @Pointcut("(@annotation(in.gov.sih.sih26135.actuator.metrics.WorkflowTransition)) || "
      + "(execution(* in.gov.sih.sih26135.service..*Workflow*.*(..)) "
      + "&& !execution(* in.gov.sih.sih26135.service..*Workflow*.get*(..)) "
      + "&& !execution(* in.gov.sih.sih26135.service..*Workflow*.set*(..)) "
      + "&& !execution(* in.gov.sih.sih26135.service..*Workflow*.find*(..)) "
      + "&& !execution(* in.gov.sih.sih26135.service..*Workflow*.query*(..)))")
  public void workflowTransitionExecution() {}

  @Around("workflowTransitionExecution()")
  public Object timeWorkflowTransition(ProceedingJoinPoint joinPoint) throws Throwable {
    String rawClassName = joinPoint.getTarget() != null
        ? joinPoint.getTarget().getClass().getSimpleName()
        : joinPoint.getSignature().getDeclaringType().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    WorkflowTransition annotation = null;
    if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
      Method method = methodSignature.getMethod();
      annotation = method.getAnnotation(WorkflowTransition.class);
    }

    String workflowType = (annotation != null && !annotation.type().isBlank())
        ? annotation.type().toUpperCase(Locale.ROOT)
        : deriveWorkflowType(rawClassName);

    String transition = (annotation != null && !annotation.transition().isBlank())
        ? annotation.transition().toUpperCase(Locale.ROOT)
        : deriveTransition(methodName);

    Timer.Sample sample = Timer.start(meterRegistry);
    String outcome = "SUCCESS";
    try {
      return joinPoint.proceed();
    } catch (Throwable t) {
      outcome = "FAILURE";
      throw t;
    } finally {
      sample.stop(Timer.builder("workflow.transition.duration")
          .description("Execution duration of Phase 25 workflow transitions")
          .tag("workflow.type", workflowType)
          .tag("transition", transition)
          .tag("outcome", outcome)
          .register(meterRegistry));
    }
  }

  private String deriveWorkflowType(String className) {
    String clean = className;
    int cglibIdx = clean.indexOf("$$");
    if (cglibIdx != -1) {
      clean = clean.substring(0, cglibIdx);
    }
    clean = clean.replace("Impl", "").replace("WorkflowService", "").replace("Workflow", "");
    if (clean.isEmpty()) {
      return "GENERAL";
    }
    return clean.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
  }

  private String deriveTransition(String methodName) {
    return methodName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
  }
}
