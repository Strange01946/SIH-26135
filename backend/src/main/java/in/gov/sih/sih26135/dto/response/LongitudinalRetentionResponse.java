package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Standardized response DTO representing longitudinal post-placement retention tracking (formal Query Q12).
 * Exposes milestone progression at 6, 12, 24, and 36 months alongside cohort summary metrics.
 */
public record LongitudinalRetentionResponse(
    Long employmentSpellCount,
    RetentionCohortResponse cohortSummary,
    List<RetentionMilestoneResponse> milestones
) {

  /**
   * Compact constructor enforcing non-null immutable milestone list.
   */
  public LongitudinalRetentionResponse {
    milestones = milestones != null ? List.copyOf(milestones) : List.of();
  }
}
