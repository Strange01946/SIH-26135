package in.gov.sih.sih26135.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for demarcating domain workflow state transitions that require
 * automated, immutable audit trail interception and event-based persistence.
 *
 * <p>Applied to service methods executing domain state transitions.
 * When annotated, {@link in.gov.sih.sih26135.aspect.AuditTrailAspect} captures
 * the authenticated actor, correlation ID, entity identifiers, and transition outcome,
 * publishing a strongly typed {@link in.gov.sih.sih26135.event.AuditTrailEvent} upon
 * successful execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditTrail {

  /**
   * The canonical audit action code from {@code ref_audit_action}.
   * Defaults to {@code "STATUS_CHANGE"}.
   */
  String action() default "STATUS_CHANGE";

  /**
   * The logical domain entity type being transitioned (e.g., "Placement", "TrainingEnrollment",
   * "EmploymentVerification", "FollowupTask", "SkillGapRecommendation").
   */
  String entityType();

  /**
   * Human-readable, safe summary of the state transition.
   * Must not contain passwords, tokens, Aadhaar, PAN, or arbitrary serialized objects.
   */
  String summary() default "";

  /**
   * Optional parameter name or expression indicating the entity ID source argument.
   * If empty, the aspect inspects method arguments and return records reflectively.
   */
  String entityIdArg() default "";
}
