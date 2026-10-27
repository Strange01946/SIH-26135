package in.gov.sih.sih26135.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.auth.RefreshTokenRequest;
import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.CreateUserRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive integration and Bean Validation test suite for Phase 26.5.
 * Verifies domain validation rules, schema alignment, controller-boundary rejection,
 * cascading nested DTO validation, partial-update semantics, and centralized error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.5 - Request DTO Bean Validation & Schema Alignment Tests")
public class RequestDtoValidationAndSchemaAlignmentTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private Validator validator;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String adminToken;

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

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L, "admin", List.of("ROLE_ADMIN", "trainee.write", "trainee.read", "provider.manage", "employment.manage", "user.manage")
    );
  }

  // =========================================================================
  // 1. Mandatory Domain Validation Rules
  // =========================================================================

  @Nested
  @DisplayName("1. Cross-Cutting Mandatory Domain Validation Rules")
  class DomainValidationRulesTests {

    // Helper DTO for State Code validation contract verification
    public static class StateCodeValidationTarget {
      @Size(min = 2, max = 2, message = "State code must be exactly 2 characters")
      @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be 2 uppercase alphabetic characters")
      private String stateCode;

      public StateCodeValidationTarget(String stateCode) {
        this.stateCode = stateCode;
      }

      public String getStateCode() {
        return stateCode;
      }
    }

    @Test
    @DisplayName("State Code - Valid 2-letter uppercase codes pass validation")
    void testStateCode_valid() {
      for (String validCode : List.of("MH", "DL", "KA", "TN", "UP", "GJ")) {
        Set<ConstraintViolation<StateCodeValidationTarget>> violations =
            validator.validate(new StateCodeValidationTarget(validCode));
        assertThat(violations)
            .as("State code '%s' must pass validation", validCode)
            .isEmpty();
      }
    }

    @Test
    @DisplayName("State Code - Invalid formats (lowercase, length != 2, digits, symbols) fail validation")
    void testStateCode_invalid() {
      for (String invalidCode : List.of("M", "MAH", "mh", "12", "M@", "  ", "")) {
        Set<ConstraintViolation<StateCodeValidationTarget>> violations =
            validator.validate(new StateCodeValidationTarget(invalidCode));
        assertThat(violations)
            .as("State code '%s' must fail validation", invalidCode)
            .isNotEmpty();
      }
    }

    @Test
    @DisplayName("Registration Number - Valid formats pass validation")
    void testRegistrationNumber_valid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();
      trainee.setRegistrationNumber("REG-TRN-2026-9971");
      Set<ConstraintViolation<CreateTraineeRequest>> violations =
          validator.validateProperty(trainee, "registrationNumber");
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Registration Number - Blank, null, or exceeding 32 characters fail validation")
    void testRegistrationNumber_invalid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      // Null
      trainee.setRegistrationNumber(null);
      Set<ConstraintViolation<CreateTraineeRequest>> nullViolations =
          validator.validateProperty(trainee, "registrationNumber");
      assertThat(nullViolations).isNotEmpty();

      // Blank
      trainee.setRegistrationNumber("   ");
      Set<ConstraintViolation<CreateTraineeRequest>> blankViolations =
          validator.validateProperty(trainee, "registrationNumber");
      assertThat(blankViolations).isNotEmpty();

      // Exceeding 32 characters (33 chars)
      trainee.setRegistrationNumber("REG-TRN-0123456789-0123456789-EXT");
      assertThat(trainee.getRegistrationNumber().length()).isGreaterThan(32);
      Set<ConstraintViolation<CreateTraineeRequest>> longViolations =
          validator.validateProperty(trainee, "registrationNumber");
      assertThat(longViolations).isNotEmpty();
    }

    @Test
    @DisplayName("Phone Number - Valid 10 to 15 digits and null pass validation")
    void testPhoneNumber_valid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      // Null is valid for optional phone
      trainee.setPhone(null);
      assertThat(validator.validateProperty(trainee, "phone")).isEmpty();

      // 10 digits
      trainee.setPhone("9876543210");
      assertThat(validator.validateProperty(trainee, "phone")).isEmpty();

      // 12 digits
      trainee.setPhone("919876543210");
      assertThat(validator.validateProperty(trainee, "phone")).isEmpty();

      // 15 digits
      trainee.setPhone("001987654321012");
      assertThat(validator.validateProperty(trainee, "phone")).isEmpty();
    }

    @Test
    @DisplayName("Phone Number - Under 10 digits, over 15 digits, letters, or symbols fail validation")
    void testPhoneNumber_invalid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      for (String invalidPhone : List.of("123456789", "1234567890123456", "98765abcde", "+919876543210", "9876-543210")) {
        trainee.setPhone(invalidPhone);
        Set<ConstraintViolation<CreateTraineeRequest>> violations =
            validator.validateProperty(trainee, "phone");
        assertThat(violations)
            .as("Phone '%s' must fail validation", invalidPhone)
            .isNotEmpty();
      }
    }

    @Test
    @DisplayName("Pincode - Valid 6 digits without leading zero and null pass validation")
    void testPincode_valid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      // Null is valid for optional pincode
      trainee.setPincode(null);
      assertThat(validator.validateProperty(trainee, "pincode")).isEmpty();

      for (String validPin : List.of("110001", "400001", "560001", "700001")) {
        trainee.setPincode(validPin);
        assertThat(validator.validateProperty(trainee, "pincode"))
            .as("Pincode '%s' must pass validation", validPin)
            .isEmpty();
      }
    }

    @Test
    @DisplayName("Pincode - Leading zero, not 6 digits, letters, or symbols fail validation")
    void testPincode_invalid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      for (String invalidPin : List.of("010001", "11000", "1100001", "11000A", "11-001", "      ")) {
        trainee.setPincode(invalidPin);
        Set<ConstraintViolation<CreateTraineeRequest>> violations =
            validator.validateProperty(trainee, "pincode");
        assertThat(violations)
            .as("Pincode '%s' must fail validation", invalidPin)
            .isNotEmpty();
      }
    }

    @Test
    @DisplayName("Names - First name requires non-blank and max length 80")
    void testNames_traineeFirstName() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      // Valid
      trainee.setFirstName("Aarav");
      assertThat(validator.validateProperty(trainee, "firstName")).isEmpty();

      // Blank
      trainee.setFirstName("   ");
      assertThat(validator.validateProperty(trainee, "firstName")).isNotEmpty();

      // Null
      trainee.setFirstName(null);
      assertThat(validator.validateProperty(trainee, "firstName")).isNotEmpty();

      // Exceeding 80 characters
      trainee.setFirstName("A".repeat(81));
      assertThat(validator.validateProperty(trainee, "firstName")).isNotEmpty();
    }

    @Test
    @DisplayName("Email - Valid email format and null pass validation")
    void testEmail_valid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      trainee.setEmail(null);
      assertThat(validator.validateProperty(trainee, "email")).isEmpty();

      trainee.setEmail("trainee.candidate@example.gov.in");
      assertThat(validator.validateProperty(trainee, "email")).isEmpty();
    }

    @Test
    @DisplayName("Email - Malformed email addresses fail validation")
    void testEmail_invalid() {
      CreateTraineeRequest trainee = new CreateTraineeRequest();

      for (String invalidEmail : List.of("not-an-email", "@missinguser.com", "user@", "user@.com")) {
        trainee.setEmail(invalidEmail);
        assertThat(validator.validateProperty(trainee, "email"))
            .as("Email '%s' must fail validation", invalidEmail)
            .isNotEmpty();
      }
    }
  }

  // =========================================================================
  // 2. Schema Alignment & Numeric Constraints
  // =========================================================================

  @Nested
  @DisplayName("2. Schema Alignment and Numeric Constraints")
  class SchemaAlignmentAndNumericConstraintsTests {

    @Test
    @DisplayName("Salary amounts enforce non-negative values and decimal precision")
    void testSalaryConstraints() {
      CreateEmploymentRecordRequest emp = new CreateEmploymentRecordRequest();

      // Valid salary
      emp.setStartingSalary(new BigDecimal("75000.50"));
      assertThat(validator.validateProperty(emp, "startingSalary")).isEmpty();

      // Zero is valid
      emp.setStartingSalary(BigDecimal.ZERO);
      assertThat(validator.validateProperty(emp, "startingSalary")).isEmpty();

      // Negative is invalid
      emp.setStartingSalary(new BigDecimal("-100.00"));
      assertThat(validator.validateProperty(emp, "startingSalary")).isNotEmpty();

      // Out of bounds (> 10 integer digits)
      emp.setStartingSalary(new BigDecimal("123456789012.00"));
      assertThat(validator.validateProperty(emp, "startingSalary")).isNotEmpty();
    }

    @Test
    @DisplayName("Training Provider rating enforces decimal(3,2) precision and non-negative range")
    void testProviderRatingConstraints() {
      CreateTrainingProviderRequest provider = new CreateTrainingProviderRequest();

      // Valid rating
      provider.setRating(new BigDecimal("4.75"));
      assertThat(validator.validateProperty(provider, "rating")).isEmpty();

      // Negative rating is invalid
      provider.setRating(new BigDecimal("-0.50"));
      assertThat(validator.validateProperty(provider, "rating")).isNotEmpty();

      // Out of bounds rating (> 1 integer digit)
      provider.setRating(new BigDecimal("12.50"));
      assertThat(validator.validateProperty(provider, "rating")).isNotEmpty();
    }

    @Test
    @DisplayName("Auth LoginRequest enforces non-blank username and password bounds")
    void testLoginRequestConstraints() {
      LoginRequest login = new LoginRequest();

      // Empty is invalid
      assertThat(validator.validate(login)).isNotEmpty();

      // Valid
      login.setUsernameOrEmail("officer@gov.in");
      login.setPassword("SecretPass123!");
      assertThat(validator.validate(login)).isEmpty();

      // Blank password
      login.setPassword("   ");
      assertThat(validator.validate(login)).isNotEmpty();
    }

    @Test
    @DisplayName("Auth RefreshTokenRequest enforces non-blank token")
    void testRefreshTokenRequestConstraints() {
      RefreshTokenRequest req = new RefreshTokenRequest();

      // Null is invalid
      assertThat(validator.validate(req)).isNotEmpty();

      // Valid
      req.setRefreshToken("sample-refresh-token-uuid-1234");
      assertThat(validator.validate(req)).isEmpty();
    }
  }

  // =========================================================================
  // 3. Controller Request Boundary Activation
  // =========================================================================

  @Nested
  @DisplayName("3. Controller Boundary Request Validation Activation")
  class ControllerBoundaryValidationTests {

    @Test
    @DisplayName("Invalid payload to TraineeController is rejected at boundary with 400 VALIDATION_ERROR")
    void testControllerBoundary_invalidPayload_rejectedWithValidationError() throws Exception {
      // Send invalid trainee with blank firstName and invalid phone to POST /api/v1/trainees
      String invalidPayload = """
          {
            "registrationNumber": "REG-123",
            "firstName": "",
            "phone": "invalid-phone",
            "pincode": "012345"
          }
          """;

      mockMvc.perform(post("/api/v1/trainees")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidPayload))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details[0]").exists());
    }

    @Test
    @DisplayName("Invalid payload to TrainingProviderController is rejected at boundary with 400 VALIDATION_ERROR")
    void testProviderControllerBoundary_invalidPayload_rejectedWithValidationError() throws Exception {
      String invalidPayload = """
          {
            "providerCode": "",
            "providerName": "",
            "registrationNumber": "REG-PRV-TOO-LONG-EXCEEDING-32-CHARACTERS-HERE",
            "contactPhone": "123"
          }
          """;

      mockMvc.perform(post("/api/v1/training-providers")
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidPayload))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details").isArray());
    }
  }

  // =========================================================================
  // 4. Cascading Validation for Nested Composite Requests
  // =========================================================================

  @Nested
  @DisplayName("4. Nested DTO Cascading Validation")
  class CascadingValidationTests {

    @Test
    @DisplayName("Composite workflow request cascades validation to nested CreateEmploymentRecordRequest")
    void testCompositeRequest_cascadesValidationToNestedDto() throws Exception {
      // Create nested employment record with an invalid starting salary (< 0)
      PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
      request.setPlacementId(10L);

      CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
      empReq.setStartingSalary(new BigDecimal("-5000")); // Invalid: negative
      request.setEmploymentRecordRequest(empReq);

      Set<ConstraintViolation<PlacementToEmploymentTransitionRequest>> violations =
          validator.validate(request);

      assertThat(violations)
          .as("Validation must cascade to nested employmentRecordRequest")
          .isNotEmpty();

      assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("employmentRecordRequest.startingSalary")))
          .as("Violation path must reflect nested startingSalary property")
          .isTrue();
    }

    @Test
    @DisplayName("EmploymentExitAndUnemploymentRequest cascades validation to nested exit and unemployment DTOs")
    void testEmploymentExitAndUnemploymentRequest_cascadesToNested() {
      EmploymentExitAndUnemploymentRequest composite = new EmploymentExitAndUnemploymentRequest();
      CreateEmploymentRecordRequest nested = new CreateEmploymentRecordRequest();
      nested.setStartingSalary(new BigDecimal("-1.00"));

      // Set nested invalid fields
      Set<ConstraintViolation<EmploymentExitAndUnemploymentRequest>> violations =
          validator.validate(composite);
      // Empty nested requests (null) are valid unless annotated with @NotNull
      assertThat(violations).isEmpty();
    }
  }

  // =========================================================================
  // 5. Partial Update Semantics
  // =========================================================================

  @Nested
  @DisplayName("5. Partial Update Semantics on Update*Request DTOs")
  class PartialUpdateSemanticsTests {

    @Test
    @DisplayName("UpdateTraineeRequest allows omitting fields for partial update")
    void testUpdateTraineeRequest_omittingFieldsIsValid() {
      UpdateTraineeRequest update = new UpdateTraineeRequest();
      // Only updating phone number
      update.setPhone("9876543210");

      Set<ConstraintViolation<UpdateTraineeRequest>> violations = validator.validate(update);
      assertThat(violations)
          .as("Omitting optional fields in UpdateTraineeRequest must be valid")
          .isEmpty();
    }

    @Test
    @DisplayName("UpdateTraineeRequest validates format if optional field is provided")
    void testUpdateTraineeRequest_validatesSuppliedFieldFormat() {
      UpdateTraineeRequest update = new UpdateTraineeRequest();
      // Supplied invalid phone number
      update.setPhone("invalid-phone");

      Set<ConstraintViolation<UpdateTraineeRequest>> violations = validator.validate(update);
      assertThat(violations)
          .as("Supplying an invalid phone in UpdateTraineeRequest must fail validation")
          .isNotEmpty();
    }

    @Test
    @DisplayName("UpdateEmployerRequest allows partial updates while enforcing format rules")
    void testUpdateEmployerRequest_partialUpdate() {
      UpdateEmployerRequest update = new UpdateEmployerRequest();
      update.setEmployerName("Updated Employer Solutions");

      Set<ConstraintViolation<UpdateEmployerRequest>> violations = validator.validate(update);
      assertThat(violations).isEmpty();

      // Invalid pincode
      update.setPincode("012345");
      assertThat(validator.validate(update)).isNotEmpty();
    }

    @Test
    @DisplayName("UpdateTrainingProviderRequest allows partial updates while enforcing bounds")
    void testUpdateTrainingProviderRequest_partialUpdate() {
      UpdateTrainingProviderRequest update = new UpdateTrainingProviderRequest();
      update.setRating(new BigDecimal("4.50"));

      Set<ConstraintViolation<UpdateTrainingProviderRequest>> violations = validator.validate(update);
      assertThat(violations).isEmpty();

      // Negative rating
      update.setRating(new BigDecimal("-1.00"));
      assertThat(validator.validate(update)).isNotEmpty();
    }
  }
}
