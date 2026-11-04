package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection record representing skill gap severity distribution and recommendation uptake
 * (formal Query Q14) from {@code vw_skill_gap_fact}.
 */
public record SkillGapSeverityMixProjection(
    String severityCode,
    String severityName,
    Long currentGapCount,
    Long traineeCount,
    Long recommendationCount,
    Long acceptedRecommendationCount
) {}
