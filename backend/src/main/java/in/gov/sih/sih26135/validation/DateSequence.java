package in.gov.sih.sih26135.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an end or resolution date is chronologically on or after its corresponding start or identification date.
 *
 * <p>Supports {@link java.time.LocalDate}, {@link java.time.LocalDateTime}, {@link java.time.LocalTime},
 * and any compatible {@link Comparable} date/time representations.
 *
 * <p>Nullable semantics: If either field is {@code null}, validation passes, allowing optional fields
 * or partial updates while strictly enforcing sequencing whenever both fields are present.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateSequenceValidator.class)
@Documented
@Repeatable(DateSequence.List.class)
public @interface DateSequence {

  /**
   * Validation error message template.
   */
  String message() default "must be greater than or equal to start date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * Property name of the chronologically earlier start/identification date field (e.g. "startDate", "identifiedOn").
   */
  String startField();

  /**
   * Property name of the chronologically later end/resolution date field (e.g. "endDate", "resolvedOn").
   */
  String endField();

  /**
   * Whether equal dates/times are considered valid. Defaults to {@code true}.
   */
  boolean allowEqual() default true;

  /**
   * Field path to attach the constraint violation to. Defaults to empty string, which attaches
   * the violation directly to {@link #endField()}.
   */
  String targetField() default "";

  @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface List {
    DateSequence[] value();
  }
}
