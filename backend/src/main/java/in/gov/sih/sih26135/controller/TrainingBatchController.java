package in.gov.sih.sih26135.controller;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.TrainingBatchService;
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
 * REST controller for managing Training Batch domain resources and batch status transitions.
 *
 * <p>Base Route: /api/v1/training-batches
 * Consumes: CreateTrainingBatchRequest, UpdateTrainingBatchRequest, ChangeBatchStatusRequest
 * Produces: TrainingBatchResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/training-batches")
@PreAuthorize("hasAuthority('course.manage')")
public class TrainingBatchController {

  private final TrainingBatchService trainingBatchService;

  public TrainingBatchController(TrainingBatchService trainingBatchService) {
    this.trainingBatchService = trainingBatchService;
  }

  /**
   * Retrieves a training batch by primary key identifier.
   *
   * @param id primary key identifier of the training batch
   * @return 200 OK with TrainingBatchResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingBatchResponse>> getById(@PathVariable Long id) {
    TrainingBatchResponse response = trainingBatchService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves training batches with optional filtering by code, course ID, provider ID, center ID, program ID, or status ID.
   *
   * @param code optional batch code filter
   * @param courseId optional course identifier filter
   * @param providerId optional provider identifier filter
   * @param centerId optional center identifier filter
   * @param programId optional program identifier filter
   * @param statusId optional status identifier filter
   * @return 200 OK with list of training batches or single matched batch enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getTrainingBatches(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "courseId", required = false) Long courseId,
      @RequestParam(value = "providerId", required = false) Long providerId,
      @RequestParam(value = "centerId", required = false) Long centerId,
      @RequestParam(value = "programId", required = false) Long programId,
      @RequestParam(value = "statusId", required = false) Long statusId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction) {
    if (code != null && !code.isBlank()) {
      TrainingBatchResponse response = trainingBatchService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.TRAINING_BATCH_SORT_FIELDS, "id"
    );
    if (courseId != null) {
      Page<TrainingBatchResponse> batches = trainingBatchService.getBatchesByCourseId(courseId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
    }
    if (providerId != null) {
      Page<TrainingBatchResponse> batches = trainingBatchService.getBatchesByProviderId(providerId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
    }
    if (centerId != null) {
      Page<TrainingBatchResponse> batches = trainingBatchService.getBatchesByCenterId(centerId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
    }
    if (programId != null) {
      Page<TrainingBatchResponse> batches = trainingBatchService.getBatchesByProgramId(programId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
    }
    if (statusId != null) {
      Page<TrainingBatchResponse> batches = trainingBatchService.getBatchesByStatusId(statusId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
    }
    Page<TrainingBatchResponse> batches = trainingBatchService.getAllBatches(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(batches)));
  }

  /**
   * Creates a new training batch record.
   *
   * @param request training batch creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TrainingBatchResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TrainingBatchResponse>> createBatch(
      @Valid @RequestBody CreateTrainingBatchRequest request,
      HttpServletRequest httpRequest) {
    TrainingBatchResponse response = trainingBatchService.createBatch(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Training batch created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing training batch record.
   *
   * @param id primary key identifier of the training batch to update
   * @param request training batch update payload
   * @return 200 OK with updated TrainingBatchResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingBatchResponse>> updateBatch(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTrainingBatchRequest request) {
    TrainingBatchResponse response = trainingBatchService.updateBatch(id, request);
    return ResponseEntity.ok(ApiResponse.success("Training batch updated successfully", response));
  }

  /**
   * Updates the lifecycle status of a training batch.
   *
   * @param id primary key identifier of the training batch
   * @param request batch status change payload
   * @return 200 OK with updated TrainingBatchResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<ApiResponse<TrainingBatchResponse>> updateBatchStatus(
      @PathVariable Long id,
      @Valid @RequestBody ChangeBatchStatusRequest request) {
    TrainingBatchResponse response = trainingBatchService.updateBatchStatus(id, request);
    return ResponseEntity.ok(ApiResponse.success("Training batch status updated successfully", response));
  }

  /**
   * Soft-deletes a training batch record.
   *
   * @param id primary key identifier of the training batch to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable Long id) {
    trainingBatchService.deleteBatch(id);
    return ResponseEntity.ok(ApiResponse.success("Training batch deleted successfully"));
  }
}
