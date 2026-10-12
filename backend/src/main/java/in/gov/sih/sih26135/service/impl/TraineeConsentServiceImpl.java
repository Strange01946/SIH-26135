package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeConsentMapper;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.service.TraineeConsentService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeConsentServiceImpl implements TraineeConsentService {

  private final TraineeConsentRepository traineeConsentRepository;
  private final TraineeRepository traineeRepository;
  private final RefConsentTypeRepository refConsentTypeRepository;
  private final RefConsentStatusRepository refConsentStatusRepository;
  private final TraineeConsentMapper traineeConsentMapper;

  public TraineeConsentServiceImpl(
      TraineeConsentRepository traineeConsentRepository,
      TraineeRepository traineeRepository,
      RefConsentTypeRepository refConsentTypeRepository,
      RefConsentStatusRepository refConsentStatusRepository,
      TraineeConsentMapper traineeConsentMapper) {
    this.traineeConsentRepository = traineeConsentRepository;
    this.traineeRepository = traineeRepository;
    this.refConsentTypeRepository = refConsentTypeRepository;
    this.refConsentStatusRepository = refConsentStatusRepository;
    this.traineeConsentMapper = traineeConsentMapper;
  }

  @Override
  public TraineeConsentResponse getById(Long id) {
    TraineeConsent consent = traineeConsentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id"));
    return traineeConsentMapper.toResponse(consent);
  }

  @Override
  public List<TraineeConsentResponse> getConsentsByTraineeId(Long traineeId) {
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    return traineeConsentRepository.findByTraineeId(traineeId).stream()
        .map(traineeConsentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeConsentResponse> getConsentsByTraineeAndType(Long traineeId, Long consentTypeId) {
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    if (!refConsentTypeRepository.existsById(consentTypeId)) {
      throw new ResourceNotFoundException("ConsentType", "id");
    }
    return traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentTypeId).stream()
        .map(traineeConsentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeConsentResponse> getConsentsByTraineeAndStatus(Long traineeId, Long consentStatusId) {
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    if (!refConsentStatusRepository.existsById(consentStatusId)) {
      throw new ResourceNotFoundException("ConsentStatus", "id");
    }
    return traineeConsentRepository.findByTraineeIdAndConsentStatusId(traineeId, consentStatusId).stream()
        .map(traineeConsentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TraineeConsentResponse createConsent(CreateTraineeConsentRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Consent creation request cannot be null");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getConsentTypeId() == null) {
      throw new BadRequestException("Consent type ID is required");
    }
    if (request.getConsentStatusId() == null) {
      throw new BadRequestException("Consent status ID is required");
    }
    if (request.getPurpose() == null || request.getPurpose().isBlank()) {
      throw new BadRequestException("Consent purpose is required");
    }
    if (request.getPolicyVersion() == null || request.getPolicyVersion().isBlank()) {
      throw new BadRequestException("Policy version is required");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    RefConsentType consentType = refConsentTypeRepository.findById(request.getConsentTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "id"));
    RefConsentStatus consentStatus = refConsentStatusRepository.findById(request.getConsentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "id"));

    LocalDateTime grantedAt = request.getGrantedAt() != null ? request.getGrantedAt() : LocalDateTime.now();
    request.setGrantedAt(grantedAt);

    TraineeConsent consent = traineeConsentMapper.toEntity(request, trainee, consentType, consentStatus, actorUserId);
    LocalDateTime now = LocalDateTime.now();
    consent.setCreatedAt(now);
    consent.setUpdatedAt(now);

    TraineeConsent saved = traineeConsentRepository.save(consent);
    return traineeConsentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TraineeConsentResponse updateConsentStatus(
      Long consentId,
      UpdateTraineeConsentStatusRequest request,
      Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Consent status update request cannot be null");
    }
    if (request.getConsentStatusId() == null) {
      throw new BadRequestException("Consent status ID is required");
    }

    TraineeConsent consent = traineeConsentRepository.findById(consentId)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id"));
    RefConsentStatus status = refConsentStatusRepository.findById(request.getConsentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "id"));

    consent.setConsentStatus(status);

    if (request.getRevokedAt() != null) {
      if (consent.getGrantedAt() != null && request.getRevokedAt().isBefore(consent.getGrantedAt())) {
        throw new BadRequestException(
            "Revocation timestamp cannot precede grant timestamp",
            "INVALID_REVOCATION_DATE"
        );
      }
      consent.setRevokedAt(request.getRevokedAt());
    }

    consent.setUpdatedAt(LocalDateTime.now());
    TraineeConsent saved = traineeConsentRepository.save(consent);
    return traineeConsentMapper.toResponse(saved);
  }

  @Override
  public boolean hasActiveConsent(Long traineeId, String consentCode) {
    if (traineeId == null || consentCode == null || consentCode.isBlank()) {
      return false;
    }
    if (!traineeRepository.existsById(traineeId)) {
      return false;
    }
    RefConsentType consentType = refConsentTypeRepository.findByConsentCode(consentCode).orElse(null);
    if (consentType == null) {
      return false;
    }
    List<TraineeConsent> consents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentType.getId());
    if (consents == null || consents.isEmpty()) {
      return false;
    }
    TraineeConsent authoritative = consents.stream()
        .max(Comparator.comparing((TraineeConsent c) -> c.getUpdatedAt() != null ? c.getUpdatedAt() : (c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.MIN))
            .thenComparing(TraineeConsent::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(consents.get(0));

    RefConsentStatus status = authoritative.getConsentStatus();
    String statusCode = status != null ? status.getStatusCode() : null;
    if (!"GRANTED".equalsIgnoreCase(statusCode)) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    if (authoritative.getRevokedAt() != null && !authoritative.getRevokedAt().isAfter(now)) {
      return false;
    }
    if (authoritative.getExpiresAt() != null && !authoritative.getExpiresAt().isAfter(now)) {
      return false;
    }
    if (authoritative.getGrantedAt() != null && authoritative.getGrantedAt().isAfter(now)) {
      return false;
    }
    return true;
  }

  @Override
  public void validateConsent(Long traineeId, String consentCode) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required for consent validation", "TRAINEE_ID_REQUIRED");
    }
    if (consentCode == null || consentCode.isBlank()) {
      throw new BadRequestException("Consent code is required for consent validation", "CONSENT_CODE_REQUIRED");
    }
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    RefConsentType consentType = refConsentTypeRepository.findByConsentCode(consentCode)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "consentCode"));

    checkConsentValidityOrThrow(traineeId, consentType.getId(), consentCode);
  }

  @Override
  public void validateConsent(Long traineeId, Long consentTypeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required for consent validation", "TRAINEE_ID_REQUIRED");
    }
    if (consentTypeId == null) {
      throw new BadRequestException("Consent type ID is required for consent validation", "CONSENT_TYPE_ID_REQUIRED");
    }
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    RefConsentType consentType = refConsentTypeRepository.findById(consentTypeId)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "id"));

    checkConsentValidityOrThrow(traineeId, consentTypeId, consentType.getConsentCode());
  }

  private void checkConsentValidityOrThrow(Long traineeId, Long consentTypeId, String consentCode) {
    List<TraineeConsent> consents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentTypeId);
    if (consents == null || consents.isEmpty()) {
      throw new BadRequestException(
          "Trainee has not granted consent for " + consentCode,
          "CONSENT_REQUIRED"
      );
    }

    TraineeConsent authoritative = consents.stream()
        .max(Comparator.comparing((TraineeConsent c) -> c.getUpdatedAt() != null ? c.getUpdatedAt() : (c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.MIN))
            .thenComparing(TraineeConsent::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(consents.get(0));

    RefConsentStatus status = authoritative.getConsentStatus();
    String statusCode = status != null ? status.getStatusCode() : null;

    if (statusCode == null || !"GRANTED".equalsIgnoreCase(statusCode)) {
      if ("REVOKED".equalsIgnoreCase(statusCode)) {
        throw new BadRequestException(
            "Trainee consent for " + consentCode + " has been revoked",
            "CONSENT_REVOKED"
        );
      } else if ("EXPIRED".equalsIgnoreCase(statusCode)) {
        throw new BadRequestException(
            "Trainee consent for " + consentCode + " has expired",
            "CONSENT_EXPIRED"
        );
      } else {
        throw new BadRequestException(
            "Trainee has not granted consent for " + consentCode,
            "CONSENT_NOT_GRANTED"
        );
      }
    }

    LocalDateTime now = LocalDateTime.now();

    if (authoritative.getRevokedAt() != null && !authoritative.getRevokedAt().isAfter(now)) {
      throw new BadRequestException(
          "Trainee consent for " + consentCode + " has been revoked",
          "CONSENT_REVOKED"
      );
    }

    if (authoritative.getExpiresAt() != null && !authoritative.getExpiresAt().isAfter(now)) {
      throw new BadRequestException(
          "Trainee consent for " + consentCode + " has expired",
          "CONSENT_EXPIRED"
      );
    }

    if (authoritative.getGrantedAt() != null && authoritative.getGrantedAt().isAfter(now)) {
      throw new BadRequestException(
          "Trainee consent for " + consentCode + " is not yet effective",
          "CONSENT_NOT_YET_EFFECTIVE"
      );
    }
  }
}
