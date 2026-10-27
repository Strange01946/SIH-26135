package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.FollowupTaskService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * REST controller for managing Follow-up Task domain resources.
 *
 * <p>Base Route: /api/v1/followup-tasks
 * Consumes: CreateFollowupTaskRequest, UpdateFollowupTaskRequest
 * Produces: FollowupTaskResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/followup-tasks")
@PreAuthorize("hasAuthority('survey.manage')")
public class FollowupTaskController {

  private final FollowupTaskService followupTaskService;

  public FollowupTaskController(FollowupTaskService followupTaskService) {
    this.followupTaskService = followupTaskService;
  }

  /**
   * Retrieves a follow-up task by primary key identifier.
   *
   * @param id primary key identifier of the task
   * @return 200 OK with FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FollowupTaskResponse>> getById(@PathVariable Long id) {
    FollowupTaskResponse response = followupTaskService.getFollowupTaskById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single follow-up task for a specific campaign and trainee.
   *
   * @param campaignId campaign identifier
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"campaignId", "traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<FollowupTaskResponse>> getByCampaignAndTrainee(
      @RequestParam("campaignId") Long campaignId,
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("campaignId", "traineeId"));
    FollowupTaskResponse response = followupTaskService.getFollowupTaskByCampaignAndTrainee(campaignId, traineeId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated follow-up tasks linked to a specific campaign.
   *
   * @param campaignId campaign identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByCampaign(
      @RequestParam("campaignId") Long campaignId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("campaignId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByCampaign(campaignId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks linked to a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!campaignId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!campaignId", "!traineeId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("enrollmentId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByEnrollment(enrollmentId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks linked to a specific placement record.
   *
   * @param placementId placement record identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"placementId", "!campaignId", "!traineeId", "!enrollmentId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByPlacement(
      @RequestParam("placementId") Long placementId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("placementId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByPlacement(placementId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks linked to a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByEmployment(
      @RequestParam("employmentId") Long employmentId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("employmentId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByEmployment(employmentId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks associated with a specific survey.
   *
   * @param surveyId survey identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getBySurvey(
      @RequestParam("surveyId") Long surveyId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("surveyId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksBySurvey(surveyId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks with a specific followup status.
   *
   * @param statusId followup status identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByStatus(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks with a specific followup outcome.
   *
   * @param outcomeId followup outcome identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"outcomeId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByOutcome(
      @RequestParam("outcomeId") Long outcomeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("outcomeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByOutcome(outcomeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks assigned to a specific user.
   *
   * @param assignedUserId assigned user identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"assignedUserId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByAssignedUser(
      @RequestParam("assignedUserId") Long assignedUserId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("assignedUserId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByAssignedUser(assignedUserId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Retrieves paginated follow-up tasks scheduled on a specific date.
   *
   * @param scheduledDate scheduled date (ISO-8601 YYYY-MM-DD)
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"scheduledDate", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId"})
  public ResponseEntity<ApiResponse<PagedResponse<FollowupTaskResponse>>> getByScheduledDate(
      @RequestParam("scheduledDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("scheduledDate", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.FOLLOWUP_TASK_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByScheduledDate(scheduledDate, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(tasks)));
  }

  /**
   * Creates a new follow-up task.
   *
   * @param request task creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created FollowupTaskResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<FollowupTaskResponse>> createFollowupTask(
      @RequestBody CreateFollowupTaskRequest request,
      HttpServletRequest httpRequest) {
    FollowupTaskResponse response = followupTaskService.createFollowupTask(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Follow-up task created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing follow-up task.
   *
   * @param id primary key identifier of the task to update
   * @param request task update payload
   * @return 200 OK with updated FollowupTaskResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<FollowupTaskResponse>> updateFollowupTask(
      @PathVariable Long id,
      @RequestBody UpdateFollowupTaskRequest request) {
    FollowupTaskResponse response = followupTaskService.updateFollowupTask(id, request);
    return ResponseEntity.ok(ApiResponse.success("Follow-up task updated successfully", response));
  }

  /**
   * Deletes a follow-up task.
   *
   * @param id primary key identifier of the task to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteFollowupTask(@PathVariable Long id) {
    followupTaskService.deleteFollowupTask(id);
    return ResponseEntity.ok(ApiResponse.success("Follow-up task deleted successfully"));
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
