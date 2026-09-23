package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import java.util.List;

public interface EmploymentVerificationEvidenceService {

  EmploymentVerificationEvidenceResponse createVerificationEvidence(CreateEmploymentVerificationEvidenceRequest request);

  EmploymentVerificationEvidenceResponse updateVerificationEvidence(Long id, UpdateEmploymentVerificationEvidenceRequest request);

  EmploymentVerificationEvidenceResponse getVerificationEvidenceById(Long id);

  List<EmploymentVerificationEvidenceResponse> getEvidenceByVerificationId(Long verificationId);

  List<EmploymentVerificationEvidenceResponse> getEvidenceByAttemptId(Long attemptId);

  List<EmploymentVerificationEvidenceResponse> getEvidenceByTypeId(Long typeId);

  void deleteVerificationEvidence(Long id);
}
