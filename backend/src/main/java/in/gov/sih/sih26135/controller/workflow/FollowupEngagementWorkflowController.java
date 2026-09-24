package in.gov.sih.sih26135.controller.workflow;

import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.RecordEngagementAttemptResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.TraineeEngagementHistoryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.FollowupEngagementWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for orchestrating Longitudinal Follow-up and Trainee Engagement workflows.
 *
 * <p>Coordinates cross-domain actions across longitudinal campaign tasks, communication
 * logs, survey response completions, and comprehensive trainee engagement history tracking.
 *
 * <p>Base Route: /api/v1/workflows/followup-engagement (alias: /api/v1/workflows/followup-engagements)
 */
@RestController
@RequestMapping({"/api/v1/workflows/followup-engagement", "/api/v1/workflows/followup-engagements"})
public class FollowupEngagementWorkflowController {

  private final FollowupEngagementWorkflowService followupEngagementWorkflowService;

  public FollowupEngagementWorkflowController(
      FollowupEngagementWorkflowService followupEngagementWorkflowService) {
    this.followupEngagementWorkflowService = followupEngagementWorkflowService;
  }

  /**
   * Records a follow-up survey response and synchronizes the associated task status and outcome.
   *
   * @param request workflow payload containing task ID, survey response specs, and task status updates
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with RecordFollowupSurveyResponseResponse enveloped in ApiResponse
   */
  @PostMapping("/record-survey-response")
  public ResponseEntity<ApiResponse<RecordFollowupSurveyResponseResponse>> recordSurveyResponse(
      @RequestBody RecordFollowupSurveyResponseRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    RecordFollowupSurveyResponseResponse response =
        followupEngagementWorkflowService.recordFollowupSurveyResponse(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Follow-up survey response recorded successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Records a communication engagement attempt and updates the follow-up task status and channel.
   *
   * @param request workflow payload containing task ID, communication log specs, and task status updates
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with RecordEngagementAttemptResponse enveloped in ApiResponse
   */
  @PostMapping("/record-engagement-attempt")
  public ResponseEntity<ApiResponse<RecordEngagementAttemptResponse>> recordEngagementAttempt(
      @RequestBody RecordEngagementAttemptRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    RecordEngagementAttemptResponse response =
        followupEngagementWorkflowService.recordEngagementAttempt(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Engagement attempt recorded successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Retrieves the comprehensive longitudinal engagement history for a specific trainee.
   *
   * @param traineeId unique identifier of the trainee
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with TraineeEngagementHistoryResponse enveloped in ApiResponse
   */
  @GetMapping("/history")
  public ResponseEntity<ApiResponse<TraineeEngagementHistoryResponse>> getTraineeEngagementHistory(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    TraineeEngagementHistoryResponse response =
        followupEngagementWorkflowService.getTraineeEngagementHistory(traineeId);
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
