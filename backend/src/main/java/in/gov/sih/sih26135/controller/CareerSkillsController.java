package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.career.CareerSkills.*;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CareerSkillsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/career-skills")
@SecurityRequirement(name = "bearerAuth")
public class CareerSkillsController {
  private static final String TRAINEE = "hasRole('trainee')";
  private static final String GOVERNMENT = "hasAnyRole('super_admin', 'government_admin')";
  private static final String ASSESSOR = "hasRole('assessor') and hasAuthority('assessment.manage')";
  private final CareerSkillsService career;

  public CareerSkillsController(CareerSkillsService career) {
    this.career = career;
  }

  @GetMapping("/reference")
  @PreAuthorize(TRAINEE + " or " + GOVERNMENT + " or (" + ASSESSOR + ")")
  public ApiResponse<Reference> careerSkillsReference() {
    return ApiResponse.ok(career.reference());
  }

  @GetMapping("/me")
  @PreAuthorize(TRAINEE)
  public ApiResponse<Profile> myCareerSkills() {
    return ApiResponse.ok(career.ownProfile());
  }

  @PutMapping("/me")
  @PreAuthorize(TRAINEE)
  public ApiResponse<Profile> saveMyCareerSkills(@Valid @RequestBody Save input) {
    return ApiResponse.ok(career.save(input));
  }

  @GetMapping("/me/training")
  @PreAuthorize(TRAINEE)
  public ApiResponse<List<Training>> myCareerTraining(@RequestParam(required = false) @Size(max = 120) String search) {
    return ApiResponse.ok(career.ownTraining(search));
  }

  @PutMapping("/me/training/{batchId}/interest")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(TRAINEE)
  public void saveMyTrainingInterest(@PathVariable @Positive long batchId, @Valid @RequestBody Interest input) {
    career.saveInterest(batchId, input);
  }

  @GetMapping("/trainees/{traineeId}")
  @PreAuthorize(GOVERNMENT + " and hasAuthority('employment.manage')")
  public ApiResponse<Profile> governmentCareerSkills(@PathVariable @Positive long traineeId) {
    return ApiResponse.ok(career.governmentProfile(traineeId));
  }

  @PutMapping("/roles/{roleId}/requirements")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(GOVERNMENT + " and hasAuthority('assessment.manage')")
  public void saveCareerRoleRequirement(@PathVariable @Positive long roleId, @Valid @RequestBody RoleRequirement input) {
    career.saveRequirement(roleId, input);
  }

  @GetMapping("/assessments/{assessmentId}/trainees/{traineeId}")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<Profile> assessmentCareerSkills(@PathVariable @Positive long assessmentId, @PathVariable @Positive long traineeId) {
    return ApiResponse.ok(career.assessmentProfile(assessmentId, traineeId));
  }

  @PutMapping("/assessments/{assessmentId}/trainees/{traineeId}")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<Profile> recordCareerSkillObservation(@PathVariable @Positive long assessmentId, @PathVariable @Positive long traineeId,
                                                         @Valid @RequestBody Observe input) {
    return ApiResponse.ok(career.recordEvidence(assessmentId, traineeId, input));
  }

  @GetMapping("/provider/batches/{batchId}/interest")
  @PreAuthorize("hasRole('training_provider') and hasAuthority('venue.booking.request')")
  public ApiResponse<InterestCount> providerTrainingInterest(@PathVariable @Positive long batchId) {
    return ApiResponse.ok(career.providerInterest(batchId));
  }
}
