package in.gov.sih.sih26135.communication.service;

import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestResult;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;

/**
 * Authoritative application-level notification dispatch interface.
 *
 * <p>Orchestrates statutory DPDP Act consent validation, authoritative recipient resolution,
 * template rendering, multi-channel provider dispatch (Twilio SMS, Twilio WhatsApp, SendGrid/SMTP Email),
 * and immutable communication logging.
 */
public interface NotificationDispatcherService {

  /**
   * Dispatches an outbound notification according to the provided command specifications.
   *
   * @param command dispatch command containing traineeId, channel, purpose, templateCode, and parameters
   * @return sanitized notification dispatch response containing communication log ID and provider message ID
   */
  NotificationDispatchResponse dispatchNotification(CommunicationDispatchCommand command);

  /**
   * Dispatches a notification triggered by a domain {@link CommunicationTriggerEvent}.
   * Resolves the unmasked recipient from the authoritative database record using the event's entity ID.
   *
   * @param event strongly typed domain communication trigger event
   * @return sanitized notification dispatch response
   */
  NotificationDispatchResponse dispatchFromTriggerEvent(CommunicationTriggerEvent event);

  /**
   * Executes a controlled live smoke-test workflow for authorized administrative/developer verification.
   *
   * @param request smoke test request specifying channel and optional recipient override
   * @return delivery proof result including provider Message SID and verified log entry
   */
  LiveSmokeTestResult executeLiveSmokeTest(LiveSmokeTestRequest request);
}
