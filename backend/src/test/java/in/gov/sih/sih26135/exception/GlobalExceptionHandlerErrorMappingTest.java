package in.gov.sih.sih26135.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.external.vendor.controller.UnverifiedThirdPartyVendorController;
import in.gov.sih.sih26135.controller.analytics.AnalyticsSummaryController;
import in.gov.sih.sih26135.controller.auth.AuthController;
import in.gov.sih.sih26135.controller.unverified.UnverifiedBypassController;
import in.gov.sih.sih26135.controller.SpoofedWhitelistedPackageController;
import in.gov.sih.sih26135.controller.workflow.VerificationWorkflowController;
import in.gov.sih.sih26135.dto.auth.SpoofedAuthRequest;
import in.gov.sih.sih26135.dto.request.SpoofedWhitelistedPackageRequestDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Complete Error Mapping & Security Hardening Tests")
class GlobalExceptionHandlerErrorMappingTest {

  private GlobalExceptionHandler exceptionHandler;

  @Mock
  private HttpServletRequest request;

  /**
   * Dummy project class outside verified DTO packages (in in.gov.sih.sih26135.exception)
   * used to verify that ending with "Request" does not erroneously classify as client request DTO.
   */
  static class ArbitraryProjectRequest {
    private String customField;
  }

  /**
   * Unverified class annotated with @RestController outside in.gov.sih.sih26135.controller.* namespace
   * used to verify fail-closed HTTP 500 behavior.
   */
  @RestController
  static class ExternalRestControllerMock {
    public void execute(String param) {}
  }

