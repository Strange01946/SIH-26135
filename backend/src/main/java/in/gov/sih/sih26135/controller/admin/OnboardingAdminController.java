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
@PreAuthorize(OnboardingAdminService.VIEW_ACCESS)
public class OnboardingAdminController {

  private final OnboardingAdminService onboardingAdminService;

  public OnboardingAdminController(OnboardingAdminService onboardingAdminService) {
    this.onboardingAdminService = onboardingAdminService;
  }

  @Operation(summary = "List Onboarding Applications",
      description = "List applications by optional status. State administrators and district officers see only their assigned jurisdiction.")
  @GetMapping("/applications")
  public ResponseEntity<ApiResponse<List<OnboardingApplicationResponse>>> getApplications(
      @RequestParam(value = "status", required = false) String status) {
    List<OnboardingApplicationResponse> applications = onboardingAdminService.getApplications(status);
    return ResponseEntity.ok(ApiResponse.ok(applications));
  }

  @Operation(summary = "Get Onboarding Application Details", description = "Retrieve an application in the reviewer's permitted jurisdiction.")
  @GetMapping("/applications/{id}")
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> getApplicationById(
      @PathVariable @Positive Long id) {
    OnboardingApplicationResponse application = onboardingAdminService.getApplicationById(id);
    return ResponseEntity.ok(ApiResponse.ok(application));
  }

  @Operation(summary = "Approve Application", description = "Approve a pending application in the permitted jurisdiction and activate the portal account.")
  @PostMapping("/applications/{id}/approve")
  @PreAuthorize(OnboardingAdminService.REVIEW_ACCESS)
  public ResponseEntity<ApiResponse<OnboardingApplicationResponse>> approveApplication(
      @PathVariable @Positive Long id,
      @AuthenticationPrincipal UserPrincipal principal) {
    Long reviewerId = principal != null ? principal.getId() : null;
    OnboardingApplicationResponse response = onboardingAdminService.approveApplication(id, reviewerId);
    return ResponseEntity.ok(ApiResponse.success("Application approved successfully", response));
  }

  @Operation(summary = "Reject Application", description = "Reject a pending application in the permitted jurisdiction and disable the portal account.")
  @PostMapping("/applications/{id}/reject")
  @PreAuthorize(OnboardingAdminService.REVIEW_ACCESS)
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
