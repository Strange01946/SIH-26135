package in.gov.sih.sih26135.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;

/**
 * Reusable, stateless, thread-safe, null-safe mathematical and aggregation utility
 * for analytics and dashboard metrics.
 *
 * <p>Standardizes numeric operations across all analytical queries (Q01–Q22):
 * <ul>
 *   <li>All percentage and decimal calculations strictly use {@link BigDecimal} with scale 2 and {@link RoundingMode#HALF_UP}.</li>
 *   <li>Counts strictly use {@link Long} with overflow detection via {@link Math#addExact(long, long)}.</li>
 *   <li>Zero denominators and null operands safely return {@code null} without throwing {@link ArithmeticException} or returning NaN/Infinity.</li>
 *   <li>Preserves distinct semantics between {@code null} (unobservable/undefined) and {@code 0.00} (zero rate).</li>
 * </ul>
 */
public final class AnalyticsCalculationUtils {

  public static final int DEFAULT_PERCENTAGE_SCALE = 2;
  public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
  public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private AnalyticsCalculationUtils() {
    // Utility class: prevent instantiation
  }

  /**
   * Normalizes a {@link BigDecimal} value by returning {@code null} if the value is null or zero.
   * Useful for preparing denominators in SQL/view division expressions.
   *
   * @param value input decimal value
   * @return {@code null} if value is null or equal to zero; otherwise the original value
   */
  public static BigDecimal nullIfZero(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return value;
  }

  /**
   * Normalizes a {@link Long} count by returning {@code null} if the value is null or zero.
   *
   * @param value input count
   * @return {@code null} if value is null or zero; otherwise the original value
   */
  public static Long nullIfZero(Long value) {
    if (value == null || value == 0L) {
      return null;
    }
    return value;
  }

  /**
   * Safely divides two {@link BigDecimal} operands with specified scale and rounding mode.
   * Returns {@code null} if either operand is null or the denominator is zero.
   *
   * @param numerator dividend
   * @param denominator divisor
   * @param scale decimal scale of the quotient
   * @param roundingMode rounding algorithm
   * @return quotient rounded to scale, or {@code null} if division is undefined
   */
  public static BigDecimal safeDivide(
      BigDecimal numerator,
      BigDecimal denominator,
      int scale,
      RoundingMode roundingMode) {
    if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return numerator.divide(denominator, scale, roundingMode);
  }

