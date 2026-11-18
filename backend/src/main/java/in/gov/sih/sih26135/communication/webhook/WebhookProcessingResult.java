package in.gov.sih.sih26135.communication.webhook;

/**
 * Result contract returned after processing and reconciling an incoming provider delivery webhook.
 */
public record WebhookProcessingResult(
    boolean accepted,
    String provider,
    String providerMessageId,
    Long communicationLogId,
    String previousStatus,
    String updatedStatus,
    String resolutionCode,
    String explanation
) {

  public static WebhookProcessingResult updated(
      String provider,
      String providerMessageId,
      Long communicationLogId,
      String previousStatus,
      String updatedStatus) {
    return new WebhookProcessingResult(
        true,
        provider,
        providerMessageId,
        communicationLogId,
        previousStatus,
        updatedStatus,
        "STATUS_UPDATED",
        "Communication log status successfully updated to " + updatedStatus
    );
  }

  public static WebhookProcessingResult ignored(
      String provider,
      String providerMessageId,
      Long communicationLogId,
      String currentStatus,
      String resolutionCode,
      String explanation) {
    return new WebhookProcessingResult(
        true,
        provider,
        providerMessageId,
        communicationLogId,
        currentStatus,
        currentStatus,
        resolutionCode,
        explanation
    );
  }

  public static WebhookProcessingResult notFound(String provider, String providerMessageId) {
    return new WebhookProcessingResult(
        false,
        provider,
        providerMessageId,
        null,
        null,
        null,
        "MESSAGE_NOT_FOUND",
        "No matching communication log found for provider message ID: " + providerMessageId
    );
  }

  public static WebhookProcessingResult error(String provider, String providerMessageId, String errorCode, String explanation) {
    return new WebhookProcessingResult(
        false,
        provider,
        providerMessageId,
        null,
        null,
        null,
        errorCode,
        explanation
    );
  }
}
