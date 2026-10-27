package in.gov.sih.sih26135.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link DateSequence}.
 * Fails fast with {@link IllegalArgumentException} on invalid configuration (missing fields, incompatible types).
 */
public class DateSequenceValidator implements ConstraintValidator<DateSequence, Object> {

  private DateSequence annotation;
  private String startField;
  private String endField;
  private boolean allowEqual;
  private String targetField;
  private String customMessage;

  @Override
  public void initialize(DateSequence constraintAnnotation) {
    this.annotation = constraintAnnotation;
    this.startField = constraintAnnotation.startField();
    this.endField = constraintAnnotation.endField();
    this.allowEqual = constraintAnnotation.allowEqual();
    this.targetField = constraintAnnotation.targetField().isBlank()
        ? constraintAnnotation.endField()
        : constraintAnnotation.targetField();
    this.customMessage = constraintAnnotation.message();
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    // Fail-fast configuration check on target class
    ValidationPropertyUtils.validateDateSequenceConfiguration(value.getClass(), this.annotation);

    ValidationPropertyUtils.PropertyAccessor startAccessor =
        ValidationPropertyUtils.resolveProperty(value.getClass(), startField);
    ValidationPropertyUtils.PropertyAccessor endAccessor =
        ValidationPropertyUtils.resolveProperty(value.getClass(), endField);

    Object startVal = startAccessor.read(value);
    Object endVal = endAccessor.read(value);

    // If either value is null, sequence validation cannot be evaluated; pass through to field-level nullability checks
    if (startVal == null || endVal == null) {
      return true;
    }

    if (!(startVal instanceof Comparable) || !(endVal instanceof Comparable)) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: properties '%s' and '%s' must implement Comparable",
          value.getClass().getName(), startField, endField));
    }

    int comparison;
    try {
      comparison = ((Comparable) endVal).compareTo(startVal);
    } catch (ClassCastException ex) {
      throw new IllegalArgumentException(String.format(
          "Invalid @DateSequence configuration on %s: incompatible types for '%s' (%s) and '%s' (%s)",
          value.getClass().getName(), startField, startVal.getClass().getName(),
          endField, endVal.getClass().getName()), ex);
    }

    boolean valid = allowEqual ? comparison >= 0 : comparison > 0;
    if (!valid) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(customMessage)
          .addPropertyNode(targetField)
          .addConstraintViolation();
      return false;
    }

    return true;
  }
}
