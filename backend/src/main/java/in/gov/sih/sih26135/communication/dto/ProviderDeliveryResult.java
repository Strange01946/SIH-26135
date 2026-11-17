package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import java.time.LocalDateTime;

/**
 * Cohesive delivery result returned by concrete communication providers
 * (Twilio SMS, Twilio WhatsApp, SendGrid Email, SMTP Email, or Test Mocks).
 */
public record ProviderDeliveryResult(
    CommunicationChannel channel,
    String providerName,
    String providerMessageId,
    String statusCode,
    boolean accepted,
    String rawResponse,
    String errorCode,
    String errorMessage,
    LocalDateTime dispatchedAt
) {

  public static ProviderDeliveryResult success(
      CommunicationChannel channel,
      String providerName,
      String providerMessageId,
      String statusCode,
      String rawResponse) {
    return new ProviderDeliveryResult(
        channel,
        providerName,
        providerMessageId,
        statusCode != null ? statusCode : "SENT",
        true,
        rawResponse,
        null,
        null,
        LocalDateTime.now()
    );
  }

  public static ProviderDeliveryResult failure(
      CommunicationChannel channel,
      String providerName,
      String providerMessageId,
      String errorCode,
      String errorMessage,
      String rawResponse) {
    return new ProviderDeliveryResult(
        channel,
        providerName,
        providerMessageId,
        "FAILED",
        false,
        rawResponse,
        errorCode,
        errorMessage,
        LocalDateTime.now()
    );
  }
}
