package in.gov.sih.sih26135.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnalyticsCalculationUtils Unit Tests")
class AnalyticsCalculationUtilsTest {

  @Nested
  @DisplayName("Safe Division Tests")
  class SafeDivisionTests {

    @Test
    @DisplayName("safeDivide: positive numerator and denominator")
    void testSafeDividePositive() {
      BigDecimal result = AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), new BigDecimal("2.00"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("safeDivide: zero numerator returns 0.00")
    void testSafeDivideZeroNumerator() {
      BigDecimal result = AnalyticsCalculationUtils.safeDivide(BigDecimal.ZERO, new BigDecimal("5.00"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("safeDivide: zero denominator returns null (no exception)")
    void testSafeDivideZeroDenominator() {
      assertThat(AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), BigDecimal.ZERO)).isNull();
      assertThat(AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), new BigDecimal("0.00"))).isNull();
    }

    @Test
    @DisplayName("safeDivide: null numerator returns null")
    void testSafeDivideNullNumerator() {
      assertThat(AnalyticsCalculationUtils.safeDivide(null, new BigDecimal("5.00"))).isNull();
    }

    @Test
    @DisplayName("safeDivide: null denominator returns null")
    void testSafeDivideNullDenominator() {
      assertThat(AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), null)).isNull();
    }

    @Test
    @DisplayName("safeDivide: both operands null returns null")
    void testSafeDivideBothNull() {
      assertThat(AnalyticsCalculationUtils.safeDivide((BigDecimal) null, (BigDecimal) null)).isNull();
      assertThat(AnalyticsCalculationUtils.safeDivide((Long) null, (Long) null)).isNull();
    }

    @Test
    @DisplayName("safeDivide: negative operands")
    void testSafeDivideNegative() {
      BigDecimal negNum = AnalyticsCalculationUtils.safeDivide(new BigDecimal("-10.00"), new BigDecimal("2.00"));
      assertThat(negNum).isEqualByComparingTo(new BigDecimal("-5.00"));

      BigDecimal negDenom = AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), new BigDecimal("-2.00"));
      assertThat(negDenom).isEqualByComparingTo(new BigDecimal("-5.00"));

      BigDecimal bothNeg = AnalyticsCalculationUtils.safeDivide(new BigDecimal("-10.00"), new BigDecimal("-2.00"));
      assertThat(bothNeg).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("safeDivide: very large BigDecimal values")
    void testSafeDivideLargeValues() {
      BigDecimal large = new BigDecimal("1000000000000000000000000000000.00");
      BigDecimal result = AnalyticsCalculationUtils.safeDivide(large, new BigDecimal("2.00"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("500000000000000000000000000000.00"));
    }

    @Test
    @DisplayName("safeDivide: very small decimal values")
    void testSafeDivideSmallValues() {
      BigDecimal small = new BigDecimal("0.0001");
      BigDecimal result = AnalyticsCalculationUtils.safeDivide(small, new BigDecimal("2.00"), 4, RoundingMode.HALF_UP);
      assertThat(result).isEqualByComparingTo(new BigDecimal("0.0001"));
    }

    @Test
    @DisplayName("safeDivide: non-terminating division requiring HALF_UP rounding")
    void testSafeDivideNonTerminating() {
      BigDecimal result = AnalyticsCalculationUtils.safeDivide(new BigDecimal("10.00"), new BigDecimal("3.00"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("3.33"));
    }

    @Test
    @DisplayName("safeDivide(Long, Long): normal, zero, null, and non-terminating")
    void testSafeDivideLong() {
      assertThat(AnalyticsCalculationUtils.safeDivide(10L, 2L)).isEqualByComparingTo(new BigDecimal("5.00"));
      assertThat(AnalyticsCalculationUtils.safeDivide(10L, 3L)).isEqualByComparingTo(new BigDecimal("3.33"));
      assertThat(AnalyticsCalculationUtils.safeDivide(0L, 5L)).isEqualByComparingTo(new BigDecimal("0.00"));
      assertThat(AnalyticsCalculationUtils.safeDivide(10L, 0L)).isNull();
      assertThat(AnalyticsCalculationUtils.safeDivide(null, 5L)).isNull();
      assertThat(AnalyticsCalculationUtils.safeDivide(10L, null)).isNull();
    }
  }

  @Nested
  @DisplayName("nullIfZero Tests")
  class NullIfZeroTests {

    @Test
    @DisplayName("nullIfZero(BigDecimal): null, zero, zero with scale, and non-zero")
    void testNullIfZeroBigDecimal() {
      assertThat(AnalyticsCalculationUtils.nullIfZero((BigDecimal) null)).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(BigDecimal.ZERO)).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(new BigDecimal("0.00"))).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(new BigDecimal("0.0000"))).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(new BigDecimal("42.50")))
          .isEqualByComparingTo(new BigDecimal("42.50"));
    }

    @Test
    @DisplayName("nullIfZero(Long): null, 0L, and non-zero")
    void testNullIfZeroLong() {
      assertThat(AnalyticsCalculationUtils.nullIfZero((Long) null)).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(0L)).isNull();
      assertThat(AnalyticsCalculationUtils.nullIfZero(100L)).isEqualTo(100L);
    }
  }

  @Nested
  @DisplayName("Percentage Calculation Tests")
  class PercentageCalculationTests {

    @Test
    @DisplayName("calculatePercentage: exact percentage (50 / 100 = 50.00%)")
    void testExactPercentage() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("50"), new BigDecimal("100"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("calculatePercentage: rounding HALF_UP on 1/6 (16.67%)")
    void testRoundingHalfUpOneSixth() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("1"), new BigDecimal("6"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("16.67"));
    }

    @Test
    @DisplayName("calculatePercentage: rounding HALF_UP on 1/3 (33.33%)")
    void testRoundingHalfUpOneThird() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("1"), new BigDecimal("3"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("calculatePercentage: exact half tie-break on 1/8 (12.50%)")
    void testOneEighth() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("1"), new BigDecimal("8"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    @DisplayName("calculatePercentage: zero denominator returns null")
    void testZeroDenominatorReturnsNull() {
      assertThat(AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("50"), BigDecimal.ZERO)).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("50"), new BigDecimal("0.00"))).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentage(50L, 0L)).isNull();
    }

    @Test
    @DisplayName("calculatePercentage: null numerator returns null")
    void testNullNumeratorReturnsNull() {
      assertThat(AnalyticsCalculationUtils.calculatePercentage((BigDecimal) null, new BigDecimal("100"))).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentage((Long) null, 100L)).isNull();
    }

    @Test
    @DisplayName("calculatePercentage: null denominator returns null")
    void testNullDenominatorReturnsNull() {
      assertThat(AnalyticsCalculationUtils.calculatePercentage(new BigDecimal("50"), null)).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentage(50L, null)).isNull();
    }

    @Test
    @DisplayName("calculatePercentage: zero numerator returns 0.00")
    void testZeroNumeratorReturnsZero() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(BigDecimal.ZERO, new BigDecimal("100"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));

      BigDecimal resultLong = AnalyticsCalculationUtils.calculatePercentage(0L, 100L);
      assertThat(resultLong).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("calculatePercentage: values above 100% (150 / 100 = 150.00%)")
    void testAboveHundredPercent() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(150L, 100L);
      assertThat(result).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("calculatePercentage: negative values (-25 / 100 = -25.00%)")
    void testNegativePercentage() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(-25L, 100L);
      assertThat(result).isEqualByComparingTo(new BigDecimal("-25.00"));
    }

    @Test
    @DisplayName("calculatePercentage: custom scale and rounding mode")
    void testCustomScaleAndRounding() {
      BigDecimal result = AnalyticsCalculationUtils.calculatePercentage(
          new BigDecimal("1"),
          new BigDecimal("3"),
          4,
          RoundingMode.DOWN);
      assertThat(result).isEqualByComparingTo(new BigDecimal("33.3333"));
    }
  }

  @Nested
  @DisplayName("Decimal Rounding Tests")
  class DecimalRoundingTests {

    @Test
    @DisplayName("round: rounds up at .005")
    void testRoundUp() {
      BigDecimal result = AnalyticsCalculationUtils.round(new BigDecimal("12.345"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.35"));
    }

    @Test
    @DisplayName("round: rounds down at .004")
    void testRoundDown() {
      BigDecimal result = AnalyticsCalculationUtils.round(new BigDecimal("12.344"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.34"));
    }

    @Test
    @DisplayName("round: already at scale 2")
    void testAlreadyScale2() {
      BigDecimal result = AnalyticsCalculationUtils.round(new BigDecimal("12.34"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.34"));
    }

    @Test
    @DisplayName("round: scale less than 2 pads zeroes")
    void testScaleLessThan2() {
      BigDecimal result = AnalyticsCalculationUtils.round(new BigDecimal("12"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("round: multiple decimal places")
    void testMultipleDecimals() {
      BigDecimal result = AnalyticsCalculationUtils.round(new BigDecimal("12.34567"));
      assertThat(result).isEqualByComparingTo(new BigDecimal("12.35"));
    }

    @Test
    @DisplayName("round: null input returns null")
    void testRoundNull() {
      assertThat(AnalyticsCalculationUtils.round(null)).isNull();
      assertThat(AnalyticsCalculationUtils.round(null, 4, RoundingMode.HALF_UP)).isNull();
    }

    @Test
    @DisplayName("round: large monetary values")
    void testLargeMonetaryValue() {
      BigDecimal large = new BigDecimal("999999999.995");
      BigDecimal result = AnalyticsCalculationUtils.round(large);
      assertThat(result).isEqualByComparingTo(new BigDecimal("1000000000.00"));
    }

    @Test
    @DisplayName("round: negative monetary values")
    void testNegativeMonetaryValue() {
      BigDecimal resultUp = AnalyticsCalculationUtils.round(new BigDecimal("-12.345"));
      assertThat(resultUp).isEqualByComparingTo(new BigDecimal("-12.35"));

      BigDecimal resultDown = AnalyticsCalculationUtils.round(new BigDecimal("-12.344"));
      assertThat(resultDown).isEqualByComparingTo(new BigDecimal("-12.34"));
    }
  }

  @Nested
  @DisplayName("Count Aggregation Tests")
  class CountAggregationTests {

    @Test
    @DisplayName("safeSum: normal addition")
    void testSafeSumNormal() {
      assertThat(AnalyticsCalculationUtils.safeSum(10L, 20L)).isEqualTo(30L);
    }

    @Test
    @DisplayName("safeSum: null handling")
    void testSafeSumNullHandling() {
      assertThat(AnalyticsCalculationUtils.safeSum(null, 20L)).isEqualTo(20L);
      assertThat(AnalyticsCalculationUtils.safeSum(10L, null)).isEqualTo(10L);
      assertThat(AnalyticsCalculationUtils.safeSum(null, null)).isNull();
    }

    @Test
    @DisplayName("safeSum: zero values")
    void testSafeSumZero() {
      assertThat(AnalyticsCalculationUtils.safeSum(0L, 0L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("safeSum: overflow detection via Math.addExact")
    void testSafeSumOverflow() {
      assertThatThrownBy(() -> AnalyticsCalculationUtils.safeSum(Long.MAX_VALUE, 1L))
          .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("sumCounts: varargs normal, nulls, and all nulls")
    void testSumCountsVarargs() {
      assertThat(AnalyticsCalculationUtils.sumCounts(10L, 20L, 30L)).isEqualTo(60L);
      assertThat(AnalyticsCalculationUtils.sumCounts(10L, null, 30L)).isEqualTo(40L);
      assertThat(AnalyticsCalculationUtils.sumCounts((Long) null, (Long) null)).isNull();
      assertThat(AnalyticsCalculationUtils.sumCounts((Long[]) null)).isNull();
    }

    @Test
    @DisplayName("sumCounts: collection handling")
    void testSumCountsCollection() {
      assertThat(AnalyticsCalculationUtils.sumCounts(List.of(5L, 15L, 25L))).isEqualTo(45L);
      assertThat(AnalyticsCalculationUtils.sumCounts(Arrays.asList(5L, null, 25L))).isEqualTo(30L);
      assertThat(AnalyticsCalculationUtils.sumCounts(Collections.emptyList())).isNull();
      assertThat(AnalyticsCalculationUtils.sumCounts((List<Long>) null)).isNull();
    }

    @Test
    @DisplayName("sumCounts: overflow detection in sequence")
    void testSumCountsOverflow() {
      List<Long> values = List.of(Long.MAX_VALUE - 5, 3L, 3L);
      assertThatThrownBy(() -> AnalyticsCalculationUtils.sumCounts(values))
          .isInstanceOf(ArithmeticException.class);
    }
  }

  @Nested
  @DisplayName("Monetary Aggregation Tests")
  class MonetaryAggregationTests {

    @Test
    @DisplayName("sumMonetary: normal addition without premature rounding")
    void testSumMonetaryNormal() {
      BigDecimal val1 = new BigDecimal("100.555");
      BigDecimal val2 = new BigDecimal("200.445");
      BigDecimal sum = AnalyticsCalculationUtils.sumMonetary(val1, val2);
      assertThat(sum).isEqualByComparingTo(new BigDecimal("301.000"));
    }

    @Test
    @DisplayName("sumMonetary: null operands and all nulls")
    void testSumMonetaryNulls() {
      assertThat(AnalyticsCalculationUtils.sumMonetary(new BigDecimal("100.00"), null)).isEqualByComparingTo(new BigDecimal("100.00"));
      assertThat(AnalyticsCalculationUtils.sumMonetary((BigDecimal) null, (BigDecimal) null)).isNull();
      assertThat(AnalyticsCalculationUtils.sumMonetary((BigDecimal[]) null)).isNull();
      assertThat(AnalyticsCalculationUtils.sumMonetary((List<BigDecimal>) null)).isNull();
      assertThat(AnalyticsCalculationUtils.sumMonetary(Collections.emptyList())).isNull();
    }
  }

  @Nested
  @DisplayName("Average Calculation Tests")
  class SafeAverageTests {

    @Test
    @DisplayName("safeAverage: normal division")
    void testSafeAverageNormal() {
      BigDecimal sum = new BigDecimal("100.00");
      BigDecimal avg = AnalyticsCalculationUtils.safeAverage(sum, 4L);
      assertThat(avg).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("safeAverage: repeating decimal rounded HALF_UP")
    void testSafeAverageRepeating() {
      BigDecimal sum = new BigDecimal("100.00");
      BigDecimal avg = AnalyticsCalculationUtils.safeAverage(sum, 3L);
      assertThat(avg).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("safeAverage: null sum, null count, zero count, and negative count")
    void testSafeAverageEdgeCases() {
      assertThat(AnalyticsCalculationUtils.safeAverage(null, 4L)).isNull();
      assertThat(AnalyticsCalculationUtils.safeAverage(new BigDecimal("100.00"), null)).isNull();
      assertThat(AnalyticsCalculationUtils.safeAverage(new BigDecimal("100.00"), 0L)).isNull();
      assertThat(AnalyticsCalculationUtils.safeAverage(new BigDecimal("100.00"), -5L)).isNull();
    }
  }

  @Nested
  @DisplayName("Delta and Percentage Change Tests")
  class DeltaAndChangeTests {

    @Test
    @DisplayName("calculateDelta: normal and null handling")
    void testCalculateDelta() {
      BigDecimal current = new BigDecimal("150.00");
      BigDecimal baseline = new BigDecimal("100.00");
      assertThat(AnalyticsCalculationUtils.calculateDelta(current, baseline)).isEqualByComparingTo(new BigDecimal("50.00"));
      assertThat(AnalyticsCalculationUtils.calculateDelta(null, baseline)).isNull();
      assertThat(AnalyticsCalculationUtils.calculateDelta(current, null)).isNull();
    }

    @Test
    @DisplayName("calculatePercentageChange: positive increase")
    void testPercentageChangeIncrease() {
      BigDecimal current = new BigDecimal("150.00");
      BigDecimal baseline = new BigDecimal("100.00");
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(current, baseline))
          .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("calculatePercentageChange: negative decrease")
    void testPercentageChangeDecrease() {
      BigDecimal current = new BigDecimal("80.00");
      BigDecimal baseline = new BigDecimal("100.00");
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(current, baseline))
          .isEqualByComparingTo(new BigDecimal("-20.00"));
    }

    @Test
    @DisplayName("calculatePercentageChange: zero baseline returns null")
    void testPercentageChangeZeroBaseline() {
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(new BigDecimal("50.00"), BigDecimal.ZERO)).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(new BigDecimal("50.00"), new BigDecimal("0.00"))).isNull();
    }

    @Test
    @DisplayName("calculatePercentageChange: null operands return null")
    void testPercentageChangeNull() {
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(null, new BigDecimal("100.00"))).isNull();
      assertThat(AnalyticsCalculationUtils.calculatePercentageChange(new BigDecimal("50.00"), null)).isNull();
    }
  }
}
