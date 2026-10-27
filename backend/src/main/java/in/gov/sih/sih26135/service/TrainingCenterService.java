package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingCenterService {

  TrainingCenterResponse getById(Long id);

  TrainingCenterResponse getByCode(String centerCode);

  List<TrainingCenterResponse> getAllTrainingCenters();

  List<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId);

  List<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId);

  List<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId);

  Page<TrainingCenterResponse> getAllTrainingCenters(Pageable pageable);

  Page<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId, Pageable pageable);

  Page<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId, Pageable pageable);

  Page<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId, Pageable pageable);

  TrainingCenterResponse createTrainingCenter(CreateTrainingCenterRequest request);

  TrainingCenterResponse updateTrainingCenter(Long id, UpdateTrainingCenterRequest request);

  void deleteTrainingCenter(Long id);
}
