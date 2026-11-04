package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing skill gap severity breakdown and recommendation uptake mix
 * (formal Query Q14).
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
