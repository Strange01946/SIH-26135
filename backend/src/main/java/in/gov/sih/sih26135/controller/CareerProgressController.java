package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.career.CareerProgress.*;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CareerProgressService;
import in.gov.sih.sih26135.service.TraineeSurveyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class CareerProgressController {
  private static final String GOVERNMENT = "hasAnyAuthority('ROLE_super_admin', 'ROLE_government_admin')";
  private static final String EMPLOYMENT = GOVERNMENT + " and hasAuthority('employment.manage')";
  private static final String SURVEYS = EMPLOYMENT + " and hasAuthority('survey.manage')";
  private final CareerProgressService careers;
  private final TraineeSurveyService surveys;

  public CareerProgressController(CareerProgressService careers, TraineeSurveyService surveys) {
    this.careers = careers;
    this.surveys = surveys;
  }

  @GetMapping("/career-progress/candidates")
  @PreAuthorize(EMPLOYMENT)
  public ApiResponse<List<Candidate>> findCareerCandidates(@RequestParam @NotBlank @Size(min = 2, max = 160) String search) {
    return ApiResponse.ok(careers.findCandidates(search));
  }

  @GetMapping("/career-progress/candidates/{id}")
  @PreAuthorize(EMPLOYMENT)
  public ApiResponse<CandidateHistory> getCareerHistory(@PathVariable @Positive long id) {
    return ApiResponse.ok(careers.history(id));
  }

  @GetMapping("/career-progress/candidates/{id}/surveys")
  @PreAuthorize(SURVEYS)
  public ApiResponse<TraineeSurvey.StaffOverview> getCandidateFollowups(@PathVariable @Positive long id) {
    return ApiResponse.ok(surveys.getStaffOverview(id));
  }

  @PostMapping("/career-progress/candidates/{id}/surveys")
  @PreAuthorize(SURVEYS)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TraineeSurvey.Assignment> scheduleCandidateFollowup(@PathVariable @Positive long id,
      @Valid @RequestBody TraineeSurvey.ScheduleRequest request) {
    return ApiResponse.ok(surveys.schedule(id, request));
  }

  @GetMapping("/career-progress/candidates/{id}/surveys/{submissionId}")
  @PreAuthorize(SURVEYS)
  public ApiResponse<TraineeSurvey.SubmissionDetail> getCandidateSurveySubmission(@PathVariable @Positive long id,
      @PathVariable @Positive long submissionId) {
    return ApiResponse.ok(surveys.getStaffSubmission(id, submissionId));
  }

  @GetMapping("/employments/{id}/retention")
  @PreAuthorize("(" + EMPLOYMENT + ") or hasAuthority('ROLE_trainee')")
  public ApiResponse<EmploymentRetention> getEmploymentRetention(@PathVariable @Positive long id) {
    return ApiResponse.ok(careers.retention(id));
  }

  @GetMapping("/career-progress/exit-options")
  @PreAuthorize(EMPLOYMENT)
  public ApiResponse<ExitOptions> getCareerExitOptions() {
    return ApiResponse.ok(careers.exitOptions());
  }

  @PostMapping("/employments/{id}/exit")
  @PreAuthorize(EMPLOYMENT)
  public ApiResponse<EmploymentExitAndUnemploymentResponse> recordCareerExit(
      @PathVariable @Positive long id, @RequestBody @Valid EmploymentExitRequest input) {
    return ApiResponse.ok(careers.recordExit(id, input));
  }

  @PostMapping("/employments/{id}/unemployment")
  @PreAuthorize(EMPLOYMENT)
  public ApiResponse<TraineeUnemploymentEventResponse> confirmCareerUnemployment(
      @PathVariable @Positive long id, @RequestBody @Valid ReportUnemploymentRequest input) {
    return ApiResponse.ok(careers.confirmUnemployment(id, input));
  }

}
