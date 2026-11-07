package in.gov.sih.sih26135.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Government MIS Report Generation and Data Export APIs.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Default and maximum export row count guardrails (bounded memory).</li>
 *   <li>Per-user sliding window download rate limits.</li>
 *   <li>Per-user and system-wide concurrent export limits.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "export")
public class AnalyticsExportProperties {

  /**
   * Default row count ceiling for an export stream.
   */
  private int rowLimitDefault = 10000;

  /**
   * Hard maximum allowable row count ceiling for an export stream.
   * Any request that exceeds this limit is rejected with HTTP 400 before streaming begins.
   */
  private int rowLimitMax = 50000;

  /**
   * Maximum allowed export requests per authenticated user within a 1-minute window.
   */
  private int rateLimitPerMinute = 5;

  /**
   * Maximum allowed export requests per authenticated user within a 1-hour window.
   */
  private int rateLimitPerHour = 30;

  /**
   * Maximum simultaneous active export streams allowed per authenticated user.
   */
  private int concurrencyLimitPerUser = 2;

  /**
   * Maximum simultaneous active export streams allowed globally across the instance.
   */
  private int concurrencyLimitGlobal = 10;

  public int getRowLimitDefault() {
    return rowLimitDefault;
  }

  public void setRowLimitDefault(int rowLimitDefault) {
    this.rowLimitDefault = rowLimitDefault;
  }

  public int getRowLimitMax() {
    return rowLimitMax;
  }

  public void setRowLimitMax(int rowLimitMax) {
    this.rowLimitMax = rowLimitMax;
  }

  public int getRateLimitPerMinute() {
    return rateLimitPerMinute;
  }

  public void setRateLimitPerMinute(int rateLimitPerMinute) {
    this.rateLimitPerMinute = rateLimitPerMinute;
  }

  public int getRateLimitPerHour() {
    return rateLimitPerHour;
  }

  public void setRateLimitPerHour(int rateLimitPerHour) {
    this.rateLimitPerHour = rateLimitPerHour;
  }

  public int getConcurrencyLimitPerUser() {
    return concurrencyLimitPerUser;
  }

  public void setConcurrencyLimitPerUser(int concurrencyLimitPerUser) {
    this.concurrencyLimitPerUser = concurrencyLimitPerUser;
  }

  public int getConcurrencyLimitGlobal() {
    return concurrencyLimitGlobal;
  }

  public void setConcurrencyLimitGlobal(int concurrencyLimitGlobal) {
    this.concurrencyLimitGlobal = concurrencyLimitGlobal;
  }
}
