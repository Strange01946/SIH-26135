package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an illegal or unsupported lifecycle transition is attempted on a data quality issue.
 * Maps to HTTP 409 Conflict.
 */
public class InvalidDataQualityTransitionException extends BaseException {

  public InvalidDataQualityTransitionException(String message) {
    super(message, HttpStatus.CONFLICT, OperationalErrorCodes.INVALID_DQ_TRANSITION);
  }

  public InvalidDataQualityTransitionException(String fromStatus, String toStatus) {
    super("Cannot transition data quality issue from " + fromStatus + " to " + toStatus,
        HttpStatus.CONFLICT, OperationalErrorCodes.INVALID_DQ_TRANSITION);
  }
}
