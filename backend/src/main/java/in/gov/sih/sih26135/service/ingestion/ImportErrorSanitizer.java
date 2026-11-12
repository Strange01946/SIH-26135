package in.gov.sih.sih26135.service.ingestion;

import org.springframework.dao.DataAccessException;

/**
 * Sanitizes and bounds row-level import error codes and messages stored in {@code import_records}.
 * Ensures zero raw stack traces, SQL strings, credentials, or unnecessary PII are persisted.
 */
public final class ImportErrorSanitizer {

  public static final int MAX_ERROR_CODE_LENGTH = 64;
  public static final int MAX_ERROR_MESSAGE_LENGTH = 500;

  private ImportErrorSanitizer() {
  }

  public record DbFailureClassification(boolean duplicate, String errorCode, String errorMessage) {}

  public static String sanitizeErrorCode(String rawCode, String fallback) {
    String candidate = (rawCode != null && !rawCode.isBlank()) ? rawCode.trim() : fallback;
    if (candidate == null || candidate.isBlank()) {
      return "IMPORT_ERROR";
    }
    String clean = candidate.replaceAll("[^A-Za-z0-9_.-]", "_");
    return clean.length() <= MAX_ERROR_CODE_LENGTH
        ? clean
        : clean.substring(0, MAX_ERROR_CODE_LENGTH);
  }

  public static String sanitizeErrorMessage(String rawMessage, String fallback) {
    String candidate = (rawMessage != null && !rawMessage.isBlank()) ? rawMessage : fallback;
    if (candidate == null || candidate.isBlank()) {
      return "Record processing failed";
    }
    String singleLine = candidate
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('\t', ' ')
        .replaceAll("\\s{2,}", " ")
        .trim();
    if (singleLine.isEmpty()) {
      return "Record processing failed";
    }
    return singleLine.length() <= MAX_ERROR_MESSAGE_LENGTH
        ? singleLine
        : singleLine.substring(0, MAX_ERROR_MESSAGE_LENGTH - 3) + "...";
  }

  /**
   * Classifies a database exception safely without exposing raw SQL statements or sensitive row values.
   */
  public static DbFailureClassification classifyDatabaseFailure(Throwable ex) {
    String rootMessage = "";
    Throwable cur = ex;
    while (cur != null) {
      if (cur.getMessage() != null) {
        rootMessage = rootMessage + " " + cur.getMessage();
      }
      cur = cur.getCause();
    }
    String lower = rootMessage.toLowerCase();

    if (lower.contains("uk_trainees_registration_number")) {
      return new DbFailureClassification(true, "DUPLICATE_TRAINEE_REGISTRATION",
          "Duplicate trainee registration number conflicts with existing record");
    }
    if (lower.contains("uk_trainees_user_id")) {
      return new DbFailureClassification(true, "DUPLICATE_TRAINEE_USER",
          "Trainee user account is already linked to an existing trainee record");
    }
    if (lower.contains("uk_placement_records_number")) {
      return new DbFailureClassification(true, "PLACEMENT_NUMBER_ALREADY_EXISTS",
          "Duplicate placement number conflicts with existing record");
    }
    if (lower.contains("uk_placement_records_application")) {
      return new DbFailureClassification(true, "DUPLICATE_PLACEMENT_APPLICATION",
          "Placement record already exists for the referenced job application");
    }
    if (lower.contains("uk_placement_records_enrollment_posting")) {
      return new DbFailureClassification(true, "DUPLICATE_PLACEMENT_ENROLLMENT_POSTING",
          "Placement record already exists for the referenced enrollment and job posting");
    }
    if (lower.contains("duplicate entry") || lower.contains("unique constraint") || lower.contains("duplicate key")) {
      return new DbFailureClassification(true, "DUPLICATE_RECORD",
          "Row conflicts with an existing unique record");
    }
    if (lower.contains("foreign key constraint") || lower.contains("a foreign key constraint fails")) {
      return new DbFailureClassification(false, "FOREIGN_KEY_NOT_FOUND",
          "One or more referenced foreign key entities do not exist");
    }
    if (lower.contains("check constraint")) {
      return new DbFailureClassification(false, "DATABASE_CHECK_VIOLATION",
          "Row violates a database value or chronological check constraint");
    }
    if (ex instanceof DataAccessException) {
      return new DbFailureClassification(false, "DATABASE_PERSISTENCE_FAILED",
          "Database persistence failed for record due to data integrity constraints");
    }
    return new DbFailureClassification(false, "ROW_PERSISTENCE_FAILED",
        "Record processing failed during persistence");
  }
}
