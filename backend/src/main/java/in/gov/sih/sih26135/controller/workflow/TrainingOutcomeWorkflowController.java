package in.gov.sih.sih26135.controller.workflow;

import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for orchestrating Trainee Training Outcome workflows.
 *
 * <p>Coordinates cross-domain actions across training enrollment completion,
 * assessment result recording, and certificate issuance.
 *
 * <p>Base Route: /api/v1/workflows/training-outcomes (alias: /api/v1/workflows/training-outcome)
 */
@RestController
@RequestMapping({"/api/v1/workflows/training-outcomes", "/api/v1/workflows/training-outcome"})
public class TrainingOutcomeWorkflowController {

  private final TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;

  public TrainingOutcomeWorkflowController(
      TrainingOutcomeWorkflowService trainingOutcomeWorkflowService) {
    this.trainingOutcomeWorkflowService = trainingOutcomeWorkflowService;
  }

  /**
   * Records an assessment result and conditionally issues a certificate and updates enrollment status.
   *
   * @param request workflow payload containing assessment result, optional certificate, and enrollment updates
   * @param httpRequest HTTP servlet request for URI extraction and parameter validation
   * @return 201 Created with RecordAssessmentAndIssueCertificateResponse enveloped in ApiResponse
   */
  @PostMapping("/record-assessment-and-issue-certificate")
  public ResponseEntity<ApiResponse<RecordAssessmentAndIssueCertificateResponse>> recordAssessmentResultAndIssueCertificate(
      @RequestBody RecordAssessmentAndIssueCertificateRequest request,
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + httpRequest.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
    RecordAssessmentAndIssueCertificateResponse response =
        trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Assessment result recorded and certificate issued successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Retrieves an aggregate training outcome summary for a specific trainee and enrollment.
   *
   * @param traineeId unique identifier of the trainee
   * @param enrollmentId unique identifier of the training enrollment
   * @param httpRequest HTTP servlet request for query parameter validation
   * @return 200 OK with TraineeTrainingOutcomeSummaryResponse enveloped in ApiResponse
   */
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<TraineeTrainingOutcomeSummaryResponse>> getTraineeTrainingOutcomeSummary(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "enrollmentId"));
    TraineeTrainingOutcomeSummaryResponse response =
        trainingOutcomeWorkflowService.getTraineeTrainingOutcomeSummary(traineeId, enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(response));
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
