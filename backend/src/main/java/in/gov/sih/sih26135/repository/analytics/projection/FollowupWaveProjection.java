package in.gov.sih.sih26135.repository.analytics.projection;

import java.math.BigDecimal;

/**
 * Projection for formal Query Q19 follow-up outreach success by wave.
 *
 * @param followupTypeCode follow-up type code (MONTH_3, MONTH_6, MONTH_12, etc.)
 * @param typeName human-readable wave type name
 * @param followupOffsetMonths offset in months from placement/completion
 * @param taskCount total follow-up tasks in this wave
 * @param traineeCount distinct trainees in this wave
 * @param successCount count of successful contacts
 * @param noResponseCount count of tasks with no response
 * @param unreachableCount count of tasks where contact was unreachable / invalid
 * @param successRatePct percentage of tasks successfully contacted
 */
public record FollowupWaveProjection(
    String followupTypeCode,
    String typeName,
    Integer followupOffsetMonths,
    Long taskCount,
    Long traineeCount,
    Long successCount,
    Long noResponseCount,
    Long unreachableCount,
    BigDecimal successRatePct
) {}
