package in.gov.sih.sih26135.controller.workflow;

import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentToEmploymentTransitionResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for orchestrating Employment Lifecycle and Career Timeline workflows.
 *
 * <p>Coordinates cross-domain transitions between active employment exit, unemployment
 * period initiation, re-employment spells, and comprehensive longitudinal career timeline tracking.
 *
 * <p>Base Route: /api/v1/workflows/employment-lifecycle (alias: /api/v1/workflows/employment-lifecycles)
 */
@RestController
@RequestMapping({"/api/v1/workflows/employment-lifecycle", "/api/v1/workflows/employment-lifecycles"})
@PreAuthorize("hasAuthority('employment.manage')")
public class EmploymentLifecycleWorkflowController {

  private final EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService;

  public EmploymentLifecycleWorkflowController(
      EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService) {
    this.employmentLifecycleWorkflowService = employmentLifecycleWorkflowService;
  }

  /**
   * Records an employment exit event and conditionally initiates a trainee unemployment period.
   *
   * @param request workflow payload containing exit event details and optional unemployment initiation specs
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with EmploymentExitAndUnemploymentResponse enveloped in ApiResponse
   */
  @PostMapping("/record-exit-and-initiate-unemployment")
  public ResponseEntity<ApiResponse<EmploymentExitAndUnemploymentResponse>> recordExitAndInitiateUnemployment(
      @RequestBody EmploymentExitAndUnemploymentRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    EmploymentExitAndUnemploymentResponse response =
        employmentLifecycleWorkflowService.recordExitAndInitiateUnemployment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment exit recorded and unemployment initiated successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Transitions an existing unemployment spell into a new active employment spell.
   *
   * @param request workflow payload containing unemployment event ID, new employment specs, and initial salary
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with UnemploymentToEmploymentTransitionResponse enveloped in ApiResponse
   */
  @PostMapping("/transition-unemployment-to-employment")
  public ResponseEntity<ApiResponse<UnemploymentToEmploymentTransitionResponse>> transitionUnemploymentToEmployment(
      @RequestBody UnemploymentToEmploymentTransitionRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    UnemploymentToEmploymentTransitionResponse response =
        employmentLifecycleWorkflowService.transitionUnemploymentToEmployment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Unemployment transitioned to employment successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Retrieves the comprehensive longitudinal career timeline for a specific trainee.
   *
   * @param traineeId unique identifier of the trainee
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with TraineeCareerTimelineResponse enveloped in ApiResponse
   */
  @GetMapping("/timeline")
  public ResponseEntity<ApiResponse<TraineeCareerTimelineResponse>> getTraineeCareerTimeline(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    TraineeCareerTimelineResponse response =
        employmentLifecycleWorkflowService.getTraineeCareerTimeline(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }
}
