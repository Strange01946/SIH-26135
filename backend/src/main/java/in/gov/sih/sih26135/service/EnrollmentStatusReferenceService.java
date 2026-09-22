package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EnrollmentStatusResponse;
import java.util.List;

public interface EnrollmentStatusReferenceService {

  List<EnrollmentStatusResponse> getAllEnrollmentStatuses();

  EnrollmentStatusResponse getById(Long id);

  EnrollmentStatusResponse getByCode(String statusCode);
}
