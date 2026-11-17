package in.gov.sih.sih26135.communication.consent;

/**
 * Authoritative boundary interface for validating statutory candidate communication consent
 * under the Digital Personal Data Protection (DPDP) Act.
 */
public interface CommunicationConsentValidator {

  /**
   * Validates both channel-specific consent (SMS, WhatsApp, Email) and purpose-specific consent
   * (e.g. employment follow-up) for a candidate prior to external notification dispatch.
   *
   * @param traineeId unique identifier of the candidate
   * @param channelCode communication channel code (SMS, WHATSAPP, EMAIL)
   * @param purposeCode communication purpose code (FOLLOWUP_REMINDER, OPERATIONAL, etc.)
   * @throws in.gov.sih.sih26135.communication.exception.CommunicationConsentException if consent is missing, expired, or withdrawn
   */
  void validateCommunicationConsent(Long traineeId, String channelCode, String purposeCode);

  /**
   * Validates a specific named consent code for a candidate.
   *
   * @param traineeId unique identifier of the candidate
   * @param consentCode statutory consent code (e.g., COMMUNICATION_SMS, COMMUNICATION_EMAIL, COMMUNICATION_WHATSAPP)
   * @throws in.gov.sih.sih26135.communication.exception.CommunicationConsentException if consent is missing, expired, or withdrawn
   */
  void validateConsentRecord(Long traineeId, String consentCode);
}
