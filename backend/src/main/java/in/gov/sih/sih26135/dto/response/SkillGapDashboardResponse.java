package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard response for Skill Gap Analytics (formal Queries Q13 and Q14).
 *
 * <p>Summary Metric Semantics:
 * <ul>
 *   <li>{@code totalCurrentGaps}: Nationwide count of active/unresolved skill gaps ({@code is_current = 1}).</li>
 *   <li>{@code totalAffectedTrainees}: Distinct count of trainees currently having at least one active skill gap.</li>
 *   <li>{@code totalRecommendations}: Total remediation recommendation actions created across active skill gaps.</li>
 *   <li>{@code totalAcceptedRecommendations}: Total remediation recommendations accepted by trainees.</li>
 *   <li>{@code overallAcceptanceRatePct}: Overall acceptance percentage: {@code (totalAcceptedRecommendations / totalRecommendations) * 100}.</li>
 * </ul>
 * Note: A recommendation assignment rate percentage is unsupported by the frozen schema because
 * {@code skill_gap_recommendations} does not track an assignment lifecycle separate from recommendation creation and acceptance.
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
