package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
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
 * REST controller for managing Employment Verification outcome domain resources.
 *
 * <p>Base Route: /api/v1/employment-verifications
 * Consumes: CreateEmploymentVerificationRequest, UpdateEmploymentVerificationRequest
 * Produces: EmploymentVerificationResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employment-verifications")
@PreAuthorize("hasAuthority('verification.manage')")
public class EmploymentVerificationController {

  private final EmploymentVerificationService employmentVerificationService;

  public EmploymentVerificationController(EmploymentVerificationService employmentVerificationService) {
    this.employmentVerificationService = employmentVerificationService;
  }

  /**
   * Retrieves an employment verification by primary key identifier.
   *
   * @param id primary key identifier of the verification
   * @return 200 OK with EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationResponse>> getById(@PathVariable Long id) {
    EmploymentVerificationResponse response = employmentVerificationService.getEmploymentVerificationById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single employment verification by unique verification number.
   *
   * @param verificationNumber unique verification number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"verificationNumber", "!employmentId", "!isCurrent", "!traineeId", "!statusId"})
  public ResponseEntity<ApiResponse<EmploymentVerificationResponse>> getByVerificationNumber(
      @RequestParam("verificationNumber") String verificationNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "verificationNumber");
    EmploymentVerificationResponse response =
        employmentVerificationService.getEmploymentVerificationByNumber(verificationNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves the current active verification record for a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "isCurrent", "!verificationNumber", "!traineeId", "!statusId"})
  public ResponseEntity<ApiResponse<EmploymentVerificationResponse>> getCurrentByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("employmentId", "isCurrent"));
    if (!isCurrent) {
      throw new BadRequestException(
          "Only isCurrent=true is supported for current employment verification filter",
          "INVALID_FILTER_PARAMETER");
    }
    EmploymentVerificationResponse response =
        employmentVerificationService.getCurrentVerificationByEmploymentId(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all verification records for a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "!isCurrent", "!verificationNumber", "!traineeId", "!statusId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationResponse>>> getByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    List<EmploymentVerificationResponse> verifications =
        employmentVerificationService.getVerificationsByEmploymentId(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(verifications));
  }

  /**
   * Retrieves all verification records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!employmentId", "!isCurrent", "!verificationNumber", "!statusId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<EmploymentVerificationResponse> verifications =
        employmentVerificationService.getVerificationsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(verifications));
  }

  /**
   * Retrieves all verification records with a specific record verification status.
   *
   * @param statusId record verification status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!employmentId", "!isCurrent", "!verificationNumber", "!traineeId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<EmploymentVerificationResponse> verifications =
        employmentVerificationService.getVerificationsByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(verifications));
  }

  /**
   * Creates a new employment verification record.
   *
   * @param request verification creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentVerificationResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentVerificationResponse>> createEmploymentVerification(
      @RequestBody CreateEmploymentVerificationRequest request,
      HttpServletRequest httpRequest) {
    EmploymentVerificationResponse response =
        employmentVerificationService.createEmploymentVerification(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment verification created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment verification record.
   *
   * @param id primary key identifier of the verification record to update
   * @param request verification update payload
   * @return 200 OK with updated EmploymentVerificationResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationResponse>> updateEmploymentVerification(
      @PathVariable Long id,
      @RequestBody UpdateEmploymentVerificationRequest request) {
    EmploymentVerificationResponse response =
        employmentVerificationService.updateEmploymentVerification(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Employment verification updated successfully",
        response));
  }

  /**
   * Deletes an employment verification record.
   *
   * @param id primary key identifier of the verification record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEmploymentVerification(@PathVariable Long id) {
    employmentVerificationService.deleteEmploymentVerification(id);
    return ResponseEntity.ok(ApiResponse.success("Employment verification deleted successfully"));
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

  private void validateCompoundQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }
}
