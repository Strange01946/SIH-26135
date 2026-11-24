package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.career.CareerProgress.*;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CareerProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class CareerProgressController {
  private static final String GOVERNMENT = "hasAnyAuthority('ROLE_super_admin', 'ROLE_government_admin')";
  private static final String EMPLOYMENT = GOVERNMENT + " and hasAuthority('employment.manage')";
  private static final String REMEDIATION = GOVERNMENT + " and hasAnyAuthority('assessment.manage', 'enrollment.manage')";
  private final CareerProgressService careers;

  public CareerProgressController(CareerProgressService careers) {
    this.careers = careers;
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

  @GetMapping("/career-progress/recommendations/{id}/batches")
  @PreAuthorize(REMEDIATION)
  public ApiResponse<List<BatchOption>> getRemediationBatches(@PathVariable @Positive long id) {
    return ApiResponse.ok(careers.batches(id));
  }

  @PostMapping("/career-progress/recommendations/{id}/enrollment")
  @PreAuthorize(REMEDIATION)
  public ApiResponse<SkillGapRemediationEnrollmentResponse> enrollCareerRemediation(
      @PathVariable @Positive long id, @RequestBody @Valid RemediationRequest input) {
    return ApiResponse.ok(careers.enroll(id, input.batchId()));
  }
}
