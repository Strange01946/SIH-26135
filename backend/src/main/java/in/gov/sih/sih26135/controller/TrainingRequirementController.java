package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.venue.TrainingRequirementDto.*;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.impl.TrainingRequirementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training-requirements")
public class TrainingRequirementController {
  private static final String REVIEW = "hasAuthority('venue.booking.review')";
  private static final String PROVIDER = "hasAuthority('venue.booking.request')";
  private final TrainingRequirementService requirements;

  public TrainingRequirementController(TrainingRequirementService requirements) {
    this.requirements = requirements;
  }

  @GetMapping("/government")
  @PreAuthorize(REVIEW)
  public ApiResponse<List<Requirement>> listGovernmentRequirements(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(requirements.governmentList(page, size));
  }

  @PostMapping("/government")
  @PreAuthorize(REVIEW)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Requirement> publishTrainingRequirement(@Valid @RequestBody Publish request) {
    return ApiResponse.ok(requirements.publish(request));
  }

  @GetMapping("/government/{id}")
  @PreAuthorize(REVIEW)
  public ApiResponse<RequirementDetails> governmentRequirementDetails(@PathVariable @Positive long id) {
    return ApiResponse.ok(requirements.governmentDetails(id));
  }

  @PostMapping("/government/{id}/select")
  @PreAuthorize(REVIEW)
  public ApiResponse<Requirement> selectTrainingProvider(@PathVariable @Positive long id,
      @Valid @RequestBody SelectProvider request) {
    return ApiResponse.ok(requirements.select(id, request.applicationId()));
  }

  @PostMapping("/government/{id}/centre")
  @PreAuthorize(REVIEW)
  public ApiResponse<Requirement> setTrainingCentre(@PathVariable @Positive long id,
      @Valid @RequestBody SetCentre request) {
    return ApiResponse.ok(requirements.setCentre(id, request.centerId()));
  }

  @GetMapping("/open")
  @PreAuthorize(PROVIDER)
  public ApiResponse<List<Requirement>> openTrainingRequirements(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
      @RequestParam(required = false) @Positive Long stateId, @RequestParam(required = false) @Positive Long districtId) {
    return ApiResponse.ok(requirements.openRequirements(page, size, stateId, districtId));
  }

  @GetMapping("/my-applications")
  @PreAuthorize(PROVIDER)
  public ApiResponse<List<Application>> myTrainingApplications(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(requirements.myApplications(page, size));
  }

  @GetMapping("/{id}")
  @PreAuthorize(PROVIDER)
  public ApiResponse<RequirementDetails> providerRequirementDetails(@PathVariable @Positive long id) {
    return ApiResponse.ok(requirements.providerDetails(id));
  }

  @PostMapping("/{id}/applications")
  @PreAuthorize(PROVIDER)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Application> applyForTrainingRequirement(@PathVariable @Positive long id,
      @Valid @RequestBody Apply request) {
    return ApiResponse.ok(requirements.apply(id, request));
  }
}
