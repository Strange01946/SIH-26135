package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRequest;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapService;
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
 * REST controller for managing Skill Gap domain resources.
 *
 * <p>Base Route: /api/v1/skill-gaps
 * Consumes: CreateSkillGapRequest, UpdateSkillGapRequest
 * Produces: SkillGapResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/skill-gaps")
@PreAuthorize("hasAuthority('assessment.manage')")
public class SkillGapController {

  private final SkillGapService skillGapService;

  public SkillGapController(SkillGapService skillGapService) {
    this.skillGapService = skillGapService;
  }

  /**
   * Retrieves a skill gap by primary key identifier.
   *
   * @param id primary key identifier of the skill gap
   * @return 200 OK with SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapResponse>> getById(@PathVariable Long id) {
    SkillGapResponse response = skillGapService.getSkillGapById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single skill gap by unique skill gap number.
   *
   * @param skillGapNumber unique skill gap number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillGapNumber", "!assessmentId", "!skillId", "!traineeId",
      "!isCurrent", "!severityId", "!statusId", "!sourceId"
  })
  public ResponseEntity<ApiResponse<SkillGapResponse>> getBySkillGapNumber(
      @RequestParam("skillGapNumber") String skillGapNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillGapNumber");
    SkillGapResponse response = skillGapService.getSkillGapByNumber(skillGapNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single skill gap by assessment identifier and skill identifier.
   *
   * @param assessmentId assessment identifier
   * @param skillId skill identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assessmentId", "skillId", "!traineeId", "!isCurrent",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<SkillGapResponse>> getByAssessmentAndSkill(
      @RequestParam("assessmentId") Long assessmentId,
      @RequestParam("skillId") Long skillId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("assessmentId", "skillId"));
    SkillGapResponse response = skillGapService.getSkillGapByAssessmentAndSkill(assessmentId, skillId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all skill gaps linked to a specific assessment.
   *
   * @param assessmentId assessment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assessmentId", "!skillId", "!traineeId", "!isCurrent",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getByAssessment(
      @RequestParam("assessmentId") Long assessmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assessmentId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsByAssessment(assessmentId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves current active skill gaps for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "isCurrent", "!assessmentId", "!skillId",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getCurrentByTrainee(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "isCurrent"));
    if (!isCurrent) {
      throw new BadRequestException(
          "Only isCurrent=true is supported for current skill gaps filter",
          "INVALID_FILTER_PARAMETER");
    }
    List<SkillGapResponse> skillGaps = skillGapService.getCurrentSkillGapsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves all skill gaps for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "!isCurrent", "!assessmentId", "!skillId",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves current active skill gaps for a specific skill.
   *
   * @param skillId skill identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillId", "isCurrent", "!assessmentId", "!traineeId",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getCurrentBySkill(
      @RequestParam("skillId") Long skillId,
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillId", "isCurrent"));
    if (!isCurrent) {
      throw new BadRequestException(
          "Only isCurrent=true is supported for current skill gaps filter",
          "INVALID_FILTER_PARAMETER");
    }
    List<SkillGapResponse> skillGaps = skillGapService.getCurrentSkillGapsBySkill(skillId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves all skill gaps for a specific skill.
   *
   * @param skillId skill identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "skillId", "!isCurrent", "!assessmentId", "!traineeId",
      "!severityId", "!statusId", "!sourceId", "!skillGapNumber"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getBySkill(
      @RequestParam("skillId") Long skillId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsBySkill(skillId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves all skill gaps with a specific severity level.
   *
   * @param severityId skill gap severity reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!skillGapNumber", "!assessmentId", "!skillId",
      "!traineeId", "!isCurrent", "!statusId", "!sourceId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getBySeverity(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsBySeverity(severityId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves all skill gaps with a specific gap status.
   *
   * @param statusId skill gap status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!skillGapNumber", "!assessmentId", "!skillId",
      "!traineeId", "!isCurrent", "!severityId", "!sourceId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Retrieves all skill gaps linked to a specific gap source.
   *
   * @param sourceId skill gap source reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceId", "!skillGapNumber", "!assessmentId", "!skillId",
      "!traineeId", "!isCurrent", "!severityId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapResponse>>> getBySource(
      @RequestParam("sourceId") Long sourceId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceId");
    List<SkillGapResponse> skillGaps = skillGapService.getSkillGapsBySource(sourceId);
    return ResponseEntity.ok(ApiResponse.ok(skillGaps));
  }

  /**
   * Creates a new skill gap record.
   *
   * @param request skill gap creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SkillGapResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SkillGapResponse>> createSkillGap(
      @RequestBody CreateSkillGapRequest request,
      HttpServletRequest httpRequest) {
    SkillGapResponse response = skillGapService.createSkillGap(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Skill gap created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing skill gap record.
   *
   * @param id primary key identifier of the skill gap to update
   * @param request skill gap update payload
   * @return 200 OK with updated SkillGapResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapResponse>> updateSkillGap(
      @PathVariable Long id,
      @RequestBody UpdateSkillGapRequest request) {
    SkillGapResponse response = skillGapService.updateSkillGap(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap updated successfully",
        response));
  }

  /**
   * Deletes a skill gap record.
   *
   * @param id primary key identifier of the skill gap to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSkillGap(@PathVariable Long id) {
    skillGapService.deleteSkillGap(id);
    return ResponseEntity.ok(ApiResponse.success("Skill gap deleted successfully"));
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
