package in.gov.sih.sih26135.controller.workflow;

import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
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
 * REST controller for orchestrating Placement to Employment transition workflows.
 *
 * <p>Coordinates cross-domain transitions from placement offer verification to
 * employment record creation and initial salary history registration.
 *
 * <p>Base Route: /api/v1/workflows/placement-employment (alias: /api/v1/workflows/placement-employments)
 */
@RestController
@RequestMapping({"/api/v1/workflows/placement-employment", "/api/v1/workflows/placement-employments"})
@PreAuthorize("hasAnyAuthority('placement.manage', 'employment.manage')")
public class PlacementEmploymentWorkflowController {

  private final PlacementEmploymentWorkflowService placementEmploymentWorkflowService;

  public PlacementEmploymentWorkflowController(
      PlacementEmploymentWorkflowService placementEmploymentWorkflowService) {
    this.placementEmploymentWorkflowService = placementEmploymentWorkflowService;
  }

  /**
   * Transitions a confirmed placement record to an active employment record and logs initial salary.
   *
   * @param request workflow payload containing placement transition details, employment specs, and salary history
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with PlacementToEmploymentTransitionResponse enveloped in ApiResponse
   */
  @PostMapping("/transition-placement-to-employment")
  public ResponseEntity<ApiResponse<PlacementToEmploymentTransitionResponse>> transitionPlacementToEmployment(
      @RequestBody PlacementToEmploymentTransitionRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    PlacementToEmploymentTransitionResponse response =
        placementEmploymentWorkflowService.transitionPlacementToEmployment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Placement transitioned to employment successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Retrieves an aggregate placement and employment summary for a specific trainee.
   *
   * @param traineeId unique identifier of the trainee
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with TraineePlacementEmploymentSummaryResponse enveloped in ApiResponse
   */
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<TraineePlacementEmploymentSummaryResponse>> getTraineePlacementEmploymentSummary(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    TraineePlacementEmploymentSummaryResponse response =
        placementEmploymentWorkflowService.getTraineePlacementEmploymentSummary(traineeId);
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
