package in.gov.sih.sih26135.controller.admin;

import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.OnboardingAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding Administration", description = "Review and approval workflow for organization onboarding applications")
@Validated
@RestController
@RequestMapping("/api/v1/admin/onboarding")
@PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_government_officer')")
public class OnboardingAdminController {

  private final OnboardingAdminService onboardingAdminService;

  public OnboardingAdminController(OnboardingAdminService onboardingAdminService) {
    this.onboardingAdminService = onboardingAdminService;
  }

  @Operation(summary = "List Onboarding Applications", description = "Retrieve list of onboarding applications with optional status filter")
  @GetMapping("/applications")
  public ResponseEntity<ApiResponse<List<OnboardingApplicationResponse>>> getApplications(
      @RequestParam(value = "status", required = false) String status) {
    List<OnboardingApplicationResponse> applications = onboardingAdminService.getApplications(status);
    return ResponseEntity.ok(ApiResponse.ok(applications));
  }

  @Operation(summary = "Get Onboarding Application Details", description = "Retrieve single application by ID")
  @GetMapping("/applications/{id}")
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> getApplicationById(
      @PathVariable @Positive Long id) {
    OnboardingApplicationResponse application = onboardingAdminService.getApplicationById(id);
    return ResponseEntity.ok(ApiResponse.ok(application));
  }

  @Operation(summary = "Approve Application", description = "Approve onboarding application, activate user, and grant accreditation")
  @PostMapping("/applications/{id}/approve")
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> approveApplication(
      @PathVariable @Positive Long id,
      @AuthenticationPrincipal UserPrincipal principal) {
    Long reviewerId = principal != null ? principal.getId() : null;
    OnboardingApplicationResponse response = onboardingAdminService.approveApplication(id, reviewerId);
    return ResponseEntity.ok(ApiResponse.success("Application approved successfully", response));
  }

  @Operation(summary = "Reject Application", description = "Reject onboarding application with reason and disable user")
  @PostMapping("/applications/{id}/reject")
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> rejectApplication(
      @PathVariable @Positive Long id,
      @RequestBody(required = false) Map<String, String> body,
      @AuthenticationPrincipal UserPrincipal principal) {
    String reason = body != null ? body.get("rejectionReason") : "Application rejected by administrator";
    Long reviewerId = principal != null ? principal.getId() : null;
    OnboardingApplicationResponse response = onboardingAdminService.rejectApplication(id, reason, reviewerId);
    return ResponseEntity.ok(ApiResponse.success("Application rejected successfully", response));
  }
}
