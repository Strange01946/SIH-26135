package in.gov.sih.sih26135.communication.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import jakarta.servlet.http.HttpServletRequest;
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
 *   <li>Channel-provider matching (Twilio strictly for SMS/WhatsApp, SendGrid strictly for Email).</li>
 *   <li>Out-of-order safety: DELIVERED or READ records cannot be downgraded to SENT, QUEUED, or FAILED.</li>
 *   <li>Delivery failure progression: FAILED can be recorded after SENT.</li>
 *   <li>Duplicate delivery idempotency: duplicate events return success without corrupting state.</li>
 *   <li>DEMO/simulated record immutability: simulated entries cannot be marked DELIVERED by external webhooks.</li>
 *   <li>Statutory consent guardrails: BLOCKED_NO_CONSENT records are immutable to webhooks.</li>
 *   <li>Strict status mapping: missing or unrecognized provider status rejects with UNKNOWN_WEBHOOK_STATUS (never manufactures SENT).</li>
 *   <li>Full SendGrid JSON event array batch processing.</li>
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
            if (root.isArray()) {
              if (root.isEmpty()) {
                return WebhookProcessingResult.error(normProvider, null, "EMPTY_PAYLOAD", "SendGrid webhook payload is empty array");
              }
              if (root.size() == 1) {
                JsonNode first = root.get(0);
                providerMessageId = extractSendGridMessageId(first);
                rawStatus = first.path("event").asText(null);
                errorCode = first.path("status").asText(null);
                errorMessage = first.path("reason").asText(null);
              } else {
                // Multi-event array processing: iterate over all events
                return processSendGridEventArray(normProvider, root);
              }
            } else if (root.isObject()) {
              providerMessageId = extractSendGridMessageId(root);
              rawStatus = root.path("event").asText(null);
              errorCode = root.path("status").asText(null);
              errorMessage = root.path("reason").asText(null);
            }
          }
        }
        case "mock", "test" -> {
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

  private WebhookProcessingResult processSendGridEventArray(String provider, JsonNode root) {
    int updatedCount = 0;
    WebhookProcessingResult lastResult = null;
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    for (JsonNode eventNode : root) {
      String msgId = extractSendGridMessageId(eventNode);
      String evStatus = eventNode.path("event").asText(null);
      String evCode = eventNode.path("status").asText(null);
      String evMsg = eventNode.path("reason").asText(null);

      if (msgId != null && !msgId.isBlank()) {
        final String fMsgId = msgId.trim();
        final String fStatus = evStatus;
        final String fCode = evCode;
        final String fMsg = evMsg;
        WebhookProcessingResult itemRes = txTemplate.execute(status ->
            reconcileCommunicationLog(provider, fMsgId, fStatus, fCode, fMsg));
        if (itemRes != null && itemRes.accepted()) {
          updatedCount++;
          lastResult = itemRes;
        }
      }
    }

    return new WebhookProcessingResult(
        true,
        provider,
        lastResult != null ? lastResult.providerMessageId() : "BATCH",
        lastResult != null ? lastResult.communicationLogId() : null,
        lastResult != null ? lastResult.previousStatus() : null,
        lastResult != null ? lastResult.updatedStatus() : "BATCH_COMPLETED",
        "BATCH_PROCESSED",
        "Successfully reconciled " + updatedCount + " of " + root.size() + " SendGrid events"
    );
  }

  private String extractSendGridMessageId(JsonNode node) {
    if (node == null) {
      return null;
    }
    String msgId = node.path("sg_message_id").asText(null);
    if (msgId == null || msgId.isBlank()) {
      msgId = node.path("message_id").asText(null);
    }
    return msgId;
  }

  private WebhookProcessingResult reconcileCommunicationLog(
      String provider,
      String providerMessageId,
      String rawStatus,
      String errorCode,
      String errorMessage) {

    Optional<CommunicationLog> logOpt = communicationLogRepository.findByProviderMessageId(providerMessageId);
    if (logOpt.isEmpty() && providerMessageId.contains(".")) {
      // SendGrid often appends filter strings after a period: e.g. <msgId>.<filter>
      String prefixId = providerMessageId.substring(0, providerMessageId.indexOf('.'));
      logOpt = communicationLogRepository.findByProviderMessageId(prefixId);
    }

    if (logOpt.isEmpty()) {
      log.warn("[CommunicationWebhook] No communication log found for provider message ID: {}", providerMessageId);
      return WebhookProcessingResult.notFound(provider, providerMessageId);
    }

    CommunicationLog commLog = logOpt.get();
    if (commLog.getProviderMessageId().startsWith(MockWebhookSignatureValidator.DEMO_RECEIPT_PREFIX)
        && !"mock".equals(provider) && !"test".equals(provider)) {
      return WebhookProcessingResult.error(provider, providerMessageId, "PROVIDER_CHANNEL_MISMATCH",
          "Demo receipt fixtures can only be reconciled by the mock provider");
    }
    String currentStatusCode = commLog.getCommunicationStatus() != null
        ? commLog.getCommunicationStatus().getStatusCode()
        : "UNKNOWN";

    // Guardrail 1: Channel-Provider boundary check
    RefCommunicationChannel channel = commLog.getCommunicationChannel();
    if (channel != null && channel.getChannelCode() != null) {
      String channelCode = channel.getChannelCode().trim().toUpperCase();
      if ("twilio".equalsIgnoreCase(provider)) {
        if (!"SMS".equals(channelCode) && !"WHATSAPP".equals(channelCode)) {
          log.warn("[CommunicationWebhook] Provider channel mismatch: twilio webhook received for log {} with channel {}", commLog.getId(), channelCode);
          return WebhookProcessingResult.error(provider, providerMessageId, "PROVIDER_CHANNEL_MISMATCH",
              "Authenticated provider twilio cannot reconcile non-SMS/WhatsApp channel: " + channelCode);
        }
      } else if ("sendgrid".equalsIgnoreCase(provider)) {
        if (!"EMAIL".equals(channelCode)) {
          log.warn("[CommunicationWebhook] Provider channel mismatch: sendgrid webhook received for log {} with channel {}", commLog.getId(), channelCode);
          return WebhookProcessingResult.error(provider, providerMessageId, "PROVIDER_CHANNEL_MISMATCH",
              "Authenticated provider sendgrid cannot reconcile non-EMAIL channel: " + channelCode);
        }
      }
    }

    // Guardrail 2: DEMO simulated records cannot become DELIVERED via external webhooks
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

    // Guardrail 3: Statutory consent-blocked records cannot be modified by webhooks
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

    // Guardrail 4: Strict status mapping (reject unknown or missing status; NEVER manufacture SENT)
    String mappedTargetStatus = mapToStandardStatus(provider, rawStatus);
    if (mappedTargetStatus == null) {
      log.warn("[CommunicationWebhook] Unknown or missing status '{}' from provider {}. Update rejected.", rawStatus, provider);
      return WebhookProcessingResult.error(
          provider,
          providerMessageId,
          "UNKNOWN_WEBHOOK_STATUS",
          "Unknown or missing provider delivery status: " + rawStatus
      );
    }

    // Guardrail 5: Idempotent handling of duplicate delivery events
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

    // Guardrail 6: Out-of-order delivery protection
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

    // Resolve target status entity (Never substitute SENT if missing!)
    RefCommunicationStatus targetStatusEntity = refCommunicationStatusRepository.findByStatusCode(mappedTargetStatus)
        .orElseThrow(() -> new IllegalStateException("Required reference communication status not found in catalog: " + mappedTargetStatus));

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
    if (currentStatus == null || incomingStatus == null) {
      return false;
    }
    String cur = currentStatus.toUpperCase();
    String inc = incomingStatus.toUpperCase();

    // Terminal success states (DELIVERED or READ) cannot be downgraded or transitioned to FAILED
    if ("DELIVERED".equals(cur) || "READ".equals(cur)) {
      if ("READ".equals(cur)) {
        return !"READ".equals(inc);
      }
      // cur is DELIVERED: only progression to READ is permitted
      return !"READ".equals(inc);
    }

    // Incoming FAILED is a valid transition from QUEUED, ATTEMPTED, or SENT
    if ("FAILED".equals(inc)) {
      return "FAILED".equals(cur); // Duplicate if already FAILED
    }

    // If current is already FAILED, it cannot regress to QUEUED, ATTEMPTED, or SENT
    if ("FAILED".equals(cur)) {
      return true;
    }

    // Monotonic progression: QUEUED (10) -> ATTEMPTED (20) -> SENT (30) -> DELIVERED (40) -> READ (50)
    int currentRank = getStatusRank(cur);
    int incomingRank = getStatusRank(inc);
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
      default -> 0;
    };
  }

  private String mapToStandardStatus(String provider, String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return null;
    }
    String s = rawStatus.trim().toLowerCase();
    return switch (s) {
      case "delivered" -> "DELIVERED";
      case "read", "open", "click" -> "READ";
      case "sent", "processed" -> "SENT";
      case "queued", "accepted", "sending" -> "QUEUED";
      case "failed", "undelivered", "bounce", "dropped", "blocked" -> "FAILED";
      default -> null; // Strictly reject unknown status (never default to SENT)
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
