package in.gov.sih.sih26135.controller.auth;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.registration.ApplicationStatusResponse;
import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.RegisterEmployerRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTraineeRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTrainingProviderRequest;
import in.gov.sih.sih26135.dto.registration.RegistrationReferenceDataResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Registration", description = "Public candidate and organization onboarding registration endpoints")
@Validated
@RestController
@RequestMapping("/api/v1/auth/register")
public class RegistrationController {

  private final RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @Operation(summary = "Register Trainee", description = "Creates an active trainee account and records explicit core data processing consent")
  @PostMapping("/trainee")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ApiResponse<AuthResponse>> registerTrainee(
      @Valid @RequestBody RegisterTraineeRequest request,
      HttpServletRequest httpRequest) {
    AuthResponse response = registrationService.registerTrainee(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Trainee account registered successfully", response, httpRequest.getRequestURI()));
  }

  @Operation(summary = "Register Training Provider", description = "Public application creating pending user and training provider record")
  @PostMapping("/training-provider")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> registerTrainingProvider(
      @Valid @RequestBody RegisterTrainingProviderRequest request,
      HttpServletRequest httpRequest) {
    OnboardingApplicationResponse response = registrationService.registerTrainingProvider(request);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success("Training provider application submitted successfully", response, httpRequest.getRequestURI()));
  }

  @Operation(summary = "Register Employer", description = "Public application creating pending user and employer record")
  @PostMapping("/employer")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> registerEmployer(
      @Valid @RequestBody RegisterEmployerRequest request,
      HttpServletRequest httpRequest) {
    OnboardingApplicationResponse response = registrationService.registerEmployer(request);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success("Employer application submitted successfully", response, httpRequest.getRequestURI()));
  }

  @Operation(summary = "Check Application Status", description = "Public lookup of organization application status by application number")
  @GetMapping("/status/{applicationNumber}")
  public ResponseEntity<ApiResponse<ApplicationStatusResponse>> getApplicationStatus(
      @PathVariable String applicationNumber) {
    ApplicationStatusResponse response = registrationService.getApplicationStatus(applicationNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(summary = "Get Registration Reference Data", description = "Public reference data for genders, states, districts, and organization types")
  @GetMapping("/reference-data")
  public ResponseEntity<ApiResponse<RegistrationReferenceDataResponse>> getReferenceData() {
    RegistrationReferenceDataResponse response = registrationService.getReferenceData();
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
