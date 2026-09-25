package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
 * REST controller for managing Employment Exit Event domain resources.
 *
 * <p>Base Route: /api/v1/employment-exit-events
 * Consumes: CreateEmploymentExitEventRequest, UpdateEmploymentExitEventRequest
 * Produces: EmploymentExitEventResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employment-exit-events")
@PreAuthorize("hasAuthority('employment.manage')")
public class EmploymentExitEventController {

  private final EmploymentExitEventService employmentExitEventService;

  public EmploymentExitEventController(EmploymentExitEventService employmentExitEventService) {
    this.employmentExitEventService = employmentExitEventService;
  }

  /**
   * Retrieves an employment exit event by primary key identifier.
   *
   * @param id primary key identifier of the exit event
   * @return 200 OK with EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentExitEventResponse>> getById(@PathVariable Long id) {
    EmploymentExitEventResponse response = employmentExitEventService.getExitEventById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves an employment exit event for a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "employmentId", "!traineeId", "!enrollmentId", "!separationDate",
      "!exitReasonId", "!separationNatureId", "!statusId"
  })
  public ResponseEntity<ApiResponse<EmploymentExitEventResponse>> getByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    EmploymentExitEventResponse response = employmentExitEventService.getExitEventByEmploymentId(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all employment exit events for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "!employmentId", "!enrollmentId", "!separationDate",
      "!exitReasonId", "!separationNatureId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all employment exit events linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "enrollmentId", "!employmentId", "!traineeId", "!separationDate",
      "!exitReasonId", "!separationNatureId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsByEnrollmentId(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all employment exit events occurring on a specific separation date.
   *
   * @param separationDate date of separation
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "separationDate", "!employmentId", "!traineeId", "!enrollmentId",
      "!exitReasonId", "!separationNatureId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getBySeparationDate(
      @RequestParam("separationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate separationDate,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "separationDate");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsBySeparationDate(separationDate);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all employment exit events for a specific exit reason.
   *
   * @param exitReasonId employment exit reason reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "exitReasonId", "!employmentId", "!traineeId", "!enrollmentId",
      "!separationDate", "!separationNatureId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getByExitReasonId(
      @RequestParam("exitReasonId") Long exitReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "exitReasonId");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsByExitReasonId(exitReasonId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all employment exit events for a specific separation nature.
   *
   * @param separationNatureId separation nature reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "separationNatureId", "!employmentId", "!traineeId", "!enrollmentId",
      "!separationDate", "!exitReasonId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getBySeparationNatureId(
      @RequestParam("separationNatureId") Long separationNatureId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "separationNatureId");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsBySeparationNatureId(separationNatureId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all employment exit events with a specific record verification status.
   *
   * @param statusId record verification status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmploymentExitEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!employmentId", "!traineeId", "!enrollmentId",
      "!separationDate", "!exitReasonId", "!separationNatureId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitEventResponse>>> getByVerificationStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<EmploymentExitEventResponse> events = employmentExitEventService.getExitEventsByVerificationStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Creates a new employment exit event.
   *
   * @param request creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmploymentExitEventResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmploymentExitEventResponse>> createExitEvent(
      @RequestBody CreateEmploymentExitEventRequest request,
      HttpServletRequest httpRequest) {
    EmploymentExitEventResponse response = employmentExitEventService.createExitEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Employment exit event created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employment exit event.
   *
   * @param id primary key identifier of the exit event to update
   * @param request update payload
   * @return 200 OK with updated EmploymentExitEventResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmploymentExitEventResponse>> updateExitEvent(
      @PathVariable Long id,
      @RequestBody UpdateEmploymentExitEventRequest request) {
    EmploymentExitEventResponse response = employmentExitEventService.updateExitEvent(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Employment exit event updated successfully",
        response));
  }

  /**
   * Deletes an employment exit event.
   *
   * @param id primary key identifier of the exit event to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteExitEvent(@PathVariable Long id) {
    employmentExitEventService.deleteExitEvent(id);
    return ResponseEntity.ok(ApiResponse.success("Employment exit event deleted successfully"));
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
