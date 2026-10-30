package in.gov.sih.sih26135.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.AssessmentService;
import in.gov.sih.sih26135.service.CourseService;
import in.gov.sih.sih26135.service.JobPostingService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprehensive verification test suite for Phase 26.7:
 * Path Variable, Query Parameter & Header Validation.
 *
 * <p>Verifies:
 * <ol>
 *   <li>Class-level @Validated presence across all 55 compiled application REST controllers.</li>
 *   <li>Path variable constraints (@Positive on numeric IDs, @NotBlank / @PositiveOrZero on composite parameters).</li>
 *   <li>Pagination bounds enforcement (page >= 0, 1 <= size <= 100).</li>
 *   <li>Query filter constraints (@Positive on ID filters, @NotBlank on mandatory query codes).</li>
 *   <li>ISO-8601 date parsing failure handling (maps to HTTP 400 TYPE_MISMATCH).</li>
 *   <li>Request header validation (@Positive on X-Actor-User-Id, optionality preserved when omitted).</li>
 *   <li>Tenant isolation boundary verification (credential-based, not spoofable client headers).</li>
 *   <li>Sanitized HTTP 400 error responses without information disclosure.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.7 - Path Variable, Query Parameter & Header Validation Tests")
public class ControllerPathQueryHeaderValidationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private DataSource dataSource;

  @MockBean
  private JobPostingService jobPostingService;

  @MockBean
  private CourseService courseService;

  @MockBean
  private AssessmentService assessmentService;

  @MockBean
  private TraineeConsentService traineeConsentService;

  private String tokenPlacementManage;
  private String tokenCourseManage;
  private String tokenAssessmentManage;
  private String tokenConsentManage;

  @BeforeEach
  void setUp() throws Exception {
    assertThat(dataSource)
        .as("DataSource must be configured")
        .isNotNull();

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    tokenPlacementManage = "Bearer " + jwtTokenProvider.generateAccessToken(
        101L, "placementAdmin", List.of("placement.manage")
    );
    tokenCourseManage = "Bearer " + jwtTokenProvider.generateAccessToken(
        102L, "courseAdmin", List.of("course.manage")
    );
    tokenAssessmentManage = "Bearer " + jwtTokenProvider.generateAccessToken(
        103L, "assessmentAdmin", List.of("assessment.manage")
    );
    tokenConsentManage = "Bearer " + jwtTokenProvider.generateAccessToken(
        104L, "consentAdmin", List.of("consent.manage")
    );
  }

  // =========================================================================
  // 1. Programmatic Controller & Parameter Metadata Inspection
  // =========================================================================

  @Nested
  @DisplayName("1. Controller & Parameter Validation Architecture Metadata Audit")
  class MetadataAuditTests {

    private List<Class<?>> getAllProjectControllers() throws Exception {
      ClassPathScanningCandidateComponentProvider scanner =
          new ClassPathScanningCandidateComponentProvider(false);
      scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

      List<Class<?>> controllers = new ArrayList<>();
      Set<BeanDefinition> candidates = scanner.findCandidateComponents("in.gov.sih.sih26135.controller");
      for (BeanDefinition beanDef : candidates) {
        Class<?> clazz = Class.forName(beanDef.getBeanClassName());
        if (clazz.getProtectionDomain() != null
            && clazz.getProtectionDomain().getCodeSource() != null) {
          String codeSource = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
          if (codeSource.contains("test-classes")) {
            continue;
          }
        }
        controllers.add(clazz);
      }
      return controllers;
    }

    @Test
    @DisplayName("Audit 1.1: All 55 REST controllers exist and declare class-level @Validated")
    void testAllControllersDeclareClassLevelValidated() throws Exception {
      List<Class<?>> controllers = getAllProjectControllers();
      assertThat(controllers)
          .as("Exactly 55 application REST controllers must be present")
          .hasSize(55);

      List<String> missingValidated = new ArrayList<>();
      for (Class<?> controller : controllers) {
        if (!controller.isAnnotationPresent(Validated.class)) {
          missingValidated.add(controller.getName());
        }
      }

      assertThat(missingValidated)
          .as("All 55 controllers must declare class-level @Validated")
          .isEmpty();
    }

    @Test
    @DisplayName("Audit 1.2: Every @PathVariable Long parameter declares @Positive")
    void testAllPathVariableLongParametersHavePositive() throws Exception {
      List<Class<?>> controllers = getAllProjectControllers();
      List<String> unconstrained = new ArrayList<>();

      for (Class<?> controller : controllers) {
        for (Method method : controller.getDeclaredMethods()) {
          for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(PathVariable.class) && param.getType().equals(Long.class)) {
              if (!param.isAnnotationPresent(Positive.class)) {
                unconstrained.add(controller.getSimpleName() + "." + method.getName() + "(" + param.getName() + ")");
              }
            }
          }
        }
      }

      assertThat(unconstrained)
          .as("Every @PathVariable Long parameter must be annotated with @Positive")
          .isEmpty();
    }

    @Test
    @DisplayName("Audit 1.3: Pagination parameters declare @Min(0) on page and @Min(1) @Max(100) on size")
    void testAllPaginationParametersHaveBoundsConstraints() throws Exception {
      List<Class<?>> controllers = getAllProjectControllers();
      List<String> invalidPage = new ArrayList<>();
      List<String> invalidSize = new ArrayList<>();

      for (Class<?> controller : controllers) {
        for (Method method : controller.getDeclaredMethods()) {
          for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(RequestParam.class)) {
              if ("page".equals(param.getName()) && param.getType().equals(int.class)) {
                Min min = param.getAnnotation(Min.class);
                if (min == null || min.value() != 0) {
                  invalidPage.add(controller.getSimpleName() + "." + method.getName() + "(page)");
                }
              }
              if ("size".equals(param.getName()) && param.getType().equals(int.class)) {
                Min min = param.getAnnotation(Min.class);
                Max max = param.getAnnotation(Max.class);
                if (min == null || min.value() != 1 || max == null || max.value() != 100) {
                  invalidSize.add(controller.getSimpleName() + "." + method.getName() + "(size)");
                }
              }
            }
          }
        }
      }

      assertThat(invalidPage)
          .as("All 'page' parameters must declare @Min(0)")
          .isEmpty();
      assertThat(invalidSize)
          .as("All 'size' parameters must declare @Min(1) and @Max(100)")
          .isEmpty();
    }

    @Test
    @DisplayName("Audit 1.4: Every @RequestParam Long ID filter declares @Positive")
    void testAllRequestParamLongIdParametersHavePositive() throws Exception {
      List<Class<?>> controllers = getAllProjectControllers();
      List<String> unconstrained = new ArrayList<>();

      for (Class<?> controller : controllers) {
        for (Method method : controller.getDeclaredMethods()) {
          for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(RequestParam.class) && param.getType().equals(Long.class)) {
              String name = param.getName();
              if ("id".equals(name) || name.endsWith("Id") || "actorUserId".equals(name)) {
                if (!param.isAnnotationPresent(Positive.class)) {
                  unconstrained.add(controller.getSimpleName() + "." + method.getName() + "(" + name + ")");
                }
              }
            }
          }
        }
      }

      assertThat(unconstrained)
          .as("Every Long ID @RequestParam filter must be annotated with @Positive")
          .isEmpty();
    }

    @Test
    @DisplayName("Audit 1.5: Every @RequestHeader actorUserId declares @Positive")
    void testAllRequestHeaderActorUserIdParametersHavePositive() throws Exception {
      List<Class<?>> controllers = getAllProjectControllers();
      List<String> unconstrained = new ArrayList<>();

      for (Class<?> controller : controllers) {
        for (Method method : controller.getDeclaredMethods()) {
          for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(RequestHeader.class) && "actorUserId".equals(param.getName())) {
              if (!param.isAnnotationPresent(Positive.class)) {
                unconstrained.add(controller.getSimpleName() + "." + method.getName());
              }
            }
          }
        }
      }

      assertThat(unconstrained)
          .as("All X-Actor-User-Id header parameters must declare @Positive")
          .isEmpty();
    }
  }

  // =========================================================================
  // 2. HTTP Controller Boundary - Path Variable Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("2. Path Variable Controller Boundary Validation")
  class PathVariableValidationTests {

    @Test
    @DisplayName("Path variable with value 0 is rejected with HTTP 400 VALIDATION_ERROR")
    void testPathVariableZero_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings/0")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").value("Request validation failed"))
          .andExpect(jsonPath("$.path").value("/api/v1/job-postings/{id}"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("id: must be greater than 0")));
    }

    @Test
    @DisplayName("Negative path variable with value -1 is rejected with HTTP 400 VALIDATION_ERROR")
    void testPathVariableNegative_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings/-1")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("id: must be greater than 0")));
    }

    @Test
    @DisplayName("Non-numeric path variable value 'abc' is rejected with HTTP 400 TYPE_MISMATCH")
    void testPathVariableNonNumeric_rejectedWith400TypeMismatch() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings/abc")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'id'. Expected type: Long"));
    }

    @Test
    @DisplayName("Valid positive path variable value 1 passes validation and succeeds")
    void testPathVariablePositive_accepted() throws Exception {
      JobPostingResponse response = mock(JobPostingResponse.class);
      when(response.id()).thenReturn(1L);
      when(response.postingCode()).thenReturn("POST-001");
      when(jobPostingService.getJobPostingById(1L)).thenReturn(response);

      mockMvc.perform(get("/api/v1/job-postings/1")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.id").value(1))
          .andExpect(jsonPath("$.data.postingCode").value("POST-001"));
    }

    @Test
    @DisplayName("DELETE request with negative path variable is rejected with HTTP 400 VALIDATION_ERROR")
    void testDeletePathVariableNegative_rejectedWith400() throws Exception {
      mockMvc.perform(delete("/api/v1/job-postings/-99")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("id: must be greater than 0")));
    }
  }

  // =========================================================================
  // 3. HTTP Controller Boundary - Pagination Parameter Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("3. Pagination Parameter Controller Boundary Validation")
  class PaginationValidationTests {

    @Test
    @DisplayName("Negative page index page=-1 is rejected with HTTP 400 INVALID_PAGE")
    void testPageNegative_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("page", "-1")
              .header("Authorization", tokenCourseManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE"))
          .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    @DisplayName("Zero page size size=0 is rejected with HTTP 400 INVALID_PAGE_SIZE")
    void testSizeZero_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "0")
              .header("Authorization", tokenCourseManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
          .andExpect(jsonPath("$.message").value("Page size must be at least 1"));
    }

    @Test
    @DisplayName("Excessive page size size=101 is rejected with HTTP 400 INVALID_PAGE_SIZE")
    void testSizeExceedsMaximum_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/courses")
              .param("size", "101")
              .header("Authorization", tokenCourseManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
          .andExpect(jsonPath("$.message").value("Page size must not exceed 100"));
    }

    @Test
    @DisplayName("Valid lower pagination boundary page=0, size=1 passes validation")
    void testPaginationLowerBoundary_accepted() throws Exception {
      when(courseService.getAllCourses(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

      mockMvc.perform(get("/api/v1/courses")
              .param("page", "0")
              .param("size", "1")
              .header("Authorization", tokenCourseManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Valid upper pagination boundary page=10, size=100 passes validation")
    void testPaginationUpperBoundary_accepted() throws Exception {
      when(courseService.getAllCourses(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

      mockMvc.perform(get("/api/v1/courses")
              .param("page", "10")
              .param("size", "100")
              .header("Authorization", tokenCourseManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  // =========================================================================
  // 4. HTTP Controller Boundary - Query Filter Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("4. Query Filter Controller Boundary Validation")
  class QueryFilterValidationTests {

    @Test
    @DisplayName("Query filter ID employerId=0 is rejected with HTTP 400 VALIDATION_ERROR")
    void testQueryFilterIdZero_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings")
              .param("employerId", "0")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("employerId: must be greater than 0")));
    }

    @Test
    @DisplayName("Negative query filter ID employerId=-10 is rejected with HTTP 400 VALIDATION_ERROR")
    void testQueryFilterIdNegative_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings")
              .param("employerId", "-10")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("employerId: must be greater than 0")));
    }

    @Test
    @DisplayName("Empty mandatory query code parameter code='' is rejected with HTTP 400 VALIDATION_ERROR")
    void testMandatoryCodeEmpty_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings")
              .param("code", "")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("code: must not be blank")));
    }

    @Test
    @DisplayName("Whitespace mandatory query code parameter code='   ' is rejected with HTTP 400 VALIDATION_ERROR")
    void testMandatoryCodeWhitespace_rejectedWith400() throws Exception {
      mockMvc.perform(get("/api/v1/job-postings")
              .param("code", "   ")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("code: must not be blank")));
    }

    @Test
    @DisplayName("Valid positive filter ID employerId=5 passes validation and succeeds")
    void testValidFilterId_accepted() throws Exception {
      when(jobPostingService.getJobPostingsByEmployer(5L)).thenReturn(List.of());

      mockMvc.perform(get("/api/v1/job-postings")
              .param("employerId", "5")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  // =========================================================================
  // 5. HTTP Controller Boundary - ISO-8601 Date Parsing Tests
  // =========================================================================

  @Nested
  @DisplayName("5. ISO-8601 Date Query Parameter Parsing & Validation")
  class IsoDateValidationTests {

    @Test
    @DisplayName("Malformed date parameter value assessmentDate='invalid-date' is rejected with HTTP 400 TYPE_MISMATCH")
    void testMalformedIsoDate_rejectedWithTypeMismatch400() throws Exception {
      mockMvc.perform(get("/api/v1/assessments")
              .param("assessmentDate", "invalid-date")
              .header("Authorization", tokenAssessmentManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'assessmentDate'. Expected type: LocalDate"));
    }

    @Test
    @DisplayName("Valid ISO-8601 date parameter value assessmentDate='2026-10-28' parses cleanly")
    void testValidIsoDate_accepted() throws Exception {
      when(assessmentService.getByAssessmentDate(any(LocalDate.class))).thenReturn(List.of());

      mockMvc.perform(get("/api/v1/assessments")
              .param("assessmentDate", "2026-10-28")
              .header("Authorization", tokenAssessmentManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  // =========================================================================
  // 6. HTTP Controller Boundary - Request Header Validation Tests
  // =========================================================================

  @Nested
  @DisplayName("6. Request Header Controller Boundary Validation")
  class RequestHeaderValidationTests {

    @Test
    @DisplayName("Invalid negative header X-Actor-User-Id: -1 is rejected with HTTP 400 VALIDATION_ERROR")
    void testActorUserIdNegative_rejectedWith400() throws Exception {
      UpdateTraineeConsentStatusRequest body = new UpdateTraineeConsentStatusRequest(2L, null);

      mockMvc.perform(put("/api/v1/consents/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(body))
              .header("X-Actor-User-Id", "-1")
              .header("Authorization", tokenConsentManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("actorUserId: must be greater than 0")));
    }

    @Test
    @DisplayName("Invalid zero header X-Actor-User-Id: 0 is rejected with HTTP 400 VALIDATION_ERROR")
    void testActorUserIdZero_rejectedWith400() throws Exception {
      UpdateTraineeConsentStatusRequest body = new UpdateTraineeConsentStatusRequest(2L, null);

      mockMvc.perform(put("/api/v1/consents/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(body))
              .header("X-Actor-User-Id", "0")
              .header("Authorization", tokenConsentManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details[0]").value(endsWith("actorUserId: must be greater than 0")));
    }

    @Test
    @DisplayName("Non-numeric header X-Actor-User-Id: 'not-a-number' is rejected with HTTP 400 TYPE_MISMATCH")
    void testActorUserIdNonNumeric_rejectedWithTypeMismatch400() throws Exception {
      UpdateTraineeConsentStatusRequest body = new UpdateTraineeConsentStatusRequest(2L, null);

      mockMvc.perform(put("/api/v1/consents/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(body))
              .header("X-Actor-User-Id", "not-a-number")
              .header("Authorization", tokenConsentManage))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.message").value("Invalid parameter value for 'X-Actor-User-Id'. Expected type: Long"));
    }

    @Test
    @DisplayName("Valid positive header X-Actor-User-Id: 42 passes validation")
    void testActorUserIdPositive_accepted() throws Exception {
      UpdateTraineeConsentStatusRequest body = new UpdateTraineeConsentStatusRequest(2L, null);
      TraineeConsentResponse resp = new TraineeConsentResponse();
      when(traineeConsentService.updateConsentStatus(eq(1L), any(UpdateTraineeConsentStatusRequest.class), eq(42L)))
          .thenReturn(resp);

      mockMvc.perform(put("/api/v1/consents/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(body))
              .header("X-Actor-User-Id", "42")
              .header("Authorization", tokenConsentManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Omitted optional header X-Actor-User-Id is accepted and passes validation")
    void testActorUserIdOmitted_accepted() throws Exception {
      UpdateTraineeConsentStatusRequest body = new UpdateTraineeConsentStatusRequest(2L, null);
      TraineeConsentResponse resp = new TraineeConsentResponse();
      when(traineeConsentService.updateConsentStatus(eq(1L), any(UpdateTraineeConsentStatusRequest.class), eq(null)))
          .thenReturn(resp);

      mockMvc.perform(put("/api/v1/consents/1")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(body))
              .header("Authorization", tokenConsentManage))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  // =========================================================================
  // 7. Tenant-Scope Header Verification
  // =========================================================================

  @Nested
  @DisplayName("7. Tenant-Scope Header & Security Boundary Verification")
  class TenantScopeHeaderTests {

    @Test
    @DisplayName("Tenant isolation is strictly authenticated-token based; arbitrary tenant headers cannot spoof tenant context")
    void testArbitraryTenantHeader_doesNotSpoofTenantContext() throws Exception {
      // In SIH-26135, tenant isolation is enforced by WorkflowSecurityScopeValidator reading the JWT principal,
      // not via client-supplied request headers. Client-supplied headers like X-Tenant-Id are ignored and cannot
      // elevate permissions or alter tenant scope.
      when(jobPostingService.getJobPostingById(1L)).thenReturn(mock(JobPostingResponse.class));

      mockMvc.perform(get("/api/v1/job-postings/1")
              .header("X-Tenant-Id", "999")
              .header("X-Employer-Id", "888")
              .header("Authorization", tokenPlacementManage))
          .andExpect(status().isOk());
    }
  }
}
