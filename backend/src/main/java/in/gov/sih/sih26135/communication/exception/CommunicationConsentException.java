package in.gov.sih.sih26135.communication.exception;

import in.gov.sih.sih26135.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an outbound communication dispatch is rejected due to missing,
 * invalid, expired, or withdrawn/revoked statutory communication consent.
 *
 * <p>Enforces:
 * <ul>
 *   <li>HTTP 403 Forbidden status code.</li>
 *   <li>Standardized error code {@code CONSENT_WITHDRAWN} for revoked consent.</li>
 *   <li>Standardized error code {@code CONSENT_NOT_GRANTED} for missing, expired, or inactive consent.</li>
 * </ul>
 */
public class CommunicationConsentException extends BaseException {

  public CommunicationConsentException(String message, String errorCode) {
    super(message, HttpStatus.FORBIDDEN, errorCode);
  }

  public CommunicationConsentException(String message, Throwable cause, String errorCode) {
    super(message, cause, HttpStatus.FORBIDDEN, errorCode);
  }
}
