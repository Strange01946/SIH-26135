package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
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
 * REST controller for managing Training Enrollment domain resources.
 *
 * <p>Base Route: /api/v1/training-enrollments
 * Consumes: CreateTrainingEnrollmentRequest, UpdateTrainingEnrollmentRequest
 * Produces: TrainingEnrollmentResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/training-enrollments")
public class TrainingEnrollmentController {

  private final TrainingEnrollmentService trainingEnrollmentService;

  public TrainingEnrollmentController(TrainingEnrollmentService trainingEnrollmentService) {
    this.trainingEnrollmentService = trainingEnrollmentService;
  }

  /**
   * Retrieves a training enrollment by primary key identifier.
   *
   * @param id primary key identifier of the training enrollment
   * @return 200 OK with TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingEnrollmentResponse>> getById(@PathVariable Long id) {
    TrainingEnrollmentResponse response = trainingEnrollmentService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single training enrollment by unique enrollment number.
   *
   * @param enrollmentNumber unique enrollment number
   * @return 200 OK with TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "enrollmentNumber")
  public ResponseEntity<ApiResponse<TrainingEnrollmentResponse>> getByEnrollmentNumber(
      @RequestParam("enrollmentNumber") String enrollmentNumber) {
    TrainingEnrollmentResponse response = trainingEnrollmentService.getByEnrollmentNumber(enrollmentNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all training enrollments for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments for a specific batch.
   *
   * @param batchId training batch identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "batchId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByBatchId(
      @RequestParam("batchId") Long batchId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByBatchId(batchId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments under a specific program.
   *
   * @param programId program identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "programId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByProgramId(
      @RequestParam("programId") Long programId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByProgramId(programId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments for a specific course.
   *
   * @param courseId course identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "courseId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByCourseId(
      @RequestParam("courseId") Long courseId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByCourseId(courseId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments with a specific training provider.
   *
   * @param providerId training provider identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "providerId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByProviderId(
      @RequestParam("providerId") Long providerId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByProviderId(providerId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments at a specific training center.
   *
   * @param centerId training center identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "centerId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByCenterId(
      @RequestParam("centerId") Long centerId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByCenterId(centerId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments with a specific enrollment status.
   *
   * @param statusId enrollment status identifier
   * @return 200 OK with list of TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "statusId")
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId) {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Retrieves all training enrollments.
   *
   * @return 200 OK with list of all TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<TrainingEnrollmentResponse>>> getAllEnrollments() {
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getAllEnrollments();
    return ResponseEntity.ok(ApiResponse.ok(enrollments));
  }

  /**
   * Creates a new training enrollment record.
   *
   * @param request training enrollment creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TrainingEnrollmentResponse>> createEnrollment(
      @RequestBody CreateTrainingEnrollmentRequest request,
      HttpServletRequest httpRequest) {
    TrainingEnrollmentResponse response = trainingEnrollmentService.createEnrollment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Training enrollment created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing training enrollment record.
   *
   * @param id primary key identifier of the training enrollment to update
   * @param request training enrollment update payload
   * @return 200 OK with updated TrainingEnrollmentResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingEnrollmentResponse>> updateEnrollment(
      @PathVariable Long id,
      @RequestBody UpdateTrainingEnrollmentRequest request) {
    TrainingEnrollmentResponse response = trainingEnrollmentService.updateEnrollment(id, request);
    return ResponseEntity.ok(ApiResponse.success("Training enrollment updated successfully", response));
  }

  /**
   * Soft-deletes a training enrollment record.
   *
   * @param id primary key identifier of the training enrollment to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEnrollment(@PathVariable Long id) {
    trainingEnrollmentService.deleteEnrollment(id);
    return ResponseEntity.ok(ApiResponse.success("Training enrollment deleted successfully"));
  }
}
