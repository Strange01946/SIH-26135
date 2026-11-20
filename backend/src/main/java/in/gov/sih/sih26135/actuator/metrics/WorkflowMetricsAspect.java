package in.gov.sih.sih26135.actuator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

  @Around("execution(* in.gov.sih.sih26135.service.*WorkflowService.*(..))")
  public Object timeWorkflowTransition(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    String workflowType = deriveWorkflowType(className);
    String transition = deriveTransition(methodName);

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
    String clean = className.replace("Impl", "").replace("WorkflowService", "").replace("Workflow", "");
    if (clean.isEmpty()) {
      return "GENERAL";
    }
    return clean.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
  }

  private String deriveTransition(String methodName) {
    return methodName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
  }
}
