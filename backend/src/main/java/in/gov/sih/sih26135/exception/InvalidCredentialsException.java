package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails due to invalid credentials, inactive account, or token validation failure.
 */
public class InvalidCredentialsException extends BaseException {

  public InvalidCredentialsException(String message) {
    super(message, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
  }

  public InvalidCredentialsException() {
    this("Invalid username/email or password");
  }
}
