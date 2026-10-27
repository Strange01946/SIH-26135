package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Validated
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
  public ResponseEntity<ApiResponse<EmploymentVerificationRequestResponse>> getById(@PathVariable @Positive Long id) {
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
      @RequestParam("requestNumber") @NotBlank String requestNumber,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("requestNumber"));
    EmploymentVerificationRequestResponse response =
        employmentVerificationRequestService.getVerificationRequestByNumber(requestNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated employment verification requests linked to a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "!requestNumber", "!traineeId", "!statusId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationRequestResponse>>> getByEmploymentId(
      @RequestParam("employmentId") @Positive Long employmentId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("employmentId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_VERIFICATION_REQUEST_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByEmploymentId(employmentId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(requests)));
  }

  /**
   * Retrieves paginated employment verification requests for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!requestNumber", "!employmentId", "!statusId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationRequestResponse>>> getByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_VERIFICATION_REQUEST_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByTraineeId(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(requests)));
  }

  /**
   * Retrieves paginated employment verification requests with a specific request status.
   *
   * @param statusId verification request status reference identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!requestNumber", "!employmentId", "!traineeId", "!assignedVerifierUserId"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationRequestResponse>>> getByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_VERIFICATION_REQUEST_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByStatusId(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(requests)));
  }

  /**
   * Retrieves paginated employment verification requests assigned to a specific verifier user.
   *
   * @param assignedVerifierUserId assigned verifier user identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentVerificationRequestResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"assignedVerifierUserId", "!requestNumber", "!employmentId", "!traineeId", "!statusId"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationRequestResponse>>> getByAssignedVerifierId(
      @RequestParam("assignedVerifierUserId") @Positive Long assignedVerifierUserId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("assignedVerifierUserId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_VERIFICATION_REQUEST_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentVerificationRequestResponse> requests =
        employmentVerificationRequestService.getRequestsByAssignedVerifierId(assignedVerifierUserId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(requests)));
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
      @Valid @RequestBody CreateEmploymentVerificationRequestRequest request,
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
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateEmploymentVerificationRequestRequest request) {
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
  public ResponseEntity<ApiResponse<Void>> deleteVerificationRequest(@PathVariable @Positive Long id) {
    employmentVerificationRequestService.deleteVerificationRequest(id);
    return ResponseEntity.ok(ApiResponse.success("Employment verification request deleted successfully"));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
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
