package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ApplicationStatusResponse;
import java.util.List;

public interface ApplicationStatusReferenceService {

  List<ApplicationStatusResponse> getAllApplicationStatuses();

  ApplicationStatusResponse getById(Long id);

  ApplicationStatusResponse getByCode(String statusCode);
}
