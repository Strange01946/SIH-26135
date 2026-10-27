package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.PlacementRecordService;
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
 * REST controller for managing Placement Record domain resources.
 *
 * <p>Base Route: /api/v1/placement-records
 * Consumes: CreatePlacementRecordRequest, UpdatePlacementRecordRequest
 * Produces: PlacementRecordResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/placement-records")
@PreAuthorize("hasAuthority('placement.manage')")
public class PlacementRecordController {

  private final PlacementRecordService placementRecordService;

  public PlacementRecordController(PlacementRecordService placementRecordService) {
    this.placementRecordService = placementRecordService;
  }

  /**
   * Retrieves a placement record by primary key identifier.
   *
   * @param id primary key identifier of the placement record
   * @return 200 OK with PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PlacementRecordResponse>> getById(@PathVariable Long id) {
    PlacementRecordResponse response = placementRecordService.getPlacementRecordById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a placement record by unique placement number.
   *
   * @param placementNumber unique placement number
   * @return 200 OK with PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PlacementRecordResponse>> getByPlacementNumber(
      @RequestParam("placementNumber") String placementNumber,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("placementNumber"));
    PlacementRecordResponse response = placementRecordService.getPlacementRecordByNumber(placementNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a placement record by associated job application identifier.
   *
   * @param jobApplicationId job application identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"jobApplicationId", "!placementNumber", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PlacementRecordResponse>> getByJobApplicationId(
      @RequestParam("jobApplicationId") Long jobApplicationId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("jobApplicationId"));
    PlacementRecordResponse response = placementRecordService.getPlacementRecordByJobApplicationId(jobApplicationId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated placement records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!placementNumber", "!jobApplicationId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!placementNumber", "!jobApplicationId", "!traineeId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("enrollmentId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByEnrollment(enrollmentId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records with a specific employer.
   *
   * @param employerId employer identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employerId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByEmployer(
      @RequestParam("employerId") Long employerId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("employerId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByEmployer(employerId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records resulting from a specific job posting.
   *
   * @param jobPostingId job posting identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"jobPostingId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByJobPosting(
      @RequestParam("jobPostingId") Long jobPostingId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("jobPostingId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByJobPosting(jobPostingId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records associated with a specific course.
   *
   * @param courseId course identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"courseId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByCourse(
      @RequestParam("courseId") Long courseId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("courseId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByCourse(courseId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records with a specific placement status.
   *
   * @param statusId placement status identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByStatus(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated placement records with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted placement records
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("includeDeleted", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getAllPlacementRecords(includeDeleted, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves all active placement records with pagination.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with paginated active PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<PlacementRecordResponse>>> getAllPlacementRecords(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.PLACEMENT_RECORD_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<PlacementRecordResponse> records = placementRecordService.getAllPlacementRecords(false, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }

  /**
   * Creates a new placement record.
   *
   * @param request placement record creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created PlacementRecordResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<PlacementRecordResponse>> createPlacementRecord(
      @RequestBody CreatePlacementRecordRequest request,
      HttpServletRequest httpRequest) {
    PlacementRecordResponse response = placementRecordService.createPlacementRecord(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Placement record created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing placement record.
   *
   * @param id primary key identifier of the placement record to update
   * @param request placement record update payload
   * @return 200 OK with updated PlacementRecordResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<PlacementRecordResponse>> updatePlacementRecord(
      @PathVariable Long id,
      @RequestBody UpdatePlacementRecordRequest request) {
    PlacementRecordResponse response = placementRecordService.updatePlacementRecord(id, request);
    return ResponseEntity.ok(ApiResponse.success("Placement record updated successfully", response));
  }

  /**
   * Soft-deletes a placement record.
   *
   * @param id primary key identifier of the placement record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePlacementRecord(@PathVariable Long id) {
    placementRecordService.deletePlacementRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Placement record deleted successfully"));
  }
}
