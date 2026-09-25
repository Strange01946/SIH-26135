package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.PlacementRecordService;
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
    validateOnlyQueryParameter(httpRequest, "placementNumber");
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
    validateOnlyQueryParameter(httpRequest, "jobApplicationId");
    PlacementRecordResponse response = placementRecordService.getPlacementRecordByJobApplicationId(jobApplicationId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all placement records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!placementNumber", "!jobApplicationId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!placementNumber", "!jobApplicationId", "!traineeId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByEnrollment(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records with a specific employer.
   *
   * @param employerId employer identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employerId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByEmployer(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByEmployer(employerId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records resulting from a specific job posting.
   *
   * @param jobPostingId job posting identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"jobPostingId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByJobPosting(
      @RequestParam("jobPostingId") Long jobPostingId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobPostingId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByJobPosting(jobPostingId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records associated with a specific course.
   *
   * @param courseId course identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"courseId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByCourse(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByCourse(courseId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records with a specific placement status.
   *
   * @param statusId placement status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<PlacementRecordResponse> records = placementRecordService.getPlacementRecordsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all placement records with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted placement records
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "includeDeleted");
    List<PlacementRecordResponse> records = placementRecordService.getAllPlacementRecords(includeDeleted);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all active placement records.
   *
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with list of active PlacementRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!placementNumber", "!jobApplicationId", "!traineeId", "!enrollmentId", "!employerId", "!jobPostingId", "!courseId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<PlacementRecordResponse>>> getAllPlacementRecords(
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException("Unsupported query parameters: " + httpRequest.getParameterMap().keySet());
    }
    List<PlacementRecordResponse> records = placementRecordService.getAllPlacementRecords(false);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
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
