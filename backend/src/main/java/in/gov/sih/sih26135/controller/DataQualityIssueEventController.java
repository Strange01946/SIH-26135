package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.DataQualityIssueEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Data Quality Issue Event domain resources.
 *
 * <p>Base Route: /api/v1/data-quality-issue-events
 * Consumes: CreateDataQualityIssueEventRequest
 * Produces: DataQualityIssueEventResponse enveloped in ApiResponse
 *
 * <p>Issue events are append-only audit records; updates and deletions are strictly prohibited.
 */
@RestController
@RequestMapping("/api/v1/data-quality-issue-events")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
public class DataQualityIssueEventController {

  private final DataQualityIssueEventService dataQualityIssueEventService;

  public DataQualityIssueEventController(DataQualityIssueEventService dataQualityIssueEventService) {
    this.dataQualityIssueEventService = dataQualityIssueEventService;
  }

  /**
   * Retrieves a data quality issue event by primary key identifier.
   *
   * @param id primary key identifier of the issue event
   * @return 200 OK with DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityIssueEventResponse>> getById(@PathVariable Long id) {
    DataQualityIssueEventResponse response = dataQualityIssueEventService.getIssueEventById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all event history for a specific data quality issue.
   *
   * @param issueId data quality issue identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "issueId", "!statusId", "!changedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueEventResponse>>> getByIssueId(
      @RequestParam("issueId") Long issueId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "issueId");
    List<DataQualityIssueEventResponse> events = dataQualityIssueEventService.getIssueEventsByIssueId(issueId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all data quality issue events resulting in a specific issue status.
   *
   * @param statusId resulting data quality issue status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!issueId", "!changedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueEventResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<DataQualityIssueEventResponse> events = dataQualityIssueEventService.getIssueEventsByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all data quality issue events performed by a specific user.
   *
   * @param changedByUserId user identifier who made the change
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "changedByUserId", "!issueId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueEventResponse>>> getByChangedByUserId(
      @RequestParam("changedByUserId") Long changedByUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "changedByUserId");
    List<DataQualityIssueEventResponse> events = dataQualityIssueEventService.getIssueEventsByChangedByUserId(changedByUserId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Records a new data quality issue event.
   *
   * @param request event creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<DataQualityIssueEventResponse>> recordIssueEvent(
      @RequestBody CreateDataQualityIssueEventRequest request,
      HttpServletRequest httpRequest) {
    DataQualityIssueEventResponse response = dataQualityIssueEventService.recordIssueEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Data quality issue event recorded successfully",
            response,
            httpRequest.getRequestURI()));
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
