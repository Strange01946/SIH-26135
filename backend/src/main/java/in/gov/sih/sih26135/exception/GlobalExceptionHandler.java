package in.gov.sih.sih26135.exception;

import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler for REST APIs.
 * Ensures consistent, sanitized error responses without leaking sensitive internal details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
      "(?i)(?:"
          + "SENTINEL_[A-Za-z0-9_]+"
          + "|\\b[0-9]{4}[ -]?[0-9]{4}[ -]?[0-9]{4}\\b"
          + "|\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
          + "|eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"
          + "|\\b(?:password|secret|credential|token|apikey)[\\s:=]+[^\\s,;]+"
          + "|\\b(?:SELECT|INSERT|UPDATE|DELETE|DROP)\\b.*\\b(?:FROM|INTO|SET|TABLE|WHERE)\\b"
          + ")"
  );

  private boolean containsSensitiveData(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    if (text.toUpperCase().contains("SENTINEL")) {
      return true;
    }
    return SENSITIVE_DATA_PATTERN.matcher(text).find();
  }

  private static final Pattern SAFE_STANDARD_VALIDATION_PATTERN = Pattern.compile(
      "^("
          + "must not be (null|blank|empty)"
          + "|is required"
          + "|must be a well-formed email address"
          + "|must be a valid (email|URL|url|date|ISO date)"
          + "|must be a (positive|negative) number"
          + "|must be (greater than|less than)( or equal to)? -?[0-9]+(\\.[0-9]+)?"
          + "|must be between -?[0-9]+(\\.[0-9]+)? and -?[0-9]+(\\.[0-9]+)?"
          + "|must be (true|false)"
          + "|must be (a )?(past|future) date( or present)?"
          + "|must be in the (past|future)( or present)?"
          + "|(size|length) must be between [0-9]+ and [0-9]+"
          + "|cannot exceed [0-9]+ characters"
          + "|numeric value out of bounds( \\(<[0-9]+ digits>\\.<[0-9]+ digits> expected\\))?"
          + "|is invalid"
          + "|invalid format"
          + "|size is out of allowed range"
          + ")$",
      Pattern.CASE_INSENSITIVE
  );

  private boolean containsRejectedValue(String message, Object rejectedValue) {
    if (message == null || message.isBlank() || rejectedValue == null) {
      return false;
    }
    String rejectedStr = String.valueOf(rejectedValue).trim();
    if (rejectedStr.isBlank()) {
      return false;
    }
    if (rejectedStr.length() == 1 && Character.isDigit(rejectedStr.charAt(0))) {
      return false;
    }
    if ("true".equalsIgnoreCase(rejectedStr) || "false".equalsIgnoreCase(rejectedStr)) {
      return false;
    }
    return message.toLowerCase().contains(rejectedStr.toLowerCase());
  }

  private boolean isSafeStandardValidationMessage(String propertyName, String message, Object rejectedValue) {
    if (message == null || message.isBlank()) {
      return false;
    }
    if (containsSensitiveData(message)) {
      return false;
    }
    if (containsRejectedValue(message, rejectedValue)) {
      return false;
    }
    String normalized = message.trim();
    if (propertyName != null && !propertyName.isBlank()) {
      String simpleName = propertyName.trim();
      int lastDot = simpleName.lastIndexOf('.');
      if (lastDot >= 0 && lastDot < simpleName.length() - 1) {
        simpleName = simpleName.substring(lastDot + 1);
      }
      String fullPrefix = propertyName.trim().toLowerCase() + " ";
      String simplePrefix = simpleName.toLowerCase() + " ";
      if (normalized.toLowerCase().startsWith(fullPrefix)) {
        normalized = normalized.substring(fullPrefix.length()).trim();
      } else if (normalized.toLowerCase().startsWith(simplePrefix)) {
        normalized = normalized.substring(simplePrefix.length()).trim();
      }
    }
    return SAFE_STANDARD_VALIDATION_PATTERN.matcher(normalized).matches();
  }

  private Object getSafeRejectedValue(FieldError fieldError) {
    if (fieldError == null) {
      return null;
    }
    try {
      return fieldError.getRejectedValue();
    } catch (Exception ignored) {
      return null;
    }
  }

  private Object getSafeInvalidValue(ConstraintViolation<?> violation) {
    if (violation == null) {
      return null;
    }
    try {
      return violation.getInvalidValue();
    } catch (Exception ignored) {
      return null;
    }
  }

  private String sanitizeFieldError(FieldError fieldError) {
    if (fieldError == null) {
      return "field: is invalid";
    }
    String field = fieldError.getField() != null ? fieldError.getField() : "field";
    String defaultMessage = fieldError.getDefaultMessage();
    Object rejectedValue = getSafeRejectedValue(fieldError);
    if (!isSafeStandardValidationMessage(field, defaultMessage, rejectedValue)) {
      return field + ": " + getSafeValidationMessage(fieldError.getCode());
    }
    return field + ": " + defaultMessage;
  }

  private String sanitizeObjectError(ObjectError objectError) {
    if (objectError instanceof FieldError fieldError) {
      return sanitizeFieldError(fieldError);
    }
    if (objectError == null) {
      return "object: is invalid";
    }
    String name = objectError.getObjectName() != null ? objectError.getObjectName() : "object";
    String defaultMessage = objectError.getDefaultMessage();
    if (!isSafeStandardValidationMessage(name, defaultMessage, null)) {
      return name + ": " + getSafeValidationMessage(objectError.getCode());
    }
    return name + ": " + defaultMessage;
  }

  private String sanitizeParameterError(String paramName, MessageSourceResolvable resolvable, Object rejectedValue) {
    if (resolvable == null) {
      return paramName + ": is invalid";
    }
    String message = resolvable.getDefaultMessage();
    String code = null;
    if (resolvable.getCodes() != null && resolvable.getCodes().length > 0) {
      for (String c : resolvable.getCodes()) {
        int dot = c.lastIndexOf('.');
        String simple = dot >= 0 ? c.substring(dot + 1) : c;
        if (!simple.isBlank()) {
          code = simple;
          break;
        }
      }
    }
    if (!isSafeStandardValidationMessage(paramName, message, rejectedValue)) {
      return paramName + ": " + getSafeValidationMessage(code);
    }
    return paramName + ": " + message;
  }

  private String getSafeValidationMessage(String code) {
    if (code == null) {
      return "is invalid";
    }
    return switch (code) {
      case "NotNull", "NotBlank", "NotEmpty" -> "must not be blank";
      case "Email" -> "must be a well-formed email address";
      case "Min", "DecimalMin" -> "must be greater than or equal to minimum value";
      case "Max", "DecimalMax" -> "must be less than or equal to maximum value";
      case "Positive" -> "must be greater than 0";
      case "PositiveOrZero" -> "must be greater than or equal to 0";
      case "Negative" -> "must be less than 0";
      case "NegativeOrZero" -> "must be less than or equal to 0";
      case "Size", "Length" -> "size is out of allowed range";
      case "Digits" -> "numeric value out of bounds";
      case "Pattern" -> "invalid format";
      case "Past", "PastOrPresent" -> "must be a date in the past or present";
      case "Future", "FutureOrPresent" -> "must be a date in the future or present";
      case "AssertTrue" -> "must be true";
      case "AssertFalse" -> "must be false";
      default -> "is invalid";
    };
  }

  private String sanitizeConstraintViolation(ConstraintViolation<?> violation) {
    if (violation == null) {
      return "property: is invalid";
    }
    String property = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "property";
    String message = violation.getMessage();
    Object invalidValue = getSafeInvalidValue(violation);
    if (!isSafeStandardValidationMessage(property, message, invalidValue)) {
      return property + ": " + getSafeConstraintMessage(violation);
    }
    return property + ": " + message;
  }

  private String getSafeConstraintMessage(ConstraintViolation<?> violation) {
    if (violation != null) {
      try {
        if (violation.getConstraintDescriptor() != null
            && violation.getConstraintDescriptor().getAnnotation() != null) {
          String annotationName = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
          return getSafeValidationMessage(annotationName);
        }
      } catch (Exception ignored) {
      }
    }
    return "is invalid";
  }

  private String getSafeMessageForErrorCode(String errorCode, HttpStatus status) {
    if (errorCode == null) {
      return status != null ? status.getReasonPhrase() : "Request processing failed";
    }
    return switch (errorCode) {
      case "INVALID_IDENTITY" -> "Identity verification failed. Please verify the provided identity details.";
      case "INVALID_CREDENTIALS" -> "Invalid credentials provided";
      case "UNAUTHORIZED" -> "Full authentication is required to access this resource";
      case "ACCESS_DENIED" -> "Access is denied";
      case "CONSENT_REQUIRED" -> "Trainee consent is required to perform this action";
      case "RESOURCE_NOT_FOUND" -> "The requested resource was not found";
      case "VALIDATION_ERROR" -> "Request validation failed";
      case "BAD_REQUEST" -> "The request is invalid";
      case "MALFORMED_REQUEST", "MALFORMED_JSON" -> "Malformed or unreadable request body";
      case "TYPE_MISMATCH" -> "Invalid parameter value provided";
      case "MISSING_PARAMETER" -> "Required parameter is missing";
      case "RESOURCE_CONFLICT" -> "A resource with the specified unique attributes already exists";
      case "DUPLICATE_PLACEMENT_EMPLOYMENT" -> "Placement record is already linked to another employment record";
      case "DUPLICATE_ENROLLMENT" -> "Trainee is already enrolled in this training batch";
      case "EXIT_EVENT_ALREADY_EXISTS" -> "Employment exit event already exists for this employment record";
      case "UNEMPLOYMENT_EVENT_NOT_ACTIVE" -> "Trainee does not have an active unemployment record to transition";
      case "OPEN_REQUEST_EXISTS" -> "An open verification request already exists for this employment record";
      case "ACTIVE_VERIFICATION_ALREADY_EXISTS" -> "Employment record already has an active verification";
      case "DUPLICATE_SURVEY_RESPONSE" -> "Survey response already exists for this survey, trainee, and attempt";
      case "DUPLICATE_PROVIDER_MESSAGE_ID" -> "Communication log already exists for this provider message ID";
      case "ACTIVE_SKILL_GAP_ALREADY_EXISTS" -> "Trainee already has an active current skill gap for this skill";
      case "ASSESSMENT_RESULT_ALREADY_EXISTS" -> "Assessment result already exists for this trainee assessment";
      case "CERTIFICATE_NUMBER_ALREADY_EXISTS" -> "Certificate number already exists";
      default -> {
        String readable = errorCode.replace('_', ' ').toLowerCase();
        yield "Request validation failed: " + readable;
      }
    };
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
    String clientMessage = ex.getMessage();
    if (clientMessage == null || clientMessage.isBlank()) {
      clientMessage = ex.getStatus().getReasonPhrase();
    } else if (ex.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
      clientMessage = "An unexpected error occurred. Please try again later.";
    } else if (containsSensitiveData(clientMessage)) {
      clientMessage = getSafeMessageForErrorCode(ex.getErrorCode(), ex.getStatus());
    }
    String exceptionType = !ex.getClass().getSimpleName().isEmpty()
        ? ex.getClass().getSimpleName()
        : ex.getClass().getName();
    log.warn("Application exception on {}: status={}, code={}, exception={}",
        request.getRequestURI(), ex.getStatus().value(), ex.getErrorCode(), exceptionType);
    ErrorResponse errorResponse = new ErrorResponse(
        ex.getStatus().value(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        clientMessage,
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> validationErrors = Stream.concat(
            ex.getBindingResult().getFieldErrors().stream().map(this::sanitizeFieldError),
            ex.getBindingResult().getGlobalErrors().stream().map(this::sanitizeObjectError)
        )
        .sorted()
        .toList();
    List<String> invalidFields = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getField)
        .distinct()
        .sorted()
        .toList();

    log.warn("Validation error on {}: status={}, code={}, invalidFields={}",
        request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidFields);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        request.getRequestURI(),
        validationErrors
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpServletRequest request) {

    List<String> validationErrors = new ArrayList<>();
    List<String> invalidParams = new ArrayList<>();

    List<ParameterValidationResult> results = new ArrayList<>();
    if (ex.getValueResults() != null) {
      results.addAll(ex.getValueResults());
    }
    if (ex.getBeanResults() != null) {
      results.addAll(ex.getBeanResults());
    }
    for (ParameterValidationResult result : results) {
        if (result == null) {
          continue;
        }
        String paramName = "parameter";
        if (result.getMethodParameter() != null) {
          if (result.getMethodParameter().getParameterName() == null) {
            result.getMethodParameter().initParameterNameDiscovery(new org.springframework.core.StandardReflectionParameterNameDiscoverer());
          }
          if (result.getMethodParameter().getParameterName() != null) {
            paramName = result.getMethodParameter().getParameterName();
          }
        }
        invalidParams.add(paramName);

        if (result.getResolvableErrors() != null) {
          for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
            if (resolvable instanceof FieldError fieldError) {
              validationErrors.add(sanitizeFieldError(fieldError));
            } else if (resolvable instanceof ObjectError objectError) {
              validationErrors.add(sanitizeObjectError(objectError));
            } else {
              validationErrors.add(sanitizeParameterError(paramName, resolvable, result.getArgument()));
            }
          }
        }
      }

    Collections.sort(validationErrors);
    Collections.sort(invalidParams);

    log.warn("Handler method validation error on {}: status={}, code={}, invalidParams={}",
        request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidParams);

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        request.getRequestURI(),
        validationErrors
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request) {

    // Evaluate all violations: classify as HTTP 400 only when origin is positively identifiable
    // as supported client request validation (controller method parameter or request DTO).
    // Any entity/persistence, internal service/repository, unknown origin, or null root bean fails closed as HTTP 500.
    if (!isSupportedClientRequestValidation(ex)) {
      log.error("Internal or unverified constraint violation on {}: status={}, code={}, failing closed",
          request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR");
      ErrorResponse errorResponse = new ErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
          "INTERNAL_SERVER_ERROR",
          "An unexpected error occurred. Please try again later.",
          request.getRequestURI()
      );
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Confirmed request / method parameter validation failure: map to HTTP 400 Bad Request
    List<String> validationErrors = ex.getConstraintViolations().stream()
        .map(this::sanitizeConstraintViolation)
        .sorted()
        .toList();
    List<String> invalidProperties = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown")
        .distinct()
        .sorted()
        .toList();

    log.warn("Request constraint violation on {}: status={}, code={}, invalidProperties={}",
        request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", invalidProperties);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "VALIDATION_ERROR",
        "Request validation failed",
        request.getRequestURI(),
        validationErrors
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  private boolean isSupportedClientRequestValidation(ConstraintViolationException ex) {
    if (ex.getConstraintViolations() == null || ex.getConstraintViolations().isEmpty()) {
      return false;
    }

    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      if (violation == null) {
        return false;
      }

      Class<?> rootBeanClass = violation.getRootBeanClass();
      if (rootBeanClass == null) {
        // Unknown or missing root bean origin -> fail closed to HTTP 500
        return false;
      }

      // Explicitly reject entity / persistence-layer origins
      if (rootBeanClass.isAnnotationPresent(Entity.class)
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.entity.")) {
        return false;
      }

      // Explicitly reject internal service or repository layer origins
      if (rootBeanClass.isAnnotationPresent(Service.class)
          || rootBeanClass.isAnnotationPresent(Repository.class)
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.service.")
          || rootBeanClass.getName().startsWith("in.gov.sih.sih26135.repository.")) {
        return false;
      }

      // Positively identify supported Request DTOs from verified packages only
      if (isVerifiedRequestDto(rootBeanClass)) {
        continue;
      }

      // Positively identify supported project API Controller with ElementKind.PARAMETER node
      if (isVerifiedProjectController(rootBeanClass) && hasParameterNode(violation)) {
        continue;
      }

      // Unidentified origin (unknown class, unverified third-party controller, controller without parameter node, or internal helper) fails closed
      return false;
    }

    return true;
  }

  /**
   * Authoritative allowlist of verified application controller classes.
   * Merely declaring an approved package, class name ending with "Controller", or controller annotations
   * cannot spoof trust. Only authentic compiled project controllers are trusted.
   */
  private static final Set<Class<?>> TRUSTED_CONTROLLER_CLASSES = Set.of(
      // in.gov.sih.sih26135.controller
      in.gov.sih.sih26135.controller.AssessmentController.class,
      in.gov.sih.sih26135.controller.AssessmentResultController.class,
      in.gov.sih.sih26135.controller.AttendanceRecordController.class,
      in.gov.sih.sih26135.controller.AuditLogController.class,
      in.gov.sih.sih26135.controller.CertificationController.class,
      in.gov.sih.sih26135.controller.ConsentController.class,
      in.gov.sih.sih26135.controller.CourseController.class,
      in.gov.sih.sih26135.controller.DataQualityIssueController.class,
      in.gov.sih.sih26135.controller.DataQualityIssueEventController.class,
      in.gov.sih.sih26135.controller.DataQualityRuleController.class,
      in.gov.sih.sih26135.controller.EmployerController.class,
      in.gov.sih.sih26135.controller.EmploymentExitEventController.class,
      in.gov.sih.sih26135.controller.EmploymentRecordController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationAttemptController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationEvidenceController.class,
      in.gov.sih.sih26135.controller.EmploymentVerificationRequestController.class,
      in.gov.sih.sih26135.controller.FollowupCampaignController.class,
      in.gov.sih.sih26135.controller.FollowupTaskController.class,
      in.gov.sih.sih26135.controller.ImportBatchController.class,
      in.gov.sih.sih26135.controller.ImportRecordController.class,
      in.gov.sih.sih26135.controller.JobApplicationController.class,
      in.gov.sih.sih26135.controller.JobPostingController.class,
      in.gov.sih.sih26135.controller.PermissionController.class,
      in.gov.sih.sih26135.controller.PlacementRecordController.class,
      in.gov.sih.sih26135.controller.ProgramController.class,
      in.gov.sih.sih26135.controller.RoleController.class,
      in.gov.sih.sih26135.controller.SalaryHistoryController.class,
      in.gov.sih.sih26135.controller.SchemeController.class,
      in.gov.sih.sih26135.controller.SkillGapAssessmentController.class,
      in.gov.sih.sih26135.controller.SkillGapController.class,
      in.gov.sih.sih26135.controller.SkillGapObservationController.class,
      in.gov.sih.sih26135.controller.SkillGapRecommendationController.class,
      in.gov.sih.sih26135.controller.SurveyQuestionController.class,
      in.gov.sih.sih26135.controller.SurveyResponseAnswerController.class,
      in.gov.sih.sih26135.controller.SurveyResponseController.class,
      in.gov.sih.sih26135.controller.SystemEventLogController.class,
      in.gov.sih.sih26135.controller.TraineeAssessmentController.class,
      in.gov.sih.sih26135.controller.TraineeController.class,
      in.gov.sih.sih26135.controller.TraineeUnemploymentEventController.class,
      in.gov.sih.sih26135.controller.TrainingBatchController.class,
      in.gov.sih.sih26135.controller.TrainingCenterController.class,
      in.gov.sih.sih26135.controller.TrainingEnrollmentController.class,
      in.gov.sih.sih26135.controller.TrainingProviderController.class,
      in.gov.sih.sih26135.controller.UserController.class,
      // in.gov.sih.sih26135.controller.analytics
      in.gov.sih.sih26135.controller.analytics.AnalyticsFactController.class,
      in.gov.sih.sih26135.controller.analytics.AnalyticsSummaryController.class,
      // in.gov.sih.sih26135.controller.auth
      in.gov.sih.sih26135.controller.auth.AuthController.class,
      // in.gov.sih.sih26135.controller.workflow
      in.gov.sih.sih26135.controller.workflow.EmploymentLifecycleWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.FollowupEngagementWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.PlacementEmploymentWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.SkillGapRemediationWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.TrainingOutcomeWorkflowController.class,
      in.gov.sih.sih26135.controller.workflow.VerificationWorkflowController.class
  );

  /**
   * Authoritative allowlist of verified client request DTO classes.
   * Merely declaring a whitelisted package or class name ending with "Request" cannot spoof trust.
   */
  private static final Set<Class<?>> TRUSTED_REQUEST_DTO_CLASSES = Set.of(
      // in.gov.sih.sih26135.dto.request
      in.gov.sih.sih26135.dto.request.AssignCourseProgramRequest.class,
      in.gov.sih.sih26135.dto.request.AssignCourseSkillRequest.class,
      in.gov.sih.sih26135.dto.request.AssignJobRoleSkillRequest.class,
      in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.AssignRolePermissionRequest.class,
      in.gov.sih.sih26135.dto.request.AssignUserRoleRequest.class,
      in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateAuditLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCertificationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateCourseRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.CreateDataQualityRuleRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmployerRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest.class,
      in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest.class,
      in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest.class,
      in.gov.sih.sih26135.dto.request.CreateImportBatchRequest.class,
      in.gov.sih.sih26135.dto.request.CreateImportRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobPostingRequest.class,
      in.gov.sih.sih26135.dto.request.CreateJobRoleRequest.class,
      in.gov.sih.sih26135.dto.request.CreatePermissionRequest.class,
      in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest.class,
      in.gov.sih.sih26135.dto.request.CreateProgramRequest.class,
      in.gov.sih.sih26135.dto.request.CreateRoleRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSchemeRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapObservationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillGapRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSkillRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.CreateUserRequest.class,
      in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest.class,
      in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest.class,
      in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest.class,
      in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest.class,
      in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest.class,
      in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest.class,
      in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.TerminateEmpanelmentRequest.class,
      in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest.class,
      in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAssessmentResultRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateAttendanceRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCertificationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCourseRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateCourseSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateDataQualityRuleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmployerRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentExitEventRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationEvidenceRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateFollowupCampaignRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobApplicationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobPostingRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobRoleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateJobRoleSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateProgramRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateRoleRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSalaryHistoryRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSchemeRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillCategoryRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapObservationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillGapRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSkillRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyQuestionRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyResponseAnswerRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeAssessmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest.class,
      in.gov.sih.sih26135.dto.request.UpdateUserRequest.class,
      // in.gov.sih.sih26135.dto.auth
      in.gov.sih.sih26135.dto.auth.LoginRequest.class,
      in.gov.sih.sih26135.dto.auth.RefreshTokenRequest.class
  );

  private boolean isVerifiedProjectController(Class<?> rootBeanClass) {
    if (rootBeanClass == null) {
      return false;
    }
    // Fail closed against package-spoofing: require inclusion in the explicit allowlist of known project controllers.
    // Merely declaring a whitelisted package, class suffix "Controller", or controller annotations is insufficient.
    return TRUSTED_CONTROLLER_CLASSES.contains(rootBeanClass);
  }

  private boolean isVerifiedRequestDto(Class<?> rootBeanClass) {
    if (rootBeanClass == null) {
      return false;
    }
    // Fail closed against package-spoofing: require inclusion in the explicit allowlist of known request DTOs.
    // Merely declaring a whitelisted package or class suffix "Request" is insufficient.
    return TRUSTED_REQUEST_DTO_CLASSES.contains(rootBeanClass);
  }

  private boolean hasParameterNode(ConstraintViolation<?> violation) {
    Path path = violation.getPropertyPath();
    if (path == null) {
      return false;
    }
    try {
      for (Path.Node node : path) {
        if (node != null && node.getKind() == ElementKind.PARAMETER) {
          return true;
        }
      }
    } catch (Exception ignored) {
      return false;
    }
    return false;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {
    String paramName = ex.getName();
    String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type";
    String message = String.format("Invalid parameter value for '%s'. Expected type: %s", paramName, requiredType);

    log.warn("Parameter type mismatch on {} for parameter '{}': status={}, code={}, expectedType={}",
        request.getRequestURI(), paramName, HttpStatus.BAD_REQUEST.value(), "TYPE_MISMATCH", requiredType);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "TYPE_MISMATCH",
        message,
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpServletRequest request) {
    String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

    log.warn("Missing request parameter on {}: status={}, code={}, parameter='{}'",
        request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), "MISSING_PARAMETER", ex.getParameterName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MISSING_PARAMETER",
        message,
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingPathVariableException.class)
  public ResponseEntity<ErrorResponse> handleMissingPathVariable(
      MissingPathVariableException ex,
      HttpServletRequest request) {
    // MissingPathVariableException indicates a server-side route/controller parameter mismatch
    // (a controller method expects a URI variable not defined in its @RequestMapping pattern).
    // This is an internal configuration/server error (HTTP 500), not an invalid client request.
    log.error("Server-side path variable configuration mismatch on {}: status={}, code={}, variable='{}', exception={}",
        request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", ex.getVariableName(), ex.getClass().getSimpleName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Malformed HTTP message on {}: status={}, code={}",
        request.getRequestURI(), HttpStatus.BAD_REQUEST.value(), "MALFORMED_JSON");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "MALFORMED_JSON",
        "Malformed or unreadable request body",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    log.warn("Method not supported on {}: status={}, code={}, method={}",
        request.getRequestURI(), HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED", ex.getMethod());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED.value(),
        HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
        "METHOD_NOT_ALLOWED",
        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpServletRequest request) {
    String contentType = ex.getContentType() != null ? ex.getContentType().toString() : "unknown";
    log.warn("Media type not supported on {}: status={}, code={}, contentType={}",
        request.getRequestURI(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "UNSUPPORTED_MEDIA_TYPE", contentType);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
        "UNSUPPORTED_MEDIA_TYPE",
        "Content type '" + ex.getContentType() + "' is not supported",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex,
      HttpServletRequest request) {
    log.warn("Media type not acceptable on {}: status={}, code={}",
        request.getRequestURI(), HttpStatus.NOT_ACCEPTABLE.value(), "NOT_ACCEPTABLE");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.NOT_ACCEPTABLE.value(),
        HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(),
        "NOT_ACCEPTABLE",
        "Requested media type is not acceptable",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
    log.warn("Resource not found on {}: status={}, code={}",
        request.getRequestURI(), HttpStatus.NOT_FOUND.value(), "ENDPOINT_NOT_FOUND");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.NOT_FOUND.value(),
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "ENDPOINT_NOT_FOUND",
        "No static resource or endpoint found for path: " + request.getRequestURI(),
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public void handleAccessDeniedException(AccessDeniedException ex) {
    throw ex;
  }

  @ExceptionHandler(AuthenticationException.class)
  public void handleAuthenticationException(AuthenticationException ex) {
    throw ex;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex,
      HttpServletRequest request) {
    String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
    String errorMsg = rootMsg != null ? rootMsg : "";

    // 1. Employment spell and employment records
    if (errorMsg.contains("uk_employment_records_current_per_type")) {
      return conflictResponse("ACTIVE_SPELL_ALREADY_EXISTS",
          "Trainee already has an active current spell for this engagement type", request);
    }
    if (errorMsg.contains("uk_employment_records_placement")) {
      return conflictResponse("DUPLICATE_PLACEMENT_EMPLOYMENT",
          "Placement record is already linked to another employment record", request);
    }
    if (errorMsg.contains("uk_employment_records_number")) {
      return conflictResponse("EMPLOYMENT_NUMBER_ALREADY_EXISTS",
          "Employment number already exists", request);
    }
    if (errorMsg.contains("uk_employment_records_trainee_employer_start")) {
      return conflictResponse("DUPLICATE_TRAINEE_EMPLOYER_START",
          "Employment record already exists for trainee, employer and start date", request);
    }
    if (errorMsg.contains("uk_employment_exit_events_employment")) {
      return conflictResponse("EXIT_EVENT_ALREADY_EXISTS",
          "Employment exit event already exists for this employment record", request);
    }
    if (errorMsg.contains("uk_salary_history_employment_from")) {
      return conflictResponse("DUPLICATE_EMPLOYMENT_EFFECTIVE_FROM",
          "Salary history already exists for this employment record and effective from date", request);
    }
    if (errorMsg.contains("uk_salary_history_employment_milestone")) {
      return conflictResponse("DUPLICATE_EMPLOYMENT_MILESTONE",
          "Salary history already exists for this employment record and observation milestone", request);
    }

    // 2. Employment verification requests
    if (errorMsg.contains("uk_emp_verif_requests_open")) {
      return conflictResponse("OPEN_REQUEST_EXISTS",
          "An open verification request already exists for this employment record", request);
    }
    if (errorMsg.contains("uk_emp_verif_requests_cycle")) {
      return conflictResponse("DUPLICATE_CYCLE_NUMBER",
          "Verification request for this employment record and cycle number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verif_requests_number")) {
      return conflictResponse("DUPLICATE_REQUEST_NUMBER",
          "Request number already exists", request);
    }

    // 3. Employment verification records (Phase 25.4)
    if (errorMsg.contains("uk_emp_verifications_current")) {
      return conflictResponse("ACTIVE_VERIFICATION_ALREADY_EXISTS",
          "Employment record already has an active verification", request);
    }
    if (errorMsg.contains("uk_emp_verifications_cycle")) {
      return conflictResponse("DUPLICATE_VERIFICATION_CYCLE",
          "Verification for this employment record and cycle number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verifications_number")) {
      return conflictResponse("DUPLICATE_VERIFICATION_NUMBER",
          "Verification number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verifications_request")) {
      return conflictResponse("DUPLICATE_VERIFICATION_REQUEST",
          "Verification record already exists for this verification request", request);
    }

    // 4. Unemployment events
    if (errorMsg.contains("uk_unemployment_events_current")) {
      return conflictResponse("ACTIVE_PERIOD_ALREADY_EXISTS",
          "Trainee already has an active unemployment period", request);
    }
    if (errorMsg.contains("uk_unemployment_events_trainee_period")) {
      return conflictResponse("DUPLICATE_UNEMPLOYMENT_PERIOD",
          "Unemployment event already exists for this trainee and period", request);
    }
    if (errorMsg.contains("uk_unemployment_events_trainee_start")) {
      return conflictResponse("UNEMPLOYMENT_START_DATE_EXISTS",
          "An unemployment event already exists for trainee on start date", request);
    }
    if (errorMsg.contains("uk_unemployment_events_exit")) {
      return conflictResponse("DUPLICATE_EXIT_UNEMPLOYMENT",
          "Unemployment event already exists for this exit event", request);
    }

    // 5. Skill gaps and recommendations (Phase 25.5)
    if (errorMsg.contains("uk_skill_gaps_current")) {
      return conflictResponse("ACTIVE_SKILL_GAP_ALREADY_EXISTS",
          "Trainee already has an active current skill gap for this skill", request);
    }
    if (errorMsg.contains("uk_skill_gaps_assessment_skill")) {
      return conflictResponse("DUPLICATE_ASSESSMENT_SKILL_GAP",
          "Skill gap already exists for this assessment and skill", request);
    }
    if (errorMsg.contains("uk_skill_gaps_number")) {
      return conflictResponse("DUPLICATE_SKILL_GAP_NUMBER",
          "Skill gap number already exists", request);
    }
    if (errorMsg.contains("uk_skill_gap_assessments_number")) {
      return conflictResponse("DUPLICATE_ASSESSMENT_NUMBER",
          "Skill gap assessment number already exists", request);
    }
    if (errorMsg.contains("uk_sgr_number")) {
      return conflictResponse("DUPLICATE_RECOMMENDATION_NUMBER",
          "Skill gap recommendation number already exists", request);
    }
    if (errorMsg.contains("uk_sgr_gap_action_course")) {
      return conflictResponse("DUPLICATE_GAP_ACTION_COURSE",
          "A recommendation with this action type and course already exists for this skill gap", request);
    }

    // 6. Training enrollments, batches, attendance, courses
    if (errorMsg.contains("uk_training_enrollments_trainee_batch")) {
      return conflictResponse("DUPLICATE_ENROLLMENT",
          "Trainee is already enrolled in this training batch", request);
    }
    if (errorMsg.contains("uk_training_enrollments_number")) {
      return conflictResponse("DUPLICATE_ENROLLMENT_NUMBER",
          "Training enrollment number already exists", request);
    }
    if (errorMsg.contains("uk_training_batches_code")) {
      return conflictResponse("BATCH_CODE_ALREADY_EXISTS",
          "Batch code already exists", request);
    }
    if (errorMsg.contains("uk_attendance_enrollment_session")) {
      return conflictResponse("ATTENDANCE_ALREADY_EXISTS",
          "Attendance record already exists for this enrollment, date, and sequence", request);
    }
    if (errorMsg.contains("uk_courses_code")) {
      return conflictResponse("COURSE_CODE_ALREADY_EXISTS",
          "Course code already exists", request);
    }
    if (errorMsg.contains("uk_training_centers_code")) {
      return conflictResponse("CENTER_CODE_ALREADY_EXISTS",
          "Center code already exists", request);
    }
    if (errorMsg.contains("uk_training_providers_code")) {
      return conflictResponse("PROVIDER_CODE_ALREADY_EXISTS",
          "Provider code already exists", request);
    }
    if (errorMsg.contains("uk_training_providers_registration")) {
      return conflictResponse("REGISTRATION_NUMBER_ALREADY_EXISTS",
          "Registration number already exists", request);
    }

    // 7. Users and identity
    if (errorMsg.contains("uk_users_username")) {
      return conflictResponse("USERNAME_ALREADY_EXISTS",
          "Username already exists", request);
    }
    if (errorMsg.contains("uk_users_email")) {
      return conflictResponse("EMAIL_ALREADY_EXISTS",
          "Email already exists", request);
    }
    if (errorMsg.contains("uk_trainees_registration_number")) {
      return conflictResponse("DUPLICATE_TRAINEE_REGISTRATION",
          "Trainee registration number already exists", request);
    }
    if (errorMsg.contains("uk_trainees_user_id")) {
      return conflictResponse("USER_ALREADY_LINKED",
          "Trainee already linked to this user ID", request);
    }

    // 8. Placements, surveys, follow-ups
    if (errorMsg.contains("uk_placement_records_number")) {
      return conflictResponse("PLACEMENT_NUMBER_ALREADY_EXISTS",
          "Placement number already exists", request);
    }
    if (errorMsg.contains("uk_placement_records_application")) {
      return conflictResponse("DUPLICATE_PLACEMENT_APPLICATION",
          "Placement record already exists for this job application", request);
    }
    if (errorMsg.contains("uk_placement_records_enrollment_posting")) {
      return conflictResponse("DUPLICATE_PLACEMENT_ENROLLMENT_POSTING",
          "Placement record already exists for this enrollment and job posting", request);
    }
    if (errorMsg.contains("uk_job_applications_trainee_posting")) {
      return conflictResponse("DUPLICATE_JOB_APPLICATION",
          "Trainee has already applied for this job posting", request);
    }
    if (errorMsg.contains("uk_job_postings_code")) {
      return conflictResponse("POSTING_CODE_ALREADY_EXISTS",
          "Job posting code already exists", request);
    }
    if (errorMsg.contains("uk_followup_campaigns_code")) {
      return conflictResponse("CAMPAIGN_CODE_ALREADY_EXISTS",
          "Campaign code already exists", request);
    }
    if (errorMsg.contains("uk_followup_tasks_campaign_trainee")) {
      return conflictResponse("DUPLICATE_FOLLOWUP_TASK",
          "Follow-up task already exists for this campaign and trainee", request);
    }
    if (errorMsg.contains("uk_survey_responses_attempt")) {
      return conflictResponse("DUPLICATE_SURVEY_RESPONSE",
          "Survey response already exists for this survey, trainee, and attempt", request);
    }
    if (errorMsg.contains("uk_survey_response_answers_question")) {
      return conflictResponse("DUPLICATE_RESPONSE_ANSWER",
          "Answer already exists for this survey response and question", request);
    }
    if (errorMsg.contains("uk_communication_logs_provider_message")) {
      return conflictResponse("DUPLICATE_PROVIDER_MESSAGE_ID",
          "Communication log already exists for this provider message ID", request);
    }

    // 9. Assessments and Certifications (Phase 25.10)
    if (errorMsg.contains("uk_assessments_code")) {
      return conflictResponse("ASSESSMENT_CODE_ALREADY_EXISTS",
          "Assessment code already exists", request);
    }
    if (errorMsg.contains("uk_trainee_assessments_attempt")) {
      return conflictResponse("TRAINEE_ASSESSMENT_ATTEMPT_ALREADY_EXISTS",
          "Trainee assessment attempt already exists", request);
    }
    if (errorMsg.contains("uk_assessment_results_attempt")) {
      return conflictResponse("ASSESSMENT_RESULT_ALREADY_EXISTS",
          "Assessment result already exists for this trainee assessment", request);
    }
    if (errorMsg.contains("uk_certifications_certificate_number")) {
      return conflictResponse("CERTIFICATE_NUMBER_ALREADY_EXISTS",
          "Certificate number already exists", request);
    }

    // 10. Employers and Organizations
    if (errorMsg.contains("uk_employers_code")) {
      return conflictResponse("EMPLOYER_CODE_ALREADY_EXISTS",
          "Employer code already exists", request);
    }
    if (errorMsg.contains("uk_employers_registration")) {
      return conflictResponse("REGISTRATION_NUMBER_ALREADY_EXISTS",
          "Registration number already exists", request);
    }
    if (errorMsg.contains("uk_employers_gstin")) {
      return conflictResponse("GSTIN_ALREADY_EXISTS",
          "GSTIN already exists", request);
    }

    // 11. Security, Roles, Skills, Schemes, Programs, Data Quality
    if (errorMsg.contains("uk_roles_code")) {
      return conflictResponse("ROLE_CODE_ALREADY_EXISTS",
          "Role code already exists", request);
    }
    if (errorMsg.contains("uk_permissions_code")) {
      return conflictResponse("PERMISSION_CODE_ALREADY_EXISTS",
          "Permission code already exists", request);
    }
    if (errorMsg.contains("uk_job_roles_code")) {
      return conflictResponse("JOB_ROLE_CODE_ALREADY_EXISTS",
          "Job role code already exists", request);
    }
    if (errorMsg.contains("uk_skills_code")) {
      return conflictResponse("SKILL_CODE_ALREADY_EXISTS",
          "Skill code already exists", request);
    }
    if (errorMsg.contains("uk_skills_name")) {
      return conflictResponse("SKILL_NAME_ALREADY_EXISTS",
          "Skill name already exists", request);
    }
    if (errorMsg.contains("uk_programs_code")) {
      return conflictResponse("PROGRAM_CODE_ALREADY_EXISTS",
          "Program code already exists", request);
    }
    if (errorMsg.contains("uk_schemes_code")) {
      return conflictResponse("SCHEME_CODE_ALREADY_EXISTS",
          "Scheme code already exists", request);
    }
    if (errorMsg.contains("uk_data_quality_rules_code")) {
      return conflictResponse("RULE_CODE_EXISTS",
          "Rule code already exists", request);
    }
    if (errorMsg.contains("uk_import_batches_code")) {
      return conflictResponse("BATCH_CODE_EXISTS",
          "Batch code already exists", request);
    }

    // 12. Additional domain unique constraints
    if (errorMsg.contains("uk_emp_verif_attempts_number")) {
      return conflictResponse("DUPLICATE_VERIFICATION_ATTEMPT_NUMBER",
          "Verification attempt number already exists", request);
    }
    if (errorMsg.contains("uk_emp_verif_evidence_reference")) {
      return conflictResponse("DUPLICATE_EVIDENCE_REFERENCE",
          "Verification evidence reference already exists", request);
    }
    if (errorMsg.contains("uk_employer_branches_code")) {
      return conflictResponse("BRANCH_CODE_ALREADY_EXISTS",
          "Employer branch code already exists", request);
    }
    if (errorMsg.contains("uk_import_records_batch_row")) {
      return conflictResponse("DUPLICATE_IMPORT_ROW",
          "Import record already exists for this batch and row", request);
    }
    if (errorMsg.contains("uk_skill_gap_observations_number")) {
      return conflictResponse("DUPLICATE_OBSERVATION_NUMBER",
          "Skill gap observation number already exists", request);
    }
    if (errorMsg.contains("uk_survey_questions_version_code")) {
      return conflictResponse("DUPLICATE_QUESTION_CODE",
          "Survey question already exists for this survey, version, and question code", request);
    }
    if (errorMsg.contains("uk_survey_questions_version_order")) {
      return conflictResponse("DUPLICATE_QUESTION_ORDER",
          "Survey question order already exists for this survey, version, and order number", request);
    }

    // 13. Generic unique constraint / duplicate entry fallback
    if (errorMsg.contains("Duplicate entry") || errorMsg.contains("duplicate key")) {
      return conflictResponse("RESOURCE_CONFLICT",
          "A resource with the specified unique attributes already exists", request);
    }

    // 14. Non-duplicate integrity violations (foreign keys, nulls, check constraints)
    log.error("Unrecognized database integrity violation on {}: status={}, code={}",
        request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected database error occurred. Please try again later.",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> conflictResponse(String code, String message, HttpServletRequest request) {
    log.warn("Database integrity conflict on {}: status={}, code={}",
        request.getRequestURI(), HttpStatus.CONFLICT.value(), code);
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.CONFLICT.value(),
        HttpStatus.CONFLICT.getReasonPhrase(),
        code,
        message,
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception processing request to {}: status={}, code={}, exception={}",
        request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", ex.getClass().getName());
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred. Please try again later.",
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
