package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the number of records in an import batch exceeds the maximum allowed limit.
 * Maps to HTTP 400 Bad Request.
 */
public class ImportRowLimitExceededException extends BaseException {

  private final int rowCount;
  private final int maxLimit;

  public ImportRowLimitExceededException(String message) {
    super(message, HttpStatus.BAD_REQUEST, OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED);
    this.rowCount = 0;
    this.maxLimit = 0;
  }

  public ImportRowLimitExceededException(int rowCount, int maxLimit) {
    super("Import row count " + rowCount + " exceeds maximum allowed limit of " + maxLimit,
        HttpStatus.BAD_REQUEST, OperationalErrorCodes.IMPORT_ROW_LIMIT_EXCEEDED);
    this.rowCount = rowCount;
    this.maxLimit = maxLimit;
  }

  public int getRowCount() {
    return rowCount;
  }

  public int getMaxLimit() {
    return maxLimit;
  }
}
