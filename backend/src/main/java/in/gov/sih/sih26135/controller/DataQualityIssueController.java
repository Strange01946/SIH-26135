package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.DataQualityIssueService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Data Quality Issue domain resources.
 *
 * <p>Base Route: /api/v1/data-quality-issues
 * Consumes: CreateDataQualityIssueRequest, UpdateDataQualityIssueRequest, ResolveDataQualityIssueRequest
 * Produces: DataQualityIssueResponse enveloped in ApiResponse
 *
 * <p>Data quality issues do not support deletion; resolutions are performed via dedicated domain actions.
 */
@RestController
@RequestMapping("/api/v1/data-quality-issues")
public class DataQualityIssueController {

  private final DataQualityIssueService dataQualityIssueService;

  public DataQualityIssueController(DataQualityIssueService dataQualityIssueService) {
    this.dataQualityIssueService = dataQualityIssueService;
  }

  /**
   * Retrieves a data quality issue by primary key identifier.
   *
   * @param id primary key identifier of the issue
   * @return 200 OK with DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> getById(@PathVariable Long id) {
    DataQualityIssueResponse response = dataQualityIssueService.getIssueById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all data quality issues triggered by a specific data quality rule.
   *
   * @param ruleId data quality rule identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "ruleId", "!categoryId", "!severityId", "!statusId",
      "!sourceId", "!entityType", "!entityId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByRuleId(
      @RequestParam("ruleId") Long ruleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "ruleId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByRuleId(ruleId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues belonging to a specific category.
   *
   * @param categoryId data quality category reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "categoryId", "!ruleId", "!severityId", "!statusId",
      "!sourceId", "!entityType", "!entityId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByCategoryId(
      @RequestParam("categoryId") Long categoryId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "categoryId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByCategoryId(categoryId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues with a specific severity level.
   *
   * @param severityId data quality severity reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!ruleId", "!categoryId", "!statusId",
      "!sourceId", "!entityType", "!entityId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getBySeverityId(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesBySeverityId(severityId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues with a specific issue status.
   *
   * @param statusId data quality issue status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!ruleId", "!categoryId", "!severityId",
      "!sourceId", "!entityType", "!entityId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues detected by a specific source.
   *
   * @param sourceId detection source reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceId", "!ruleId", "!categoryId", "!severityId",
      "!statusId", "!entityType", "!entityId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByDetectionSourceId(
      @RequestParam("sourceId") Long sourceId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByDetectionSourceId(sourceId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues for a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!ruleId", "!categoryId",
      "!severityId", "!statusId", "!sourceId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("entityType", "entityId"));
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByEntity(entityType, entityId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues for a specific entity type.
   *
   * @param entityType target entity type name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!ruleId", "!categoryId",
      "!severityId", "!statusId", "!sourceId", "!assignedUserId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "entityType");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByEntityType(entityType);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues assigned to a specific user.
   *
   * @param assignedUserId assigned user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assignedUserId", "!ruleId", "!categoryId", "!severityId",
      "!statusId", "!sourceId", "!entityType", "!entityId", "!resolvedByUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByAssignedUserId(
      @RequestParam("assignedUserId") Long assignedUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assignedUserId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByAssignedUserId(assignedUserId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Retrieves all data quality issues resolved by a specific user.
   *
   * @param resolvedByUserId resolving user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "resolvedByUserId", "!ruleId", "!categoryId", "!severityId",
      "!statusId", "!sourceId", "!entityType", "!entityId", "!assignedUserId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityIssueResponse>>> getByResolvedByUserId(
      @RequestParam("resolvedByUserId") Long resolvedByUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "resolvedByUserId");
    List<DataQualityIssueResponse> issues = dataQualityIssueService.getIssuesByResolvedByUserId(resolvedByUserId);
    return ResponseEntity.ok(ApiResponse.ok(issues));
  }

  /**
   * Reports a new data quality issue.
   *
   * @param request issue creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> reportIssue(
      @RequestBody CreateDataQualityIssueRequest request,
      HttpServletRequest httpRequest) {
    DataQualityIssueResponse response = dataQualityIssueService.reportIssue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Data quality issue reported successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing data quality issue.
   *
   * @param id primary key identifier of the issue to update
   * @param request issue update payload
   * @return 200 OK with updated DataQualityIssueResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> updateIssue(
      @PathVariable Long id,
      @RequestBody UpdateDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityIssueService.updateIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue updated successfully",
        response));
  }

  /**
   * Assigns a data quality issue to a specific user.
   *
   * @param id primary key identifier of the issue to assign
   * @param assignedUserId user identifier to assign the issue to
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with updated DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/assign")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> assignIssue(
      @PathVariable Long id,
      @RequestParam("assignedUserId") Long assignedUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assignedUserId");
    DataQualityIssueResponse response =
        dataQualityIssueService.assignIssue(id, assignedUserId);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue assigned successfully",
        response));
  }

  /**
   * Resolves or dismisses a data quality issue.
   *
   * @param id primary key identifier of the issue to resolve
   * @param request issue resolution payload
   * @return 200 OK with resolved DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/resolve")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> resolveIssue(
      @PathVariable Long id,
      @RequestBody ResolveDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityIssueService.resolveIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue resolved successfully",
        response));
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
