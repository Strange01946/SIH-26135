package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.TraineeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Validated
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
  public ResponseEntity<ApiResponse<TraineeResponse>> getById(@PathVariable @Positive Long id) {
    TraineeResponse response = traineeService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves trainees with optional filtering by registration number or user ID,
   * or returns a paginated list of trainees.
   *
   * @param registrationNumber optional registration number filter
   * @param userId optional user ID filter
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @return 200 OK with paginated trainees or single matched trainee enveloped in ApiResponse
   */
  @GetMapping
  @PreAuthorize("hasAuthority('trainee.read')")
  public ResponseEntity<ApiResponse<?>> getTrainees(
      @RequestParam(value = "registrationNumber", required = false) String registrationNumber,
      @RequestParam(value = "userId", required = false) @Positive Long userId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction) {
    if (registrationNumber != null && !registrationNumber.isBlank()) {
      TraineeResponse response = traineeService.getByRegistrationNumber(registrationNumber.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (userId != null) {
      TraineeResponse response = traineeService.getByUserId(userId);
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.TRAINEE_SORT_FIELDS, "id"
    );
    Page<TraineeResponse> trainees = traineeService.getAllTrainees(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(trainees)));
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
      @Valid @RequestBody CreateTraineeRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) @Positive Long actorUserId,
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
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateTraineeRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) @Positive Long actorUserId) {
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
      @PathVariable @Positive Long id,
      @RequestHeader(value = "X-Actor-User-Id", required = false) @Positive Long actorUserId) {
    traineeService.deleteTrainee(id, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("Trainee deleted successfully"));
  }
}
