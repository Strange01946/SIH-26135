package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Deterministic test-only mock SMS provider.
 * Guarantees zero external network calls during automated Maven test battery.
 */
@Component
public class MockSmsProvider implements SmsCommunicationProvider {

  public record DispatchedSms(
      String recipientPhone,
      String messageText,
      String templateCode,
      String messageSid,
      LocalDateTime timestamp
  ) {
  }

  private final List<DispatchedSms> dispatchedMessages = new CopyOnWriteArrayList<>();
  private boolean simulateFailure = false;
  private String failureErrorCode = "SIMULATED_SMS_ERROR";
  private String failureErrorMessage = "Simulated mock SMS delivery failure";
  private String customStatusCode = null;
  private Boolean customAccepted = null;

  @Override
  public ProviderDeliveryResult sendSms(String recipientPhone, String messageText, String templateCode) {
    if (customStatusCode != null) {
      boolean accepted = customAccepted != null ? customAccepted : true;
      String sid = "SM_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
      return new ProviderDeliveryResult(
          CommunicationChannel.SMS,
          getProviderName(),
          sid,
          customStatusCode,
          accepted,
          "{\"status\":\"" + customStatusCode + "\"}",
          accepted ? null : failureErrorCode,
          failureErrorMessage,
          LocalDateTime.now()
      );
    }

    if (simulateFailure) {
      return ProviderDeliveryResult.failure(
          CommunicationChannel.SMS,
          getProviderName(),
          null,
          failureErrorCode,
          failureErrorMessage,
          "{\"status\":\"failed\",\"error\":\"" + failureErrorMessage + "\"}"
      );
    }

    String sid = "SM_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    dispatchedMessages.add(new DispatchedSms(recipientPhone, messageText, templateCode, sid, LocalDateTime.now()));

    return ProviderDeliveryResult.success(
        CommunicationChannel.SMS,
        getProviderName(),
        sid,
        "SENT",
        "{\"sid\":\"" + sid + "\",\"status\":\"sent\",\"channel\":\"SMS\"}"
    );
  }

  @Override
  public String getProviderName() {
    return "MockSmsProvider";
  }

  public List<DispatchedSms> getDispatchedMessages() {
    return Collections.unmodifiableList(new ArrayList<>(dispatchedMessages));
  }

  public DispatchedSms getLastMessage() {
    if (dispatchedMessages.isEmpty()) {
      return null;
    }
    return dispatchedMessages.get(dispatchedMessages.size() - 1);
  }

  public void clear() {
    dispatchedMessages.clear();
    simulateFailure = false;
    customStatusCode = null;
    customAccepted = null;
  }

  public void setSimulateFailure(boolean simulateFailure) {
    this.simulateFailure = simulateFailure;
  }

  public void setFailureDetails(String errorCode, String errorMessage) {
    this.failureErrorCode = errorCode;
    this.failureErrorMessage = errorMessage;
  }

  public void setCustomResult(String customStatusCode, Boolean customAccepted, String failureErrorMessage) {
    this.customStatusCode = customStatusCode;
    this.customAccepted = customAccepted;
    this.failureErrorMessage = failureErrorMessage;
  }
}
