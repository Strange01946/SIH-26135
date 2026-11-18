package in.gov.sih.sih26135.communication.webhook;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Service contract for handling, reconciling, and persisting external communication
 * provider delivery receipts and status update webhooks.
 */
public interface CommunicationWebhookService {

  /**
   * Processes an incoming delivery receipt webhook from the specified provider.
   *
   * @param provider provider identifier ("twilio", "sendgrid", "mock")
   * @param request raw HTTP servlet request
   * @param bodyBytes raw request body bytes
   * @param formParams parsed form parameters (for URL-encoded POST)
   * @return reconciliation outcome
   */
  WebhookProcessingResult processWebhook(
      String provider,
      HttpServletRequest request,
      byte[] bodyBytes,
      Map<String, String> formParams
  );
}
