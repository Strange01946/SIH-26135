package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard response DTO for Longitudinal Follow-up Outreach and SLA Performance (Query Q19).
 *
 * @param totalTasks total follow-up tasks across all campaigns
 * @param totalTrainees distinct trainees scheduled for follow-up outreach
 * @param completedTasks count of follow-up tasks completed
 * @param pendingTasks count of follow-up tasks currently open / pending
 * @param successfulContacts count of tasks resulting in successful contact
 * @param noResponseCount count of tasks resulting in no response
 * @param unreachableCount count of tasks where contact was unreachable
 * @param contactSuccessRatePct percentage of tasks resulting in successful contact
 * @param unreachableRatePct percentage of tasks where contact was unreachable
 * @param taskCompletionRatePct percentage of tasks completed
 * @param wavePerformance wave-by-wave performance breakdown (Q19)
 * @param slaPerformance SLA performance status and metrics
 */
public record FollowupDashboardResponse(
    Long totalTasks,
    Long totalTrainees,
    Long completedTasks,
    Long pendingTasks,
    Long successfulContacts,
    Long noResponseCount,
    Long unreachableCount,
    BigDecimal contactSuccessRatePct,
    BigDecimal unreachableRatePct,
    BigDecimal taskCompletionRatePct,
    List<FollowupWavePerformanceResponse> wavePerformance,
    FollowupSlaResponse slaPerformance
) {}
