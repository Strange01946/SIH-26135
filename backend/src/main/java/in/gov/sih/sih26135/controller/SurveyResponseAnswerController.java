package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseAnswerResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SurveyResponseAnswerService;
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
 * REST controller for managing Survey Response Answer domain resources.
 *
 * <p>Base Route: /api/v1/survey-response-answers
 * Consumes: CreateSurveyResponseAnswerRequest, UpdateSurveyResponseAnswerRequest
 * Produces: SurveyResponseAnswerResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/survey-response-answers")
public class SurveyResponseAnswerController {

  private final SurveyResponseAnswerService surveyResponseAnswerService;

  public SurveyResponseAnswerController(SurveyResponseAnswerService surveyResponseAnswerService) {
    this.surveyResponseAnswerService = surveyResponseAnswerService;
  }

  /**
   * Retrieves a survey response answer by primary key identifier.
   *
   * @param id primary key identifier of the answer
   * @return 200 OK with SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseAnswerResponse>> getById(@PathVariable Long id) {
    SurveyResponseAnswerResponse response = surveyResponseAnswerService.getSurveyResponseAnswerById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single answer for a specific survey response and question.
   *
   * @param surveyResponseId survey response identifier
   * @param surveyQuestionId survey question identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyResponseId", "surveyQuestionId"})
  public ResponseEntity<ApiResponse<SurveyResponseAnswerResponse>> getByResponseAndQuestion(
      @RequestParam("surveyResponseId") Long surveyResponseId,
      @RequestParam("surveyQuestionId") Long surveyQuestionId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("surveyResponseId", "surveyQuestionId"));
    SurveyResponseAnswerResponse response = surveyResponseAnswerService.getAnswerByResponseAndQuestion(surveyResponseId, surveyQuestionId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all answers for a specific survey response.
   *
   * @param surveyResponseId survey response identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyResponseId", "!surveyQuestionId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseAnswerResponse>>> getByResponse(
      @RequestParam("surveyResponseId") Long surveyResponseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyResponseId");
    List<SurveyResponseAnswerResponse> answers = surveyResponseAnswerService.getAnswersByResponse(surveyResponseId);
    return ResponseEntity.ok(ApiResponse.ok(answers));
  }

  /**
   * Retrieves all answers for a specific survey question across responses.
   *
   * @param surveyQuestionId survey question identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyQuestionId", "!surveyResponseId"})
  public ResponseEntity<ApiResponse<List<SurveyResponseAnswerResponse>>> getByQuestion(
      @RequestParam("surveyQuestionId") Long surveyQuestionId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyQuestionId");
    List<SurveyResponseAnswerResponse> answers = surveyResponseAnswerService.getAnswersByQuestion(surveyQuestionId);
    return ResponseEntity.ok(ApiResponse.ok(answers));
  }

  /**
   * Creates a new survey response answer.
   *
   * @param request answer creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SurveyResponseAnswerResponse>> createSurveyResponseAnswer(
      @RequestBody CreateSurveyResponseAnswerRequest request,
      HttpServletRequest httpRequest) {
    SurveyResponseAnswerResponse response = surveyResponseAnswerService.createSurveyResponseAnswer(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Survey response answer created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing survey response answer.
   *
   * @param id primary key identifier of the answer to update
   * @param request answer update payload
   * @return 200 OK with updated SurveyResponseAnswerResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseAnswerResponse>> updateSurveyResponseAnswer(
      @PathVariable Long id,
      @RequestBody UpdateSurveyResponseAnswerRequest request) {
    SurveyResponseAnswerResponse response = surveyResponseAnswerService.updateSurveyResponseAnswer(id, request);
    return ResponseEntity.ok(ApiResponse.success("Survey response answer updated successfully", response));
  }

  /**
   * Deletes a survey response answer.
   *
   * @param id primary key identifier of the answer to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSurveyResponseAnswer(@PathVariable Long id) {
    surveyResponseAnswerService.deleteSurveyResponseAnswer(id);
    return ResponseEntity.ok(ApiResponse.success("Survey response answer deleted successfully"));
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
