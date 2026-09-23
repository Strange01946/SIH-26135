package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import java.util.List;

public interface EmploymentVerificationService {

  EmploymentVerificationResponse createEmploymentVerification(CreateEmploymentVerificationRequest request);

  EmploymentVerificationResponse updateEmploymentVerification(Long id, UpdateEmploymentVerificationRequest request);

  EmploymentVerificationResponse getEmploymentVerificationById(Long id);

  EmploymentVerificationResponse getEmploymentVerificationByNumber(String verificationNumber);

  EmploymentVerificationResponse getCurrentVerificationByEmploymentId(Long employmentId);

  List<EmploymentVerificationResponse> getVerificationsByEmploymentId(Long employmentId);

  List<EmploymentVerificationResponse> getVerificationsByTraineeId(Long traineeId);

  List<EmploymentVerificationResponse> getVerificationsByStatusId(Long statusId);

  void deleteEmploymentVerification(Long id);
}
