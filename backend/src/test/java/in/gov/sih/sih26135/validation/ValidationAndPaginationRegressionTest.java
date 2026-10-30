package in.gov.sih.sih26135.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.CourseService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 26.13 — Comprehensive Validation, Pagination, Sorting & Malformed Input Regression Tests.
 *
 * <p>Exhaustively verifies:
 * <ul>
 *   <li>DTO validation rules across auth, core domain, and workflow requests (valid, missing, blank, min, max, patterns).</li>
 *   <li>Multiple simultaneous validation violations with deterministic alphabetical error detail ordering.</li>
 *   <li>Anti-echo privacy: sensitive passwords, tokens, and secrets are strictly masked from error responses.</li>
 *   <li>Controller boundary non-execution: invalid payloads fail before controller business logic executes.</li>
 *   <li>Pagination boundaries: first page, middle page, out-of-bounds page, zero-indexing, negative page, min/max limits, oversized pages.</li>
 *   <li>Sorting behavior: default sorting, ASC/DESC, alias resolution, invalid sort properties, sensitive sort term rejection, malformed sort expressions.</li>
 *   <li>Malformed input & type conversion: broken JSON, empty body, missing body, wrong data types, invalid dates, path variable / query param mismatches.</li>
 *   <li>API response contracts: standardized {@code ApiResponse} and {@code ErrorResponse} envelopes with correlation headers.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.13 — Validation & Pagination Regression Testing")
public class ValidationAndPaginationRegressionTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private Validator validator;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @SpyBean
  private CourseService courseService;

  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    assertThat(dataSource)
        .as("DataSource bean must be present in application context")
        .isNotNull();

    // Enforce database isolation: target strictly SIH26135_TEST
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "ROLE_ADMIN",
            "ADMIN",
            "course.manage",
            "trainee.read",
            "trainee.write",
            "placement.manage",
            "center.manage",
            "provider.manage",
            "enrollment.manage",
            "employment.manage",
            "user.manage",
            "audit.read",
            "system.manage",
            "survey.manage",
            "assessment.manage"
        )
    );
  }

  // =========================================================================
  // 1. DTO VALIDATION REGRESSION TESTS (Section 2.1)
  // =========================================================================

  @Nested
  @DisplayName("1. DTO Request Body Bean Validation Regression Tests")
  class DtoValidationRegressionTests {

    @Test
    @DisplayName("LoginRequest - Blank usernameOrEmail is rejected by controller with HTTP 400 BAD_REQUEST")
    void testLoginRequest_blankUsernameRejected() throws Exception {
      LoginRequest request = new LoginRequest();
      request.setUsernameOrEmail("   ");
      request.setPassword("ValidPassword123!");

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.message").value("Username or email is required"));
    }

    @Test
    @DisplayName("LoginRequest - Blank password is rejected by controller with HTTP 400 BAD_REQUEST and sanitized message")
    void testLoginRequest_blankPasswordRejected() throws Exception {
      LoginRequest request = new LoginRequest();
      request.setUsernameOrEmail("valid_user");
      request.setPassword("");

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
          // Phase 26.9 Sanitization: "Password is required" contains "password" and is masked to "The request is invalid"
          .andExpect(jsonPath("$.message").value("The request is invalid"));
    }

    @Test
    @DisplayName("LoginRequest - Bean Validation detects missing usernameOrEmail and password on DTO")
    void testLoginRequest_dtoBeanValidationViolations() {
      LoginRequest request = new LoginRequest("", "");

      Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
      assertThat(violations).hasSize(2);

      List<String> propertyPaths = violations.stream()
          .map(v -> v.getPropertyPath().toString())
          .sorted()
          .toList();
      assertThat(propertyPaths).containsExactly("password", "usernameOrEmail");
    }

    @Test
    @DisplayName("CreateCourseRequest - Multiple simultaneous violations are alphabetically sorted in error details")
    void testCreateCourseRequest_multipleViolationsAlphabeticalOrder() throws Exception {
      CreateCourseRequest request = new CreateCourseRequest();
      request.setCourseCode(""); // violation 1
      request.setCourseName(""); // violation 2
      request.setDurationHours(-1); // violation 3

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasSize(3)))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("courseCode")))
          .andExpect(jsonPath("$.details[1]").value(org.hamcrest.Matchers.containsString("courseName")))
          .andExpect(jsonPath("$.details[2]").value(org.hamcrest.Matchers.containsString("durationHours")));
    }

    @Test
    @DisplayName("LoginRequest - Sensitive password value is never echoed in error response")
    void testLoginRequest_passwordNeverEchoedInError() throws Exception {
      String secretPassword = "SuperSensitivePasswordSecret999!";
      String payload = "{\"usernameOrEmail\":\"\",\"password\":\"" + secretPassword + "\"}";

      MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload))
          .andExpect(status().isBadRequest())
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      assertThat(responseBody).doesNotContain(secretPassword);
    }

    @Test
    @DisplayName("CreateCourseRequest - Missing required fields rejected with VALIDATION_ERROR")
    void testCreateCourseRequest_missingRequiredFieldsRejected() throws Exception {
      CreateCourseRequest request = new CreateCourseRequest();
      request.setCourseCode(null);
      request.setCourseName("");

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("courseCode")))
          .andExpect(jsonPath("$.details[1]").value(org.hamcrest.Matchers.containsString("courseName")));

      // Verify controller business logic was NOT executed
      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("CreateCourseRequest - Field exceeding max length rejected")
    void testCreateCourseRequest_oversizedCourseCodeRejected() throws Exception {
      CreateCourseRequest request = new CreateCourseRequest();
      request.setCourseCode("A".repeat(33)); // Max is 32
      request.setCourseName("Valid Course Name");

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("courseCode")));

      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("CreateCourseRequest - Negative durationHours rejected with PositiveOrZero constraint")
    void testCreateCourseRequest_negativeDurationHoursRejected() throws Exception {
      CreateCourseRequest request = new CreateCourseRequest();
      request.setCourseCode("CRS-VAL-99");
      request.setCourseName("Validation Course");
      request.setDurationHours(-5); // Must be >= 0

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("durationHours")));

      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("CreateCourseRequest - Boundary values: courseCode exactly 32 chars and durationHours = 0 accepted by validator")
    void testCreateCourseRequest_boundaryValuesAccepted() {
      CreateCourseRequest request = new CreateCourseRequest();
      request.setCourseCode("C".repeat(32)); // exactly max length
      request.setCourseName("Valid Boundary Course");
      request.setDurationHours(0); // minimum allowed boundary

      Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateEmployerRequest - Invalid email, phone, and pincode patterns rejected")
    void testCreateEmployerRequest_invalidPatternsRejected() throws Exception {
      CreateEmployerRequest request = new CreateEmployerRequest();
      request.setEmployerCode("EMP-VAL-01");
      request.setEmployerName("Validation Employer");
      request.setContactEmail("invalid-email-address"); // @Email
      request.setContactPhone("123"); // @Pattern 10-15 digits
      request.setPincode("012345"); // @Pattern 6 digits, cannot start with 0

      mockMvc.perform(post("/api/v1/employers")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    @DisplayName("CreateEmployerRequest - Valid pattern boundaries accepted by validator")
    void testCreateEmployerRequest_validPatternBoundariesAccepted() {
      CreateEmployerRequest request = new CreateEmployerRequest();
      request.setEmployerCode("EMP-VAL-02");
      request.setEmployerName("Validation Employer 2");
      request.setContactEmail("valid.contact@domain.gov.in");
      request.setContactPhone("9876543210"); // exactly 10 digits
      request.setPincode("400001"); // valid 6-digit pincode

      Set<ConstraintViolation<CreateEmployerRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateTrainingBatchRequest - Inverted dates rejected by @DateSequence cross-field validator")
    void testCreateTrainingBatchRequest_invertedDatesRejected() throws Exception {
      CreateTrainingBatchRequest request = new CreateTrainingBatchRequest();
      request.setBatchCode("BAT-INV-01");
      request.setStartDate(LocalDate.of(2026, 12, 1));
      request.setEndDate(LocalDate.of(2026, 11, 1)); // endDate before startDate
      request.setCapacity(25);

      mockMvc.perform(post("/api/v1/training-batches")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("endDate")));
    }

    @Test
    @DisplayName("CreateTrainingBatchRequest - Non-positive capacity rejected by @Positive")
    void testCreateTrainingBatchRequest_nonPositiveCapacityRejected() throws Exception {
      CreateTrainingBatchRequest request = new CreateTrainingBatchRequest();
      request.setBatchCode("BAT-CAP-01");
      request.setStartDate(LocalDate.of(2026, 11, 1));
      request.setEndDate(LocalDate.of(2026, 12, 1));
      request.setCapacity(0); // Must be > 0

      mockMvc.perform(post("/api/v1/training-batches")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("capacity")));
    }

    @Test
    @DisplayName("CreateJobPostingRequest - Inverted salary rejected by @NumberRange and dates by @DateSequence")
    void testCreateJobPostingRequest_invertedSalaryAndDatesRejected() throws Exception {
      CreateJobPostingRequest request = new CreateJobPostingRequest();
      request.setPostingCode("JOB-INV-01");
      request.setPostingTitle("Senior Engineer");
      request.setMinSalary(new BigDecimal("75000.00"));
      request.setMaxSalary(new BigDecimal("50000.00")); // max < min
      request.setPostedDate(LocalDate.of(2026, 12, 1));
      request.setClosingDate(LocalDate.of(2026, 11, 1)); // closing < posted

      mockMvc.perform(post("/api/v1/job-postings")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details").isArray())
          .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("CreateTraineeRequest - Underage or future date of birth rejected by @ValidDateOfBirth")
    void testCreateTraineeRequest_invalidDateOfBirthRejected() throws Exception {
      CreateTraineeRequest futureDob = new CreateTraineeRequest();
      futureDob.setRegistrationNumber("REG-TRN-2026-FUT");
      futureDob.setFirstName("Future");
      futureDob.setDateOfBirth(LocalDate.now().plusYears(1)); // future

      mockMvc.perform(post("/api/v1/trainees")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(futureDob)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("dateOfBirth")));
    }
  }

  // =========================================================================
  // 2. PAGINATION BOUNDARY REGRESSION TESTS (Section 2.2)
  // =========================================================================

  @Nested
  @DisplayName("2. Pagination Boundary & Limit Regression Tests")
  class PaginationBoundaryRegressionTests {

    @Test
    @DisplayName("Pagination - Zero page index (first page) is accepted and returns page: 0, first: true")
    void testPagination_firstPageZeroBased() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "5")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.page").value(0))
          .andExpect(jsonPath("$.data.size").value(5))
          .andExpect(jsonPath("$.data.first").value(true))
          .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Pagination - Middle page navigation returns first: false and hasPrevious: true")
    void testPagination_middlePageNavigation() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "1")
              .param("size", "2")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.page").value(1))
          .andExpect(jsonPath("$.data.first").value(false))
          .andExpect(jsonPath("$.data.hasPrevious").value(true));
    }

    @Test
    @DisplayName("Pagination - Page index beyond available range returns empty content array without error")
    void testPagination_outOfBoundsPageReturnsEmptyArray() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "9999")
              .param("size", "20")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.content").isEmpty())
          .andExpect(jsonPath("$.data.page").value(9999))
          .andExpect(jsonPath("$.data.first").value(false))
          .andExpect(jsonPath("$.data.last").value(true))
          .andExpect(jsonPath("$.data.hasNext").value(false))
          .andExpect(jsonPath("$.data.hasPrevious").value(true));
    }

    @Test
    @DisplayName("Pagination - Negative page index is rejected with HTTP 400 INVALID_PAGE")
    void testPagination_negativePageRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "-1")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE"))
          .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    @DisplayName("Pagination - Default page size is applied when size parameter is omitted")
    void testPagination_defaultPageSizeApplied() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.size").value(PaginationUtils.DEFAULT_SIZE));
    }

    @Test
    @DisplayName("Pagination - Minimum allowed page size (size=1) is accepted")
    void testPagination_minimumPageSizeAccepted() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "1")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    @DisplayName("Pagination - Maximum allowed page size (size=100) is accepted")
    void testPagination_maximumPageSizeAccepted() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "100")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("Pagination - Oversized page size (>100) is rejected with HTTP 400 INVALID_PAGE_SIZE")
    void testPagination_oversizedPageSizeRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "101")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
          .andExpect(jsonPath("$.message").value("Page size must not exceed 100"));
    }

    @Test
    @DisplayName("Pagination - Zero page size (size=0) is rejected with HTTP 400 INVALID_PAGE_SIZE")
    void testPagination_zeroPageSizeRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "0")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
          .andExpect(jsonPath("$.message").value("Page size must be at least 1"));
    }

    @Test
    @DisplayName("Pagination - Negative page size is rejected with HTTP 400 INVALID_PAGE_SIZE")
    void testPagination_negativePageSizeRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "-5")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("Pagination - Full 9-field pagination metadata integrity verified")
    void testPagination_fullMetadataIntegrity() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "10")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.page").isNumber())
          .andExpect(jsonPath("$.data.size").isNumber())
          .andExpect(jsonPath("$.data.totalElements").isNumber())
          .andExpect(jsonPath("$.data.totalPages").isNumber())
          .andExpect(jsonPath("$.data.first").isBoolean())
          .andExpect(jsonPath("$.data.last").isBoolean())
          .andExpect(jsonPath("$.data.hasNext").isBoolean())
          .andExpect(jsonPath("$.data.hasPrevious").isBoolean());
    }
  }

  // =========================================================================
  // 3. SORTING BOUNDARY REGRESSION TESTS (Section 2.3)
  // =========================================================================

  @Nested
  @DisplayName("3. Sorting Boundary, Allowlist & Direction Regression Tests")
  class SortingBoundaryRegressionTests {

    @Test
    @DisplayName("Sorting - Default sorting is applied when sort parameter is omitted")
    void testSorting_defaultSortingApplied() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Sorting - Explicit ASC sorting is accepted and executed")
    void testSorting_explicitAscSorting() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("sort", "courseCode")
              .param("direction", "ASC")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Sorting - Explicit DESC sorting is accepted and executed")
    void testSorting_explicitDescSorting() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("sort", "courseCode")
              .param("direction", "DESC")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Sorting - Case-insensitive and snake_case aliases resolve cleanly")
    void testSorting_caseInsensitiveAndAliases() throws Exception {
      for (String sortKey : List.of("coursecode", "COURSECODE", "course_code", "courseCode")) {
        mockMvc.perform(get("/api/v1/courses")
                .param("sort", sortKey)
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
      }
    }

    @Test
    @DisplayName("Sorting - Unsupported sort property is rejected with HTTP 400 INVALID_SORT_FIELD")
    void testSorting_unsupportedSortPropertyRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("sort", "unsupportedPropertyXYZ")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("Sorting - Sensitive sort terms (password, secret, token) are rejected fail-closed without echoing input")
    void testSorting_sensitiveSortFieldsRejectedAntiEcho() throws Exception {
      for (String sensitiveField : List.of("passwordHash", "secret", "token", "aadhaarVaultRef")) {
        MvcResult result = mockMvc.perform(get("/api/v1/courses")
                .param("sort", sensitiveField)
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
            .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
            .andReturn();

        // Anti-echo confirmation: sensitive term must NOT appear in response body
        assertThat(result.getResponse().getContentAsString()).doesNotContain(sensitiveField);
      }
    }

    @Test
    @DisplayName("Sorting - Unsupported sort direction is rejected with HTTP 400 INVALID_SORT_DIRECTION")
    void testSorting_unsupportedDirectionRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("direction", "SIDEWAYS")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"));
    }

    @Test
    @DisplayName("Sorting - Malformed SQL injection sort expressions are rejected fail-closed")
    void testSorting_sqlInjectionSortRejected() throws Exception {
      for (String maliciousSort : List.of("id;DROP TABLE courses", "id' OR '1'='1", "id,asc")) {
        mockMvc.perform(get("/api/v1/courses")
                .param("sort", maliciousSort)
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
      }
    }

    @Test
    @DisplayName("Sorting - Deterministic tie-breaker stability: secondary sort by id is applied")
    void testSorting_tieBreakerStability() {
      Pageable pageable = PaginationUtils.buildPageable(
          0, 10, "courseName", "ASC", PaginationUtils.COURSE_SORT_FIELDS, "id", Sort.Direction.ASC
      );

      Sort sort = pageable.getSort();
      assertThat(sort.getOrderFor("courseName")).isNotNull();
      assertThat(sort.getOrderFor("courseName").getDirection()).isEqualTo(Sort.Direction.ASC);

      // Verify secondary id tie-breaker
      assertThat(sort.getOrderFor("id")).isNotNull();
      assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }
  }

  // =========================================================================
  // 4. MALFORMED REQUEST BODY & TYPE CONVERSION TESTS (Section 2.4)
  // =========================================================================

  @Nested
  @DisplayName("4. Malformed Input, Body & Type Conversion Regression Tests")
  class MalformedInputAndTypeConversionTests {

    @Test
    @DisplayName("Malformed JSON - Broken syntax (missing closing brace) returns HTTP 400 MALFORMED_JSON")
    void testMalformedJson_syntaxErrorReturnsMalformedJson() throws Exception {
      String brokenJson = "{\"courseCode\": \"CRS-01\", \"courseName\": \"Broken JSON\"";

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(brokenJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));

      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("Empty Request Body - Empty string with application/json returns HTTP 400 MALFORMED_JSON")
    void testEmptyRequestBody_returnsMalformedJson() throws Exception {
      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(""))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("body")));

      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("Type Conversion Error - String supplied where integer expected in JSON returns HTTP 400 MALFORMED_JSON")
    void testTypeConversion_stringWhereNumberExpectedReturnsMalformedJson() throws Exception {
      String invalidTypeJson = "{\"courseCode\":\"CRS-99\",\"courseName\":\"Valid Name\",\"durationHours\":\"not-a-number\"}";

      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidTypeJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

      verify(courseService, never()).createCourse(any());
    }

    @Test
    @DisplayName("Type Conversion Error - Non-numeric path variable returns HTTP 400 TYPE_MISMATCH")
    void testTypeConversion_nonNumericPathVariable() throws Exception {
      mockMvc.perform(get("/api/v1/courses/not-a-number")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid parameter value for 'id'")));
    }

    @Test
    @DisplayName("Path Variable Constraint - Non-positive path variable returns HTTP 400 VALIDATION_ERROR")
    void testPathVariableConstraint_negativeIdRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses/-1")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Query Parameter Type Mismatch - Non-integer page parameter returns HTTP 400 TYPE_MISMATCH")
    void testQueryParamTypeMismatch_nonIntegerPage() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "abc")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("page")));
    }

    @Test
    @DisplayName("Query Parameter Type Mismatch - Non-integer size parameter returns HTTP 400 TYPE_MISMATCH")
    void testQueryParamTypeMismatch_nonIntegerSize() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "xyz")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("size")));
    }

    @Test
    @DisplayName("Security & Privacy - Error responses never leak stack traces or internal class names")
    void testSecurityPrivacy_noStackTraceOrInternalDetailsLeaked() throws Exception {
      MvcResult result = mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"durationHours\": \"not-a-number\"}"))
          .andExpect(status().isBadRequest())
          .andReturn();

      String responseBody = result.getResponse().getContentAsString();
      assertThat(responseBody).doesNotContain("at in.gov.sih");
      assertThat(responseBody).doesNotContain("org.springframework");
      assertThat(responseBody).doesNotContain("com.fasterxml.jackson");
      assertThat(responseBody).doesNotContain("NullPointerException");
      assertThat(responseBody).doesNotContain("SELECT ");
      assertThat(responseBody).doesNotContain("INSERT ");
    }
  }

  // =========================================================================
  // 5. API RESPONSE CONTRACT VERIFICATION (Section 2.5)
  // =========================================================================

  @Nested
  @DisplayName("5. API Response Envelope Contract Verification")
  class ApiResponseContractVerificationTests {

    @Test
    @DisplayName("Contract - Error responses contain status, error, code, message, path, timestamp, correlationId")
    void testErrorResponseEnvelopeContract() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "-1")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(header().exists("X-Correlation-Id"))
          .andExpect(header().exists("X-Request-Id"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE"))
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.path").value("/api/v1/courses"))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.correlationId").isString());
    }

    @Test
    @DisplayName("Contract - Paginated success responses contain ApiResponse envelope with PagedResponse data")
    void testPaginatedSuccessEnvelopeContract() throws Exception {
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
          .andExpect(jsonPath("$.correlationId").isString())
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
  }
}
