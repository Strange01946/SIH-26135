package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an authenticated user exceeds the configured download rate limit
 * or concurrent export limit. Maps to HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends BaseException {

  private final long retryAfterSeconds;

  public RateLimitExceededException(String message, long retryAfterSeconds) {
    super(message, HttpStatus.TOO_MANY_REQUESTS, "EXPORT_RATE_LIMIT_EXCEEDED");
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public RateLimitExceededException(String message, String errorCode, long retryAfterSeconds) {
    super(message, HttpStatus.TOO_MANY_REQUESTS, errorCode);
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