  /**
   * Unverified class annotated with @Controller outside in.gov.sih.sih26135.controller.* namespace
   * used to verify fail-closed HTTP 500 behavior.
   */
  @Controller
  static class ExternalControllerMock {
    public void execute(String param) {}
  }

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    org.mockito.Mockito.lenient().when(request.getRequestURI()).thenReturn("/api/v1/test-endpoint");
  }

  // =========================================================================
  // 1. Client-Side Web Binding and Validation Exceptions (HTTP 400 / 405 / 406 / 415)
  // =========================================================================

  @Nested
  @DisplayName("1. Client Parameter & Validation Error Mapping")
  class ClientParameterValidationMappingTests {

    @Test
    @DisplayName("MethodArgumentTypeMismatchException maps to HTTP 400 TYPE_MISMATCH")
    void testMethodArgumentTypeMismatch_mapsTo400() {
      MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
          "invalid-id", Long.class, "traineeId", mock(MethodParameter.class), new NumberFormatException());

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getError()).isEqualTo("Bad Request");
      assertThat(response.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
      assertThat(response.getBody().getMessage()).contains("traineeId").contains("Long");
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test-endpoint");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException maps to HTTP 400 MISSING_PARAMETER")
    void testMissingServletRequestParameter_mapsTo400() {
      MissingServletRequestParameterException ex =
          new MissingServletRequestParameterException("status", "String");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingServletRequestParameter(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("MISSING_PARAMETER");
      assertThat(response.getBody().getMessage()).contains("status");
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test-endpoint");
    }

    @Test
    @DisplayName("MissingPathVariableException maps to HTTP 500 INTERNAL_SERVER_ERROR without leaking internal variable name")
    void testMissingPathVariable_mapsTo500InternalServerError() {
      MissingPathVariableException ex = mock(MissingPathVariableException.class);
      when(ex.getVariableName()).thenReturn("enrollmentId");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingPathVariable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test-endpoint");
      // Strictly zero disclosure of internal controller variable name
      assertThat(response.getBody().getMessage()).doesNotContain("enrollmentId");
    }

    @Test
    @DisplayName("ConstraintViolationException from request/method parameter maps to HTTP 400 VALIDATION_ERROR with details")
    void testConstraintViolation_fromMethodParameter_mapsTo400WithDetails() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      when(path.toString()).thenReturn("traineeId");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be greater than 0");
      doReturn(in.gov.sih.sih26135.controller.TraineeController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      assertThat(response.getBody().getDetails()).containsExactly("traineeId: must be greater than 0");
    }

    @Test
    @DisplayName("ConstraintViolationException with unknown root bean class and parameter node fails closed to HTTP 500")
    void testConstraintViolation_unknownRootBeanWithParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("unknownParam");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must not be null");
      // Root bean class is an unknown third-party/helper class outside supported controllers/DTOs
      doReturn(Object.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException with null root bean class and parameter node fails closed to HTTP 500")
    void testConstraintViolation_nullRootBeanWithParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("traineeId");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must be greater than 0");
      // Explicitly null root bean class
      doReturn(null).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from controller without parameter node fails closed to HTTP 500")
    void testConstraintViolation_controllerWithoutParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node propNode = mock(Path.Node.class);
      when(propNode.getKind()).thenReturn(jakarta.validation.ElementKind.PROPERTY);
      when(path.iterator()).thenReturn(Collections.singletonList(propNode).iterator());
      when(violation.getPropertyPath()).thenReturn(path);
      doReturn(in.gov.sih.sih26135.controller.TraineeController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from external class annotated with @RestController with parameter node fails closed to HTTP 500")
    void testConstraintViolation_fromExternalRestControllerWithParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("param");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must not be blank");
      // External class annotated with @RestController, outside in.gov.sih.sih26135.controller.*
      doReturn(ExternalRestControllerMock.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("ExternalRestControllerMock");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from external class annotated with @Controller with parameter node fails closed to HTTP 500")
    void testConstraintViolation_fromExternalControllerWithParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("param");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must not be blank");
      // External class annotated with @Controller, outside in.gov.sih.sih26135.controller.*
      doReturn(ExternalControllerMock.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("ExternalControllerMock");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from unverified third-party vendor controller fails closed to HTTP 500")
    void testConstraintViolation_fromUnverifiedThirdPartyVendorController_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("vendorId");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("vendor secret token leaked");
      doReturn(UnverifiedThirdPartyVendorController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("UnverifiedThirdPartyVendorController");
      assertThat(response.getBody().getMessage()).doesNotContain("vendor secret token leaked");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from unverified controller attempting subpackage bypass fails closed to HTTP 500")
    void testConstraintViolation_fromUnverifiedBypassController_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("bypassId");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must not be empty");
      doReturn(UnverifiedBypassController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("UnverifiedBypassController");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from controller spoofing exact whitelisted package fails closed to HTTP 500 without leaking secrets")
    void testConstraintViolation_fromSpoofedWhitelistedPackageController_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("spoofedParam");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("Validation failed with secret " + SpoofedWhitelistedPackageController.SENSITIVE_TOKEN);
      doReturn(SpoofedWhitelistedPackageController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Strictly zero leakage of spoofed class name or embedded sensitive token
      assertThat(response.getBody().getMessage()).doesNotContain("SpoofedWhitelistedPackageController");
      assertThat(response.getBody().getMessage()).doesNotContain(SpoofedWhitelistedPackageController.SENSITIVE_TOKEN);
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from verified analytics controller maps to HTTP 400 VALIDATION_ERROR")
    void testConstraintViolation_fromAnalyticsController_mapsTo400() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      when(path.toString()).thenReturn("filterDate");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be a valid ISO date");
      doReturn(AnalyticsSummaryController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("filterDate: must be a valid ISO date");
    }

    @Test
    @DisplayName("ConstraintViolationException from verified auth controller maps to HTTP 400 VALIDATION_ERROR")
    void testConstraintViolation_fromAuthController_mapsTo400() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      when(path.toString()).thenReturn("loginHint");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must not be blank");
      doReturn(AuthController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("loginHint: must not be blank");
    }

    @Test
    @DisplayName("ConstraintViolationException from verified workflow controller maps to HTTP 400 VALIDATION_ERROR")
    void testConstraintViolation_fromWorkflowController_mapsTo400() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      when(path.toString()).thenReturn("requestId");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be greater than 0");
      doReturn(VerificationWorkflowController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("requestId: must be greater than 0");
    }

    @Test
    @DisplayName("ConstraintViolationException from request DTO maps to HTTP 400 VALIDATION_ERROR with details")
    void testConstraintViolation_fromRequestDto_mapsTo400WithDetails() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("email");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be a valid email");
      doReturn(in.gov.sih.sih26135.dto.request.CreateTraineeRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("email: must be a valid email");
    }

    @Test
    @DisplayName("ConstraintViolationException from auth request DTO in verified package maps to HTTP 400 VALIDATION_ERROR with details")
    void testConstraintViolation_fromAuthRequestDto_mapsTo400WithDetails() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("username");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must not be blank");
      doReturn(in.gov.sih.sih26135.dto.auth.LoginRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("username: must not be blank");
    }

    @Test
    @DisplayName("ConstraintViolationException from DTO spoofing exact whitelisted package in.gov.sih.sih26135.dto.request fails closed to HTTP 500")
    void testConstraintViolation_fromSpoofedWhitelistedPackageRequestDto_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("spoofedPayload");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("invalid spoofed payload");
      doReturn(SpoofedWhitelistedPackageRequestDto.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Strictly zero disclosure of spoofed DTO class name
      assertThat(response.getBody().getMessage()).doesNotContain("SpoofedWhitelistedPackageRequestDto");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from unverified DTO in auth package in.gov.sih.sih26135.dto.auth fails closed to HTTP 500")
    void testConstraintViolation_fromSpoofedAuthRequest_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("spoofedToken");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("invalid spoofed token");
      doReturn(SpoofedAuthRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Strictly zero disclosure of spoofed auth DTO class name
      assertThat(response.getBody().getMessage()).doesNotContain("SpoofedAuthRequest");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from arbitrary project class ending with Request outside verified packages fails closed to HTTP 500")
    void testConstraintViolation_fromArbitraryProjectClassEndingWithRequest_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      org.mockito.Mockito.lenient().when(path.toString()).thenReturn("customField");
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.lenient().when(violation.getMessage()).thenReturn("must not be null");
      // Project class ending with "Request" but located in in.gov.sih.sih26135.exception, NOT in verified DTO packages
      doReturn(ArbitraryProjectRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Zero disclosure of internal class name or property
      assertThat(response.getBody().getMessage()).doesNotContain("ArbitraryProjectRequest");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from entity/persistence validation maps to sanitized HTTP 500 without leaking entity internals")
    void testConstraintViolation_fromEntityPersistence_mapsToSanitized500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      doReturn(in.gov.sih.sih26135.entity.Trainee.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Persistence validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Strictly zero exposure of entity or property names
      assertThat(response.getBody().getMessage()).doesNotContain("Trainee");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from internal service layer maps to sanitized HTTP 500")
    void testConstraintViolation_fromInternalService_mapsToSanitized500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      doReturn(in.gov.sih.sih26135.service.impl.UserServiceImpl.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Internal service validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("UserServiceImpl");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException from internal service with parameter node fails closed to HTTP 500")
    void testConstraintViolation_fromInternalServiceWithParamNode_failsClosedTo500() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      org.mockito.Mockito.lenient().when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      org.mockito.Mockito.lenient().when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      org.mockito.Mockito.lenient().when(violation.getPropertyPath()).thenReturn(path);
      doReturn(in.gov.sih.sih26135.service.impl.UserServiceImpl.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Internal service validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("UserServiceImpl");
      assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException with empty/unknown violations fails closed to HTTP 500")
    void testConstraintViolation_emptyViolations_failsClosedTo500() {
      ConstraintViolationException ex = new ConstraintViolationException("Unknown validation failed", Collections.emptySet());

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException maps to HTTP 400 VALIDATION_ERROR with field details")
    void testMethodArgumentNotValid_mapsTo400() {
      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError("request", "email", "must be a well-formed email address");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("email: must be a well-formed email address");
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException maps to HTTP 415 UNSUPPORTED_MEDIA_TYPE")
    void testHttpMediaTypeNotSupported_mapsTo415() {
      HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
          MediaType.TEXT_PLAIN, Collections.singletonList(MediaType.APPLICATION_JSON));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMediaTypeNotSupported(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
      assertThat(response.getBody().getMessage()).contains("text/plain");
    }

    @Test
    @DisplayName("HttpMediaTypeNotAcceptableException maps to HTTP 406 NOT_ACCEPTABLE")
    void testHttpMediaTypeNotAcceptable_mapsTo406() {
      HttpMediaTypeNotAcceptableException ex = new HttpMediaTypeNotAcceptableException("Not acceptable");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMediaTypeNotAcceptable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("NOT_ACCEPTABLE");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException maps to HTTP 405 METHOD_NOT_ALLOWED")
    void testHttpRequestMethodNotSupported_mapsTo405() {
      HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("METHOD_NOT_ALLOWED");
      assertThat(response.getBody().getMessage()).contains("DELETE");
    }

    @Test
    @DisplayName("NoResourceFoundException maps to HTTP 404 ENDPOINT_NOT_FOUND")
    void testNoResourceFound_mapsTo404() {
      NoResourceFoundException ex = mock(NoResourceFoundException.class);
      when(ex.getResourcePath()).thenReturn("/unknown/route");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ENDPOINT_NOT_FOUND");
    }
  }

  // =========================================================================
  // 2. BaseException Subclasses Mapping (HTTP 400, 401, 404, 409)
  // =========================================================================

  @Nested
  @DisplayName("2. Application Domain BaseException Mapping")
  class BaseExceptionMappingTests {

    @Test
    @DisplayName("BadRequestException maps to HTTP 400 with custom code")
    void testBadRequestException_mapsTo400() {
      BadRequestException ex = new BadRequestException("Invalid date range", "INVALID_DATE_RANGE");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_DATE_RANGE");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid date range");
    }

    @Test
    @DisplayName("ConflictException maps to HTTP 409 with custom code")
    void testConflictException_mapsTo409() {
      ConflictException ex = new ConflictException("Enrollment already exists", "DUPLICATE_ENROLLMENT");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ENROLLMENT");
      assertThat(response.getBody().getMessage()).isEqualTo("Enrollment already exists");
    }

    @Test
    @DisplayName("ResourceNotFoundException maps to HTTP 404")
    void testResourceNotFoundException_mapsTo404() {
      ResourceNotFoundException ex = new ResourceNotFoundException("Trainee", "id");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
      assertThat(response.getBody().getMessage()).contains("Trainee not found");
    }

    @Test
    @DisplayName("InvalidCredentialsException maps to HTTP 401 UNAUTHORIZED")
    void testInvalidCredentialsException_mapsTo401() {
      InvalidCredentialsException ex = new InvalidCredentialsException("Invalid token signature");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
    }
  }

  // =========================================================================
  // 3. Security Filter Exception Re-throwing (AccessDenied & Authentication)
  // =========================================================================

  @Nested
  @DisplayName("3. Security Exceptions Re-thrown for Framework Handlers")
  class SecurityExceptionRethrowTests {

    @Test
    @DisplayName("AccessDeniedException is rethrown to delegate to RestAccessDeniedHandler")
    void testAccessDeniedException_isRethrown() {
      AccessDeniedException ex = new AccessDeniedException("Access is denied");

      assertThatThrownBy(() -> exceptionHandler.handleAccessDeniedException(ex))
          .isSameAs(ex);
    }

    @Test
    @DisplayName("AuthenticationException is rethrown to delegate to RestAuthenticationEntryPoint")
    void testAuthenticationException_isRethrown() {
      AuthenticationException ex = mock(AuthenticationException.class);

      assertThatThrownBy(() -> exceptionHandler.handleAuthenticationException(ex))
          .isSameAs(ex);
    }
  }

  // =========================================================================
  // 4. DataIntegrityViolationException Database Constraint Mappings (HTTP 409)
  // =========================================================================

  @Nested
  @DisplayName("4. Database Constraint Violation Mappings")
  class DatabaseConstraintMappingTests {

    private DataIntegrityViolationException buildDbException(String message) {
      SQLIntegrityConstraintViolationException sqlEx = new SQLIntegrityConstraintViolationException(message);
      return new DataIntegrityViolationException("could not execute statement", sqlEx);
    }

    @Test
    @DisplayName("Translates uk_emp_verifications_current into 409 ACTIVE_VERIFICATION_ALREADY_EXISTS")
    void testVerificationCurrentConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '5-1' for key 'uk_emp_verifications_current'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ACTIVE_VERIFICATION_ALREADY_EXISTS");
      assertThat(response.getBody().getMessage()).contains("active verification");
    }

    @Test
    @DisplayName("Translates uk_emp_verifications_cycle into 409 DUPLICATE_VERIFICATION_CYCLE")
    void testVerificationCycleConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '5-2' for key 'uk_emp_verifications_cycle'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_VERIFICATION_CYCLE");
    }

    @Test
    @DisplayName("Translates uk_emp_verifications_number into 409 DUPLICATE_VERIFICATION_NUMBER")
    void testVerificationNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'VER-001' for key 'uk_emp_verifications_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_VERIFICATION_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_emp_verifications_request into 409 DUPLICATE_VERIFICATION_REQUEST")
    void testVerificationRequestConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '10' for key 'uk_emp_verifications_request'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_VERIFICATION_REQUEST");
    }

    @Test
    @DisplayName("Translates uk_skill_gaps_current into 409 ACTIVE_SKILL_GAP_ALREADY_EXISTS")
    void testSkillGapCurrentConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '100-20-1' for key 'uk_skill_gaps_current'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ACTIVE_SKILL_GAP_ALREADY_EXISTS");
      assertThat(response.getBody().getMessage()).contains("active current skill gap");
    }

    @Test
    @DisplayName("Translates uk_skill_gaps_assessment_skill into 409 DUPLICATE_ASSESSMENT_SKILL_GAP")
    void testSkillGapAssessmentSkillConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '10-20' for key 'uk_skill_gaps_assessment_skill'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ASSESSMENT_SKILL_GAP");
    }

    @Test
    @DisplayName("Translates uk_skill_gaps_number into 409 DUPLICATE_SKILL_GAP_NUMBER")
    void testSkillGapNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'GAP-001' for key 'uk_skill_gaps_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_SKILL_GAP_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_skill_gap_assessments_number into 409 DUPLICATE_ASSESSMENT_NUMBER")
    void testSkillGapAssessmentNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'SGA-001' for key 'uk_skill_gap_assessments_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ASSESSMENT_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_sgr_number into 409 DUPLICATE_RECOMMENDATION_NUMBER")
    void testSkillGapRecommendationNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '1-1' for key 'uk_sgr_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_RECOMMENDATION_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_training_enrollments_trainee_batch into 409 DUPLICATE_ENROLLMENT")
    void testTrainingEnrollmentTraineeBatchConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '50-10' for key 'uk_training_enrollments_trainee_batch'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ENROLLMENT");
    }

    @Test
    @DisplayName("Translates uk_training_enrollments_number into 409 DUPLICATE_ENROLLMENT_NUMBER")
    void testTrainingEnrollmentNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'ENR-001' for key 'uk_training_enrollments_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ENROLLMENT_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_users_username into 409 USERNAME_ALREADY_EXISTS")
    void testUserUsernameConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'johndoe' for key 'uk_users_username'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("USERNAME_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Translates uk_users_email into 409 EMAIL_ALREADY_EXISTS")
    void testUserEmailConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'john@example.com' for key 'uk_users_email'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Translates uk_trainees_registration_number into 409 DUPLICATE_TRAINEE_REGISTRATION")
    void testTraineeRegistrationConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'REG-001' for key 'uk_trainees_registration_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_TRAINEE_REGISTRATION");
    }

    @Test
    @DisplayName("Translates generic duplicate entry error into 409 RESOURCE_CONFLICT")
    void testGenericDuplicateEntry_mapsTo409ResourceConflict() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'unmapped_val' for key 'uk_custom_table_key'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_CONFLICT");
      assertThat(response.getBody().getMessage()).contains("resource with the specified unique attributes already exists");
      // Must not leak internal database or key names
      assertThat(response.getBody().getMessage()).doesNotContain("uk_custom_table_key");
      assertThat(response.getBody().getMessage()).doesNotContain("unmapped_val");
    }

    @Test
    @DisplayName("Foreign key or unrelated database integrity violation returns sanitized HTTP 500 without leaking SQL internals")
    void testForeignKeyViolation_returnsSanitized500() {
      DataIntegrityViolationException ex = buildDbException(
          "Cannot add or update a child row: a foreign key constraint fails (`SIH26135`.`users`, CONSTRAINT `fk_secret_table`)");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected database error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("SIH26135");
      assertThat(response.getBody().getMessage()).doesNotContain("fk_secret_table");
    }
  }

  // =========================================================================
  // 5. Generic Unhandled Exceptions (HTTP 500 - Sanitized)
  // =========================================================================

  @Nested
  @DisplayName("5. Unhandled Generic Exceptions - Sanitized HTTP 500")
  class UnhandledGenericExceptionTests {

    @Test
    @DisplayName("Unhandled RuntimeException returns sanitized HTTP 500 without leaking stack traces or internal messages")
    void testUnhandledRuntimeException_returnsSanitized500() {
      RuntimeException ex = new RuntimeException("Internal SQL syntax error: SELECT * FROM confidential_table");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      // Strictly zero exposure of internal message or table names
      assertThat(response.getBody().getMessage()).doesNotContain("confidential_table");
      assertThat(response.getBody().getMessage()).doesNotContain("Internal SQL syntax error");
    }

    @Test
    @DisplayName("NullPointerException returns sanitized HTTP 500")
    void testNullPointerException_returnsSanitized500() {
      NullPointerException ex = new NullPointerException("Null pointer in DatabaseService line 102");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("DatabaseService");
    }
  }
}
