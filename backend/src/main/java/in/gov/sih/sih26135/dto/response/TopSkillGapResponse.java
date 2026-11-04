package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing top current skill gaps (formal Query Q13).
 *
 * <p>Aggregates skill gaps by skill and severity.
 * Fields {@code sectorId}, {@code sectorName}, {@code jobRoleId}, and {@code jobRoleName}
 * are provided for future model extensibility and evaluate to {@code null} where unsupported by the base query.
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
    BigDecimal avgGapLevelDelta,
    Long sectorId,
    String sectorName,
    Long jobRoleId,
    String jobRoleName
) {}
