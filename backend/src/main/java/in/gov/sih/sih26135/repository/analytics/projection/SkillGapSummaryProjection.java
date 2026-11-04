package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection record representing overall skill gap totals and recommendation metrics
 * across all current skill gaps from {@code vw_skill_gap_fact}.
 */
public record SkillGapSummaryProjection(
    Long totalCurrentGaps,
    Long totalAffectedTrainees,
    Long totalRecommendations,
    Long totalAcceptedRecommendations
) {}
