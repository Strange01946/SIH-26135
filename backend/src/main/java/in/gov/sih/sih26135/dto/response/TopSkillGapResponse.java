package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing top current skill gaps (formal Query Q13).
 *
 * <p>Aggregates skill gaps by skill and severity based on {@code vw_skill_gap_summary}.
 * Note on Sector and Role Coverage:
 * Source view {@code vw_skill_gap_summary} aggregates strictly by skill and severity code.
 * In the frozen schema, skills have a many-to-many relationship with job roles and courses,
 * while {@code skill_gap_assessments.job_role_id} is optional and absent on non-role assessments.
 * Sector and job-role dimensions are not part of formal Query Q13 and cannot be joined without
 * altering skill-gap ranking semantics or causing cartesian count inflation.
 */
public record TopSkillGapResponse(
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
