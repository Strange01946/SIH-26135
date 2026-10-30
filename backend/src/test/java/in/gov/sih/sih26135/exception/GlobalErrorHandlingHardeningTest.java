package in.gov.sih.sih26135.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 26.8 - Global Error Handling & Exception Mapping Hardening Tests")
class GlobalErrorHandlingHardeningTest {

  private MockMvc mockMvc;
  private GlobalExceptionHandler exceptionHandler;
  private ObjectMapper objectMapper;

  public enum TestStatus {
    ACTIVE,
    PENDING,
    COMPLETED
  }

  public static class SampleRequestPayload {
    @NotBlank(message = "name must not be blank")
    private String name;

    @NotNull(message = "status is required")
    private TestStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Min(value = 1, message = "count must be greater than or equal to 1")
    private Integer count;

    public SampleRequestPayload() {}

    public SampleRequestPayload(String name, TestStatus status, LocalDate startDate, Integer count) {
      this.name = name;
      this.status = status;
      this.startDate = startDate;
      this.count = count;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
  }

  @RestController
  @RequestMapping("/api/test/hardening")
  static class HardeningTestController {

    @PostMapping(value = "/payload", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processPayload(@RequestBody @Valid SampleRequestPayload payload) {
      return ResponseEntity.ok("processed:" + payload.getName());
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getResource() {
      return ResponseEntity.ok("ok-resource");
    }

    @PostMapping(value = "/resource", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postResource(@RequestBody String body) {
      return ResponseEntity.ok("created-resource");
    }

    @GetMapping("/param-filter")
    public ResponseEntity<String> filterParam(
        @RequestParam("code") String code,
        @RequestParam(value = "amount", required = false) Long amount) {
      return ResponseEntity.ok("code=" + code + ",amount=" + amount);
    }

    @GetMapping("/enum-filter")
    public ResponseEntity<String> enumFilter(@RequestParam("status") TestStatus status) {
      return ResponseEntity.ok("status=" + status);
    }

    @GetMapping("/path-check/{id}")
    public ResponseEntity<String> pathCheck(@PathVariable("id") Long id) {
      return ResponseEntity.ok("id=" + id);
    }

    @GetMapping("/server-mismatch-route")
    public ResponseEntity<String> brokenRoute(@PathVariable("unmappedVariable") String var) {
      return ResponseEntity.ok("var=" + var);
    }
  }

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    mockMvc = MockMvcBuilders.standaloneSetup(new HardeningTestController())
        .setControllerAdvice(exceptionHandler)
        .build();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  // =========================================================================
  // 1. MethodArgumentNotValidException (Request DTO Bean Validation)
  // =========================================================================
  @Nested
  @DisplayName("1. MethodArgumentNotValidException - Request DTO Validation")
  class MethodArgumentNotValidTests {

    @Test
    @DisplayName("Validation failure returns HTTP 400 with VALIDATION_ERROR and sorted field errors")
    void testMethodArgumentNotValid_returnsHttp400WithSortedErrors() throws Exception {
      String invalidJson = """
          {
            "name": "",
            "count": 0
          }
          """;

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.path").value("/api/test/hardening/payload"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details[0]").value("count: count must be greater than or equal to 1"))
          .andExpect(jsonPath("$.details[1]").value("name: name must not be blank"))
          .andExpect(jsonPath("$.details[2]").value("status: status is required"));
    }
  }

  // =========================================================================
  // 2. HttpMessageNotReadableException (Malformed JSON, Enums, Dates, Types)
  // =========================================================================
  @Nested
  @DisplayName("2. HttpMessageNotReadableException - Request Body Deserialization")
  class HttpMessageNotReadableTests {

