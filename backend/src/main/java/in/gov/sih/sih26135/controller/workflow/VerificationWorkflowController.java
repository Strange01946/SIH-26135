package in.gov.sih.sih26135.controller.workflow;

import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationSummaryResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
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
 * REST controller for orchestrating Employment Verification workflows.
 *
 * <p>Coordinates cross-domain actions across survey responses, employment verification requests,
 * multi-attempt verification verdicts, evidence recording, and employment record status synchronization.
 *
 * <p>Base Route: /api/v1/workflows/verification (alias: /api/v1/workflows/verifications)
 */
@RestController
@RequestMapping({"/api/v1/workflows/verification", "/api/v1/workflows/verifications"})
@PreAuthorize("hasAuthority('verification.manage')")
public class VerificationWorkflowController {

  private final VerificationWorkflowService verificationWorkflowService;

  public VerificationWorkflowController(VerificationWorkflowService verificationWorkflowService) {
    this.verificationWorkflowService = verificationWorkflowService;
  }

  /**
   * Initiates a new employment verification request originating from a follow-up survey response.
   *
   * @param request workflow payload containing survey response ID and verification request details
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @PostMapping("/initiate-from-survey")
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> initiateVerificationFromSurvey(
      @RequestBody InitiateVerificationFromSurveyRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    EmploymentVerificationRequestResponse response =
        verificationWorkflowService.initiateVerificationFromSurvey(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment verification initiated from survey successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Records a formal verification verdict, links supporting evidence, and synchronizes status with request and employment records.
   *
   * @param request workflow payload containing verification specs, evidence list, and status synchronization codes
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with RecordVerificationVerdictResponse enveloped in ApiResponse
   */
  @PostMapping("/record-verdict")
  public ResponseEntity<ApiResponse<RecordVerificationVerdictResponse>> recordVerificationVerdict(
      @RequestBody RecordVerificationVerdictRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    RecordVerificationVerdictResponse response =
        verificationWorkflowService.recordVerificationVerdictAndSynchronize(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Verification verdict recorded and synchronized successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Retrieves an aggregate employment verification summary for a given employment record.
   *
   * @param employmentId unique identifier of the employment record
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with EmploymentVerificationSummaryResponse enveloped in ApiResponse
   */
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<EmploymentVerificationSummaryResponse>> getEmploymentVerificationSummary(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    EmploymentVerificationSummaryResponse response =
        verificationWorkflowService.getEmploymentVerificationSummary(employmentId);
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
