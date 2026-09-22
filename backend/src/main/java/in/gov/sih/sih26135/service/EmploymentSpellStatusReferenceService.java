package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentSpellStatusResponse;
import java.util.List;

public interface EmploymentSpellStatusReferenceService {

  List<EmploymentSpellStatusResponse> getAllSpellStatuses();

  EmploymentSpellStatusResponse getById(Long id);

  EmploymentSpellStatusResponse getByCode(String statusCode);

  List<EmploymentSpellStatusResponse> getByIsActive(Boolean isActiveFlag);
}
