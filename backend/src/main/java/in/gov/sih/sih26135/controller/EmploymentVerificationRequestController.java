package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Employment Verification Request domain resources.
 *
 * <p>Base Route: /api/v1/employment-verification-requests
 * Consumes: CreateEmploymentVerificationRequestRequest, UpdateEmploymentVerificationRequestRequest
 * Produces: EmploymentVerificationRequestResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employment-verification-requests")
@PreAuthorize("hasAuthority('verification.manage')")
public class EmploymentVerificationRequestController {

  private final EmploymentVerificationRequestService employmentVerificationRequestService;

  public EmploymentVerificationRequestController(
      EmploymentVerificationRequestService employmentVerificationRequestService) {
    this.employmentVerificationRequestService = employmentVerificationRequestService;
  }

  /**
   * Retrieves an employment verification request by primary key identifier.
   *
   * @param id primary key identifier of the verification request
   * @return 200 OK with EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> getById(@PathVariable Long id) {
    EmploymentVerificationRequestResponse response =
        employmentVerificationRequestService.getVerificationRequestById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single employment verification request by unique request number.
   *
   * @param requestNumber unique request number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"requestNumber", "!employmentId", "!traineeId", "!statusId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> getByRequestNumber(
      @RequestParam("requestNumber") String requestNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "requestNumber");
    EmploymentVerificationRequestResponse response =
        employmentVerificationRequestService.getVerificationRequestByNumber(requestNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all employment verification requests linked to a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "!requestNumber", "!traineeId", "!statusId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationRequestResponse>>> getByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    List<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByEmploymentId(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(requests));
  }

  /**
   * Retrieves all employment verification requests for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!requestNumber", "!employmentId", "!statusId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationRequestResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(requests));
  }

  /**
   * Retrieves all employment verification requests with a specific request status.
   *
   * @param statusId verification request status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!requestNumber", "!employmentId", "!traineeId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationRequestResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(requests));
  }

  /**
   * Retrieves all employment verification requests assigned to a specific verifier user.
   *
   * @param assignedVerifierUserId assigned verifier user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"assignedVerifierUserId", "!requestNumber", "!employmentId", "!traineeId", "!statusId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationRequestResponse>>> getByAssignedVerifierId(
      @RequestParam("assignedVerifierUserId") Long assignedVerifierUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assignedVerifierUserId");
    List<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByAssignedVerifierId(assignedVerifierUserId);
    return ResponseEntity.ok(ApiResponse.ok(requests));
  }

  /**
   * Creates a new employment verification request.
   *
   * @param request verification request creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> createVerificationRequest(
      @RequestBody CreateEmploymentVerificationRequestRequest request,
      HttpServletRequest httpRequest) {
    EmploymentVerificationRequestResponse response =
        employmentVerificationRequestService.createVerificationRequest(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment verification request created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment verification request.
   *
   * @param id primary key identifier of the verification request to update
   * @param request verification request update payload
   * @return 200 OK with updated EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> updateVerificationRequest(
      @PathVariable Long id,
      @RequestBody UpdateEmploymentVerificationRequestRequest request) {
    EmploymentVerificationRequestResponse response =
        employmentVerificationRequestService.updateVerificationRequest(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Employment verification request updated successfully",
        response));
  }

  /**
   * Deletes an employment verification request.
   *
   * @param id primary key identifier of the verification request to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteVerificationRequest(@PathVariable Long id) {
    employmentVerificationRequestService.deleteVerificationRequest(id);
    return ResponseEntity.ok(ApiResponse.success("Employment verification request deleted successfully"));
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
