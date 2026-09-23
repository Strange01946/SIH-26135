package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptStatusResponse;
import java.util.List;

public interface EmploymentVerificationAttemptStatusReferenceService {

  List<EmploymentVerificationAttemptStatusResponse> getAllAttemptStatuses();

  EmploymentVerificationAttemptStatusResponse getById(Long id);

  EmploymentVerificationAttemptStatusResponse getByCode(String statusCode);
}
