package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection record representing top current skill gaps (formal Query Q13)
 * from {@code vw_skill_gap_summary} joined with {@code skills}.
 */
public record TopSkillGapProjection(
    Long skillId,
    String skillCode,
    String skillName,
    Long skillGapSeverityId,
    String severityCode,
    Long gapCount,
    Long traineeCount,
    Long currentGapCount,
    BigDecimal avgGapLevelDelta
) {}
