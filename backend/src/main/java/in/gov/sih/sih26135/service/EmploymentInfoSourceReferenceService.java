package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentInfoSourceResponse;
import java.util.List;

public interface EmploymentInfoSourceReferenceService {

  List<EmploymentInfoSourceResponse> getAllInfoSources();

  EmploymentInfoSourceResponse getById(Long id);

  EmploymentInfoSourceResponse getByCode(String sourceCode);

  List<EmploymentInfoSourceResponse> getByIsSelfReported(Boolean isSelfReportedFlag);
}