    @Test
    @DisplayName("Malformed JSON syntax returns HTTP 400 MALFORMED_JSON without echoing raw content")
    void testMalformedJson_returnsHttp400() throws Exception {
      String brokenJson = "{\"name\": \"SecretPayload\", \"count\": }";

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(brokenJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"))
          .andExpect(jsonPath("$.path").value("/api/test/hardening/payload"))
          .andExpect(jsonPath("$.message", not(containsString("SecretPayload"))));
    }

    @Test
    @DisplayName("Invalid enum value in body returns HTTP 400 MALFORMED_JSON with field identification and no class disclosure")
    void testInvalidEnumValue_returnsHttp400Safely() throws Exception {
      String jsonWithInvalidEnum = """
          {
            "name": "Test Item",
            "status": "NON_EXISTENT_ENUM_VALUE",
            "count": 5
          }
          """;

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(jsonWithInvalidEnum))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Invalid enum value for field 'status'"))
          .andExpect(jsonPath("$.details[0]").value("status: invalid enum value"))
          .andExpect(jsonPath("$.message", not(containsString("TestStatus"))))
          .andExpect(jsonPath("$.message", not(containsString("NON_EXISTENT_ENUM_VALUE"))));
    }

    @Test
    @DisplayName("Invalid date/time format in body returns HTTP 400 MALFORMED_JSON with safe message")
    void testInvalidDateFormat_returnsHttp400Safely() throws Exception {
      String jsonWithInvalidDate = """
          {
            "name": "Test Item",
            "status": "ACTIVE",
            "startDate": "28/10/2026-bad-format",
            "count": 5
          }
          """;

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(jsonWithInvalidDate))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Invalid date/time format for field 'startDate'"))
          .andExpect(jsonPath("$.details[0]").value("startDate: invalid date/time format"))
          .andExpect(jsonPath("$.message", not(containsString("28/10/2026-bad-format"))));
    }

    @Test
    @DisplayName("Incompatible type in body returns HTTP 400 MALFORMED_JSON with safe message")
    void testIncompatibleType_returnsHttp400Safely() throws Exception {
      String jsonWithIncompatibleType = """
          {
            "name": "Test Item",
            "status": "ACTIVE",
            "count": "not-an-integer-number"
          }
          """;

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(jsonWithIncompatibleType))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Invalid value format for field 'count'"))
          .andExpect(jsonPath("$.details[0]").value("count: invalid format"))
          .andExpect(jsonPath("$.message", not(containsString("not-an-integer-number"))));
    }

    @Test
    @DisplayName("Missing request body on required payload returns HTTP 400 MALFORMED_JSON")
    void testMissingRequestBody_returnsHttp400() throws Exception {
      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Required request body is missing"));
    }
  }

  // =========================================================================
  // 3. MethodArgumentTypeMismatchException (Path and Query Conversion)
  // =========================================================================
  @Nested
  @DisplayName("3. MethodArgumentTypeMismatchException - Parameter Conversion")
  class MethodArgumentTypeMismatchTests {

