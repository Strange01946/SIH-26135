package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested operation causes a state or uniqueness conflict (HTTP 409).
 */
public class ConflictException extends BaseException {

  public ConflictException(String message) {
    super(message, HttpStatus.CONFLICT, "RESOURCE_CONFLICT");
  }

  public ConflictException(String message, String errorCode) {
    super(message, HttpStatus.CONFLICT, errorCode != null ? errorCode : "RESOURCE_CONFLICT");
  }
}
