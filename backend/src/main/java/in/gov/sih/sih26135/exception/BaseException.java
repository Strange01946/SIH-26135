package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Base abstract runtime exception for SIH 26135 application exceptions.
 * Encapsulates an HTTP status and a machine-readable error code.
 */
public abstract class BaseException extends RuntimeException {

  private final HttpStatus status;
  private final String errorCode;

  protected BaseException(String message) {
    this(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
  }

  protected BaseException(String message, HttpStatus status, String errorCode) {
    super(message);
    this.status = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
  }

  protected BaseException(String message, Throwable cause, HttpStatus status, String errorCode) {
    super(message, cause);
    this.status = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
