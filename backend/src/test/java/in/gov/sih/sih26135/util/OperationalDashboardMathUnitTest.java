package in.gov.sih.sih26135.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.gov.sih.sih26135.dto.request.OperationalDashboardFilter;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Phase 28.7 - OperationalMetricsMath & Filter Window Unit Tests")
class OperationalDashboardMathUnitTest {

  @Test
  @DisplayName("Handles 0/0, 0/N, N/N, repeating fractions, HALF_UP midpoints, large counts, and null operands")
  void percentageAndRoundingSemantics() {
    // 0/0 -> null (not NaN or 100%)
    assertThat(OperationalMetricsMath.percentage(0L, 0L)).isNull();
    assertThat(OperationalMetricsMath.percentage(5L, 0L)).isNull();
    assertThat(OperationalMetricsMath.percentage(null, 10L)).isNull();
    assertThat(OperationalMetricsMath.percentage(10L, null)).isNull();
    assertThat(OperationalMetricsMath.percentage(-1L, 10L)).isNull();

    // 0/N -> 0.00
    assertThat(OperationalMetricsMath.percentage(0L, 7L)).isEqualByComparingTo("0.00");

    // N/N -> 100.00
    assertThat(OperationalMetricsMath.percentage(42L, 42L)).isEqualByComparingTo("100.00");

    // Non-divisible fractions
    assertThat(OperationalMetricsMath.percentage(1L, 3L)).isEqualByComparingTo("33.33");
    assertThat(OperationalMetricsMath.percentage(2L, 3L)).isEqualByComparingTo("66.67");
    assertThat(OperationalMetricsMath.percentage(1L, 6L)).isEqualByComparingTo("16.67");

    // Exact HALF_UP midpoints (0.125 -> 0.13, 0.375 -> 0.38, 0.005 -> 0.01)
    assertThat(OperationalMetricsMath.percentage(1L, 800L)).isEqualByComparingTo("0.13");
    assertThat(OperationalMetricsMath.percentage(3L, 800L)).isEqualByComparingTo("0.38");
    assertThat(OperationalMetricsMath.percentage(1L, 20000L)).isEqualByComparingTo("0.01");

    // Large counts without 32-bit overflow
    assertThat(OperationalMetricsMath.percentage(500_000_000L, 1_000_000_000L)).isEqualByComparingTo("50.00");
    assertThat(OperationalMetricsMath.safeAdd(null, 5L)).isEqualTo(5L);
    assertThat(OperationalMetricsMath.safeAdd(3L, null)).isEqualTo(3L);
    assertThatThrownBy(() -> OperationalMetricsMath.safeAdd(Long.MAX_VALUE, 1L))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  @DisplayName("Data Cleanliness Index handles empty populations, clamping, multi-issue entities, and clean cohorts safely")
  void dataCleanlinessIndexBoundaries() {
    // Empty eligible population returns null (never fake 100% score)
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(0L, 0L)).isNull();
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(null, 0L)).isNull();
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(-5L, 0L)).isNull();

    // No affected entities -> 100.00
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(10L, null)).isEqualByComparingTo("100.00");
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(10L, 0L)).isEqualByComparingTo("100.00");

    // Repeating fraction (2 clean of 3 eligible -> 66.67%)
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(3L, 1L)).isEqualByComparingTo("66.67");

    // Over-count safety clamp -> never drops below 0.00
    assertThat(OperationalMetricsMath.dataCleanlinessIndex(4L, 10L)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("Durations and hourly velocity safeguard zero/negative values and preserve precision before final division")
  void durationAndVelocitySafeguards() {
    assertThat(OperationalMetricsMath.averageHoursFromSeconds(BigDecimal.ZERO, 0L)).isNull();
    assertThat(OperationalMetricsMath.averageHoursFromSeconds(new BigDecimal("-10"), 2L)).isNull();
    assertThat(OperationalMetricsMath.averageHoursFromSeconds(null, 2L)).isNull();
    // 5400s across 2 issues = 2700s avg = 0.75 hours
    assertThat(OperationalMetricsMath.averageHoursFromSeconds(new BigDecimal("5400"), 2L))
        .isEqualByComparingTo("0.75");
    // 54s across 1 issue = 0.015h -> HALF_UP 0.02
    assertThat(OperationalMetricsMath.averageHoursFromSeconds(new BigDecimal("54"), 1L))
        .isEqualByComparingTo("0.02");

    // Zero or negative duration velocity returns null cleanly (no ArithmeticException / Infinity)
    assertThat(OperationalMetricsMath.ratePerHour(5L, BigDecimal.ZERO)).isNull();
    assertThat(OperationalMetricsMath.ratePerHour(5L, new BigDecimal("-3600"))).isNull();
    assertThat(OperationalMetricsMath.ratePerHour(null, new BigDecimal("3600"))).isNull();
    assertThat(OperationalMetricsMath.ratePerHour(3L, new BigDecimal("7200"))).isEqualByComparingTo("1.50");
  }

  @Test
  @DisplayName("Date window parser enforces half-open [startInclusive, endExclusive) and rejects invalid ranges")
  void dateWindowValidation() {
    LocalDateTime ref = LocalDateTime.of(2026, 11, 12, 14, 30, 0);

    // Date-only window spans [2026-11-01T00:00:00, 2026-11-02T00:00:00) = 24.00h
    OperationalDashboardFilter sameDay = OperationalDashboardFilter.parseAndValidate(
        "2026-11-01", "2026-11-01", null, null, null, ref);
    assertThat(sameDay.timeWindowApplied()).isTrue();
    assertThat(sameDay.windowStart()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
    assertThat(sameDay.windowEndExclusive()).isEqualTo(LocalDateTime.of(2026, 11, 2, 0, 0));
    assertThat(OperationalMetricsMath.hoursFromSeconds(sameDay.windowDurationExactSeconds()))
        .isEqualByComparingTo("24.00");

    // Exact date-time window
    OperationalDashboardFilter exactWindow = OperationalDashboardFilter.parseAndValidate(
        "2026-11-12T10:00:00", "2026-11-12T12:30:00", null, null, null, ref);
    assertThat(OperationalMetricsMath.hoursFromSeconds(exactWindow.windowDurationExactSeconds()))
        .isEqualByComparingTo("2.50");

    // Zero-duration datetime window [T, T) rejected
    assertThatThrownBy(() -> OperationalDashboardFilter.parseAndValidate(
        "2026-11-12T10:00:00", "2026-11-12T10:00:00", null, null, null, ref))
        .isInstanceOf(BadRequestException.class);

    // Inverted range rejected
    assertThatThrownBy(() -> OperationalDashboardFilter.parseAndValidate(
        "2026-11-12", "2026-11-01", null, null, null, ref))
        .isInstanceOf(BadRequestException.class);

    // One-sided range or mixed formats or unparseable strings rejected
    assertThatThrownBy(() -> OperationalDashboardFilter.parseAndValidate(
        "2026-11-01", null, null, null, null, ref))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> OperationalDashboardFilter.parseAndValidate(
        "2026-11-01", "2026-11-02T00:00:00", null, null, null, ref))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> OperationalDashboardFilter.parseAndValidate(
        "not-a-date", "2026-11-02", null, null, null, ref))
        .isInstanceOf(BadRequestException.class);
  }
}
