package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import java.util.List;

public interface UnemploymentFactService {

  List<UnemploymentFactResponse> getAllUnemploymentFacts();

  UnemploymentFactResponse getUnemploymentFactById(Long unemploymentEventId);

  List<UnemploymentFactResponse> getUnemploymentFactsByTraineeId(Long traineeId);

  List<UnemploymentFactResponse> getUnemploymentFactsByUnemploymentReasonId(Long unemploymentReasonId);

  List<UnemploymentFactResponse> getUnemploymentFactsByLabourStatusId(Long labourStatusId);

  List<UnemploymentFactResponse> getCurrentUnemploymentFacts();

  List<UnemploymentFactResponse> getUnemploymentFactsByTraineeDistrictId(Long traineeDistrictId);
}
