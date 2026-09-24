package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AssessmentResultService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
 * REST controller for managing Assessment Result domain resources.
 *
 * <p>Base Route: /api/v1/assessment-results
 * Consumes: CreateAssessmentResultRequest, UpdateAssessmentResultRequest
 * Produces: AssessmentResultResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/assessment-results")
public class AssessmentResultController {

  private final AssessmentResultService assessmentResultService;

  public AssessmentResultController(AssessmentResultService assessmentResultService) {
    this.assessmentResultService = assessmentResultService;
  }

  /**
   * Retrieves an assessment result by primary key identifier.
   *
   * @param id primary key identifier of the assessment result
   * @return 200 OK with AssessmentResultResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AssessmentResultResponse>> getById(@PathVariable Long id) {
    AssessmentResultResponse response = assessmentResultService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves the assessment result for a specific trainee assessment attempt.
   *
   * @param traineeAssessmentId trainee assessment attempt identifier
   * @return 200 OK with AssessmentResultResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeAssessmentId")
  public ResponseEntity<ApiResponse<AssessmentResultResponse>> getByTraineeAssessmentId(
      @RequestParam("traineeAssessmentId") Long traineeAssessmentId) {
    AssessmentResultResponse response = assessmentResultService.getByTraineeAssessmentId(traineeAssessmentId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all assessment results for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of AssessmentResultResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId) {
    List<AssessmentResultResponse> results = assessmentResultService.getByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(results));
  }

  /**
   * Retrieves all assessment results with a specific outcome (e.g. PASS, FAIL).
   *
   * @param outcomeId assessment outcome identifier
   * @return 200 OK with list of AssessmentResultResponse enveloped in ApiResponse
   */
  @GetMapping(params = "outcomeId")
  public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>> getByAssessmentOutcomeId(
      @RequestParam("outcomeId") Long outcomeId) {
    List<AssessmentResultResponse> results = assessmentResultService.getByAssessmentOutcomeId(outcomeId);
    return ResponseEntity.ok(ApiResponse.ok(results));
  }

  /**
   * Retrieves all assessment results.
   *
   * @return 200 OK with list of all AssessmentResultResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<AssessmentResultResponse>>> getAllAssessmentResults() {
    List<AssessmentResultResponse> results = assessmentResultService.getAllAssessmentResults();
    return ResponseEntity.ok(ApiResponse.ok(results));
  }

  /**
   * Creates a new assessment result record.
   *
   * @param request assessment result creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created AssessmentResultResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<AssessmentResultResponse>> createAssessmentResult(
      @RequestBody CreateAssessmentResultRequest request,
      HttpServletRequest httpRequest) {
    AssessmentResultResponse response = assessmentResultService.createAssessmentResult(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Assessment result created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing assessment result record.
   *
   * @param id primary key identifier of the assessment result to update
   * @param request assessment result update payload
   * @return 200 OK with updated AssessmentResultResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<AssessmentResultResponse>> updateAssessmentResult(
      @PathVariable Long id,
      @RequestBody UpdateAssessmentResultRequest request) {
    AssessmentResultResponse response = assessmentResultService.updateAssessmentResult(id, request);
    return ResponseEntity.ok(ApiResponse.success("Assessment result updated successfully", response));
  }

  /**
   * Deletes an assessment result record.
   *
   * @param id primary key identifier of the assessment result to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAssessmentResult(@PathVariable Long id) {
    assessmentResultService.deleteAssessmentResult(id);
    return ResponseEntity.ok(ApiResponse.success("Assessment result deleted successfully"));
  }
}
