package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing follow-up wave performance (formal Query Q19).
 *
 * @param typeCode follow-up type code (MONTH_3, MONTH_6, MONTH_12, etc.)
 * @param typeName human-readable wave type name
 * @param offsetMonths offset in months from placement/completion
 * @param taskCount total follow-up tasks in this wave
 * @param traineeCount distinct trainees in this wave
 * @param successCount count of successful contacts
 * @param noResponseCount count of tasks with no response
 * @param unreachableCount count of tasks where contact was unreachable
 * @param pendingCount count of open / pending tasks in this wave
 * @param successRatePct percentage of tasks successfully contacted
 * @param unreachableRatePct percentage of tasks where contact was unreachable
 * @param sharePct percentage of all follow-up tasks in this wave
 */
public record FollowupWavePerformanceResponse(
    String typeCode,
    String typeName,
    Integer offsetMonths,
    Long taskCount,
    Long traineeCount,
    Long successCount,
    Long noResponseCount,
    Long unreachableCount,
    Long pendingCount,
    BigDecimal successRatePct,
    BigDecimal unreachableRatePct,
    BigDecimal sharePct
) {}
