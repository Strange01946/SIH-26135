package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.dto.trainee.TrainingRegistration.*;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TrainingRegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class TrainingRegistrationController {
  private final TrainingRegistrationService training;

  public TrainingRegistrationController(TrainingRegistrationService training) {
    this.training = training;
  }

  @GetMapping("/trainees/me/training")
  @PreAuthorize("hasRole('trainee')")
  public ApiResponse<RegistrationBatchPage> findOpenTraining(
      @RequestParam(required = false) @Size(max = 120) String search,
      @RequestParam(required = false) @Positive Long stateId,
      @RequestParam(required = false) @Positive Long districtId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate through,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(training.find(search, stateId, districtId, from, through, page, size));
  }

  @GetMapping("/trainees/me/training/{batchId}")
  @PreAuthorize("hasRole('trainee')")
  public ApiResponse<TrainingBatchDetails> viewTraining(@PathVariable @Positive long batchId) {
    return ApiResponse.ok(training.details(batchId));
  }

  @PostMapping("/trainees/me/training/{batchId}/enrollment")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('trainee')")
  public ApiResponse<TrainingEnrollmentResponse> registerForTraining(@PathVariable @Positive long batchId) {
    return ApiResponse.ok(training.register(batchId));
  }

  @GetMapping("/training-batches/{batchId}/roster")
  @PreAuthorize("hasRole('training_provider') and hasAuthority('enrollment.manage')")
  public ApiResponse<TrainingRosterPage> viewTrainingBatchRoster(@PathVariable @Positive long batchId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(training.roster(batchId, page, size));
  }
}
