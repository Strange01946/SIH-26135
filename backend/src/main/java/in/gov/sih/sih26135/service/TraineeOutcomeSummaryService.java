package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import java.util.List;

public interface TraineeOutcomeSummaryService {

  List<TraineeOutcomeSummaryResponse> getAllTraineeOutcomes();

  TraineeOutcomeSummaryResponse getTraineeOutcomeByTraineeId(Long traineeId);

  List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByStateId(Long stateId);

  List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByDistrictId(Long districtId);

  List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByCurrentEmploymentStatusId(Long currentEmploymentStatusId);

  List<TraineeOutcomeSummaryResponse> getTraineeOutcomesBySnapshotIsEmployedFlag(Boolean snapshotIsEmployedFlag);
}
