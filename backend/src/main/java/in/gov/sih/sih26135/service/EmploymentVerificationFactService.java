package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EmploymentVerificationFactService {

  PagedResponse<EmploymentVerificationFactResponse> getAllEmploymentVerifications(Pageable pageable);

  List<EmploymentVerificationFactResponse> getAllEmploymentVerifications();

  EmploymentVerificationFactResponse getEmploymentVerificationById(Long employmentVerificationId);

  EmploymentVerificationFactResponse getEmploymentVerificationByNumber(String verificationNumber);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByTraineeId(Long traineeId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByTraineeId(
      Long traineeId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmploymentId(Long employmentId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmploymentId(
      Long employmentId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByPlacementId(Long placementId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByPlacementId(
      Long placementId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmployerId(Long employerId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmployerId(
      Long employerId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByStatusId(Long statusId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByStatusId(
      Long statusId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByMethodId(Long methodId);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByMethodId(
      Long methodId, Pageable pageable);

  List<EmploymentVerificationFactResponse> getCurrentEmploymentVerifications();

  PagedResponse<EmploymentVerificationFactResponse> getCurrentEmploymentVerifications(Pageable pageable);

  List<EmploymentVerificationFactResponse> getEmploymentVerificationsByIsVerifiedFlag(Boolean isVerifiedFlag);

  PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByIsVerifiedFlag(
      Boolean isVerifiedFlag, Pageable pageable);
}
