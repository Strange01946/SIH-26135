package in.gov.sih.sih26135.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reusable, null-safe mathematical computation utility for Phase 28.7 operational scorecards and
 * monitoring metrics.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>All ratios, percentages, averages, and rates use {@link BigDecimal} with
 *       {@link RoundingMode#HALF_UP} and explicit scale ({@value #DEFAULT_SCALE}).</li>
 *   <li>Zero denominators, null inputs, and negative/zero durations return {@code null} safely
 *       instead of throwing {@link ArithmeticException}, producing NaN/Infinity, or fabricating
 *       misleading 100.00% no-data scores.</li>
 *   <li>Intermediate multiplications are performed before final division to preserve precision.</li>
 * </ul>
 */
public final class OperationalMetricsMath {

  public static final int DEFAULT_SCALE = 2;
  public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);
  private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600L);

  private OperationalMetricsMath() {}

  /**
   * Computes {@code (numerator / denominator) * 100} using {@link RoundingMode#HALF_UP} at scale 2.
   * Returns {@code null} if either operand is null, numerator is negative, or denominator <= 0.
   */
  public static BigDecimal percentage(Long numerator, Long denominator) {
    return percentage(numerator, denominator, DEFAULT_SCALE);
  }

  /**
   * Computes {@code (numerator / denominator) * 100} using {@link RoundingMode#HALF_UP}.
   * Returns {@code null} if either operand is null, numerator is negative, or denominator <= 0.
   */
  public static BigDecimal percentage(Long numerator, Long denominator, int scale) {
    if (numerator == null || numerator < 0L || denominator == null || denominator <= 0L) {
      return null;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(denominator), scale, DEFAULT_ROUNDING);
  }

  /**
   * Computes the Data Cleanliness Index:
   * {@code ((eligiblePopulation - distinctAffectedEntities) / eligiblePopulation) * 100}.
   *
   * <p>Returns {@code null} when {@code eligiblePopulation} is null or non-positive so no-data
   * cohorts are never misrepresented as 100.00% clean. Bounds affected entities to
   * {@code [0, eligiblePopulation]} to guarantee the index remains in {@code [0.00, 100.00]}.
   */
  public static BigDecimal dataCleanlinessIndex(Long eligiblePopulation, Long distinctAffectedEntities) {
    if (eligiblePopulation == null || eligiblePopulation <= 0L) {
      return null;
    }
    long rawAffected = distinctAffectedEntities != null ? Math.max(0L, distinctAffectedEntities) : 0L;
    long boundedAffected = Math.min(eligiblePopulation, rawAffected);
    long cleanPopulation = Math.subtractExact(eligiblePopulation, boundedAffected);
    return percentage(cleanPopulation, eligiblePopulation, DEFAULT_SCALE);
  }

  /**
   * Computes an average duration in hours from total accumulated seconds and item count:
   * {@code totalSeconds / (count * 3600)}. Returns {@code null} if totalSeconds is null or
   * negative, or if count is null or non-positive.
   */
  public static BigDecimal averageHoursFromSeconds(BigDecimal totalSeconds, Long count) {
    if (totalSeconds == null || count == null || count <= 0L || totalSeconds.compareTo(BigDecimal.ZERO) < 0) {
      return null;
    }
    BigDecimal denominator = BigDecimal.valueOf(count).multiply(SECONDS_PER_HOUR);
    return totalSeconds.divide(denominator, DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  /**
   * Computes an hourly rate / velocity: {@code (count * 3600) / durationSeconds}.
   * Returns {@code null} when {@code count} is null/negative or {@code durationSeconds} is
   * null/non-positive (prevents division by zero and misleading rates on zero-duration windows).
   */
  public static BigDecimal ratePerHour(Long count, BigDecimal durationSeconds) {
    if (count == null || count < 0L || durationSeconds == null || durationSeconds.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return BigDecimal.valueOf(count)
        .multiply(SECONDS_PER_HOUR)
        .divide(durationSeconds, DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  /**
   * Converts a non-negative duration in seconds to hours using {@link RoundingMode#HALF_UP}
   * at scale 2, returning {@code null} if null or negative.
   */
  public static BigDecimal hoursFromSeconds(BigDecimal durationSeconds) {
    if (durationSeconds == null || durationSeconds.compareTo(BigDecimal.ZERO) < 0) {
      return null;
    }
    return durationSeconds.divide(SECONDS_PER_HOUR, DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  /**
   * Converts microseconds to exact fractional seconds without premature rounding.
   */
  public static BigDecimal microsecondsToExactSeconds(BigDecimal microseconds) {
    if (microseconds == null || microseconds.compareTo(BigDecimal.ZERO) < 0) {
      return null;
    }
    return microseconds.divide(BigDecimal.valueOf(1_000_000L));
  }

  /**
   * Null-safe, overflow-checked addition of two counts.
   */
  public static long safeAdd(Long a, Long b) {
    return Math.addExact(a != null ? a : 0L, b != null ? b : 0L);
  }
}
