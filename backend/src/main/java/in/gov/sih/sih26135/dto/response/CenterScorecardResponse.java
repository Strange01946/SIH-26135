package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Phase 30.5 standardized response DTO representing Center Cleanliness Scorecard & Enrollment Outcome Metrics.
 */
public record CenterScorecardResponse(
    Long centerId,
    String centerCode,
    String centerName,
    Long providerId,
    BigDecimal cleanlinessIndexPct,
    String cleanlinessFormula,
    String cleanlinessScope,
    Long eligiblePopulation,
    Long distinctAffectedEntities,
    Long cleanEntitiesEstimate,
    Long totalActiveIssues,
    Map<String, Long> activeIssuesBySeverity,
    Map<String, Long> activeIssuesByCategory,
    Long totalEnrollments,
    Long completedEnrollments,
    Long droppedEnrollments,
    Long inProgressEnrollments,
    BigDecimal completionRatePct,
    BigDecimal dropoutRatePct,
    LocalDateTime evaluatedAt
) {

  public CenterScorecardResponse {
    cleanlinessIndexPct = cleanlinessIndexPct != null
        ? cleanlinessIndexPct.setScale(2, RoundingMode.HALF_UP)
        : null;
    completionRatePct = completionRatePct != null
        ? completionRatePct.setScale(2, RoundingMode.HALF_UP)
        : null;
    dropoutRatePct = dropoutRatePct != null
        ? dropoutRatePct.setScale(2, RoundingMode.HALF_UP)
        : null;
    activeIssuesBySeverity = activeIssuesBySeverity != null
        ? Collections.unmodifiableMap(activeIssuesBySeverity)
        : Map.of();
    activeIssuesByCategory = activeIssuesByCategory != null
        ? Collections.unmodifiableMap(activeIssuesByCategory)
        : Map.of();
  }
}
