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
 * Validates that an upper numerical or financial boundary (e.g. {@code maxSalary}) is greater than
 * or equal to its corresponding lower boundary (e.g. {@code minSalary}).
 *
 * <p>Preserves exact decimal precision when comparing {@link java.math.BigDecimal} values without
 * precision loss or floating-point conversions.
 *
 * <p>Nullable semantics: If either field is {@code null}, validation passes, allowing optional fields
 * or partial updates while strictly enforcing boundary invariants whenever both fields are present.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NumberRangeValidator.class)
@Documented
@Repeatable(NumberRange.List.class)
public @interface NumberRange {

  /**
   * Validation error message template.
   */
  String message() default "must be greater than or equal to minimum value";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * Property name of the minimum value field (e.g. "minSalary", "passScore").
   */
  String minField();

  /**
   * Property name of the maximum value field (e.g. "maxSalary", "maximumScore").
   */
  String maxField();

  /**
   * Whether equal values are considered valid. Defaults to {@code true}.
   */
  boolean allowEqual() default true;

  /**
   * Field path to attach the constraint violation to. Defaults to empty string, which attaches
   * the violation directly to {@link #maxField()}.
   */
  String targetField() default "";

  @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface List {
    NumberRange[] value();
  }
}
