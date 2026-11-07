package in.gov.sih.sih26135.service.export;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * True streaming JSON writer that produces a valid, top-level structured JSON document
 * while writing row records incrementally to prevent large in-memory collection materialization.
 *
 * <p>Produces:
 * <pre>{@code
 * {
 *   "reportType": "...",
 *   "reportingPeriod": { ... },
 *   "scope": { ... },
 *   "generatedAt": "...",
 *   "summary": { ... },
 *   "rows": [ ... ],
 *   "metadata": { "privacyThreshold": 5, "totalRows": N }
 * }
 * }</pre>
 */
public class StructuredJsonStreamWriter implements AutoCloseable {

  private final JsonGenerator jg;
  private int rowCount = 0;

  public StructuredJsonStreamWriter(OutputStream outputStream, ObjectMapper objectMapper) throws IOException {
    this.jg = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);
    this.jg.setCodec(objectMapper);
  }

  public void writeRootStart(
      String reportType,
      Map<String, Object> reportingPeriod,
      Map<String, Object> scope,
      String generatedAt) throws IOException {
    jg.writeStartObject();
    jg.writeStringField("reportType", reportType);
    jg.writeObjectField("reportingPeriod", reportingPeriod);
    jg.writeObjectField("scope", scope);
    jg.writeStringField("generatedAt", generatedAt);
  }

  public void writeSummary(Object summary) throws IOException {
    if (summary != null) {
      jg.writeObjectField("summary", summary);
    }
  }

  public void startRows() throws IOException {
    jg.writeArrayFieldStart("rows");
  }

  public void writeRow(Object row) throws IOException {
    if (row != null) {
      jg.writeObject(row);
      rowCount++;
    }
  }

  public void endRows() throws IOException {
    jg.writeEndArray();
  }

  public void writeMetadata(int totalRows, long privacyThreshold) throws IOException {
    jg.writeObjectFieldStart("metadata");
    jg.writeNumberField("privacyThreshold", privacyThreshold);
    jg.writeNumberField("totalRows", totalRows);
    jg.writeEndObject();
  }

  public void writeRootEnd() throws IOException {
    jg.writeEndObject();
    jg.flush();
  }

  @Override
  public void close() throws IOException {
    jg.close();
  }

  public int getRowCount() {
    return rowCount;
  }
}
