package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentRecordService;
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
 * REST controller for managing Employment Record domain resources.
 *
 * <p>Base Route: /api/v1/employment-records
 * Consumes: CreateEmploymentRecordRequest, UpdateEmploymentRecordRequest
 * Produces: EmploymentRecordResponse enveloped in ApiResponse
 */
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
  public ResponseEntity<ApiResponse<EmploymentRecordResponse>> getById(@PathVariable Long id) {
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
      @RequestParam("employmentNumber") String employmentNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentNumber");
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
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    EmploymentRecordResponse response = employmentRecordService.getEmploymentRecordByPlacementId(placementId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all employment records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!isCurrent", "!employmentNumber", "!placementId", "!employerId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves current active employment records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "isCurrent", "!employmentNumber", "!placementId", "!employerId", "!statusId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getCurrentByTrainee(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "isCurrent"));
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment records filter", "INVALID_FILTER_PARAMETER");
    }
    List<EmploymentRecordResponse> records = employmentRecordService.getCurrentEmploymentRecordsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all employment records with a specific employer.
   *
   * @param employerId employer identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employerId", "!employmentNumber", "!placementId", "!traineeId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getByEmployer(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    List<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByEmployer(employerId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all employment records with a specific employment spell status.
   *
   * @param statusId employment spell status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!employmentNumber", "!placementId", "!traineeId", "!employerId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<EmploymentRecordResponse> records = employmentRecordService.getEmploymentRecordsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all employment records with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted employment records
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!employmentNumber", "!placementId", "!traineeId", "!employerId", "!statusId", "!isCurrent"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "includeDeleted");
    List<EmploymentRecordResponse> records = employmentRecordService.getAllEmploymentRecords(includeDeleted);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all active employment records.
   *
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with list of active EmploymentRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!employmentNumber", "!placementId", "!traineeId", "!employerId", "!statusId", "!includeDeleted", "!isCurrent"})
  public ResponseEntity<ApiResponse<List<EmploymentRecordResponse>>> getAllEmploymentRecords(
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException("Unsupported query parameters: " + httpRequest.getParameterMap().keySet(), "UNSUPPORTED_PARAMETER");
    }
    List<EmploymentRecordResponse> records = employmentRecordService.getAllEmploymentRecords(false);
    return ResponseEntity.ok(ApiResponse.ok(records));
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
      @RequestBody CreateEmploymentRecordRequest request,
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
      @PathVariable Long id,
      @RequestBody UpdateEmploymentRecordRequest request) {
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
  public ResponseEntity<ApiResponse<Void>> deleteEmploymentRecord(@PathVariable Long id) {
    employmentRecordService.deleteEmploymentRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Employment record deleted successfully"));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }

  private void validateCompoundQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }
}
