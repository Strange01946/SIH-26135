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
 * Deterministic test-only mock Email provider.
 * Guarantees zero external network calls during automated Maven test battery.
 */
@Component
public class MockEmailProvider implements EmailCommunicationProvider {

  public record DispatchedEmail(
      String recipientEmail,
      String subject,
      String bodyText,
      String bodyHtml,
      String messageId,
      LocalDateTime timestamp
  ) {
  }

  private final List<DispatchedEmail> dispatchedEmails = new CopyOnWriteArrayList<>();
  private boolean simulateFailure = false;
  private String failureErrorCode = "SIMULATED_EMAIL_ERROR";
  private String failureErrorMessage = "Simulated mock Email delivery failure";

  @Override
  public ProviderDeliveryResult sendEmail(String recipientEmail, String subject, String bodyText, String bodyHtml) {
    if (simulateFailure) {
      return ProviderDeliveryResult.failure(
          CommunicationChannel.EMAIL,
          getProviderName(),
          null,
          failureErrorCode,
          failureErrorMessage,
          "{\"status\":\"failed\",\"error\":\"" + failureErrorMessage + "\"}"
      );
    }

    String messageId = "EM_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    dispatchedEmails.add(new DispatchedEmail(recipientEmail, subject, bodyText, bodyHtml, messageId, LocalDateTime.now()));

    return ProviderDeliveryResult.success(
        CommunicationChannel.EMAIL,
        getProviderName(),
        messageId,
        "SENT",
        "{\"messageId\":\"" + messageId + "\",\"status\":\"sent\",\"channel\":\"EMAIL\"}"
    );
  }

  @Override
  public String getProviderName() {
    return "MockEmailProvider";
  }

  public List<DispatchedEmail> getDispatchedEmails() {
    return Collections.unmodifiableList(new ArrayList<>(dispatchedEmails));
  }

  public DispatchedEmail getLastEmail() {
    if (dispatchedEmails.isEmpty()) {
      return null;
    }
    return dispatchedEmails.get(dispatchedEmails.size() - 1);
  }

  public void clear() {
    dispatchedEmails.clear();
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
