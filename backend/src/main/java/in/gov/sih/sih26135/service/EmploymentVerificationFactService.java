package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import java.util.List;

public interface EmploymentVerificationFactService {

  List<EmploymentVerificationFactResponse> getAllEmploymentVerifications();

  EmploymentVerificationFactResponse getEmploymentVerificationById(Long employmentVerificationId);

  EmploymentVerificationFactResponse getEmploymentVerificationByNumber(String verificationNumber);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByTraineeId(Long traineeId);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmploymentId(Long employmentId);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByPlacementId(Long placementId);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmployerId(Long employerId);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByStatusId(Long statusId);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByMethodId(Long methodId);

  List<EmploymentVerificationFactResponse> getCurrentEmploymentVerifications();

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByIsVerifiedFlag(Boolean isVerifiedFlag);
}
