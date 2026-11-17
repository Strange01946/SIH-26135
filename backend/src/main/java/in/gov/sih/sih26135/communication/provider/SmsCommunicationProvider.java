package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;

/**
 * Outbound SMS delivery provider interface.
 */
public interface SmsCommunicationProvider {

  /**
   * Dispatches an outbound SMS to an authorized recipient.
   *
   * @param recipientPhone target recipient phone number in E.164 format (e.g. +919876543210)
   * @param messageText plain-text message body
   * @param templateCode approved template identifier
   * @return delivery result with provider message identifier and acceptance status
   */
  ProviderDeliveryResult sendSms(String recipientPhone, String messageText, String templateCode);

  /**
   * Returns human-readable provider name.
   */
  String getProviderName();
}
