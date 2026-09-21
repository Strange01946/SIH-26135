package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import java.util.List;

public interface TraineeConsentService {

  TraineeConsentResponse getById(Long id);

  List<TraineeConsentResponse> getConsentsByTraineeId(Long traineeId);

  List<TraineeConsentResponse> getConsentsByTraineeAndType(Long traineeId, Long consentTypeId);

  List<TraineeConsentResponse> getConsentsByTraineeAndStatus(Long traineeId, Long consentStatusId);

  TraineeConsentResponse createConsent(CreateTraineeConsentRequest request, Long actorUserId);

  TraineeConsentResponse updateConsentStatus(Long consentId, UpdateTraineeConsentStatusRequest request, Long actorUserId);
}
