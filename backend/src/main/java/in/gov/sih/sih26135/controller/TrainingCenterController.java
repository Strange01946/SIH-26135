package in.gov.sih.sih26135.controller;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.TrainingCenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Training Center domain resources.
 *
 * <p>Base Route: /api/v1/training-centers
 * Consumes: CreateTrainingCenterRequest, UpdateTrainingCenterRequest
 * Produces: TrainingCenterResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/training-centers")
@PreAuthorize("hasAuthority('center.manage')")
public class TrainingCenterController {

  private final TrainingCenterService trainingCenterService;

  public TrainingCenterController(TrainingCenterService trainingCenterService) {
    this.trainingCenterService = trainingCenterService;
  }

  /**
   * Retrieves a training center by primary key identifier.
   *
   * @param id primary key identifier of the training center
   * @return 200 OK with TrainingCenterResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingCenterResponse>> getById(@PathVariable Long id) {
    TrainingCenterResponse response = trainingCenterService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves training centers with optional filtering by code, provider ID, district ID, or state ID.
   *
   * @param code optional center code filter
   * @param providerId optional provider identifier filter
   * @param districtId optional district identifier filter
   * @param stateId optional state identifier filter
   * @return 200 OK with list of training centers or single matched training center enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getTrainingCenters(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "providerId", required = false) Long providerId,
      @RequestParam(value = "districtId", required = false) Long districtId,
      @RequestParam(value = "stateId", required = false) Long stateId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction) {
    if (code != null && !code.isBlank()) {
      TrainingCenterResponse response = trainingCenterService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.TRAINING_CENTER_SORT_FIELDS, "id"
    );
    if (providerId != null) {
      Page<TrainingCenterResponse> centers = trainingCenterService.getTrainingCentersByProviderId(providerId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(centers)));
    }
    if (districtId != null) {
      Page<TrainingCenterResponse> centers = trainingCenterService.getTrainingCentersByDistrictId(districtId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(centers)));
    }
    if (stateId != null) {
      Page<TrainingCenterResponse> centers = trainingCenterService.getTrainingCentersByStateId(stateId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(centers)));
    }
    Page<TrainingCenterResponse> centers = trainingCenterService.getAllTrainingCenters(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(centers)));
  }

  /**
   * Creates a new training center record.
   *
   * @param request training center creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TrainingCenterResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TrainingCenterResponse>> createTrainingCenter(
      @Valid @RequestBody CreateTrainingCenterRequest request,
      HttpServletRequest httpRequest) {
    TrainingCenterResponse response = trainingCenterService.createTrainingCenter(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Training center created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing training center record.
   *
   * @param id primary key identifier of the training center to update
   * @param request training center update payload
   * @return 200 OK with updated TrainingCenterResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingCenterResponse>> updateTrainingCenter(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTrainingCenterRequest request) {
    TrainingCenterResponse response = trainingCenterService.updateTrainingCenter(id, request);
    return ResponseEntity.ok(ApiResponse.success("Training center updated successfully", response));
  }

  /**
   * Soft-deletes a training center record.
   *
   * @param id primary key identifier of the training center to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTrainingCenter(@PathVariable Long id) {
    trainingCenterService.deleteTrainingCenter(id);
    return ResponseEntity.ok(ApiResponse.success("Training center deleted successfully"));
  }
}
