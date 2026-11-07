package in.gov.sih.sih26135.service.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * High-performance streaming CSV writer conforming strictly to RFC 4180 with built-in
 * spreadsheet formula injection (CSV Injection / CWE-1236) protection.
 *
 * <p>Security & Formatting Invariants:
 * <ul>
 *   <li>Formula Injection Mitigation: Untrusted text starting with {@code =}, {@code +}, {@code -},
 *       {@code @}, tab, or CR is safely prefixed with a single quote ({@code '}) to neutralize
 *       executable formula interpretation in Microsoft Excel, LibreOffice Calc, and Google Sheets.</li>
 *   <li>Preservation of Legitimate Numerics: Valid integers, decimals, negative numbers, and ISO dates
 *       are emitted uncorrupted without unnecessary prefixing.</li>
 *   <li>RFC 4180 Compliant Escaping: Delimiters, double quotes, and embedded newlines are properly quoted;
 *       internal double quotes are doubled ({@code ""}).</li>
 *   <li>Suppressed and Null Values: Emitted as empty fields, ensuring suppressed cells are never
 *       falsely serialized as zeros.</li>
 * </ul>
 */
public class SafeCsvStreamWriter implements AutoCloseable {

  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");
  private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T.*)?$");

  private final Writer writer;
  private int rowCount = 0;

  public SafeCsvStreamWriter(OutputStream outputStream) {
    this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
  }

  public SafeCsvStreamWriter(Writer writer) {
    this.writer = writer;
  }

  /**
   * Writes the CSV column header row.
   *
   * @param headers list of column header names
   * @throws IOException on write failure
   */
  public void writeHeader(List<String> headers) throws IOException {
    writeRawRow(headers);
  }

  /**
   * Writes a data row with full CSV injection sanitization and RFC 4180 escaping.
   *
   * @param values row cell values
   * @throws IOException on write failure
   */
  public void writeRow(List<Object> values) throws IOException {
    if (values == null || values.isEmpty()) {
      writer.write("\r\n");
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      Object val = values.get(i);
      sb.append(formatCell(val));
    }
    sb.append("\r\n");
    writer.write(sb.toString());

    rowCount++;
    if (rowCount % 100 == 0) {
      writer.flush();
    }
  }

  private void writeRawRow(List<String> headers) throws IOException {
    if (headers == null || headers.isEmpty()) {
      writer.write("\r\n");
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < headers.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      String header = headers.get(i);
      sb.append(escapeRfc4180(header != null ? header : ""));
    }
    sb.append("\r\n");
    writer.write(sb.toString());
    writer.flush();
  }

  /**
   * Formats and sanitizes an individual cell value.
   */
  public static String formatCell(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof BigDecimal bd) {
      return bd.toPlainString();
    }
    if (value instanceof Number num) {
      return num.toString();
    }
    if (value instanceof LocalDate ld) {
      return ld.toString();
    }
    if (value instanceof LocalDateTime ldt) {
      return ldt.toString();
    }
    if (value instanceof Boolean b) {
      return b.toString();
    }

    String str = value.toString();
    if (str.isEmpty()) {
      return "";
    }

    // Preserve valid numeric and ISO date strings without quote prefixing
    if (NUMERIC_PATTERN.matcher(str).matches() || ISO_DATE_PATTERN.matcher(str).matches()) {
      return str;
    }

    // Spreadsheet formula injection defense:
    // If the text begins with formula triggers (=, +, -, @, \t, \r), prefix with a single quote
    char firstChar = str.charAt(0);
    if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@'
        || firstChar == '\t' || firstChar == '\r') {
      str = "'" + str;
    }

    return escapeRfc4180(str);
  }

  /**
   * Escapes a string in accordance with RFC 4180.
   */
  private static String escapeRfc4180(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    boolean needsQuotes = text.contains(",")
        || text.contains("\"")
        || text.contains("\n")
        || text.contains("\r")
        || text.startsWith("'");

    if (needsQuotes) {
      return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    return text;
  }

  @Override
  public void close() throws IOException {
    writer.flush();
  }

  public int getRowCount() {
    return rowCount;
  }
}
