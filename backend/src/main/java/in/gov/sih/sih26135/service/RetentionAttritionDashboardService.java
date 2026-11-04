package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;

/**
 * Service interface defining contracts for longitudinal retention tracking,
 * unemployment duration, re-employment conversion, and employment exit/attrition analytics.
 */
public interface RetentionAttritionDashboardService {

  /**
   * Retrieves multi-milestone longitudinal post-placement retention tracking (formal Query Q12).
   * Exposes observable counts, retained counts, and survival rates at 6, 12, 24, and 36 months.
   *
   * @return longitudinal retention response
   */
  LongitudinalRetentionResponse getLongitudinalRetention();

  /**
   * Retrieves consolidated unemployment KPIs, reasons, separation analysis,
   * and exit attrition analytics (formal Queries Q15, Q16, Q17, Q18).
   *
   * @return consolidated attrition dashboard response
   */
  AttritionDashboardResponse getAttritionDashboard();
}
