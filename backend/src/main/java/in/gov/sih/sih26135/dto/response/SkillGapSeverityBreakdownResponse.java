package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing skill gap severity breakdown and recommendation uptake mix
 * (formal Query Q14).
 *
 * <p>Severity Categories:
 * Reflects canonical schema reference codes from {@code ref_skill_gap_severity}:
 * {@code NONE} (No gap / meets requirement), {@code LOW} (Low), {@code MODERATE} (Moderate),
 * {@code HIGH} (High), and {@code CRITICAL} (Critical). All categories are preserved without
 * artificial merging or renaming to ensure source data integrity.
 *
 * <p>Recommendation Metric Semantics:
 * <ul>
 *   <li>{@code recommendationCount}: Total remediation recommendations created for gaps of this severity.</li>
 *   <li>{@code acceptedRecommendationCount}: Count of recommendations accepted by trainees ({@code is_accepted_flag = 1}).</li>
 *   <li>{@code sharePct}: Percentage share of current gaps in this severity category over total active gaps.</li>
 *   <li>{@code acceptanceRatePct}: Recommendation acceptance rate: {@code (acceptedRecommendationCount / recommendationCount) * 100}.</li>
 * </ul>
 * Note: A distinct recommendation assignment-rate percentage is unsupported by the schema,
 * as {@code skill_gap_recommendations} does not track assignment workflows (lacks assigned user, assignment status,
 * or assignment timestamps) separate from recommendation creation and acceptance.
 */
public record SkillGapSeverityBreakdownResponse(
    String severityCode,
    String severityName,
    Long currentGapCount,
    Long traineeCount,
    Long recommendationCount,
    Long acceptedRecommendationCount,
    BigDecimal sharePct,
    BigDecimal acceptanceRatePct
) {}
