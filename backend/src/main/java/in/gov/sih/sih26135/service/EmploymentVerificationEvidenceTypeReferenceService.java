package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceTypeResponse;
import java.util.List;

public interface EmploymentVerificationEvidenceTypeReferenceService {

  List<EmploymentVerificationEvidenceTypeResponse> getAllEvidenceTypes();

  EmploymentVerificationEvidenceTypeResponse getById(Long id);

  EmploymentVerificationEvidenceTypeResponse getByCode(String typeCode);
}
