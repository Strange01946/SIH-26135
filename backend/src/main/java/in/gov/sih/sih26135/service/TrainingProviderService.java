package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.TrainingProviderResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingProviderService {

  TrainingProviderResponse getById(Long id);

  TrainingProviderResponse getByCode(String providerCode);

  TrainingProviderResponse getByRegistrationNumber(String registrationNumber);

  List<TrainingProviderResponse> getAllTrainingProviders();

  List<TrainingProviderResponse> getTrainingProvidersByStateId(Long stateId);

  List<TrainingProviderResponse> getTrainingProvidersByDistrictId(Long districtId);

  Page<TrainingProviderResponse> getAllTrainingProviders(Pageable pageable);

  Page<TrainingProviderResponse> getTrainingProvidersByStateId(Long stateId, Pageable pageable);

  Page<TrainingProviderResponse> getTrainingProvidersByDistrictId(Long districtId, Pageable pageable);

  TrainingProviderResponse createTrainingProvider(CreateTrainingProviderRequest request);

  TrainingProviderResponse updateTrainingProvider(Long id, UpdateTrainingProviderRequest request);

  void deleteTrainingProvider(Long id);
}
