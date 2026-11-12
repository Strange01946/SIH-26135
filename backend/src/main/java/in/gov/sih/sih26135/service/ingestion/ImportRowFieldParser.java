package in.gov.sih.sih26135.service.ingestion;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared typed field parsing, Bean Validation bridging, and safe existence lookup utilities
 * reused by Trainee and Placement import row processors.
 */
final class ImportRowFieldParser {

  private static final Pattern POSITIVE_LONG_PATTERN = Pattern.compile("^[0-9]+$");

  private ImportRowFieldParser() {
  }

  static final class RowValidationFailureException extends RuntimeException {
    private final String errorCode;

    RowValidationFailureException(String errorCode, String message) {
      super(message);
      this.errorCode = errorCode;
    }

    String getErrorCode() {
      return errorCode;
    }
  }

  static String requireNonBlank(Map<String, String> fields, String fieldName, int maxLength) {
    String raw = fields.get(fieldName);
    if (raw == null || raw.isBlank()) {
      throw new RowValidationFailureException(
          "MISSING_REQUIRED_FIELD",
          "Required field '" + fieldName + "' is missing or blank");
    }
    String trimmed = raw.trim();
    if (trimmed.length() > maxLength) {
      throw new RowValidationFailureException(
          "FIELD_LENGTH_EXCEEDED",
          "Field '" + fieldName + "' exceeds maximum length of " + maxLength + " characters");
    }
    return trimmed;
  }

  static String optionalTrimmed(Map<String, String> fields, String fieldName, int maxLength) {
    String raw = fields.get(fieldName);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.length() > maxLength) {
      throw new RowValidationFailureException(
          "FIELD_LENGTH_EXCEEDED",
          "Field '" + fieldName + "' exceeds maximum length of " + maxLength + " characters");
    }
    return trimmed;
  }

  static Long requirePositiveLong(Map<String, String> fields, String fieldName) {
    String raw = requireNonBlank(fields, fieldName, 32);
    return parsePositiveLongValue(raw, fieldName);
  }

  static Long optionalPositiveLong(Map<String, String> fields, String fieldName) {
    String raw = fields.get(fieldName);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return parsePositiveLongValue(raw.trim(), fieldName);
  }

  private static Long parsePositiveLongValue(String trimmed, String fieldName) {
    if (!POSITIVE_LONG_PATTERN.matcher(trimmed).matches()) {
      throw new RowValidationFailureException(
          "INVALID_NUMERIC_VALUE",
          "Field '" + fieldName + "' must be a positive integer");
    }
    try {
      long value = Long.parseLong(trimmed);
      if (value <= 0) {
        throw new RowValidationFailureException(
            "INVALID_NUMERIC_VALUE",
            "Field '" + fieldName + "' must be greater than zero");
      }
      return value;
    } catch (NumberFormatException ex) {
      throw new RowValidationFailureException(
          "INVALID_NUMERIC_VALUE",
          "Field '" + fieldName + "' is out of valid numeric range");
    }
  }

  static LocalDate optionalDate(Map<String, String> fields, String fieldName) {
    String raw = fields.get(fieldName);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(raw.trim());
    } catch (DateTimeParseException ex) {
      throw new RowValidationFailureException(
          "INVALID_DATE_FORMAT",
          "Field '" + fieldName + "' must follow ISO yyyy-MM-dd format");
    }
  }

  static BigDecimal optionalNonNegativeDecimal(Map<String, String> fields, String fieldName) {
    String raw = fields.get(fieldName);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      BigDecimal bd = new BigDecimal(raw.trim());
      if (bd.compareTo(BigDecimal.ZERO) < 0) {
        throw new RowValidationFailureException(
            "INVALID_SALARY",
            "Field '" + fieldName + "' must not be negative");
      }
      if (bd.scale() > 2 || (bd.precision() - Math.max(bd.scale(), 0)) > 10) {
        throw new RowValidationFailureException(
            "INVALID_NUMERIC_VALUE",
            "Field '" + fieldName + "' exceeds DECIMAL(12,2) precision bounds");
      }
      return bd;
    } catch (NumberFormatException ex) {
      throw new RowValidationFailureException(
          "INVALID_NUMERIC_VALUE",
          "Field '" + fieldName + "' must be a valid decimal number");
    }
  }

  static <T> void runBeanValidation(Validator validator, T dto) {
    if (validator == null) {
      return;
    }
    Set<ConstraintViolation<T>> violations = validator.validate(dto);
    if (violations != null && !violations.isEmpty()) {
      ConstraintViolation<T> first = violations.stream()
          .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
          .orElse(violations.iterator().next());
      String path = first.getPropertyPath().toString();
      String code = switch (path) {
        case "dateOfBirth" -> "INVALID_DATE_OF_BIRTH";
        case "email" -> "INVALID_EMAIL_FORMAT";
        case "phone" -> "INVALID_PHONE_FORMAT";
        case "pincode" -> "INVALID_PINCODE";
        case "offeredSalary", "joiningSalary" -> "INVALID_SALARY";
        case "currencyCode" -> "INVALID_CURRENCY_CODE";
        default -> "VALIDATION_ERROR";
      };
      String safeMsg = "Validation failed for field '" + path + "': " + first.getMessage();
      throw new RowValidationFailureException(code, safeMsg);
    }
  }

  static boolean existsActiveById(JdbcTemplate jdbcTemplate, String table, String pkCol, Long id, boolean hasDeletedAt) {
    if (id == null) {
      return false;
    }
    String sql = hasDeletedAt
        ? "SELECT COUNT(*) FROM " + table + " WHERE " + pkCol + " = ? AND deleted_at IS NULL"
        : "SELECT COUNT(*) FROM " + table + " WHERE " + pkCol + " = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
    return count != null && count > 0;
  }

  static Long queryOptionalLong(JdbcTemplate jdbcTemplate, String sql, Object... args) {
    try {
      return jdbcTemplate.queryForObject(sql, Long.class, args);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }
}
