package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection record representing reasons for employment exit and voluntary vs involuntary mix
 * (formal Query Q17) from {@code vw_attrition_reason_summary} joined with reference tables.
 */
public record AttritionReasonProjection(
    Long employmentExitReasonId,
    String exitReasonCode,
    String exitReasonName,
    Long separationNatureId,
    String separationNatureCode,
    String separationNatureName,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Long exitCount,
    Long traineeCount
) {}
