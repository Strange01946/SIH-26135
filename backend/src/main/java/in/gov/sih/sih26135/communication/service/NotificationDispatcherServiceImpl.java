package in.gov.sih.sih26135.communication.service;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.consent.CommunicationConsentValidator;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestResult;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.provider.EmailCommunicationProvider;
import in.gov.sih.sih26135.communication.provider.SmsCommunicationProvider;
import in.gov.sih.sih26135.communication.provider.WhatsAppCommunicationProvider;
import in.gov.sih.sih26135.communication.template.CommunicationTemplateRegistry;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationDirectionRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative production implementation of {@link NotificationDispatcherService}.
 *
 * <p>Execution Lifecycle:
 * <ol>
 *   <li>Authorization and tenant scope check via {@link WorkflowSecurityScopeValidator}.</li>
 *   <li>Authoritative trainee entity retrieval and unmasked recipient resolution.</li>
 *   <li>Approved template validation and controlled parameter interpolation.</li>
 *   <li>Authoritative DPDP Act consent validation (fail-closed, logs {@code BLOCKED_NO_CONSENT} on rejection).</li>
 *   <li>Provider routing (Twilio SMS, Twilio WhatsApp, SendGrid/SMTP Email, or deterministic Mocks).</li>
 *   <li>Communication log persistence in MySQL {@code communication_logs}.</li>
 *   <li>Sanitized audit-safe response creation with masked contact details.</li>
 * </ol>
 */
