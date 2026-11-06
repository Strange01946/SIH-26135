package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface UnemploymentFactService {

  PagedResponse<UnemploymentFactResponse> getAllUnemploymentFacts(Pageable pageable);

  List<UnemploymentFactResponse> getAllUnemploymentFacts();

  UnemploymentFactResponse getUnemploymentFactById(Long unemploymentEventId);

  List<UnemploymentFactResponse> getUnemploymentFactsByTraineeId(Long traineeId);

  PagedResponse<UnemploymentFactResponse> getUnemploymentFactsByTraineeId(Long traineeId, Pageable pageable);

  List<UnemploymentFactResponse> getUnemploymentFactsByUnemploymentReasonId(Long unemploymentReasonId);

  PagedResponse<UnemploymentFactResponse> getUnemploymentFactsByUnemploymentReasonId(
      Long unemploymentReasonId, Pageable pageable);

  List<UnemploymentFactResponse> getUnemploymentFactsByLabourStatusId(Long labourStatusId);

  PagedResponse<UnemploymentFactResponse> getUnemploymentFactsByLabourStatusId(Long labourStatusId, Pageable pageable);

  List<UnemploymentFactResponse> getCurrentUnemploymentFacts();

  PagedResponse<UnemploymentFactResponse> getCurrentUnemploymentFacts(Pageable pageable);

  List<UnemploymentFactResponse> getUnemploymentFactsByTraineeDistrictId(Long traineeDistrictId);

  PagedResponse<UnemploymentFactResponse> getUnemploymentFactsByTraineeDistrictId(
      Long traineeDistrictId, Pageable pageable);
}
