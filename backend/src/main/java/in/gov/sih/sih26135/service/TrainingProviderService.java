package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.TrainingProviderResponse;
import java.util.List;

public interface TrainingProviderService {

  TrainingProviderResponse getById(Long id);

  TrainingProviderResponse getByCode(String providerCode);

  TrainingProviderResponse getByRegistrationNumber(String registrationNumber);

  List<TrainingProviderResponse> getAllTrainingProviders();

  List<TrainingProviderResponse> getTrainingProvidersByStateId(Long stateId);

  List<TrainingProviderResponse> getTrainingProvidersByDistrictId(Long districtId);

  TrainingProviderResponse createTrainingProvider(CreateTrainingProviderRequest request);

  TrainingProviderResponse updateTrainingProvider(Long id, UpdateTrainingProviderRequest request);

  void deleteTrainingProvider(Long id);
}
