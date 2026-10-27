package in.gov.sih.sih26135.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a date of birth represents a valid past calendar date and that the individual
 * has achieved the minimum required age (default 15 years old).
 *
 * <p>Enforces:
 * <ul>
 *   <li>Date of birth cannot be in the future.</li>
 *   <li>The individual must be at least {@link #minAge()} years old.</li>
 *   <li>Exactly {@link #minAge()} years old on the current calendar date is accepted.</li>
 *   <li>Calculates age strictly using calendar date semantics (handling leap years).</li>
 *   <li>Nullable semantics: if {@code null}, validation passes (allowing optional fields).</li>
 * </ul>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateOfBirthValidator.class)
@Documented
public @interface ValidDateOfBirth {

  /**
   * Validation error message template.
   */
  String message() default "must be at least 15 years old and not in the future";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * Minimum required age in complete calendar years. Defaults to 15.
   */
  int minAge() default 15;
}
