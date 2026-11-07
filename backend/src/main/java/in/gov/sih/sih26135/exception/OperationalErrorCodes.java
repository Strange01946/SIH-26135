package in.gov.sih.sih26135.exception;

/**
 * Machine-readable error codes standardized for Phase 28 Operational Management domain operations.
 */
public final class OperationalErrorCodes {

  private OperationalErrorCodes() {
    // Utility / constant class
  }

  /**
   * Thrown when an update or deletion is attempted on an append-only audit log record.
   * HTTP 405 Method Not Allowed / HTTP 403 Forbidden.
   */
  public static final String AUDIT_LOG_IMMUTABLE = "AUDIT_LOG_IMMUTABLE";

  /**
   * Thrown when a referenced data quality rule cannot be found by identifier or code.
   * HTTP 404 Not Found.
   */
  public static final String DQ_RULE_NOT_FOUND = "DQ_RULE_NOT_FOUND";

  /**
   * Thrown when a data quality issue undergoes an invalid state transition (e.g. modifying a resolved/dismissed issue).
   * HTTP 409 Conflict.
   */
  public static final String INVALID_DQ_TRANSITION = "INVALID_DQ_TRANSITION";

  /**
   * Thrown when modifications are attempted on an import batch that is already in a terminal state (COMPLETED, FAILED, CANCELLED).
   * HTTP 409 Conflict.
   */
  public static final String IMPORT_BATCH_TERMINAL = "IMPORT_BATCH_TERMINAL";

  /**
   * Thrown when an import batch or ingestion payload exceeds the maximum allowed row limit.
   * HTTP 400 Bad Request.
   */
  public static final String IMPORT_ROW_LIMIT_EXCEEDED = "IMPORT_ROW_LIMIT_EXCEEDED";
}
