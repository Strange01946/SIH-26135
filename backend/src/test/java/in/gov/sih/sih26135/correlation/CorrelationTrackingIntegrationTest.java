package in.gov.sih.sih26135.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.exception.ErrorResponse;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.config.SecurityCorsProperties;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprehensive verification test suite for Phase 26.10 Request Correlation Tracking.
 *
 * <p>Verifies:
 * <ol>
 *   <li>Auto-generation of standard UUID when headers are absent.</li>
 *   <li>Preservation of valid incoming {@code X-Correlation-Id}.</li>
 *   <li>Preservation of valid incoming {@code X-Request-Id} when {@code X-Correlation-Id} is absent.</li>
 *   <li>Conflict resolution: {@code X-Correlation-Id} takes strict precedence over {@code X-Request-Id}.</li>
 *   <li>Security sanitization: Malformed, CRLF, oversized, or special-char headers are rejected and replaced.</li>
 *   <li>Response header propagation: Both {@code X-Correlation-Id} and {@code X-Request-Id} are returned.</li>
 *   <li>Envelope inclusion: Correlation ID is present in both {@link ApiResponse} and {@link ErrorResponse}.</li>
 *   <li>Security filter error mapping: 401 Unauthorized and 403 Forbidden envelopes include correlation IDs.</li>
 *   <li>HTTP status coverage: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 405 Method Not Allowed, and 500 Internal Server Error.</li>
 *   <li>Lifecycle cleanup: ThreadLocal and SLF4J MDC are cleared after both normal and exceptional flows.</li>
 *   <li>Sequential isolation: No correlation ID leakage across requests on the same thread.</li>
 *   <li>Direct handler verification: Both overloads of {@link GlobalExceptionHandler#buildSanitizedResponse} attach correlation headers to {@link ResponseEntity}.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.10 - Correlation Tracking & Request Context Verification")
public class CorrelationTrackingIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SecurityCorsProperties securityCorsProperties;

  @Autowired
  private CorrelationIdFilter correlationIdFilter;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private GlobalExceptionHandler globalExceptionHandler;

  @BeforeEach
  @AfterEach
  void cleanThreadContext() {
    CorrelationContextHolder.clear();
    MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
    MDC.remove(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
  }

  @Nested
  @DisplayName("1. Ingress Header Resolution & Precedence Tests")
  class HeaderResolutionTests {

    @Test
    @DisplayName("1.1 Auto-generates valid UUID when correlation headers are absent")
    void testAutoGenerateCorrelationId_whenHeadersAbsent() throws Exception {
      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andReturn();

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      String reqHeader = result.getResponse().getHeader("X-Request-Id");

      assertThat(corrHeader).isNotBlank();
      assertThat(reqHeader).isEqualTo(corrHeader);
      assertThat(UUID.fromString(corrHeader)).isNotNull();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.has("correlationId")).isTrue();
      assertThat(root.get("correlationId").asText()).isEqualTo(corrHeader);
    }

    @Test
    @DisplayName("1.2 Preserves valid custom X-Correlation-Id header")
    void testPreserveCustomCorrelationId_whenValid() throws Exception {
      String customId = "trace-client-12345-abc";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", customId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(header().string("X-Correlation-Id", customId))
          .andExpect(header().string("X-Request-Id", customId))
          .andExpect(jsonPath("$.correlationId").value(customId))
          .andReturn();

      assertThat(result.getResponse().getHeader("X-Correlation-Id")).isEqualTo(customId);
    }

    @Test
    @DisplayName("1.3 Preserves valid X-Request-Id when X-Correlation-Id is absent")
    void testPreserveCustomRequestId_whenCorrelationIdAbsent() throws Exception {
      String requestId = "req-service-67890-xyz";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Request-Id", requestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(header().string("X-Correlation-Id", requestId))
          .andExpect(header().string("X-Request-Id", requestId))
          .andExpect(jsonPath("$.correlationId").value(requestId))
          .andReturn();

      assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo(requestId);
    }

    @Test
    @DisplayName("1.4 X-Correlation-Id takes strict precedence over X-Request-Id in conflict")
    void testPrecedence_xCorrelationIdTakesPrecedenceOverXRequestId() throws Exception {
      String correlationId = "primary-corr-111";
      String requestId = "secondary-req-222";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", correlationId)
              .header("X-Request-Id", requestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      assertThat(result.getResponse().getHeader("X-Correlation-Id")).isEqualTo(correlationId);
      assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo(correlationId);
    }
  }

  @Nested
  @DisplayName("2. Security Sanitization & Log-Injection Defense Tests")
  class SanitizationDefenseTests {

    @Test
    @DisplayName("2.1 CRLF injection in X-Correlation-Id is rejected and replaced with safe UUID")
    void testRejectCrlfInjection() throws Exception {
      String maliciousHeader = "test-id\r\nInjected-Header: evil";

      // Verify unit validator explicitly rejects CRLF header
      assertThat(CorrelationIdFilter.isValidCorrelationId(maliciousHeader)).isFalse();

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", maliciousHeader)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andReturn();

      assertThat(result.getResponse().getStatus()).isIn(400, 500);

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      assertThat(corrHeader).isNotNull();
      assertThat(corrHeader).doesNotContain("\r", "\n", "Injected", "evil");
      assertThat(UUID.fromString(corrHeader)).isNotNull();
    }

    @Test
    @DisplayName("2.2 HTML/Script tags in header are rejected and replaced with safe UUID")
    void testRejectHtmlAndSpecialCharacters() throws Exception {
      String maliciousHeader = "<script>alert('xss')</script>";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", maliciousHeader)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      assertThat(corrHeader).isNotNull();
      assertThat(corrHeader).doesNotContain("<", ">", "script", "alert");
      assertThat(UUID.fromString(corrHeader)).isNotNull();
    }

    @Test
    @DisplayName("2.3 Oversized header (> 64 characters) is rejected and replaced with safe UUID")
    void testRejectOversizedHeader() throws Exception {
      String oversizedHeader = "a".repeat(65);

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", oversizedHeader)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      assertThat(corrHeader).isNotNull();
      assertThat(corrHeader).isNotEqualTo(oversizedHeader);
      assertThat(UUID.fromString(corrHeader)).isNotNull();
    }

    @Test
    @DisplayName("2.4 Blank or whitespace-only header is rejected and replaced with safe UUID")
    void testRejectBlankHeader() throws Exception {
      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", "    ")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      assertThat(corrHeader).isNotBlank();
      assertThat(UUID.fromString(corrHeader)).isNotNull();
    }

    @Test
    @DisplayName("2.5 Whitespace-padded header (leading or trailing whitespace) is strictly rejected and replaced with safe UUID")
    void testRejectWhitespacePaddedHeader() throws Exception {
      String paddedHeader = "  padded-correlation-id  ";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", paddedHeader)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String corrHeader = result.getResponse().getHeader("X-Correlation-Id");
      assertThat(corrHeader).isNotBlank();
      assertThat(corrHeader).isNotEqualTo(paddedHeader);
      assertThat(corrHeader).isNotEqualTo(paddedHeader.trim());
      assertThat(UUID.fromString(corrHeader)).isNotNull();
    }

    @Test
    @DisplayName("2.6 Direct isValidCorrelationId unit test: Strict validation without normalization")
    void testIsValidCorrelationId_strictValidation() {
      // Valid identifiers
      assertThat(CorrelationIdFilter.isValidCorrelationId("valid-id-123")).isTrue();
      assertThat(CorrelationIdFilter.isValidCorrelationId("TRACE_ID_456")).isTrue();
      assertThat(CorrelationIdFilter.isValidCorrelationId("a".repeat(64))).isTrue();

      // Rejected: Any whitespace (leading, trailing, internal, tab/newline)
      assertThat(CorrelationIdFilter.isValidCorrelationId(" leading-space")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("trailing-space ")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("  padded  ")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("internal space")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("\tleading-tab")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("newline\n")).isFalse();

      // Rejected: Invalid format or boundaries
      assertThat(CorrelationIdFilter.isValidCorrelationId("")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("   ")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId(null)).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("a".repeat(65))).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("<script>")).isFalse();
      assertThat(CorrelationIdFilter.isValidCorrelationId("id;drop table")).isFalse();
    }
  }

  @Nested
  @DisplayName("3. HTTP Status Coverage & Header Propagation Tests")
  class HttpStatusCoverageTests {

    @Test
    @DisplayName("3.1 401 Unauthorized via RestAuthenticationEntryPoint includes correlation tracking")
    void test401UnauthorizedIncludesCorrelationTracking() throws Exception {
      String correlationId = "auth-challenge-401";

      MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isUnauthorized())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.2 404 Not Found includes correlation headers and ErrorResponse correlationId")
    void test404NotFoundIncludesCorrelationTracking() throws Exception {
      String correlationId = "not-found-404-trace";
      String token = "Bearer " + jwtTokenProvider.generateAccessToken(1L, "admin", List.of("ROLE_ADMIN"));

      MvcResult result = mockMvc.perform(get("/api/v1/non-existent-endpoint-test")
              .header("Authorization", token)
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isNotFound())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.3 400 Bad Request via GlobalExceptionHandler includes correlation tracking")
    void test400BadRequestIncludesCorrelationTracking() throws Exception {
      String correlationId = "bad-request-400-trace";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .header("X-Correlation-Id", correlationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{ invalid json }"))
          .andExpect(status().isBadRequest())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.4 Public 200 OK endpoint includes correlation response headers")
    void test200OkIncludesCorrelationResponseHeaders() throws Exception {
      String correlationId = "health-check-corr-id";

      mockMvc.perform(get("/actuator/health")
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isOk())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId));
    }

    @Test
    @DisplayName("3.5 403 Forbidden via RestAccessDeniedHandler includes correlation response headers and body correlationId")
    void test403ForbiddenIncludesCorrelationTracking() throws Exception {
      String correlationId = "forbidden-403-trace-id";
      // Generate token without trainee.write authority (only trainee.read)
      String token = "Bearer " + jwtTokenProvider.generateAccessToken(100L, "trainee-reader", List.of("trainee.read"));

      MvcResult result = mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", token)
              .header("X-Correlation-Id", correlationId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.6 500 Internal Server Error via GlobalExceptionHandler includes correlation response headers and body correlationId")
    void test500InternalServerErrorIncludesCorrelationTracking() throws Exception {
      String correlationId = "internal-error-500-trace-id";
      String token = "Bearer " + jwtTokenProvider.generateAccessToken(101L, "admin-user", List.of("ROLE_ADMIN"));

      MvcResult result = mockMvc.perform(get("/api/test/correlation/internal-error")
              .header("Authorization", token)
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isInternalServerError())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.7 201 Created endpoint includes correlation response headers and ApiResponse body correlationId")
    void test201CreatedIncludesCorrelationTracking() throws Exception {
      String correlationId = "created-201-trace-id";
      String token = "Bearer " + jwtTokenProvider.generateAccessToken(102L, "admin-user", List.of("ROLE_ADMIN"));

      MvcResult result = mockMvc.perform(post("/api/test/correlation/created")
              .header("Authorization", token)
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isCreated())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data").value("resource-123"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.8 405 Method Not Allowed via GlobalExceptionHandler verifies HttpHeaders overload carries correlation headers")
    void test405MethodNotAllowedWithCustomHeadersIncludesCorrelationTracking() throws Exception {
      String correlationId = "method-not-allowed-405-trace";
      String token = "Bearer " + jwtTokenProvider.generateAccessToken(103L, "admin-user", List.of("ROLE_ADMIN"));

      MvcResult result = mockMvc.perform(patch("/api/v1/auth/login")
              .header("Authorization", token)
              .header("X-Correlation-Id", correlationId))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId))
          .andExpect(header().exists("Allow"))
          .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
          .andExpect(jsonPath("$.correlationId").value(correlationId))
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      JsonNode root = objectMapper.readTree(responseBody);
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("3.9 Direct GlobalExceptionHandler invocation verifies ResponseEntity.getHeaders() carries correlation headers for both overloads")
    void testDirectGlobalExceptionHandler_headersAlwaysSetOnResponseEntity() {
      // Case A: Overload buildSanitizedResponse(response, status) without initial headers
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test-direct");
      CorrelationContextHolder.setCorrelationId("direct-test-cid-1");
      ResponseEntity<ErrorResponse> entity1 = globalExceptionHandler.handleGenericException(
          new RuntimeException("Simulated direct crash"), request);

      assertThat(entity1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(entity1.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("direct-test-cid-1");
      assertThat(entity1.getHeaders().getFirst("X-Request-Id")).isEqualTo("direct-test-cid-1");
      assertThat(entity1.getBody()).isNotNull();
      assertThat(entity1.getBody().getCorrelationId()).isEqualTo("direct-test-cid-1");

      // Case B: Overload buildSanitizedResponse(response, headers, status) with custom HttpHeaders (Allow)
      CorrelationContextHolder.setCorrelationId("direct-test-cid-2");
      HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "POST"));
      ResponseEntity<ErrorResponse> entity2 = globalExceptionHandler.handleMethodNotSupported(ex, request);

      assertThat(entity2.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(entity2.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("direct-test-cid-2");
      assertThat(entity2.getHeaders().getFirst("X-Request-Id")).isEqualTo("direct-test-cid-2");
      assertThat(entity2.getHeaders().getAllow()).isNotEmpty();
      assertThat(entity2.getBody()).isNotNull();
      assertThat(entity2.getBody().getCorrelationId()).isEqualTo("direct-test-cid-2");

      // Case C: Overload buildSanitizedResponse(response, headers, status) with custom HttpHeaders (Accept)
      CorrelationContextHolder.setCorrelationId("direct-test-cid-3");
      HttpMediaTypeNotSupportedException mediaEx = new HttpMediaTypeNotSupportedException(
          MediaType.APPLICATION_XML, List.of(MediaType.APPLICATION_JSON));
      ResponseEntity<ErrorResponse> entity3 = globalExceptionHandler.handleHttpMediaTypeNotSupported(mediaEx, request);

      assertThat(entity3.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(entity3.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("direct-test-cid-3");
      assertThat(entity3.getHeaders().getFirst("X-Request-Id")).isEqualTo("direct-test-cid-3");
      assertThat(entity3.getHeaders().getAccept()).isNotEmpty();
      assertThat(entity3.getBody()).isNotNull();
      assertThat(entity3.getBody().getCorrelationId()).isEqualTo("direct-test-cid-3");
    }
  }

  @Nested
  @DisplayName("4. Lifecycle Cleanup, Context Restoration & Thread Isolation Tests")
  class LifecycleCleanupTests {

    @Test
    @DisplayName("4.1 CorrelationContextHolder and MDC are populated during filter and purged when no prior context existed")
    void testThreadLocalAndMdcLifecycle_whenNoPriorContext() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
      request.addHeader("X-Correlation-Id", "lifecycle-test-id");
      MockHttpServletResponse response = new MockHttpServletResponse();

      final String[] observedContextId = new String[1];
      final String[] observedMdcCorrId = new String[1];
      final String[] observedMdcReqId = new String[1];

      FilterChain filterChain = (req, res) -> {
        observedContextId[0] = CorrelationContextHolder.getCorrelationId();
        observedMdcCorrId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        observedMdcReqId[0] = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      };

      correlationIdFilter.doFilter(request, response, filterChain);

      assertThat(observedContextId[0]).isEqualTo("lifecycle-test-id");
      assertThat(observedMdcCorrId[0]).isEqualTo("lifecycle-test-id");
      assertThat(observedMdcReqId[0]).isEqualTo("lifecycle-test-id");

      // Verify fail-safe purge
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();

      assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("lifecycle-test-id");
      assertThat(response.getHeader("X-Request-Id")).isEqualTo("lifecycle-test-id");
    }

    @Test
    @DisplayName("4.2 Case A — Consistent upstream context: Preserved and restored across request execution")
    void testCaseA_consistentUpstreamContext_isPreservedAndRestored() throws Exception {
      String upstreamConsistentId = "upstream-consistent-111";
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, upstreamConsistentId);
      MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, upstreamConsistentId);
      CorrelationContextHolder.setCorrelationId(upstreamConsistentId);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
      request.addHeader("X-Correlation-Id", "downstream-incoming-222");
      MockHttpServletResponse response = new MockHttpServletResponse();

      final String[] observedContextId = new String[1];
      final String[] observedMdcCorrId = new String[1];
      final String[] observedMdcReqId = new String[1];

      FilterChain filterChain = (req, res) -> {
        observedContextId[0] = CorrelationContextHolder.getCorrelationId();
        observedMdcCorrId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        observedMdcReqId[0] = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      };

      correlationIdFilter.doFilter(request, response, filterChain);

      // During request execution, all context stores match the request's ID
      assertThat(observedContextId[0]).isEqualTo("downstream-incoming-222");
      assertThat(observedMdcCorrId[0]).isEqualTo("downstream-incoming-222");
      assertThat(observedMdcReqId[0]).isEqualTo("downstream-incoming-222");

      // Post-completion: upstream context is restored across all stores
      assertThat(CorrelationContextHolder.getCorrelationId()).isEqualTo(upstreamConsistentId);
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isEqualTo(upstreamConsistentId);
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isEqualTo(upstreamConsistentId);

      // Clean up for subsequent tests
      CorrelationContextHolder.clear();
      MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
      MDC.remove(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
    }

    @Test
    @DisplayName("4.3 Case B — Inconsistent upstream context: Stores are synchronized during request and independently restored afterward")
    void testCaseB_inconsistentUpstreamContext_preservesEachStoreIndependently() throws Exception {
      String upstreamContextHolderId = "upstream-holder-aaa";
      String upstreamMdcCorrId = "upstream-mdc-corr-bbb";
      String upstreamMdcReqId = "upstream-mdc-req-ccc";

      CorrelationContextHolder.setCorrelationId(upstreamContextHolderId);
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, upstreamMdcCorrId);
      MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, upstreamMdcReqId);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
      request.addHeader("X-Correlation-Id", "downstream-incoming-333");
      MockHttpServletResponse response = new MockHttpServletResponse();

      final String[] observedContextId = new String[1];
      final String[] observedMdcCorrId = new String[1];
      final String[] observedMdcReqId = new String[1];

      FilterChain filterChain = (req, res) -> {
        observedContextId[0] = CorrelationContextHolder.getCorrelationId();
        observedMdcCorrId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        observedMdcReqId[0] = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      };

      correlationIdFilter.doFilter(request, response, filterChain);

      // During request execution: all stores are synchronized to the request's ID
      assertThat(observedContextId[0]).isEqualTo("downstream-incoming-333");
      assertThat(observedMdcCorrId[0]).isEqualTo("downstream-incoming-333");
      assertThat(observedMdcReqId[0]).isEqualTo("downstream-incoming-333");

      // Post-completion: each context store is independently restored to its exact upstream value
      assertThat(CorrelationContextHolder.getCorrelationId()).isEqualTo(upstreamContextHolderId);
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isEqualTo(upstreamMdcCorrId);
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isEqualTo(upstreamMdcReqId);

      // Clean up for subsequent tests
      CorrelationContextHolder.clear();
      MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
      MDC.remove(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
    }

    @Test
    @DisplayName("4.4 Lifecycle cleanup and restoration on exception: no leakage into subsequent operations")
    void testCleanupAndRestorationOnException() {
      // Scenario 1: With upstream context, throwing downstream restores upstream context
      String upstreamCorr = "upstream-corr-fault";
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, upstreamCorr);
      CorrelationContextHolder.setCorrelationId(upstreamCorr);

      MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/test");
      MockHttpServletResponse response1 = new MockHttpServletResponse();

      FilterChain throwingChain = (req, res) -> {
        assertThat(CorrelationContextHolder.getCorrelationId()).isNotEqualTo(upstreamCorr);
        throw new RuntimeException("Simulated filter failure");
      };

      assertThatThrownBy(() -> correlationIdFilter.doFilter(request1, response1, throwingChain))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Simulated filter failure");

      assertThat(CorrelationContextHolder.getCorrelationId()).isEqualTo(upstreamCorr);
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isEqualTo(upstreamCorr);

      // Clear upstream context for Scenario 2
      CorrelationContextHolder.clear();
      MDC.remove(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

      // Scenario 2: Without upstream context, throwing downstream purges completely
      MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/v1/test");
      MockHttpServletResponse response2 = new MockHttpServletResponse();

      assertThatThrownBy(() -> correlationIdFilter.doFilter(request2, response2, throwingChain))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Simulated filter failure");

      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("4.5 Sequential requests on the same thread receive independent IDs and do not leak state")
    void testSequentialRequestsOnSameThread_receiveIndependentIdsAndDoNotLeak() throws Exception {
      long callingThreadId = Thread.currentThread().getId();

      // Request 1 on current thread
      MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/v1/resource/1");
      MockHttpServletResponse res1 = new MockHttpServletResponse();
      final String[] id1 = new String[1];
      final long[] thread1 = new long[1];

      correlationIdFilter.doFilter(req1, res1, (req, res) -> {
        thread1[0] = Thread.currentThread().getId();
        id1[0] = CorrelationContextHolder.getCorrelationId();
      });

      assertThat(thread1[0]).isEqualTo(callingThreadId);
      assertThat(id1[0]).isNotBlank();
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();

      // Request 2 on current thread
      MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/resource/2");
      MockHttpServletResponse res2 = new MockHttpServletResponse();
      final String[] id2 = new String[1];
      final long[] thread2 = new long[1];

      correlationIdFilter.doFilter(req2, res2, (req, res) -> {
        thread2[0] = Thread.currentThread().getId();
        id2[0] = CorrelationContextHolder.getCorrelationId();
      });

      assertThat(thread2[0]).isEqualTo(callingThreadId);
      assertThat(id2[0]).isNotBlank();
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();

      // Verify independence
      assertThat(id1[0]).isNotEqualTo(id2[0]);
    }

    @Test
    @DisplayName("4.6 Servlet dispatch configuration: async dispatch is skipped while error dispatch is enabled")
    void testDispatchConfiguration() {
      assertThat(correlationIdFilter.shouldNotFilterAsyncDispatch())
          .as("Async dispatch should be skipped as application uses synchronous Spring MVC REST controllers")
          .isTrue();
      assertThat(correlationIdFilter.shouldNotFilterErrorDispatch())
          .as("Error dispatch should be filtered to re-establish correlation context on error dispatch")
          .isFalse();
    }

    @Test
    @DisplayName("4.7 Full servlet lifecycle: Correlation ID preserved across REQUEST and ERROR dispatches with guaranteed cleanup")
    void testErrorDispatch_preservesCorrelationIdAcrossLifecycleAndCleansUp() throws Exception {
      // 1. Phase 1: Inbound client request (DispatcherType.REQUEST)
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/faulty-resource");
      request.setDispatcherType(jakarta.servlet.DispatcherType.REQUEST);
      request.addHeader("X-Correlation-Id", "lifecycle-err-cid-777");
      MockHttpServletResponse response = new MockHttpServletResponse();

      final String[] phase1ObservedContextId = new String[1];
      final String[] phase1ObservedMdcId = new String[1];

      // Downstream in Phase 1 encounters an unhandled fault escaping Spring MVC
      FilterChain requestChain = (req, res) -> {
        phase1ObservedContextId[0] = CorrelationContextHolder.getCorrelationId();
        phase1ObservedMdcId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        throw new ServletException("Simulated container-level unhandled exception");
      };

      assertThatThrownBy(() -> correlationIdFilter.doFilter(request, response, requestChain))
          .isInstanceOf(ServletException.class)
          .hasMessage("Simulated container-level unhandled exception");

      // Verify Phase 1 established the correlation ID during execution
      assertThat(phase1ObservedContextId[0]).isEqualTo("lifecycle-err-cid-777");
      assertThat(phase1ObservedMdcId[0]).isEqualTo("lifecycle-err-cid-777");

      // Verify ThreadLocal and MDC were cleaned up when Phase 1 completed
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();

      // Verify the request attribute was preserved on the HttpServletRequest
      assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE))
          .isEqualTo("lifecycle-err-cid-777");

      // 2. Phase 2: Servlet Container initiates ERROR dispatch to /error on the same request
      request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
      request.setRequestURI("/error");
      request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 500);
      request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_MESSAGE, "Internal Server Error");

      final String[] phase2ObservedContextId = new String[1];
      final String[] phase2ObservedMdcCorrId = new String[1];
      final String[] phase2ObservedMdcReqId = new String[1];

      FilterChain errorChain = (req, res) -> {
        // During ERROR dispatch, verify context was re-established from the initial request attribute
        phase2ObservedContextId[0] = CorrelationContextHolder.getCorrelationId();
        phase2ObservedMdcCorrId[0] = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        phase2ObservedMdcReqId[0] = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY);
      };

      // Execute Phase 2: ERROR dispatch (CorrelationIdFilter runs because shouldNotFilterErrorDispatch == false)
      correlationIdFilter.doFilter(request, response, errorChain);

      // Assert the original correlation ID was preserved and active during ERROR dispatch
      assertThat(phase2ObservedContextId[0]).isEqualTo("lifecycle-err-cid-777");
      assertThat(phase2ObservedMdcCorrId[0]).isEqualTo("lifecycle-err-cid-777");
      assertThat(phase2ObservedMdcReqId[0]).isEqualTo("lifecycle-err-cid-777");

      // Assert egress headers on response
      assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("lifecycle-err-cid-777");
      assertThat(response.getHeader("X-Request-Id")).isEqualTo("lifecycle-err-cid-777");

      // 3. Post-completion cleanup after ERROR dispatch
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("4.8 Full servlet lifecycle with auto-generated UUID: Preserved across ERROR dispatch and cleaned up")
    void testErrorDispatch_preservesAutoGeneratedUuidAcrossLifecycle() throws Exception {
      // Phase 1: REQUEST dispatch with no headers -> auto-generates UUID
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/faulty-resource");
      request.setDispatcherType(jakarta.servlet.DispatcherType.REQUEST);
      MockHttpServletResponse response = new MockHttpServletResponse();

      final String[] generatedId = new String[1];

      FilterChain requestChain = (req, res) -> {
        generatedId[0] = CorrelationContextHolder.getCorrelationId();
        throw new ServletException("Simulated container unhandled fault");
      };

      assertThatThrownBy(() -> correlationIdFilter.doFilter(request, response, requestChain))
          .isInstanceOf(ServletException.class);

      assertThat(generatedId[0]).isNotBlank();
      // Ensure post-phase 1 cleanup
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();

      // Phase 2: Container ERROR dispatch
      request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
      request.setRequestURI("/error");

      final String[] errorPhaseId = new String[1];
      correlationIdFilter.doFilter(request, response, (req, res) -> {
        errorPhaseId[0] = CorrelationContextHolder.getCorrelationId();
      });

      // Verify the auto-generated UUID was preserved across error dispatch
      assertThat(errorPhaseId[0]).isEqualTo(generatedId[0]);

      // Ensure post-phase 2 cleanup
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("4.9 MockMvc container ERROR dispatch to /error preserves correlation tracking headers and cleans up")
    void testMockMvcErrorDispatch_preservesCorrelationHeadersAndCleansUp() throws Exception {
      String correlationId = "mockmvc-error-cid-555";

      mockMvc.perform(get("/error")
              .header("X-Correlation-Id", correlationId)
              .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 500)
              .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/test"))
          .andExpect(status().is5xxServerError())
          .andExpect(header().string("X-Correlation-Id", correlationId))
          .andExpect(header().string("X-Request-Id", correlationId));

      // Post-request context cleanup
      assertThat(CorrelationContextHolder.getCorrelationId()).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
      assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
    }
  }

  @Nested
  @DisplayName("5. Envelope Unit Contracts & CORS Integration")
  class EnvelopeAndCorsContractsTests {

    @Test
    @DisplayName("5.1 ApiResponse includes correlationId when context holder is populated")
    void testApiResponse_includesCorrelationIdFromContext() throws Exception {
      CorrelationContextHolder.setCorrelationId("envelope-cid-123");

      ApiResponse<String> response = ApiResponse.ok("test-data");
      String json = objectMapper.writeValueAsString(response);

      assertThat(json).contains("\"correlationId\":\"envelope-cid-123\"");
      assertThat(response.getCorrelationId()).isEqualTo("envelope-cid-123");
    }

    @Test
    @DisplayName("5.2 ApiResponse omits correlationId when context holder is null")
    void testApiResponse_omitsCorrelationIdWhenNull() throws Exception {
      CorrelationContextHolder.clear();

      ApiResponse<String> response = ApiResponse.ok("test-data");
      String json = objectMapper.writeValueAsString(response);

      assertThat(json).doesNotContain("\"correlationId\"");
      assertThat(response.getCorrelationId()).isNull();
    }

    @Test
    @DisplayName("5.3 ErrorResponse includes correlationId when context holder is populated")
    void testErrorResponse_includesCorrelationIdFromContext() throws Exception {
      CorrelationContextHolder.setCorrelationId("error-cid-456");

      ErrorResponse errorResponse = new ErrorResponse(400, "Bad Request", "BAD_REQUEST", "Error message", "/api/test");
      String json = objectMapper.writeValueAsString(errorResponse);

      assertThat(json).contains("\"correlationId\":\"error-cid-456\"");
      assertThat(errorResponse.getCorrelationId()).isEqualTo("error-cid-456");
    }

    @Test
    @DisplayName("5.4 ErrorResponse omits correlationId when context holder is null")
    void testErrorResponse_omitsCorrelationIdWhenNull() throws Exception {
      CorrelationContextHolder.clear();

      ErrorResponse errorResponse = new ErrorResponse(400, "Bad Request", "BAD_REQUEST", "Error message", "/api/test");
      String json = objectMapper.writeValueAsString(errorResponse);

      assertThat(json).doesNotContain("\"correlationId\"");
      assertThat(errorResponse.getCorrelationId()).isNull();
    }

    @Test
    @DisplayName("5.5 SecurityCorsProperties exposes and allows correlation headers")
    void testCorsProperties_containsCorrelationHeaders() {
      assertThat(securityCorsProperties.getAllowedHeaders())
          .contains("X-Correlation-Id", "X-Request-Id");
      assertThat(securityCorsProperties.getExposedHeaders())
          .contains("X-Correlation-Id", "X-Request-Id");
    }
  }

  @RestController
  @RequestMapping("/api/test/correlation")
  static class CorrelationTestEndpointController {

    @PostMapping("/created")
    public ResponseEntity<ApiResponse<String>> testCreatedEndpoint(HttpServletRequest request) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Created successfully", "resource-123", request.getRequestURI()));
    }

    @GetMapping("/internal-error")
    public ResponseEntity<Void> testInternalError() {
      throw new RuntimeException("Simulated internal server fault");
    }
  }
}
