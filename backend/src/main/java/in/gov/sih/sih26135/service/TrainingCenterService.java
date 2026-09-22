package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import java.util.List;

public interface TrainingCenterService {

  TrainingCenterResponse getById(Long id);

  TrainingCenterResponse getByCode(String centerCode);

  List<TrainingCenterResponse> getAllTrainingCenters();

  List<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId);

  List<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId);

  List<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId);

  TrainingCenterResponse createTrainingCenter(CreateTrainingCenterRequest request);

  TrainingCenterResponse updateTrainingCenter(Long id, UpdateTrainingCenterRequest request);

  void deleteTrainingCenter(Long id);
}
