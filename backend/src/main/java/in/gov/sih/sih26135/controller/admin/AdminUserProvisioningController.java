package in.gov.sih.sih26135.controller.admin;

import in.gov.sih.sih26135.dto.registration.ProvisionAssessorRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionGovernmentAdminRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionSuperAdminRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.OnboardingAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin User Provisioning", description = "Privileged role provisioning endpoints for Assessors, Government Administrators, and Super Admins")
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserProvisioningController {

  private final OnboardingAdminService onboardingAdminService;

  public AdminUserProvisioningController(OnboardingAdminService onboardingAdminService) {
    this.onboardingAdminService = onboardingAdminService;
  }

  @Operation(summary = "Provision Assessor", description = "Provision an Assessor user account with assessment management authorities")
  @PostMapping("/provision-assessor")
  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_government_officer')")
  public ResponseEntity<ApiResponse<UserResponse>> provisionAssessor(
      @Valid @RequestBody ProvisionAssessorRequest request,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest httpRequest) {
    Long actorId = principal != null ? principal.getId() : null;
    UserResponse response = onboardingAdminService.provisionAssessor(request, actorId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Assessor account provisioned successfully", response, httpRequest.getRequestURI()));
  }

  @Operation(summary = "Provision Government Admin", description = "Provision a Government Official or Analyst account")
  @PostMapping("/provision-government-admin")
  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin')")
  public ResponseEntity<ApiResponse<UserResponse>> provisionGovernmentAdmin(
      @Valid @RequestBody ProvisionGovernmentAdminRequest request,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest httpRequest) {
    Long actorId = principal != null ? principal.getId() : null;
    UserResponse response = onboardingAdminService.provisionGovernmentAdmin(request, actorId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Government administrative account provisioned successfully", response, httpRequest.getRequestURI()));
  }

  @Operation(summary = "Provision Super Admin", description = "Provision a new Super Administrator account (strictly Super Admin only)")
  @PostMapping("/provision-super-admin")
  @PreAuthorize("hasAuthority('ROLE_super_admin')")
  public ResponseEntity<ApiResponse<UserResponse>> provisionSuperAdmin(
      @Valid @RequestBody ProvisionSuperAdminRequest request,
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest httpRequest) {
    Long actorId = principal != null ? principal.getId() : null;
    UserResponse response = onboardingAdminService.provisionSuperAdmin(request, actorId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Super Administrator account provisioned successfully", response, httpRequest.getRequestURI()));
  }
}
