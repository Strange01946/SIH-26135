package in.gov.sih.sih26135.security.jwt;

/**
 * Exception thrown when JWT parsing, signature verification, expiration, or claims validation fails.
 */
public class JwtValidationException extends RuntimeException {

  public enum ErrorCode {
    EXPIRED,
    INVALID_SIGNATURE,
    MALFORMED,
    UNSUPPORTED,
    INVALID_ISSUER,
    INVALID_TOKEN_TYPE,
    MISSING_REQUIRED_CLAIM,
    GENERAL_ERROR
  }

  private final ErrorCode errorCode;

  public JwtValidationException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public JwtValidationException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
