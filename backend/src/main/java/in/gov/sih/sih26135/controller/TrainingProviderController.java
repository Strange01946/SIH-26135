package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.TrainingProviderResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TrainingProviderService;
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
 * REST controller for managing Training Provider domain resources.
 *
 * <p>Base Route: /api/v1/training-providers
 * Consumes: CreateTrainingProviderRequest, UpdateTrainingProviderRequest
 * Produces: TrainingProviderResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/training-providers")
public class TrainingProviderController {

  private final TrainingProviderService trainingProviderService;

  public TrainingProviderController(TrainingProviderService trainingProviderService) {
    this.trainingProviderService = trainingProviderService;
  }

  /**
   * Retrieves a training provider by primary key identifier.
   *
   * @param id primary key identifier of the training provider
   * @return 200 OK with TrainingProviderResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingProviderResponse>> getById(@PathVariable Long id) {
    TrainingProviderResponse response = trainingProviderService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves training providers with optional filtering by code, registration number, state, or district.
   *
   * @param code optional provider code filter
   * @param registrationNumber optional registration number filter
   * @param stateId optional state identifier filter
   * @param districtId optional district identifier filter
   * @return 200 OK with list of training providers or single matched provider enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getTrainingProviders(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "registrationNumber", required = false) String registrationNumber,
      @RequestParam(value = "stateId", required = false) Long stateId,
      @RequestParam(value = "districtId", required = false) Long districtId) {
    if (code != null && !code.isBlank()) {
      TrainingProviderResponse response = trainingProviderService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (registrationNumber != null && !registrationNumber.isBlank()) {
      TrainingProviderResponse response = trainingProviderService.getByRegistrationNumber(registrationNumber.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (stateId != null) {
      List<TrainingProviderResponse> providers = trainingProviderService.getTrainingProvidersByStateId(stateId);
      return ResponseEntity.ok(ApiResponse.ok(providers));
    }
    if (districtId != null) {
      List<TrainingProviderResponse> providers = trainingProviderService.getTrainingProvidersByDistrictId(districtId);
      return ResponseEntity.ok(ApiResponse.ok(providers));
    }
    List<TrainingProviderResponse> providers = trainingProviderService.getAllTrainingProviders();
    return ResponseEntity.ok(ApiResponse.ok(providers));
  }

  /**
   * Creates a new training provider record.
   *
   * @param request training provider creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TrainingProviderResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TrainingProviderResponse>> createTrainingProvider(
      @RequestBody CreateTrainingProviderRequest request,
      HttpServletRequest httpRequest) {
    TrainingProviderResponse response = trainingProviderService.createTrainingProvider(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Training provider created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing training provider record.
   *
   * @param id primary key identifier of the training provider to update
   * @param request training provider update payload
   * @return 200 OK with updated TrainingProviderResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TrainingProviderResponse>> updateTrainingProvider(
      @PathVariable Long id,
      @RequestBody UpdateTrainingProviderRequest request) {
    TrainingProviderResponse response = trainingProviderService.updateTrainingProvider(id, request);
    return ResponseEntity.ok(ApiResponse.success("Training provider updated successfully", response));
  }

  /**
   * Soft-deletes a training provider record.
   *
   * @param id primary key identifier of the training provider to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteTrainingProvider(@PathVariable Long id) {
    trainingProviderService.deleteTrainingProvider(id);
    return ResponseEntity.ok(ApiResponse.success("Training provider deleted successfully"));
  }
}
