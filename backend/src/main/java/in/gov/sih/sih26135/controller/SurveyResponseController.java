package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SurveyResponseService;
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

/**
 * REST controller for managing Survey Response domain resources.
 *
 * <p>Base Route: /api/v1/survey-responses
 * Consumes: CreateSurveyResponseRequest, UpdateSurveyResponseRequest
 * Produces: SurveyResponseResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/survey-responses")
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
  public ResponseEntity<ApiResponse<SurveyResponseResponse>> getById(@PathVariable Long id) {
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
      @RequestParam("surveyId") Long surveyId,
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("attemptNumber") Integer attemptNumber,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("surveyId", "traineeId", "attemptNumber"));
    SurveyResponseResponse response = surveyResponseService.getSurveyResponseByAttempt(surveyId, traineeId, attemptNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all survey responses associated with a specific survey.
   *
   * @param surveyId survey identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getBySurvey(
      @RequestParam("surveyId") Long surveyId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesBySurvey(surveyId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
  }

  /**
   * Retrieves all survey responses submitted by a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!surveyId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
  }

  /**
   * Retrieves all survey responses linked to a specific follow-up task.
   *
   * @param followupTaskId follow-up task identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"followupTaskId", "!surveyId", "!traineeId", "!attemptNumber", "!enrollmentId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getByFollowupTask(
      @RequestParam("followupTaskId") Long followupTaskId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTaskId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByFollowupTask(followupTaskId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
  }

  /**
   * Retrieves all survey responses linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"enrollmentId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!statusId", "!versionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByEnrollment(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
  }

  /**
   * Retrieves all survey responses with a specific survey response status.
   *
   * @param statusId survey response status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"statusId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!versionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
  }

  /**
   * Retrieves all survey responses linked to a specific survey template version.
   *
   * @param versionId survey template version identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "!surveyId", "!traineeId", "!attemptNumber", "!followupTaskId", "!enrollmentId", "!statusId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseResponse>>> getByTemplateVersion(
      @RequestParam("versionId") Long versionId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "versionId");
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByTemplateVersion(versionId);
    return ResponseEntity.ok(ApiResponse.ok(responses));
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
      @RequestBody CreateSurveyResponseRequest request,
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
      @PathVariable Long id,
      @RequestBody UpdateSurveyResponseRequest request) {
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
  public ResponseEntity<ApiResponse<Void>> deleteSurveyResponse(@PathVariable Long id) {
    surveyResponseService.deleteSurveyResponse(id);
    return ResponseEntity.ok(ApiResponse.success("Survey response deleted successfully"));
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
