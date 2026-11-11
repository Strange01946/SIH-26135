package in.gov.sih.sih26135.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Data Quality Scanning & Anomaly Detection Engine.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Bounded chunk processing sizes for memory and database safety.</li>
 *   <li>Configurable, approved statutory minimum wage thresholds per jurisdiction.</li>
 *   <li>Configurable upper-bound realistic salary validation threshold.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "data-quality")
public class DataQualityEngineProperties {

  /**
   * Bounded batch chunk size for keyset-paginated entity evaluation.
   */
  private int batchChunkSize = 500;

  /**
   * Distributed named lock acquisition timeout in seconds.
   */
  private int lockTimeoutSeconds = 10;

  /**
   * Optional maximum realistic monthly salary boundary (in INR).
   * Salaries above this threshold are flagged as potentially suspicious/erroneous.
   * Defaults to 5,000,000 INR/month.
   */
  private BigDecimal maximumMonthlySalaryThreshold = new BigDecimal("5000000.00");

  /**
   * Authoritative statutory minimum wage thresholds (in INR/month) mapped by jurisdiction (state ID or code).
   * If a state does not have an approved threshold configured, statutory wage evaluations are skipped safely
   * without fabricating fake legal violations.
   */
  private Map<String, BigDecimal> stateMinimumWages = new HashMap<>();

  public int getBatchChunkSize() {
    return batchChunkSize;
  }

  public void setBatchChunkSize(int batchChunkSize) {
    if (batchChunkSize > 0 && batchChunkSize <= 5000) {
      this.batchChunkSize = batchChunkSize;
    }
  }

  public int getLockTimeoutSeconds() {
    return lockTimeoutSeconds;
  }

  public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
    if (lockTimeoutSeconds > 0 && lockTimeoutSeconds <= 60) {
      this.lockTimeoutSeconds = lockTimeoutSeconds;
    }
  }

  public BigDecimal getMaximumMonthlySalaryThreshold() {
    return maximumMonthlySalaryThreshold;
  }

  public void setMaximumMonthlySalaryThreshold(BigDecimal maximumMonthlySalaryThreshold) {
    this.maximumMonthlySalaryThreshold = maximumMonthlySalaryThreshold;
  }

  public Map<String, BigDecimal> getStateMinimumWages() {
    return stateMinimumWages;
  }

  public void setStateMinimumWages(Map<String, BigDecimal> stateMinimumWages) {
    this.stateMinimumWages = stateMinimumWages != null ? stateMinimumWages : new HashMap<>();
  }

  /**
   * Resolves the configured statutory minimum wage for a given state ID.
   *
   * @param stateId unique state identifier
   * @return statutory minimum wage BigDecimal, or {@code null} if not configured
   */
  public BigDecimal resolveMinimumWageForState(Long stateId) {
    if (stateId == null || stateMinimumWages == null) {
      return null;
    }
    BigDecimal wage = stateMinimumWages.get(String.valueOf(stateId));
    if (wage != null) {
      return wage;
    }
    return stateMinimumWages.get("STATE_" + stateId);
  }
}
