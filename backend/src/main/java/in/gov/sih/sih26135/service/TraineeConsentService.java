package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.entity.Trainee;
import java.time.LocalDateTime;
import java.util.List;

public interface TraineeConsentService {

  TraineeConsentResponse getById(Long id);

  List<TraineeConsentResponse> getConsentsByTraineeId(Long traineeId);

  List<TraineeConsentResponse> getConsentsByTraineeAndType(Long traineeId, Long consentTypeId);

  List<TraineeConsentResponse> getConsentsByTraineeAndStatus(Long traineeId, Long consentStatusId);

  TraineeConsentResponse createConsent(CreateTraineeConsentRequest request, Long actorUserId);

  /** Records the explicit core grant for the trainee and owner created in the registration transaction. */
  void recordRegistrationConsent(Trainee trainee, Long ownerUserId, LocalDateTime grantedAt);

  TraineeConsentResponse updateConsentStatus(Long consentId, UpdateTraineeConsentStatusRequest request, Long actorUserId);

  boolean hasActiveConsent(Long traineeId, String consentCode);

  void validateConsent(Long traineeId, String consentCode);

  void validateConsent(Long traineeId, Long consentTypeId);
}
