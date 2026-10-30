package in.gov.sih.sih26135.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.exception.ErrorResponse;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.security.handler.RestAccessDeniedHandler;
import in.gov.sih.sih26135.security.handler.RestAuthenticationEntryPoint;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 26.12 — Response Envelope Alignment & Backward Compatibility Contract Verification.
 *
 * <p>Exhaustively audits and verifies:
 * <ul>
 *   <li>100% of all 55 REST controllers and their 629 endpoints return {@link ResponseEntity}
 *       with an {@link ApiResponse} envelope.</li>
 *   <li>Zero endpoints return bare entities, un-enveloped collections, or raw Spring Data {@link Page}.</li>
 *   <li>Single-resource responses adhere strictly to {@link ApiResponse} schema (preserving null {@code data}).</li>
 *   <li>Paginated responses adhere strictly to {@link ApiResponse}&lt;{@link PagedResponse}&lt;T&gt;&gt; schema.</li>
 *   <li>Empty pages serialize deterministic pagination metadata (page 0, size 0, total 0, empty list).</li>
 *   <li>Error responses adhere strictly to sanitized {@link ErrorResponse} schema.</li>
 *   <li>Security entry points and access denied handlers return standard {@link ApiResponse} envelopes.</li>
 *   <li>Live MockMvc execution verifies actual HTTP status codes, headers, and serialized JSON trees.</li>
 *   <li>Backward compatibility is preserved across all response fields and naming conventions.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.12 — Response Envelope Alignment & Backward Compatibility Contract Tests")
public class ResponseEnvelopeAlignmentContractTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

  @Autowired
  private RestAccessDeniedHandler restAccessDeniedHandler;

  private ObjectMapper objectMapper;
  private String adminToken;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "ROLE_ADMIN",
            "ADMIN",
            "course.manage",
            "trainee.read",
            "placement.manage",
            "center.manage",
            "provider.manage",
            "enrollment.manage"
        )
    );
    CorrelationContextHolder.clear();
  }

  @AfterEach
  void tearDown() {
    CorrelationContextHolder.clear();
  }

  // =========================================================================
  // 1. ARCHITECTURAL & REFLECTION AUDIT OF ALL 55 REST CONTROLLERS
  // =========================================================================

  @Nested
  @DisplayName("1. Exhaustive REST Controller Envelope Uniformity Audit")
  class RestControllerReflectionAuditTests {

    @Test
    @DisplayName("Verify exactly 55 production REST controllers exist and all 629 endpoints return ApiResponse envelope")
    void testAuditAll55ControllersAnd629EndpointsReturnApiResponseEnvelope() {
      Map<String, Object> controllerBeans = applicationContext.getBeansWithAnnotation(RestController.class);

      // Filter production controllers under in.gov.sih.sih26135.controller (exclude test mocks)
      List<Class<?>> productionControllers = controllerBeans.values().stream()
          .map(AopUtils::getTargetClass)
          .filter(clazz -> clazz.getName().startsWith("in.gov.sih.sih26135.controller"))
          .filter(clazz -> !clazz.getName().contains("Test")
              && !clazz.getName().contains("Spoofed")
              && !clazz.getName().contains("Unverified"))
          .distinct()
          .sorted(Comparator.comparing(Class::getName))
          .toList();

      // Baseline comparison: User specification noted 54 as baseline; verified actual production count is 55.
      assertThat(productionControllers)
          .as("Audit must discover exactly 55 production REST controllers across core domain, analytics, auth, and workflows")
          .hasSize(55);

      int totalAuditedEndpoints = 0;
      List<String> violations = new ArrayList<>();

      for (Class<?> controllerClass : productionControllers) {
        Method[] methods = controllerClass.getDeclaredMethods();
        for (Method method : methods) {
          boolean isEndpoint = method.isAnnotationPresent(GetMapping.class)
              || method.isAnnotationPresent(PostMapping.class)
              || method.isAnnotationPresent(PutMapping.class)
              || method.isAnnotationPresent(DeleteMapping.class)
              || method.isAnnotationPresent(PatchMapping.class)
              || (method.isAnnotationPresent(RequestMapping.class) && !method.getDeclaringClass().equals(Object.class));

          if (!isEndpoint) {
            continue;
          }

          totalAuditedEndpoints++;

          // Check return type is ResponseEntity
          if (!ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
            violations.add(String.format("%s.%s returns %s, expected ResponseEntity",
                controllerClass.getSimpleName(), method.getName(), method.getReturnType().getSimpleName()));
            continue;
          }

          // Check generic return type is ApiResponse
          Type genericReturnType = method.getGenericReturnType();
          if (!(genericReturnType instanceof ParameterizedType parameterizedType)) {
            violations.add(String.format("%s.%s has raw ResponseEntity return type without ApiResponse generic",
                controllerClass.getSimpleName(), method.getName()));
            continue;
          }

          Type[] typeArgs = parameterizedType.getActualTypeArguments();
          if (typeArgs.length == 0) {
            violations.add(String.format("%s.%s has empty type arguments on ResponseEntity",
                controllerClass.getSimpleName(), method.getName()));
            continue;
          }

          String genericTypeName = typeArgs[0].getTypeName();
          if (!genericTypeName.contains("ApiResponse")) {
            violations.add(String.format("%s.%s returns ResponseEntity<%s>, expected ResponseEntity<ApiResponse<...>>",
                controllerClass.getSimpleName(), method.getName(), genericTypeName));
          }
        }
      }

      assertThat(violations)
          .as("Zero envelope contract violations permitted across all production endpoints")
          .isEmpty();

      assertThat(totalAuditedEndpoints)
          .as("Audit must inspect and verify exactly 629 endpoints across all 55 controllers")
          .isEqualTo(629);
    }

    @Test
    @DisplayName("Verify GlobalExceptionHandler returns ResponseEntity<ErrorResponse> for all handled exceptions")
    void testAuditGlobalExceptionHandlerReturnsErrorResponseEnvelope() {
      Class<?> handlerClass = GlobalExceptionHandler.class;
      Method[] methods = handlerClass.getDeclaredMethods();

      int handlerCount = 0;
      List<String> violations = new ArrayList<>();

      for (Method method : methods) {
        if (!method.isAnnotationPresent(ExceptionHandler.class)) {
          continue;
        }

        handlerCount++;
        Class<?> returnType = method.getReturnType();

        // AccessDeniedException and AuthenticationException are rethrown for Spring Security handling
        if (returnType.equals(void.class)) {
          ExceptionHandler ann = method.getAnnotation(ExceptionHandler.class);
          List<Class<? extends Throwable>> handled = Arrays.asList(ann.value());
          boolean isSecurityRethrow = handled.contains(AccessDeniedException.class)
              || handled.contains(org.springframework.security.core.AuthenticationException.class);
          if (!isSecurityRethrow) {
            violations.add(String.format("GlobalExceptionHandler.%s returns void for non-security exception", method.getName()));
          }
          continue;
        }

        if (!ResponseEntity.class.isAssignableFrom(returnType)) {
          violations.add(String.format("GlobalExceptionHandler.%s returns %s, expected ResponseEntity<ErrorResponse>",
              method.getName(), returnType.getSimpleName()));
          continue;
        }

        Type genericReturnType = method.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType paramType)) {
          violations.add(String.format("GlobalExceptionHandler.%s has raw ResponseEntity return type", method.getName()));
          continue;
        }

        Type[] typeArgs = paramType.getActualTypeArguments();
        if (typeArgs.length == 0 || !typeArgs[0].getTypeName().contains("ErrorResponse")) {
          violations.add(String.format("GlobalExceptionHandler.%s does not return ResponseEntity<ErrorResponse>", method.getName()));
        }
      }

      assertThat(violations).isEmpty();
      assertThat(handlerCount)
          .as("GlobalExceptionHandler must handle all standard application, validation, and database errors")
          .isGreaterThanOrEqualTo(14);
    }
  }

  // =========================================================================
  // 2. SERIALIZATION CONTRACT: SINGLE-RESOURCE ApiResponse<T>
  // =========================================================================

  @Nested
  @DisplayName("2. Single-Resource ApiResponse<T> Serialization Contracts")
  class SingleResourceSerializationTests {

    record SampleResourceDto(Long id, String name, String code) {}

    @Test
    @DisplayName("Single-resource ApiResponse serializes success, message, data, and timestamp")
    void testSingleResourceSerializationWithPayload() throws Exception {
      SampleResourceDto dto = new SampleResourceDto(42L, "Sample Resource", "SMP-042");
      ApiResponse<SampleResourceDto> response = ApiResponse.success("Resource retrieved successfully", dto);

      String json = objectMapper.writeValueAsString(response);
      JsonNode root = objectMapper.readTree(json);

      assertThat(root.hasNonNull("success")).isTrue();
      assertThat(root.get("success").asBoolean()).isTrue();
      assertThat(root.get("message").asText()).isEqualTo("Resource retrieved successfully");
      assertThat(root.hasNonNull("timestamp")).isTrue();

      // Data payload integrity
      JsonNode dataNode = root.get("data");
      assertThat(dataNode).isNotNull();
      assertThat(dataNode.get("id").asLong()).isEqualTo(42L);
      assertThat(dataNode.get("name").asText()).isEqualTo("Sample Resource");
      assertThat(dataNode.get("code").asText()).isEqualTo("SMP-042");

      // Optional fields omitted when null
      assertThat(root.has("code")).isFalse();
      assertThat(root.has("path")).isFalse();
    }

    @Test
    @DisplayName("Single-resource ApiResponse with null payload explicitly serializes 'data: null' for frontend compatibility")
    void testSingleResourceSerializationWithNullPayload() throws Exception {
      ApiResponse<Void> response = ApiResponse.success("Operation completed");

      String json = objectMapper.writeValueAsString(response);
      JsonNode root = objectMapper.readTree(json);

      assertThat(root.get("success").asBoolean()).isTrue();
      assertThat(root.get("message").asText()).isEqualTo("Operation completed");
      assertThat(root.has("data")).isTrue();
      assertThat(root.get("data").isNull()).isTrue();
      assertThat(root.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("Single-resource ApiResponse serializes path and correlationId when present")
    void testSingleResourceSerializationWithPathAndCorrelationId() throws Exception {
      SampleResourceDto dto = new SampleResourceDto(100L, "Tracked Item", "TRK-100");
      String correlationId = "corr-test-" + UUID.randomUUID();
      ApiResponse<SampleResourceDto> response = ApiResponse.success(
          "Tracked retrieval", dto, "/api/v1/tracked/100", correlationId
      );

      String json = objectMapper.writeValueAsString(response);
      JsonNode root = objectMapper.readTree(json);

      assertThat(root.get("success").asBoolean()).isTrue();
      assertThat(root.get("path").asText()).isEqualTo("/api/v1/tracked/100");
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Single-resource ApiResponse roundtrip deserialization via @JsonCreator")
    void testSingleResourceDeserializationRoundTrip() throws Exception {
      String json = """
          {
            "success": true,
            "message": "Deserialized successfully",
            "data": { "id": 1, "name": "RoundTrip Test", "code": "RT-01" },
            "timestamp": "2026-10-30T10:00:00Z",
            "path": "/api/v1/test",
            "correlationId": "corr-roundtrip-999"
          }
          """;

      ApiResponse<SampleResourceDto> deserialized = objectMapper.readValue(
          json, new TypeReference<ApiResponse<SampleResourceDto>>() {}
      );

      assertThat(deserialized.isSuccess()).isTrue();
      assertThat(deserialized.getMessage()).isEqualTo("Deserialized successfully");
      assertThat(deserialized.getData()).isNotNull();
      assertThat(deserialized.getData().id()).isEqualTo(1L);
      assertThat(deserialized.getData().name()).isEqualTo("RoundTrip Test");
      assertThat(deserialized.getPath()).isEqualTo("/api/v1/test");
      assertThat(deserialized.getCorrelationId()).isEqualTo("corr-roundtrip-999");
    }
  }

  // =========================================================================
  // 3. SERIALIZATION CONTRACT: PAGINATED ApiResponse<PagedResponse<T>>
  // =========================================================================

  @Nested
  @DisplayName("3. Paginated Resource ApiResponse<PagedResponse<T>> Serialization Contracts")
  class PaginatedResourceSerializationTests {

    record ItemDto(Long id, String name) {}

    @Test
    @DisplayName("Paginated ApiResponse serializes outer envelope and all 9 PagedResponse fields")
    void testPaginatedResponseSerializationNonEmpty() throws Exception {
      List<ItemDto> content = List.of(
          new ItemDto(1L, "First Item"),
          new ItemDto(2L, "Second Item")
      );
      Page<ItemDto> springPage = new PageImpl<>(content, PageRequest.of(0, 20), 45L);
      PagedResponse<ItemDto> pagedResponse = PagedResponse.of(springPage);
      ApiResponse<PagedResponse<ItemDto>> envelope = ApiResponse.ok(pagedResponse);

      String json = objectMapper.writeValueAsString(envelope);
      JsonNode root = objectMapper.readTree(json);

      // Outer envelope
      assertThat(root.get("success").asBoolean()).isTrue();
      assertThat(root.get("message").asText()).isEqualTo("Operation completed successfully");
      assertThat(root.hasNonNull("timestamp")).isTrue();

      // PagedResponse payload inside 'data'
      JsonNode data = root.get("data");
      assertThat(data).isNotNull();

      // Verify all 9 standard pagination fields
      assertThat(data.has("content")).isTrue();
      assertThat(data.get("content").isArray()).isTrue();
      assertThat(data.get("content")).hasSize(2);
      assertThat(data.get("content").get(0).get("name").asText()).isEqualTo("First Item");

      assertThat(data.get("page").asInt()).isEqualTo(0);
      assertThat(data.get("size").asInt()).isEqualTo(20);
      assertThat(data.get("totalElements").asLong()).isEqualTo(45L);
      assertThat(data.get("totalPages").asInt()).isEqualTo(3);
      assertThat(data.get("first").asBoolean()).isTrue();
      assertThat(data.get("last").asBoolean()).isFalse();
      assertThat(data.get("hasNext").asBoolean()).isTrue();
      assertThat(data.get("hasPrevious").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Empty PagedResponse serializes deterministic metadata: content:[], totalElements:0, flags")
    void testEmptyPagedResponseSerialization() throws Exception {
      PagedResponse<ItemDto> emptyResponse = PagedResponse.empty();
      ApiResponse<PagedResponse<ItemDto>> envelope = ApiResponse.ok(emptyResponse);

      String json = objectMapper.writeValueAsString(envelope);
      JsonNode root = objectMapper.readTree(json);

      JsonNode data = root.get("data");
      assertThat(data.get("content").isArray()).isTrue();
      assertThat(data.get("content")).isEmpty();
      assertThat(data.get("page").asInt()).isEqualTo(0);
      assertThat(data.get("size").asInt()).isEqualTo(0);
      assertThat(data.get("totalElements").asLong()).isEqualTo(0L);
      assertThat(data.get("totalPages").asInt()).isEqualTo(0);
      assertThat(data.get("first").asBoolean()).isTrue();
      assertThat(data.get("last").asBoolean()).isTrue();
      assertThat(data.get("hasNext").asBoolean()).isFalse();
      assertThat(data.get("hasPrevious").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("PagedResponse.from with entity-to-DTO mapper produces identical metadata")
    void testPagedResponseFromMapper() throws Exception {
      record RawEntity(Long id, String rawField) {}
      List<RawEntity> entities = List.of(new RawEntity(10L, "Raw 10"));
      Page<RawEntity> rawPage = new PageImpl<>(entities, PageRequest.of(1, 10), 25L);

      PagedResponse<ItemDto> mappedResponse = PagedResponse.from(rawPage, e -> new ItemDto(e.id(), "Mapped " + e.rawField()));

      assertThat(mappedResponse.getContent()).hasSize(1);
      assertThat(mappedResponse.getContent().get(0).name()).isEqualTo("Mapped Raw 10");
      assertThat(mappedResponse.getPage()).isEqualTo(1);
      assertThat(mappedResponse.getSize()).isEqualTo(10);
      assertThat(mappedResponse.getTotalElements()).isEqualTo(25L);
      assertThat(mappedResponse.getTotalPages()).isEqualTo(3);
      assertThat(mappedResponse.isFirst()).isFalse();
      assertThat(mappedResponse.isLast()).isFalse();
      assertThat(mappedResponse.isHasNext()).isTrue();
      assertThat(mappedResponse.isHasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Paginated ApiResponse roundtrip deserialization via @JsonCreator")
    void testPaginatedResponseDeserializationRoundTrip() throws Exception {
      String json = """
          {
            "success": true,
            "message": "Page loaded",
            "data": {
              "content": [
                { "id": 1, "name": "Item A" }
              ],
              "page": 0,
              "size": 10,
              "totalElements": 1,
              "totalPages": 1,
              "first": true,
              "last": true,
              "hasNext": false,
              "hasPrevious": false
            },
            "timestamp": "2026-10-30T10:00:00Z"
          }
          """;

      ApiResponse<PagedResponse<ItemDto>> deserialized = objectMapper.readValue(
          json, new TypeReference<ApiResponse<PagedResponse<ItemDto>>>() {}
      );

      assertThat(deserialized.isSuccess()).isTrue();
      assertThat(deserialized.getData().getContent()).hasSize(1);
      assertThat(deserialized.getData().getContent().get(0).name()).isEqualTo("Item A");
      assertThat(deserialized.getData().getTotalElements()).isEqualTo(1L);
      assertThat(deserialized.getData().isFirst()).isTrue();
      assertThat(deserialized.getData().isLast()).isTrue();
    }
  }

  // =========================================================================
  // 4. SERIALIZATION CONTRACT: ErrorResponse
  // =========================================================================

  @Nested
  @DisplayName("4. ErrorResponse Serialization Contracts")
  class ErrorResponseSerializationTests {

    @Test
    @DisplayName("Sanitized ErrorResponse serializes status, error, code, message, path, correlationId, and omits nulls")
    void testSanitizedErrorResponseSerialization() throws Exception {
      String correlationId = "corr-err-" + UUID.randomUUID();
      ErrorResponse errorResponse = new ErrorResponse(
          400, "Bad Request", "VALIDATION_ERROR", "Request validation failed", "/api/v1/test", null, correlationId
      );

      String json = objectMapper.writeValueAsString(errorResponse);
      JsonNode root = objectMapper.readTree(json);

      assertThat(root.get("status").asInt()).isEqualTo(400);
      assertThat(root.get("error").asText()).isEqualTo("Bad Request");
      assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
      assertThat(root.get("message").asText()).isEqualTo("Request validation failed");
      assertThat(root.get("path").asText()).isEqualTo("/api/v1/test");
      assertThat(root.get("correlationId").asText()).isEqualTo(correlationId);
      assertThat(root.hasNonNull("timestamp")).isTrue();

      // Null fields omitted by @JsonInclude(NON_NULL)
      assertThat(root.has("details")).isFalse();
      assertThat(root.has("type")).isFalse();
    }

    @Test
    @DisplayName("Validation ErrorResponse serializes details array when populated")
    void testValidationErrorResponseSerializationWithDetails() throws Exception {
      List<String> details = List.of(
          "name: Name must not be blank",
          "code: Code must match uppercase alphanumeric pattern"
      );
      ErrorResponse errorResponse = new ErrorResponse(
          422, "Unprocessable Entity", "VALIDATION_ERROR", "Field validation failed", "/api/v1/test", details
      );

      String json = objectMapper.writeValueAsString(errorResponse);
      JsonNode root = objectMapper.readTree(json);

      assertThat(root.has("details")).isTrue();
      assertThat(root.get("details").isArray()).isTrue();
      assertThat(root.get("details")).hasSize(2);
      assertThat(root.get("details").get(0).asText()).isEqualTo("name: Name must not be blank");
    }
  }

  // =========================================================================
  // 5. SECURITY HANDLER SERIALIZATION CONTRACTS
  // =========================================================================

  @Nested
  @DisplayName("5. Spring Security Error Handler Contracts")
  class SecurityHandlerSerializationTests {

    @Test
    @DisplayName("RestAuthenticationEntryPoint returns 401 with standard ApiResponse envelope and correlation headers")
    void testRestAuthenticationEntryPointContract() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/secure/resource");
      MockHttpServletResponse response = new MockHttpServletResponse();
      String testCorrelationId = "corr-auth-" + UUID.randomUUID();
      CorrelationContextHolder.setCorrelationId(testCorrelationId);

      restAuthenticationEntryPoint.commence(
          request, response, new BadCredentialsException("Invalid credentials")
      );

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
      assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
      assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(testCorrelationId);
      assertThat(response.getHeader("X-Request-Id")).isEqualTo(testCorrelationId);

      JsonNode root = objectMapper.readTree(response.getContentAsString());
      assertThat(root.get("success").asBoolean()).isFalse();
      assertThat(root.get("code").asText()).isEqualTo("UNAUTHORIZED");
      assertThat(root.get("message").asText()).isEqualTo("Authentication is required to access this resource");
      assertThat(root.get("path").asText()).isEqualTo("/api/v1/secure/resource");
      assertThat(root.get("data").isNull()).isTrue();
      assertThat(root.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("RestAccessDeniedHandler returns 403 with standard ApiResponse envelope and correlation headers")
    void testRestAccessDeniedHandlerContract() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/privileged");
      MockHttpServletResponse response = new MockHttpServletResponse();
      String testCorrelationId = "corr-denied-" + UUID.randomUUID();
      CorrelationContextHolder.setCorrelationId(testCorrelationId);

      restAccessDeniedHandler.handle(
          request, response, new AccessDeniedException("Access denied")
      );

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
      assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
      assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(testCorrelationId);
      assertThat(response.getHeader("X-Request-Id")).isEqualTo(testCorrelationId);

      JsonNode root = objectMapper.readTree(response.getContentAsString());
      assertThat(root.get("success").asBoolean()).isFalse();
      assertThat(root.get("code").asText()).isEqualTo("FORBIDDEN");
      assertThat(root.get("message").asText()).isEqualTo("Access denied");
      assertThat(root.get("path").asText()).isEqualTo("/api/v1/admin/privileged");
      assertThat(root.get("data").isNull()).isTrue();
      assertThat(root.hasNonNull("timestamp")).isTrue();
    }
  }

  // =========================================================================
  // 6. LIVE MockMvc CONTROLLER ENDPOINT CONTRACT TESTS
  // =========================================================================

  @Nested
  @DisplayName("6. Live Controller Endpoint Envelope Contract Tests")
  class LiveControllerMockMvcTests {

    @Test
    @DisplayName("Live core domain paginated endpoint returns ApiResponse<PagedResponse<T>> envelope")
    void testLiveCoreDomainPaginatedEndpointContract() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "5")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.page").value(0))
          .andExpect(jsonPath("$.data.size").value(5))
          .andExpect(jsonPath("$.data.totalElements").isNumber())
          .andExpect(jsonPath("$.data.totalPages").isNumber())
          .andExpect(jsonPath("$.data.first").isBoolean())
          .andExpect(jsonPath("$.data.last").isBoolean())
          .andExpect(jsonPath("$.data.hasNext").isBoolean())
          .andExpect(jsonPath("$.data.hasPrevious").isBoolean());
    }

    @Test
    @DisplayName("Live resource lookup not found returns sanitized ErrorResponse envelope")
    void testLiveResourceNotFoundReturnsErrorResponseEnvelope() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("code", "NONEXISTENT_COURSE_CODE_99999")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.path").value("/api/v1/courses"))
          .andExpect(jsonPath("$.correlationId").isString())
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Live validation error returns sanitized ErrorResponse envelope with code INVALID_PAGE")
    void testLiveValidationErrorReturnsErrorResponseEnvelope() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "-1")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE"))
          .andExpect(jsonPath("$.message").value("Page index must not be less than zero"))
          .andExpect(jsonPath("$.path").value("/api/v1/courses"))
          .andExpect(jsonPath("$.correlationId").isString());
    }

    @Test
    @DisplayName("Live unauthorized request returns standard ApiResponse envelope with code UNAUTHORIZED")
    void testLiveUnauthorizedReturnsApiResponseEnvelope() throws Exception {
      mockMvc.perform(get("/api/v1/users")
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isUnauthorized())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
          .andExpect(jsonPath("$.data").isEmpty())
          .andExpect(jsonPath("$.path").value("/api/v1/users"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Live 404 for non-existent endpoint returns sanitized ErrorResponse envelope")
    void testLiveNonExistentEndpointReturnsErrorResponseEnvelope() throws Exception {
      mockMvc.perform(get("/api/v1/non-existent-endpoint-phase2612")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
          .andExpect(jsonPath("$.correlationId").isString());
    }
  }

  // =========================================================================
  // 7. BACKWARD COMPATIBILITY CONTRACT SAFEGUARDS
  // =========================================================================

  @Nested
  @DisplayName("7. Backward Compatibility Contract Safeguards")
  class BackwardCompatibilityContractSafeguardsTests {

    @Test
    @DisplayName("Verify ApiResponse property contract is frozen and preserves all consumer fields")
    void testApiResponseContractFreeze() {
      Set<String> declaredFields = Arrays.stream(ApiResponse.class.getDeclaredFields())
          .map(java.lang.reflect.Field::getName)
          .collect(Collectors.toSet());

      assertThat(declaredFields)
          .as("ApiResponse must retain all contractual fields expected by frontend and downstream consumers")
          .containsExactlyInAnyOrder("success", "message", "data", "timestamp", "path", "code", "correlationId");
    }

    @Test
    @DisplayName("Verify PagedResponse property contract is frozen and preserves all consumer fields")
    void testPagedResponseContractFreeze() {
      Set<String> declaredFields = Arrays.stream(PagedResponse.class.getDeclaredFields())
          .map(java.lang.reflect.Field::getName)
          .collect(Collectors.toSet());

      assertThat(declaredFields)
          .as("PagedResponse must retain all contractual fields expected by frontend pagination tables")
          .containsExactlyInAnyOrder(
              "content", "page", "size", "totalElements", "totalPages", "first", "last", "hasNext", "hasPrevious"
          );
    }

    @Test
    @DisplayName("Verify ErrorResponse property contract is frozen and preserves all consumer fields")
    void testErrorResponseContractFreeze() {
      Set<String> declaredFields = Arrays.stream(ErrorResponse.class.getDeclaredFields())
          .map(java.lang.reflect.Field::getName)
          .collect(Collectors.toSet());

      assertThat(declaredFields)
          .as("ErrorResponse must retain all contractual fields expected by error boundary handlers")
          .containsExactlyInAnyOrder(
              "type", "timestamp", "status", "error", "code", "message", "path", "details", "correlationId"
          );
    }
  }
}
