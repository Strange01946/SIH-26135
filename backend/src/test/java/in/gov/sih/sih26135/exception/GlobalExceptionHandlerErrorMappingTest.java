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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.time.LocalDate;
import java.util.List;
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

  static DataIntegrityViolationException buildDbException(String message) {
    SQLIntegrityConstraintViolationException sqlEx = new SQLIntegrityConstraintViolationException(message);
    return new DataIntegrityViolationException("could not execute statement", sqlEx);
  }

  /**
   * Dummy project class outside verified DTO packages (in in.gov.sih.sih26135.exception)
   * used to verify that ending with "Request" does not erroneously classify as client request DTO.
   */
  static class ArbitraryProjectRequest {
    private String customField;
  }

  enum SampleTestStatus {
    ACTIVE,
    INACTIVE
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
    org.mockito.Mockito.lenient().when(request.getAttribute(
        org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
        .thenAnswer(inv -> request.getRequestURI());
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
    @DisplayName("MissingRequestHeaderException maps to HTTP 400 MISSING_HEADER")
    void testMissingRequestHeader_mapsTo400() {
      org.springframework.web.bind.MissingRequestHeaderException ex =
          mock(org.springframework.web.bind.MissingRequestHeaderException.class);
      when(ex.getHeaderName()).thenReturn("X-Actor-User-Id");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingRequestHeader(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getError()).isEqualTo("Bad Request");
      assertThat(response.getBody().getCode()).isEqualTo("MISSING_HEADER");
      assertThat(response.getBody().getMessage()).contains("X-Actor-User-Id");
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
    @DisplayName("HttpMessageNotReadableException maps to HTTP 400 MALFORMED_JSON and sanitizes raw payload")
    void testHttpMessageNotReadable_mapsTo400MalformedJson_sanitizesPayload() {
      org.springframework.http.converter.HttpMessageNotReadableException ex =
          new org.springframework.http.converter.HttpMessageNotReadableException(
              "JSON parse error: Unexpected character in {\"password\": \"SecretPassword123!\", \"salary\": 90000}");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).isEqualTo("Malformed or unreadable request body");
      assertThat(response.getBody().getMessage()).doesNotContain("SecretPassword123!");
      assertThat(response.getBody().getMessage()).doesNotContain("90000");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException with invalid enum value maps to HTTP 400 MALFORMED_JSON with field message and details")
    void testHttpMessageNotReadable_invalidEnum_mapsTo400WithSafeMessage() {
      JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "status");
      InvalidFormatException cause = new InvalidFormatException(
          null, "Cannot deserialize value", "INVALID_STATUS", SampleTestStatus.class);
      cause.prependPath(ref);

      HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, mock(HttpInputMessage.class));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid enum value for field 'status'");
      assertThat(response.getBody().getDetails()).containsExactly("status: invalid enum value");
      // Zero leakage of target Enum class name or raw value
      assertThat(response.getBody().getMessage()).doesNotContain("SampleTestStatus");
      assertThat(response.getBody().getMessage()).doesNotContain("INVALID_STATUS");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException with invalid date format maps to HTTP 400 MALFORMED_JSON with field message and details")
    void testHttpMessageNotReadable_invalidDate_mapsTo400WithSafeMessage() {
      JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "startDate");
      InvalidFormatException cause = new InvalidFormatException(
          null, "Cannot deserialize value", "not-a-date", LocalDate.class);
      cause.prependPath(ref);

      HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, mock(HttpInputMessage.class));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid date/time format for field 'startDate'");
      assertThat(response.getBody().getDetails()).containsExactly("startDate: invalid date/time format");
      assertThat(response.getBody().getMessage()).doesNotContain("not-a-date");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException with incompatible type maps to HTTP 400 MALFORMED_JSON with field message and details")
    void testHttpMessageNotReadable_incompatibleType_mapsTo400WithSafeMessage() {
      JsonMappingException.Reference ref = new JsonMappingException.Reference(null, "capacity");
      InvalidFormatException cause = new InvalidFormatException(
          null, "Cannot deserialize value", "text-instead-of-int", Integer.class);
      cause.prependPath(ref);

      HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", cause, mock(HttpInputMessage.class));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid value format for field 'capacity'");
      assertThat(response.getBody().getDetails()).containsExactly("capacity: invalid format");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException for missing body maps to HTTP 400 MALFORMED_JSON")
    void testHttpMessageNotReadable_missingBody_mapsTo400() {
      HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
          "Required request body is missing: public ResponseEntity<CourseResponse> in.gov.sih.sih26135.controller.CourseController.createCourse(CourseRequest)");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).isEqualTo("Required request body is missing");
      // Zero disclosure of controller or class signatures
      assertThat(response.getBody().getMessage()).doesNotContain("CourseController");
    }

    @Test
    @DisplayName("ServletRequestBindingException maps to HTTP 400 BAD_REQUEST")
    void testServletRequestBinding_mapsTo400() {
      ServletRequestBindingException ex =
          new ServletRequestBindingException("Binding failure");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleServletRequestBinding(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getError()).isEqualTo("Bad Request");
      assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid request binding or missing parameter/header");
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test-endpoint");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException with Enum type simplifies expected type without leaking internal Enum name")
    void testMethodArgumentTypeMismatch_withEnum_sanitizesType() {
      MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
          "INVALID", SampleTestStatus.class, "status", mock(MethodParameter.class), new IllegalArgumentException());

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter value for 'status'. Expected type: valid enum value");
      assertThat(response.getBody().getMessage()).doesNotContain("SampleTestStatus");
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
    @DisplayName("ConstraintViolationException from verified CommunicationLogController maps to HTTP 400 VALIDATION_ERROR")
    void testConstraintViolation_fromCommunicationLogController_mapsTo400() {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      Path.Node paramNode = mock(Path.Node.class);
      when(paramNode.getKind()).thenReturn(jakarta.validation.ElementKind.PARAMETER);
      when(path.iterator()).thenReturn(Collections.singletonList(paramNode).iterator());
      when(path.toString()).thenReturn("page");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be greater than or equal to minimum value");
      doReturn(in.gov.sih.sih26135.controller.CommunicationLogController.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Set.of(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("page: must be greater than or equal to minimum value");
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
    @DisplayName("HttpMediaTypeNotSupportedException maps to HTTP 415 UNSUPPORTED_MEDIA_TYPE and preserves Accept header")
    void testHttpMediaTypeNotSupported_mapsTo415() {
      HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
          MediaType.TEXT_PLAIN, Collections.singletonList(MediaType.APPLICATION_JSON));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMediaTypeNotSupported(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
      assertThat(response.getBody().getMessage()).contains("text/plain");
      assertThat(response.getHeaders().getAccept()).contains(MediaType.APPLICATION_JSON);
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
    @DisplayName("HttpRequestMethodNotSupportedException maps to HTTP 405 METHOD_NOT_ALLOWED and preserves Allow header")
    void testHttpRequestMethodNotSupported_mapsTo405() {
      HttpRequestMethodNotSupportedException ex =
          new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("METHOD_NOT_ALLOWED");
      assertThat(response.getBody().getMessage()).contains("DELETE");
      assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    @DisplayName("NoResourceFoundException maps to HTTP 404 ENDPOINT_NOT_FOUND")
    void testNoResourceFound_mapsTo404() {
      NoResourceFoundException ex = mock(NoResourceFoundException.class);

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

    @Test
    @DisplayName("BaseException with HttpStatus.INTERNAL_SERVER_ERROR sanitizes client-facing message")
    void testBaseException_withInternalServerError_sanitizesMessage() {
      BaseException ex = new BaseException("Internal database schema error: select * from secret_keys",
          HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR") {};

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DATABASE_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("secret_keys");
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
    @DisplayName("Translates uk_emp_verif_attempts_number into 409 DUPLICATE_VERIFICATION_ATTEMPT_NUMBER")
    void testVerificationAttemptNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'ATT-001' for key 'uk_emp_verif_attempts_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_VERIFICATION_ATTEMPT_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_emp_verif_evidence_reference into 409 DUPLICATE_EVIDENCE_REFERENCE")
    void testVerificationEvidenceReferenceConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'REF-DOC-101' for key 'uk_emp_verif_evidence_reference'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_EVIDENCE_REFERENCE");
    }

    @Test
    @DisplayName("Translates uk_employer_branches_code into 409 BRANCH_CODE_ALREADY_EXISTS")
    void testEmployerBranchesCodeConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'BR-01' for key 'uk_employer_branches_code'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("BRANCH_CODE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Translates uk_import_records_batch_row into 409 DUPLICATE_IMPORT_ROW")
    void testImportRecordsBatchRowConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '1-5' for key 'uk_import_records_batch_row'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_IMPORT_ROW");
    }

    @Test
    @DisplayName("Translates uk_skill_gap_observations_number into 409 DUPLICATE_OBSERVATION_NUMBER")
    void testSkillGapObservationNumberConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry 'OBS-001' for key 'uk_skill_gap_observations_number'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_OBSERVATION_NUMBER");
    }

    @Test
    @DisplayName("Translates uk_survey_questions_version_code into 409 DUPLICATE_QUESTION_CODE")
    void testSurveyQuestionsVersionCodeConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '1-1-Q01' for key 'uk_survey_questions_version_code'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_QUESTION_CODE");
    }

    @Test
    @DisplayName("Translates uk_survey_questions_version_order into 409 DUPLICATE_QUESTION_ORDER")
    void testSurveyQuestionsVersionOrderConstraint_mapsTo409() {
      DataIntegrityViolationException ex = buildDbException(
          "Duplicate entry '1-1-1' for key 'uk_survey_questions_version_order'");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_QUESTION_ORDER");
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

  // =========================================================================
  // 6. Log and Response Body Sanitization Regression Tests
  // =========================================================================

  @Nested
  @DisplayName("6. Log and Response Body Sanitization Regression Tests")
  class LogSanitizationRegressionTests {

    private ListAppender<ILoggingEvent> logAppender;
    private Logger handlerLogger;

    @BeforeEach
    void setupLogger() {
      handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
      logAppender = new ListAppender<>();
      logAppender.start();
      handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDownLogger() {
      if (handlerLogger != null && logAppender != null) {
        handlerLogger.detachAppender(logAppender);
      }
    }

    private String getAllCapturedLogs() {
      StringBuilder sb = new StringBuilder();
      for (ILoggingEvent event : logAppender.list) {
        sb.append(event.getFormattedMessage()).append("\n");
        if (event.getThrowableProxy() != null) {
          sb.append(event.getThrowableProxy().getClassName()).append(": ")
              .append(event.getThrowableProxy().getMessage()).append("\n");
        }
      }
      return sb.toString();
    }

    @Test
    @DisplayName("Malformed JSON: Raw password and token sentinels do not leak in logs or response body")
    void testMalformedJson_sentinelsDoNotLeakInLogsOrResponse() {
      String sentinelPassword = "SENTINEL_PASSWORD_SECRET_RAW_987654";
      String sentinelToken = "SENTINEL_TOKEN_SECRET_RAW_ABCDEF";
      when(request.getRequestURI()).thenReturn("/api/auth/login");

      HttpInputMessage inputMessage = mock(HttpInputMessage.class);
      HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
          "JSON parse error: Unexpected character in password '" + sentinelPassword + "' and token '" + sentinelToken + "'",
          new RuntimeException("Raw payload token=" + sentinelToken),
          inputMessage);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex, request);

      // Assert client response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("MALFORMED_JSON");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelPassword);
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelToken);

      // Assert captured logs
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelPassword);
      assertThat(logs).doesNotContain(sentinelToken);
      assertThat(logs).contains("/api/auth/login");
      assertThat(logs).contains("MALFORMED_JSON");
      assertThat(logs).contains("status=400");
    }

    @Test
    @DisplayName("Type Mismatch: Sensitive salary sentinel does not leak in logs or response body")
    void testTypeMismatch_salarySentinelDoesNotLeakInLogsOrResponse() {
      String sentinelSalary = "SENTINEL_SALARY_VALUE_999999.50";
      when(request.getRequestURI()).thenReturn("/api/v1/salary-histories");

      MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
      when(ex.getName()).thenReturn("salary");
      doReturn(Long.class).when(ex).getRequiredType();

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, request);

      // Assert client response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelSalary);

      // Assert captured logs
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelSalary);
      assertThat(logs).contains("/api/v1/salary-histories");
      assertThat(logs).contains("TYPE_MISMATCH");
      assertThat(logs).contains("parameter 'salary'");
      assertThat(logs).contains("expectedType=Long");
      assertThat(logs).contains("status=400");
    }

    @Test
    @DisplayName("Domain Exception: Sensitive identity sentinel does not leak in logs or response body")
    void testDomainException_identitySentinelDoesNotLeakInLogsOrResponse() {
      String sentinelAadhaar = "SENTINEL_AADHAAR_9999_8888_7777";
      when(request.getRequestURI()).thenReturn("/api/v1/trainees/verify");

      BadRequestException ex = new BadRequestException(
          "Identity verification failed for Aadhaar " + sentinelAadhaar, "INVALID_IDENTITY");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_IDENTITY");
      // Must not leak the sensitive identity sentinel in client response body
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelAadhaar);
      assertThat(response.getBody().getMessage()).isEqualTo("Identity verification failed. Please verify the provided identity details.");

      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelAadhaar);
      assertThat(logs).contains("/api/v1/trainees/verify");
      assertThat(logs).contains("code=INVALID_IDENTITY");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("exception=BadRequestException");
    }

    @Test
    @DisplayName("500-level BaseException: Internal secret sentinel is masked in both client response and server logs")
    void testBaseException500_secretSentinelDoesNotLeakInLogsOrResponse() {
      String sentinelSecret = "SENTINEL_INTERNAL_ENCRYPTION_KEY_SECRET_789";
      when(request.getRequestURI()).thenReturn("/api/v1/secure-vault");

      BaseException ex = new BaseException("Failed with secret: " + sentinelSecret,
          HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR") {};

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      // Client response must be completely masked
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelSecret);

      // Server logs must not contain the secret sentinel
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelSecret);
      assertThat(logs).contains("/api/v1/secure-vault");
      assertThat(logs).contains("code=INTERNAL_ERROR");
      assertThat(logs).contains("status=500");
    }

    @Test
    @DisplayName("Database Integrity: Recognized duplicate unique constraint does not leak SQL or bound parameters in logs or response")
    void testDataIntegrityViolation_recognizedDuplicate_sentinelsDoNotLeak() {
      String sentinelSql = "SENTINEL_SQL_INSERT_INTO_EMPLOYMENTS_PLACEMENTS_999";
      String sentinelParam = "SENTINEL_BOUND_PARAM_PLC_12345";
      when(request.getRequestURI()).thenReturn("/api/v1/placements/12345/transition-to-employment");

      DataIntegrityViolationException ex = buildDbException(
          "could not execute statement; SQL [" + sentinelSql + " with param " + sentinelParam
              + "]; constraint [uk_employment_records_placement]");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelSql);
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelParam);

      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelSql);
      assertThat(logs).doesNotContain(sentinelParam);
      assertThat(logs).contains("status=409");
      assertThat(logs).contains("code=DUPLICATE_PLACEMENT_EMPLOYMENT");
    }

    @Test
    @DisplayName("Database Integrity: Unrecognized non-duplicate integrity violation returns HTTP 500 without leaking SQL or bound parameters")
    void testDataIntegrityViolation_unrecognizedNonDuplicate_sentinelsDoNotLeak() {
      String sentinelSql = "SENTINEL_SQL_INSERT_INTO_USERS_CREDENTIALS_888";
      String sentinelParam = "SENTINEL_BOUND_PASSWORD_HASH_ABC";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      DataIntegrityViolationException ex = buildDbException(
          "could not execute statement; SQL [" + sentinelSql + " with param " + sentinelParam
              + "]; Cannot add or update a child row: a foreign key constraint fails (`SIH26135`.`users`, CONSTRAINT `fk_nonexistent_table`)");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      // Must return HTTP 500 Internal Server Error (not 409 Conflict)
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected database error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelSql);
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelParam);
      assertThat(response.getBody().getMessage()).doesNotContain("SIH26135");

      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelSql);
      assertThat(logs).doesNotContain(sentinelParam);
      assertThat(logs).contains("status=500");
      assertThat(logs).contains("code=INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Unexpected Exception: Raw database credentials and stack trace sentinels do not leak in logs or response body")
    void testUnexpectedException_credentialSentinelsDoNotLeakInLogsOrResponse() {
      String sentinelDbPassword = "SENTINEL_RAW_DB_PASSWORD_SUPER_SECRET_999";
      String sentinelQuery = "SENTINEL_RAW_SQL_SELECT_ALL_FROM_CONFIDENTIAL_VAULT";
      when(request.getRequestURI()).thenReturn("/api/v1/admin/operations");

      RuntimeException nestedCause = new RuntimeException("Nested SQL error for query: " + sentinelQuery);
      RuntimeException ex = new RuntimeException("DB Connection failed with password: " + sentinelDbPassword, nestedCause);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelDbPassword);
      assertThat(response.getBody().getMessage()).doesNotContain(sentinelQuery);

      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelDbPassword);
      assertThat(logs).doesNotContain(sentinelQuery);
      assertThat(logs).contains("/api/v1/admin/operations");
      assertThat(logs).contains("status=500");
      assertThat(logs).contains("code=INTERNAL_SERVER_ERROR");
      assertThat(logs).contains("exception=java.lang.RuntimeException");
    }

    @Test
    @DisplayName("Validation Error: Sensitive sentinel in custom message does not leak in logs or response details")
    void testValidationException_sanitizesFieldErrorsInLogs() {
      String sentinelSalary = "SENTINEL_REJECTED_SALARY_999999";
      when(request.getRequestURI()).thenReturn("/api/v1/salary-histories");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError("salaryHistoryRequest", "amount", sentinelSalary, false, null, null, "invalid amount: " + sentinelSalary);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not leak the sensitive sentinel in response details, while preserving safe field-level feedback
      assertThat(response.getBody().getDetails()).containsExactly("amount: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(sentinelSalary);

      // Server logs must NOT log the rejected value or the raw error message containing sentinel
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelSalary);
      assertThat(logs).contains("/api/v1/salary-histories");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidFields=[amount]");
    }

    @Test
    @DisplayName("Constraint Violation: Sensitive sentinel in custom violation message does not leak in logs or response details")
    void testConstraintViolation_sentinelInMessageDoesNotLeakInLogsOrResponse() {
      String sentinelAadhaar = "SENTINEL_CONSTRAINT_AADHAAR_9999_8888_7777";
      when(request.getRequestURI()).thenReturn("/api/v1/trainees/verify");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("aadhaarNumber");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("Invalid Aadhaar number: " + sentinelAadhaar);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTraineeRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not leak sensitive sentinel in response details, while preserving safe field-level feedback
      assertThat(response.getBody().getDetails()).containsExactly("aadhaarNumber: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(sentinelAadhaar);

      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(sentinelAadhaar);
      assertThat(logs).contains("/api/v1/trainees/verify");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidProperties=[aadhaarNumber]");
    }

    @Test
    @DisplayName("Validation Error: Custom FieldError echoing arbitrary rejected value is sanitized in response details and logs")
    void testValidationException_arbitraryRejectedValueEcho_isSanitizedInResponseDetailsAndLogs() {
      String arbitraryRejectedValue = "CUSTOM_USER_VALUE_123";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "userRequest",
          "username",
          arbitraryRejectedValue,
          false,
          null,
          null,
          "Username " + arbitraryRejectedValue + " is invalid or already taken"
      );
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not echo the arbitrary rejected value in response details; preserves safe field-level feedback
      assertThat(response.getBody().getDetails()).containsExactly("username: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitraryRejectedValue);

      // Server logs must NOT log the rejected value or the echoed custom message
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(arbitraryRejectedValue);
      assertThat(logs).contains("/api/v1/users");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidFields=[username]");
    }

    @Test
    @DisplayName("Constraint Violation: Custom message echoing arbitrary submitted value is sanitized in response details and logs")
    void testConstraintViolation_arbitrarySubmittedValueEcho_isSanitizedInResponseDetailsAndLogs() {
      String arbitrarySubmittedValue = "CUSTOM_SUBMITTED_INPUT_456";
      when(request.getRequestURI()).thenReturn("/api/v1/training-batches");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("batchCode");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("Invalid batch code: " + arbitrarySubmittedValue);
      when(violation.getInvalidValue()).thenReturn(arbitrarySubmittedValue);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not echo the arbitrary submitted value in response details; preserves safe property feedback
      assertThat(response.getBody().getDetails()).containsExactly("batchCode: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitrarySubmittedValue);

      // Server logs must NOT log the submitted value or the raw error message
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(arbitrarySubmittedValue);
      assertThat(logs).contains("/api/v1/training-batches");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidProperties=[batchCode]");
    }

    @Test
    @DisplayName("Constraint Violation: Message starting with numeric value out of bounds followed by arbitrary text with null invalidValue is sanitized")
    void testConstraintViolation_numericValueOutOfBoundsWithArbitraryText_nullInvalidValue_isSanitized() {
      String arbitraryCustomText = "CUSTOM_OUT_OF_BOUNDS_PAYLOAD_789";
      when(request.getRequestURI()).thenReturn("/api/v1/training-batches");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("capacity");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("numeric value out of bounds: " + arbitraryCustomText);
      when(violation.getInvalidValue()).thenReturn(null);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not leak the arbitrary custom text in response details; preserves safe fallback feedback
      assertThat(response.getBody().getDetails()).containsExactly("capacity: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitraryCustomText);

      // Server logs must NOT log the arbitrary custom text
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(arbitraryCustomText);
      assertThat(logs).contains("/api/v1/training-batches");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidProperties=[capacity]");
    }

    @Test
    @DisplayName("Validation Error: Single-digit rejected value with arbitrary appended text in message is sanitized in response details and logs")
    void testValidationException_singleDigitRejectedValueWithArbitraryAppendedText_isSanitized() {
      Integer singleDigitValue = 5;
      String arbitraryAppendedText = "CUSTOM_APPENDED_NOTE_456";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "userRequest",
          "retryCount",
          singleDigitValue,
          false,
          null,
          null,
          "must be greater than 0 - " + arbitraryAppendedText
      );
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not leak the arbitrary appended text in response details; preserves safe fallback feedback
      assertThat(response.getBody().getDetails()).containsExactly("retryCount: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitraryAppendedText);

      // Server logs must NOT log the arbitrary appended text
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(arbitraryAppendedText);
      assertThat(logs).contains("/api/v1/users");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidFields=[retryCount]");
    }

    @Test
    @DisplayName("Constraint Violation: Single-digit invalidValue with arbitrary appended text in message is sanitized in response details and logs")
    void testConstraintViolation_singleDigitInvalidValueWithArbitraryAppendedText_isSanitized() {
      Integer singleDigitValue = 0;
      String arbitraryAppendedText = "CUSTOM_LEAKED_DETAIL_123";
      when(request.getRequestURI()).thenReturn("/api/v1/training-batches");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("durationWeeks");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("must be greater than 0 with leaked param " + arbitraryAppendedText);
      when(violation.getInvalidValue()).thenReturn(singleDigitValue);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(400);
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).isNotNull();
      // Must not leak the arbitrary appended text in response details; preserves safe fallback feedback
      assertThat(response.getBody().getDetails()).containsExactly("durationWeeks: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitraryAppendedText);

      // Server logs must NOT log the arbitrary appended text
      String logs = getAllCapturedLogs();
      assertThat(logs).doesNotContain(arbitraryAppendedText);
      assertThat(logs).contains("/api/v1/training-batches");
      assertThat(logs).contains("status=400");
      assertThat(logs).contains("code=VALIDATION_ERROR");
      assertThat(logs).contains("invalidProperties=[durationWeeks]");
    }
  }

  // =========================================================================
  // 7. Phase 26.9 - Error Sanitization, Data Privacy & Anti-Echo Tests
  // =========================================================================

  @Nested
  @DisplayName("7. Phase 26.9 - Error Sanitization, Data Privacy & Anti-Echo Tests")
  class Phase269ErrorSanitizationAndDataPrivacyTests {

    @Test
    @DisplayName("Aadhaar: Unformatted 12-digit Aadhaar number in BaseException message is sanitized to code-derived safe message")
    void testAadhaar_unformatted12Digits_inBaseException_isSanitized() {
      String rawAadhaar = "123456789012";
      when(request.getRequestURI()).thenReturn("/api/v1/trainees/verify");

      BadRequestException ex = new BadRequestException(
          "Trainee verification failed for Aadhaar " + rawAadhaar, "INVALID_IDENTITY");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_IDENTITY");
      assertThat(response.getBody().getMessage()).doesNotContain(rawAadhaar);
      assertThat(response.getBody().getMessage()).isEqualTo("Identity verification failed. Please verify the provided identity details.");
    }

    @Test
    @DisplayName("Aadhaar: Space-formatted 4-4-4 Aadhaar number in validation message is sanitized in response details")
    void testAadhaar_spaceFormatted_inValidationError_isSanitized() {
      String spaceAadhaar = "1234 5678 9012";
      when(request.getRequestURI()).thenReturn("/api/v1/trainees");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "traineeRequest", "aadhaarNumber", spaceAadhaar, false, null, null, "Invalid Aadhaar: " + spaceAadhaar);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("aadhaarNumber: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(spaceAadhaar);
    }

    @Test
    @DisplayName("Aadhaar: Hyphen-formatted 4-4-4 Aadhaar number in constraint violation message is sanitized")
    void testAadhaar_hyphenFormatted_inConstraintViolation_isSanitized() {
      String hyphenAadhaar = "1234-5678-9012";
      when(request.getRequestURI()).thenReturn("/api/v1/trainees");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("aadhaarNumber");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("Aadhaar " + hyphenAadhaar + " format mismatch");
      when(violation.getInvalidValue()).thenReturn(hyphenAadhaar);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTraineeRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("aadhaarNumber: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(hyphenAadhaar);
    }

    @Test
    @DisplayName("Aadhaar: Parenthesis-wrapped Aadhaar number in URI path is sanitized in NoResourceFoundException")
    void testAadhaar_parenthesisFormatted_inEndpointNotFound_isSanitized() {
      String sensitiveUri = "/api/v1/trainees/(123456789012)";
      when(request.getRequestURI()).thenReturn(sensitiveUri);

      NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, sensitiveUri);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ENDPOINT_NOT_FOUND");
      assertThat(response.getBody().getMessage()).doesNotContain("123456789012");
      assertThat(response.getBody().getMessage()).isEqualTo("No static resource or endpoint found");
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
    }

    @Test
    @DisplayName("PAN: Uppercase PAN number in BaseException message is sanitized to code-derived safe message")
    void testPAN_uppercase_inBaseException_isSanitized() {
      String pan = "ABCDE1234F";
      when(request.getRequestURI()).thenReturn("/api/v1/employers/verify");

      BadRequestException ex = new BadRequestException(
          "Duplicate PAN " + pan + " already registered", "INVALID_PAN");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_PAN");
      assertThat(response.getBody().getMessage()).doesNotContain(pan);
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid PAN format");
    }

    @Test
    @DisplayName("PAN: Lowercase PAN number in validation error is sanitized in response details")
    void testPAN_lowercase_inValidationError_isSanitized() {
      String lowerPan = "abcde1234f";
      when(request.getRequestURI()).thenReturn("/api/v1/employers");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "employerRequest", "panNumber", lowerPan, false, null, null, "Invalid pan " + lowerPan);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("panNumber: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(lowerPan);
    }

    @Test
    @DisplayName("Database URL: JDBC URL with credentials in exception message is completely suppressed")
    void testDatabaseUrl_withCredentials_inException_isSanitized() {
      String sensitiveUrl = "jdbc:mysql://db.prod.internal:3306/sih26135?user=dbadmin&password=SuperSecretPassword123!";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      RuntimeException ex = new RuntimeException("Unable to connect to " + sensitiveUrl);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("jdbc:mysql");
      assertThat(response.getBody().getMessage()).doesNotContain("db.prod.internal");
      assertThat(response.getBody().getMessage()).doesNotContain("dbadmin");
      assertThat(response.getBody().getMessage()).doesNotContain("SuperSecretPassword123!");
    }

    @Test
    @DisplayName("Database URL: PostgreSQL URL with credentials in DataIntegrityViolationException is suppressed")
    void testPostgresUrl_withCredentials_inDataIntegrityViolation_isSanitized() {
      String pgUrl = "postgresql://admin:secretPass@10.0.1.5:5432/production";
      when(request.getRequestURI()).thenReturn("/api/v1/batches");

      DataIntegrityViolationException ex = buildDbException("could not connect to " + pgUrl);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected database error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("postgresql://");
      assertThat(response.getBody().getMessage()).doesNotContain("secretPass");
      assertThat(response.getBody().getMessage()).doesNotContain("10.0.1.5");
    }

    @Test
    @DisplayName("Raw SQL: SELECT query with column names in BaseException message is suppressed")
    void testRawSql_selectQuery_inBaseException_isSanitized() {
      String sql = "SELECT password_hash, salt FROM users WHERE username = 'admin'";
      when(request.getRequestURI()).thenReturn("/api/v1/users/admin");

      BadRequestException ex = new BadRequestException(
          "Failed executing: " + sql, "DATABASE_ERROR");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DATABASE_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected database error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("SELECT");
      assertThat(response.getBody().getMessage()).doesNotContain("password_hash");
      assertThat(response.getBody().getMessage()).doesNotContain("users");
      assertThat(response.getBody().getMessage()).doesNotContain("'admin'");
    }

    @Test
    @DisplayName("Raw SQL: INSERT query in constraint violation message is suppressed in response details")
    void testRawSql_insertQuery_inConstraintViolation_isSanitized() {
      String sql = "INSERT INTO payroll (trainee_id, salary) VALUES (101, 75000.00)";
      when(request.getRequestURI()).thenReturn("/api/v1/salary-histories");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("amount");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("SQL Error: " + sql);
      when(violation.getInvalidValue()).thenReturn("75000.00");
      doReturn(in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest.class).when(violation).getRootBeanClass();

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("amount: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("INSERT INTO");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("payroll");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("75000.00");
    }

    @Test
    @DisplayName("JWT: Full JWT token in BaseException message is suppressed to code-derived message")
    void testJwtToken_inBaseException_isSanitized() {
      String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
      when(request.getRequestURI()).thenReturn("/api/auth/validate");

      BadRequestException ex = new BadRequestException("Token expired: " + jwt, "INVALID_TOKEN");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INVALID_TOKEN");
      assertThat(response.getBody().getMessage()).doesNotContain(jwt);
      assertThat(response.getBody().getMessage()).doesNotContain("eyJ");
      assertThat(response.getBody().getMessage()).isEqualTo("Invalid or expired authentication token");
    }

    @Test
    @DisplayName("Bearer Token: Bearer token header value in validation error is suppressed in response details")
    void testBearerToken_inValidationMessage_isSanitized() {
      String bearer = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyPayloadString12345";
      when(request.getRequestURI()).thenReturn("/api/auth/refresh");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "refreshTokenRequest", "token", bearer, false, null, null, "Invalid authorization: " + bearer);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("token: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("Bearer");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("eyJ");
    }

    @Test
    @DisplayName("API Key: API key assignment in generic exception is suppressed")
    void testApiKey_assignment_inException_isSanitized() {
      String apiKeyStr = "api_key=ak_live_998877665544332211";
      when(request.getRequestURI()).thenReturn("/api/v1/integrations");

      RuntimeException ex = new RuntimeException("API authentication failed: " + apiKeyStr);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("api_key");
      assertThat(response.getBody().getMessage()).doesNotContain("ak_live_");
    }

    @Test
    @DisplayName("Filesystem: Windows absolute path in exception message is suppressed")
    void testWindowsPath_inException_isSanitized() {
      String winPath = "C:\\Users\\aditya\\workspace\\backend\\src\\main\\resources\\application-prod.properties";
      when(request.getRequestURI()).thenReturn("/api/v1/admin/config");

      RuntimeException ex = new RuntimeException("Configuration file missing: " + winPath);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("C:\\Users");
      assertThat(response.getBody().getMessage()).doesNotContain("application-prod.properties");
    }

    @Test
    @DisplayName("Filesystem: Unix absolute path in validation error is suppressed in response details")
    void testUnixPath_inValidationMessage_isSanitized() {
      String unixPath = "/etc/ssl/certs/private.key";
      when(request.getRequestURI()).thenReturn("/api/v1/certificates");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "certRequest", "certificatePath", unixPath, false, null, null, "File not found at " + unixPath);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("certificatePath: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("/etc/ssl");
    }

    @Test
    @DisplayName("Internal Package: Internal application package name in BaseException is suppressed")
    void testInternalPackageName_inBaseException_isSanitized() {
      String internalClass = "in.gov.sih.sih26135.service.impl.UserServiceImpl";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      BadRequestException ex = new BadRequestException(
          "Failure occurred inside " + internalClass, "BAD_REQUEST");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
      assertThat(response.getBody().getMessage()).isEqualTo("The request is invalid");
      assertThat(response.getBody().getMessage()).doesNotContain("in.gov.sih.sih26135");
      assertThat(response.getBody().getMessage()).doesNotContain("UserServiceImpl");
    }

    @Test
    @DisplayName("Hibernate: Hibernate package reference in exception message is suppressed")
    void testHibernateClassName_inException_isSanitized() {
      String hibernateClass = "org.hibernate.exception.ConstraintViolationException";
      when(request.getRequestURI()).thenReturn("/api/v1/courses");

      RuntimeException ex = new RuntimeException("Exception from " + hibernateClass);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("org.hibernate");
    }

    @Test
    @DisplayName("Stack Trace: Line number and method location in exception message is suppressed")
    void testStackTrace_inException_isSanitized() {
      String stackLine = "at in.gov.sih.sih26135.controller.UserController.createUser(UserController.java:85)";
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      RuntimeException ex = new RuntimeException("Crash occurred: " + stackLine);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("UserController.java:85");
      assertThat(response.getBody().getMessage()).doesNotContain("at in.gov.sih");
    }

    @Test
    @DisplayName("Stack Trace: 'Caused by:' prefix in exception message is suppressed")
    void testCausedBy_inException_isSanitized() {
      String causedBy = "Caused by: java.lang.NullPointerException: object reference is null";
      when(request.getRequestURI()).thenReturn("/api/v1/programs");

      RuntimeException ex = new RuntimeException("Wrapped error: " + causedBy);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("Caused by:");
    }

    @Test
    @DisplayName("URI Sanitization: Aadhaar number in probe path is sanitized in NoResourceFoundException")
    void testNoResourceFound_withAadhaarInPath_sanitizesUri() {
      String sensitiveUri = "/api/v1/users/123456789012";
      when(request.getRequestURI()).thenReturn(sensitiveUri);

      NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, sensitiveUri);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ENDPOINT_NOT_FOUND");
      assertThat(response.getBody().getMessage()).isEqualTo("No static resource or endpoint found");
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getMessage()).doesNotContain("123456789012");
      assertThat(response.getBody().getPath()).doesNotContain("123456789012");
    }

    @Test
    @DisplayName("URI Sanitization: Sentinel token in probe path is sanitized in NoResourceFoundException")
    void testNoResourceFound_withSentinelTokenInPath_sanitizesUri() {
      String sensitiveUri = "/api/v1/auth/tokens/SENTINEL_TOKEN_SECRET_999";
      when(request.getRequestURI()).thenReturn(sensitiveUri);

      NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, sensitiveUri);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ENDPOINT_NOT_FOUND");
      assertThat(response.getBody().getMessage()).isEqualTo("No static resource or endpoint found");
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getMessage()).doesNotContain("SENTINEL");
      assertThat(response.getBody().getPath()).doesNotContain("SENTINEL");
    }

    @Test
    @DisplayName("URI Sanitization: Unmapped path with no matching route template fails closed to /sanitized-path in NoResourceFoundException")
    void testNoResourceFound_withNoMatchingRouteTemplate_failsClosedToSanitizedPath() {
      String normalUri = "/api/v1/nonexistent-feature";
      when(request.getRequestURI()).thenReturn(normalUri);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn(null);

      NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, normalUri);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ENDPOINT_NOT_FOUND");
      assertThat(response.getBody().getMessage()).isEqualTo("No static resource or endpoint found");
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("nonexistent-feature");
    }

    @Test
    @DisplayName("Anti-Echo: String array rejected value is not echoed in validation response details")
    void testValidationException_stringArrayRejectedValue_isNotEchoed() {
      String maliciousValue = "malicious_script_payload_123";
      String[] rejectedArray = new String[]{maliciousValue, "second_element"};
      when(request.getRequestURI()).thenReturn("/api/v1/items");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "itemRequest", "tags", rejectedArray, false, null, null, "Invalid tags: " + maliciousValue);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("tags: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(maliciousValue);
    }

    @Test
    @DisplayName("Anti-Echo: Iterable rejected value is not echoed in validation response details")
    void testValidationException_iterableRejectedValue_isNotEchoed() {
      String leakedEmail = "leaked_user_email@domain.com";
      List<String> rejectedList = List.of(leakedEmail);
      when(request.getRequestURI()).thenReturn("/api/v1/users");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "userRequest", "emails", rejectedList, false, null, null, "Email " + leakedEmail + " is already taken");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("emails: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(leakedEmail);
    }

    @Test
    @DisplayName("Defense-in-Depth: Field name containing sensitive data is sanitized to 'field'")
    void testValidationException_sensitiveFieldName_isSanitizedToField() {
      String sensitiveField = "SENTINEL_SECRET_FIELD_NAME";
      when(request.getRequestURI()).thenReturn("/api/v1/config");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "configRequest", sensitiveField, "value", false, null, null, "must not be blank");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("field: must not be blank");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(sensitiveField);
    }

    @Test
    @DisplayName("Anti-Echo: Single-digit rejected value echoed in custom message is strictly detected and sanitized")
    void testContainsRejectedValue_singleDigitRejectedValueEchoed_isSanitized() {
      Integer rejectedDigit = 5;
      when(request.getRequestURI()).thenReturn("/api/v1/test");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "count", rejectedDigit, false, null, null, "Provided " + rejectedDigit + " is invalid");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("count: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("5");
    }

    @Test
    @DisplayName("Anti-Echo: Boolean false rejected value echoed in custom message is strictly detected and sanitized")
    void testContainsRejectedValue_booleanFalseRejectedValueEchoed_isSanitized() {
      Boolean rejectedBool = false;
      when(request.getRequestURI()).thenReturn("/api/v1/test");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "active", rejectedBool, false, null, null, "Value " + rejectedBool + " is not permitted");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("active: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("false");
    }

    @Test
    @DisplayName("Anti-Echo: Boolean true rejected value echoed in custom message is strictly detected and sanitized")
    void testContainsRejectedValue_booleanTrueRejectedValueEchoed_isSanitized() {
      Boolean rejectedBool = true;
      when(request.getRequestURI()).thenReturn("/api/v1/test");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "flag", rejectedBool, false, null, null, "Setting " + rejectedBool + " is disallowed");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("flag: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain("true");
    }

    @Test
    @DisplayName("Anti-Echo: Cyclic collection passed as rejected value is handled safely without StackOverflowError")
    void testContainsRejectedValue_cyclicCollection_handlesWithoutStackOverflow() {
      java.util.List<Object> cyclicList = new java.util.ArrayList<>();
      cyclicList.add(cyclicList);
      when(request.getRequestURI()).thenReturn("/api/v1/test");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "items", cyclicList, false, null, null, "items: is invalid");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("items: is invalid");
    }

    @Test
    @DisplayName("Anti-Echo: Deeply nested array exceeding max collection depth terminates gracefully")
    void testContainsRejectedValue_deepCollectionExceedingMaxDepth_handlesGracefully() {
      Object[] nested = new Object[]{ new Object[]{ new Object[]{ new Object[]{ "deepPayload" } } } };
      when(request.getRequestURI()).thenReturn("/api/v1/test");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "tree", nested, false, null, null, "tree: is invalid");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("tree: is invalid");
    }

    @Test
    @DisplayName("Safe-by-Construction: BaseException with arbitrary unapproved message and unknown code fails closed to generic safe fallback")
    void testBaseException_arbitraryUnapprovedMessageUnknownCode_failsClosedToGenericSafeMessage() {
      String arbitraryInternalMessage = "Some proprietary internal business failure note that should never leak";
      when(request.getRequestURI()).thenReturn("/api/v1/custom");

      BadRequestException ex = new BadRequestException(arbitraryInternalMessage, "UNKNOWN_PROPRIETARY_CODE");

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("UNKNOWN_PROPRIETARY_CODE");
      assertThat(response.getBody().getMessage()).isEqualTo("The request is invalid");
      assertThat(response.getBody().getMessage()).doesNotContain("proprietary");
      assertThat(response.getBody().getMessage()).doesNotContain("internal");
      assertThat(response.getBody().getMessage()).doesNotContain("failure note");
    }

    @Test
    @DisplayName("Safe-by-Construction: BaseException with 500 status and arbitrary message fails closed to unexpected error message")
    void testBaseException_500LevelArbitraryMessageUnknownCode_failsClosedToInternalErrorMessage() {
      String internalDetail = "Internal DB table trainees schema column mismatch";
      when(request.getRequestURI()).thenReturn("/api/v1/internal");

      BaseException ex = new BaseException(internalDetail, HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN_500_CODE") {};

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("UNKNOWN_500_CODE");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
      assertThat(response.getBody().getMessage()).doesNotContain("trainees");
      assertThat(response.getBody().getMessage()).doesNotContain("schema");
      assertThat(response.getBody().getMessage()).doesNotContain("column");
    }

    @Test
    @DisplayName("Safe-by-Construction: Constraint violation with unrecognized constraint code falls back to 'is invalid'")
    void testConstraintViolation_unrecognizedConstraintAnnotation_failsClosedToIsInvalid() {
      String arbitraryValue = "CUSTOM_PAYLOAD_XYZ";
      when(request.getRequestURI()).thenReturn("/api/v1/training-batches");

      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      Path path = mock(Path.class);
      when(path.toString()).thenReturn("batchCode");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("Custom error echoing " + arbitraryValue);
      when(violation.getInvalidValue()).thenReturn(arbitraryValue);
      doReturn(in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class).when(violation).getRootBeanClass();

      // Constraint descriptor returns null or unrecognized annotation
      when(violation.getConstraintDescriptor()).thenReturn(null);

      ConstraintViolationException ex = new ConstraintViolationException("Validation failed", Collections.singleton(violation));

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("batchCode: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(arbitraryValue);
    }

    @Test
    @DisplayName("Anti-Echo: Rejected value nested exactly at the depth boundary (depth 3 leaf) is detected and sanitized")
    void testContainsRejectedValue_nestedExactlyAtDepthBoundary_isSanitized() {
      String boundarySecret = "BOUNDARY_SECRET_TOKEN_123";
      Object[] lvl2 = new Object[]{ boundarySecret };
      Object[] lvl1 = new Object[]{ lvl2 };
      Object[] lvl0 = new Object[]{ lvl1 }; // depth 0 (lvl0) -> depth 1 (lvl1) -> depth 2 (lvl2) -> depth 3 (leaf)
      when(request.getRequestURI()).thenReturn("/api/v1/test-endpoint");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "data", lvl0, false, null, null, "Validation error echoing " + boundarySecret);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("data: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(boundarySecret);
    }

    @Test
    @DisplayName("Anti-Echo: Rejected value nested beyond the depth boundary fails closed without exposing nested value")
    void testContainsRejectedValue_nestedBeyondDepthBoundary_failsClosed() {
      String deepSecret = "DEEP_SECRET_PAYLOAD_456";
      Object[] lvl3 = new Object[]{ deepSecret };
      Object[] lvl2 = new Object[]{ lvl3 };
      Object[] lvl1 = new Object[]{ lvl2 };
      Object[] lvl0 = new Object[]{ lvl1 }; // depth 0 -> 1 -> 2 -> 3 (collection beyond limit)
      when(request.getRequestURI()).thenReturn("/api/v1/test-endpoint");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "testDto", "data", lvl0, false, null, null, "Custom message with " + deepSecret);
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("data: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(deepSecret);
    }

    @Test
    @DisplayName("Anti-Echo: Unrelated safe standard validation message is preserved")
    void testContainsRejectedValue_unrelatedSafeMessage_isPreserved() {
      when(request.getRequestURI()).thenReturn("/api/v1/test-endpoint");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "userDto", "username", "john_doe", false, null, null, "must not be blank");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("username: must not be blank");
    }

    @Test
    @DisplayName("Anti-Echo: Set Iterable implementation echoing rejected value is strictly sanitized")
    void testContainsRejectedValue_iterableSetImplementation_isSanitized() {
      String leakedSetElement = "LEAKED_SET_VALUE_789";
      Set<String> rejectedSet = Set.of(leakedSetElement);
      when(request.getRequestURI()).thenReturn("/api/v1/test-endpoint");

      MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
      BindingResult bindingResult = mock(BindingResult.class);
      FieldError fieldError = new FieldError(
          "itemDto", "tags", rejectedSet, false, null, null, "Tag " + leakedSetElement + " already registered");
      when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
      when(ex.getBindingResult()).thenReturn(bindingResult);

      ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().getDetails()).containsExactly("tags: is invalid");
      assertThat(response.getBody().getDetails().get(0)).doesNotContain(leakedSetElement);
    }

    @Test
    @DisplayName("URI Sanitization: Sensitive path segment (Aadhaar/sentinel) is sanitized to /sanitized-path")
    void testUriSanitization_sensitivePathSegment_sanitizesToSanitizedPath() {
      String sensitiveUri = "/api/v1/users/123456789012";
      when(request.getRequestURI()).thenReturn(sensitiveUri);

      BadRequestException ex = new BadRequestException("Identity verification failed", "INVALID_IDENTITY");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("123456789012");
    }

    @Test
    @DisplayName("URI Sanitization: Arbitrary user-controlled path segment is sanitized to /sanitized-path when no template exists")
    void testUriSanitization_arbitraryUserControlledPathSegment_sanitizesToSanitizedPath() {
      String arbitraryProbeUri = "/api/v1/users/arbitrary-user-controlled-slug-value";
      when(request.getRequestURI()).thenReturn(arbitraryProbeUri);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn(null);

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("arbitrary-user-controlled-slug-value");
    }

    @Test
    @DisplayName("URI Sanitization: Numeric path variable resolves to route template and never leaks raw numeric ID")
    void testUriSanitization_numericPathVariable_resolvesToRouteTemplateWithoutRawId() {
      String rawUriWithNumericId = "/api/v1/job-postings/42";
      when(request.getRequestURI()).thenReturn(rawUriWithNumericId);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/api/v1/job-postings/{id}");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/job-postings/{id}");
      assertThat(response.getBody().getPath()).doesNotContain("42");
    }

    @Test
    @DisplayName("URI Sanitization: Long numeric identifier never appears raw in returned error path")
    void testUriSanitization_longNumericIdentifier_resolvesToRouteTemplateWithoutLongId() {
      String rawUriWithLongId = "/api/v1/trainees/998877665544332211";
      when(request.getRequestURI()).thenReturn(rawUriWithLongId);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/api/v1/trainees/{id}");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/trainees/{id}");
      assertThat(response.getBody().getPath()).doesNotContain("998877665544332211");
    }

    @Test
    @DisplayName("URI Sanitization: Numeric path variable with missing route template falls back to /sanitized-path")
    void testUriSanitization_numericPathVariable_missingTemplateFallsBackToSanitizedPath() {
      String rawUriWithNumericId = "/api/v1/users/12345";
      when(request.getRequestURI()).thenReturn(rawUriWithNumericId);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn(null);

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("12345");
    }

    @Test
    @DisplayName("URI Sanitization: Route template containing sensitive data fails closed to /sanitized-path")
    void testUriSanitization_routeTemplateWithSensitiveData_failsClosedToSanitizedPath() {
      String rawUri = "/api/v1/auth/tokens/999";
      when(request.getRequestURI()).thenReturn(rawUri);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/api/v1/auth/tokens/SENTINEL_TOKEN_SECRET/{id}");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("SENTINEL");
    }

    @Test
    @DisplayName("URI Sanitization: Route template with untrusted prefix fails closed to /sanitized-path")
    void testUriSanitization_routeTemplateWithUntrustedPrefix_failsClosedToSanitizedPath() {
      String rawUri = "/internal/admin/42";
      when(request.getRequestURI()).thenReturn(rawUri);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/internal/admin/{id}");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("internal");
      assertThat(response.getBody().getPath()).doesNotContain("42");
    }

    @Test
    @DisplayName("URI Sanitization: Query parameters on route template are stripped by default")
    void testUriSanitization_routeTemplateWithQueryParams_stripsQueryParams() {
      String rawUri = "/api/v1/users/42?token=secret123";
      when(request.getRequestURI()).thenReturn(rawUri);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/api/v1/users/{id}?token=secret123");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/{id}");
      assertThat(response.getBody().getPath()).doesNotContain("token");
      assertThat(response.getBody().getPath()).doesNotContain("secret123");
    }

    @Test
    @DisplayName("URI Sanitization: Query parameters containing tokens or credentials are completely suppressed")
    void testUriSanitization_queryParamsContainingTokens_suppressesQueryParams() {
      String uriWithQuery = "/api/v1/users?token=superSecretTokenValue123&password=secretPassword456";
      when(request.getRequestURI()).thenReturn(uriWithQuery);

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users");
      assertThat(response.getBody().getPath()).doesNotContain("token");
      assertThat(response.getBody().getPath()).doesNotContain("password");
      assertThat(response.getBody().getPath()).doesNotContain("superSecretTokenValue123");
      assertThat(response.getBody().getPath()).doesNotContain("secretPassword456");
    }

    @Test
    @DisplayName("URI Sanitization: Untrusted route outside API prefix with no known sensitive pattern fails closed to /sanitized-path")
    void testUriSanitization_untrustedRouteNoSensitivePattern_failsClosedToSanitizedPath() {
      String untrustedUri = "/unmatched/probe/untrusted/endpoint";
      when(request.getRequestURI()).thenReturn(untrustedUri);

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/sanitized-path");
      assertThat(response.getBody().getPath()).doesNotContain("unmatched");
      assertThat(response.getBody().getPath()).doesNotContain("probe");
    }

    @Test
    @DisplayName("URI Sanitization: Safe known route template is preferred when available from HandlerMapping")
    void testUriSanitization_safeKnownRouteTemplate_resolvesTemplateWhenAttributePresent() {
      String rawUriWithSlug = "/api/v1/users/custom-slug-identifier";
      when(request.getRequestURI()).thenReturn(rawUriWithSlug);
      when(request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
          .thenReturn("/api/v1/users/{id}");

      BadRequestException ex = new BadRequestException("The request is invalid", "BAD_REQUEST");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleBaseException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/{id}");
      assertThat(response.getBody().getPath()).doesNotContain("custom-slug-identifier");
    }
  }
}
