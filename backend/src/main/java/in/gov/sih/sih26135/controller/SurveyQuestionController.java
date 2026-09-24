package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.response.SurveyQuestionResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SurveyQuestionService;
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
 * REST controller for managing Survey Question domain resources.
 *
 * <p>Base Route: /api/v1/survey-questions
 * Consumes: CreateSurveyQuestionRequest, UpdateSurveyQuestionRequest
 * Produces: SurveyQuestionResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/survey-questions")
public class SurveyQuestionController {

  private final SurveyQuestionService surveyQuestionService;

  public SurveyQuestionController(SurveyQuestionService surveyQuestionService) {
    this.surveyQuestionService = surveyQuestionService;
  }

  /**
   * Retrieves a survey question by primary key identifier.
   *
   * @param id primary key identifier of the survey question
   * @return 200 OK with SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyQuestionResponse>> getById(@PathVariable Long id) {
    SurveyQuestionResponse response = surveyQuestionService.getSurveyQuestionById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a survey question by survey template version and question code.
   *
   * @param versionId survey template version identifier
   * @param questionCode unique question code within the version
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "questionCode", "!displayOrder", "!ordered", "!questionTypeId"})
  public ResponseEntity<ApiResponse<SurveyQuestionResponse>> getByVersionAndCode(
      @RequestParam("versionId") Long versionId,
      @RequestParam("questionCode") String questionCode,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("versionId", "questionCode"));
    SurveyQuestionResponse response = surveyQuestionService.getSurveyQuestionByVersionAndCode(versionId, questionCode);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a survey question by survey template version and display order.
   *
   * @param versionId survey template version identifier
   * @param displayOrder display order of the question within the version
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "displayOrder", "!questionCode", "!ordered", "!questionTypeId"})
  public ResponseEntity<ApiResponse<SurveyQuestionResponse>> getByVersionAndDisplayOrder(
      @RequestParam("versionId") Long versionId,
      @RequestParam("displayOrder") Integer displayOrder,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("versionId", "displayOrder"));
    SurveyQuestionResponse response = surveyQuestionService.getSurveyQuestionByVersionAndDisplayOrder(versionId, displayOrder);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves questions by survey template version, optionally sorted by display order.
   *
   * @param versionId survey template version identifier
   * @param ordered boolean indicating if the questions should be ordered by display order
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "ordered", "!questionCode", "!displayOrder", "!questionTypeId"})
  public ResponseEntity<ApiResponse<List<SurveyQuestionResponse>>> getByVersionAndOrdered(
      @RequestParam("versionId") Long versionId,
      @RequestParam("ordered") boolean ordered,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("versionId", "ordered"));
    List<SurveyQuestionResponse> questions = ordered
        ? surveyQuestionService.getQuestionsByVersionOrdered(versionId)
        : surveyQuestionService.getQuestionsByVersion(versionId);
    return ResponseEntity.ok(ApiResponse.ok(questions));
  }

  /**
   * Retrieves all questions for a specific survey template version.
   *
   * @param versionId survey template version identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"versionId", "!questionCode", "!displayOrder", "!ordered", "!questionTypeId"})
  public ResponseEntity<ApiResponse<List<SurveyQuestionResponse>>> getByVersion(
      @RequestParam("versionId") Long versionId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "versionId");
    List<SurveyQuestionResponse> questions = surveyQuestionService.getQuestionsByVersion(versionId);
    return ResponseEntity.ok(ApiResponse.ok(questions));
  }

  /**
   * Retrieves all questions of a specific question type.
   *
   * @param questionTypeId question type reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SurveyQuestionResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"questionTypeId", "!versionId", "!questionCode", "!displayOrder", "!ordered"})
  public ResponseEntity<ApiResponse<List<SurveyQuestionResponse>>> getByType(
      @RequestParam("questionTypeId") Long questionTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "questionTypeId");
    List<SurveyQuestionResponse> questions = surveyQuestionService.getQuestionsByType(questionTypeId);
    return ResponseEntity.ok(ApiResponse.ok(questions));
  }

  /**
   * Creates a new survey question.
   *
   * @param request question creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SurveyQuestionResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SurveyQuestionResponse>> createSurveyQuestion(
      @RequestBody CreateSurveyQuestionRequest request,
      HttpServletRequest httpRequest) {
    SurveyQuestionResponse response = surveyQuestionService.createSurveyQuestion(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Survey question created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing survey question.
   *
   * @param id primary key identifier of the question to update
   * @param request question update payload
   * @return 200 OK with updated SurveyQuestionResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SurveyQuestionResponse>> updateSurveyQuestion(
      @PathVariable Long id,
      @RequestBody UpdateSurveyQuestionRequest request) {
    SurveyQuestionResponse response = surveyQuestionService.updateSurveyQuestion(id, request);
    return ResponseEntity.ok(ApiResponse.success("Survey question updated successfully", response));
  }

  /**
   * Deletes a survey question.
   *
   * @param id primary key identifier of the question to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSurveyQuestion(@PathVariable Long id) {
    surveyQuestionService.deleteSurveyQuestion(id);
    return ResponseEntity.ok(ApiResponse.success("Survey question deleted successfully"));
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
