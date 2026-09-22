package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentExitReasonResponse;
import java.util.List;

public interface EmploymentExitReasonReferenceService {

  List<EmploymentExitReasonResponse> getAllExitReasons();

  EmploymentExitReasonResponse getById(Long id);

  EmploymentExitReasonResponse getByCode(String reasonCode);
}
