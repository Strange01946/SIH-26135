package in.gov.sih.sih26135.repository.analytics.projection;

/**
 * Projection representing authoritative nationwide aggregate totals over {@code vw_survey_response_fact}.
 *
 * @param totalResponseRows total survey response rows (invitations/opportunities)
 * @param totalTrainees distinct trainees surveyed
 * @param submittedCount count of submitted survey responses
 * @param pendingCount count of pending / unsubmitted responses
 */
public record SurveyOverallSummaryProjection(
    Long totalResponseRows,
    Long totalTrainees,
    Long submittedCount,
    Long pendingCount
) {}
