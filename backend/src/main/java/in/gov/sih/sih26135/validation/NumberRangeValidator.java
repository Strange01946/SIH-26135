package in.gov.sih.sih26135.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Validator implementation for {@link NumberRange}.
 * Fails fast with {@link IllegalArgumentException} on invalid configuration (missing fields, incompatible types).
 */
public class NumberRangeValidator implements ConstraintValidator<NumberRange, Object> {

  private NumberRange annotation;
  private String minField;
  private String maxField;
  private boolean allowEqual;
  private String targetField;
  private String customMessage;

  @Override
  public void initialize(NumberRange constraintAnnotation) {
    this.annotation = constraintAnnotation;
    this.minField = constraintAnnotation.minField();
    this.maxField = constraintAnnotation.maxField();
    this.allowEqual = constraintAnnotation.allowEqual();
    this.targetField = constraintAnnotation.targetField().isBlank()
        ? constraintAnnotation.maxField()
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
    ValidationPropertyUtils.validateNumberRangeConfiguration(value.getClass(), this.annotation);

    ValidationPropertyUtils.PropertyAccessor minAccessor =
        ValidationPropertyUtils.resolveProperty(value.getClass(), minField);
    ValidationPropertyUtils.PropertyAccessor maxAccessor =
        ValidationPropertyUtils.resolveProperty(value.getClass(), maxField);

    Object minVal = minAccessor.read(value);
    Object maxVal = maxAccessor.read(value);

    // If either value is null, range validation cannot be evaluated; pass through to field-level nullability checks
    if (minVal == null || maxVal == null) {
      return true;
    }

    int comparison;
    if (minVal instanceof BigDecimal minDec && maxVal instanceof BigDecimal maxDec) {
      // Use BigDecimal.compareTo to ignore scale differences (e.g. 100.00 vs 100)
      comparison = maxDec.compareTo(minDec);
    } else if (minVal instanceof Comparable && maxVal instanceof Comparable) {
      try {
        comparison = ((Comparable) maxVal).compareTo(minVal);
      } catch (ClassCastException ex) {
        throw new IllegalArgumentException(String.format(
            "Invalid @NumberRange configuration on %s: incompatible types for '%s' (%s) and '%s' (%s)",
            value.getClass().getName(), minField, minVal.getClass().getName(),
            maxField, maxVal.getClass().getName()), ex);
      }
    } else {
      throw new IllegalArgumentException(String.format(
          "Invalid @NumberRange configuration on %s: properties '%s' and '%s' must implement Comparable",
          value.getClass().getName(), minField, maxField));
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
