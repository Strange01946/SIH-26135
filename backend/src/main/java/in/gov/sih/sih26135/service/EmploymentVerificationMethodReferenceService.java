package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationMethodResponse;
import java.util.List;

public interface EmploymentVerificationMethodReferenceService {

  List<EmploymentVerificationMethodResponse> getAllMethods();

  EmploymentVerificationMethodResponse getById(Long id);

  EmploymentVerificationMethodResponse getByCode(String methodCode);
}
