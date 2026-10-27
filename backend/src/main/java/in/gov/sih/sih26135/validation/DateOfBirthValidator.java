package in.gov.sih.sih26135.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Validator implementation for {@link ValidDateOfBirth}.
 *
 * <p>Enforces calendar-based minimum age (default 15) and non-future date rules.
 * Uses {@link Clock#systemDefaultZone()} for runtime Bean Validation execution.
 * Package-private constructor and setter are provided exclusively for deterministic unit testing.
 */
public class DateOfBirthValidator implements ConstraintValidator<ValidDateOfBirth, LocalDate> {

  private int minAge;
  private Clock clock;

  /**
   * Default constructor invoked by the standard Bean Validation provider at runtime.
   * Uses {@link Clock#systemDefaultZone()} to preserve standard application timezone semantics.
   */
  public DateOfBirthValidator() {
    this(Clock.systemDefaultZone());
  }

  /**
   * Unit-testing constructor allowing deterministic clock injection.
   *
   * @param clock deterministic clock instance (falls back to systemDefaultZone if null)
   */
  DateOfBirthValidator(Clock clock) {
    this.clock = clock != null ? clock : Clock.systemDefaultZone();
  }

  /**
   * Unit-testing setter allowing deterministic clock injection.
   *
   * @param clock deterministic clock instance (falls back to systemDefaultZone if null)
   */
  void setClock(Clock clock) {
    this.clock = clock != null ? clock : Clock.systemDefaultZone();
  }

  @Override
  public void initialize(ValidDateOfBirth constraintAnnotation) {
    this.minAge = constraintAnnotation.minAge();
  }

  @Override
  public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
    // Nullable handling: let @NotNull or domain contract enforce presence if required
    if (dateOfBirth == null) {
      return true;
    }

    LocalDate today = LocalDate.now(clock);

    // 1. Must not be in the future
    if (dateOfBirth.isAfter(today)) {
      return false;
    }

    // 2. Must be at least minAge years old using calendar-date semantics
    // Note: dob.plusYears(minAge) accurately handles leap years (e.g. Feb 29 -> Feb 28 on non-leap years)
    if (dateOfBirth.plusYears(minAge).isAfter(today)) {
      return false;
    }

    return true;
  }
}
