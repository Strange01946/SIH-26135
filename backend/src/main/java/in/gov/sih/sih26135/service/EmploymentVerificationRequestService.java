package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import java.util.List;

public interface EmploymentVerificationRequestService {

  EmploymentVerificationRequestResponse createVerificationRequest(CreateEmploymentVerificationRequestRequest request);

  EmploymentVerificationRequestResponse updateVerificationRequest(Long id, UpdateEmploymentVerificationRequestRequest request);

  EmploymentVerificationRequestResponse getVerificationRequestById(Long id);

  EmploymentVerificationRequestResponse getVerificationRequestByNumber(String requestNumber);

  List<EmploymentVerificationRequestResponse> getRequestsByEmploymentId(Long employmentId);

  List<EmploymentVerificationRequestResponse> getRequestsByTraineeId(Long traineeId);

  List<EmploymentVerificationRequestResponse> getRequestsByStatusId(Long statusId);

  List<EmploymentVerificationRequestResponse> getRequestsByAssignedVerifierId(Long assignedVerifierUserId);

  void deleteVerificationRequest(Long id);
}
