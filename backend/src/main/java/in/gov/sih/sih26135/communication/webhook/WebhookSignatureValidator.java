package in.gov.sih.sih26135.communication.webhook;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Strategy interface for verifying incoming HTTP delivery webhook signatures
 * from external communication providers.
 */
public interface WebhookSignatureValidator {

  /**
   * Identifies the provider code supported by this validator (e.g. "twilio", "sendgrid", "mock").
   */
  String getSupportedProvider();

  /**
   * Validates the cryptographic or token signature of an incoming webhook request.
   *
   * @param request HTTP servlet request containing headers and parameters
   * @param bodyBytes raw HTTP request body bytes
   * @return true if authentic; false if invalid, unauthorized, or expired
   */
  boolean validateSignature(HttpServletRequest request, byte[] bodyBytes);
}
