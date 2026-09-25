package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
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
 * REST controller for managing Employment Verification Evidence domain resources.
 *
 * <p>Base Route: /api/v1/employment-verification-evidence
 * Consumes: CreateEmploymentVerificationEvidenceRequest, UpdateEmploymentVerificationEvidenceRequest
 * Produces: EmploymentVerificationEvidenceResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employment-verification-evidence")
@PreAuthorize("hasAuthority('verification.manage')")
public class EmploymentVerificationEvidenceController {

  private final EmploymentVerificationEvidenceService employmentVerificationEvidenceService;

  public EmploymentVerificationEvidenceController(
      EmploymentVerificationEvidenceService employmentVerificationEvidenceService) {
    this.employmentVerificationEvidenceService = employmentVerificationEvidenceService;
  }

  /**
   * Retrieves an employment verification evidence record by primary key identifier.
   *
   * @param id primary key identifier of the evidence record
   * @return 200 OK with EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationEvidenceResponse>> getById(@PathVariable Long id) {
    EmploymentVerificationEvidenceResponse response =
        employmentVerificationEvidenceService.getVerificationEvidenceById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all evidence records linked to a specific employment verification.
   *
   * @param verificationId employment verification identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"verificationId", "!attemptId", "!typeId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationEvidenceResponse>>> getByVerificationId(
      @RequestParam("verificationId") Long verificationId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "verificationId");
    List<EmploymentVerificationEvidenceResponse> evidenceList =
        employmentVerificationEvidenceService.getEvidenceByVerificationId(verificationId);
    return ResponseEntity.ok(ApiResponse.ok(evidenceList));
  }

  /**
   * Retrieves all evidence records captured during a specific verification attempt.
   *
   * @param attemptId verification attempt identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"attemptId", "!verificationId", "!typeId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationEvidenceResponse>>> getByAttemptId(
      @RequestParam("attemptId") Long attemptId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "attemptId");
    List<EmploymentVerificationEvidenceResponse> evidenceList =
        employmentVerificationEvidenceService.getEvidenceByAttemptId(attemptId);
    return ResponseEntity.ok(ApiResponse.ok(evidenceList));
  }

  /**
   * Retrieves all evidence records of a specific evidence type.
   *
   * @param typeId evidence type reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"typeId", "!verificationId", "!attemptId"})
  public ResponseEntity<ApiResponse<List<EmploymentVerificationEvidenceResponse>>> getByTypeId(
      @RequestParam("typeId") Long typeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "typeId");
    List<EmploymentVerificationEvidenceResponse> evidenceList =
        employmentVerificationEvidenceService.getEvidenceByTypeId(typeId);
    return ResponseEntity.ok(ApiResponse.ok(evidenceList));
  }

  /**
   * Creates a new employment verification evidence record.
   *
   * @param request evidence creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentVerificationEvidenceResponse>> createVerificationEvidence(
      @RequestBody CreateEmploymentVerificationEvidenceRequest request,
      HttpServletRequest httpRequest) {
    EmploymentVerificationEvidenceResponse response =
        employmentVerificationEvidenceService.createVerificationEvidence(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment verification evidence created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment verification evidence record.
   *
   * @param id primary key identifier of the evidence record to update
   * @param request evidence update payload
   * @return 200 OK with updated EmploymentVerificationEvidenceResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationEvidenceResponse>> updateVerificationEvidence(
      @PathVariable Long id,
      @RequestBody UpdateEmploymentVerificationEvidenceRequest request) {
    EmploymentVerificationEvidenceResponse response =
        employmentVerificationEvidenceService.updateVerificationEvidence(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Employment verification evidence updated successfully",
        response));
  }

  /**
   * Deletes an employment verification evidence record.
   *
   * @param id primary key identifier of the evidence record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteVerificationEvidence(@PathVariable Long id) {
    employmentVerificationEvidenceService.deleteVerificationEvidence(id);
    return ResponseEntity.ok(ApiResponse.success("Employment verification evidence deleted successfully"));
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
