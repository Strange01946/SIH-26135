package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AccreditationStatusResponse;
import java.util.List;

public interface AccreditationReferenceService {

  List<AccreditationStatusResponse> getAllAccreditationStatuses();

  AccreditationStatusResponse getById(Long id);

  AccreditationStatusResponse getByCode(String statusCode);
}
