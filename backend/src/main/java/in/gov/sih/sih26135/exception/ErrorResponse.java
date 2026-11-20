package in.gov.sih.sih26135.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized, sanitized error response body for API error handling.
 * Designed to prevent leakage of credentials, SQL queries, or internal stack traces.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sanitized standard error response body preventing sensitive leakage")
public class ErrorResponse {

  @Schema(description = "Error type category", example = "VALIDATION_ERROR")
  private String type;

  @Schema(description = "Timestamp when error occurred", example = "2026-11-19T18:00:00")
  private LocalDateTime timestamp;

  @Schema(description = "HTTP status code", example = "400")
  private int status;

  @Schema(description = "HTTP status description", example = "Bad Request")
  private String error;

  @Schema(description = "Machine-readable operational error code", example = "TRAINEE_MISMATCH")
  private String code;

  @Schema(description = "Sanitized human-readable error explanation", example = "Request validation failed")
  private String message;

  @Schema(description = "Target request path where error occurred", example = "/api/v1/trainees")
  private String path;

  @Schema(description = "List of specific validation or error details", example = "[\"traineeId is required\"]")
  private List<String> details;

  @Schema(description = "Unique correlation identifier for tracking in logs", example = "c8f9b2a1-0000-4000-a000-000000000001")
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
