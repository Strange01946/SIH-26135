package in.gov.sih.sih26135.actuator.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit method annotation marking a workflow state transition to be intercepted,
 * timed, and categorized into Micrometer's {@code workflow.transition.duration} metric.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowTransition {

  /**
   * Optional custom workflow type override (e.g. "VERIFICATION", "EMPLOYMENT_LIFECYCLE").
   * Defaults to class-derived uppercase name.
   */
  String type() default "";

  /**
   * Optional custom transition name override (e.g. "INITIATE_VERIFICATION", "RECORD_EXIT").
   * Defaults to method-derived uppercase name.
   */
  String transition() default "";
}
