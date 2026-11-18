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
import in.gov.sih.sih26135.communication.ratelimit.CommunicationRateLimiter;
import in.gov.sih.sih26135.communication.template.CommunicationTemplateRegistry;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRequestRepository;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationDirectionRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.security.InternalAuthenticationToken;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final CommunicationRateLimiter rateLimiter;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final CertificationRepository certificationRepository;
  private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;

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
      PlatformTransactionManager transactionManager,
      @Autowired(required = false) CommunicationRateLimiter rateLimiter,
      @Autowired(required = false) TrainingEnrollmentRepository trainingEnrollmentRepository,
      @Autowired(required = false) CertificationRepository certificationRepository,
      @Autowired(required = false) EmploymentVerificationRequestRepository employmentVerificationRequestRepository) {
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
    this.rateLimiter = rateLimiter;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.certificationRepository = certificationRepository;
    this.employmentVerificationRequestRepository = employmentVerificationRequestRepository;
  }

  @Override
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

    // 2. Validate and enforce effective actor user ID (never trust unauthenticated caller-supplied user IDs)
    Long effectiveInitiatedByUserId = resolveEffectiveInitiatedByUserId(command.initiatedByUserId());

    // 3. Resolve Candidate Record (Read-Only)
    Trainee trainee = traineeRepository.findById(command.traineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));

    // 4. Resolve Real Unmasked Recipient
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
      // Record BLOCKED_NO_CONSENT in communication_logs in an isolated transaction before failing closed
      recordBlockedConsentLog(trainee, channelCode, purposeCode, command.templateCode(), effectiveInitiatedByUserId, ex.getErrorCode());
      log.warn("[NotificationDispatcher] Outbound dispatch blocked by consent validator: traineeId={}, channel={}, code={}",
          trainee.getId(), channelCode, ex.getErrorCode());
      throw ex; // Re-throw to caller (HTTP 403)
    }

    // 5b. Authoritative Multi-Tenant Rate Limiting & Anti-Spam Check
    if (rateLimiter != null) {
      Long authoritativeProviderId = rateLimiter.resolveAuthoritativeProviderId(trainee.getId(), null);
      String paramsHash = calculateParametersHash(command.templateParameters());
      boolean effectiveRetry = command.isRetry() && isAuthorizedInternalQueueWorker();
      rateLimiter.checkAndReserveQuota(trainee.getId(), channel, authoritativeProviderId, command.templateCode(), paramsHash, effectiveRetry);
    }

    // 6. Concrete Provider Delivery (Executed OUTSIDE any open database transaction)
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
      String classifiedCode = classifyProviderException(ex);
      boolean isTimeout = "AMBIGUOUS_PROVIDER_TIMEOUT".equals(classifiedCode) || in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl.isAmbiguousTimeout(ex, classifiedCode, ex.getMessage());
      String statusCode = isTimeout ? "ATTEMPTED" : "FAILED";
      deliveryResult = new ProviderDeliveryResult(
          channel,
          resolveProviderName(channel),
          null,
          statusCode,
          false,
          null,
          classifiedCode,
          ex.getMessage(),
          LocalDateTime.now()
      );
    }

    // 7. Persist Communication Log Record in an isolated transaction
    CommunicationLog logEntity = persistCommunicationLog(
        trainee,
        channelCode,
        purposeCode,
        command.templateCode(),
        effectiveInitiatedByUserId,
        deliveryResult
    );

    // 8. Return Sanitized Dispatch Response
    boolean isDemo = properties.isDemoMode() || "SIMULATED".equalsIgnoreCase(logEntity.getCommunicationStatus().getStatusCode());
    String effectiveDeliveryMode = isDemo ? "DEMO" : "LIVE";
    String statusCode = logEntity.getCommunicationStatus().getStatusCode();
    boolean isSimulated = isDemo;
    boolean isPhysicallyDelivered = "DELIVERED".equalsIgnoreCase(statusCode);
    boolean isAmbiguous = in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl.isAmbiguousTimeout(null, deliveryResult.errorCode(), deliveryResult.errorMessage());
    boolean effectiveAccepted = deliveryResult.accepted() && !"FAILED".equalsIgnoreCase(statusCode) && !isAmbiguous;

    String safeMessage;
    String errorCode = null;
    String errorExplanation = null;

    if (isAmbiguous) {
      errorCode = "AMBIGUOUS_PROVIDER_TIMEOUT";
      errorExplanation = deliveryResult.errorMessage() != null ? deliveryResult.errorMessage() : "Provider response timed out";
      safeMessage = "Notification dispatch outcome ambiguous: provider response timed out (manual reconciliation required)";
    } else if (!effectiveAccepted || "FAILED".equalsIgnoreCase(statusCode)) {
      errorCode = deliveryResult.errorCode() != null ? deliveryResult.errorCode() : "DELIVERY_FAILED";
      errorExplanation = logEntity.getFailureReason() != null ? logEntity.getFailureReason() : deliveryResult.errorMessage();
      if (errorExplanation == null || errorExplanation.isBlank()) {
        errorExplanation = "Provider rejected or failed notification dispatch";
      }
      safeMessage = isDemo
          ? "Simulated notification failed: " + errorExplanation
          : "Notification dispatch failed: " + errorExplanation;
    } else if (isDemo) {
      safeMessage = "Notification simulated successfully in DEMO mode (no external network transmission)";
    } else if ("DELIVERED".equalsIgnoreCase(statusCode)) {
      safeMessage = "Notification confirmed delivered to recipient device";
    } else if ("SENT".equalsIgnoreCase(statusCode)) {
      safeMessage = "Notification dispatched to carrier network";
    } else if ("ATTEMPTED".equalsIgnoreCase(statusCode)) {
      safeMessage = "Notification dispatch attempted";
    } else {
      safeMessage = "Notification accepted by communication provider and queued for delivery";
    }

    LocalDateTime deliveredAt = isPhysicallyDelivered ? logEntity.getDeliveredAt() : null;

    return new NotificationDispatchResponse(
        logEntity.getId(),
        channelCode,
        purposeCode,
        effectiveDeliveryMode,
        statusCode,
        isSimulated,
        isPhysicallyDelivered,
        CommunicationTriggerEvent.maskRecipient(rawRecipient),
        deliveryResult.providerName(),
        deliveryResult.providerMessageId(),
        effectiveAccepted,
        safeMessage,
        errorCode,
        errorExplanation,
        deliveryResult.dispatchedAt(),
        deliveredAt
    );
  }

  @Override
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

    String refId = event.getCorrelationId() != null && !event.getCorrelationId().isBlank()
        ? event.getCorrelationId()
        : (event.getEventId() != null && !event.getEventId().isBlank()
            ? event.getEventId()
            : "EVT-" + event.entityId());

    String rawTplCode = event.templateCode() != null && !event.templateCode().isBlank()
        ? event.templateCode().trim().toUpperCase()
        : "OPERATIONAL_ALERT";

    Map<String, String> payloadAttrs = extractPayloadAttributes(event.payloadSummary());
    Map<String, String> params = new HashMap<>();
    String effectiveTplCode = rawTplCode;

    switch (rawTplCode) {
      case "OPERATIONAL_ALERT" -> {
        params.put("referenceId", refId);
      }
      case "TWILIO_TRIAL_GENERIC" -> {
        String msg = event.payloadSummary() != null && !event.payloadSummary().isBlank()
            ? event.payloadSummary().trim()
            : "Lifecycle notice for event " + event.getEventType() + " (Ref: " + refId + ")";
        params.put("message", msg);
      }
      case "LIVE_SMOKE_TEST" -> {
        params.put("providerName", event.channel() != null ? event.channel() : "System");
        params.put("channel", channel.name());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
      }
      case "TRAINEE_ENROLLMENT_CONFIRMATION" -> {
        String batchCode = payloadAttrs.get("batchCode");
        String courseName = payloadAttrs.get("courseName");
        String startDate = payloadAttrs.get("startDate");

        if ((batchCode == null || courseName == null || startDate == null) && trainingEnrollmentRepository != null) {
          try {
            List<TrainingEnrollment> enrollments = trainingEnrollmentRepository.findByTraineeId(traineeId);
            if (enrollments != null && !enrollments.isEmpty()) {
              TrainingEnrollment latest = enrollments.get(enrollments.size() - 1);
              if (batchCode == null && latest.getTrainingBatch() != null) {
                batchCode = latest.getTrainingBatch().getBatchCode();
              }
              if (courseName == null && latest.getCourse() != null) {
                courseName = latest.getCourse().getCourseName();
              }
              if (startDate == null) {
                if (latest.getStartDate() != null) {
                  startDate = latest.getStartDate().toString();
                } else if (latest.getTrainingBatch() != null && latest.getTrainingBatch().getStartDate() != null) {
                  startDate = latest.getTrainingBatch().getStartDate().toString();
                }
              }
            }
          } catch (Exception ex) {
            log.warn("[NotificationDispatcher] Error resolving enrollment for traineeId={}: {}", traineeId, ex.getMessage());
          }
        }

        if (batchCode != null && !batchCode.isBlank()
            && courseName != null && !courseName.isBlank()
            && startDate != null && !startDate.isBlank()) {
          params.put("batchCode", batchCode.trim());
          params.put("courseName", courseName.trim());
          params.put("startDate", startDate.trim());
        } else {
          effectiveTplCode = fallbackGenericTemplate(event, refId, params);
        }
      }
      case "ASSESSMENT_CERTIFICATION_NOTICE" -> {
        String certNum = payloadAttrs.get("certificateNumber");
        if (certNum == null && certificationRepository != null) {
          try {
            List<Certification> certs = certificationRepository.findByTraineeId(traineeId);
            if (certs != null && !certs.isEmpty()) {
              certNum = certs.get(certs.size() - 1).getCertificateNumber();
            }
          } catch (Exception ex) {
            log.warn("[NotificationDispatcher] Error resolving certification for traineeId={}: {}", traineeId, ex.getMessage());
          }
        }
        if (certNum != null && !certNum.isBlank()) {
          params.put("certificateNumber", certNum.trim());
        } else {
          effectiveTplCode = fallbackGenericTemplate(event, refId, params);
        }
      }
      case "PLACEMENT_OFFER_ALERT" -> {
        String companyName = payloadAttrs.get("companyName");
        String jobTitle = payloadAttrs.get("jobTitle");
        if (companyName != null && !companyName.isBlank() && jobTitle != null && !jobTitle.isBlank()) {
          params.put("companyName", companyName.trim());
          params.put("jobTitle", jobTitle.trim());
        } else {
          effectiveTplCode = fallbackGenericTemplate(event, refId, params);
        }
      }
      case "VERIFICATION_STATUS_NOTICE" -> {
        String status = payloadAttrs.get("status");
        String verificationId = payloadAttrs.get("verificationId");
        if ((status == null || verificationId == null) && employmentVerificationRequestRepository != null) {
          try {
            List<EmploymentVerificationRequest> requests = employmentVerificationRequestRepository.findByTraineeId(traineeId);
            if (requests != null && !requests.isEmpty()) {
              EmploymentVerificationRequest latest = requests.get(requests.size() - 1);
              if (status == null && latest.getEmploymentVerificationRequestStatus() != null) {
                status = latest.getEmploymentVerificationRequestStatus().getStatusCode();
              }
              if (verificationId == null) {
                verificationId = latest.getRequestNumber();
              }
            }
          } catch (Exception ex) {
            log.warn("[NotificationDispatcher] Error resolving verification request for traineeId={}: {}", traineeId, ex.getMessage());
          }
        }
        if (status != null && !status.isBlank() && verificationId != null && !verificationId.isBlank()) {
          params.put("status", status.trim());
          params.put("verificationId", verificationId.trim());
        } else {
          effectiveTplCode = fallbackGenericTemplate(event, refId, params);
        }
      }
      case "FOLLOWUP_REMINDER" -> {
        String milestone = payloadAttrs.get("milestoneMonth");
        if (milestone != null && !milestone.isBlank()) {
          params.put("milestoneMonth", milestone.trim());
        } else {
          effectiveTplCode = fallbackGenericTemplate(event, refId, params);
        }
      }
      default -> {
        for (Map.Entry<String, String> entry : payloadAttrs.entrySet()) {
          params.put(entry.getKey(), entry.getValue());
        }
        if (params.isEmpty()) {
          params.put("referenceId", refId);
        }
      }
    }

    CommunicationDispatchCommand command = new CommunicationDispatchCommand(
        traineeId,
        channel,
        CommunicationPurpose.OPERATIONAL,
        effectiveTplCode,
        params,
        event.actorUserId() != null ? event.actorUserId() : 1L,
        null,
        null
    );

    return dispatchNotification(command);
  }

  private String fallbackGenericTemplate(CommunicationTriggerEvent event, String refId, Map<String, String> params) {
    if (event.payloadSummary() != null && !event.payloadSummary().isBlank()) {
      params.put("message", event.payloadSummary().trim());
      return "TWILIO_TRIAL_GENERIC";
    } else {
      params.put("referenceId", refId);
      return "OPERATIONAL_ALERT";
    }
  }

  private Map<String, String> extractPayloadAttributes(String payloadSummary) {
    Map<String, String> attributes = new HashMap<>();
    if (payloadSummary == null || payloadSummary.isBlank()) {
      return attributes;
    }
    String trimmed = payloadSummary.trim();
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      Matcher m = Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(trimmed);
      while (m.find()) {
        attributes.put(m.group(1), m.group(2).trim());
      }
      return attributes;
    }
    String[] lines = trimmed.split("[,;\\n\\r]+");
    for (String part : lines) {
      int sep = part.indexOf('=');
      if (sep == -1) {
        sep = part.indexOf(':');
      }
      if (sep > 0 && sep < part.length() - 1) {
        String key = part.substring(0, sep).trim();
        String val = part.substring(sep + 1).trim();
        if (key.matches("[A-Za-z0-9_]+") && !val.isBlank()) {
          attributes.put(key, val);
        }
      }
    }
    return attributes;
  }

  @Override
  public LiveSmokeTestResult executeLiveSmokeTest(LiveSmokeTestRequest request) {
    if (request == null) {
      throw new BadRequestException("Live smoke test request cannot be null", "REQUEST_REQUIRED");
    }
    if (request.channel() == null) {
      throw new BadRequestException("Channel is required for live smoke test", "CHANNEL_REQUIRED");
    }

    CommunicationChannel channel = request.channel();
    Long targetTraineeId = request.targetTraineeId() != null ? request.targetTraineeId() : request.traineeId();

    if (targetTraineeId == null) {
      throw new BadRequestException("Target trainee ID is required for live smoke test", "TRAINEE_ID_REQUIRED");
    }

    // When live recipient override is enabled for smoke tests, require explicit test recipient configuration
    if (properties.isLiveRecipientOverrideEnabled()) {
      String resolvedTestRecipient = resolveChannelTestRecipient(channel, request.recipientOverride());
      if (resolvedTestRecipient == null || resolvedTestRecipient.isBlank()) {
        log.error("[LiveSmokeTest] FATAL: Live recipient override is enabled, but required test recipient for channel {} is missing", channel);
        throw new BadRequestException(
            "Live recipient override is enabled for live smoke test, but no valid test recipient is configured for channel " + channel
                + " (e.g. SIH_LIVE_TEST_PHONE, SIH_LIVE_TEST_WHATSAPP, SIH_LIVE_TEST_EMAIL)",
            "RECIPIENT_OVERRIDE_REQUIRED"
        );
      }
    }

    // Explicit DPDP Act Consent Enactment: Automatic grant/regrant has been completely eliminated.
    // The test trainee must legitimately have valid consent in the active database catalog.
    // If consent is missing, revoked, or expired, dispatchNotification() fails closed with HTTP 403.

    Map<String, String> params = new HashMap<>();
    String providerName = switch (channel) {
      case SMS -> smsProvider.getProviderName();
      case WHATSAPP -> whatsAppProvider.getProviderName();
      case EMAIL -> emailProvider.getProviderName();
      default -> "System";
    };
    params.put("providerName", providerName);
    params.put("channel", channel.name());
    params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

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

    String mode = properties.getMode().name();
    String proofSummary = String.format(
        "Mode=%s | Channel=%s | Provider=%s | MessageSID=%s | Status=%s | LogId=%d | MaskedRecipient=%s",
        mode,
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
        mode,
        dispatchResponse.recipientMasked(),
        dispatchResponse.providerMessageId(),
        dispatchResponse.status(),
        dispatchResponse.accepted(),
        dispatchResponse.communicationLogId(),
        proofSummary,
        dispatchResponse.dispatchedAt()
    );
  }

  public CommunicationLog persistCommunicationLog(
      Trainee trainee,
      String channelCode,
      String purposeCode,
      String templateCode,
      Long initiatedByUserId,
      ProviderDeliveryResult result) {

    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tt.execute(txStatus -> {
      RefCommunicationChannel channel = refCommunicationChannelRepository.findByChannelCode(channelCode)
          .orElseGet(() -> refCommunicationChannelRepository.findById(1L).orElseThrow());

      RefCommunicationDirection direction = refCommunicationDirectionRepository.findByDirectionCode("OUTBOUND")
          .orElseGet(() -> refCommunicationDirectionRepository.findById(1L).orElseThrow());

      RefCommunicationPurpose purpose = refCommunicationPurposeRepository.findByPurposeCode(purposeCode)
          .orElseGet(() -> refCommunicationPurposeRepository.findByPurposeCode("OPERATIONAL")
              .orElseGet(() -> refCommunicationPurposeRepository.findById(6L).orElseThrow()));

      String rawStatus = result.statusCode();
      String normalized = rawStatus != null ? rawStatus.trim().toUpperCase() : "";

      boolean isAmbiguous = in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl.isAmbiguousTimeout(null, result.errorCode(), result.errorMessage());

      String mappedStatus;
      if (isAmbiguous) {
        mappedStatus = "ATTEMPTED";
      } else if ("FAILED".equals(normalized) || "UNDELIVERED".equals(normalized) || !result.accepted()) {
        mappedStatus = "FAILED";
      } else if (properties.isDemoMode() || "SIMULATED".equals(normalized)) {
        mappedStatus = "SIMULATED";
      } else if ("DELIVERED".equals(normalized)) {
        mappedStatus = "DELIVERED";
      } else if ("SENT".equals(normalized)) {
        mappedStatus = "SENT";
      } else if ("ATTEMPTED".equals(normalized) || "SENDING".equals(normalized)) {
        mappedStatus = "ATTEMPTED";
      } else if ("QUEUED".equals(normalized) || "ACCEPTED".equals(normalized)) {
        mappedStatus = "QUEUED";
      } else {
        mappedStatus = result.accepted() ? "QUEUED" : "FAILED";
      }

      RefCommunicationStatus status = refCommunicationStatusRepository.findByStatusCode(mappedStatus)
          .orElseGet(() -> refCommunicationStatusRepository.findById(
              "SIMULATED".equals(mappedStatus) ? 9L :
              "DELIVERED".equals(mappedStatus) ? 4L :
              "SENT".equals(mappedStatus) ? 3L :
              "ATTEMPTED".equals(mappedStatus) ? 2L :
              "QUEUED".equals(mappedStatus) ? 1L : 7L
          ).orElseThrow());

      RefConsentType consentType = channel.getRequiredConsentType();

      LocalDateTime now = LocalDateTime.now();
      CommunicationLog logEntity = new CommunicationLog(trainee, channel, direction, purpose, status);
      logEntity.setConsentType(consentType);
      logEntity.setConsentCheckedAt(now);
      logEntity.setInitiatedByUserId(initiatedByUserId);
      logEntity.setProviderMessageId(result.providerMessageId());
      logEntity.setMessageTemplateCode(templateCode);

      boolean isFailed = "FAILED".equals(mappedStatus);
      if (isFailed) {
        logEntity.setFailedAt(now);
        String reason = result.errorMessage();
        if (reason == null || reason.isBlank()) {
          reason = "Provider returned delivery failure status: " + (rawStatus != null && !rawStatus.isBlank() ? rawStatus : "FAILED");
        }
        logEntity.setFailureReason(reason);
      } else if (isAmbiguous) {
        String reason = result.errorMessage() != null ? result.errorMessage() : "Provider response timed out";
        logEntity.setFailureReason("Provider response timed out; delivery status is ambiguous (manual reconciliation required): " + reason);
      } else {
        logEntity.setFailureReason(null);
      }

      logEntity.setQueuedAt(now);
      if ("SENT".equals(mappedStatus) || "DELIVERED".equals(mappedStatus)) {
        logEntity.setSentAt(now);
      }
      if ("DELIVERED".equals(mappedStatus)) {
        logEntity.setDeliveredAt(now);
      }
      logEntity.setCreatedAt(now);
      logEntity.setUpdatedAt(now);

      return communicationLogRepository.save(logEntity);
    });
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

  private Long resolveEffectiveInitiatedByUserId(Long commandUserId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof InternalAuthenticationToken internalAuth) {
      InternalServicePrincipal principal = internalAuth.getPrincipal();
      if (principal != null) {
        log.info("[NotificationDispatcher] Internal service dispatch: serviceId={}, serviceName={}, tenantScopeType={}",
            principal.getServiceId(), principal.getServiceName(), principal.getTenantScopeType());
      } else {
        log.info("[NotificationDispatcher] Internal service dispatch authenticated: {}", internalAuth.getName());
      }
      // Truthful internal audit identity: automated background/internal executions do not originate
      // from a human user. When commandUserId is null, persist null in communication_logs.initiated_by_user_id
      // (which is nullable). Never artificially attribute system operations to human user ID 1.
      return commandUserId;
    }
    if (workflowSecurityScopeValidator != null) {
      Long authUserId = workflowSecurityScopeValidator.resolveAuthenticatedUserId();
      if (authUserId == null) {
        throw new BadRequestException("Authenticated user identity cannot be resolved for dispatch", "AUTHENTICATED_USER_UNRESOLVED");
      }
      if (commandUserId != null && !commandUserId.equals(authUserId) && !workflowSecurityScopeValidator.isAdministrativeCaller()) {
        log.warn("Security violation: authenticated user {} attempted to claim initiatedByUserId={}", authUserId, commandUserId);
        throw new BadRequestException("Caller cannot claim a different initiatedByUserId than the authenticated user", "ACTOR_USER_MISMATCH");
      }
      return authUserId;
    }
    return commandUserId;
  }

  private String resolveChannelTestRecipient(CommunicationChannel channel, String requestOverride) {
    if (requestOverride != null && !requestOverride.isBlank()) {
      return requestOverride.trim();
    }
    return switch (channel) {
      case SMS -> properties.getTwilio().getLiveTestPhone();
      case WHATSAPP -> properties.getTwilio().getLiveTestWhatsapp();
      case EMAIL -> {
        String sg = properties.getSendGrid().getLiveTestEmail();
        yield (sg != null && !sg.isBlank()) ? sg : properties.getSmtp().getLiveTestEmail();
      }
      default -> null;
    };
  }

  private String calculateParametersHash(Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "EMPTY";
    }
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      parameters.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> {
            md.update(e.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (e.getValue() != null) {
              md.update(e.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
          });
      byte[] digest = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.substring(0, 16);
    } catch (Exception ex) {
      return "HASH_FALLBACK";
    }
  }

  private boolean isAuthorizedInternalQueueWorker() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof InternalAuthenticationToken internalAuth) {
      InternalServicePrincipal principal = (InternalServicePrincipal) internalAuth.getPrincipal();
      return principal != null && "COMMUNICATION_DISPATCH_QUEUE_WORKER".equalsIgnoreCase(principal.getServiceId());
    }
    return false;
  }

  private String classifyProviderException(Exception ex) {
    if (in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl.isAmbiguousTimeout(ex, null, ex != null ? ex.getMessage() : null)) {
      return "AMBIGUOUS_PROVIDER_TIMEOUT";
    }
    if (in.gov.sih.sih26135.communication.queue.DurableDispatchQueueServiceImpl.isDefiniteConnectionFailure(ex, null)) {
      return "PROVIDER_CONNECT_FAILURE";
    }
    if (ex instanceof org.springframework.web.client.RestClientResponseException rre) {
      return String.valueOf(rre.getStatusCode().value());
    }
    return "DISPATCH_ERROR";
  }

  private String resolveProviderName(CommunicationChannel channel) {
    if (channel == null) return "System";
    return switch (channel) {
      case SMS -> smsProvider.getProviderName();
      case WHATSAPP -> whatsAppProvider.getProviderName();
      case EMAIL -> emailProvider.getProviderName();
      default -> "System";
    };
  }
}
