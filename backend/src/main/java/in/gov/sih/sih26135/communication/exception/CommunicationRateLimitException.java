package in.gov.sih.sih26135.communication.exception;

import in.gov.sih.sih26135.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an outbound notification request exceeds anti-spam or tenant throughput rate limits.
 */
public class CommunicationRateLimitException extends BaseException {

  private final long retryAfterSeconds;
  private final String rateLimitScope;

  public CommunicationRateLimitException(String message, String errorCode, long retryAfterSeconds, String rateLimitScope) {
    super(message, HttpStatus.TOO_MANY_REQUESTS, errorCode);
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    this.rateLimitScope = rateLimitScope;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  public String getRateLimitScope() {
    return rateLimitScope;
  }
}
