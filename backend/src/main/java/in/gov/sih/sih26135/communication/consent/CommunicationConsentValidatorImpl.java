package in.gov.sih.sih26135.communication.consent;

import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative implementation of {@link CommunicationConsentValidator}.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Strict statutory channel isolation: SMS requires COMMUNICATION_SMS, Email requires COMMUNICATION_EMAIL,
 *       WhatsApp requires COMMUNICATION_WHATSAPP. No cross-channel substitution permitted.</li>
 *   <li>DATA_PROCESSING alone never authorizes outbound communications.</li>
 *   <li>Purpose-specific consent: When communication purpose requires explicit consent (e.g. EMPLOYMENT_FOLLOW_UP),
 *       both channel consent and purpose consent are verified.</li>
 *   <li>HTTP 403 error contract with machine-readable codes {@code CONSENT_WITHDRAWN} and {@code CONSENT_NOT_GRANTED}.</li>
 *   <li>Multi-tenant and trainee scope validation via {@link WorkflowSecurityScopeValidator}.</li>
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class CommunicationConsentValidatorImpl implements CommunicationConsentValidator {

  private final TraineeRepository traineeRepository;
  private final TraineeConsentRepository traineeConsentRepository;
  private final RefConsentTypeRepository refConsentTypeRepository;
  private final RefCommunicationChannelRepository refCommunicationChannelRepository;
  private final RefCommunicationPurposeRepository refCommunicationPurposeRepository;
  private final WorkflowSecurityScopeValidator workflowSecurityScopeValidator;

  @Autowired
  public CommunicationConsentValidatorImpl(
      TraineeRepository traineeRepository,
      TraineeConsentRepository traineeConsentRepository,
      RefConsentTypeRepository refConsentTypeRepository,
      RefCommunicationChannelRepository refCommunicationChannelRepository,
      RefCommunicationPurposeRepository refCommunicationPurposeRepository,
      WorkflowSecurityScopeValidator workflowSecurityScopeValidator) {
    this.traineeRepository = traineeRepository;
    this.traineeConsentRepository = traineeConsentRepository;
    this.refConsentTypeRepository = refConsentTypeRepository;
    this.refCommunicationChannelRepository = refCommunicationChannelRepository;
    this.refCommunicationPurposeRepository = refCommunicationPurposeRepository;
    this.workflowSecurityScopeValidator = workflowSecurityScopeValidator;
  }

  @Override
  public void validateCommunicationConsent(Long traineeId, String channelCode, String purposeCode) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required for communication consent validation", "TRAINEE_ID_REQUIRED");
    }
    if (channelCode == null || channelCode.isBlank()) {
      throw new BadRequestException("Communication channel code is required", "CHANNEL_CODE_REQUIRED");
    }
    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "id");
    }

    // Tenant and security scope validation
    if (workflowSecurityScopeValidator != null) {
      workflowSecurityScopeValidator.validateTraineeScope(traineeId);
    }

    // 1. Mandatory Base Consent: DATA_PROCESSING (DPDP Act 2023 foundational requirement)
    validateConsentRecord(traineeId, "DATA_PROCESSING");

    // 2. Resolve Channel Mandatory Consent Type
    String requiredChannelConsentCode = resolveChannelConsentCode(channelCode.trim().toUpperCase());
    validateConsentRecord(traineeId, requiredChannelConsentCode);

    // 3. Resolve Purpose Mandatory Consent Type (if purpose defines one)
    if (purposeCode != null && !purposeCode.isBlank()) {
      String requiredPurposeConsentCode = resolvePurposeConsentCode(purposeCode.trim().toUpperCase());
      if (requiredPurposeConsentCode != null) {
        validateConsentRecord(traineeId, requiredPurposeConsentCode);
      }
    }
  }

  @Override
  public void validateConsentRecord(Long traineeId, String consentCode) {
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

    List<TraineeConsent> consents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(
        traineeId, consentType.getId()
    );

    if (consents == null || consents.isEmpty()) {
      throw new CommunicationConsentException(
          "Trainee has not granted communication consent for " + consentCode,
          "CONSENT_NOT_GRANTED"
      );
    }

    // Authoritative consent resolution: latest updated or created
    TraineeConsent authoritative = consents.stream()
        .max(Comparator.comparing((TraineeConsent c) ->
                c.getUpdatedAt() != null ? c.getUpdatedAt() : (c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.MIN))
            .thenComparing(TraineeConsent::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(consents.get(0));

    RefConsentStatus status = authoritative.getConsentStatus();
    String statusCode = status != null ? status.getStatusCode() : null;

    if (statusCode == null || !"GRANTED".equalsIgnoreCase(statusCode)) {
      if ("REVOKED".equalsIgnoreCase(statusCode)) {
        throw new CommunicationConsentException(
            "Trainee consent for " + consentCode + " has been revoked",
            "CONSENT_WITHDRAWN"
        );
      } else if ("EXPIRED".equalsIgnoreCase(statusCode)) {
        throw new CommunicationConsentException(
            "Trainee consent for " + consentCode + " has expired",
            "CONSENT_NOT_GRANTED"
        );
      } else {
        throw new CommunicationConsentException(
            "Trainee has not granted communication consent for " + consentCode,
            "CONSENT_NOT_GRANTED"
        );
      }
    }

    LocalDateTime now = LocalDateTime.now();

    if (authoritative.getRevokedAt() != null && !authoritative.getRevokedAt().isAfter(now)) {
      throw new CommunicationConsentException(
          "Trainee consent for " + consentCode + " has been revoked",
          "CONSENT_WITHDRAWN"
      );
    }

    if (authoritative.getExpiresAt() != null && !authoritative.getExpiresAt().isAfter(now)) {
      throw new CommunicationConsentException(
          "Trainee consent for " + consentCode + " has expired",
          "CONSENT_NOT_GRANTED"
      );
    }

    if (authoritative.getGrantedAt() != null && authoritative.getGrantedAt().isAfter(now)) {
      throw new CommunicationConsentException(
          "Trainee consent for " + consentCode + " is not yet effective",
          "CONSENT_NOT_GRANTED"
      );
    }
  }

  private String resolveChannelConsentCode(String channelCode) {
    return refCommunicationChannelRepository.findByChannelCode(channelCode)
        .map(RefCommunicationChannel::getRequiredConsentType)
        .map(RefConsentType::getConsentCode)
        .orElseGet(() -> switch (channelCode) {
          case "SMS" -> "COMMUNICATION_SMS";
          case "WHATSAPP" -> "COMMUNICATION_WHATSAPP";
          case "EMAIL" -> "COMMUNICATION_EMAIL";
          case "PHONE" -> "COMMUNICATION_PHONE";
          default -> throw new BadRequestException("Unsupported communication channel: " + channelCode, "UNSUPPORTED_CHANNEL");
        });
  }

  private String resolvePurposeConsentCode(String purposeCode) {
    return refCommunicationPurposeRepository.findByPurposeCode(purposeCode)
        .map(RefCommunicationPurpose::getRequiredConsentType)
        .map(RefConsentType::getConsentCode)
        .orElseGet(() -> switch (purposeCode) {
          case "FOLLOWUP_REMINDER", "EMPLOYMENT_CHECK", "WAGE_CHECK", "RETENTION_CHECK", "SURVEY_INVITE" -> "EMPLOYMENT_FOLLOW_UP";
          default -> null; // Operational, placement alert, verification notices do not mandate secondary purpose consent
        });
  }
}