    @Test
    @DisplayName("Non-numeric path variable returns HTTP 400 TYPE_MISMATCH")
    void testPathTypeMismatch_returnsHttp400() throws Exception {
      mockMvc.perform(get("/api/test/hardening/path-check/abc"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'id'. Expected type: Long"))
          .andExpect(jsonPath("$.path").value("/api/test/hardening/path-check/{id}"))
          .andExpect(jsonPath("$.message", not(containsString("abc"))));
    }

    @Test
    @DisplayName("Non-numeric query parameter returns HTTP 400 TYPE_MISMATCH")
    void testQueryTypeMismatch_returnsHttp400() throws Exception {
      mockMvc.perform(get("/api/test/hardening/param-filter?code=TEST&amount=not-a-number"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'amount'. Expected type: Long"))
          .andExpect(jsonPath("$.message", not(containsString("not-a-number"))));
    }

    @Test
    @DisplayName("Invalid enum query parameter returns HTTP 400 TYPE_MISMATCH without leaking enum class")
    void testEnumQueryTypeMismatch_returnsHttp400Safely() throws Exception {
      mockMvc.perform(get("/api/test/hardening/enum-filter?status=INVALID_STATUS_CODE"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'status'. Expected type: valid enum value"))
          .andExpect(jsonPath("$.message", not(containsString("TestStatus"))))
          .andExpect(jsonPath("$.message", not(containsString("INVALID_STATUS_CODE"))));
    }
  }

  // =========================================================================
  // 4. MissingServletRequestParameterException (Missing Mandatory Parameters)
  // =========================================================================
  @Nested
  @DisplayName("4. MissingServletRequestParameterException - Missing Parameters")
  class MissingServletRequestParameterTests {

    @Test
    @DisplayName("Missing mandatory query parameter returns HTTP 400 MISSING_PARAMETER")
    void testMissingQueryParameter_returnsHttp400() throws Exception {
      mockMvc.perform(get("/api/test/hardening/param-filter"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
          .andExpect(jsonPath("$.message").value("Required parameter 'code' is missing"))
          .andExpect(jsonPath("$.path").value("/api/test/hardening/param-filter"));
    }
  }

  // =========================================================================
  // 5. MissingPathVariableException (Server Route Mapping Defect -> HTTP 500)
  // =========================================================================
  @Nested
  @DisplayName("5. MissingPathVariableException - Server Routing Defect")
  class MissingPathVariableTests {

    @Test
    @DisplayName("Missing path variable mapping returns HTTP 500 without leaking internal variable name")
    void testMissingPathVariable_returnsHttp500Sanitized() throws Exception {
      mockMvc.perform(get("/api/test/hardening/server-mismatch-route"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.error").value("Internal Server Error"))
          .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
          .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
          .andExpect(jsonPath("$.message", not(containsString("unmappedVariable"))));
    }
  }

  // =========================================================================
  // 6. HttpRequestMethodNotSupportedException (HTTP 405 + Allow Header)
  // =========================================================================
  @Nested
  @DisplayName("6. HttpRequestMethodNotSupportedException - Method Not Allowed & Allow Header")
  class HttpRequestMethodNotSupportedTests {

    @Test
    @DisplayName("Unsupported HTTP method returns HTTP 405 METHOD_NOT_ALLOWED and preserves Allow header")
    void testMethodNotSupported_returnsHttp405WithAllowHeader() throws Exception {
      mockMvc.perform(patch("/api/test/hardening/resource"))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(header().exists(HttpHeaders.ALLOW))
          .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
          .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
          .andExpect(jsonPath("$.status").value(405))
          .andExpect(jsonPath("$.error").value("Method Not Allowed"))
          .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
          .andExpect(jsonPath("$.message").value("HTTP method 'PATCH' is not supported for this endpoint"))
          .andExpect(jsonPath("$.path").value("/sanitized-path"));
    }
  }

  // =========================================================================
  // 7. HttpMediaTypeNotSupportedException (HTTP 415 + Accept Header)
  // =========================================================================
  @Nested
  @DisplayName("7. HttpMediaTypeNotSupportedException - Unsupported Media Type & Accept Header")
  class HttpMediaTypeNotSupportedTests {

    @Test
    @DisplayName("Unsupported media type returns HTTP 415 UNSUPPORTED_MEDIA_TYPE and preserves Accept header")
    void testMediaTypeNotSupported_returnsHttp415WithAcceptHeader() throws Exception {
      mockMvc.perform(post("/api/test/hardening/resource")
              .contentType(MediaType.APPLICATION_XML)
              .content("<data>test</data>"))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(header().exists(HttpHeaders.ACCEPT))
          .andExpect(header().string(HttpHeaders.ACCEPT, containsString("application/json")))
          .andExpect(jsonPath("$.status").value(415))
          .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
          .andExpect(jsonPath("$.message").value(containsString("Content type 'application/xml")))
          .andExpect(jsonPath("$.path").value("/sanitized-path"));
    }
  }

  // =========================================================================
  // 8. RFC 7807 Compliance & Uniform Error Envelope Verification
  // =========================================================================
  @Nested
  @DisplayName("8. RFC 7807 Compliance & Error Envelope Verification")
  class Rfc7807ComplianceTests {

    @Test
    @DisplayName("ErrorResponse JSON serialization produces uniform RFC 7807-compatible structure")
    void testErrorResponse_rfc7807SerializationCompatibility() throws Exception {
      ErrorResponse errorResponse = new ErrorResponse(
          400,
          "Bad Request",
          "VALIDATION_ERROR",
          "Request validation failed",
          "/api/v1/courses",
          List.of("code: must not be blank")
      );

      String json = objectMapper.writeValueAsString(errorResponse);

      // Verify uniform field presence
      assertThat(json).contains("\"status\":400");
      assertThat(json).contains("\"error\":\"Bad Request\"");
      assertThat(json).contains("\"code\":\"VALIDATION_ERROR\"");
      assertThat(json).contains("\"message\":\"Request validation failed\"");
      assertThat(json).contains("\"path\":\"/api/v1/courses\"");
      assertThat(json).contains("\"timestamp\":");
      assertThat(json).contains("\"details\":[\"code: must not be blank\"]");

      // Verify null optional fields like 'type' are omitted by NON_NULL policy
      assertThat(json).doesNotContain("\"type\":");

      // Verify agreement of status code
      assertThat(errorResponse.getStatus()).isEqualTo(400);
      assertThat(errorResponse.getError()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("ErrorResponse allows explicit RFC 7807 type URI when configured")
    void testErrorResponse_explicitTypeUriSerialization() throws Exception {
      ErrorResponse errorResponse = new ErrorResponse(
          404,
          "Not Found",
          "RESOURCE_NOT_FOUND",
          "The requested resource was not found",
          "/api/v1/courses/999"
      );
      errorResponse.setType("https://api.sih26135.gov.in/errors/resource-not-found");

      String json = objectMapper.writeValueAsString(errorResponse);

      assertThat(json).contains("\"type\":\"https://api.sih26135.gov.in/errors/resource-not-found\"");
      assertThat(json).contains("\"status\":404");
      assertThat(json).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
    }

    @Test
    @DisplayName("Valid request processing proceeds uninterrupted (HTTP 200)")
    void testValidRequestProcessing_succeedsWith200() throws Exception {
      SampleRequestPayload validPayload = new SampleRequestPayload(
          "Valid Course", TestStatus.ACTIVE, LocalDate.of(2026, 11, 1), 10);

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validPayload)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").value("processed:Valid Course"));
    }
  }

  // =========================================================================
  // 9. Phase 26.9 - Error Sanitization & Information Leakage Prevention MockMvc Integration Tests
  // =========================================================================
  @Nested
  @DisplayName("9. Phase 26.9 - Error Sanitization & Information Leakage Prevention MockMvc Tests")
  class ErrorSanitizationMockMvcTests {

    @Test
    @DisplayName("404 endpoint not found: Aadhaar in request URI path is sanitized and not reflected")
    void testNoResourceFound_withAadhaarInUri_doesNotLeakAadhaar() throws Exception {
      String probePath = "/api/test/hardening/trainees/123456789012";

      mockMvc.perform(get(probePath))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
          .andExpect(jsonPath("$.path").value("/sanitized-path"))
          .andExpect(jsonPath("$.message").value("No static resource or endpoint found"))
          .andExpect(jsonPath("$.message", not(containsString("123456789012"))));
    }

    @Test
    @DisplayName("404 endpoint not found: Sentinel token in request URI path is sanitized and not reflected")
    void testNoResourceFound_withSentinelTokenInUri_doesNotLeakToken() throws Exception {
      String probePath = "/api/test/hardening/auth/tokens/SENTINEL_TOKEN_SECRET_XYZ";

      mockMvc.perform(get(probePath))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
          .andExpect(jsonPath("$.path").value("/sanitized-path"))
          .andExpect(jsonPath("$.message").value("No static resource or endpoint found"))
          .andExpect(jsonPath("$.message", not(containsString("SENTINEL"))));
    }

    @Test
    @DisplayName("Malformed JSON: Broken payload with sensitive credentials is not reflected in error response")
    void testMalformedJson_withSensitivePayload_suppressesCredentials() throws Exception {
      String brokenPayload = "{\"password\": \"SuperSecretAdminPass123!\", \"count\": }";

      mockMvc.perform(post("/api/test/hardening/payload")
              .contentType(MediaType.APPLICATION_JSON)
              .content(brokenPayload))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"))
          .andExpect(jsonPath("$.message", not(containsString("SuperSecretAdminPass123!"))));
    }

    @Test
    @DisplayName("Type mismatch: Invalid parameter value is not reflected in error message (anti-echo)")
    void testTypeMismatch_invalidParameterValue_isNotReflected() throws Exception {
      String invalidValue = "not-a-valid-numeric-id";

      mockMvc.perform(get("/api/test/hardening/path-check/" + invalidValue))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'id'. Expected type: Long"))
          .andExpect(jsonPath("$.message", not(containsString(invalidValue))));
    }

    @Test
    @DisplayName("Valid endpoint processing preserves standard API contracts")
    void testValidEndpoint_preservesContract() throws Exception {
      mockMvc.perform(get("/api/test/hardening/path-check/42"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").value("id=42"));
    }
  }
}
