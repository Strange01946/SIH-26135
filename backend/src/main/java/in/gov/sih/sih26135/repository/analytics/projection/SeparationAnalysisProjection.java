package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection record representing voluntary vs involuntary separation totals (formal Query Q18)
 * from {@code vw_attrition_reason_summary}.
 */
public record SeparationAnalysisProjection(
    Long voluntaryExitCount,
    Long involuntaryExitCount,
    Long otherExitCount,
    Long totalExitCount
) {}
