package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTraineeConsentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeConsentStatusRequest;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeConsentMapper;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.TraineeConsentService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired(required = false)
  private WorkflowSecurityScopeValidator securityScopeValidator;

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

  @Autowired
  public TraineeConsentServiceImpl(
      TraineeConsentRepository traineeConsentRepository,
      TraineeRepository traineeRepository,
      RefConsentTypeRepository refConsentTypeRepository,
      RefConsentStatusRepository refConsentStatusRepository,
      TraineeConsentMapper traineeConsentMapper,
      @Autowired(required = false) WorkflowSecurityScopeValidator securityScopeValidator) {
    this(traineeConsentRepository, traineeRepository, refConsentTypeRepository, refConsentStatusRepository, traineeConsentMapper);
    this.securityScopeValidator = securityScopeValidator;
  }

  public void setSecurityScopeValidator(WorkflowSecurityScopeValidator securityScopeValidator) {
    this.securityScopeValidator = securityScopeValidator;
  }

  @Override
  public TraineeConsentResponse getById(Long id) {
    TraineeConsent consent = traineeConsentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id"));
    if (securityScopeValidator != null && consent.getTrainee() != null) {
      securityScopeValidator.validateTraineeScope(consent.getTrainee().getId());
    }
    return traineeConsentMapper.toResponse(consent);
  }

  @Override
  public List<TraineeConsentResponse> getConsentsByTraineeId(Long traineeId) {
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(traineeId);
    }
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }
    return traineeConsentRepository.findByTraineeId(traineeId).stream()
        .map(traineeConsentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeConsentResponse> getConsentsByTraineeAndType(Long traineeId, Long consentTypeId) {
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(traineeId);
    }
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
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(traineeId);
    }
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
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeConsentResponse createConsent(CreateTraineeConsentRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Consent creation request cannot be null");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(request.getTraineeId());
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

    boolean selfService = securityScopeValidator != null && securityScopeValidator.isTraineeCaller();
    Trainee trainee = selfService ? lockOwnTrainee(request.getTraineeId())
        : traineeRepository.findById(request.getTraineeId())
            .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    RefConsentType consentType = refConsentTypeRepository.findById(request.getConsentTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "id"));
    RefConsentStatus consentStatus = refConsentStatusRepository.findById(request.getConsentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "id"));

    if (selfService) {
      if (!"GRANTED".equals(consentStatus.getStatusCode())) {
        throw new BadRequestException("Use the withdrawal action to withdraw consent.", "INVALID_CONSENT_STATUS");
      }
      TraineeConsent latest = latestConsent(trainee.getId(), consentType.getId());
      if (isActive(latest, LocalDateTime.now())) {
        return traineeConsentMapper.toResponse(latest);
      }
      request.setGrantedAt(null);
      request.setExpiresAt(null);
      request.setPurpose(consentType.getPurpose());
      request.setPolicyVersion("v1.0");
      request.setCapturedChannel("WEB_PORTAL");
      actorUserId = trainee.getUser().getId();
    }
    return persistConsent(request, trainee, consentType, consentStatus, actorUserId);
  }

  private Trainee lockOwnTrainee(Long traineeId) {
    Long actor = securityScopeValidator.resolveAuthenticatedUserId();
    return traineeRepository.findLockedById(traineeId)
        .filter(trainee -> actor != null && trainee.getUser() != null && actor.equals(trainee.getUser().getId()))
        .orElseThrow(() -> new AccessDeniedException("You can manage consent for your own profile only."));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordRegistrationConsent(Trainee trainee, Long ownerUserId, LocalDateTime grantedAt) {
    if (trainee == null || trainee.getId() == null || trainee.getUser() == null
        || ownerUserId == null || !ownerUserId.equals(trainee.getUser().getId())) {
      throw new IllegalArgumentException("Registration consent must be tied to the newly created trainee account");
    }
    RefConsentType type = refConsentTypeRepository.findByConsentCode("DATA_PROCESSING")
        .orElseThrow(() -> new IllegalStateException("DATA_PROCESSING consent type is not configured"));
    RefConsentStatus status = refConsentStatusRepository.findByStatusCode("GRANTED")
        .orElseThrow(() -> new IllegalStateException("GRANTED consent status is not configured"));
    var request = new CreateTraineeConsentRequest();
    request.setTraineeId(trainee.getId());
    request.setConsentTypeId(type.getId());
    request.setConsentStatusId(status.getId());
    request.setPurpose("Process registration and training records");
    request.setPolicyVersion("v1.0");
    request.setCapturedChannel("WEB_PORTAL");
    request.setGrantedAt(grantedAt);
    persistConsent(request, trainee, type, status, ownerUserId);
  }

  private TraineeConsentResponse persistConsent(CreateTraineeConsentRequest request, Trainee trainee,
      RefConsentType consentType, RefConsentStatus consentStatus, Long actorUserId) {
    // Consent timestamps use DATETIME without fractional seconds in MySQL.
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    request.setGrantedAt(request.getGrantedAt() == null ? now : request.getGrantedAt().truncatedTo(ChronoUnit.SECONDS));

    TraineeConsent consent = traineeConsentMapper.toEntity(request, trainee, consentType, consentStatus, actorUserId);
    consent.setCreatedAt(now);
    consent.setUpdatedAt(now);

    TraineeConsent saved = traineeConsentRepository.save(consent);
    return traineeConsentMapper.toResponse(saved);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
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

    boolean selfService = securityScopeValidator != null && securityScopeValidator.isTraineeCaller();
    if (selfService) {
      Long ownerId = traineeConsentRepository.findOwnerTraineeId(consentId)
          .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id"));
      lockOwnTrainee(ownerId);
    }
    TraineeConsent consent = traineeConsentRepository.findById(consentId)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeConsent", "id"));
    if (!selfService && securityScopeValidator != null && consent.getTrainee() != null) {
      securityScopeValidator.validateTraineeScope(consent.getTrainee().getId());
    }
    RefConsentStatus status = refConsentStatusRepository.findById(request.getConsentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "id"));

    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    LocalDateTime revokedAt = request.getRevokedAt();
    if (selfService) {
      if (!"REVOKED".equals(status.getStatusCode())) {
        throw new BadRequestException("Use the grant action to grant consent.", "INVALID_CONSENT_STATUS");
      }
      TraineeConsent latest = latestConsent(consent.getTrainee().getId(), consent.getConsentType().getId());
      if (latest == null || !consentId.equals(latest.getId())) {
        throw new ConflictException("This consent changed. Review the current choice and try again.", "CONSENT_CHANGED");
      }
      if ("REVOKED".equals(consent.getConsentStatus().getStatusCode())) {
        return traineeConsentMapper.toResponse(consent);
      }
      revokedAt = consent.getGrantedAt() != null && consent.getGrantedAt().isAfter(now) ? consent.getGrantedAt() : now;
    }
    consent.setConsentStatus(status);

    if (revokedAt != null) {
      if (consent.getGrantedAt() != null && revokedAt.isBefore(consent.getGrantedAt())) {
        throw new BadRequestException(
            "Revocation timestamp cannot precede grant timestamp",
            "INVALID_REVOCATION_DATE"
        );
      }
      consent.setRevokedAt(revokedAt.truncatedTo(ChronoUnit.SECONDS));
    }

    consent.setUpdatedAt(now);
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
    return isActive(latestConsent(traineeId, consentType.getId()), LocalDateTime.now());
  }

  private TraineeConsent latestConsent(Long traineeId, Long consentTypeId) {
    List<TraineeConsent> consents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentTypeId);
    return consents.stream()
        .max(Comparator.comparing((TraineeConsent c) -> c.getUpdatedAt() != null ? c.getUpdatedAt() : (c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.MIN))
            .thenComparing(TraineeConsent::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
  }

  private static boolean isActive(TraineeConsent consent, LocalDateTime now) {
    return consent != null && consent.getConsentStatus() != null
        && "GRANTED".equalsIgnoreCase(consent.getConsentStatus().getStatusCode())
        && (consent.getRevokedAt() == null || consent.getRevokedAt().isAfter(now))
        && (consent.getExpiresAt() == null || consent.getExpiresAt().isAfter(now))
        && (consent.getGrantedAt() == null || !consent.getGrantedAt().isAfter(now));
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
    TraineeConsent authoritative = latestConsent(traineeId, consentTypeId);
    if (authoritative == null) {
      throw new BadRequestException(
          "Trainee has not granted consent for " + consentCode,
          "CONSENT_REQUIRED"
      );
    }

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
