package in.gov.sih.sih26135.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized, sanitized error response body for API error handling.
 * Designed to prevent leakage of credentials, SQL queries, or internal stack traces.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private String type;
  private LocalDateTime timestamp;
  private int status;
  private String error;
  private String code;
  private String message;
  private String path;
  private List<String> details;
  private String correlationId;

  public ErrorResponse() {
    this.timestamp = LocalDateTime.now();
    this.correlationId = CorrelationContextHolder.getCorrelationId();
  }

  public ErrorResponse(int status, String error, String code, String message, String path) {
    this.timestamp = LocalDateTime.now();
    this.status = status;
    this.error = error;
    this.code = code;
    this.message = message;
    this.path = path;
    this.correlationId = CorrelationContextHolder.getCorrelationId();
  }

  public ErrorResponse(int status, String error, String code, String message, String path, List<String> details) {
    this(status, error, code, message, path);
    this.details = details;
  }

  public ErrorResponse(int status, String error, String code, String message, String path, List<String> details, String correlationId) {
    this(status, error, code, message, path, details);
    if (correlationId != null) {
      this.correlationId = correlationId;
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<String> getDetails() {
    return details;
  }

  public void setDetails(List<String> details) {
    this.details = details;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
}
