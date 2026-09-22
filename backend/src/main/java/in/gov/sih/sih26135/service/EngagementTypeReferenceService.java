package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EngagementTypeResponse;
import java.util.List;

public interface EngagementTypeReferenceService {

  List<EngagementTypeResponse> getAllEngagementTypes();

  EngagementTypeResponse getById(Long id);

  EngagementTypeResponse getByCode(String typeCode);

  List<EngagementTypeResponse> getByRequiresEmployer(Boolean requiresEmployer);
}
