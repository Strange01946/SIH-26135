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
 * Deterministic test-only mock WhatsApp provider.
 * Guarantees zero external network calls during automated Maven test battery.
 */
@Component
public class MockWhatsAppProvider implements WhatsAppCommunicationProvider {

  public record DispatchedWhatsApp(
      String recipientPhone,
      String messageText,
      String templateCode,
      String messageSid,
      LocalDateTime timestamp
  ) {
  }

  private final List<DispatchedWhatsApp> dispatchedMessages = new CopyOnWriteArrayList<>();
  private boolean simulateFailure = false;
  private String failureErrorCode = "SIMULATED_WHATSAPP_ERROR";
  private String failureErrorMessage = "Simulated mock WhatsApp delivery failure";

  @Override
  public ProviderDeliveryResult sendWhatsApp(String recipientPhone, String messageText, String templateCode) {
    if (simulateFailure) {
      return ProviderDeliveryResult.failure(
          CommunicationChannel.WHATSAPP,
          getProviderName(),
          null,
          failureErrorCode,
          failureErrorMessage,
          "{\"status\":\"failed\",\"error\":\"" + failureErrorMessage + "\"}"
      );
    }

    String sid = "WA_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    dispatchedMessages.add(new DispatchedWhatsApp(recipientPhone, messageText, templateCode, sid, LocalDateTime.now()));

    return ProviderDeliveryResult.success(
        CommunicationChannel.WHATSAPP,
        getProviderName(),
        sid,
        "SIMULATED",
        "{\"sid\":\"" + sid + "\",\"status\":\"simulated\",\"channel\":\"WHATSAPP\"}"
    );
  }

  @Override
  public String getProviderName() {
    return "MockWhatsAppProvider";
  }

  public List<DispatchedWhatsApp> getDispatchedMessages() {
    return Collections.unmodifiableList(new ArrayList<>(dispatchedMessages));
  }

  public DispatchedWhatsApp getLastMessage() {
    if (dispatchedMessages.isEmpty()) {
      return null;
    }
    return dispatchedMessages.get(dispatchedMessages.size() - 1);
  }

  public void clear() {
    dispatchedMessages.clear();
    simulateFailure = false;
  }

  public void setSimulateFailure(boolean simulateFailure) {
    this.simulateFailure = simulateFailure;
  }

  public void setFailureDetails(String errorCode, String errorMessage) {
    this.failureErrorCode = errorCode;
    this.failureErrorMessage = errorMessage;
  }
}
