package in.gov.sih.sih26135.service.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Memory-bounded RFC 4180 streaming CSV parser handling UTF-8 BOM stripping, quoted fields,
 * escaped double quotes ({@code ""}), embedded commas/newlines, header validation, empty-row skip
 * classification, malformed row isolation, and stable 1-based source data row numbering.
 */
public class StreamingCsvRecordParser implements StreamingImportParser {

  private static final int BUFFER_SIZE = 8192;

  private enum State {
    AT_FIELD_START,
    IN_UNQUOTED_FIELD,
    IN_QUOTED_FIELD,
    AFTER_QUOTE_IN_QUOTED,
    AFTER_QUOTE_BAD_TRAILING
  }

  private record RawCsvRecord(
      boolean eof,
      boolean blankLine,
      boolean malformedQuote,
      List<String> columns
  ) {}

  private final BufferedReader reader;
  private final String entityType;
  private final int maxRowBytes;
  private final List<String> canonicalHeaders;
  private int currentRowNumber = 0;

  public StreamingCsvRecordParser(InputStream inputStream, String entityType, int maxRowBytes) {
    if (inputStream == null) {
      throw new ImportStreamParseException("STAGED_FILE_NOT_FOUND", "CSV input stream cannot be null");
    }
    this.entityType = entityType;
    this.maxRowBytes = Math.max(maxRowBytes, 1024);
    this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE);
    this.canonicalHeaders = readAndValidateHeaders();
  }

  private List<String> readAndValidateHeaders() {
    try {
      RawCsvRecord headerRecord;
      boolean firstRecord = true;
      while (true) {
        headerRecord = readRawRecord(firstRecord);
        firstRecord = false;
        if (headerRecord.eof()) {
          throw new ImportStreamParseException("INVALID_CSV_HEADER", "CSV file is missing a header row");
        }
        if (!headerRecord.blankLine()) {
          break;
        }
      }

      if (headerRecord.malformedQuote()) {
        throw new ImportStreamParseException("INVALID_CSV_HEADER", "CSV header row contains malformed quotes");
      }

      List<String> rawHeaders = headerRecord.columns();
      if (rawHeaders == null || rawHeaders.isEmpty()) {
        throw new ImportStreamParseException("INVALID_CSV_HEADER", "CSV header row cannot be empty");
      }

      List<String> normalized = new ArrayList<>(rawHeaders.size());
      Set<String> seen = new LinkedHashSet<>();
      for (String rawHeader : rawHeaders) {
        String trimmed = rawHeader != null ? rawHeader.trim() : "";
        if (trimmed.isEmpty()) {
          throw new ImportStreamParseException("INVALID_CSV_HEADER", "CSV header contains an empty column name");
        }
        String canonical = ImportContractSchema.normalizeFieldName(entityType, trimmed);
        if (canonical == null) {
          String safeHeader = trimmed.replaceAll("[^A-Za-z0-9_.-]", "_");
          if (safeHeader.length() > 40) {
            safeHeader = safeHeader.substring(0, 40);
          }
          throw new ImportStreamParseException(
              "INVALID_CSV_HEADER",
              "Unsupported CSV column header: " + safeHeader
          );
        }
        if (!seen.add(canonical)) {
          throw new ImportStreamParseException(
              "INVALID_CSV_HEADER",
              "Duplicate CSV column header: " + canonical
          );
        }
        normalized.add(canonical);
      }

      List<String> missing = ImportContractSchema.getMissingRequiredHeaders(entityType, seen);
      if (!missing.isEmpty()) {
        throw new ImportStreamParseException(
            "INVALID_CSV_HEADER",
            "CSV header missing required columns: " + String.join(", ", missing)
        );
      }
      return List.copyOf(normalized);
    } catch (IOException e) {
      throw new ImportStreamParseException("CSV_READ_ERROR", "Failed to read CSV header", e);
    }
  }

  @Override
  public ParsedImportRow nextRow() throws ImportStreamParseException {
    RawCsvRecord record;
    try {
      record = readRawRecord(false);
    } catch (IOException e) {
      throw new ImportStreamParseException("CSV_READ_ERROR", "I/O error reading CSV stream", e);
    }

    if (record.eof()) {
      return null;
    }

    int rowNum = ++currentRowNumber;
    if (record.blankLine()) {
      return ParsedImportRow.skipped(rowNum, "EMPTY_ROW", "Blank CSV source row skipped per empty-row policy");
    }
    if (record.malformedQuote()) {
      return ParsedImportRow.malformed(
          rowNum,
          "MALFORMED_CSV_ROW",
          "Malformed CSV quoting or unclosed quote on source row " + rowNum
      );
    }
    if (record.columns().size() != canonicalHeaders.size()) {
      return ParsedImportRow.malformed(
          rowNum,
          "MALFORMED_CSV_ROW",
          "CSV column count (" + record.columns().size()
              + ") does not match header count (" + canonicalHeaders.size() + ")"
      );
    }

    Map<String, String> rowMap = new LinkedHashMap<>(canonicalHeaders.size() * 2);
    for (int i = 0; i < canonicalHeaders.size(); i++) {
      String rawValue = record.columns().get(i);
      rowMap.put(canonicalHeaders.get(i), rawValue);
    }
    return ParsedImportRow.data(rowNum, rowMap);
  }

  private RawCsvRecord readRawRecord(boolean stripBom) throws IOException {
    List<String> fields = new ArrayList<>();
    StringBuilder fieldBuf = new StringBuilder();
    State state = State.AT_FIELD_START;
    boolean malformedQuote = false;
    boolean sawQuotes = false;
    int charsRead = 0;

    while (true) {
      int ch = reader.read();
      if (stripBom && charsRead == 0 && ch == '\uFEFF') {
        ch = reader.read();
      }

      if (ch == -1) {
        if (charsRead == 0) {
          return new RawCsvRecord(true, false, false, List.of());
        }
        if (state == State.IN_QUOTED_FIELD) {
          malformedQuote = true;
        }
        fields.add(fieldBuf.toString());
        break;
      }

      charsRead++;
      if (charsRead > maxRowBytes) {
        throw new ImportStreamParseException(
            "ROW_TOO_LARGE",
            "CSV record exceeds maximum configured row length of " + maxRowBytes + " characters"
        );
      }

      char c = (char) ch;
      switch (state) {
        case AT_FIELD_START -> {
          if (c == '"') {
            sawQuotes = true;
            state = State.IN_QUOTED_FIELD;
          } else if (c == ',') {
            fields.add("");
          } else if (c == '\r') {
            consumeOptionalLf();
            if (fields.isEmpty() && !sawQuotes && fieldBuf.isEmpty()) {
              return new RawCsvRecord(false, true, false, List.of());
            }
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == '\n') {
            if (fields.isEmpty() && !sawQuotes && fieldBuf.isEmpty()) {
              return new RawCsvRecord(false, true, false, List.of());
            }
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else {
            fieldBuf.append(c);
            state = State.IN_UNQUOTED_FIELD;
          }
        }
        case IN_UNQUOTED_FIELD -> {
          if (c == ',') {
            fields.add(fieldBuf.toString());
            fieldBuf.setLength(0);
            state = State.AT_FIELD_START;
          } else if (c == '\r') {
            consumeOptionalLf();
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == '\n') {
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == '"') {
            malformedQuote = true;
            fieldBuf.append(c);
          } else {
            fieldBuf.append(c);
          }
        }
        case IN_QUOTED_FIELD -> {
          if (c == '"') {
            state = State.AFTER_QUOTE_IN_QUOTED;
          } else {
            fieldBuf.append(c);
          }
        }
        case AFTER_QUOTE_IN_QUOTED -> {
          if (c == '"') {
            fieldBuf.append('"');
            state = State.IN_QUOTED_FIELD;
          } else if (c == ',') {
            fields.add(fieldBuf.toString());
            fieldBuf.setLength(0);
            state = State.AT_FIELD_START;
          } else if (c == '\r') {
            consumeOptionalLf();
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == '\n') {
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == ' ' || c == '\t') {
            // Tolerate harmless trailing spaces after closing quote before delimiter/EOL
          } else {
            malformedQuote = true;
            state = State.AFTER_QUOTE_BAD_TRAILING;
          }
        }
        case AFTER_QUOTE_BAD_TRAILING -> {
          if (c == ',') {
            fields.add(fieldBuf.toString());
            fieldBuf.setLength(0);
            state = State.AT_FIELD_START;
          } else if (c == '\r') {
            consumeOptionalLf();
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          } else if (c == '\n') {
            fields.add(fieldBuf.toString());
            return buildRecordResult(fields, malformedQuote, sawQuotes);
          }
        }
      }
    }

    return buildRecordResult(fields, malformedQuote, sawQuotes);
  }

  private RawCsvRecord buildRecordResult(List<String> fields, boolean malformedQuote, boolean sawQuotes) {
    boolean blankLine = !malformedQuote
        && !sawQuotes
        && fields.size() == 1
        && (fields.get(0) == null || fields.get(0).trim().isEmpty());
    return new RawCsvRecord(false, blankLine, malformedQuote, fields);
  }

  private void consumeOptionalLf() throws IOException {
    reader.mark(1);
    int next = reader.read();
    if (next != '\n' && next != -1) {
      reader.reset();
    }
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
