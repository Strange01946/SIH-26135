package in.gov.sih.sih26135.exception;

/**
 * Thrown when a distributed named lock cannot be acquired during data quality issue registration.
 */
public class DataQualityLockAcquisitionException extends RuntimeException {

  public DataQualityLockAcquisitionException(String message) {
    super(message);
  }

  public DataQualityLockAcquisitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
