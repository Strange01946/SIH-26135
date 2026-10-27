package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmploymentVerificationAttemptService {

  EmploymentVerificationAttemptResponse createVerificationAttempt(CreateEmploymentVerificationAttemptRequest request);

  EmploymentVerificationAttemptResponse updateVerificationAttempt(Long id, UpdateEmploymentVerificationAttemptRequest request);

  EmploymentVerificationAttemptResponse getVerificationAttemptById(Long id);

  List<EmploymentVerificationAttemptResponse> getAttemptsByRequestId(Long requestId);

  Page<EmploymentVerificationAttemptResponse> getAttemptsByRequestId(Long requestId, Pageable pageable);

  List<EmploymentVerificationAttemptResponse> getAttemptsByMethodId(Long methodId);

  Page<EmploymentVerificationAttemptResponse> getAttemptsByMethodId(Long methodId, Pageable pageable);

  List<EmploymentVerificationAttemptResponse> getAttemptsByStatusId(Long statusId);

  Page<EmploymentVerificationAttemptResponse> getAttemptsByStatusId(Long statusId, Pageable pageable);

  void deleteVerificationAttempt(Long id);
}
