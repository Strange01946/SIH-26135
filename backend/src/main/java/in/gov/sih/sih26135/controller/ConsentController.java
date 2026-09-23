package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TraineeConsentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * REST controller for managing Trainee Consent domain resources.
 *
 * <p>Base Route: /api/v1/consents
 * Consumes: CreateTraineeConsentRequest, UpdateTraineeConsentStatusRequest
 * Produces: TraineeConsentResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/consents")
public class ConsentController {

  private final TraineeConsentService traineeConsentService;

  public ConsentController(TraineeConsentService traineeConsentService) {
    this.traineeConsentService = traineeConsentService;
  }

  /**
   * Retrieves a consent record by primary key identifier.
   *
   * @param id primary key identifier of the consent
   * @return 200 OK with TraineeConsentResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeConsentResponse>> getById(@PathVariable Long id) {
    TraineeConsentResponse response = traineeConsentService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves consent records for a trainee with optional filtering by type or status.
   *
   * @param traineeId mandatory trainee identifier
   * @param consentTypeId optional consent type identifier filter
   * @param consentStatusId optional consent status identifier filter
   * @return 200 OK with list of TraineeConsentResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<TraineeConsentResponse>>> getConsents(
      @RequestParam(value = "traineeId", required = false) Long traineeId,
      @RequestParam(value = "consentTypeId", required = false) Long consentTypeId,
      @RequestParam(value = "consentStatusId", required = false) Long consentStatusId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required to query consents", "MISSING_REQUIRED_PARAMETER");
    }
    if (consentTypeId != null) {
      List<TraineeConsentResponse> consents = traineeConsentService.getConsentsByTraineeAndType(traineeId, consentTypeId);
      return ResponseEntity.ok(ApiResponse.ok(consents));
    }
    if (consentStatusId != null) {
      List<TraineeConsentResponse> consents = traineeConsentService.getConsentsByTraineeAndStatus(traineeId, consentStatusId);
      return ResponseEntity.ok(ApiResponse.ok(consents));
    }
    List<TraineeConsentResponse> consents = traineeConsentService.getConsentsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(consents));
  }

  /**
   * Creates a new trainee consent record.
   *
   * @param request consent creation payload
   * @param actorUserId optional ID of the actor capturing the consent
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TraineeConsentResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TraineeConsentResponse>> createConsent(
      @RequestBody CreateTraineeConsentRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId,
      HttpServletRequest httpRequest) {
    TraineeConsentResponse response = traineeConsentService.createConsent(request, actorUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Consent recorded successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates consent status and/or revocation timestamp.
   *
   * @param id primary key identifier of the consent record
   * @param request consent status update payload
   * @param actorUserId optional ID of the actor updating the status
   * @return 200 OK with updated TraineeConsentResponse enveloped in ApiResponse
   */
  @PutMapping(path = {"/{id}", "/{id}/status"})
  public ResponseEntity<ApiResponse<TraineeConsentResponse>> updateConsentStatus(
      @PathVariable Long id,
      @RequestBody UpdateTraineeConsentStatusRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    TraineeConsentResponse response = traineeConsentService.updateConsentStatus(id, request, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("Consent status updated successfully", response));
  }
}
