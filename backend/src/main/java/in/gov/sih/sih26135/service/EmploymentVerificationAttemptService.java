package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import java.util.List;

public interface EmploymentVerificationAttemptService {

  EmploymentVerificationAttemptResponse createVerificationAttempt(CreateEmploymentVerificationAttemptRequest request);

  EmploymentVerificationAttemptResponse updateVerificationAttempt(Long id, UpdateEmploymentVerificationAttemptRequest request);

  EmploymentVerificationAttemptResponse getVerificationAttemptById(Long id);

  List<EmploymentVerificationAttemptResponse> getAttemptsByRequestId(Long requestId);

  List<EmploymentVerificationAttemptResponse> getAttemptsByMethodId(Long methodId);

  List<EmploymentVerificationAttemptResponse> getAttemptsByStatusId(Long statusId);

  void deleteVerificationAttempt(Long id);
}
