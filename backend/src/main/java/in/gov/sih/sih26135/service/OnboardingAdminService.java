package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.ProvisionAssessorRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionGovernmentAdminRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionSuperAdminRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import java.util.List;

public interface OnboardingAdminService {

  List<OnboardingApplicationResponse> getApplications(String status);

  OnboardingApplicationResponse getApplicationById(Long id);

  OnboardingApplicationResponse approveApplication(Long id, Long reviewerUserId);

  OnboardingApplicationResponse rejectApplication(Long id, String reason, Long reviewerUserId);

  UserResponse provisionAssessor(ProvisionAssessorRequest request, Long actorUserId);

  UserResponse provisionGovernmentAdmin(ProvisionGovernmentAdminRequest request, Long actorUserId);

  UserResponse provisionSuperAdmin(ProvisionSuperAdminRequest request, Long actorUserId);
}
