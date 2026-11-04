package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing grouped distribution and category breakdown results
 * (Q08, Q09, Q14, Q17, Q20, Q21, Q22).
 * Supports category identifiers/names, record counts ({@link Long}),
 * normalized percentages ({@link BigDecimal}), optional grouping dimensions,
 * and auxiliary metrics (e.g., average duration, salary, gap delta).
 */
public record DistributionBreakdownResponse(
    String categoryCode,
    String categoryName,
    Long count,
    BigDecimal percentage,
    String dimension,
    BigDecimal auxiliaryMetric
) {

  /**
   * Factory method for simple category distribution breakdown.
   *
   * @param categoryCode machine-readable category code
   * @param categoryName human-readable category name
   * @param count count of items in this category
   * @param percentage percentage share
   * @return initialized DistributionBreakdownResponse
   */
  public static DistributionBreakdownResponse of(
      String categoryCode,
      String categoryName,
      Long count,
      BigDecimal percentage) {
    return new DistributionBreakdownResponse(categoryCode, categoryName, count, percentage, null, null);
  }

  /**
   * Factory method for dimensional category distribution breakdown.
   *
   * @param categoryCode machine-readable category code
   * @param categoryName human-readable category name
   * @param count count of items in this category
   * @param percentage percentage share
   * @param dimension grouping dimension identifier
   * @return initialized DistributionBreakdownResponse
   */
  public static DistributionBreakdownResponse of(
      String categoryCode,
      String categoryName,
      Long count,
      BigDecimal percentage,
      String dimension) {
    return new DistributionBreakdownResponse(categoryCode, categoryName, count, percentage, dimension, null);
  }

  /**
   * Factory method for category distribution breakdown with auxiliary metric.
   *
   * @param categoryCode machine-readable category code
   * @param categoryName human-readable category name
   * @param count count of items in this category
   * @param percentage percentage share
   * @param dimension grouping dimension identifier
   * @param auxiliaryMetric auxiliary metric value
   * @return initialized DistributionBreakdownResponse
   */
  public static DistributionBreakdownResponse of(
      String categoryCode,
      String categoryName,
      Long count,
      BigDecimal percentage,
      String dimension,
      BigDecimal auxiliaryMetric) {
    return new DistributionBreakdownResponse(categoryCode, categoryName, count, percentage, dimension, auxiliaryMetric);
  }
}
