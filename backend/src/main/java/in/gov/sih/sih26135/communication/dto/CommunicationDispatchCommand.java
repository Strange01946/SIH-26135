package in.gov.sih.sih26135.communication.dto;

import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable command object representing an authorized application request
 * to dispatch a notification via {@link in.gov.sih.sih26135.communication.service.NotificationDispatcherService}.
 */
public record CommunicationDispatchCommand(
    Long traineeId,
    CommunicationChannel channel,
    CommunicationPurpose purpose,
    String templateCode,
    Map<String, String> templateParameters,
    Long initiatedByUserId,
    Long followupTaskId,
    Long surveyResponseId,
    boolean isRetry
) {

  public CommunicationDispatchCommand {
    if (templateParameters == null) {
      templateParameters = Collections.emptyMap();
    } else {
      templateParameters = Collections.unmodifiableMap(templateParameters);
    }
    if (purpose == null) {
      purpose = CommunicationPurpose.OPERATIONAL;
    }
  }

  /**
   * Backward-compatible 8-argument constructor defaulting isRetry to false.
   */
  public CommunicationDispatchCommand(
      Long traineeId,
      CommunicationChannel channel,
      CommunicationPurpose purpose,
      String templateCode,
      Map<String, String> templateParameters,
      Long initiatedByUserId,
      Long followupTaskId,
      Long surveyResponseId) {
    this(traineeId, channel, purpose, templateCode, templateParameters, initiatedByUserId, followupTaskId, surveyResponseId, false);
  }

  public CommunicationDispatchCommand asRetry() {
    return new CommunicationDispatchCommand(
        traineeId, channel, purpose, templateCode, templateParameters, initiatedByUserId, followupTaskId, surveyResponseId, true
    );
  }

  public static CommunicationDispatchCommand of(
      Long traineeId,
      CommunicationChannel channel,
      CommunicationPurpose purpose,
      String templateCode,
      Map<String, String> templateParameters,
      Long initiatedByUserId) {
    return new CommunicationDispatchCommand(
        traineeId,
        channel,
        purpose,
        templateCode,
        templateParameters,
        initiatedByUserId,
        null,
        null,
        false
    );
  }
}
