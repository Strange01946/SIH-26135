package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an update or mutation is attempted on an import batch that is already in a terminal status.
 * Maps to HTTP 409 Conflict.
 */
public class ImportBatchTerminalException extends BaseException {

  public ImportBatchTerminalException(String message) {
    super(message, HttpStatus.CONFLICT, OperationalErrorCodes.IMPORT_BATCH_TERMINAL);
  }

  public ImportBatchTerminalException(Long batchId, String currentStatus) {
    super("Import batch id=" + batchId + " is in terminal status: " + currentStatus,
        HttpStatus.CONFLICT, OperationalErrorCodes.IMPORT_BATCH_TERMINAL);
  }
}
