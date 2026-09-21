package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import org.springframework.stereotype.Component;

@Component
public class TraineeConsentMapper {

  public TraineeConsentResponse toResponse(TraineeConsent entity) {
    if (entity == null) {
      return null;
    }

    return new TraineeConsentResponse(
        entity.getId(),
        entity.getTrainee() != null ? entity.getTrainee().getId() : null,
        entity.getTrainee() != null ? entity.getTrainee().getRegistrationNumber() : null,
        entity.getConsentType() != null ? entity.getConsentType().getId() : null,
        entity.getConsentType() != null ? entity.getConsentType().getConsentCode() : null,
        entity.getConsentType() != null ? entity.getConsentType().getConsentName() : null,
        entity.getConsentStatus() != null ? entity.getConsentStatus().getId() : null,
        entity.getConsentStatus() != null ? entity.getConsentStatus().getStatusCode() : null,
        entity.getConsentStatus() != null ? entity.getConsentStatus().getStatusName() : null,
        entity.getPurpose(),
        entity.getPolicyVersion(),
        entity.getGrantedAt(),
        entity.getRevokedAt(),
        entity.getExpiresAt(),
        entity.getCapturedChannel(),
        entity.getCapturedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public TraineeConsent toEntity(
      CreateTraineeConsentRequest request,
      Trainee trainee,
      RefConsentType consentType,
      RefConsentStatus consentStatus,
      Long actorUserId) {
    if (request == null) {
      return null;
    }

    TraineeConsent consent = new TraineeConsent();
    consent.setTrainee(trainee);
    consent.setConsentType(consentType);
    consent.setConsentStatus(consentStatus);
    consent.setPurpose(request.getPurpose());
    consent.setPolicyVersion(request.getPolicyVersion());
    consent.setGrantedAt(request.getGrantedAt());
    consent.setExpiresAt(request.getExpiresAt());
    consent.setCapturedChannel(request.getCapturedChannel());
    consent.setCapturedByUserId(actorUserId);
    return consent;
  }
}
