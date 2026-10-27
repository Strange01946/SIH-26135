package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.CommunicationLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST controller for managing Communication Log domain resources.
 *
 * <p>Base Route: /api/v1/communication-logs
 * Consumes: CreateCommunicationLogRequest, UpdateCommunicationLogRequest
 * Produces: CommunicationLogResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/communication-logs")
@PreAuthorize("hasAnyAuthority('survey.manage', 'system.manage')")
public class CommunicationLogController {

  private final CommunicationLogService communicationLogService;

  public CommunicationLogController(CommunicationLogService communicationLogService) {
    this.communicationLogService = communicationLogService;
  }

  /**
   * Retrieves a communication log entry by primary key identifier.
   *
   * @param id primary key identifier of the communication log
   * @return 200 OK with CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CommunicationLogResponse>> getById(@PathVariable Long id) {
    CommunicationLogResponse response = communicationLogService.getCommunicationLogById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a communication log entry by provider message identifier.
   *
   * @param providerMessageId provider message identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "providerMessageId", "!traineeId", "!followupTaskId", "!surveyId",
      "!surveyResponseId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<CommunicationLogResponse>> getByProviderMessageId(
      @RequestParam("providerMessageId") String providerMessageId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("providerMessageId"));
    CommunicationLogResponse response = communicationLogService.getCommunicationLogByProviderMessageId(providerMessageId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated communication logs across the entire system.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "!providerMessageId", "!traineeId", "!followupTaskId", "!surveyId",
      "!surveyResponseId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getAllCommunicationLogs(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getAllCommunicationLogs(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "!providerMessageId", "!followupTaskId", "!surveyId",
      "!surveyResponseId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific follow-up task.
   *
   * @param followupTaskId follow-up task identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "followupTaskId", "!providerMessageId", "!traineeId", "!surveyId",
      "!surveyResponseId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByFollowupTask(
      @RequestParam("followupTaskId") Long followupTaskId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("followupTaskId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByFollowupTask(followupTaskId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific survey.
   *
   * @param surveyId survey identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "surveyId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyResponseId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getBySurvey(
      @RequestParam("surveyId") Long surveyId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("surveyId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsBySurvey(surveyId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific survey response.
   *
   * @param surveyResponseId survey response identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "surveyResponseId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyId", "!channelId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getBySurveyResponse(
      @RequestParam("surveyResponseId") Long surveyResponseId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("surveyResponseId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsBySurveyResponse(surveyResponseId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific channel.
   *
   * @param channelId communication channel identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "channelId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyId", "!surveyResponseId", "!statusId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByChannel(
      @RequestParam("channelId") Long channelId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("channelId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByChannel(channelId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific status.
   *
   * @param statusId communication status identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyId", "!surveyResponseId", "!channelId", "!purposeId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByStatus(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific purpose.
   *
   * @param purposeId communication purpose identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "purposeId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyId", "!surveyResponseId", "!channelId", "!statusId", "!directionId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByPurpose(
      @RequestParam("purposeId") Long purposeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("purposeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByPurpose(purposeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated communication logs for a specific direction.
   *
   * @param directionId communication direction identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated CommunicationLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "directionId", "!providerMessageId", "!traineeId", "!followupTaskId",
      "!surveyId", "!surveyResponseId", "!channelId", "!statusId", "!purposeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<CommunicationLogResponse>>> getByDirection(
      @RequestParam("directionId") Long directionId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("directionId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COMMUNICATION_LOG_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<CommunicationLogResponse> responses = communicationLogService.getCommunicationLogsByDirection(directionId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Records a new communication log entry.
   *
   * @param request log creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created CommunicationLogResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<CommunicationLogResponse>> logCommunication(
      @RequestBody CreateCommunicationLogRequest request,
      HttpServletRequest httpRequest) {
    CommunicationLogResponse response = communicationLogService.logCommunication(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Communication logged successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing communication log entry.
   *
   * @param id primary key identifier of the log to update
   * @param request log update payload
   * @return 200 OK with updated CommunicationLogResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CommunicationLogResponse>> updateCommunicationLog(
      @PathVariable Long id,
      @RequestBody UpdateCommunicationLogRequest request) {
    CommunicationLogResponse response = communicationLogService.updateCommunicationLog(id, request);
    return ResponseEntity.ok(ApiResponse.success("Communication log updated successfully", response));
  }

  /**
   * Deletes a communication log entry.
   *
   * @param id primary key identifier of the log to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCommunicationLog(@PathVariable Long id) {
    communicationLogService.deleteCommunicationLog(id);
    return ResponseEntity.ok(ApiResponse.success("Communication log deleted successfully"));
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
