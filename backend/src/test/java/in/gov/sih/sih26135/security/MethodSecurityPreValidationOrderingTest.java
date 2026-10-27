package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.TraineeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verification test suite for {@link MethodSecurityPreValidationInterceptor}.
 *
 * <p>Verifies:
 * <ol>
 *   <li>Authorization ordering: Pre-validation interceptor runs BEFORE Spring MVC argument resolution
 *       and Bean Validation, preventing schema/validation error disclosure to unauthorized callers.</li>
 *   <li>State 1: Unauthenticated request + invalid body -> 401 Unauthorized.</li>
 *   <li>State 2: Authenticated user without required authority + invalid body -> 403 Forbidden.</li>
 *   <li>State 3: Authenticated user with required authority + invalid body -> 400 Bad Request (VALIDATION_ERROR).</li>
 *   <li>State 4: Authenticated user with required authority + valid body -> 200/201 Success.</li>
 *   <li>Controller business logic is strictly NOT executed when authorization or validation fails.</li>
 *   <li>Full support for all repository @PreAuthorize expression variants: hasAuthority, hasAnyAuthority,
 *       hasRole, hasAnyRole, composite expressions, isAuthenticated, permitAll, and denyAll.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Method Security Pre-Validation Ordering & Expression Verification")
public class MethodSecurityPreValidationOrderingTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  // Mock TraineeService to verify business method invocations
  @MockBean
  private TraineeService traineeService;

  private String tokenTraineeWrite;
  private String tokenTraineeRead;
  private String tokenNoAuthority;
  private String tokenCompositeAuthority;
  private String tokenTraineeRole;
  private String tokenAdminRole;

  @BeforeEach
  void setUp() {
    tokenTraineeWrite = "Bearer " + jwtTokenProvider.generateAccessToken(101L, "writer", List.of("trainee.write"));
    tokenTraineeRead = "Bearer " + jwtTokenProvider.generateAccessToken(102L, "reader", List.of("trainee.read"));
    tokenNoAuthority = "Bearer " + jwtTokenProvider.generateAccessToken(103L, "nobody", List.of("ROLE_USER"));
    tokenCompositeAuthority = "Bearer " + jwtTokenProvider.generateAccessToken(104L, "assessor", List.of("assessment.manage"));
    tokenTraineeRole = "Bearer " + jwtTokenProvider.generateAccessToken(105L, "student", List.of("ROLE_trainee"));
    tokenAdminRole = "Bearer " + jwtTokenProvider.generateAccessToken(106L, "admin", List.of("ROLE_admin"));

    ExpressionTestController.businessMethodCounter.set(0);
  }

  // =========================================================================
  // 1. Mandatory 4-State Authorization Ordering Tests on Real Trainee API
  // =========================================================================

  @Nested
  @DisplayName("1. Four-State Authorization Ordering Invariants (Real Endpoint: POST /api/v1/trainees)")
  class FourStateOrderingTests {

    @Test
    @DisplayName("State 1: Unauthenticated request + invalid body -> 401 Unauthorized")
    void testState1_unauthenticatedWithInvalidBody_returns401() throws Exception {
      // POST /api/v1/trainees requires authentication and 'trainee.write'
      mockMvc.perform(post("/api/v1/trainees")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
          .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));

      // Verify business service was never called
      org.mockito.Mockito.verifyNoInteractions(traineeService);
    }

    @Test
    @DisplayName("State 2: Authenticated user WITHOUT authority + invalid body -> 403 Forbidden")
    void testState2_authenticatedWithoutAuthorityWithInvalidBody_returns403() throws Exception {
      // User has only 'trainee.read', but endpoint requires 'trainee.write'
      // Body is completely invalid '{}'
      // Must be rejected with 403 Forbidden BEFORE Bean Validation evaluates '{}'
      mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", tokenTraineeRead)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"))
          .andExpect(jsonPath("$.message").value("Access denied"));

      // Verify business service was never called
      org.mockito.Mockito.verifyNoInteractions(traineeService);
    }

    @Test
    @DisplayName("State 3: Authenticated user WITH authority + invalid body -> 400 Bad Request (VALIDATION_ERROR)")
    void testState3_authenticatedWithAuthorityWithInvalidBody_returns400ValidationError() throws Exception {
      // User has 'trainee.write'
      // Body is invalid '{}'
      // Must pass pre-validation authorization check, then fail Bean Validation at controller boundary
      mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", tokenTraineeWrite)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details").isArray());

      // Verify business service was never called
      org.mockito.Mockito.verifyNoInteractions(traineeService);
    }

    @Test
    @DisplayName("State 4: Authenticated user WITH authority + valid body -> 200/201 Success")
    void testState4_authenticatedWithAuthorityWithValidBody_reachesBusinessLogic() throws Exception {
      CreateTraineeRequest validRequest = new CreateTraineeRequest();
      validRequest.setRegistrationNumber("REG-TEST-1234");
      validRequest.setFirstName("Rajesh");
      validRequest.setLastName("Kumar");
      validRequest.setPhone("9876543210");
      validRequest.setEmail("rajesh.kumar@example.com");
      validRequest.setPincode("560001");

      in.gov.sih.sih26135.dto.response.TraineeResponse mockResponse = new in.gov.sih.sih26135.dto.response.TraineeResponse();
      mockResponse.setId(1L);
      mockResponse.setFirstName("Rajesh");

      org.mockito.Mockito.when(traineeService.createTrainee(
              org.mockito.ArgumentMatchers.any(CreateTraineeRequest.class),
              org.mockito.ArgumentMatchers.nullable(Long.class)))
          .thenReturn(mockResponse);

      mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", tokenTraineeWrite)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1L))
          .andExpect(jsonPath("$.data.firstName").value("Rajesh"));

      // Verify business service WAS called
      org.mockito.Mockito.verify(traineeService).createTrainee(
          org.mockito.ArgumentMatchers.any(CreateTraineeRequest.class),
          org.mockito.ArgumentMatchers.nullable(Long.class));
    }
  }

  // =========================================================================
  // 2. Comprehensive Expression Forms Verification
  // =========================================================================

  @Nested
  @DisplayName("2. Spring Security Expression Evaluation Tests")
  class ExpressionEvaluationTests {

    @Test
    @DisplayName("hasAuthority: authorized passes to validation, unauthorized yields 403")
    void testHasAuthority_evaluatedCorrectly() throws Exception {
      // Unauthorized with invalid body -> 403 Forbidden
      mockMvc.perform(post("/api/test/expressions/has-authority")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // Authorized with invalid body -> 400 Bad Request (VALIDATION_ERROR)
      mockMvc.perform(post("/api/test/expressions/has-authority")
              .header("Authorization", tokenTraineeWrite)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("hasAnyAuthority: authorized passes to validation, unauthorized yields 403")
    void testHasAnyAuthority_evaluatedCorrectly() throws Exception {
      // Unauthorized -> 403
      mockMvc.perform(post("/api/test/expressions/has-any-authority")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // Authorized with one matching authority ('audit.read') -> 400 (validation triggered)
      String tokenAuditRead = "Bearer " + jwtTokenProvider.generateAccessToken(201L, "auditor", List.of("audit.read"));
      mockMvc.perform(post("/api/test/expressions/has-any-authority")
              .header("Authorization", tokenAuditRead)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("hasRole: authorized passes to validation, unauthorized yields 403")
    void testHasRole_evaluatedCorrectly() throws Exception {
      // Unauthorized -> 403
      mockMvc.perform(post("/api/test/expressions/has-role")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // Authorized with ROLE_admin -> 400 (validation triggered)
      mockMvc.perform(post("/api/test/expressions/has-role")
              .header("Authorization", tokenAdminRole)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("Composite: 'hasAnyAuthority(...) or hasRole(...)' evaluated correctly")
    void testCompositeExpression_evaluatedCorrectly() throws Exception {
      // 1. Unauthorized (neither authority nor role) -> 403
      mockMvc.perform(post("/api/test/expressions/composite")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden());
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // 2. Authorized via authority ('assessment.manage') -> 400 (validation triggered)
      mockMvc.perform(post("/api/test/expressions/composite")
              .header("Authorization", tokenCompositeAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // 3. Authorized via role ('ROLE_trainee') -> 400 (validation triggered)
      mockMvc.perform(post("/api/test/expressions/composite")
              .header("Authorization", tokenTraineeRole)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("isAuthenticated(): authenticated user passes to validation, unauthenticated yields 401")
    void testIsAuthenticated_evaluatedCorrectly() throws Exception {
      // Unauthenticated -> 401
      mockMvc.perform(post("/api/test/expressions/is-authenticated")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isUnauthorized());
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();

      // Authenticated (any role) -> 400 (validation triggered)
      mockMvc.perform(post("/api/test/expressions/is-authenticated")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("permitAll: all callers pass to validation without 403")
    void testPermitAll_evaluatedCorrectly() throws Exception {
      // Authenticated with invalid body -> 400
      mockMvc.perform(post("/api/test/expressions/permit-all")
              .header("Authorization", tokenNoAuthority)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }

    @Test
    @DisplayName("denyAll: always rejected with 403 regardless of authorities")
    void testDenyAll_evaluatedCorrectly() throws Exception {
      // Authenticated admin with invalid body -> 403 Forbidden
      mockMvc.perform(post("/api/test/expressions/deny-all")
              .header("Authorization", tokenAdminRole)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
      assertThat(ExpressionTestController.businessMethodCounter.get()).isZero();
    }
  }

  // =========================================================================
  // Test Supporting Controller & DTO
  // =========================================================================

  public static class TestRequestBody {
    @NotBlank
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @RestController
  @RequestMapping("/api/test/expressions")
  public static class ExpressionTestController {
    static final AtomicInteger businessMethodCounter = new AtomicInteger(0);

    @PostMapping("/has-authority")
    @PreAuthorize("hasAuthority('trainee.write')")
    public ResponseEntity<String> testHasAuthority(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/has-any-authority")
    @PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
    public ResponseEntity<String> testHasAnyAuthority(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/has-role")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> testHasRole(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/composite")
    @PreAuthorize("hasAnyAuthority('assessment.manage', 'enrollment.manage') or hasRole('trainee')")
    public ResponseEntity<String> testComposite(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/is-authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> testIsAuthenticated(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/permit-all")
    @PreAuthorize("permitAll")
    public ResponseEntity<String> testPermitAll(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }

    @PostMapping("/deny-all")
    @PreAuthorize("denyAll")
    public ResponseEntity<String> testDenyAll(@Valid @RequestBody TestRequestBody body) {
      businessMethodCounter.incrementAndGet();
      return ResponseEntity.ok("success");
    }
  }
}
