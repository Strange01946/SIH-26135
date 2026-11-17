package in.gov.sih.sih26135.communication.provider;

import in.gov.sih.sih26135.communication.dto.ProviderDeliveryResult;

/**
 * Outbound Email delivery provider interface.
 */
public interface EmailCommunicationProvider {

  /**
   * Dispatches an outbound email to an authorized recipient.
   *
   * @param recipientEmail target recipient email address
   * @param subject email subject line
   * @param bodyText plain-text message body
   * @param bodyHtml HTML-formatted message body
   * @return delivery result with provider message identifier and acceptance status
   */
  ProviderDeliveryResult sendEmail(String recipientEmail, String subject, String bodyText, String bodyHtml);

  /**
   * Returns human-readable provider name.
   */
  String getProviderName();
}
