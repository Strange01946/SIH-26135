package in.gov.sih.sih26135.exception;

import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
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

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
    log.warn("Application exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(
        ex.getStatus().value(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        ex.getMessage(),
        request.getRequestURI()
    );
    return new ResponseEntity<>(errorResponse, ex.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .toList();

    log.warn("Validation error on {}: {}", request.getRequestURI(), validationErrors);
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
      log.error("Internal or unverified constraint violation on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
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
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .toList();

    log.warn("Request constraint violation on {}: {}", request.getRequestURI(), validationErrors);
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

    log.warn("Parameter type mismatch on {} for parameter '{}': {}", request.getRequestURI(), paramName, ex.getMessage());
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

    log.warn("Missing request parameter on {}: {}", request.getRequestURI(), ex.getMessage());
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
    log.error("Server-side path variable configuration mismatch on {}: variable '{}' is missing from URI template mapping",
        request.getRequestURI(), ex.getVariableName(), ex);
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
    log.warn("Malformed HTTP message on {}: {}", request.getRequestURI(), ex.getMessage());
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
    log.warn("Method not supported on {}: {}", request.getRequestURI(), ex.getMethod());
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
    log.warn("Media type not supported on {}: {}", request.getRequestURI(), ex.getContentType());
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
    log.warn("Media type not acceptable on {}: {}", request.getRequestURI(), ex.getMessage());
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
    log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getResourcePath());
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
    log.warn("Database integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());

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

    // 9. Generic unique constraint / duplicate entry fallback
    if (errorMsg.contains("Duplicate entry") || errorMsg.contains("duplicate key")) {
      return conflictResponse("RESOURCE_CONFLICT",
          "A resource with the specified unique attributes already exists", request);
    }

    // 10. Non-duplicate integrity violations (foreign keys, nulls, check constraints)
    log.error("Unrecognized database integrity violation on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
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
    log.error("Unhandled exception processing request to {}", request.getRequestURI(), ex);
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
