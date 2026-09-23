package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestStatusResponse;
import java.util.List;

public interface EmploymentVerificationRequestStatusReferenceService {

  List<EmploymentVerificationRequestStatusResponse> getAllStatuses();

  EmploymentVerificationRequestStatusResponse getById(Long id);

  EmploymentVerificationRequestStatusResponse getByCode(String statusCode);
}
