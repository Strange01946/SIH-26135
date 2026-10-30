package in.gov.sih.sih26135;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.external.vendor.controller.UnverifiedThirdPartyVendorController;
import in.gov.sih.sih26135.controller.SpoofedWhitelistedPackageController;
import in.gov.sih.sih26135.controller.VerifiedTestController;
import in.gov.sih.sih26135.controller.unverified.UnverifiedBypassController;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * End-to-end API-level integration tests verifying actual HTTP status codes,
 * response bodies, security sanitization, and controller classification boundaries
 * through the Spring MVC request-handling pipeline and GlobalExceptionHandler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API-Level Exception Handler & Controller Classification Integration Tests")
class ApiLevelExceptionHandlerTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GlobalExceptionHandler globalExceptionHandler;

  @Autowired
  private in.gov.sih.sih26135.security.jwt.JwtTokenProvider jwtTokenProvider;

  private String createValidAdminBearerToken() {
    return "Bearer " + jwtTokenProvider.generateAccessToken(1L, "admin", java.util.List.of("ROLE_ADMIN"));
  }

  @BeforeEach
  void verifyDatabaseIsolation() throws Exception {
    assertThat(dataSource)
        .as("DataSource bean must be present in application context")
        .isNotNull();

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      // Fail-closed guard: strictly rejects SIH26135, null, blank, or any non-SIH26135_TEST catalog
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
  }

  // =========================================================================
  // 1. Live Application Endpoints through Full Integrated Spring Boot Pipeline
  // =========================================================================

  @Test
  @DisplayName("POST /api/v1/auth/login with empty body maps to HTTP 400 BAD_REQUEST")
  void testLoginWithEmptyBody_returnsHttp400BadRequest() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Username or email is required"))
        .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
  }

  @Test
  @DisplayName("Verified application controller DTO validation failure maps to HTTP 400 VALIDATION_ERROR with field details")
  void testVerifiedControllerDtoValidation_mapsToHttp400ValidationError() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new VerifiedTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(post("/api/test/verified/dto-check")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.details[0]").value("code: code must not be blank"));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login with malformed JSON maps to HTTP 400 MALFORMED_JSON")
  void testLoginWithMalformedJson_returnsHttp400MalformedJson() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{invalid-json-content"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
        .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"))
        .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
  }

  @Test
  @DisplayName("PATCH /api/v1/auth/login with authenticated bearer token maps to HTTP 405 METHOD_NOT_ALLOWED")
  void testLoginWithUnsupportedMethod_returnsHttp405MethodNotAllowed() throws Exception {
    String authHeader = createValidAdminBearerToken();
    mockMvc.perform(patch("/api/v1/auth/login")
            .header("Authorization", authHeader))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.error").value("Method Not Allowed"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
        .andExpect(jsonPath("$.message").value("HTTP method 'PATCH' is not supported for this endpoint"))
        .andExpect(jsonPath("$.path").value("/sanitized-path"));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login with unsupported media type maps to HTTP 415 UNSUPPORTED_MEDIA_TYPE")
  void testLoginWithUnsupportedMediaType_returnsHttp415() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_XML)
            .content("<login><username>admin</username></login>"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.message").value(containsString("Content type 'application/xml")))
        .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
  }

  @Test
  @DisplayName("GET /api/v1/nonexistent-endpoint-404 with authenticated bearer token maps to HTTP 404 ENDPOINT_NOT_FOUND")
  void testNonExistentEndpoint_returnsHttp404NotFound() throws Exception {
    String authHeader = createValidAdminBearerToken();
    mockMvc.perform(get("/api/v1/nonexistent-endpoint-404")
            .header("Authorization", authHeader))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
        .andExpect(jsonPath("$.path").value("/sanitized-path"));
  }

  @Test
  @DisplayName("GET /api/v1/trainees unauthenticated maps to HTTP 401 UNAUTHORIZED at security boundary")
  void testUnauthenticatedAccess_returnsHttp401Unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/trainees"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
        .andExpect(jsonPath("$.path").value("/api/v1/trainees"));
  }

  // =========================================================================
  // 2. Controller Classification Boundary through Request Pipeline
  // =========================================================================

  @Test
  @DisplayName("Verified application controller parameter violation maps to HTTP 400 VALIDATION_ERROR")
  void testVerifiedController_mapsToHttp400() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new VerifiedTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/verified/param-check/invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.details[0]").value("id: must be a positive number"));
  }

  @Test
  @DisplayName("Unverified third-party controller parameter violation fails closed to HTTP 500 without leaking secrets")
  void testUnverifiedThirdPartyController_failsClosedToHttp500() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new UnverifiedThirdPartyVendorController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/thirdparty/vendor-action/invalid"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
        .andExpect(jsonPath("$.details").doesNotExist())
        // Strictly verify zero information disclosure of third-party vendor secrets or class names
        .andExpect(jsonPath("$.message", not(containsString("secret-vendor-token"))))
        .andExpect(jsonPath("$.message", not(containsString("UnverifiedThirdPartyVendorController"))));
  }

  @Test
  @DisplayName("Unverified controller attempting subpackage prefix bypass fails closed to HTTP 500")
  void testUnverifiedBypassController_failsClosedToHttp500() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new UnverifiedBypassController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/bypass/attempt/invalid"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
        .andExpect(jsonPath("$.message", not(containsString("UnverifiedBypassController"))));
  }

  @Test
  @DisplayName("Controller spoofing exact whitelisted package fails closed to HTTP 500 without leaking secrets")
  void testSpoofedWhitelistedController_failsClosedToHttp500() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new SpoofedWhitelistedPackageController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/spoofed/param-check/invalid"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
        .andExpect(jsonPath("$.details").doesNotExist())
        // Strictly verify zero disclosure of spoofed controller class name or sensitive secret token
        .andExpect(jsonPath("$.message", not(containsString("SpoofedWhitelistedPackageController"))))
        .andExpect(jsonPath("$.message", not(containsString(SpoofedWhitelistedPackageController.SENSITIVE_TOKEN))));
  }

  // =========================================================================
  // 3. Server Configuration & Unhandled Exceptions Sanitization
  // =========================================================================

  @RestController
  @RequestMapping("/api/test/mismatch")
  static class PathVariableMismatchTestController {
    // Missing path variable in mapping pattern triggers MissingPathVariableException
    @GetMapping("/route-with-mismatch")
    public String brokenRoute(@PathVariable("internalSecretVar") String var) {
      return var;
    }
  }

  @Test
  @DisplayName("MissingPathVariableException maps to HTTP 500 without disclosing internal variable name")
  void testMissingPathVariable_mapsToHttp500Sanitized() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new PathVariableMismatchTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/mismatch/route-with-mismatch"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
        .andExpect(jsonPath("$.message", not(containsString("internalSecretVar"))));
  }

  @RestController
  @RequestMapping("/api/test/fault")
  static class DatabaseFaultTestController {
    @GetMapping("/sql-error")
    public String triggerDatabaseFault() {
      throw new RuntimeException("SQL Execution Error: SELECT * FROM `users` WHERE password_hash = 'secret_hash'; Connection URL: jdbc:mysql://root:super_secret_pw@db.internal:3306/SIH26135");
    }
  }

  @Test
  @DisplayName("Unhandled internal database runtime exception maps to sanitized HTTP 500 with zero credential disclosure")
  void testUnhandledDatabaseFault_mapsToSanitizedHttp500() throws Exception {
    MockMvc pipelineMockMvc = MockMvcBuilders.standaloneSetup(new DatabaseFaultTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    pipelineMockMvc.perform(get("/api/test/fault/sql-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
        .andExpect(jsonPath("$.details").doesNotExist())
        // Verify zero disclosure of SQL query, table names, password, or connection URL
        .andExpect(jsonPath("$.message", not(containsString("SELECT"))))
        .andExpect(jsonPath("$.message", not(containsString("password_hash"))))
        .andExpect(jsonPath("$.message", not(containsString("super_secret_pw"))))
        .andExpect(jsonPath("$.message", not(containsString("jdbc:mysql"))));
  }
}
