package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard response for Skill Gap, Industry Demand Alignment,
 * and Remediation Analytics (formal Queries Q13 and Q14).
 */
public record SkillGapDashboardResponse(
    Long totalCurrentGaps,
    Long totalAffectedTrainees,
    Long totalRecommendations,
    Long totalAcceptedRecommendations,
    BigDecimal overallAcceptanceRatePct,
    List<TopSkillGapResponse> topSkillGaps,
    List<SkillGapSeverityBreakdownResponse> severityBreakdown
) {

  /**
   * Compact constructor enforcing non-null immutable lists.
   */
  public SkillGapDashboardResponse {
    topSkillGaps = topSkillGaps != null ? List.copyOf(topSkillGaps) : List.of();
    severityBreakdown = severityBreakdown != null ? List.copyOf(severityBreakdown) : List.of();
  }
}
