package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;

/**
 * Opt-in request contract for executing a controlled live smoke-test against real communication providers.
 */
public record LiveSmokeTestRequest(
    CommunicationChannel channel,
    Long targetTraineeId,
    String recipientOverride,
    String customMessage
) {
}
