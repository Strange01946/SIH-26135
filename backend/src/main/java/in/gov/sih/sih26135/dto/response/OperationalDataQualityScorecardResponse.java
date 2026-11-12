package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Phase 28.7 response DTO for {@code GET /api/v1/operational/data-quality-scorecard}.
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@code dataCleanlinessIndexPct}, {@code eligiblePopulation}, {@code distinctAffectedEntities},
 *       {@code cleanEntitiesEstimate}, and {@code domainBreakdown} always reflect the
 *       <b>current</b> non-deleted entity cohort and all currently active unresolved issues
 *       ({@code is_open_flag = 1}), regardless of whether {@code detected_at} preceded
 *       {@code windowStart} ({@code cleanlinessScope = "CURRENT_COHORT_ACTIVE_ISSUES"}).</li>
 *   <li>{@code totalActiveIssues}, {@code activeIssuesByStatus}, {@code activeIssuesBySeverity},
 *       {@code topFailingRules}, {@code unruledActiveIssues}, {@code resolvedIssueCount},
 *       {@code reopenedIssueCount}, and {@code averageResolutionTimeHours} reflect period-filtered
 *       issue activity when {@code timeWindowApplied == true}
 *       ({@code periodMetricsScope = "WINDOWED_ISSUE_ACTIVITY"}), or all current/lifetime activity
 *       otherwise.</li>
 * </ul>
 */
public record OperationalDataQualityScorecardResponse(
    BigDecimal dataCleanlinessIndexPct,
    String cleanlinessIndexFormula,
    String cleanlinessScope,
    Long eligiblePopulation,
    Long distinctAffectedEntities,
    Long cleanEntitiesEstimate,
    Long totalActiveIssues,
    Map<String, Long> activeIssuesByStatus,
    Map<String, Long> activeIssuesBySeverity,
    List<DomainCleanlinessMetric> domainBreakdown,
    String periodMetricsScope,
    String topRulesRankingBasis,
    List<FailingRuleMetric> topFailingRules,
    Long unruledActiveIssues,
    BigDecimal averageResolutionTimeHours,
    String resolutionTimeUnit,
    Long resolvedIssueCount,
    Long reopenedIssueCount,
    LocalDateTime windowStart,
    LocalDateTime windowEndExclusive,
    boolean timeWindowApplied,
    LocalDateTime evaluatedAt
) {

  public OperationalDataQualityScorecardResponse {
    activeIssuesByStatus = activeIssuesByStatus != null
        ? Collections.unmodifiableMap(activeIssuesByStatus)
        : Map.of();
    activeIssuesBySeverity = activeIssuesBySeverity != null
        ? Collections.unmodifiableMap(activeIssuesBySeverity)
        : Map.of();
    domainBreakdown = domainBreakdown != null
        ? Collections.unmodifiableList(domainBreakdown)
        : List.of();
    topFailingRules = topFailingRules != null
        ? Collections.unmodifiableList(topFailingRules)
        : List.of();
  }

  public record DomainCleanlinessMetric(
      String entityType,
      Long eligibleCount,
      Long distinctAffectedCount,
      Long activeIssueCount,
      BigDecimal cleanlinessIndexPct
  ) {}

  public record FailingRuleMetric(
      Long ruleId,
      String ruleCode,
      String ruleName,
      String targetEntityType,
      Long activeIssueCount,
      Long distinctAffectedEntities
  ) {}
}