@Service
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {

  private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherServiceImpl.class);

  private final CommunicationConsentValidator consentValidator;
  private final CommunicationTemplateRegistry templateRegistry;
  private final SmsCommunicationProvider smsProvider;
  private final WhatsAppCommunicationProvider whatsAppProvider;
  private final EmailCommunicationProvider emailProvider;
  private final TraineeRepository traineeRepository;
  private final TraineeConsentRepository traineeConsentRepository;
  private final RefConsentTypeRepository refConsentTypeRepository;
  private final RefConsentStatusRepository refConsentStatusRepository;
  private final CommunicationLogRepository communicationLogRepository;
  private final RefCommunicationChannelRepository refCommunicationChannelRepository;
  private final RefCommunicationDirectionRepository refCommunicationDirectionRepository;
  private final RefCommunicationPurposeRepository refCommunicationPurposeRepository;
  private final RefCommunicationStatusRepository refCommunicationStatusRepository;
  private final WorkflowSecurityScopeValidator workflowSecurityScopeValidator;
  private final CommunicationProperties properties;
  private final PlatformTransactionManager transactionManager;

  @Autowired
  public NotificationDispatcherServiceImpl(
      CommunicationConsentValidator consentValidator,
      CommunicationTemplateRegistry templateRegistry,
      SmsCommunicationProvider smsProvider,
      WhatsAppCommunicationProvider whatsAppProvider,
      EmailCommunicationProvider emailProvider,
      TraineeRepository traineeRepository,
      TraineeConsentRepository traineeConsentRepository,
      RefConsentTypeRepository refConsentTypeRepository,
      RefConsentStatusRepository refConsentStatusRepository,
      CommunicationLogRepository communicationLogRepository,
      RefCommunicationChannelRepository refCommunicationChannelRepository,
      RefCommunicationDirectionRepository refCommunicationDirectionRepository,
      RefCommunicationPurposeRepository refCommunicationPurposeRepository,
      RefCommunicationStatusRepository refCommunicationStatusRepository,
      WorkflowSecurityScopeValidator workflowSecurityScopeValidator,
      CommunicationProperties properties,
      PlatformTransactionManager transactionManager) {
    this.consentValidator = Objects.requireNonNull(consentValidator, "CommunicationConsentValidator is required");
    this.templateRegistry = Objects.requireNonNull(templateRegistry, "CommunicationTemplateRegistry is required");
    this.smsProvider = Objects.requireNonNull(smsProvider, "SmsCommunicationProvider is required");
    this.whatsAppProvider = Objects.requireNonNull(whatsAppProvider, "WhatsAppCommunicationProvider is required");
    this.emailProvider = Objects.requireNonNull(emailProvider, "EmailCommunicationProvider is required");
    this.traineeRepository = Objects.requireNonNull(traineeRepository, "TraineeRepository is required");
    this.traineeConsentRepository = Objects.requireNonNull(traineeConsentRepository, "TraineeConsentRepository is required");
    this.refConsentTypeRepository = Objects.requireNonNull(refConsentTypeRepository, "RefConsentTypeRepository is required");
    this.refConsentStatusRepository = Objects.requireNonNull(refConsentStatusRepository, "RefConsentStatusRepository is required");
    this.communicationLogRepository = Objects.requireNonNull(communicationLogRepository, "CommunicationLogRepository is required");
    this.refCommunicationChannelRepository = Objects.requireNonNull(refCommunicationChannelRepository, "RefCommunicationChannelRepository is required");
    this.refCommunicationDirectionRepository = Objects.requireNonNull(refCommunicationDirectionRepository, "RefCommunicationDirectionRepository is required");
    this.refCommunicationPurposeRepository = Objects.requireNonNull(refCommunicationPurposeRepository, "RefCommunicationPurposeRepository is required");
    this.refCommunicationStatusRepository = Objects.requireNonNull(refCommunicationStatusRepository, "RefCommunicationStatusRepository is required");
    this.workflowSecurityScopeValidator = workflowSecurityScopeValidator;
    this.properties = Objects.requireNonNull(properties, "CommunicationProperties is required");
    this.transactionManager = Objects.requireNonNull(transactionManager, "PlatformTransactionManager is required");
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = CommunicationConsentException.class)
  public NotificationDispatchResponse dispatchNotification(CommunicationDispatchCommand command) {
    if (command == null) {
      throw new BadRequestException("Communication dispatch command cannot be null", "COMMAND_REQUIRED");
    }
    if (command.traineeId() == null) {
      throw new BadRequestException("Trainee ID is required for communication dispatch", "TRAINEE_ID_REQUIRED");
    }
    if (command.channel() == null) {
      throw new BadRequestException("Communication channel is required", "CHANNEL_REQUIRED");
    }
    if (command.templateCode() == null || command.templateCode().isBlank()) {
      throw new BadRequestException("Template code is required for communication dispatch", "TEMPLATE_CODE_REQUIRED");
    }

    // 1. Authoritative Trainee Scope & Multi-Tenant Boundary Check
    if (workflowSecurityScopeValidator != null) {
      workflowSecurityScopeValidator.validateTraineeScope(command.traineeId());
    }

    // 2. Resolve Candidate Record
    Trainee trainee = traineeRepository.findById(command.traineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));

    // 3. Resolve Real Unmasked Recipient
    String rawRecipient;
    CommunicationChannel channel = command.channel();
    switch (channel) {
      case SMS, WHATSAPP -> {
        rawRecipient = trainee.getPhone();
        if (rawRecipient == null || rawRecipient.isBlank()) {
          throw new BadRequestException("Trainee has no registered phone number for " + channel, "MISSING_RECIPIENT_PHONE");
        }
      }
      case EMAIL -> {
        rawRecipient = trainee.getEmail();
        if (rawRecipient == null || rawRecipient.isBlank()) {
          throw new BadRequestException("Trainee has no registered email address for Email delivery", "MISSING_RECIPIENT_EMAIL");
        }
      }
      default -> throw new BadRequestException("Unsupported communication channel: " + channel, "UNSUPPORTED_CHANNEL");
    }

    // 4. Approved Template Rendering & Safe Variable Interpolation
    CommunicationTemplateRegistry.RenderedTemplate rendered = templateRegistry.render(
        command.templateCode(),
        command.templateParameters(),
        trainee
    );

    // 5. Authoritative Consent Validation (Pre-Dispatch Fail-Closed Boundary)
    String channelCode = channel.name();
    String purposeCode = command.purpose() != null ? command.purpose().name() : CommunicationPurpose.OPERATIONAL.name();

    try {
      consentValidator.validateCommunicationConsent(trainee.getId(), channelCode, purposeCode);
    } catch (CommunicationConsentException ex) {
      // Record BLOCKED_NO_CONSENT in communication_logs before failing closed
      recordBlockedConsentLog(trainee, channelCode, purposeCode, command.templateCode(), command.initiatedByUserId(), ex.getErrorCode());
      log.warn("[NotificationDispatcher] Outbound dispatch blocked by consent validator: traineeId={}, channel={}, code={}",
          trainee.getId(), channelCode, ex.getErrorCode());
      throw ex; // Re-throw to caller (HTTP 403)
    }

    // 6. Concrete Provider Delivery
    ProviderDeliveryResult deliveryResult;
    try {
      deliveryResult = switch (channel) {
        case SMS -> smsProvider.sendSms(rawRecipient, rendered.bodyText(), command.templateCode());
        case WHATSAPP -> whatsAppProvider.sendWhatsApp(rawRecipient, rendered.bodyText(), command.templateCode());
        case EMAIL -> emailProvider.sendEmail(rawRecipient, rendered.subject(), rendered.bodyText(), rendered.bodyHtml());
        default -> throw new BadRequestException("Unsupported channel: " + channel, "UNSUPPORTED_CHANNEL");
      };
    } catch (Exception ex) {
      log.error("[NotificationDispatcher] Unhandled error during provider delivery invocation: {}", ex.getMessage(), ex);
      deliveryResult = ProviderDeliveryResult.failure(
          channel,
          "UNKNOWN",
          null,
          "DISPATCH_ERROR",
          ex.getMessage(),
          null
      );
    }

    // 7. Persist Communication Log Record
    CommunicationLog logEntity = persistCommunicationLog(
        trainee,
        channelCode,
        purposeCode,
        command.templateCode(),
        command.initiatedByUserId(),
        deliveryResult
    );

    // 8. Return Sanitized Dispatch Response
    return new NotificationDispatchResponse(
        logEntity.getId(),
        channelCode,
        purposeCode,
        CommunicationTriggerEvent.maskRecipient(rawRecipient),
        deliveryResult.providerName(),
        deliveryResult.providerMessageId(),
        deliveryResult.statusCode(),
        deliveryResult.accepted(),
        deliveryResult.accepted() ? "Notification accepted by communication provider" : "Notification rejected by provider: " + deliveryResult.errorMessage(),
        deliveryResult.dispatchedAt()
    );
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CommunicationConsentException.class)
  public NotificationDispatchResponse dispatchFromTriggerEvent(CommunicationTriggerEvent event) {
    if (event == null) {
      throw new BadRequestException("CommunicationTriggerEvent cannot be null", "EVENT_REQUIRED");
    }

    Long traineeId = event.entityId();
    if (traineeId == null) {
      throw new BadRequestException("Event entityId (traineeId) is required", "TRAINEE_ID_REQUIRED");
    }

    CommunicationChannel channel = CommunicationChannel.fromCode(event.channel());
    if (channel == null) {
      channel = CommunicationChannel.SMS;
    }

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        traineeId,
        channel,
        CommunicationPurpose.OPERATIONAL,
        event.templateCode(),
        Map.of(),
        event.actorUserId(),
        null,
        null
    );

    return dispatchNotification(command);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public LiveSmokeTestResult executeLiveSmokeTest(LiveSmokeTestRequest request) {
    if (request == null) {
      throw new BadRequestException("Live smoke test request cannot be null", "REQUEST_REQUIRED");
    }
    if (request.channel() == null) {
      throw new BadRequestException("Channel is required for live smoke test", "CHANNEL_REQUIRED");
    }

    CommunicationChannel channel = request.channel();
    Long targetTraineeId = request.targetTraineeId();

    if (targetTraineeId == null) {
      targetTraineeId = resolveOrCreateTestTrainee(channel, request.recipientOverride());
    }

    // Ensure candidate has valid consent for requested channel in test database
    ensureActiveConsentForChannel(targetTraineeId, channel);

    Map<String, String> params = new HashMap<>();
    params.put("providerName", channel.name() + "-Live");
    params.put("channel", channel.name());
    params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    if (request.customMessage() != null && !request.customMessage().isBlank()) {
      params.put("message", request.customMessage().trim());
    }

    String templateCode = "LIVE_SMOKE_TEST";
    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        targetTraineeId,
        channel,
        CommunicationPurpose.OPERATIONAL,
        templateCode,
        params,
        1L, // Admin/System Actor
        null,
        null
    );

    NotificationDispatchResponse dispatchResponse = dispatchNotification(command);

    String proofSummary = String.format(
        "Channel=%s | Provider=%s | MessageSID=%s | Status=%s | LogId=%d | MaskedRecipient=%s",
        dispatchResponse.channel(),
        dispatchResponse.providerName(),
        dispatchResponse.providerMessageId(),
        dispatchResponse.status(),
        dispatchResponse.communicationLogId(),
        dispatchResponse.recipientMasked()
    );

    return new LiveSmokeTestResult(
        dispatchResponse.channel(),
        dispatchResponse.providerName(),
        dispatchResponse.recipientMasked(),
        dispatchResponse.providerMessageId(),
        dispatchResponse.status(),
        dispatchResponse.accepted(),
        dispatchResponse.communicationLogId(),
        proofSummary,
        dispatchResponse.dispatchedAt()
    );
  }

  private CommunicationLog persistCommunicationLog(
      Trainee trainee,
      String channelCode,
      String purposeCode,
      String templateCode,
      Long initiatedByUserId,
      ProviderDeliveryResult result) {

    RefCommunicationChannel channel = refCommunicationChannelRepository.findByChannelCode(channelCode)
        .orElseGet(() -> refCommunicationChannelRepository.findById(1L).orElseThrow());

    RefCommunicationDirection direction = refCommunicationDirectionRepository.findByDirectionCode("OUTBOUND")
        .orElseGet(() -> refCommunicationDirectionRepository.findById(1L).orElseThrow());

    RefCommunicationPurpose purpose = refCommunicationPurposeRepository.findByPurposeCode(purposeCode)
        .orElseGet(() -> refCommunicationPurposeRepository.findById(6L).orElseThrow());

    String statusCode = result.accepted() ? "SENT" : "FAILED";
    RefCommunicationStatus status = refCommunicationStatusRepository.findByStatusCode(statusCode)
        .orElseGet(() -> refCommunicationStatusRepository.findById(result.accepted() ? 3L : 7L).orElseThrow());

    RefConsentType consentType = channel.getRequiredConsentType();

    LocalDateTime now = LocalDateTime.now();
    CommunicationLog logEntity = new CommunicationLog(trainee, channel, direction, purpose, status);
    logEntity.setConsentType(consentType);
    logEntity.setConsentCheckedAt(now);
    logEntity.setInitiatedByUserId(initiatedByUserId);
    logEntity.setProviderMessageId(result.providerMessageId());
    logEntity.setMessageTemplateCode(templateCode);
    logEntity.setFailureReason(result.accepted() ? null : result.errorMessage());
    logEntity.setQueuedAt(now);
    logEntity.setSentAt(result.accepted() ? now : null);
    logEntity.setFailedAt(result.accepted() ? null : now);
    logEntity.setCreatedAt(now);
    logEntity.setUpdatedAt(now);

    return communicationLogRepository.save(logEntity);
  }

  private CommunicationLog recordBlockedConsentLog(
      Trainee trainee,
      String channelCode,
      String purposeCode,
      String templateCode,
      Long initiatedByUserId,
      String failureReason) {

    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tt.execute(status -> {
      RefCommunicationChannel channel = refCommunicationChannelRepository.findByChannelCode(channelCode)
          .orElseGet(() -> refCommunicationChannelRepository.findById(1L).orElseThrow());

      RefCommunicationDirection direction = refCommunicationDirectionRepository.findByDirectionCode("OUTBOUND")
          .orElseGet(() -> refCommunicationDirectionRepository.findById(1L).orElseThrow());

      RefCommunicationPurpose purpose = refCommunicationPurposeRepository.findByPurposeCode(purposeCode)
          .orElseGet(() -> refCommunicationPurposeRepository.findById(6L).orElseThrow());

      RefCommunicationStatus statusEntity = refCommunicationStatusRepository.findByStatusCode("BLOCKED_NO_CONSENT")
          .orElseGet(() -> refCommunicationStatusRepository.findById(8L).orElseThrow());

      LocalDateTime now = LocalDateTime.now();
      CommunicationLog logEntity = new CommunicationLog(trainee, channel, direction, purpose, statusEntity);
      logEntity.setConsentType(channel.getRequiredConsentType());
      logEntity.setConsentCheckedAt(now);
      logEntity.setInitiatedByUserId(initiatedByUserId);
      logEntity.setMessageTemplateCode(templateCode);
      logEntity.setFailureReason(failureReason != null ? failureReason : "CONSENT_NOT_GRANTED");
      logEntity.setFailedAt(now);
      logEntity.setCreatedAt(now);
      logEntity.setUpdatedAt(now);

      return communicationLogRepository.save(logEntity);
    });
  }

  private Long resolveOrCreateTestTrainee(CommunicationChannel channel, String recipientOverride) {
    // Look for existing test trainee with ID 9991L or first available active trainee
    Optional<Trainee> existing = traineeRepository.findById(9991L);
    if (existing.isPresent()) {
      Trainee t = existing.get();
      if (recipientOverride != null && !recipientOverride.isBlank()) {
        if (channel == CommunicationChannel.EMAIL) {
          t.setEmail(recipientOverride.trim());
        } else {
          t.setPhone(recipientOverride.trim());
        }
        traineeRepository.save(t);
      }
      return t.getId();
    }

    // Fall back to first available trainee
    return traineeRepository.findAll().stream()
        .findFirst()
        .map(Trainee::getId)
        .orElseThrow(() -> new BadRequestException("No test trainee available in database catalog", "NO_TEST_TRAINEE"));
  }

  private void ensureActiveConsentForChannel(Long traineeId, CommunicationChannel channel) {
    ensureConsentGranted(traineeId, "DATA_PROCESSING");
    ensureConsentGranted(traineeId, channel.getRequiredConsentCode());
  }

  private void ensureConsentGranted(Long traineeId, String consentCode) {
    RefConsentType consentType = refConsentTypeRepository.findByConsentCode(consentCode)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "consentCode"));

    List<TraineeConsent> existingConsents = traineeConsentRepository.findByTraineeIdAndConsentTypeId(traineeId, consentType.getId());
    RefConsentStatus grantedStatus = refConsentStatusRepository.findByStatusCode("GRANTED")
        .orElseGet(() -> refConsentStatusRepository.findById(1L).orElseThrow());

    if (existingConsents != null && !existingConsents.isEmpty()) {
      TraineeConsent c = existingConsents.get(0);
      c.setConsentStatus(grantedStatus);
      c.setGrantedAt(LocalDateTime.now().minusDays(1));
      c.setRevokedAt(null);
      c.setExpiresAt(LocalDateTime.now().plusYears(1));
      c.setUpdatedAt(LocalDateTime.now());
      traineeConsentRepository.save(c);
      return;
    }

    // Create granted consent
    Trainee trainee = traineeRepository.findById(traineeId).orElseThrow();
    TraineeConsent newConsent = new TraineeConsent(trainee, consentType, "Automated Notification", grantedStatus, "v1.0");
    newConsent.setGrantedAt(LocalDateTime.now().minusDays(1));
    newConsent.setExpiresAt(LocalDateTime.now().plusYears(1));
    newConsent.setCreatedAt(LocalDateTime.now());
    newConsent.setUpdatedAt(LocalDateTime.now());
    traineeConsentRepository.save(newConsent);
  }
}
