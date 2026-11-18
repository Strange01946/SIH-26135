package in.gov.sih.sih26135.communication.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Production implementation of {@link CommunicationWebhookService}.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Reconciliation against existing {@code communication_logs} by provider message ID.</li>
 *   <li>Out-of-order safety: DELIVERED or READ records cannot be downgraded to SENT or QUEUED.</li>
 *   <li>Duplicate delivery idempotency: duplicate events return success without corrupting state.</li>
 *   <li>DEMO/simulated record immutability: simulated entries cannot be marked DELIVERED by external webhooks.</li>
 *   <li>Statutory consent guardrails: BLOCKED_NO_CONSENT records are immutable to webhooks.</li>
 * </ul>
 */
@Service
public class CommunicationWebhookServiceImpl implements CommunicationWebhookService {

  private static final Logger log = LoggerFactory.getLogger(CommunicationWebhookServiceImpl.class);

  private final CommunicationLogRepository communicationLogRepository;
  private final RefCommunicationStatusRepository refCommunicationStatusRepository;
  private final ObjectMapper objectMapper;
  private final PlatformTransactionManager transactionManager;

  @Autowired
  public CommunicationWebhookServiceImpl(
      CommunicationLogRepository communicationLogRepository,
      RefCommunicationStatusRepository refCommunicationStatusRepository,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {
    this.communicationLogRepository = Objects.requireNonNull(communicationLogRepository, "CommunicationLogRepository is required");
    this.refCommunicationStatusRepository = Objects.requireNonNull(refCommunicationStatusRepository, "RefCommunicationStatusRepository is required");
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper is required");
    this.transactionManager = Objects.requireNonNull(transactionManager, "PlatformTransactionManager is required");
  }

  @Override
  public WebhookProcessingResult processWebhook(
      String provider,
      HttpServletRequest request,
      byte[] bodyBytes,
      Map<String, String> formParams) {

    if (provider == null || provider.isBlank()) {
      return WebhookProcessingResult.error("UNKNOWN", null, "PROVIDER_REQUIRED", "Provider parameter is required");
    }

    String normProvider = provider.trim().toLowerCase();
    String providerMessageId = null;
    String rawStatus = null;
    String errorCode = null;
    String errorMessage = null;

    // 1. Parse payload per provider format
    try {
      switch (normProvider) {
        case "twilio" -> {
          providerMessageId = extractParam(formParams, request, "MessageSid", "SmsSid");
          rawStatus = extractParam(formParams, request, "MessageStatus", "SmsStatus");
          errorCode = extractParam(formParams, request, "ErrorCode");
          errorMessage = extractParam(formParams, request, "ErrorMessage");
        }
        case "sendgrid" -> {
          if (bodyBytes != null && bodyBytes.length > 0) {
            JsonNode root = objectMapper.readTree(bodyBytes);
            if (root.isArray() && root.size() > 0) {
              JsonNode firstEvent = root.get(0);
              providerMessageId = firstEvent.path("sg_message_id").asText(null);
              rawStatus = firstEvent.path("event").asText(null);
              errorCode = firstEvent.path("status").asText(null);
              errorMessage = firstEvent.path("reason").asText(null);
            } else if (root.isObject()) {
              providerMessageId = root.path("sg_message_id").asText(null);
              rawStatus = root.path("event").asText(null);
              errorCode = root.path("status").asText(null);
              errorMessage = root.path("reason").asText(null);
            }
          }
        }
        case "mock", "test" -> {
          // Check JSON body first, then fallback to formParams
          if (bodyBytes != null && bodyBytes.length > 0) {
            try {
              JsonNode root = objectMapper.readTree(bodyBytes);
              providerMessageId = root.path("providerMessageId").asText(null);
              rawStatus = root.path("status").asText(null);
              errorCode = root.path("errorCode").asText(null);
              errorMessage = root.path("errorMessage").asText(null);
            } catch (Exception ignored) {
              // Not JSON; fall back to form params
            }
          }
          if (providerMessageId == null) {
            providerMessageId = extractParam(formParams, request, "providerMessageId", "MessageSid");
            rawStatus = extractParam(formParams, request, "status", "MessageStatus");
            errorCode = extractParam(formParams, request, "errorCode");
            errorMessage = extractParam(formParams, request, "errorMessage");
          }
        }
        default -> {
          return WebhookProcessingResult.error(normProvider, null, "UNSUPPORTED_PROVIDER", "Unsupported webhook provider: " + provider);
        }
      }
    } catch (Exception ex) {
      log.error("[CommunicationWebhook] Failed to parse {} webhook payload: {}", normProvider, ex.getMessage(), ex);
      return WebhookProcessingResult.error(normProvider, null, "PARSE_ERROR", "Failed to parse webhook payload: " + ex.getMessage());
    }

    if (providerMessageId == null || providerMessageId.isBlank()) {
      log.warn("[CommunicationWebhook] Webhook from {} missing provider message ID", normProvider);
      return WebhookProcessingResult.error(normProvider, null, "MISSING_MESSAGE_ID", "Provider message ID could not be identified in webhook payload");
    }

    final String finalProviderMsgId = providerMessageId.trim();
    final String finalRawStatus = rawStatus;
    final String finalErrorCode = errorCode;
    final String finalErrorMessage = errorMessage;

    // 2. Reconcile against communication_logs in an isolated transaction
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    return txTemplate.execute(status -> reconcileCommunicationLog(normProvider, finalProviderMsgId, finalRawStatus, finalErrorCode, finalErrorMessage));
  }

  private WebhookProcessingResult reconcileCommunicationLog(
      String provider,
      String providerMessageId,
      String rawStatus,
      String errorCode,
      String errorMessage) {

    Optional<CommunicationLog> logOpt = communicationLogRepository.findByProviderMessageId(providerMessageId);
    if (logOpt.isEmpty()) {
      log.warn("[CommunicationWebhook] No communication log found for provider message ID: {}", providerMessageId);
      return WebhookProcessingResult.notFound(provider, providerMessageId);
    }

    CommunicationLog commLog = logOpt.get();
    String currentStatusCode = commLog.getCommunicationStatus() != null
        ? commLog.getCommunicationStatus().getStatusCode()
        : "UNKNOWN";

    // Guardrail 1: DEMO simulated records cannot become DELIVERED via external webhooks
    if ("SIMULATED".equalsIgnoreCase(currentStatusCode)) {
      log.warn("[CommunicationWebhook] Attempt to reconcile DEMO simulated log {} via external webhook. Rejected.", commLog.getId());
      return WebhookProcessingResult.ignored(
          provider,
          providerMessageId,
          commLog.getId(),
          currentStatusCode,
          "SIMULATED_RECORD_IMMUTABLE",
          "DEMO simulated communication log records cannot be modified by external provider webhooks"
      );
    }

    // Guardrail 2: Statutory consent-blocked records cannot be modified by webhooks
    if ("BLOCKED_NO_CONSENT".equalsIgnoreCase(currentStatusCode)) {
      log.warn("[CommunicationWebhook] Attempt to update BLOCKED_NO_CONSENT log {}. Rejected.", commLog.getId());
      return WebhookProcessingResult.ignored(
          provider,
          providerMessageId,
          commLog.getId(),
          currentStatusCode,
          "BLOCKED_CONSENT_IMMUTABLE",
          "Consent-blocked communication log records are immutable to external provider webhooks"
      );
    }

    String mappedTargetStatus = mapToStandardStatus(provider, rawStatus);

    // Guardrail 3: Idempotent handling of duplicate delivery events
    if (currentStatusCode.equalsIgnoreCase(mappedTargetStatus)) {
      log.info("[CommunicationWebhook] Duplicate webhook for log {}: status already {}", commLog.getId(), currentStatusCode);
      return WebhookProcessingResult.ignored(
          provider,
          providerMessageId,
          commLog.getId(),
          currentStatusCode,
          "DUPLICATE_IGNORED",
          "Log status is already " + currentStatusCode
      );
    }

    // Guardrail 4: Out-of-order delivery protection
    if (isOutOfOrder(currentStatusCode, mappedTargetStatus)) {
      log.warn("[CommunicationWebhook] Out-of-order event for log {}: current={}, incoming={}. Downgrade rejected.",
          commLog.getId(), currentStatusCode, mappedTargetStatus);
      return WebhookProcessingResult.ignored(
          provider,
          providerMessageId,
          commLog.getId(),
          currentStatusCode,
          "OUT_OF_ORDER_IGNORED",
          "Cannot downgrade communication log status from " + currentStatusCode + " to " + mappedTargetStatus
      );
    }

    // Resolve target status entity
    RefCommunicationStatus targetStatusEntity = refCommunicationStatusRepository.findByStatusCode(mappedTargetStatus)
        .orElseGet(() -> refCommunicationStatusRepository.findByStatusCode("SENT")
            .orElseThrow(() -> new IllegalStateException("Status reference not found for " + mappedTargetStatus)));

    // Apply updates
    commLog.setCommunicationStatus(targetStatusEntity);
    commLog.setUpdatedAt(LocalDateTime.now());

    LocalDateTime now = LocalDateTime.now();
    switch (mappedTargetStatus) {
      case "DELIVERED" -> {
        commLog.setDeliveredAt(now);
      }
      case "READ" -> {
        commLog.setReadAt(now);
        if (commLog.getDeliveredAt() == null) {
          commLog.setDeliveredAt(now);
        }
      }
      case "SENT" -> {
        if (commLog.getSentAt() == null) {
          commLog.setSentAt(now);
        }
      }
      case "FAILED" -> {
        commLog.setFailedAt(now);
        String reason = (errorCode != null ? errorCode + ": " : "") + (errorMessage != null ? errorMessage : "Provider reported delivery failure");
        if (reason.length() > 250) {
          reason = reason.substring(0, 250);
        }
        commLog.setFailureReason(reason);
      }
      default -> {
        // QUEUED or ATTEMPTED
      }
    }

    communicationLogRepository.save(commLog);
    log.info("[CommunicationWebhook] Successfully reconciled log {}: {} -> {}",
        commLog.getId(), currentStatusCode, mappedTargetStatus);

    return WebhookProcessingResult.updated(provider, providerMessageId, commLog.getId(), currentStatusCode, mappedTargetStatus);
  }

  private boolean isOutOfOrder(String currentStatus, String incomingStatus) {
    int currentRank = getStatusRank(currentStatus);
    int incomingRank = getStatusRank(incomingStatus);

    // If current is terminal success (DELIVERED or READ), cannot revert to SENT or QUEUED or FAILED
    if (currentRank >= 40 && incomingRank < 40) {
      return true;
    }

    // General progression: cannot downgrade rank
    return incomingRank < currentRank;
  }

  private int getStatusRank(String status) {
    if (status == null) {
      return 0;
    }
    return switch (status.toUpperCase()) {
      case "QUEUED" -> 10;
      case "ATTEMPTED" -> 20;
      case "SENT" -> 30;
      case "DELIVERED" -> 40;
      case "READ" -> 50;
      case "FAILED" -> 15; // Below SENT so an already sent/delivered message doesn't easily revert to failed by delayed webhooks
      default -> 0;
    };
  }

  private String mapToStandardStatus(String provider, String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return "SENT";
    }
    String s = rawStatus.trim().toLowerCase();
    return switch (s) {
      case "delivered" -> "DELIVERED";
      case "read", "open", "click" -> "READ";
      case "sent", "processed", "deferred" -> "SENT";
      case "queued", "accepted", "sending" -> "QUEUED";
      case "failed", "undelivered", "bounce", "dropped", "blocked" -> "FAILED";
      default -> "SENT";
    };
  }

  private String extractParam(Map<String, String> formParams, HttpServletRequest request, String... candidateNames) {
    if (formParams != null) {
      for (String name : candidateNames) {
        String val = formParams.get(name);
        if (val != null && !val.isBlank()) {
          return val;
        }
      }
    }
    if (request != null) {
      for (String name : candidateNames) {
        String val = request.getParameter(name);
        if (val != null && !val.isBlank()) {
          return val;
        }
      }
    }
    return null;
  }
}
