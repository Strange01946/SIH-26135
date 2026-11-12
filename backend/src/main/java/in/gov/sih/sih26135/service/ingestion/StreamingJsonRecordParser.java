package in.gov.sih.sih26135.service.ingestion;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Memory-bounded Jackson streaming parser supporting both top-level JSON arrays ({@code [{...},{...}]})
 * and NDJSON object streams ({@code {...}\n{...}}), reading one JSON object at a time without loading
 * entire payloads into memory.
 */
public class StreamingJsonRecordParser implements StreamingImportParser {

  private static final int MAX_FIELDS_PER_OBJECT = 64;

  private final JsonParser parser;
  private final String entityType;
  private final int maxRowBytes;
  private final boolean arrayMode;
  private JsonToken pendingToken;
  private int currentRowNumber = 0;

  public StreamingJsonRecordParser(InputStream inputStream, String entityType, int maxRowBytes) {
    if (inputStream == null) {
      throw new ImportStreamParseException("STAGED_FILE_NOT_FOUND", "JSON input stream cannot be null");
    }
    this.entityType = entityType;
    this.maxRowBytes = Math.max(maxRowBytes, 1024);

    StreamReadConstraints constraints = StreamReadConstraints.builder()
        .maxStringLength(this.maxRowBytes)
        .maxNumberLength(64)
        .maxNestingDepth(16)
        .build();
    JsonFactory factory = JsonFactory.builder()
        .streamReadConstraints(constraints)
        .build();

    try {
      this.parser = factory.createParser(inputStream);
      JsonToken firstToken = this.parser.nextToken();
      if (firstToken == null) {
        throw new ImportStreamParseException("NO_DATA_ROWS", "JSON import file contains no records");
      }
      if (firstToken == JsonToken.START_ARRAY) {
        this.arrayMode = true;
        this.pendingToken = this.parser.nextToken();
      } else if (firstToken == JsonToken.START_OBJECT) {
        this.arrayMode = false;
        this.pendingToken = firstToken;
      } else {
        throw new ImportStreamParseException(
            "MALFORMED_JSON_FILE",
            "JSON import must begin with a JSON array or newline-delimited JSON objects"
        );
      }
    } catch (ImportStreamParseException e) {
      closeQuietly();
      throw e;
    } catch (JacksonException e) {
      closeQuietly();
      throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Invalid JSON syntax at stream start", e);
    } catch (IOException e) {
      closeQuietly();
      throw new ImportStreamParseException("JSON_READ_ERROR", "I/O error reading JSON stream start", e);
    }
  }

  @Override
  public ParsedImportRow nextRow() throws ImportStreamParseException {
    try {
      if (arrayMode) {
        if (pendingToken == JsonToken.END_ARRAY) {
          if (parser.nextToken() != null) {
            throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Unexpected trailing content after JSON array");
          }
          return null;
        }
        if (pendingToken == null) {
          throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Unexpected EOF before closing JSON array");
        }
      } else if (pendingToken == null) {
        return null;
      }

      int rowNum = ++currentRowNumber;
      if (pendingToken == JsonToken.VALUE_NULL) {
        pendingToken = parser.nextToken();
        return ParsedImportRow.skipped(rowNum, "EMPTY_ROW", "Null JSON record skipped per empty-row policy");
      }

      if (pendingToken != JsonToken.START_OBJECT) {
        if (arrayMode) {
          parser.skipChildren();
          pendingToken = parser.nextToken();
          return ParsedImportRow.malformed(
              rowNum,
              "MALFORMED_JSON_ROW",
              "Import record element at row " + rowNum + " must be a JSON object"
          );
        }
        throw new ImportStreamParseException("MALFORMED_JSON_FILE", "NDJSON stream contains non-object token");
      }

      return readSingleObjectRecord(rowNum);
    } catch (ImportStreamParseException e) {
      throw e;
    } catch (JacksonException e) {
      throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Malformed JSON syntax in import stream", e);
    } catch (IOException e) {
      throw new ImportStreamParseException("JSON_READ_ERROR", "I/O error reading JSON stream", e);
    }
  }

  private ParsedImportRow readSingleObjectRecord(int rowNum) throws IOException {
    Map<String, String> fields = new LinkedHashMap<>();
    String rowErrorCode = null;
    String rowErrorMessage = null;
    int fieldCount = 0;

    while (true) {
      JsonToken token = parser.nextToken();
      if (token == null) {
        throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Unexpected EOF inside JSON object");
      }
      if (token == JsonToken.END_OBJECT) {
        break;
      }
      if (token != JsonToken.FIELD_NAME) {
        throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Expected field name inside JSON object");
      }

      fieldCount++;
      String rawName = parser.currentName();
      JsonToken valueToken = parser.nextToken();
      if (valueToken == null) {
        throw new ImportStreamParseException("MALFORMED_JSON_FILE", "Unexpected EOF reading JSON field value");
      }

      if (fieldCount > MAX_FIELDS_PER_OBJECT) {
        if (valueToken.isStructStart()) {
          parser.skipChildren();
        }
        if (rowErrorCode == null) {
          rowErrorCode = "MALFORMED_JSON_ROW";
          rowErrorMessage = "JSON record exceeds maximum supported field count";
        }
        continue;
      }

      if (valueToken.isStructStart()) {
        parser.skipChildren();
        if (rowErrorCode == null) {
          rowErrorCode = "MALFORMED_JSON_ROW";
          rowErrorMessage = "Nested objects or arrays are not supported in import records";
        }
        continue;
      }

      String canonical = ImportContractSchema.normalizeFieldName(entityType, rawName);
      if (canonical == null) {
        if (rowErrorCode == null) {
          String safeKey = rawName != null ? rawName.replaceAll("[^A-Za-z0-9_.-]", "_") : "unknown";
          if (safeKey.length() > 40) {
            safeKey = safeKey.substring(0, 40);
          }
          rowErrorCode = "UNSUPPORTED_FIELD";
          rowErrorMessage = "Unsupported import field: " + safeKey;
        }
        continue;
      }

      if (valueToken == JsonToken.VALUE_NULL) {
        fields.putIfAbsent(canonical, null);
      } else {
        String scalarValue = parser.getValueAsString();
        if (scalarValue != null && scalarValue.length() > maxRowBytes) {
          if (rowErrorCode == null) {
            rowErrorCode = "ROW_TOO_LARGE";
            rowErrorMessage = "Field value exceeds maximum configured row length";
          }
        } else {
          fields.put(canonical, scalarValue);
        }
      }
    }

    pendingToken = parser.nextToken();
    if (rowErrorCode != null) {
      return ParsedImportRow.malformed(rowNum, rowErrorCode, rowErrorMessage);
    }
    return ParsedImportRow.data(rowNum, fields);
  }

  @Override
  public void close() throws IOException {
    parser.close();
  }

  private void closeQuietly() {
    try {
      if (parser != null) {
        parser.close();
      }
    } catch (IOException ignored) {
    }
  }
}
