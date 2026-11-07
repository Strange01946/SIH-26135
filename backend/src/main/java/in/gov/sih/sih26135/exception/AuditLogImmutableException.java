package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a modification or deletion is attempted on an append-only audit log record.
 * Maps by default to HTTP 405 Method Not Allowed.
 */
public class AuditLogImmutableException extends BaseException {

  public AuditLogImmutableException(String message) {
    super(message, HttpStatus.METHOD_NOT_ALLOWED, OperationalErrorCodes.AUDIT_LOG_IMMUTABLE);
  }

  public AuditLogImmutableException(String message, HttpStatus status) {
    super(message, status != null ? status : HttpStatus.METHOD_NOT_ALLOWED, OperationalErrorCodes.AUDIT_LOG_IMMUTABLE);
  }
}
