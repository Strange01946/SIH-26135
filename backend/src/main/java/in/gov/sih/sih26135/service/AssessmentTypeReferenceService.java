package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AssessmentTypeResponse;
import java.util.List;

public interface AssessmentTypeReferenceService {

  List<AssessmentTypeResponse> getAllAssessmentTypes();

  AssessmentTypeResponse getById(Long id);

  AssessmentTypeResponse getByCode(String typeCode);
}
