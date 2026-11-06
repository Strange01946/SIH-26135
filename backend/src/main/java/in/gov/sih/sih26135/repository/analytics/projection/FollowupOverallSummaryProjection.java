package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection representing authoritative nationwide aggregate totals over {@code vw_followup_fact}.
 *
 * @param totalTasks total follow-up tasks
 * @param totalTrainees distinct trainees with follow-up tasks
 * @param completedTasks tasks marked closed/completed
 * @param pendingTasks tasks still open/pending
 * @param successfulContacts tasks resulting in successful contact
 * @param noResponseCount tasks resulting in no response
 * @param unreachableCount tasks where contact was unreachable
 */
public record FollowupOverallSummaryProjection(
    Long totalTasks,
    Long totalTrainees,
    Long completedTasks,
    Long pendingTasks,
    Long successfulContacts,
    Long noResponseCount,
    Long unreachableCount
) {}
