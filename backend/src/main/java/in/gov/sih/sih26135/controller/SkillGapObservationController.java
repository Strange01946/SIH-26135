package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapObservationResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapObservationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
 * REST controller for managing Skill Gap Observation domain resources.
 *
 * <p>Base Route: /api/v1/skill-gap-observations
 * Consumes: CreateSkillGapObservationRequest, UpdateSkillGapObservationRequest
 * Produces: SkillGapObservationResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/skill-gap-observations")
public class SkillGapObservationController {

  private final SkillGapObservationService skillGapObservationService;

  public SkillGapObservationController(SkillGapObservationService skillGapObservationService) {
    this.skillGapObservationService = skillGapObservationService;
  }

  /**
   * Retrieves a skill gap observation by primary key identifier.
   *
   * @param id primary key identifier of the observation
   * @return 200 OK with SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapObservationResponse>> getById(@PathVariable Long id) {
    SkillGapObservationResponse response = skillGapObservationService.getObservationById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single skill gap observation by skill gap identifier and observation number.
   *
   * @param skillGapId skill gap identifier
   * @param observationNumber observation number within the skill gap
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapId", "observationNumber", "!severityId", "!statusId",
      "!sourceId", "!observedOn"
  })
  public ResponseEntity<ApiResponse<SkillGapObservationResponse>> getBySkillGapAndObservationNumber(
      @RequestParam("skillGapId") Long skillGapId,
      @RequestParam("observationNumber") Integer observationNumber,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillGapId", "observationNumber"));
    SkillGapObservationResponse response =
        skillGapObservationService.getObservationBySkillGapAndNumber(skillGapId, observationNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all observations linked to a specific skill gap.
   *
   * @param skillGapId skill gap identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapId", "!observationNumber", "!severityId", "!statusId",
      "!sourceId", "!observedOn"
  })
  public ResponseEntity<ApiResponse<List<SkillGapObservationResponse>>> getBySkillGap(
      @RequestParam("skillGapId") Long skillGapId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillGapId");
    List<SkillGapObservationResponse> observations =
        skillGapObservationService.getObservationsBySkillGap(skillGapId);
    return ResponseEntity.ok(ApiResponse.ok(observations));
  }

  /**
   * Retrieves all skill gap observations with a specific severity level.
   *
   * @param severityId skill gap severity reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!skillGapId", "!observationNumber", "!statusId",
      "!sourceId", "!observedOn"
  })
  public ResponseEntity<ApiResponse<List<SkillGapObservationResponse>>> getBySeverity(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    List<SkillGapObservationResponse> observations =
        skillGapObservationService.getObservationsBySeverity(severityId);
    return ResponseEntity.ok(ApiResponse.ok(observations));
  }

  /**
   * Retrieves all skill gap observations with a specific status.
   *
   * @param statusId skill gap status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!skillGapId", "!observationNumber", "!severityId",
      "!sourceId", "!observedOn"
  })
  public ResponseEntity<ApiResponse<List<SkillGapObservationResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<SkillGapObservationResponse> observations =
        skillGapObservationService.getObservationsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(observations));
  }

  /**
   * Retrieves all skill gap observations linked to a specific source.
   *
   * @param sourceId skill gap source reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceId", "!skillGapId", "!observationNumber", "!severityId",
      "!statusId", "!observedOn"
  })
  public ResponseEntity<ApiResponse<List<SkillGapObservationResponse>>> getBySource(
      @RequestParam("sourceId") Long sourceId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceId");
    List<SkillGapObservationResponse> observations =
        skillGapObservationService.getObservationsBySource(sourceId);
    return ResponseEntity.ok(ApiResponse.ok(observations));
  }

  /**
   * Retrieves all skill gap observations recorded on a specific date.
   *
   * @param observedOn observation date
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapObservationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "observedOn", "!skillGapId", "!observationNumber", "!severityId",
      "!statusId", "!sourceId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapObservationResponse>>> getByObservedOn(
      @RequestParam("observedOn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate observedOn,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "observedOn");
    List<SkillGapObservationResponse> observations =
        skillGapObservationService.getObservationsByDate(observedOn);
    return ResponseEntity.ok(ApiResponse.ok(observations));
  }

  /**
   * Creates a new skill gap observation.
   *
   * @param request observation creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SkillGapObservationResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SkillGapObservationResponse>> createObservation(
      @RequestBody CreateSkillGapObservationRequest request,
      HttpServletRequest httpRequest) {
    SkillGapObservationResponse response = skillGapObservationService.createObservation(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Skill gap observation created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing skill gap observation.
   *
   * @param id primary key identifier of the observation to update
   * @param request observation update payload
   * @return 200 OK with updated SkillGapObservationResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapObservationResponse>> updateObservation(
      @PathVariable Long id,
      @RequestBody UpdateSkillGapObservationRequest request) {
    SkillGapObservationResponse response = skillGapObservationService.updateObservation(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap observation updated successfully",
        response));
  }

  /**
   * Deletes a skill gap observation.
   *
   * @param id primary key identifier of the observation to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteObservation(@PathVariable Long id) {
    skillGapObservationService.deleteObservation(id);
    return ResponseEntity.ok(ApiResponse.success("Skill gap observation deleted successfully"));
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
