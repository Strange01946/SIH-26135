package in.gov.sih.sih26135.dto.request;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Collections;
import java.util.Map;

/**
 * Request payload for dispatching an outbound notification via
 * {@code POST /api/v1/communication-logs/dispatch}.
 *
 * <p>Notice: Client requests cannot choose delivery mode (DEMO vs LIVE).
 * Delivery mode is strictly determined by trusted server-side configuration.
 */
public record DispatchNotificationRequest(
    @NotNull(message = "Trainee ID is required")
    @Positive(message = "Trainee ID must be a positive number")
    Long traineeId,

    @NotNull(message = "Communication channel is required (SMS, WHATSAPP, EMAIL)")
    CommunicationChannel channel,

    CommunicationPurpose purpose,

    @NotBlank(message = "Template code is required")
    String templateCode,

    Map<String, String> templateParameters
) {
  public DispatchNotificationRequest {
    if (purpose == null) {
      purpose = CommunicationPurpose.OPERATIONAL;
    }
    if (templateParameters == null) {
      templateParameters = Collections.emptyMap();
    }
  }
}
