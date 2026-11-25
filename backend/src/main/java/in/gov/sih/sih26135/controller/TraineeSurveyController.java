package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TraineeSurveyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/trainees/me/followup-surveys")
@PreAuthorize("hasAuthority('ROLE_trainee')")
@SecurityRequirement(name = "bearerAuth")
public class TraineeSurveyController {
  private final TraineeSurveyService service;

  public TraineeSurveyController(TraineeSurveyService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<TraineeSurvey.Overview> getOverview() {
    return ApiResponse.ok(service.getOverview());
  }

  @GetMapping("/career-update")
  public ApiResponse<TraineeSurvey.CareerUpdateQuestionnaire> getCareerUpdate() {
    return ApiResponse.ok(service.getCareerUpdate());
  }

  @PostMapping("/career-update")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TraineeSurvey.Submission> submitCareerUpdate(@Valid @RequestBody TraineeSurvey.SubmitRequest request) {
    return ApiResponse.ok(service.submitCareerUpdate(request));
  }

  @GetMapping("/submissions/{id}")
  public ApiResponse<TraineeSurvey.SubmissionDetail> getOwnSurveySubmission(@PathVariable @Positive long id) {
    return ApiResponse.ok(service.getOwnSubmission(id));
  }

  @GetMapping("/{taskId}")
  public ApiResponse<TraineeSurvey.Questionnaire> getQuestionnaire(@PathVariable @Positive long taskId) {
    return ApiResponse.ok(service.getQuestionnaire(taskId));
  }

  @PostMapping("/{taskId}/responses")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TraineeSurvey.Submission> submitQuestionnaire(
      @PathVariable @Positive long taskId, @Valid @RequestBody TraineeSurvey.SubmitRequest request) {
    return ApiResponse.ok(service.submit(taskId, request));
  }
}
