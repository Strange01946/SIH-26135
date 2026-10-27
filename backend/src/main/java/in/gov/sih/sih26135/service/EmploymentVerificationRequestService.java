package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmploymentVerificationRequestService {

  EmploymentVerificationRequestResponse createVerificationRequest(CreateEmploymentVerificationRequestRequest request);

  EmploymentVerificationRequestResponse updateVerificationRequest(Long id, UpdateEmploymentVerificationRequestRequest request);

  EmploymentVerificationRequestResponse getVerificationRequestById(Long id);

  EmploymentVerificationRequestResponse getVerificationRequestByNumber(String requestNumber);

  List<EmploymentVerificationRequestResponse> getRequestsByEmploymentId(Long employmentId);

  Page<EmploymentVerificationRequestResponse> getRequestsByEmploymentId(Long employmentId, Pageable pageable);

  List<EmploymentVerificationRequestResponse> getRequestsByTraineeId(Long traineeId);

  Page<EmploymentVerificationRequestResponse> getRequestsByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentVerificationRequestResponse> getRequestsByStatusId(Long statusId);

  Page<EmploymentVerificationRequestResponse> getRequestsByStatusId(Long statusId, Pageable pageable);

  List<EmploymentVerificationRequestResponse> getRequestsByAssignedVerifierId(Long assignedVerifierUserId);

  Page<EmploymentVerificationRequestResponse> getRequestsByAssignedVerifierId(Long assignedVerifierUserId, Pageable pageable);

  void deleteVerificationRequest(Long id);
}
