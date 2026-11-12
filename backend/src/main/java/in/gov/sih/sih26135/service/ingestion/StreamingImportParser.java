package in.gov.sih.sih26135.service.ingestion;

import java.io.Closeable;

/**
 * Streaming pull parser contract producing bounded row representations one record at a time.
 */
public interface StreamingImportParser extends Closeable {

  /**
   * Returns the next parsed source row, or {@code null} when EOF is reached.
   *
   * @throws ImportStreamParseException on unrecoverable stream or syntax errors
   */
  ParsedImportRow nextRow() throws ImportStreamParseException;

  /**
   * Unchecked streaming exception signaling fatal header, syntax, or I/O parse failure.
   */
  class ImportStreamParseException extends RuntimeException {
    private final String errorCode;

    public ImportStreamParseException(String errorCode, String message) {
      super(message);
      this.errorCode = errorCode;
    }

    public ImportStreamParseException(String errorCode, String message, Throwable cause) {
      super(message, cause);
      this.errorCode = errorCode;
    }

    public String getErrorCode() {
      return errorCode;
    }
  }
}
