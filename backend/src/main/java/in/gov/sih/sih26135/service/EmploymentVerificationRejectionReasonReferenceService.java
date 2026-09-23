package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRejectionReasonResponse;
import java.util.List;

public interface EmploymentVerificationRejectionReasonReferenceService {

  List<EmploymentVerificationRejectionReasonResponse> getAllRejectionReasons();

  EmploymentVerificationRejectionReasonResponse getById(Long id);

  EmploymentVerificationRejectionReasonResponse getByCode(String reasonCode);
}
