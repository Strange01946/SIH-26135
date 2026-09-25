package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TraineeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Trainee domain resources.
 *
 * <p>Base Route: /api/v1/trainees
 * Consumes: CreateTraineeRequest, UpdateTraineeRequest
 * Produces: TraineeResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/trainees")
public class TraineeController {

  private final TraineeService traineeService;

  public TraineeController(TraineeService traineeService) {
    this.traineeService = traineeService;
  }

  /**
   * Retrieves a trainee by primary key identifier.
   *
   * @param id primary key identifier of the trainee
   * @return 200 OK with TraineeResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('trainee.read')")
  public ResponseEntity<ApiResponse<TraineeResponse>> getById(@PathVariable Long id) {
    TraineeResponse response = traineeService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves trainees with optional filtering by registration number or user ID.
   *
   * @param registrationNumber optional registration number filter
   * @param userId optional user ID filter
   * @return 200 OK with list of trainees or single matched trainee enveloped in ApiResponse
   */
  @GetMapping
  @PreAuthorize("hasAuthority('trainee.read')")
  public ResponseEntity<ApiResponse<?>> getTrainees(
      @RequestParam(value = "registrationNumber", required = false) String registrationNumber,
      @RequestParam(value = "userId", required = false) Long userId) {
    if (registrationNumber != null && !registrationNumber.isBlank()) {
      TraineeResponse response = traineeService.getByRegistrationNumber(registrationNumber.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (userId != null) {
      TraineeResponse response = traineeService.getByUserId(userId);
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    List<TraineeResponse> trainees = traineeService.getAllTrainees();
    return ResponseEntity.ok(ApiResponse.ok(trainees));
  }

  /**
   * Creates a new trainee record.
   *
   * @param request trainee creation payload
   * @param actorUserId optional ID of the actor creating the trainee
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TraineeResponse enveloped in ApiResponse
   */
  @PostMapping
  @PreAuthorize("hasAuthority('trainee.write')")
  public ResponseEntity<ApiResponse<TraineeResponse>> createTrainee(
      @RequestBody CreateTraineeRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId,
      HttpServletRequest httpRequest) {
    TraineeResponse response = traineeService.createTrainee(request, actorUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Trainee created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing trainee record.
   *
   * @param id primary key identifier of the trainee to update
   * @param request trainee update payload
   * @param actorUserId optional ID of the actor performing the update
   * @return 200 OK with updated TraineeResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('trainee.write')")
  public ResponseEntity<ApiResponse<TraineeResponse>> updateTrainee(
      @PathVariable Long id,
      @RequestBody UpdateTraineeRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    TraineeResponse response = traineeService.updateTrainee(id, request, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("Trainee updated successfully", response));
  }

  /**
   * Soft-deletes a trainee record.
   *
   * @param id primary key identifier of the trainee to delete
   * @param actorUserId optional ID of the actor performing the deletion
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('trainee.write')")
  public ResponseEntity<ApiResponse<Void>> deleteTrainee(
      @PathVariable Long id,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    traineeService.deleteTrainee(id, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("Trainee deleted successfully"));
  }
}
