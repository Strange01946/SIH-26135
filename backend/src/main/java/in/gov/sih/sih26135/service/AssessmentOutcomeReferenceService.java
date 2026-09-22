package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AssessmentOutcomeResponse;
import java.util.List;

public interface AssessmentOutcomeReferenceService {

  List<AssessmentOutcomeResponse> getAllAssessmentOutcomes();

  AssessmentOutcomeResponse getById(Long id);

  AssessmentOutcomeResponse getByCode(String outcomeCode);

  List<AssessmentOutcomeResponse> getPassingOutcomes();
}
