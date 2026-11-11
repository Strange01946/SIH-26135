package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.service.DataQualityWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.DataQualityIssueService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Data Quality Issue domain resources.
 *
 * <p>Base Route: /api/v1/data-quality-issues
 * Consumes: CreateDataQualityIssueRequest, UpdateDataQualityIssueRequest, ResolveDataQualityIssueRequest
 * Produces: DataQualityIssueResponse enveloped in ApiResponse
 *
 * <p>Data quality issues do not support deletion; resolutions are performed via dedicated domain actions.
 */
@Validated
@RestController
@RequestMapping("/api/v1/data-quality-issues")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
public class DataQualityIssueController {

  private final DataQualityIssueService dataQualityIssueService;
  private final DataQualityWorkflowService dataQualityWorkflowService;

  @Autowired
  public DataQualityIssueController(
      DataQualityIssueService dataQualityIssueService,
      DataQualityWorkflowService dataQualityWorkflowService) {
    this.dataQualityIssueService = Objects.requireNonNull(dataQualityIssueService, "dataQualityIssueService is required");
    this.dataQualityWorkflowService = Objects.requireNonNull(dataQualityWorkflowService, "dataQualityWorkflowService is required");
  }

  /**
   * Retrieves a data quality issue by primary key identifier.
   *
   * @param id primary key identifier of the issue
   * @return 200 OK with DataQualityIssueResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> getById(@PathVariable @Positive Long id) {
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
      @RequestParam("ruleId") @Positive Long ruleId,
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
      @RequestParam("categoryId") @Positive Long categoryId,
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
      @RequestParam("severityId") @Positive Long severityId,
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
      @RequestParam("statusId") @Positive Long statusId,
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
      @RequestParam("sourceId") @Positive Long sourceId,
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
      @RequestParam("entityId") @Positive Long entityId,
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
      @RequestParam("assignedUserId") @Positive Long assignedUserId,
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
      @RequestParam("resolvedByUserId") @Positive Long resolvedByUserId,
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
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> reportIssue(
      @Valid @RequestBody CreateDataQualityIssueRequest request,
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
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> updateIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityIssueService.updateIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue updated successfully",
        response));
  }

  /**
   * Assigns a data quality issue to a specific user.
   *
   * @param id primary key identifier of the issue to assign
   * @param assignedUserIdParam user identifier parameter to assign the issue to
   * @param requestBody JSON request body specifying assigned user ID
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with updated DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/assign")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> assignIssue(
      @PathVariable @Positive Long id,
      @RequestParam(value = "assignedUserId", required = false) @Positive Long assignedUserIdParam,
      @Valid @RequestBody(required = false) AssignDataQualityIssueRequest requestBody,
      HttpServletRequest httpRequest) {
    AssignDataQualityIssueRequest request;
    if (requestBody != null && requestBody.getAssignedUserId() != null) {
      request = requestBody;
    } else if (assignedUserIdParam != null) {
      validateOnlyQueryParameter(httpRequest, "assignedUserId");
      request = new AssignDataQualityIssueRequest(assignedUserIdParam);
    } else {
      throw new BadRequestException("assignedUserId is required", "ASSIGNED_USER_REQUIRED");
    }
    DataQualityIssueResponse response = dataQualityWorkflowService.assignIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue assigned successfully",
        response));
  }

  /**
   * Starts active investigation on an assigned data quality issue.
   *
   * @param id primary key identifier of the issue
   * @param request optional investigation remarks
   * @return 200 OK with updated DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/start-investigation")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> startInvestigation(
      @PathVariable @Positive Long id,
      @Valid @RequestBody(required = false) StartInvestigationRequest request) {
    DataQualityIssueResponse response = dataQualityWorkflowService.startInvestigation(
        id, request != null ? request : new StartInvestigationRequest());
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue investigation started successfully",
        response));
  }

  /**
   * Resolves an in-progress data quality issue with mandatory resolution notes.
   *
   * @param id primary key identifier of the issue to resolve
   * @param request issue resolution payload
   * @return 200 OK with resolved DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/resolve")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> resolveIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody ResolveDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityWorkflowService.resolveIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue resolved successfully",
        response));
  }

  /**
   * Dismisses a reported or in-progress data quality issue with mandatory remarks.
   *
   * @param id primary key identifier of the issue to dismiss
   * @param request dismissal payload containing remarks
   * @return 200 OK with dismissed DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/dismiss")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> dismissIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody DismissDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityWorkflowService.dismissIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue dismissed successfully",
        response));
  }

  /**
   * Reopens a previously resolved data quality issue with mandatory reopening reason.
   *
   * @param id primary key identifier of the issue to reopen
   * @param request reopening payload containing reason
   * @return 200 OK with reopened DataQualityIssueResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/reopen")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<DataQualityIssueResponse>> reopenIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody ReopenDataQualityIssueRequest request) {
    DataQualityIssueResponse response = dataQualityWorkflowService.reopenIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue reopened successfully",
        response));
  }

  /**
   * Retrieves the historical event trail for a data quality issue.
   *
   * @param id primary key identifier of the issue
   * @return 200 OK with list of DataQualityIssueEventResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}/events")
  public ResponseEntity<ApiResponse<List<DataQualityIssueEventResponse>>> getIssueEvents(
      @PathVariable @Positive Long id) {
    List<DataQualityIssueEventResponse> response = dataQualityWorkflowService.getIssueEvents(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
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
