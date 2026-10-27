package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import jakarta.servlet.http.HttpServletRequest;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * REST controller for managing Employment Record domain resources.
 *
 * <p>Base Route: /api/v1/employment-records
 * Consumes: CreateEmploymentRecordRequest, UpdateEmploymentRecordRequest
 * Produces: EmploymentRecordResponse enveloped in ApiResponse
 */
@Validated
@RestController
@RequestMapping("/api/v1/employment-records")
@PreAuthorize("hasAuthority('employment.manage')")
public class EmploymentRecordController {

  private final EmploymentRecordService employmentRecordService;

  public EmploymentRecordController(EmploymentRecordService employmentRecordService) {
    this.employmentRecordService = employmentRecordService;
  }

  /**
   * Retrieves an employment record by primary key identifier.
   *
   * @param id primary key identifier of the employment record
   * @return 200 OK with EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> getById(@PathVariable @Positive Long id) {
    EmploymentRecordResponse response = employmentRecordService.getEmploymentRecordById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves an employment record by unique employment number.
   *
   * @param employmentNumber unique employment number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentNumber", "!placementId", "!traineeId", "!employerId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> getByEmploymentNumber(
      @RequestParam("employmentNumber") @NotBlank String employmentNumber,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("employmentNumber"));
    EmploymentRecordResponse response = employmentRecordService.getEmploymentRecordByNumber(employmentNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves an employment record by associated placement identifier.
   *
   * @param placementId placement identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"placementId", "!employmentNumber", "!traineeId", "!employerId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> getByPlacementId(
      @RequestParam("placementId") @Positive Long placementId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("placementId"));
    EmploymentRecordResponse response = employmentRecordService.getEmploymentRecordByPlacementId(placementId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated employment records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!isCurrent", "!employmentNumber", "!placementId", "!employerId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getByTrainee(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated current active employment records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "isCurrent", "!employmentNumber", "!placementId", "!employerId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getCurrentByTrainee(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "isCurrent", "page", "size", "sort", "direction"));
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment records filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getCurrentEmploymentRecordsByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated employment records with a specific employer.
   *
   * @param employerId employer identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employerId", "!employmentNumber", "!placementId", "!traineeId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getByEmployer(
      @RequestParam("employerId") @Positive Long employerId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("employerId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByEmployer(employerId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated employment records with a specific employment spell status.
   *
   * @param statusId employment spell status identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!employmentNumber", "!placementId", "!traineeId", "!employerId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getByStatus(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByStatus(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated employment records with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted employment records
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!employmentNumber", "!placementId", "!traineeId", "!employerId", "!statusId", "!isCurrent"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("includeDeleted", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getAllEmploymentRecords(includeDeleted, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves all active employment records with pagination.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with paginated active EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!employmentNumber", "!placementId", "!traineeId", "!employerId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRecordResponse>>> getAllEmploymentRecords(
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<EmploymentRecordResponse> records = employmentRecordService.getAllEmploymentRecords(false, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Creates a new employment record.
   *
   * @param request employment record creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentRecordResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> createEmploymentRecord(
      @Valid @RequestBody CreateEmploymentRecordRequest request,
      HttpServletRequest httpRequest) {
    EmploymentRecordResponse response = employmentRecordService.createEmploymentRecord(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Employment record created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment record.
   *
   * @param id primary key identifier of the employment record to update
   * @param request employment record update payload
   * @return 200 OK with updated EmploymentRecordResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> updateEmploymentRecord(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateEmploymentRecordRequest request) {
    EmploymentRecordResponse response = employmentRecordService.updateEmploymentRecord(id, request);
    return ResponseEntity.ok(ApiResponse.success("Employment record updated successfully", response));
  }

  /**
   * Soft-deletes an employment record.
   *
   * @param id primary key identifier of the employment record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEmploymentRecord(@PathVariable @Positive Long id) {
    employmentRecordService.deleteEmploymentRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Employment record deleted successfully"));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }
}
