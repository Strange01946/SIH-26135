package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Employment Verification Attempt domain resources.
 *
 * <p>Base Route: /api/v1/employment-verification-attempts
 * Consumes: CreateEmploymentVerificationAttemptRequest, UpdateEmploymentVerificationAttemptRequest
 * Produces: EmploymentVerificationAttemptResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employment-verification-attempts")
public class EmploymentVerificationAttemptController {

  private final EmploymentVerificationAttemptService employmentVerificationAttemptService;

  public EmploymentVerificationAttemptController(
      EmploymentVerificationAttemptService employmentVerificationAttemptService) {
    this.employmentVerificationAttemptService = employmentVerificationAttemptService;
  }

  /**
   * Retrieves an employment verification attempt by primary key identifier.
   *
   * @param id primary key identifier of the verification attempt
   * @return 200 OK with EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationAttemptResponse>> getById(@PathVariable Long id) {
    EmploymentVerificationAttemptResponse response =
        employmentVerificationAttemptService.getVerificationAttemptById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all attempts linked to a specific employment verification request.
   *
   * @param requestId verification request identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"requestId", "!methodId", "!statusId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationAttemptResponse>>> getByRequestId(
      @RequestParam("requestId") Long requestId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "requestId");
    List<EmploymentVerificationAttemptResponse> attempts =
        employmentVerificationAttemptService.getAttemptsByRequestId(requestId);
    return ResponseEntity.ok(ApiResponse.ok(attempts));
  }

  /**
   * Retrieves all verification attempts using a specific verification method.
   *
   * @param methodId verification method reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"methodId", "!requestId", "!statusId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationAttemptResponse>>> getByMethodId(
      @RequestParam("methodId") Long methodId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "methodId");
    List<EmploymentVerificationAttemptResponse> attempts =
        employmentVerificationAttemptService.getAttemptsByMethodId(methodId);
    return ResponseEntity.ok(ApiResponse.ok(attempts));
  }

  /**
   * Retrieves all verification attempts with a specific attempt status.
   *
   * @param statusId verification attempt status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!requestId", "!methodId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationAttemptResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<EmploymentVerificationAttemptResponse> attempts =
        employmentVerificationAttemptService.getAttemptsByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(attempts));
  }

  /**
   * Creates a new employment verification attempt.
   *
   * @param request verification attempt creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentVerificationAttemptResponse>> createVerificationAttempt(
      @RequestBody CreateEmploymentVerificationAttemptRequest request,
      HttpServletRequest httpRequest) {
    EmploymentVerificationAttemptResponse response =
        employmentVerificationAttemptService.createVerificationAttempt(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment verification attempt created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment verification attempt.
   *
   * @param id primary key identifier of the verification attempt to update
   * @param request verification attempt update payload
   * @return 200 OK with updated EmploymentVerificationAttemptResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationAttemptResponse>> updateVerificationAttempt(
      @PathVariable Long id,
      @RequestBody UpdateEmploymentVerificationAttemptRequest request) {
    EmploymentVerificationAttemptResponse response =
        employmentVerificationAttemptService.updateVerificationAttempt(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Employment verification attempt updated successfully",
        response));
  }

  /**
   * Deletes an employment verification attempt.
   *
   * @param id primary key identifier of the verification attempt to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteVerificationAttempt(@PathVariable Long id) {
    employmentVerificationAttemptService.deleteVerificationAttempt(id);
    return ResponseEntity.ok(ApiResponse.success("Employment verification attempt deleted successfully"));
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
