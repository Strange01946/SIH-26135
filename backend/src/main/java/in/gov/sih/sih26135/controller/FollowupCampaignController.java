package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.response.FollowupCampaignResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.FollowupCampaignService;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Follow-up Campaign domain resources.
 *
 * <p>Base Route: /api/v1/followup-campaigns
 * Consumes: CreateFollowupCampaignRequest, UpdateFollowupCampaignRequest
 * Produces: FollowupCampaignResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/followup-campaigns")
@PreAuthorize("hasAuthority('survey.manage')")
public class FollowupCampaignController {

  private final FollowupCampaignService followupCampaignService;

  public FollowupCampaignController(FollowupCampaignService followupCampaignService) {
    this.followupCampaignService = followupCampaignService;
  }

  /**
   * Retrieves a follow-up campaign by primary key identifier.
   *
   * @param id primary key identifier of the campaign
   * @return 200 OK with FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FollowupCampaignResponse>> getById(@PathVariable Long id) {
    FollowupCampaignResponse response = followupCampaignService.getFollowupCampaignById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a follow-up campaign by unique campaign code.
   *
   * @param campaignCode unique campaign code
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"campaignCode", "!followupTypeId", "!surveyId", "!programId", "!courseId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<FollowupCampaignResponse>> getByCode(
      @RequestParam("campaignCode") String campaignCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "campaignCode");
    FollowupCampaignResponse response = followupCampaignService.getFollowupCampaignByCode(campaignCode);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all follow-up campaigns for a specific follow-up type.
   *
   * @param followupTypeId follow-up type reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"followupTypeId", "!campaignCode", "!surveyId", "!programId", "!courseId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getByFollowupType(
      @RequestParam("followupTypeId") Long followupTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTypeId");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsByFollowupType(followupTypeId);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all follow-up campaigns associated with a specific survey.
   *
   * @param surveyId survey identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"surveyId", "!campaignCode", "!followupTypeId", "!programId", "!courseId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getBySurvey(
      @RequestParam("surveyId") Long surveyId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyId");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsBySurvey(surveyId);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all follow-up campaigns targeting a specific program.
   *
   * @param programId program identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"programId", "!campaignCode", "!followupTypeId", "!surveyId", "!courseId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getByProgram(
      @RequestParam("programId") Long programId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "programId");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsByProgram(programId);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all follow-up campaigns targeting a specific course.
   *
   * @param courseId course identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"courseId", "!campaignCode", "!followupTypeId", "!surveyId", "!programId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getByCourse(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsByCourse(courseId);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all follow-up campaigns with a specific lifecycle status.
   *
   * @param lifecycleStatusId lifecycle status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"lifecycleStatusId", "!campaignCode", "!followupTypeId", "!surveyId", "!programId", "!courseId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getByLifecycleStatus(
      @RequestParam("lifecycleStatusId") Long lifecycleStatusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "lifecycleStatusId");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsByLifecycleStatus(lifecycleStatusId);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all follow-up campaigns scheduled to start on a specific date.
   *
   * @param scheduledStartDate scheduled start date (ISO-8601 YYYY-MM-DD)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"scheduledStartDate", "!campaignCode", "!followupTypeId", "!surveyId", "!programId", "!courseId", "!lifecycleStatusId"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getByScheduledStartDate(
      @RequestParam("scheduledStartDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledStartDate,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "scheduledStartDate");
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getFollowupCampaignsByScheduledStartDate(scheduledStartDate);
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Retrieves all active follow-up campaigns (excluding soft-deleted campaigns).
   *
   * @param httpRequest HTTP servlet request to verify no extra query parameters are passed
   * @return 200 OK with list of active FollowupCampaignResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!campaignCode", "!followupTypeId", "!surveyId", "!programId", "!courseId", "!lifecycleStatusId", "!scheduledStartDate"})
  public ResponseEntity<ApiResponse<List<FollowupCampaignResponse>>> getAllActive(HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    List<FollowupCampaignResponse> campaigns = followupCampaignService.getAllActiveFollowupCampaigns();
    return ResponseEntity.ok(ApiResponse.ok(campaigns));
  }

  /**
   * Creates a new follow-up campaign.
   *
   * @param request campaign creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created FollowupCampaignResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<FollowupCampaignResponse>> createFollowupCampaign(
      @RequestBody CreateFollowupCampaignRequest request,
      HttpServletRequest httpRequest) {
    FollowupCampaignResponse response = followupCampaignService.createFollowupCampaign(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Follow-up campaign created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing follow-up campaign.
   *
   * @param id primary key identifier of the campaign to update
   * @param request campaign update payload
   * @return 200 OK with updated FollowupCampaignResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<FollowupCampaignResponse>> updateFollowupCampaign(
      @PathVariable Long id,
      @RequestBody UpdateFollowupCampaignRequest request) {
    FollowupCampaignResponse response = followupCampaignService.updateFollowupCampaign(id, request);
    return ResponseEntity.ok(ApiResponse.success("Follow-up campaign updated successfully", response));
  }

  /**
   * Soft deletes a follow-up campaign.
   *
   * @param id primary key identifier of the campaign to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteFollowupCampaign(@PathVariable Long id) {
    followupCampaignService.deleteFollowupCampaign(id);
    return ResponseEntity.ok(ApiResponse.success("Follow-up campaign deleted successfully"));
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
}
