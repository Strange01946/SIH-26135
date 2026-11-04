package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standardized response DTO representing a single data point in a dashboard time series.
 * Supports date ranges, string period labels (e.g., month, quarter, FY),
 * count-based metrics ({@link Long}), decimal-based metrics ({@link BigDecimal}),
 * and optional grouping dimensions.
 */
public record TimeSeriesDataPointResponse(
    String period,
    LocalDate startDate,
    LocalDate endDate,
    String metricName,
    Long countValue,
    BigDecimal decimalValue,
    String dimension
) {

  /**
   * Factory method for count-based time-series metrics.
   *
   * @param period period label (e.g., "2026-Q1", "2026-10")
   * @param metricName name of the metric
   * @param countValue count metric value
   * @return initialized TimeSeriesDataPointResponse
   */
  public static TimeSeriesDataPointResponse ofCount(String period, String metricName, Long countValue) {
    return new TimeSeriesDataPointResponse(period, null, null, metricName, countValue, null, null);
  }

  /**
   * Factory method for count-based time-series metrics with explicit date range.
   *
   * @param period period label
   * @param startDate start date of the period
   * @param endDate end date of the period
   * @param metricName name of the metric
   * @param countValue count metric value
   * @return initialized TimeSeriesDataPointResponse
   */
  public static TimeSeriesDataPointResponse ofCount(
      String period,
      LocalDate startDate,
      LocalDate endDate,
      String metricName,
      Long countValue) {
    return new TimeSeriesDataPointResponse(period, startDate, endDate, metricName, countValue, null, null);
  }

  /**
   * Factory method for decimal/rate/monetary time-series metrics.
   *
   * @param period period label
   * @param metricName name of the metric
   * @param decimalValue decimal/percentage/monetary metric value
   * @return initialized TimeSeriesDataPointResponse
   */
  public static TimeSeriesDataPointResponse ofDecimal(String period, String metricName, BigDecimal decimalValue) {
    return new TimeSeriesDataPointResponse(period, null, null, metricName, null, decimalValue, null);
  }

  /**
   * Factory method for decimal/rate/monetary time-series metrics with explicit date range.
   *
   * @param period period label
   * @param startDate start date of the period
   * @param endDate end date of the period
   * @param metricName name of the metric
   * @param decimalValue decimal/percentage/monetary metric value
   * @return initialized TimeSeriesDataPointResponse
   */
  public static TimeSeriesDataPointResponse ofDecimal(
      String period,
      LocalDate startDate,
      LocalDate endDate,
      String metricName,
      BigDecimal decimalValue) {
    return new TimeSeriesDataPointResponse(period, startDate, endDate, metricName, null, decimalValue, null);
  }
}
