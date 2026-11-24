package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.ProvisionAssessorRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionGovernmentAdminRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionSuperAdminRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OnboardingAdminService {

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_government_officer')")
  List<OnboardingApplicationResponse> getApplications(String status);

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_government_officer')")
  OnboardingApplicationResponse getApplicationById(Long id);

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin')")
  OnboardingApplicationResponse approveApplication(Long id, Long reviewerUserId);

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin')")
  OnboardingApplicationResponse rejectApplication(Long id, String reason, Long reviewerUserId);

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin', 'ROLE_government_admin', 'ROLE_government_officer')")
  UserResponse provisionAssessor(ProvisionAssessorRequest request, Long actorUserId);

  @PreAuthorize("hasAnyAuthority('user.manage', 'ROLE_super_admin')")
  UserResponse provisionGovernmentAdmin(ProvisionGovernmentAdminRequest request, Long actorUserId);

  @PreAuthorize("hasAuthority('ROLE_super_admin')")
  UserResponse provisionSuperAdmin(ProvisionSuperAdminRequest request, Long actorUserId);
}
