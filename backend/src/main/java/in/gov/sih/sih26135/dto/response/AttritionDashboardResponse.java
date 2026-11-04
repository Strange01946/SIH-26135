package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Consolidated response DTO representing unemployment, re-employment, exit separation,
 * and attrition analytics for the SIH-26135 platform (Queries Q15, Q16, Q17, Q18).
 */
public record AttritionDashboardResponse(
    UnemploymentSummaryResponse unemploymentSummary,
    List<UnemploymentReasonBreakdownResponse> unemploymentReasons,
    SeparationAnalysisResponse separationAnalysis,
    List<AttritionReasonBreakdownResponse> exitReasons
) {

  /**
   * Compact constructor enforcing non-null immutable collections.
   */
  public AttritionDashboardResponse {
    unemploymentReasons = unemploymentReasons != null ? List.copyOf(unemploymentReasons) : List.of();
    exitReasons = exitReasons != null ? List.copyOf(exitReasons) : List.of();
  }
}
