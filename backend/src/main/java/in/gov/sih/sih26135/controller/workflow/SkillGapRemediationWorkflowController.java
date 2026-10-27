package in.gov.sih.sih26135.controller.workflow;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for orchestrating Skill Gap Assessment and Remediation workflows.
 *
 * <p>Coordinates cross-domain actions across comprehensive skill gap assessments,
 * identified gap items, upskilling recommendations, and remediation course enrollments.
 *
 * <p>Base Route: /api/v1/workflows/skill-gap-remediation (alias: /api/v1/workflows/skill-gap-remediations)
 */
@RestController
@RequestMapping({"/api/v1/workflows/skill-gap-remediation", "/api/v1/workflows/skill-gap-remediations"})
@PreAuthorize("hasAnyAuthority('assessment.manage', 'enrollment.manage')")
public class SkillGapRemediationWorkflowController {

  private final SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;

  public SkillGapRemediationWorkflowController(
      SkillGapRemediationWorkflowService skillGapRemediationWorkflowService) {
    this.skillGapRemediationWorkflowService = skillGapRemediationWorkflowService;
  }

  /**
   * Records a comprehensive skill gap assessment along with its identified gaps and training recommendations.
   *
   * @param request workflow payload containing assessment details, gap specifications, and recommendations
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with RecordSkillGapAssessmentWithGapsResponse enveloped in ApiResponse
   */
  @PostMapping("/record-assessment-with-gaps")
  public ResponseEntity<ApiResponse<RecordSkillGapAssessmentWithGapsResponse>> recordAssessmentWithGaps(
      @Valid @RequestBody RecordSkillGapAssessmentWithGapsRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    RecordSkillGapAssessmentWithGapsResponse response =
        skillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Skill gap assessment, gaps, and recommendations recorded successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Enrolls a trainee into a recommended remediation course and updates the recommendation status.
   *
   * @param request workflow payload containing recommendation ID, enrollment specs, and recommendation status updates
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with SkillGapRemediationEnrollmentResponse enveloped in ApiResponse
   */
  @PostMapping("/enroll-remediation-course")
  public ResponseEntity<ApiResponse<SkillGapRemediationEnrollmentResponse>> enrollRemediationCourse(
      @Valid @RequestBody SkillGapRemediationEnrollmentRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    SkillGapRemediationEnrollmentResponse response =
        skillGapRemediationWorkflowService.enrollTraineeInRemediationCourse(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Trainee enrolled in remediation course successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Accepts a skill gap recommendation for a trainee.
   *
   * @param id recommendation identifier
   * @param request optional decision payload containing traineeId, remarks, actorUserId
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 200 OK with updated SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @PostMapping("/recommendations/{id}/accept")
  @PreAuthorize("hasAnyAuthority('assessment.manage', 'enrollment.manage') or hasRole('trainee')")
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> acceptRecommendation(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) TraineeRecommendationDecisionRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    SkillGapRecommendationResponse response =
        skillGapRemediationWorkflowService.acceptRecommendation(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap recommendation accepted successfully",
        response,
        httpRequest.getRequestURI()));
  }

  /**
   * Rejects a skill gap recommendation with mandatory remarks explaining the rejection.
   *
   * @param id recommendation identifier
   * @param request decision payload containing traineeId, mandatory remarks, actorUserId
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 200 OK with updated SkillGapRecommendationResponse enveloped in ApiResponse
   */
  @PostMapping("/recommendations/{id}/reject")
  @PreAuthorize("hasAnyAuthority('assessment.manage', 'enrollment.manage') or hasRole('trainee')")
  public ResponseEntity<ApiResponse<SkillGapRecommendationResponse>> rejectRecommendation(
      @PathVariable("id") Long id,
      @Valid @RequestBody(required = false) TraineeRecommendationDecisionRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    SkillGapRecommendationResponse response =
        skillGapRemediationWorkflowService.rejectRecommendation(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap recommendation rejected successfully",
        response,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves an aggregate skill gap profile for a specific trainee including assessments, gaps, and recommendations.
   *
   * @param traineeId unique identifier of the trainee
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with TraineeSkillGapProfileResponse enveloped in ApiResponse
   */
  @GetMapping("/profile")
  public ResponseEntity<ApiResponse<TraineeSkillGapProfileResponse>> getTraineeSkillGapProfile(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    TraineeSkillGapProfileResponse response =
        skillGapRemediationWorkflowService.getTraineeSkillGapProfile(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(response));
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
