package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
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
 * REST controller for managing Skill Gap Recommendation domain resources.
 *
 * <p>Base Route: /api/v1/skill-gap-recommendations
 * Consumes: CreateSkillGapRecommendationRequest, UpdateSkillGapRecommendationRequest
 * Produces: SkillGapRecommendationResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/skill-gap-recommendations")
@PreAuthorize("hasAuthority('assessment.manage')")
public class SkillGapRecommendationController {

  private final SkillGapRecommendationService skillGapRecommendationService;

  public SkillGapRecommendationController(SkillGapRecommendationService skillGapRecommendationService) {
    this.skillGapRecommendationService = skillGapRecommendationService;
  }

  /**
   * Retrieves a skill gap recommendation by primary key identifier.
   *
   * @param id primary key identifier of the recommendation
   * @return 200 OK with SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> getById(@PathVariable Long id) {
    SkillGapRecommendationResponse response = skillGapRecommendationService.getRecommendationById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single skill gap recommendation by skill gap identifier and recommendation number.
   *
   * @param skillGapId skill gap identifier
   * @param recommendationNumber recommendation sequence number within the skill gap
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapId", "recommendationNumber", "!isAccepted", "!actionTypeId",
      "!courseId", "!skillId"
  })
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> getBySkillGapAndRecommendationNumber(
      @RequestParam("skillGapId") Long skillGapId,
      @RequestParam("recommendationNumber") Integer recommendationNumber,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillGapId", "recommendationNumber"));
    SkillGapRecommendationResponse response =
        skillGapRecommendationService.getRecommendationBySkillGapAndNumber(skillGapId, recommendationNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves accepted skill gap recommendations for a specific skill gap.
   *
   * @param skillGapId skill gap identifier
   * @param isAccepted boolean indicating accepted status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapId", "isAccepted", "!recommendationNumber", "!actionTypeId",
      "!courseId", "!skillId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapRecommendationResponse>>> getAcceptedBySkillGap(
      @RequestParam("skillGapId") Long skillGapId,
      @RequestParam("isAccepted") boolean isAccepted,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillGapId", "isAccepted"));
    if (!isAccepted) {
      throw new BadRequestException(
          "Only isAccepted=true is supported for accepted skill gap recommendations filter",
          "INVALID_FILTER_PARAMETER");
    }
    List<SkillGapRecommendationResponse> recommendations =
        skillGapRecommendationService.getAcceptedRecommendationsBySkillGap(skillGapId);
    return ResponseEntity.ok(ApiResponse.ok(recommendations));
  }

  /**
   * Retrieves all skill gap recommendations linked to a specific skill gap.
   *
   * @param skillGapId skill gap identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapId", "!recommendationNumber", "!isAccepted", "!actionTypeId",
      "!courseId", "!skillId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapRecommendationResponse>>> getBySkillGap(
      @RequestParam("skillGapId") Long skillGapId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillGapId");
    List<SkillGapRecommendationResponse> recommendations =
        skillGapRecommendationService.getRecommendationsBySkillGap(skillGapId);
    return ResponseEntity.ok(ApiResponse.ok(recommendations));
  }

  /**
   * Retrieves all skill gap recommendations for a specific action type.
   *
   * @param actionTypeId skill gap action type reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "actionTypeId", "!skillGapId", "!recommendationNumber", "!isAccepted",
      "!courseId", "!skillId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapRecommendationResponse>>> getByActionType(
      @RequestParam("actionTypeId") Long actionTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "actionTypeId");
    List<SkillGapRecommendationResponse> recommendations =
        skillGapRecommendationService.getRecommendationsByActionType(actionTypeId);
    return ResponseEntity.ok(ApiResponse.ok(recommendations));
  }

  /**
   * Retrieves all skill gap recommendations for a specific course.
   *
   * @param courseId course identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "courseId", "!skillGapId", "!recommendationNumber", "!isAccepted",
      "!actionTypeId", "!skillId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapRecommendationResponse>>> getByCourse(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    List<SkillGapRecommendationResponse> recommendations =
        skillGapRecommendationService.getRecommendationsByCourse(courseId);
    return ResponseEntity.ok(ApiResponse.ok(recommendations));
  }

  /**
   * Retrieves all skill gap recommendations for a specific skill.
   *
   * @param skillId skill identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillId", "!skillGapId", "!recommendationNumber", "!isAccepted",
      "!actionTypeId", "!courseId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapRecommendationResponse>>> getBySkill(
      @RequestParam("skillId") Long skillId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillId");
    List<SkillGapRecommendationResponse> recommendations =
        skillGapRecommendationService.getRecommendationsBySkill(skillId);
    return ResponseEntity.ok(ApiResponse.ok(recommendations));
  }

  /**
   * Creates a new skill gap recommendation.
   *
   * @param request recommendation creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> createRecommendation(
      @RequestBody CreateSkillGapRecommendationRequest request,
      HttpServletRequest httpRequest) {
    SkillGapRecommendationResponse response = skillGapRecommendationService.createRecommendation(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Skill gap recommendation created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing skill gap recommendation.
   *
   * @param id primary key identifier of the recommendation to update
   * @param request recommendation update payload
   * @return 200 OK with updated SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> updateRecommendation(
      @PathVariable Long id,
      @RequestBody UpdateSkillGapRecommendationRequest request) {
    SkillGapRecommendationResponse response = skillGapRecommendationService.updateRecommendation(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap recommendation updated successfully",
        response));
  }

  /**
   * Deletes a skill gap recommendation.
   *
   * @param id primary key identifier of the recommendation to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteRecommendation(@PathVariable Long id) {
    skillGapRecommendationService.deleteRecommendation(id);
    return ResponseEntity.ok(ApiResponse.success("Skill gap recommendation deleted successfully"));
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
