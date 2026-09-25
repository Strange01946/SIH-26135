package in.gov.sih.sih26135.security.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for account lockout and brute-force protection.
 *
 * <p>Enforces configurable threshold for maximum consecutive failed login attempts
 * and duration of the timed lockout before subsequent attempts are permitted.
 * Both properties are validated at application startup to ensure positive, non-zero values.
 */
@Configuration
@ConfigurationProperties(prefix = "security.lockout")
public class SecurityLockoutProperties {

  /**
   * Maximum number of consecutive failed login attempts before account lockout.
   * Default: 5 attempts.
   */
  private int maxFailedAttempts = 5;

  /**
   * Duration for which the account remains locked after reaching maxFailedAttempts.
   * Default: 15 minutes.
   */
  private Duration lockDuration = Duration.ofMinutes(15);

  public int getMaxFailedAttempts() {
    return maxFailedAttempts;
  }

  public void setMaxFailedAttempts(int maxFailedAttempts) {
    this.maxFailedAttempts = maxFailedAttempts;
  }

  public Duration getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(Duration lockDuration) {
    this.lockDuration = lockDuration;
  }

  /**
   * Validates configuration parameters at application startup.
   *
   * @throws IllegalStateException if maxFailedAttempts <= 0 or lockDuration is not positive
   */
  @PostConstruct
  public void validate() {
    if (maxFailedAttempts <= 0) {
      throw new IllegalStateException(
          "Invalid security.lockout.max-failed-attempts: must be greater than 0, configured value is "
              + maxFailedAttempts);
    }
    if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
      throw new IllegalStateException(
          "Invalid security.lockout.lock-duration: must be a positive duration, configured value is "
              + lockDuration);
    }
  }
}
