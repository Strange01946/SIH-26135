package in.gov.sih.sih26135.communication.exception;

import in.gov.sih.sih26135.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an external communication provider (Twilio, SendGrid, SMTP) rejects or fails
 * to accept an outbound message dispatch due to provider errors, timeouts, or network failures.
 */
public class CommunicationDeliveryException extends BaseException {

  public CommunicationDeliveryException(String message, String errorCode) {
    super(message, HttpStatus.BAD_GATEWAY, errorCode);
  }

  public CommunicationDeliveryException(String message, Throwable cause, String errorCode) {
    super(message, cause, HttpStatus.BAD_GATEWAY, errorCode);
  }
}
