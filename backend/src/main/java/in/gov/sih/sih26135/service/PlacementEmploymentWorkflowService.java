package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;

public interface PlacementEmploymentWorkflowService {

  PlacementToEmploymentTransitionResponse transitionPlacementToEmployment(
      PlacementToEmploymentTransitionRequest request);

  TraineePlacementEmploymentSummaryResponse getTraineePlacementEmploymentSummary(
      Long traineeId);
}
