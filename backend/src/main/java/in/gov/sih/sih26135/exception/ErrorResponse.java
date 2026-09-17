package in.gov.sih.sih26135.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized, sanitized error response body for API error handling.
 * Designed to prevent leakage of credentials, SQL queries, or internal stack traces.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private LocalDateTime timestamp;
  private int status;
  private String error;
  private String code;
  private String message;
  private String path;
  private List<String> details;

  public ErrorResponse() {
    this.timestamp = LocalDateTime.now();
  }

  public ErrorResponse(int status, String error, String code, String message, String path) {
    this.timestamp = LocalDateTime.now();
    this.status = status;
    this.error = error;
    this.code = code;
    this.message = message;
    this.path = path;
  }

  public ErrorResponse(int status, String error, String code, String message, String path, List<String> details) {
    this(status, error, code, message, path);
    this.details = details;
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
}
