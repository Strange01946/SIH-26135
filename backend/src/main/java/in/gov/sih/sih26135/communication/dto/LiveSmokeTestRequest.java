package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;

/**
 * Opt-in request contract for executing a controlled live smoke-test against real communication providers.
 * Supports both canonical field names (targetTraineeId, customMessage) and documentation aliases (traineeId, testPayload).
 */
public record LiveSmokeTestRequest(
    CommunicationChannel channel,
    Long targetTraineeId,
    String recipientOverride,
    String customMessage
) {

  public Long traineeId() {
    return targetTraineeId;
  }

  public String testPayload() {
    return customMessage;
  }

  public static LiveSmokeTestRequest of(
      CommunicationChannel channel,
      Long targetTraineeId,
      String recipientOverride,
      String customMessage
  ) {
    return new LiveSmokeTestRequest(channel, targetTraineeId, recipientOverride, customMessage);
  }
}
