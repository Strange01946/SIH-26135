package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
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
 * REST controller for managing Trainee Assessment attempt domain resources.
 *
 * <p>Base Route: /api/v1/trainee-assessments
 * Consumes: CreateTraineeAssessmentRequest, UpdateTraineeAssessmentRequest
 * Produces: TraineeAssessmentResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/trainee-assessments")
public class TraineeAssessmentController {

  private final TraineeAssessmentService traineeAssessmentService;

  public TraineeAssessmentController(TraineeAssessmentService traineeAssessmentService) {
    this.traineeAssessmentService = traineeAssessmentService;
  }

  /**
   * Retrieves a trainee assessment attempt by primary key identifier.
   *
   * @param id primary key identifier of the trainee assessment
   * @return 200 OK with TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeAssessmentResponse>> getById(@PathVariable Long id) {
    TraineeAssessmentResponse response = traineeAssessmentService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a specific attempt for a trainee on an assessment.
   *
   * @param assessmentId assessment identifier
   * @param traineeId trainee identifier
   * @param attemptNumber attempt number
   * @return 200 OK with TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"assessmentId", "traineeId", "attemptNumber"})
  public ResponseEntity<ApiResponse<TraineeAssessmentResponse>> getByAssessmentAndTraineeAndAttempt(
      @RequestParam("assessmentId") Long assessmentId,
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("attemptNumber") Integer attemptNumber) {
    TraineeAssessmentResponse response = traineeAssessmentService.getByAssessmentAndTraineeAndAttempt(
        assessmentId, traineeId, attemptNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all trainee attempts for a specific assessment.
   *
   * @param assessmentId assessment identifier
   * @return 200 OK with list of TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "assessmentId")
  public ResponseEntity<ApiResponse<List<TraineeAssessmentResponse>>> getByAssessmentId(
      @RequestParam("assessmentId") Long assessmentId) {
    List<TraineeAssessmentResponse> records = traineeAssessmentService.getByAssessmentId(assessmentId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all trainee assessment attempts for a specific enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @return 200 OK with list of TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "enrollmentId")
  public ResponseEntity<ApiResponse<List<TraineeAssessmentResponse>>> getByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId) {
    List<TraineeAssessmentResponse> records = traineeAssessmentService.getByEnrollmentId(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all assessment attempts by a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<TraineeAssessmentResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId) {
    List<TraineeAssessmentResponse> records = traineeAssessmentService.getByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all trainee assessments conducted on a specific date.
   *
   * @param assessmentDate assessment date (ISO-8601 YYYY-MM-DD)
   * @return 200 OK with list of TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "assessmentDate")
  public ResponseEntity<ApiResponse<List<TraineeAssessmentResponse>>> getByAssessmentDate(
      @RequestParam("assessmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assessmentDate) {
    List<TraineeAssessmentResponse> records = traineeAssessmentService.getByAssessmentDate(assessmentDate);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all trainee assessment attempts.
   *
   * @return 200 OK with list of all TraineeAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<TraineeAssessmentResponse>>> getAllTraineeAssessments() {
    List<TraineeAssessmentResponse> records = traineeAssessmentService.getAllTraineeAssessments();
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Creates a new trainee assessment attempt.
   *
   * @param request trainee assessment creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TraineeAssessmentResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TraineeAssessmentResponse>> createTraineeAssessment(
      @RequestBody CreateTraineeAssessmentRequest request,
      HttpServletRequest httpRequest) {
    TraineeAssessmentResponse response = traineeAssessmentService.createTraineeAssessment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Trainee assessment created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing trainee assessment attempt.
   *
   * @param id primary key identifier of the trainee assessment to update
   * @param request trainee assessment update payload
   * @return 200 OK with updated TraineeAssessmentResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeAssessmentResponse>> updateTraineeAssessment(
      @PathVariable Long id,
      @RequestBody UpdateTraineeAssessmentRequest request) {
    TraineeAssessmentResponse response = traineeAssessmentService.updateTraineeAssessment(id, request);
    return ResponseEntity.ok(ApiResponse.success("Trainee assessment updated successfully", response));
  }

  /**
   * Deletes a trainee assessment attempt.
   *
   * @param id primary key identifier of the trainee assessment to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTraineeAssessment(@PathVariable Long id) {
    traineeAssessmentService.deleteTraineeAssessment(id);
    return ResponseEntity.ok(ApiResponse.success("Trainee assessment deleted successfully"));
  }
}
