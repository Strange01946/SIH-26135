package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when client input/request is invalid (HTTP 400).
 */
public class BadRequestException extends BaseException {

  public BadRequestException(String message) {
    super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
  }

  public BadRequestException(String message, String errorCode) {
    super(message, HttpStatus.BAD_REQUEST, errorCode != null ? errorCode : "BAD_REQUEST");
  }
}
