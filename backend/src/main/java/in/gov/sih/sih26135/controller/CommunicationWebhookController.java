package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.webhook.CommunicationWebhookService;
import in.gov.sih.sih26135.communication.webhook.WebhookProcessingResult;
import in.gov.sih.sih26135.communication.webhook.WebhookSignatureValidator;
import in.gov.sih.sih26135.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicly reachable, signature-authenticated webhook ingestion endpoint
 * for external delivery receipts and status notifications.
 */
@RestController
@RequestMapping("/api/v1/communication/webhooks")
public class CommunicationWebhookController {

  private static final Logger log = LoggerFactory.getLogger(CommunicationWebhookController.class);

  private final CommunicationWebhookService webhookService;
  private final Map<String, WebhookSignatureValidator> validators = new HashMap<>();

  @Autowired
  public CommunicationWebhookController(
      CommunicationWebhookService webhookService,
      List<WebhookSignatureValidator> validatorList) {
    this.webhookService = webhookService;
    if (validatorList != null) {
      for (WebhookSignatureValidator validator : validatorList) {
        this.validators.put(validator.getSupportedProvider().toLowerCase(), validator);
      }
    }
  }

  /**
   * Ingests delivery receipts from external providers (e.g., Twilio SMS/WhatsApp, SendGrid Email, Mock).
   */
  @PostMapping(value = "/{provider}", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
  public ResponseEntity<ApiResponse<WebhookProcessingResult>> handleProviderWebhook(
      @PathVariable("provider") String provider,
      @RequestParam(required = false) Map<String, String> formParams,
      HttpServletRequest request) {

    String requestPath = request != null ? request.getRequestURI() : null;

    if (provider == null || provider.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("PROVIDER_REQUIRED", "Provider path variable is required", requestPath));
    }

    String normProvider = provider.trim().toLowerCase();
    WebhookSignatureValidator validator = validators.get(normProvider);
    if (validator == null && ("test".equals(normProvider) || "mock".equals(normProvider))) {
      validator = validators.get("mock");
    }

    if (validator == null) {
      log.warn("[WebhookController] Unsupported provider: {}", normProvider);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("UNSUPPORTED_PROVIDER", "No validator registered for provider: " + provider, requestPath));
    }

    // 1. Read request body bytes safely
    byte[] bodyBytes = readBodyBytesSafely(request);

    // 2. Cryptographic signature validation
    boolean isAuthentic = validator.validateSignature(request, bodyBytes);
    if (!isAuthentic) {
      log.warn("[WebhookController] Rejected invalid webhook signature for provider {} from IP {}",
          normProvider, request.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("INVALID_WEBHOOK_SIGNATURE", "Provider signature verification failed", requestPath));
    }

    // 3. Process delivery reconciliation
    WebhookProcessingResult result = webhookService.processWebhook(normProvider, request, bodyBytes, formParams);

    if (!result.accepted()) {
      if ("MESSAGE_NOT_FOUND".equalsIgnoreCase(result.resolutionCode())) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.ok(result));
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.ok(result));
    }

    return ResponseEntity.ok(ApiResponse.ok(result));
  }

  private byte[] readBodyBytesSafely(HttpServletRequest request) {
    if (request == null) {
      return new byte[0];
    }
    try (InputStream in = request.getInputStream()) {
      return in.readAllBytes();
    } catch (Exception ex) {
      log.debug("[WebhookController] Unable to read body bytes from request: {}", ex.getMessage());
      return new byte[0];
    }
  }
}
