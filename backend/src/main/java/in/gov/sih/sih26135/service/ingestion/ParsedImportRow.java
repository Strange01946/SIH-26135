package in.gov.sih.sih26135.service.ingestion;

import java.util.Map;

/**
 * Bounded per-row representation yielded incrementally by streaming CSV and JSON parsers.
 */
public record ParsedImportRow(
    int sourceRowNumber,
    RowKind rowKind,
    Map<String, String> fields,
    String errorCode,
    String errorMessage
) {

  public enum RowKind {
    DATA,
    SKIPPED,
    MALFORMED
  }

  public static ParsedImportRow data(int sourceRowNumber, Map<String, String> fields) {
    return new ParsedImportRow(sourceRowNumber, RowKind.DATA, fields, null, null);
  }

  public static ParsedImportRow skipped(int sourceRowNumber, String errorCode, String errorMessage) {
    return new ParsedImportRow(
        sourceRowNumber,
        RowKind.SKIPPED,
        Map.of(),
        ImportErrorSanitizer.sanitizeErrorCode(errorCode, "EMPTY_ROW"),
        ImportErrorSanitizer.sanitizeErrorMessage(errorMessage, "Empty source row skipped")
    );
  }

  public static ParsedImportRow malformed(int sourceRowNumber, String errorCode, String errorMessage) {
    return new ParsedImportRow(
        sourceRowNumber,
        RowKind.MALFORMED,
        Map.of(),
        ImportErrorSanitizer.sanitizeErrorCode(errorCode, "MALFORMED_ROW"),
        ImportErrorSanitizer.sanitizeErrorMessage(errorMessage, "Malformed source row")
    );
  }
}