  /**
   * Safely divides two {@link BigDecimal} operands using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param numerator dividend
   * @param denominator divisor
   * @return quotient rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
    return safeDivide(numerator, denominator, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Safely divides two {@link Long} operands with specified scale and rounding mode.
   * Returns {@code null} if either operand is null or the denominator is zero.
   *
   * @param numerator dividend count
   * @param denominator divisor count
   * @param scale decimal scale of the quotient
   * @param roundingMode rounding algorithm
   * @return quotient as {@link BigDecimal}, or {@code null} if undefined
   */
  public static BigDecimal safeDivide(
      Long numerator,
      Long denominator,
      int scale,
      RoundingMode roundingMode) {
    if (numerator == null || denominator == null || denominator == 0L) {
      return null;
    }
    return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), scale, roundingMode);
  }

  /**
   * Safely divides two {@link Long} operands using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param numerator dividend count
   * @param denominator divisor count
   * @return quotient rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal safeDivide(Long numerator, Long denominator) {
    return safeDivide(numerator, denominator, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Calculates percentage: {@code (numerator / denominator) * 100} using specified scale and rounding mode.
   * Multiplies before dividing to prevent premature precision loss on repeating fractions.
   * Returns {@code null} if numerator is null, denominator is null, or denominator is zero.
   *
   * @param numerator part value
   * @param denominator whole/total value
   * @param scale decimal scale
   * @param roundingMode rounding algorithm
   * @return calculated percentage, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentage(
      BigDecimal numerator,
      BigDecimal denominator,
      int scale,
      RoundingMode roundingMode) {
    if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return numerator.multiply(HUNDRED).divide(denominator, scale, roundingMode);
  }

  /**
   * Calculates percentage using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param numerator part value
   * @param denominator whole/total value
   * @return calculated percentage rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
    return calculatePercentage(numerator, denominator, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Calculates percentage for {@link Long} counts using specified scale and rounding mode.
   * Returns {@code null} if numerator is null, denominator is null, or denominator is zero.
   *
   * @param numerator part count
   * @param denominator total count
   * @param scale decimal scale
   * @param roundingMode rounding algorithm
   * @return calculated percentage, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentage(
      Long numerator,
      Long denominator,
      int scale,
      RoundingMode roundingMode) {
    if (numerator == null || denominator == null || denominator == 0L) {
      return null;
    }
    return BigDecimal.valueOf(numerator).multiply(HUNDRED).divide(BigDecimal.valueOf(denominator), scale, roundingMode);
  }

  /**
   * Calculates percentage for {@link Long} counts using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param numerator part count
   * @param denominator total count
   * @return calculated percentage rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentage(Long numerator, Long denominator) {
    return calculatePercentage(numerator, denominator, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Rounds a {@link BigDecimal} to the specified scale and rounding mode.
   * Returns {@code null} if the input value is null.
   *
   * @param value input decimal value
   * @param scale target scale
   * @param roundingMode rounding algorithm
   * @return rounded value, or {@code null} if input was null
   */
  public static BigDecimal round(BigDecimal value, int scale, RoundingMode roundingMode) {
    if (value == null) {
      return null;
    }
    return value.setScale(scale, roundingMode);
  }

  /**
   * Rounds a {@link BigDecimal} to 2 decimal places using {@link RoundingMode#HALF_UP}.
   * Returns {@code null} if the input value is null.
   *
   * @param value input decimal value
   * @return rounded value with scale 2, or {@code null} if input was null
   */
  public static BigDecimal round(BigDecimal value) {
    return round(value, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Safely adds two {@link Long} values with overflow detection.
   * If both operands are null, returns {@code null}.
   * If one operand is null, returns the other.
   * If both are present, returns {@link Math#addExact(long, long)}.
   *
   * @param a first operand
   * @param b second operand
   * @return sum, or {@code null} if both operands are null
   * @throws ArithmeticException if 64-bit integer addition overflows
   */
  public static Long safeSum(Long a, Long b) {
    if (a == null && b == null) {
      return null;
    }
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return Math.addExact(a, b);
  }

  /**
   * Aggregates an array of count values with overflow detection.
   * Preserves null semantics: returns {@code null} if the array is null or contains only null elements.
   * Throws {@link ArithmeticException} if the total overflows {@code Long.MAX_VALUE}.
   *
   * @param values variable number of count values
   * @return total count, or {@code null} if all items are null
   * @throws ArithmeticException if addition overflows
   */
  public static Long sumCounts(Long... values) {
    if (values == null) {
      return null;
    }
    return sumCounts(Arrays.asList(values));
  }

  /**
   * Aggregates a collection of count values with overflow detection.
   * Preserves null semantics: returns {@code null} if the collection is null or contains only null elements.
   *
   * @param values collection of count values
   * @return total count, or {@code null} if all items are null
   * @throws ArithmeticException if addition overflows
   */
  public static Long sumCounts(Collection<Long> values) {
    if (values == null) {
      return null;
    }
    boolean hasValue = false;
    long total = 0L;
    for (Long val : values) {
      if (val != null) {
        total = Math.addExact(total, val);
        hasValue = true;
      }
    }
    return hasValue ? total : null;
  }

  /**
   * Aggregates an array of monetary/decimal values without premature rounding.
   * Returns {@code null} if the array is null or contains only null elements.
   *
   * @param values variable number of monetary values
   * @return total sum, or {@code null} if all items are null
   */
  public static BigDecimal sumMonetary(BigDecimal... values) {
    if (values == null) {
      return null;
    }
    return sumMonetary(Arrays.asList(values));
  }

  /**
   * Aggregates a collection of monetary/decimal values without premature rounding.
   * Returns {@code null} if the collection is null or contains only null elements.
   *
   * @param values collection of monetary values
   * @return total sum, or {@code null} if all items are null
   */
  public static BigDecimal sumMonetary(Collection<BigDecimal> values) {
    if (values == null) {
      return null;
    }
    boolean hasValue = false;
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal val : values) {
      if (val != null) {
        total = total.add(val);
        hasValue = true;
      }
    }
    return hasValue ? total : null;
  }

  /**
   * Calculates the average of a monetary sum over a count of items.
   * Returns {@code null} if sum is null, count is null, or count is non-positive.
   *
   * @param sum total monetary sum
   * @param count count of items
   * @param scale target scale
   * @param roundingMode rounding algorithm
   * @return average value, or {@code null} if undefined
   */
  public static BigDecimal safeAverage(
      BigDecimal sum,
      Long count,
      int scale,
      RoundingMode roundingMode) {
    if (sum == null || count == null || count <= 0L) {
      return null;
    }
    return sum.divide(BigDecimal.valueOf(count), scale, roundingMode);
  }

  /**
   * Calculates the average using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param sum total monetary sum
   * @param count count of items
   * @return average value rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal safeAverage(BigDecimal sum, Long count) {
    return safeAverage(sum, count, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }

  /**
   * Calculates arithmetic delta: {@code current - baseline}.
   * Returns {@code null} if either operand is null.
   *
   * @param current current value
   * @param baseline baseline value
   * @return delta, or {@code null} if either operand is null
   */
  public static BigDecimal calculateDelta(BigDecimal current, BigDecimal baseline) {
    if (current == null || baseline == null) {
      return null;
    }
    return current.subtract(baseline);
  }

  /**
   * Calculates percentage change: {@code ((current - baseline) / |baseline|) * 100}.
   * Returns {@code null} if current is null, baseline is null, or baseline is zero.
   *
   * @param current current value
   * @param baseline baseline value
   * @param scale target scale
   * @param roundingMode rounding algorithm
   * @return percentage change, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentageChange(
      BigDecimal current,
      BigDecimal baseline,
      int scale,
      RoundingMode roundingMode) {
    if (current == null || baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return current.subtract(baseline).multiply(HUNDRED).divide(baseline.abs(), scale, roundingMode);
  }

  /**
   * Calculates percentage change using default scale (2) and {@link RoundingMode#HALF_UP}.
   *
   * @param current current value
   * @param baseline baseline value
   * @return percentage change rounded to 2 decimal places, or {@code null} if undefined
   */
  public static BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal baseline) {
    return calculatePercentageChange(current, baseline, DEFAULT_PERCENTAGE_SCALE, DEFAULT_ROUNDING_MODE);
  }
}
