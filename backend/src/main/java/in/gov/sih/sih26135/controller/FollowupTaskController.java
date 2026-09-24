package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.FollowupTaskService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

/**
 * REST controller for managing Follow-up Task domain resources.
 *
 * <p>Base Route: /api/v1/followup-tasks
 * Consumes: CreateFollowupTaskRequest, UpdateFollowupTaskRequest
 * Produces: FollowupTaskResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/followup-tasks")
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
    validateCompoundQueryParameters(httpRequest, Set.of("campaignId", "traineeId"));
    FollowupTaskResponse response = followupTaskService.getFollowupTaskByCampaignAndTrainee(campaignId, traineeId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all follow-up tasks linked to a specific campaign.
   *
   * @param campaignId campaign identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByCampaign(
      @RequestParam("campaignId") Long campaignId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "campaignId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByCampaign(campaignId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks linked to a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!campaignId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!campaignId", "!traineeId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByEnrollment(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks linked to a specific placement record.
   *
   * @param placementId placement record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"placementId", "!campaignId", "!traineeId", "!enrollmentId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByPlacement(
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByPlacement(placementId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks linked to a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByEmployment(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByEmployment(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks associated with a specific survey.
   *
   * @param surveyId survey identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!statusId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getBySurvey(
      @RequestParam("surveyId") Long surveyId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksBySurvey(surveyId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks with a specific followup status.
   *
   * @param statusId followup status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!outcomeId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks with a specific followup outcome.
   *
   * @param outcomeId followup outcome identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"outcomeId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!assignedUserId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByOutcome(
      @RequestParam("outcomeId") Long outcomeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "outcomeId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByOutcome(outcomeId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks assigned to a specific user.
   *
   * @param assignedUserId assigned user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"assignedUserId", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!scheduledDate"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByAssignedUser(
      @RequestParam("assignedUserId") Long assignedUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assignedUserId");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByAssignedUser(assignedUserId);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
  }

  /**
   * Retrieves all follow-up tasks scheduled on a specific date.
   *
   * @param scheduledDate scheduled date (ISO-8601 YYYY-MM-DD)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupTaskResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"scheduledDate", "!campaignId", "!traineeId", "!enrollmentId", "!placementId", "!employmentId", "!surveyId", "!statusId", "!outcomeId", "!assignedUserId"})
  public ResponseEntity<ApiResponse<List<FollowupTaskResponse>>> getByScheduledDate(
      @RequestParam("scheduledDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "scheduledDate");
    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByScheduledDate(scheduledDate);
    return ResponseEntity.ok(ApiResponse.ok(tasks));
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
