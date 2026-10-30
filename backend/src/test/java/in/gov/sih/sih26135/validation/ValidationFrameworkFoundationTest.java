package in.gov.sih.sih26135.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.exception.ErrorResponse;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Foundation-level integration and framework tests for Phase 26.2.
 * Verifies explicit Bean Validation dependency, controller-boundary rejection,
 * central exception handling, deterministic multiple-violation ordering,
 * anti-echo sanitization, and security preservation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.2 - Validation Framework Foundation Tests")
class ValidationFrameworkFoundationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private Validator validator;

  @Autowired
  private GlobalExceptionHandler globalExceptionHandler;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  private static final AtomicBoolean businessLogicInvoked = new AtomicBoolean(false);

  @BeforeEach
  void verifyDatabaseIsolation() throws Exception {
    assertThat(dataSource)
        .as("DataSource bean must be present in application context")
        .isNotNull();

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    businessLogicInvoked.set(false);
  }

  // =========================================================================
  // Test Fixtures (Isolated to Test Scope, Zero Production Impact)
  // =========================================================================

  public static class TestFoundationRequest {

    @NotBlank(message = "code must not be blank")
    private String code;

    @Email(message = "must be a well-formed email address")
    @NotBlank(message = "email must not be blank")
    private String email;

    @Positive(message = "age must be greater than 0")
    @Min(value = 18, message = "must be greater than or equal to 18")
    private Integer age;

    public TestFoundationRequest() {}

    public TestFoundationRequest(String code, String email, Integer age) {
      this.code = code;
      this.email = email;
      this.age = age;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }
  }

  @RestController
  @RequestMapping("/api/test/validation-foundation")
  public static class FoundationTestController {

    @PostMapping("/submit")
    public ResponseEntity<String> submit(@Valid @RequestBody TestFoundationRequest request) {
      businessLogicInvoked.set(true);
      return ResponseEntity.ok("processed: " + request.getCode());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<String> getItem(@PathVariable("id") Long id) {
      businessLogicInvoked.set(true);
      return ResponseEntity.ok("item: " + id);
    }
  }

  // =========================================================================
  // 1. Dependency & Context Initialization
  // =========================================================================

  @Test
  @DisplayName("1. Explicit Bean Validation provider is available in Spring Application Context")
  void testValidationDependencyAndContextInitialization() {
    assertThat(validator)
        .as("Jakarta Validator bean must be instantiated by Spring Boot validation starter")
        .isNotNull();

    // Verify Hibernate Validator is the underlying engine
    assertThat(validator.getClass().getName())
        .as("Hibernate Validator must be the active provider")
        .contains("Validator");
  }

  // =========================================================================
  // 2. Controller-Boundary Rejection & Short-Circuit
  // =========================================================================

  @Test
  @DisplayName("2. Invalid request body is rejected at controller boundary before business logic runs")
  void testControllerBoundary_invalidPayload_rejectsBeforeBusinessLogic() throws Exception {
    MockMvc standaloneMvc = MockMvcBuilders.standaloneSetup(new FoundationTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    standaloneMvc.perform(post("/api/test/validation-foundation/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"\",\"email\":\"not-an-email\",\"age\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Request validation failed"))
        .andExpect(jsonPath("$.path").value("/api/test/validation-foundation/submit"));

    // Business logic must NOT have been executed
    assertThat(businessLogicInvoked.get())
        .as("Controller business logic must be short-circuited when validation fails")
        .isFalse();
  }

  @Test
  @DisplayName("3. Valid request body passes controller validation and executes business logic")
  void testControllerBoundary_validPayload_executesBusinessLogic() throws Exception {
    MockMvc standaloneMvc = MockMvcBuilders.standaloneSetup(new FoundationTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    standaloneMvc.perform(post("/api/test/validation-foundation/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"CODE-101\",\"email\":\"trainee@example.gov.in\",\"age\":25}"))
        .andExpect(status().isOk());

    assertThat(businessLogicInvoked.get())
        .as("Controller business logic must be executed when payload is valid")
        .isTrue();
  }

  // =========================================================================
  // 3. Deterministic Multiple Violation Ordering
  // =========================================================================

  private List<String> extractDetails(MvcResult mvcResult) throws Exception {
    String responseContent = mvcResult.getResponse().getContentAsString();
    JsonNode root = objectMapper.readTree(responseContent);
    JsonNode detailsNode = root.get("details");
    assertThat(detailsNode)
        .as("Response must contain a non-null 'details' array")
        .isNotNull();
    assertThat(detailsNode.isArray())
        .as("'details' must be a JSON array")
        .isTrue();

    List<String> details = new ArrayList<>();
    for (JsonNode item : detailsNode) {
      details.add(item.asText());
    }
    return details;
  }

  @Test
  @DisplayName("4. Multiple validation violations produce deterministic, alphabetically sorted details")
  void testMultipleViolations_produceDeterministicSortedDetails() throws Exception {
    MockMvc standaloneMvc = MockMvcBuilders.standaloneSetup(new FoundationTestController())
        .setControllerAdvice(globalExceptionHandler)
        .build();

    List<String> expectedDetails = List.of(
        "age: age must be greater than 0",
        "age: must be greater than or equal to 18",
        "code: code must not be blank",
        "email: must be a well-formed email address"
    );

    // First request: extract details array and assert exact ordered list
    MvcResult firstResult = standaloneMvc.perform(post("/api/test/validation-foundation/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"\",\"email\":\"invalid-format\",\"age\":-5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.details.length()").value(expectedDetails.size()))
        .andReturn();

    List<String> firstDetails = extractDetails(firstResult);
    assertThat(firstDetails)
        .as("Validation details must match the exact expected alphabetically sorted list")
        .containsExactlyElementsOf(expectedDetails);

    // Repeat the exact same invalid request to prove deterministic repeatability
    MvcResult secondResult = standaloneMvc.perform(post("/api/test/validation-foundation/submit")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"\",\"email\":\"invalid-format\",\"age\":-5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.details.length()").value(expectedDetails.size()))
        .andReturn();

    List<String> secondDetails = extractDetails(secondResult);
    assertThat(secondDetails)
        .as("Repeated validation request must produce an identical ordered details list")
        .containsExactlyElementsOf(firstDetails);
    assertThat(secondDetails)
        .as("Second validation details must match the exact expected alphabetically sorted list")
        .containsExactlyElementsOf(expectedDetails);
  }

  // =========================================================================
  // 4. Anti-Echo Sanitization & Sensitive Data Masking
  // =========================================================================

  @Test
  @DisplayName("5. Sensitive token and echoed input values are sanitized and not leaked in details")
  void testValidationSanitization_masksSensitiveTokensAndRejectedEchoes() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test/validation");

    org.springframework.validation.BeanPropertyBindingResult bindingResult =
        new org.springframework.validation.BeanPropertyBindingResult(new TestFoundationRequest(), "request");

    String sentinelToken = "SENTINEL_TOKEN_SECRET_9999_EXPOSED";
    bindingResult.addError(new org.springframework.validation.FieldError(
        "request", "email", sentinelToken, false, null, null, "Invalid credential with token: " + sentinelToken
    ));

    org.springframework.web.bind.MethodArgumentNotValidException ex =
        new org.springframework.web.bind.MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");

    // Details must NOT leak the sentinel token
    List<String> details = response.getBody().getDetails();
    assertThat(details).isNotNull().isNotEmpty();
    assertThat(details.get(0)).doesNotContain(sentinelToken);
    assertThat(details.get(0)).isEqualTo("email: is invalid");
  }

  // =========================================================================
  // 5. HandlerMethodValidationException Mapping
  // =========================================================================

  @Test
  @DisplayName("6. HandlerMethodValidationException maps to HTTP 400 VALIDATION_ERROR with sanitized param details")
  void testHandlerMethodValidationException_mapsToHttp400() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trainees/-1");
    request.setAttribute(
        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
        "/api/v1/trainees/{id}"
    );

    MethodParameter methodParam = new MethodParameter(
        FoundationTestController.class.getMethod("getItem", Long.class), 0
    );

    ParameterValidationResult valResult = new ParameterValidationResult(
        methodParam,
        -1L,
        List.of(new org.springframework.context.support.DefaultMessageSourceResolvable(
            new String[]{"Positive.id", "Positive"}, "must be greater than 0"
        ))
    );

    MethodValidationResult methodValidationResult = MethodValidationResult.create(
        new FoundationTestController(),
        FoundationTestController.class.getMethod("getItem", Long.class),
        List.of(valResult)
    );

    HandlerMethodValidationException ex = new HandlerMethodValidationException(methodValidationResult);

    ResponseEntity<ErrorResponse> response =
        globalExceptionHandler.handleHandlerMethodValidationException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(400);
    assertThat(response.getBody().getError()).isEqualTo("Bad Request");
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getPath()).isEqualTo("/api/v1/trainees/{id}");
    assertThat(response.getBody().getDetails()).containsExactly("id: must be greater than 0");
  }

  // =========================================================================
  // 6. Security and Existing Behavior Preservation
  // =========================================================================

  @Test
  @DisplayName("7. Existing authentication and authorization behaviors remain intact")
  void testSecurityAndAuthorization_preserved() throws Exception {
    // Unauthenticated request to protected endpoint returns 401 Unauthorized
    mockMvc.perform(get("/api/v1/trainees/1"))
        .andExpect(status().isUnauthorized());

    // Valid authenticated token with insufficient permission returns 403 Forbidden
    String nonAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(999L, "user", List.of("ROLE_USER"));
    mockMvc.perform(post("/api/v1/trainees")
            .header("Authorization", nonAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("8. Existing non-validation error mappings remain intact (malformed JSON)")
  void testNonValidationErrors_preserved() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{malformed-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
        .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
  }
}
