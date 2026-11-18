package in.gov.sih.sih26135.communication.ratelimit;

import in.gov.sih.sih26135.communication.exception.CommunicationRateLimitException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;

/**
 * Concurrency-safe, persistent rate limiter enforcing trainee anti-spam quotas,
 * training provider throughput limits, and rapid deduplication protection.
 */
public interface CommunicationRateLimiter {

  /**
   * Default Policy Constants
   */
  int DEFAULT_MAX_SMS_PER_TRAINEE_24H = 3;
  int DEFAULT_MAX_EMAIL_PER_TRAINEE_24H = 5;
  int DEFAULT_MAX_WHATSAPP_PER_TRAINEE_24H = 3;
  int DEFAULT_MAX_PROVIDER_REQUESTS_60S = 100;
  int DEFAULT_DEDUPLICATION_WINDOW_SECONDS = 5;

  /**
   * Atomically checks and reserves rate limit quotas for a dispatch attempt.
   *
   * @param traineeId target trainee ID
   * @param channel communication channel (SMS, WHATSAPP, EMAIL)
   * @param authorizedProviderId authoritative training provider ID, or null if nationwide/unassociated
   * @param templateCode template code being dispatched
   * @param parametersHash hash of template parameters for deduplication
   * @throws CommunicationRateLimitException if trainee anti-spam, provider burst limit, or dedup check fails
   */
  default void checkAndReserveQuota(
      Long traineeId,
      CommunicationChannel channel,
      Long authorizedProviderId,
      String templateCode,
      String parametersHash) {
    checkAndReserveQuota(traineeId, channel, authorizedProviderId, templateCode, parametersHash, false);
  }

  /**
   * Atomically checks and reserves rate limit quotas with retry awareness.
   *
   * <p>Retry quota policy:
   * <ul>
   *   <li>When {@code isRetry == true}: Trainee deduplication and trainee 24h anti-spam quota are not re-penalized.</li>
   *   <li>Provider 60s burst quota (100 req / 60s) is ALWAYS enforced and reserved to protect external provider capacity.</li>
   * </ul>
   *
   * @param traineeId target trainee ID
   * @param channel communication channel
   * @param authorizedProviderId authoritative training provider ID
   * @param templateCode template code
   * @param parametersHash parameters hash
   * @param isRetry true if this execution is an automatic retry attempt
   */
  void checkAndReserveQuota(
      Long traineeId,
      CommunicationChannel channel,
      Long authorizedProviderId,
      String templateCode,
      String parametersHash,
      boolean isRetry
  );

  /**
   * Resolves the authoritative training provider ID for rate limiting from caller security context
   * and trainee enrollment records.
   *
   * @param traineeId target trainee ID
   * @param callerSuppliedProviderId caller-supplied provider ID (validated against caller scope)
   * @return authoritative provider ID, or null if nationwide/unassociated
   */
  Long resolveAuthoritativeProviderId(Long traineeId, Long callerSuppliedProviderId);
}
