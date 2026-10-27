package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.SurveyResponseService;
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
 * REST controller for managing Survey Response domain resources.
 *
 * <p>Base Route: /api/v1/survey-responses
 * Consumes: CreateSurveyResponseRequest, UpdateSurveyResponseRequest
 * Produces: SurveyResponseResponse enveloped in ApiResponse
 */
@Validated
@RestController
@RequestMapping("/api/v1/survey-responses")
@PreAuthorize("hasAuthority('survey.manage')")
public class SurveyResponseController {

  private final SurveyResponseService surveyResponseService;

  public SurveyResponseController(SurveyResponseService surveyResponseService) {
    this.surveyResponseService = surveyResponseService;
  }

  /**
   * Retrieves a survey response by primary key identifier.
   *
   * @param id primary key identifier of the survey response
   * @return 200 OK with SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseResponse>> getById(@PathVariable @Positive Long id) {
    SurveyResponseResponse response = surveyResponseService.getSurveyResponseById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a specific survey response attempt for a survey and trainee.
   *
   * @param surveyId survey identifier
   * @param traineeId trainee identifier
   * @param attemptNumber attempt sequence number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "traineeId", "attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<SurveyResponseResponse>> getByAttempt(
      @RequestParam("surveyId") @Positive Long surveyId,
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam("attemptNumber") @Positive Integer attemptNumber,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("surveyId", "traineeId", "attemptNumber"));
    SurveyResponseResponse response = surveyResponseService.getSurveyResponseByAttempt(surveyId, traineeId, attemptNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated survey responses associated with a specific survey.
   *
   * @param surveyId survey identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getBySurvey(
      @RequestParam("surveyId") @Positive Long surveyId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("surveyId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesBySurvey(surveyId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated survey responses submitted by a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!surveyId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getByTrainee(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("traineeId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesByTrainee(traineeId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated survey responses linked to a specific follow-up task.
   *
   * @param followupTaskId follow-up task identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"followupTaskId", "!surveyId", "!traineeId", "!attemptNumber", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getByFollowupTask(
      @RequestParam("followupTaskId") @Positive Long followupTaskId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("followupTaskId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesByFollowupTask(followupTaskId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated survey responses linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("enrollmentId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesByEnrollment(enrollmentId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated survey responses with a specific survey response status.
   *
   * @param statusId survey response status identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!versionId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getByStatus(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesByStatus(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Retrieves paginated survey responses linked to a specific survey template version.
   *
   * @param versionId survey template version identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: createdAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId"})
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseResponse>>> getByTemplateVersion(
      @RequestParam("versionId") @Positive Long versionId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("versionId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SURVEY_RESPONSE_SORT_FIELDS, "createdAt", Sort.Direction.DESC
    );
    Page<SurveyResponseResponse> responses = surveyResponseService.getResponsesByTemplateVersion(versionId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(responses)));
  }

  /**
   * Creates a new survey response.
   *
   * @param request response creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SurveyResponseResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SurveyResponseResponse>> createSurveyResponse(
      @Valid @RequestBody CreateSurveyResponseRequest request,
      HttpServletRequest httpRequest) {
    SurveyResponseResponse response = surveyResponseService.createSurveyResponse(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Survey response created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing survey response.
   *
   * @param id primary key identifier of the response to update
   * @param request response update payload
   * @return 200 OK with updated SurveyResponseResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseResponse>> updateSurveyResponse(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateSurveyResponseRequest request) {
    SurveyResponseResponse response = surveyResponseService.updateSurveyResponse(id, request);
    return ResponseEntity.ok(ApiResponse.success("Survey response updated successfully", response));
  }

  /**
   * Deletes a survey response.
   *
   * @param id primary key identifier of the response to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSurveyResponse(@PathVariable @Positive Long id) {
    surveyResponseService.deleteSurveyResponse(id);
    return ResponseEntity.ok(ApiResponse.success("Survey response deleted successfully"));
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
