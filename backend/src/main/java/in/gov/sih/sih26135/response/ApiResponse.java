package in.gov.sih.sih26135.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Standard generic envelope for successful REST API responses.
 *
 * @param <T> the type of the payload data
 */
public class ApiResponse<T> {

  private final boolean success;
  private final String message;
  private final T data;
  private final Instant timestamp;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String path;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String code;

  @JsonCreator
  public ApiResponse(
      @JsonProperty("success") Boolean success,
      @JsonProperty("message") String message,
      @JsonProperty("data") T data,
      @JsonProperty("timestamp") Instant timestamp,
      @JsonProperty("path") String path) {
    this(success, null, message, data, timestamp, path);
  }

  public ApiResponse(
      Boolean success,
      String code,
      String message,
      T data,
      Instant timestamp,
      String path) {
    this.success = success != null ? success : true;
    this.code = code;
    this.message = message;
    this.data = data;
    this.timestamp = timestamp != null ? timestamp : Instant.now();
    this.path = path;
  }

  private ApiResponse(String message, T data, String path) {
    this(true, null, message, data, Instant.now(), path);
  }

  /**
   * Creates a successful API response with message and payload data.
   *
   * @param message human-readable result message
   * @param data    payload data
   * @param <T>     data type
   * @return successful ApiResponse instance
   */
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(message, data, null);
  }

  /**
   * Creates a successful API response with message, payload data, and request path.
   *
   * @param message human-readable result message
   * @param data    payload data
   * @param path    request URI path
   * @param <T>     data type
   * @return successful ApiResponse instance
   */
  public static <T> ApiResponse<T> success(String message, T data, String path) {
    return new ApiResponse<>(message, data, path);
  }

  /**
   * Creates a successful API response with a message and no payload (data = null).
   *
   * @param message human-readable result message
   * @param <T>     data type
   * @return successful ApiResponse instance with null data
   */
  public static <T> ApiResponse<T> success(String message) {
    return new ApiResponse<>(message, null, null);
  }

  /**
   * Creates a successful API response with a message, request path, and no payload (data = null).
   *
   * @param message human-readable result message
   * @param path    request URI path
   * @param <T>     data type
   * @return successful ApiResponse instance with null data
   */
  public static <T> ApiResponse<T> success(String message, String path) {
    return new ApiResponse<>(message, null, path);
  }

  /**
   * Creates a successful API response with payload data and a default success message.
   *
   * @param data payload data
   * @param <T>  data type
   * @return successful ApiResponse instance
   */
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>("Operation completed successfully", data, null);
  }

  /**
   * Creates an error API response with a machine-readable code, human-readable message, and request path.
   *
   * @param code    machine-readable error code
   * @param message human-readable error message
   * @param path    request URI path
   * @param <T>     data type
   * @return error ApiResponse instance with null data and success=false
   */
  public static <T> ApiResponse<T> error(String code, String message, String path) {
    return new ApiResponse<>(false, code, message, null, Instant.now(), path);
  }

  /**
   * Creates an error API response with a message and request path.
   *
   * @param message human-readable error message
   * @param path    request URI path
   * @param <T>     data type
   * @return error ApiResponse instance with null data and success=false
   */
  public static <T> ApiResponse<T> error(String message, String path) {
    return new ApiResponse<>(false, null, message, null, Instant.now(), path);
  }

  /**
   * Creates an error API response with a message and no path.
   *
   * @param message human-readable error message
   * @param <T>     data type
   * @return error ApiResponse instance with null data and success=false
   */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, null, message, null, Instant.now(), null);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "ApiResponse{" +
        "success=" + success +
        ", code='" + code + '\'' +
        ", message='" + message + '\'' +
        ", data=" + data +
        ", timestamp=" + timestamp +
        ", path='" + path + '\'' +
        '}';
  }
}
