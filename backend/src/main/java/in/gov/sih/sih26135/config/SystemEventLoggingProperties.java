package in.gov.sih.sih26135.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Operational configuration properties for Phase 28.6 system event logging and anti-storm throttling.
 */
@Component
@ConfigurationProperties(prefix = "system-events")
public class SystemEventLoggingProperties {

  /**
   * Cooldown window (in seconds) suppressing repeated database writes for repeated
   * rate-limit rejections sharing the same (actorUserId, eventCode, sanitizedRoute) key.
   */
  private int rateLimitAlertCooldownSeconds = 60;

  /**
   * Deduplication window (in seconds) suppressing accidental duplicate event emissions
   * for identical (category, eventCode, entityType, entityId, correlationId).
   */
  private int dedupWindowSeconds = 60;

  /**
   * Hard upper bound on in-memory throttle cache entries to guarantee bounded heap usage.
   */
  private int maxThrottleCacheEntries = 10000;

  public int getRateLimitAlertCooldownSeconds() {
    return rateLimitAlertCooldownSeconds;
  }

  public void setRateLimitAlertCooldownSeconds(int rateLimitAlertCooldownSeconds) {
    if (rateLimitAlertCooldownSeconds >= 0 && rateLimitAlertCooldownSeconds <= 3600) {
      this.rateLimitAlertCooldownSeconds = rateLimitAlertCooldownSeconds;
    }
  }

  public int getDedupWindowSeconds() {
    return dedupWindowSeconds;
  }

  public void setDedupWindowSeconds(int dedupWindowSeconds) {
    if (dedupWindowSeconds >= 0 && dedupWindowSeconds <= 3600) {
      this.dedupWindowSeconds = dedupWindowSeconds;
    }
  }

  public int getMaxThrottleCacheEntries() {
    return maxThrottleCacheEntries;
  }

  public void setMaxThrottleCacheEntries(int maxThrottleCacheEntries) {
    if (maxThrottleCacheEntries >= 100 && maxThrottleCacheEntries <= 100000) {
      this.maxThrottleCacheEntries = maxThrottleCacheEntries;
    }
  }
}
